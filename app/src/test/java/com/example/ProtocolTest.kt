package com.example

import com.example.vpn.proto.Destination
import com.example.vpn.proto.ShadowsocksOutbound
import com.example.vpn.stack.DnsMap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins down the protocol primitives. A single wrong byte in an address encoding
 * or a key derivation makes an entire protocol fail to connect, so the known
 * answers are asserted here rather than discovered on a user's phone.
 */
class ProtocolTest {

    // ---- address encoding --------------------------------------------------

    @Test
    fun `domain destinations encode as type 3 with a length prefix`() {
        val destination = Destination.of("instagram.com", 443)
        assertTrue(destination.isDomain)

        val encoded = destination.encodeWithPortLast()
        assertEquals(0x03, encoded[0].toInt())               // ATYP = domain
        assertEquals(13, encoded[1].toInt())                 // "instagram.com".length
        assertEquals("instagram.com", String(encoded, 2, 13, Charsets.US_ASCII))
        // Port 443 big endian in the final two bytes.
        assertEquals(0x01, encoded[encoded.size - 2].toInt())
        assertEquals(0xBB, encoded[encoded.size - 1].toInt() and 0xFF)
    }

    @Test
    fun `ipv4 destinations encode as type 1`() {
        val destination = Destination.of("8.8.4.4", 53)
        assertTrue(!destination.isDomain)

        val encoded = destination.encodeWithPortLast()
        assertArrayEquals(
            byteArrayOf(0x01, 8, 8, 4, 4, 0x00, 53),
            encoded
        )
    }

    @Test
    fun `port first encoding puts the port before the address`() {
        val encoded = Destination.of("1.2.3.4", 8080).encodeWithPortFirst()
        assertEquals(0x1F, encoded[0].toInt())               // 8080 >> 8
        assertEquals(0x90, encoded[1].toInt() and 0xFF)      // 8080 & 0xFF
        assertEquals(0x01, encoded[2].toInt())               // ATYP = IPv4
    }

    // ---- shadowsocks key derivation ---------------------------------------

    @Test
    fun `evp bytes to key matches the reference implementation`() {
        // Known answer: MD5("password") = 5f4dcc3b5aa765d61d8327deb882cf99
        val key = ShadowsocksOutbound.evpBytesToKey("password", 16)
        assertEquals(
            "5f4dcc3b5aa765d61d8327deb882cf99",
            key.joinToString("") { "%02x".format(it) }
        )
    }

    @Test
    fun `evp bytes to key extends correctly beyond one md5 block`() {
        val key = ShadowsocksOutbound.evpBytesToKey("password", 32)
        assertEquals(32, key.size)
        // The first 16 bytes must still be plain MD5(password).
        assertEquals(
            "5f4dcc3b5aa765d61d8327deb882cf99",
            key.copyOf(16).joinToString("") { "%02x".format(it) }
        )
    }

    @Test
    fun `hkdf produces the requested length deterministically`() {
        val key = ByteArray(32) { it.toByte() }
        val salt = ByteArray(32) { (it * 3).toByte() }
        val info = "ss-subkey".toByteArray()

        val a = ShadowsocksOutbound.hkdfSha1(key, salt, info, 32)
        val b = ShadowsocksOutbound.hkdfSha1(key, salt, info, 32)
        assertEquals(32, a.size)
        assertArrayEquals(a, b)

        // A different salt must produce a different subkey.
        val other = ShadowsocksOutbound.hkdfSha1(key, ByteArray(32), info, 32)
        assertTrue(!a.contentEquals(other))
    }

    @Test
    fun `aead nonce increments little endian with carry`() {
        val nonce = ByteArray(12)
        ShadowsocksOutbound.increment(nonce)
        assertEquals(1, nonce[0].toInt())

        // 0xFF must carry into the next byte.
        nonce[0] = 0xFF.toByte()
        ShadowsocksOutbound.increment(nonce)
        assertEquals(0, nonce[0].toInt())
        assertEquals(1, nonce[1].toInt())
    }

    // ---- dns learning ------------------------------------------------------

    @Before
    fun resetDnsMap() = DnsMap.clear()

    @Test
    fun `dns map recalls the hostname for a learned ip`() {
        DnsMap.remember("instagram.com", "157.240.1.35")
        assertEquals("instagram.com", DnsMap.hostFor("157.240.1.35"))
        assertNull(DnsMap.hostFor("1.1.1.1"))
    }

    @Test
    fun `dns map normalises trailing dots and case`() {
        DnsMap.remember("WWW.Google.COM.", "142.250.185.78")
        assertEquals("www.google.com", DnsMap.hostFor("142.250.185.78"))
    }

    @Test
    fun `dns map ignores blank input`() {
        DnsMap.remember("", "9.9.9.9")
        DnsMap.remember("example.com", "")
        assertNull(DnsMap.hostFor("9.9.9.9"))
        assertEquals(0, DnsMap.size)
    }
}
