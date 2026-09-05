package com.example

import com.example.core.ConfigParser
import com.example.core.ConnectionAdvisor
import com.example.core.Protocol
import com.example.vpn.xray.XrayConfigBuilder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the protocols added on top of the Xray core, and the on-device
 * ranking that decides which node to dial.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProtocolCoverageTest {

    private fun proxyOutbound(link: String): JSONObject {
        val endpoint = ConfigParser.parse(link)
        assertNotNull("could not parse $link", endpoint)
        val config = JSONObject(XrayConfigBuilder.build(endpoint!!))
        val outbounds = config.getJSONArray("outbounds")
        for (i in 0 until outbounds.length()) {
            val item = outbounds.getJSONObject(i)
            if (item.optString("tag") == "proxy") return item
        }
        error("no proxy outbound")
    }

    @Test
    fun `hysteria2 now parses and maps onto the core's own outbound`() {
        val endpoint = ConfigParser.parse("hysteria2://secret@example.com:443?sni=a.com#H")
        assertNotNull(endpoint)
        assertEquals(Protocol.HYSTERIA2, endpoint!!.protocol)
        // Previously refused outright; the bundled core dials it natively.
        assertTrue(XrayConfigBuilder.isSupported(endpoint))

        val outbound = proxyOutbound("hysteria2://secret@example.com:443?sni=a.com#H")
        assertEquals("hysteria2", outbound.getString("protocol"))
        assertEquals(
            "secret",
            outbound.getJSONObject("settings")
                .getJSONArray("servers").getJSONObject(0).getString("password")
        )
        // Hysteria2 carries its own transport and rejects streamSettings.
        assertFalse(outbound.has("streamSettings"))
    }

    @Test
    fun `wireguard maps secret key peer and addresses`() {
        val link = "wireguard://cGJrZXk%3D@example.com:51820" +
            "?publickey=UEVFUktFWQ%3D%3D&address=172.16.0.2/32&reserved=1,2,3#W"
        val endpoint = ConfigParser.parse(link)
        assertNotNull(endpoint)
        assertEquals(Protocol.WIREGUARD, endpoint!!.protocol)
        assertTrue(XrayConfigBuilder.isSupported(endpoint))

        val settings = proxyOutbound(link).getJSONObject("settings")
        assertTrue(settings.getString("secretKey").isNotBlank())

        val peer = settings.getJSONArray("peers").getJSONObject(0)
        assertEquals("example.com:51820", peer.getString("endpoint"))
        assertTrue(peer.getString("publicKey").isNotBlank())
        assertEquals(3, peer.getJSONArray("reserved").length())

        assertEquals("172.16.0.2/32", settings.getJSONArray("address").getString(0))
        assertFalse(proxyOutbound(link).has("streamSettings"))
    }

    @Test
    fun `wireguard without an address still gets a routable default`() {
        val settings = proxyOutbound("wireguard://k@example.com:51820?publickey=p#W")
            .getJSONObject("settings")
        // An empty address list produces a tunnel that cannot route at all.
        assertTrue(settings.getJSONArray("address").length() > 0)
    }

    @Test
    fun `tuic parses but is honestly reported as unsupported`() {
        val endpoint = ConfigParser.parse("tuic://uuid:pass@example.com:443?congestion_control=bbr#T")
        assertNotNull(endpoint)
        assertEquals(Protocol.TUIC, endpoint!!.protocol)
        assertEquals("uuid", endpoint.userId)
        assertEquals("pass", endpoint.password)
        // Xray has no TUIC outbound, so claiming support would strand the user.
        assertFalse(XrayConfigBuilder.isSupported(endpoint))
    }

    @Test
    fun `advisor prefers a proven node over a fast but failing one`() {
        val advisor = ConnectionAdvisor(RuntimeEnvironment.getApplication())
        advisor.reset()

        // "fast" answers quickly but never holds; "solid" is slower and works.
        repeat(6) { advisor.record("fast", success = false, latencyMs = 20) }
        repeat(6) { advisor.record("solid", success = true, latencyMs = 320, holdSeconds = 600) }

        val ranked = advisor.rank(listOf("fast", "solid"))
        assertEquals("solid", ranked.first())
    }

    @Test
    fun `an untried node outranks one with a losing record`() {
        val advisor = ConnectionAdvisor(RuntimeEnvironment.getApplication())
        advisor.reset()
        repeat(8) { advisor.record("bad", success = false) }

        // Otherwise a new node can never earn a turn behind a failing incumbent.
        assertEquals("fresh", advisor.rank(listOf("bad", "fresh")).first())
    }

    @Test
    fun `a single lucky success does not read as perfect`() {
        val advisor = ConnectionAdvisor(RuntimeEnvironment.getApplication())
        advisor.reset()
        advisor.record("lucky", success = true, latencyMs = 10, holdSeconds = 5)

        // Laplace smoothing keeps 1/1 well below certainty.
        assertTrue(advisor.score("lucky", 1) < 1.5)
    }
}
