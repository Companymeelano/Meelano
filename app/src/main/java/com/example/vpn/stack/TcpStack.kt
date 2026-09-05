package com.example.vpn.stack

import com.example.core.ProxyEndpoint
import com.example.vpn.net.TcpHeader
import com.example.vpn.proto.Destination
import com.example.vpn.proto.OutboundFactory
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicLong

/**
 * The userspace TCP stack — the piece that actually makes websites load.
 *
 * A `VpnService` TUN device hands the app raw IP packets; the kernel will *not*
 * route them onward. This class terminates each TCP flow locally, opens a real
 * proxy tunnel to the destination, and shuttles bytes between the two. Without
 * it, a VPN can look "connected" while every TCP connection silently hangs.
 */
class TcpStack(
    private val endpoint: ProxyEndpoint,
    private val protect: (Socket) -> Boolean,
    private val writeToTun: (ByteArray) -> Unit,
    private val log: (String) -> Unit
) {
    private val connections = ConcurrentHashMap<String, TcpConnection>()

    private val workers = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "meelano-tcp").apply { isDaemon = true }
    } as ThreadPoolExecutor

    val bytesDown = AtomicLong(0)
    val bytesUp = AtomicLong(0)
    private val opened = AtomicLong(0)
    private val failed = AtomicLong(0)

    val activeConnections: Int get() = connections.size
    val totalOpened: Long get() = opened.get()
    val totalFailed: Long get() = failed.get()

    /** Feeds one IPv4/TCP packet read from the TUN device into the stack. */
    fun handlePacket(packet: ByteArray, length: Int) {
        val segment = TcpHeader.parse(packet, length) ?: return
        val key = segment.key()

        val existing = connections[key]
        if (segment.isSyn && existing == null) {
            openConnection(key, segment)
            return
        }
        if (existing == null) {
            // Unknown flow (e.g. after our own teardown): tell the app to give up
            // instead of leaving the socket hanging until its timeout.
            if (!segment.isRst) sendReset(segment)
            return
        }
        existing.onSegment(segment)
    }

    private fun openConnection(key: String, segment: TcpHeader.Segment) {
        val destinationIp = TcpHeader.ip(segment.destinationIp)
        // Prefer the domain we learned from DNS: the exit node resolves it itself,
        // which sidesteps DNS poisoning and picks a healthy CDN edge.
        val host = DnsMap.hostFor(destinationIp) ?: destinationIp
        val destination = Destination.of(host, segment.destinationPort)

        val connection = TcpConnection(
            key = key,
            localIp = segment.sourceIp,
            remoteIp = segment.destinationIp,
            localPort = segment.sourcePort,
            remotePort = segment.destinationPort,
            writeToTun = writeToTun,
            onClosed = { connections.remove(it) },
            onBytes = { down, up ->
                if (down > 0) bytesDown.addAndGet(down)
                if (up > 0) bytesUp.addAndGet(up)
            }
        )
        connections[key] = connection

        // Answer SYN immediately; the proxy dial happens in the background so the
        // app never waits on our handshake.
        connection.onSyn(segment)
        opened.incrementAndGet()

        workers.execute {
            try {
                val tunnel = OutboundFactory.create(endpoint, destination, protect)
                connection.attachOutbound(tunnel)
                connection.pumpDownstream()
            } catch (e: Exception) {
                failed.incrementAndGet()
                if (failed.get() <= 12) {
                    log("اتصال به ${destination.host}:${destination.port} ناموفق: ${e.message ?: e::class.java.simpleName}")
                }
                connection.reset()
            }
        }
    }

    private fun sendReset(segment: TcpHeader.Segment) {
        val packet = TcpHeader.build(
            sourceIp = segment.destinationIp,
            destinationIp = segment.sourceIp,
            sourcePort = segment.destinationPort,
            destinationPort = segment.sourcePort,
            sequence = segment.acknowledgement,
            acknowledgement = TcpHeader.seqAdd(segment.sequence, segment.payload.size),
            flags = TcpHeader.RST or TcpHeader.ACK,
            window = 0
        )
        runCatching { writeToTun(packet) }
    }

    fun shutdown() {
        connections.values.forEach { it.closeQuietly() }
        connections.clear()
        workers.shutdownNow()
    }
}
