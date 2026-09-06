package com.example.desktop

import com.example.core.Protocol
import com.example.desktop.core.DesktopConfigParser
import com.example.desktop.core.DesktopVipServers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The desktop parser must produce exactly what the shared outbounds expect.
 *
 * It is a separate implementation from the Android one (java.net.URI rather
 * than android.net.Uri), so these tests are what stop the two from disagreeing
 * about the same link.
 */
class DesktopParserTest {

    @Test
    fun `parses the bundled VIP fleet`() {
        // Every shipped node must parse, or the Windows build starts with an
        // empty server list and no way to connect.
        assertEquals(24, DesktopVipServers.all.size)
        DesktopVipServers.all.forEach { node ->
            val endpoint = DesktopConfigParser.parse(node.link)
            assertNotNull("failed to parse ${node.name}", endpoint)
            assertTrue("invalid endpoint for ${node.name}", endpoint!!.isValid())
            assertEquals(Protocol.VLESS, endpoint.protocol)
            assertEquals(443, endpoint.port)
            assertEquals("ws", endpoint.network)
            assertEquals("tls", endpoint.security)
        }
    }

    @Test
    fun `vless link keeps the fronted SNI and host separate from the dial address`() {
        // Domain fronting depends on this: dial the IP, but present a borrowed
        // name in SNI and Host. Collapsing them would break the whole technique.
        val link = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443" +
            "?security=tls&sni=cdn.asset.aparat.com&host=cdn.asset.aparat.com" +
            "&type=ws&path=%2Fvl%2Ft%2FtUtK%2Fgfdr&encryption=none&fp=chrome#Test"

        val endpoint = DesktopConfigParser.parse(link)!!
        assertEquals("178.239.157.239", endpoint.host)
        assertEquals("cdn.asset.aparat.com", endpoint.sni)
        assertEquals("cdn.asset.aparat.com", endpoint.wsHost)
        assertEquals("cdn.asset.aparat.com", endpoint.effectiveSni)
        // The path must come back percent-decoded or the upgrade request 404s.
        assertEquals("/vl/t/tUtK/gfdr", endpoint.path)
        assertEquals("8d0f2761-c586-4822-8238-757ff717fdf8", endpoint.userId)
    }

    @Test
    fun `parses a trojan link`() {
        val endpoint = DesktopConfigParser.parse(
            "trojan://secretpass@example.com:8443?security=tls&sni=example.com&type=tcp#Node"
        )!!
        assertEquals(Protocol.TROJAN, endpoint.protocol)
        assertEquals("example.com", endpoint.host)
        assertEquals(8443, endpoint.port)
        assertEquals("secretpass", endpoint.password)
    }

    @Test
    fun `parses a shadowsocks link in both encodings`() {
        // userinfo base64, address in the clear
        val a = DesktopConfigParser.parse(
            "ss://YWVzLTI1Ni1nY206cGFzc3dvcmQ=@1.2.3.4:8388#SS"
        )!!
        assertEquals(Protocol.SHADOWSOCKS, a.protocol)
        assertEquals("1.2.3.4", a.host)
        assertEquals(8388, a.port)
        assertEquals("aes-256-gcm", a.method)
        assertEquals("password", a.password)

        // whole thing base64
        val whole = java.util.Base64.getEncoder()
            .encodeToString("aes-256-gcm:password@1.2.3.4:8388".toByteArray())
        val b = DesktopConfigParser.parse("ss://$whole#SS")!!
        assertEquals("1.2.3.4", b.host)
        assertEquals(8388, b.port)
        assertEquals("aes-256-gcm", b.method)
    }

    @Test
    fun `parses a vmess base64 json link`() {
        val json = """{"v":"2","ps":"Node A","add":"example.org","port":"443",
            "id":"b831381d-6324-4d53-ad4f-8cda48b30811","net":"ws","type":"none",
            "host":"cdn.example.org","path":"/ws","tls":"tls"}"""
        val link = "vmess://" + java.util.Base64.getEncoder()
            .encodeToString(json.toByteArray())

        val endpoint = DesktopConfigParser.parse(link)!!
        assertEquals(Protocol.VMESS, endpoint.protocol)
        assertEquals("example.org", endpoint.host)
        assertEquals(443, endpoint.port)
        assertEquals("b831381d-6324-4d53-ad4f-8cda48b30811", endpoint.userId)
        assertEquals("ws", endpoint.network)
        assertEquals("/ws", endpoint.path)
        assertEquals("cdn.example.org", endpoint.wsHost)
        assertEquals("tls", endpoint.security)
        assertEquals("Node A", endpoint.remark)
    }

    @Test
    fun `handles an ipv6 literal without mistaking a colon for the port`() {
        val endpoint = DesktopConfigParser.parse(
            "trojan://pass@[2001:db8::1]:443?security=tls#v6"
        )!!
        assertEquals("2001:db8::1", endpoint.host)
        assertEquals(443, endpoint.port)
    }

    @Test
    fun `decodes a base64 subscription blob`() {
        val body = listOf(
            "trojan://a@one.example:443#One",
            "trojan://b@two.example:443#Two"
        ).joinToString("\n")
        val blob = java.util.Base64.getEncoder().encodeToString(body.toByteArray())

        assertTrue(DesktopConfigParser.looksLikeBase64Blob(blob))
        val parsed = DesktopConfigParser.parseSubscription(blob)
        assertEquals(2, parsed.size)
        assertEquals("one.example", parsed[0].host)
    }

    @Test
    fun `plain text subscriptions parse without base64`() {
        val body = """
            trojan://a@one.example:443#One
            not a link
            vless://8d0f2761-c586-4822-8238-757ff717fdf8@two.example:443?type=ws&security=tls#Two
        """.trimIndent()
        val parsed = DesktopConfigParser.parseSubscription(body)
        assertEquals(2, parsed.size)
    }

    @Test
    fun `a malformed link is refused rather than half-parsed`() {
        assertNull(DesktopConfigParser.parse("vless://"))
        assertNull(DesktopConfigParser.parse("nonsense"))
        assertNull(DesktopConfigParser.parse("ftp://host:21"))
        // A whole feed of junk must not throw.
        assertTrue(DesktopConfigParser.parseSubscription("!!!\n???").isEmpty())
    }
}
