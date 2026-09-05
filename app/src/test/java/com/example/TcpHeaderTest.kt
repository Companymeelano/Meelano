package com.example

import com.example.vpn.net.IpPacket
import com.example.vpn.net.TcpHeader
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the IPv4/TCP codec that the userspace stack depends on. If these
 * checksums are wrong the kernel silently drops our packets and nothing loads,
 * so the maths is pinned down here.
 */
class TcpHeaderTest {

    private val clientIp = byteArrayOf(172.toByte(), 19, 0, 2)
    private val serverIp = byteArrayOf(142.toByte(), 250.toByte(), 185.toByte(), 78)

    @Test
    fun `build then parse round trips every field`() {
        val payload = "GET / HTTP/1.1\r\n\r\n".toByteArray()
        val packet = TcpHeader.build(
            sourceIp = serverIp,
            destinationIp = clientIp,
            sourcePort = 443,
            destinationPort = 51234,
            sequence = 0x11223344L,
            acknowledgement = 0x55667788L,
            flags = TcpHeader.ACK or TcpHeader.PSH,
            window = 65535,
            payload = payload
        )

        val parsed = TcpHeader.parse(packet, packet.size)
        requireNotNull(parsed)
        assertArrayEquals(serverIp, parsed.sourceIp)
        assertArrayEquals(clientIp, parsed.destinationIp)
        assertEquals(443, parsed.sourcePort)
        assertEquals(51234, parsed.destinationPort)
        assertEquals(0x11223344L, parsed.sequence)
        assertEquals(0x55667788L, parsed.acknowledgement)
        assertEquals(65535, parsed.window)
        assertTrue(parsed.isAck)
        assertArrayEquals(payload, parsed.payload)
    }

    @Test
    fun `ipv4 header checksum verifies to zero`() {
        val packet = TcpHeader.build(
            sourceIp = serverIp,
            destinationIp = clientIp,
            sourcePort = 80,
            destinationPort = 40000,
            sequence = 1,
            acknowledgement = 1,
            flags = TcpHeader.SYN or TcpHeader.ACK,
            window = 8192
        )
        // Summing a correct IPv4 header (including its checksum) yields 0xFFFF.
        var sum = 0L
        for (i in 0 until 20 step 2) sum += IpPacket.readUShort(packet, i)
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        assertEquals(0xFFFFL, sum)
    }

    @Test
    fun `tcp checksum verifies over the pseudo header`() {
        val payload = ByteArray(37) { it.toByte() }
        val packet = TcpHeader.build(
            sourceIp = serverIp,
            destinationIp = clientIp,
            sourcePort = 443,
            destinationPort = 12345,
            sequence = 99,
            acknowledgement = 100,
            flags = TcpHeader.ACK,
            window = 4096,
            payload = payload
        )

        val tcpLength = 20 + payload.size
        var sum = 0L
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
        assertEquals(0xFFFFL, sum)
    }

    @Test
    fun `flags decode independently`() {
        val fin = TcpHeader.parse(
            TcpHeader.build(serverIp, clientIp, 1, 2, 0, 0, TcpHeader.FIN or TcpHeader.ACK, 0),
            40
        )
        requireNotNull(fin)
        assertTrue(fin.isFin)
        assertTrue(fin.isAck)
        assertTrue(!fin.isSyn)
        assertTrue(!fin.isRst)
    }

    @Test
    fun `sequence arithmetic wraps at 32 bits`() {
        assertEquals(0L, TcpHeader.seqAdd(0xFFFFFFFFL, 1))
        assertEquals(4L, TcpHeader.seqAdd(0xFFFFFFFFL, 5))
        assertEquals(1500L, TcpHeader.seqAdd(1000L, 500))
    }

    @Test
    fun `non tcp packets are rejected`() {
        val udpLike = ByteArray(40)
        udpLike[0] = 0x45
        udpLike[9] = IpPacket.PROTO_UDP.toByte()
        assertNull(TcpHeader.parse(udpLike, udpLike.size))
    }

    @Test
    fun `flow key distinguishes ports`() {
        val a = TcpHeader.parse(
            TcpHeader.build(clientIp, serverIp, 1111, 443, 0, 0, TcpHeader.SYN, 0), 40
        )!!
        val b = TcpHeader.parse(
            TcpHeader.build(clientIp, serverIp, 2222, 443, 0, 0, TcpHeader.SYN, 0), 40
        )!!
        assertTrue(a.key() != b.key())
    }
}
