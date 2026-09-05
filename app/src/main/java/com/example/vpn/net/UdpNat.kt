package com.example.vpn.net

import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * A working userspace UDP NAT for the TUN device.
 *
 * For every (srcIp, srcPort, dstIp, dstPort) flow it keeps a real, protected
 * [DatagramSocket] outside the tunnel, forwards the payload, and writes the
 * upstream answer back into TUN as a valid IPv4/UDP packet. QUIC, game traffic
 * and plain UDP services therefore keep working while the VPN owns the route.
 */
class UdpNat(
    private val protect: (DatagramSocket) -> Boolean,
    private val tunOutput: FileOutputStream,
    private val onBytes: (rx: Int, tx: Int) -> Unit,
    private val onLog: (String) -> Unit = {}
) {
    private data class Key(val srcPort: Int, val dstIp: String, val dstPort: Int)

    private class Flow(val socket: DatagramSocket, val request: IpPacket.Udp) {
        @Volatile
        var lastUsed: Long = System.currentTimeMillis()
    }

    private val flows = ConcurrentHashMap<Key, Flow>()
    private val readers = Executors.newCachedThreadPool()
    val activeFlows: Int get() = flows.size
    val forwardedPackets = AtomicLong(0)

    @Volatile
    private var running = true

    fun forward(packet: IpPacket.Udp) {
        if (!running) return
        val key = Key(
            packet.sourcePort,
            RouteTable.longToIp(bytesToLong(packet.destinationIp)),
            packet.destinationPort
        )
        val flow = flows.getOrPut(key) { createFlow(packet) ?: return }
        flow.lastUsed = System.currentTimeMillis()
        try {
            val datagram = DatagramPacket(
                packet.payload,
                packet.payload.size,
                InetSocketAddress(packet.destinationAddress, packet.destinationPort)
            )
            flow.socket.send(datagram)
            onBytes(0, packet.payload.size)
            forwardedPackets.incrementAndGet()
        } catch (e: Exception) {
            onLog("UDP flow error ${key.dstIp}:${key.dstPort} → ${e.message}")
            close(key)
        }
    }

    private fun createFlow(packet: IpPacket.Udp): Flow? = try {
        val socket = DatagramSocket().apply {
            protect(this)
            soTimeout = 0
        }
        val flow = Flow(socket, packet)
        readers.execute { pump(flow) }
        flow
    } catch (e: Exception) {
        onLog("UDP socket allocation failed: ${e.message}")
        null
    }

    private fun pump(flow: Flow) {
        val buffer = ByteArray(2048)
        while (running && !flow.socket.isClosed) {
            try {
                val response = DatagramPacket(buffer, buffer.size)
                flow.socket.receive(response)
                val payload = buffer.copyOf(response.length)
                synchronized(tunOutput) {
                    tunOutput.write(IpPacket.buildUdpResponse(flow.request, payload))
                }
                flow.lastUsed = System.currentTimeMillis()
                onBytes(payload.size, 0)
            } catch (_: Exception) {
                break
            }
        }
    }

    /** Drops flows that have been idle for longer than [idleMillis]. */
    fun evictIdle(idleMillis: Long = 60_000) {
        val now = System.currentTimeMillis()
        flows.entries.filter { now - it.value.lastUsed > idleMillis }.forEach { close(it.key) }
    }

    private fun close(key: Key) {
        flows.remove(key)?.let {
            try {
                it.socket.close()
            } catch (_: Exception) {
            }
        }
    }

    fun shutdown() {
        running = false
        flows.keys.toList().forEach { close(it) }
        readers.shutdownNow()
    }

    private fun bytesToLong(bytes: ByteArray): Long {
        var value = 0L
        for (b in bytes) value = (value shl 8) or (b.toLong() and 0xFF)
        return value
    }
}
