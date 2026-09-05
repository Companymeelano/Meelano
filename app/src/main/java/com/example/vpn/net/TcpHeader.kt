package com.example.vpn.net

import java.nio.ByteBuffer

/**
 * Real IPv4 + TCP header reader and writer.
 *
 * The userspace TCP stack ([com.example.vpn.stack.TcpStack]) uses this to parse
 * the segments the kernel writes into the TUN device and to synthesise valid
 * replies — with correct IPv4 and TCP checksums — that the kernel accepts as if
 * they came from the real peer.
 */
object TcpHeader {

    const val FIN = 0x01
    const val SYN = 0x02
    const val RST = 0x04
    const val PSH = 0x08
    const val ACK = 0x10

    data class Segment(
        val sourceIp: ByteArray,
        val destinationIp: ByteArray,
        val sourcePort: Int,
        val destinationPort: Int,
        val sequence: Long,
        val acknowledgement: Long,
        val flags: Int,
        val window: Int,
        val payload: ByteArray
    ) {
        val isSyn: Boolean get() = flags and SYN != 0
        val isAck: Boolean get() = flags and ACK != 0
        val isFin: Boolean get() = flags and FIN != 0
        val isRst: Boolean get() = flags and RST != 0

        /** Stable identity of the flow this segment belongs to. */
        fun key(): String = "${ip(sourceIp)}:$sourcePort>${ip(destinationIp)}:$destinationPort"

        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    fun ip(bytes: ByteArray): String =
        bytes.joinToString(".") { (it.toInt() and 0xFF).toString() }

    /** Parses an IPv4/TCP segment, or returns null when the packet is not TCP over IPv4. */
    fun parse(packet: ByteArray, length: Int): Segment? {
        if (IpPacket.protocolOf(packet, length) != IpPacket.PROTO_TCP) return null
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 20) return null

        val totalLength = IpPacket.readUShort(packet, 2).coerceAtMost(length)
        val dataOffset = ((packet[ihl + 12].toInt() and 0xF0) ushr 4) * 4
        if (dataOffset < 20 || ihl + dataOffset > totalLength) return null

        val payloadStart = ihl + dataOffset
        val payload =
            if (totalLength > payloadStart) packet.copyOfRange(payloadStart, totalLength)
            else ByteArray(0)

        return Segment(
            sourceIp = packet.copyOfRange(12, 16),
            destinationIp = packet.copyOfRange(16, 20),
            sourcePort = IpPacket.readUShort(packet, ihl),
            destinationPort = IpPacket.readUShort(packet, ihl + 2),
            sequence = readUInt(packet, ihl + 4),
            acknowledgement = readUInt(packet, ihl + 8),
            flags = packet[ihl + 13].toInt() and 0x3F,
            window = IpPacket.readUShort(packet, ihl + 14),
            payload = payload
        )
    }

    /**
     * Builds a complete IPv4+TCP packet. [sourceIp]/[sourcePort] describe the
     * endpoint we are impersonating (the remote peer), so the result flows
     * "inbound" towards the app when written into the TUN fd.
     */
    fun build(
        sourceIp: ByteArray,
        destinationIp: ByteArray,
        sourcePort: Int,
        destinationPort: Int,
        sequence: Long,
        acknowledgement: Long,
        flags: Int,
        window: Int,
        payload: ByteArray = EMPTY
    ): ByteArray {
        val totalLength = 20 + 20 + payload.size
        val buffer = ByteBuffer.allocate(totalLength)

        // ---- IPv4 header ----
        buffer.put(0x45.toByte())
        buffer.put(0)
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0)
        buffer.putShort(0x4000.toShort())          // don't fragment
        buffer.put(64)                             // TTL
        buffer.put(IpPacket.PROTO_TCP.toByte())
        buffer.putShort(0)                         // checksum placeholder
        buffer.put(sourceIp)
        buffer.put(destinationIp)

        // ---- TCP header ----
        buffer.putShort(sourcePort.toShort())
        buffer.putShort(destinationPort.toShort())
        buffer.putInt(sequence.toInt())
        buffer.putInt(acknowledgement.toInt())
        buffer.put(0x50)                           // data offset 5 words, no options
        buffer.put(flags.toByte())
        buffer.putShort(window.toShort())
        buffer.putShort(0)                         // checksum placeholder
        buffer.putShort(0)                         // urgent pointer
        buffer.put(payload)

        val packet = buffer.array()
        writeUShort(packet, 10, ipChecksum(packet))
        writeUShort(packet, 36, tcpChecksum(packet, payload.size))
        return packet
    }

    private val EMPTY = ByteArray(0)

    private fun ipChecksum(packet: ByteArray): Int {
        var sum = 0L
        var i = 0
        while (i < 20) {
            sum += IpPacket.readUShort(packet, i)
            i += 2
        }
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun tcpChecksum(packet: ByteArray, payloadSize: Int): Int {
        val tcpLength = 20 + payloadSize
        var sum = 0L
        // pseudo header: source ip, destination ip, zero, protocol, tcp length
        for (i in 12 until 20 step 2) sum += IpPacket.readUShort(packet, i)
        sum += IpPacket.PROTO_TCP.toLong()
        sum += tcpLength.toLong()

        var i = 20
        while (i < 20 + tcpLength - 1) {
            sum += IpPacket.readUShort(packet, i)
            i += 2
        }
        if (tcpLength % 2 == 1) sum += (packet[20 + tcpLength - 1].toInt() and 0xFF) shl 8

        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    fun readUInt(data: ByteArray, index: Int): Long =
        ((data[index].toLong() and 0xFF) shl 24) or
            ((data[index + 1].toLong() and 0xFF) shl 16) or
            ((data[index + 2].toLong() and 0xFF) shl 8) or
            (data[index + 3].toLong() and 0xFF)

    private fun writeUShort(data: ByteArray, index: Int, value: Int) {
        data[index] = ((value ushr 8) and 0xFF).toByte()
        data[index + 1] = (value and 0xFF).toByte()
    }

    /** 32-bit sequence arithmetic (wraps at 2^32). */
    fun seqAdd(value: Long, delta: Int): Long = (value + delta) and 0xFFFFFFFFL
}
