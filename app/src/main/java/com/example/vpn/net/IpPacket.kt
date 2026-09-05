package com.example.vpn.net

import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Minimal but real IPv4 / UDP packet reader & writer.
 *
 * The tunnel uses this to actually inspect the packets the OS hands to the TUN
 * device and to craft valid replies (with correct IPv4 and UDP checksums) that
 * the kernel accepts. No simulation involved.
 */
object IpPacket {

    const val PROTO_TCP = 6
    const val PROTO_UDP = 17

    data class Udp(
        val sourceIp: ByteArray,
        val destinationIp: ByteArray,
        val sourcePort: Int,
        val destinationPort: Int,
        val payload: ByteArray
    ) {
        val destinationAddress: InetAddress get() = InetAddress.getByAddress(destinationIp)

        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    /** IPv4 protocol number of the packet, or -1 when it is not IPv4. */
    fun protocolOf(packet: ByteArray, length: Int): Int {
        if (length < 20) return -1
        val version = (packet[0].toInt() and 0xF0) ushr 4
        if (version != 4) return -1
        return packet[9].toInt() and 0xFF
    }

    /** Parses an IPv4/UDP datagram; returns null when the packet is not UDP over IPv4. */
    fun parseUdp(packet: ByteArray, length: Int): Udp? {
        if (protocolOf(packet, length) != PROTO_UDP) return null
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 8) return null

        val sourceIp = packet.copyOfRange(12, 16)
        val destinationIp = packet.copyOfRange(16, 20)
        val sourcePort = readUShort(packet, ihl)
        val destinationPort = readUShort(packet, ihl + 2)
        val udpLength = readUShort(packet, ihl + 4)
        val payloadLength = (udpLength - 8).coerceAtLeast(0)
        val payloadEnd = (ihl + 8 + payloadLength).coerceAtMost(length)
        if (payloadEnd <= ihl + 8) return null

        return Udp(
            sourceIp = sourceIp,
            destinationIp = destinationIp,
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            payload = packet.copyOfRange(ihl + 8, payloadEnd)
        )
    }

    /**
     * Builds a complete IPv4+UDP response packet (source/destination swapped
     * relative to the request) that can be written straight back into the TUN fd.
     */
    fun buildUdpResponse(request: Udp, payload: ByteArray): ByteArray {
        val totalLength = 20 + 8 + payload.size
        val buffer = ByteBuffer.allocate(totalLength)

        // ---- IPv4 header ----
        buffer.put(0x45.toByte())                 // version 4, IHL 5
        buffer.put(0)                             // DSCP / ECN
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0)                        // identification
        buffer.putShort(0x4000.toShort())         // don't fragment
        buffer.put(64)                            // TTL
        buffer.put(PROTO_UDP.toByte())
        buffer.putShort(0)                        // checksum placeholder
        buffer.put(request.destinationIp)         // swapped
        buffer.put(request.sourceIp)

        // ---- UDP header ----
        buffer.putShort(request.destinationPort.toShort())
        buffer.putShort(request.sourcePort.toShort())
        buffer.putShort((8 + payload.size).toShort())
        buffer.putShort(0)                        // checksum placeholder
        buffer.put(payload)

        val packet = buffer.array()
        writeUShort(packet, 10, checksum(packet, 0, 20))
        writeUShort(packet, 26, udpChecksum(packet, payload.size))
        return packet
    }

    private fun udpChecksum(packet: ByteArray, payloadSize: Int): Int {
        val udpLength = 8 + payloadSize
        var sum = 0L
        // pseudo header: src ip, dst ip, zero, protocol, udp length
        for (i in 12 until 20 step 2) sum += readUShort(packet, i)
        sum += PROTO_UDP.toLong()
        sum += udpLength.toLong()
        var i = 20
        while (i < 20 + udpLength - 1) {
            sum += readUShort(packet, i)
            i += 2
        }
        if (udpLength % 2 == 1) sum += (packet[20 + udpLength - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        val result = (sum.inv() and 0xFFFF).toInt()
        return if (result == 0) 0xFFFF else result
    }

    private fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        while (i < offset + length - 1) {
            sum += readUShort(data, i)
            i += 2
        }
        if (length % 2 == 1) sum += (data[offset + length - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    fun readUShort(data: ByteArray, index: Int): Int =
        ((data[index].toInt() and 0xFF) shl 8) or (data[index + 1].toInt() and 0xFF)

    private fun writeUShort(data: ByteArray, index: Int, value: Int) {
        data[index] = ((value ushr 8) and 0xFF).toByte()
        data[index + 1] = (value and 0xFF).toByte()
    }
}
