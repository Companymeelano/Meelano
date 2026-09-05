package com.example

import com.example.core.ConfigParser
import com.example.vpn.xray.XrayConfigBuilder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The generated Xray config is the entire contract with the core: a wrong field
 * name does not throw, it just silently fails to connect. These tests pin the
 * shape of what we emit.
 *
 * Runs under Robolectric because ConfigParser is built on android.net.Uri,
 * which returns null on a plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class XrayConfigBuilderTest {

    private fun configFor(link: String): JSONObject {
        val endpoint = ConfigParser.parse(link)
        assertNotNull("could not parse $link", endpoint)
        return JSONObject(XrayConfigBuilder.build(endpoint!!))
    }

    private fun proxyOutbound(config: JSONObject): JSONObject {
        val outbounds = config.getJSONArray("outbounds")
        for (i in 0 until outbounds.length()) {
            val item = outbounds.getJSONObject(i)
            if (item.optString("tag") == "proxy") return item
        }
        error("no proxy outbound emitted")
    }

    @Test
    fun `reality node emits realitySettings with the public key and short id`() {
        val config = configFor(
            "vless://8d0f2761-c586-4822-8238-757ff717fdf8@example.com:443" +
                "?encryption=none&security=reality&type=tcp&sni=www.microsoft.com" +
                "&pbk=xJhH7Kq2LmNoPqRsTuVwXyZ0123456789abcdefghi&sid=a1b2c3d4&fp=chrome#R"
        )
        val stream = proxyOutbound(config).getJSONObject("streamSettings")

        // This is the whole reason the core was adopted.
        assertEquals("reality", stream.getString("security"))
        val reality = stream.getJSONObject("realitySettings")
        assertEquals("www.microsoft.com", reality.getString("serverName"))
        assertEquals("chrome", reality.getString("fingerprint"))
        assertEquals("a1b2c3d4", reality.getString("shortId"))
        assertTrue(reality.getString("publicKey").isNotBlank())

        // Reality requires the vision flow; without it the server rejects us.
        val user = proxyOutbound(config)
            .getJSONObject("settings")
            .getJSONArray("vnext").getJSONObject(0)
            .getJSONArray("users").getJSONObject(0)
        assertEquals("xtls-rprx-vision", user.getString("flow"))
    }

    @Test
    fun `plain tls vless does not request the vision flow`() {
        val config = configFor(
            "vless://8d0f2761-c586-4822-8238-757ff717fdf8@example.com:443" +
                "?encryption=none&security=tls&type=ws&path=/ws&sni=cdn.example.com#T"
        )
        val outbound = proxyOutbound(config)
        val user = outbound.getJSONObject("settings")
            .getJSONArray("vnext").getJSONObject(0)
            .getJSONArray("users").getJSONObject(0)

        // Sending the flow on a non-Reality node breaks the handshake.
        assertEquals("", user.getString("flow"))

        val stream = outbound.getJSONObject("streamSettings")
        assertEquals("tls", stream.getString("security"))
        assertEquals("ws", stream.getString("network"))
        assertEquals("/ws", stream.getJSONObject("wsSettings").getString("path"))
    }

    @Test
    fun `fronted node sends the cdn hostname in the ws Host header`() {
        // Dials a bare IP but must present the CDN name, or the edge 404s.
        val config = configFor(
            "vless://8d0f2761-c586-4822-8238-757ff717fdf8@1.2.3.4:443" +
                "?encryption=none&security=tls&type=ws&path=/p" +
                "&sni=cdn.asset.aparat.com&host=cdn.asset.aparat.com#F"
        )
        val ws = proxyOutbound(config)
            .getJSONObject("streamSettings")
            .getJSONObject("wsSettings")

        assertEquals(
            "cdn.asset.aparat.com",
            ws.getJSONObject("headers").getString("Host")
        )
    }

    @Test
    fun `trojan and shadowsocks map onto their own settings shapes`() {
        val trojan = proxyOutbound(configFor("trojan://secret@example.com:443?security=tls#TJ"))
        assertEquals("trojan", trojan.getString("protocol"))
        assertEquals(
            "secret",
            trojan.getJSONObject("settings")
                .getJSONArray("servers").getJSONObject(0).getString("password")
        )

        val ss = proxyOutbound(
            configFor("ss://YWVzLTI1Ni1nY206cGFzcw@example.com:8388#SS")
        )
        assertEquals("shadowsocks", ss.getString("protocol"))
        val server = ss.getJSONObject("settings")
            .getJSONArray("servers").getJSONObject(0)
        assertEquals("aes-256-gcm", server.getString("method"))
        assertEquals("pass", server.getString("password"))
    }

    @Test
    fun `private ranges are routed direct so the lan and uplink survive`() {
        val config = configFor("vless://u@example.com:443?encryption=none&type=tcp#D")
        val rules = config.getJSONObject("routing").getJSONArray("rules")

        var sawPrivateDirect = false
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            if (rule.optString("outboundTag") == "direct") {
                // Read the entries rather than the serialised array: JSONObject
                // escapes forward slashes, so "192.168.0.0/16" appears as
                // "192.168.0.0\/16" in toString() and a substring match fails.
                val array = rule.optJSONArray("ip")
                val ips = buildList {
                    for (j in 0 until (array?.length() ?: 0)) add(array!!.getString(j))
                }
                // Explicit CIDRs, not geoip:private — the geo databases were
                // dropped to keep 27 MB out of the APK.
                if (ips.contains("192.168.0.0/16") && ips.contains("10.0.0.0/8")) {
                    sawPrivateDirect = true
                }
            }
        }
        assertTrue("private ranges must bypass the tunnel", sawPrivateDirect)
    }

    @Test
    fun `hysteria2 is carried by the core's own outbound`() {
        // Superseded: this asserted the opposite before the hysteria outbound
        // was wired up. Xray's proxy/hysteria has client.go, so it really dials.
        val endpoint = ConfigParser.parse("hysteria2://pw@example.com:443#H2")
        assertNotNull(endpoint)
        assertTrue(XrayConfigBuilder.isSupported(endpoint!!))
    }

    @Test
    fun `tuic is refused because xray has no such outbound`() {
        val endpoint = ConfigParser.parse("tuic://id:pw@example.com:443#T")
        assertNotNull(endpoint)
        assertFalse(XrayConfigBuilder.isSupported(endpoint!!))
    }

    @Test
    fun `config always carries the three standard outbounds`() {
        val config = configFor("vless://u@example.com:443?encryption=none&type=tcp#X")
        val tags = mutableListOf<String>()
        val outbounds = config.getJSONArray("outbounds")
        for (i in 0 until outbounds.length()) {
            tags.add(outbounds.getJSONObject(i).getString("tag"))
        }
        assertTrue(tags.containsAll(listOf("proxy", "direct", "block")))
        // proxy must be first: Xray treats outbounds[0] as the default route.
        assertEquals("proxy", tags.first())
    }
}
