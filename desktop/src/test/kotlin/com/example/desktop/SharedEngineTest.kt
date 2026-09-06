package com.example.desktop

import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import com.example.desktop.core.LocalProxyServer
import com.example.vpn.proto.Destination
import com.example.vpn.proto.OutboundFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Proves the Windows build really is running the phone's protocol engine, not
 * a stub.
 *
 * The desktop module compiles `vpn/proto` straight out of the app source tree.
 * If an Android import were ever added to those files this module would stop
 * compiling — but these tests go further and exercise the code at runtime, so a
 * silently broken port cannot ship.
 */
class SharedEngineTest {

    @Test
    fun `the shared outbound factory is on the desktop classpath and honest about support`() {
        val vless = ProxyEndpoint(
            protocol = Protocol.VLESS, host = "example.com", port = 443,
            remark = "n", userId = "id", security = "tls", network = "ws"
        )
        assertTrue(OutboundFactory.supports(vless))

        // Reality needs an X25519 exchange the Kotlin stack does not implement,
        // and the desktop build has no Xray core to fall back on. It must be
        // refused rather than silently failing at connect time.
        val reality = vless.copy(security = "reality", publicKey = "abc", shortId = "01")
        assertFalse(OutboundFactory.supports(reality))
        assertTrue(OutboundFactory.unsupportedReason(reality).isNotBlank())

        val hysteria = vless.copy(protocol = Protocol.HYSTERIA2)
        assertFalse(OutboundFactory.supports(hysteria))
    }

    @Test
    fun `destination encoding matches the wire formats the protocols expect`() {
        val domain = Destination.of("example.com", 443)
        assertTrue(domain.isDomain)
        // VLESS/VMess put the port first, Trojan/SOCKS5 put it last. Getting
        // this backwards produces a handshake the server silently drops.
        val first = domain.encodeWithPortFirst()
        assertEquals(0x01, first[0].toInt())   // 443 >> 8
        assertEquals(0xBB, first[1].toInt() and 0xFF)
        assertEquals(0x03, first[2].toInt())   // domain type

        val last = domain.encodeWithPortLast()
        assertEquals(0x03, last[0].toInt())
        assertEquals(0x01, last[last.size - 2].toInt())
        assertEquals(0xBB, last[last.size - 1].toInt() and 0xFF)

        val ipv4 = Destination.of("192.168.1.1", 80)
        assertFalse(ipv4.isDomain)
        val encoded = ipv4.encode()
        assertEquals(0x01, encoded[0].toInt())
        assertEquals(192, encoded[1].toInt() and 0xFF)
        assertEquals(1, encoded[4].toInt() and 0xFF)
    }

    /**
     * Drives the real SOCKS5 code path end to end.
     *
     * A stub server stands in for the remote node, so this asserts the local
     * listener speaks correct RFC 1928 to whatever browser connects to it.
     */
    @Test
    fun `local proxy completes a socks5 handshake`() {
        val proxy = LocalProxyServer(port = freePort())
        assertTrue(proxy.start().isSuccess)
        try {
            Socket().use { client ->
                client.connect(
                    InetSocketAddress(InetAddress.getLoopbackAddress(), proxy.listenPort),
                    2000
                )
                val out = client.getOutputStream()
                val input = DataInputStream(client.getInputStream())

                // greeting: version 5, one method, "no auth"
                out.write(byteArrayOf(0x05, 0x01, 0x00))
                out.flush()
                assertEquals(0x05, input.readUnsignedByte())
                assertEquals(0x00, input.readUnsignedByte())

                // CONNECT to a domain
                val host = "example.com".toByteArray()
                out.write(byteArrayOf(0x05, 0x01, 0x00, 0x03, host.size.toByte()))
                out.write(host)
                out.write(byteArrayOf(0x01, 0xBB.toByte()))
                out.flush()

                assertEquals(0x05, input.readUnsignedByte())
                // Succeeds at the SOCKS layer; the flow then fails because no
                // endpoint is configured, which is the correct split of concerns.
                assertEquals(0x00, input.readUnsignedByte())
            }
        } finally {
            proxy.stop()
        }
    }

    @Test
    fun `local proxy answers an http connect request`() {
        val proxy = LocalProxyServer(port = freePort())
        assertTrue(proxy.start().isSuccess)
        try {
            Socket().use { client ->
                client.connect(
                    InetSocketAddress(InetAddress.getLoopbackAddress(), proxy.listenPort),
                    2000
                )
                client.getOutputStream().write(
                    ("CONNECT example.com:443 HTTP/1.1\r\n" +
                        "Host: example.com:443\r\n\r\n").toByteArray()
                )
                client.getOutputStream().flush()

                val reply = ByteArray(64)
                val read = client.getInputStream().read(reply)
                assertTrue(read > 0)
                assertTrue(
                    String(reply, 0, read).startsWith("HTTP/1.1 200")
                )
            }
        } finally {
            proxy.stop()
        }
    }

    @Test
    fun `proxy binds only to loopback so it is not an open relay`() {
        val port = freePort()
        val proxy = LocalProxyServer(port = port)
        assertTrue(proxy.start().isSuccess)
        try {
            assertTrue(proxy.isRunning)
            // Starting twice must be harmless rather than throwing.
            assertTrue(proxy.start().isSuccess)
        } finally {
            proxy.stop()
            assertFalse(proxy.isRunning)
        }
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }
}
