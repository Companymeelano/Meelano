package com.example

import com.example.vpn.net.IpPacket
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

class IpPacketTest {

    private fun udpPacket(payload: ByteArray): ByteArray {
        val total = 20 + 8 + payload.size
        val buffer = ByteBuffer.allocate(total)
        buffer.put(0x45.toByte())
        buffer.put(0)
        buffer.putShort(total.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.put(64)
        buffer.put(17)                              // UDP
        buffer.putShort(0)
        buffer.put(byteArrayOf(172.toByte(), 19, 0, 2))   // src
        buffer.put(byteArrayOf(1, 1, 1, 1))               // dst
        buffer.putShort(40000.toShort())
        buffer.putShort(53)
        buffer.putShort((8 + payload.size).toShort())
        buffer.putShort(0)
        buffer.put(payload)
        return buffer.array()
    }

    @Test
    fun `detects udp protocol`() {
        val packet = udpPacket(byteArrayOf(1, 2, 3, 4))
        assertEquals(IpPacket.PROTO_UDP, IpPacket.protocolOf(packet, packet.size))
    }

    @Test
    fun `parses udp header and payload`() {
        val payload = byteArrayOf(9, 8, 7, 6, 5)
        val packet = udpPacket(payload)
        val parsed = IpPacket.parseUdp(packet, packet.size)
        assertNotNull(parsed)
        assertEquals(40000, parsed!!.sourcePort)
        assertEquals(53, parsed.destinationPort)
        assertArrayEquals(payload, parsed.payload)
        assertArrayEquals(byteArrayOf(1, 1, 1, 1), parsed.destinationIp)
    }

    @Test
    fun `builds a valid response with swapped endpoints and checksums`() {
        val request = IpPacket.parseUdp(udpPacket(byteArrayOf(1, 2)), 30)!!
        val response = IpPacket.buildUdpResponse(request, byteArrayOf(42, 43, 44))

        assertEquals(20 + 8 + 3, response.size)
        assertEquals(IpPacket.PROTO_UDP, IpPacket.protocolOf(response, response.size))

        val parsedBack = IpPacket.parseUdp(response, response.size)!!
        // src/dst swapped relative to the request
        assertArrayEquals(request.destinationIp, parsedBack.sourceIp)
        assertArrayEquals(request.sourceIp, parsedBack.destinationIp)
        assertEquals(request.destinationPort, parsedBack.sourcePort)
        assertEquals(request.sourcePort, parsedBack.destinationPort)

        // IPv4 header checksum must verify to zero over the header
        var sum = 0L
        for (i in 0 until 20 step 2) sum += IpPacket.readUShort(response, i)
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        assertEquals(0xFFFFL, sum)
    }

    @Test
    fun `rejects non ipv4 buffers`() {
        assertEquals(-1, IpPacket.protocolOf(byteArrayOf(0x60, 0, 0), 3))
        assertNull(IpPacket.parseUdp(ByteArray(10), 10))
    }
}
