package com.example

import com.example.core.ConfigParser
import com.example.core.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConfigParserTest {

    @Test
    fun `parses vmess base64 payload`() {
        val link = "vmess://eyJhZGQiOiJkZS5tZWVsYW5vLnBybyIsInBvcnQiOjQ0MywiaWQiOiI3N2ExZjIwMC02YjAwLTQ1MDctYTRjMy02ZTI1OGE4YzU5NzQiLCJhaWQiOjAsInNjeSI6ImF1dG8iLCJuZXQiOiJ3cyIsInRscyI6InRscyIsInBhdGgiOiIvbWVlbGFubyIsInBzIjoiTWVlTGFubyBWSVAgRnJhbmtmdXJ0In0="
        val endpoint = ConfigParser.parse(link)
        assertNotNull(endpoint)
        assertEquals(Protocol.VMESS, endpoint!!.protocol)
        assertEquals("de.meelano.pro", endpoint.host)
        assertEquals(443, endpoint.port)
        assertEquals("ws", endpoint.network)
        assertEquals("/meelano", endpoint.path)
        assertEquals("tls", endpoint.security)
    }

    @Test
    fun `parses vless reality with query params`() {
        val link = "vless://96b1e600-4b31-482a-a92c-567a123bcdef@de2.meelano.pro:8443" +
            "?encryption=none&security=reality&sni=www.yahoo.com&fp=chrome&pbk=KEY&sid=6ba7b810" +
            "&type=grpc&serviceName=meelano-grpc#Node%20One"
        val endpoint = ConfigParser.parse(link)!!
        assertEquals(Protocol.VLESS, endpoint.protocol)
        assertEquals("de2.meelano.pro", endpoint.host)
        assertEquals(8443, endpoint.port)
        assertEquals("reality", endpoint.security)
        assertEquals("www.yahoo.com", endpoint.sni)
        assertEquals("grpc", endpoint.network)
        assertEquals("meelano-grpc", endpoint.serviceName)
        assertEquals("Node One", endpoint.remark)
        assertEquals("Reality", endpoint.displayProtocol)
    }

    @Test
    fun `parses trojan and hysteria2`() {
        val trojan = ConfigParser.parse("trojan://pass123@se.example.com:443?security=tls&sni=apple.com#SE")!!
        assertEquals(Protocol.TROJAN, trojan.protocol)
        assertEquals("se.example.com", trojan.host)

        val hy2 = ConfigParser.parse("hy2://user:secret@fi.example.com:2096?sni=speedtest.net#FI")!!
        assertEquals(Protocol.HYSTERIA2, hy2.protocol)
        assertEquals(2096, hy2.port)
        assertEquals("secret", hy2.password)
        assertTrue(hy2.isUdpBased)
    }

    @Test
    fun `parses shadowsocks both encodings`() {
        val plain = ConfigParser.parse("ss://YWVzLTI1Ni1nY206cGFzc3dvcmQ=@nl.example.com:8388#NL")!!
        assertEquals("aes-256-gcm", plain.method)
        assertEquals("password", plain.password)
        assertEquals("nl.example.com", plain.host)
        assertEquals(8388, plain.port)
    }

    @Test
    fun `rejects garbage`() {
        assertNull(ConfigParser.parse("not-a-link"))
        assertNull(ConfigParser.parse(""))
        assertNull(ConfigParser.parse("vless://@:0"))
    }

    @Test
    fun `parses multi line subscription`() {
        val body = """
            vless://uuid@a.example.com:443#A
            trojan://pw@b.example.com:443#B
            garbage line
            hy2://pw@c.example.com:443#C
        """.trimIndent()
        val list = ConfigParser.parseSubscription(body)
        assertEquals(3, list.size)
        assertEquals(listOf("a.example.com", "b.example.com", "c.example.com"), list.map { it.host })
    }
}
