package com.example.vpn.stack

import com.example.vpn.net.TcpHeader
import com.example.vpn.proto.Outbound
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One proxied TCP flow: the userspace peer of a socket inside the phone.
 *
 * The app's kernel believes it is talking to the real destination; in reality we
 * terminate the connection here, forward the payload through an [Outbound]
 * proxy tunnel, and synthesise the acknowledgements and inbound segments that
 * make the illusion complete.
 */
class TcpConnection(
    val key: String,
    private val localIp: ByteArray,      // the app's address
    private val remoteIp: ByteArray,     // the address the app dialled
    private val localPort: Int,
    private val remotePort: Int,
    private val writeToTun: (ByteArray) -> Unit,
    private val onClosed: (String) -> Unit,
    private val onBytes: (down: Long, up: Long) -> Unit
) {
    /** Sequence number *we* use when sending towards the app. */
    private var sendSequence: Long = (Math.random() * 0xFFFFFF).toLong()

    /** Next byte we expect from the app. */
    private var receiveNext: Long = 0

    private val closed = AtomicBoolean(false)
    private var established = false
    private var outbound: Outbound? = null
    private var proxyOut: OutputStream? = null

    /** Payload the app sent before the proxy tunnel finished connecting. */
    private val earlyData = java.io.ByteArrayOutputStream()

    private val lock = Any()

    val isClosed: Boolean get() = closed.get()

    // ---- inbound from the app (TUN -> us) ----------------------------------

    /** Handles the app's SYN: we immediately answer SYN-ACK to keep latency low. */
    fun onSyn(segment: TcpHeader.Segment) {
        receiveNext = TcpHeader.seqAdd(segment.sequence, 1)
        sendPacket(TcpHeader.SYN or TcpHeader.ACK, ByteArray(0))
        sendSequence = TcpHeader.seqAdd(sendSequence, 1)
    }

    /** Handles data/ACK/FIN segments from the app. */
    fun onSegment(segment: TcpHeader.Segment) {
        if (closed.get()) return

        if (segment.isRst) {
            closeQuietly()
            return
        }

        if (segment.payload.isNotEmpty()) {
            // Ignore retransmissions of data we already consumed.
            if (segment.sequence == receiveNext) {
                receiveNext = TcpHeader.seqAdd(receiveNext, segment.payload.size)
                forwardUpstream(segment.payload)
                sendPacket(TcpHeader.ACK, ByteArray(0))
            } else {
                // Out of order or duplicate: re-acknowledge what we do have.
                sendPacket(TcpHeader.ACK, ByteArray(0))
            }
        }

        if (segment.isFin) {
            receiveNext = TcpHeader.seqAdd(receiveNext, 1)
            sendPacket(TcpHeader.ACK, ByteArray(0))
            // Half close upstream, then tear the flow down.
            runCatching { proxyOut?.flush() }
            sendPacket(TcpHeader.FIN or TcpHeader.ACK, ByteArray(0))
            sendSequence = TcpHeader.seqAdd(sendSequence, 1)
            closeQuietly()
        }
    }

    private fun forwardUpstream(payload: ByteArray) {
        synchronized(lock) {
            val sink = proxyOut
            if (sink == null) {
                earlyData.write(payload)
                return
            }
            try {
                sink.write(payload)
                sink.flush()
                onBytes(0, payload.size.toLong())
            } catch (e: IOException) {
                reset()
            }
        }
    }

    // ---- proxy side --------------------------------------------------------

    /** Called once the proxy tunnel is live; flushes anything buffered meanwhile. */
    fun attachOutbound(tunnel: Outbound) {
        synchronized(lock) {
            if (closed.get()) {
                runCatching { tunnel.close() }
                return
            }
            outbound = tunnel
            proxyOut = tunnel.output
            established = true

            val buffered = earlyData.toByteArray()
            earlyData.reset()
            if (buffered.isNotEmpty()) {
                try {
                    tunnel.output.write(buffered)
                    tunnel.output.flush()
                    onBytes(0, buffered.size.toLong())
                } catch (e: IOException) {
                    reset()
                }
            }
        }
    }

    /** Pumps proxy -> app until the tunnel ends. Runs on its own IO thread. */
    fun pumpDownstream() {
        val tunnel = outbound ?: return
        val buffer = ByteArray(MSS)
        try {
            while (!closed.get()) {
                val count = tunnel.input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                sendPacket(TcpHeader.ACK or TcpHeader.PSH, buffer.copyOf(count))
                sendSequence = TcpHeader.seqAdd(sendSequence, count)
                onBytes(count.toLong(), 0)
            }
            // Graceful close towards the app.
            if (!closed.get()) {
                sendPacket(TcpHeader.FIN or TcpHeader.ACK, ByteArray(0))
                sendSequence = TcpHeader.seqAdd(sendSequence, 1)
            }
        } catch (e: Exception) {
            // Connection died — let the app know rather than hanging forever.
        } finally {
            closeQuietly()
        }
    }

    /** Sends RST to the app: used when the proxy refuses or dies. */
    fun reset() {
        if (closed.getAndSet(true)) return
        runCatching { sendPacket(TcpHeader.RST or TcpHeader.ACK, ByteArray(0)) }
        runCatching { outbound?.close() }
        onClosed(key)
    }

    fun closeQuietly() {
        if (closed.getAndSet(true)) return
        runCatching { outbound?.close() }
        onClosed(key)
    }

    // ---- packet synthesis --------------------------------------------------

    private fun sendPacket(flags: Int, payload: ByteArray) {
        val packet = TcpHeader.build(
            sourceIp = remoteIp,             // we impersonate the remote peer
            destinationIp = localIp,
            sourcePort = remotePort,
            destinationPort = localPort,
            sequence = sendSequence,
            acknowledgement = receiveNext,
            flags = flags,
            window = WINDOW,
            payload = payload
        )
        writeToTun(packet)
    }

    companion object {
        /** Conservative MSS that survives every mobile MTU we are likely to meet. */
        const val MSS = 1400
        const val WINDOW = 65535
    }
}
