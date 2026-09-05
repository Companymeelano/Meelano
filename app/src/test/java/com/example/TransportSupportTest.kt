package com.example

import com.example.core.ConfigParser
import com.example.core.Protocol
import com.example.vpn.proto.OutboundFactory
import com.example.vpn.proto.ShadowsocksOutbound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the share-link shapes real users actually paste, and the honesty of
 * [OutboundFactory.supports] — a node we cannot carry must be rejected up front
 * rather than failing halfway through a handshake.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransportSupportTest {

    // ---- transports --------------------------------------------------------

    @Test
    fun `grpc vless nodes are parsed and supported`() {
        val link = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@example.com:443" +
            "?encryption=none&security=tls&type=grpc&serviceName=mygrpc&sni=example.com#GrpcNode"
        val endpoint = ConfigParser.parse(link)
        assertNotNull(endpoint)
        assertEquals("grpc", endpoint!!.network)
        assertEquals("mygrpc", endpoint.serviceName)
        assertTrue(OutboundFactory.supports(endpoint))
    }

    @Test
    fun `grpc service name may arrive in the path parameter`() {
        val link = "vless://uuid-here@example.com:443?type=grpc&path=%2Ffallback&security=tls"
        val endpoint = ConfigParser.parse(link)
        assertNotNull(endpoint)
        assertEquals("/fallback", endpoint!!.serviceName)
    }

    @Test
    fun `httpupgrade nodes are parsed and supported`() {
        val link = "vless://uuid-here@example.com:443" +
            "?type=httpupgrade&security=tls&host=cdn.example.com&path=%2Fhu"
        val endpoint = ConfigParser.parse(link)
        assertNotNull(endpoint)
        assertEquals("httpupgrade", endpoint!!.network)
        assertEquals("/hu", endpoint.path)
        assertEquals("cdn.example.com", endpoint.effectiveHost)
        assertTrue(OutboundFactory.supports(endpoint))
    }

    @Test
    fun `quic and kcp transports are rejected with a clear reason`() {
        listOf("quic", "kcp", "xhttp").forEach { transport ->
            val endpoint = ConfigParser.parse(
                "vless://uuid-here@example.com:443?type=$transport&security=tls"
            )
            assertNotNull(endpoint)
            assertFalse(
                "$transport should not be claimed as supported",
                OutboundFactory.supports(endpoint!!)
            )
            assertTrue(OutboundFactory.unsupportedReason(endpoint).contains(transport))
        }
    }

    // ---- host / sni resolution --------------------------------------------

    @Test
    fun `sni and host are resolved independently for cdn nodes`() {
        val link = "vless://uuid-here@203.0.113.9:443" +
            "?security=tls&sni=front.example.com&host=front.example.com&type=ws&path=%2Fp"
        val endpoint = ConfigParser.parse(link)!!
        assertEquals("203.0.113.9", endpoint.host)
        assertEquals("front.example.com", endpoint.effectiveSni)
        assertEquals("front.example.com", endpoint.effectiveHost)
    }

    @Test
    fun `host falls back to sni and then to the dialled address`() {
        val bare = ConfigParser.parse("vless://uuid-here@example.com:443?type=ws")!!
        assertEquals("example.com", bare.effectiveSni)
        assertEquals("example.com", bare.effectiveHost)
    }

    // ---- vmess variants ----------------------------------------------------

    @Test
    fun `vmess accepts numeric tls flags`() {
        val json = """{"add":"example.com","port":"443","id":"uuid-here","net":"ws","tls":"1","path":"/x"}"""
        val encoded = android.util.Base64.encodeToString(
            json.toByteArray(), android.util.Base64.NO_WRAP
        )
        val endpoint = ConfigParser.parse("vmess://$encoded")
        assertNotNull(endpoint)
        assertEquals("tls", endpoint!!.security)
    }

    @Test
    fun `vmess without tls reports no security`() {
        val json = """{"add":"example.com","port":"80","id":"uuid-here","net":"tcp","tls":"none"}"""
        val encoded = android.util.Base64.encodeToString(
            json.toByteArray(), android.util.Base64.NO_WRAP
        )
        val endpoint = ConfigParser.parse("vmess://$encoded")!!
        assertEquals("", endpoint.security)
    }

    // ---- shadowsocks -------------------------------------------------------

    @Test
    fun `shadowsocks sip002 plaintext userinfo is parsed`() {
        val link = "ss://aes-256-gcm:secretpass@example.com:8388#Node"
        val endpoint = ConfigParser.parse(link)
        assertNotNull(endpoint)
        assertEquals("aes-256-gcm", endpoint!!.method)
        assertEquals("secretpass", endpoint.password)
        assertEquals(8388, endpoint.port)
    }

    @Test
    fun `shadowsocks ipv6 literals keep their address`() {
        val link = "ss://aes-128-gcm:pw@[2001:db8::1]:8388#v6"
        val endpoint = ConfigParser.parse(link)
        assertNotNull(endpoint)
        assertEquals("2001:db8::1", endpoint!!.host)
        assertEquals(8388, endpoint.port)
    }

    @Test
    fun `shadowsocks 2022 ciphers are rejected rather than silently failing`() {
        assertFalse(ShadowsocksOutbound.supportsMethod("2022-blake3-aes-256-gcm"))
        assertTrue(ShadowsocksOutbound.supportsMethod("aes-256-gcm"))
        assertTrue(ShadowsocksOutbound.supportsMethod("chacha20-ietf-poly1305"))
        // A blank method means "use the default", which we do implement.
        assertTrue(ShadowsocksOutbound.supportsMethod(""))
    }

    @Test
    fun `unsupported shadowsocks cipher makes the node unsupported`() {
        val link = "ss://2022-blake3-aes-256-gcm:pw@example.com:8388#New"
        val endpoint = ConfigParser.parse(link)!!
        assertEquals(Protocol.SHADOWSOCKS, endpoint.protocol)
        assertFalse(OutboundFactory.supports(endpoint))
    }

    // ---- subscription parsing ---------------------------------------------

    @Test
    fun `subscription bodies parse mixed protocols and skip junk`() {
        val body = """
            vless://uuid-here@a.example.com:443?type=ws&security=tls
            # a comment line that is not a link
            trojan://password@b.example.com:443?sni=b.example.com
            not-a-link-at-all
            ss://aes-256-gcm:pw@c.example.com:8388
        """.trimIndent()

        val endpoints = ConfigParser.parseSubscription(body)
        assertEquals(3, endpoints.size)
        assertTrue(endpoints.all { it.isValid() })
    }
}
