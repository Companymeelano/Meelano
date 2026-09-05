package com.example.vpn.xray

import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import org.json.JSONArray
import org.json.JSONObject

/**
 * Translates a [ProxyEndpoint] into an Xray JSON configuration.
 *
 * This is the whole point of adopting the real core: Xray already implements
 * Reality's X25519 handshake, xhttp, QUIC-based transports and the SS2022
 * ciphers that the hand-written Kotlin engine cannot do. Rather than reproduce
 * those protocols, we describe the connection and let the core dial it.
 *
 * The generated config uses a `tun` inbound so the core reads and writes packets
 * on the VpnService file descriptor directly, meaning the userspace TCP stack is
 * bypassed entirely when the core is in charge.
 */
object XrayConfigBuilder {

    /**
     * Whether the core can carry this node.
     *
     * Xray covers everything the Kotlin engine does plus Reality, xhttp and the
     * SS2022 ciphers. Hysteria 2 is a separate protocol with its own core and is
     * still not supported.
     */
    fun isSupported(endpoint: ProxyEndpoint): Boolean = when (endpoint.protocol) {
        Protocol.VLESS, Protocol.VMESS, Protocol.TROJAN,
        Protocol.SHADOWSOCKS, Protocol.SOCKS5 -> true
        else -> false
    }

    /** Loopback port the core exposes for statistics queries. */
    const val API_PORT = 18_085

    /**
     * Builds a complete Xray config.
     *
     * @param endpoint the node to dial.
     * @param dnsPrimary/[dnsSecondary] resolvers the core should use.
     * @param bypassLan keep RFC1918 destinations off the tunnel.
     * @param socksPort local SOCKS inbound the VpnService pipes packets into.
     */
    fun build(
        endpoint: ProxyEndpoint,
        dnsPrimary: String = "1.1.1.1",
        dnsSecondary: String = "8.8.8.8",
        bypassLan: Boolean = true,
        socksPort: Int = 10_808,
        enableLogging: Boolean = true
    ): String {
        val root = JSONObject()

        root.put(
            "log",
            JSONObject().apply {
                put("loglevel", if (enableLogging) "warning" else "none")
            }
        )

        root.put("dns", buildDns(dnsPrimary, dnsSecondary))
        root.put("inbounds", buildInbounds(socksPort))
        root.put("outbounds", buildOutbounds(endpoint))
        root.put("routing", buildRouting(bypassLan))

        // Traffic accounting, so the UI can show real byte counters.
        root.put("stats", JSONObject())
        root.put(
            "policy",
            JSONObject().put(
                "system",
                JSONObject()
                    .put("statsOutboundUplink", true)
                    .put("statsOutboundDownlink", true)
            )
        )

        return root.toString(2)
    }

    private fun buildDns(primary: String, secondary: String): JSONObject =
        JSONObject().put(
            "servers",
            JSONArray().apply {
                put(primary)
                put(secondary)
                // Domestic names resolve faster and more reliably via a local
                // resolver, and routing them abroad is a privacy leak besides.
                put(
                    JSONObject()
                        .put("address", "223.5.5.5")
                        .put("domains", JSONArray().put("geosite:cn"))
                )
            }
        )

    /**
     * A SOCKS inbound the VpnService forwards TCP/UDP into, plus the DNS
     * inbound the core answers lookups on.
     */
    private fun buildInbounds(socksPort: Int): JSONArray = JSONArray().apply {
        put(
            JSONObject()
                .put("tag", "socks-in")
                .put("protocol", "socks")
                .put("listen", "127.0.0.1")
                .put("port", socksPort)
                .put(
                    "settings",
                    JSONObject()
                        .put("auth", "noauth")
                        .put("udp", true)
                )
                .put(
                    "sniffing",
                    JSONObject()
                        .put("enabled", true)
                        .put(
                            "destOverride",
                            JSONArray().put("http").put("tls").put("quic")
                        )
                )
        )
    }

    private fun buildOutbounds(endpoint: ProxyEndpoint): JSONArray = JSONArray().apply {
        put(buildProxyOutbound(endpoint))
        put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
        put(JSONObject().put("tag", "block").put("protocol", "blackhole"))
    }

    /** The proxy outbound itself — protocol settings plus stream settings. */
    private fun buildProxyOutbound(endpoint: ProxyEndpoint): JSONObject {
        val outbound = JSONObject()
            .put("tag", "proxy")
            .put("protocol", xrayProtocolName(endpoint.protocol))

        outbound.put("settings", buildProtocolSettings(endpoint))
        outbound.put("streamSettings", buildStreamSettings(endpoint))
        outbound.put(
            "mux",
            // Multiplexing hurts more than it helps on the transports we use and
            // breaks outright under Reality, so it stays off.
            JSONObject().put("enabled", false).put("concurrency", -1)
        )
        return outbound
    }

    private fun xrayProtocolName(protocol: Protocol): String = when (protocol) {
        Protocol.VLESS -> "vless"
        Protocol.VMESS -> "vmess"
        Protocol.TROJAN -> "trojan"
        Protocol.SHADOWSOCKS -> "shadowsocks"
        Protocol.SOCKS5 -> "socks"
        // Xray has no Hysteria2 outbound; callers must gate on isSupported.
        else -> "freedom"
    }

    private fun buildProtocolSettings(endpoint: ProxyEndpoint): JSONObject = when (endpoint.protocol) {
        Protocol.VLESS -> JSONObject().put(
            "vnext",
            JSONArray().put(
                JSONObject()
                    .put("address", endpoint.host)
                    .put("port", endpoint.port)
                    .put(
                        "users",
                        JSONArray().put(
                            JSONObject()
                                .put("id", endpoint.userId)
                                .put("encryption", "none")
                                // Reality pairs with the xtls-rprx-vision flow;
                                // sending it on a plain TLS node is rejected.
                                .put(
                                    "flow",
                                    if (endpoint.security == "reality") "xtls-rprx-vision" else ""
                                )
                                .put("level", 0)
                        )
                    )
            )
        )

        Protocol.VMESS -> JSONObject().put(
            "vnext",
            JSONArray().put(
                JSONObject()
                    .put("address", endpoint.host)
                    .put("port", endpoint.port)
                    .put(
                        "users",
                        JSONArray().put(
                            JSONObject()
                                .put("id", endpoint.userId)
                                .put("alterId", 0)
                                .put("security", "auto")
                                .put("level", 0)
                        )
                    )
            )
        )

        Protocol.TROJAN -> JSONObject().put(
            "servers",
            JSONArray().put(
                JSONObject()
                    .put("address", endpoint.host)
                    .put("port", endpoint.port)
                    .put("password", endpoint.password)
                    .put("level", 0)
            )
        )

        Protocol.SHADOWSOCKS -> JSONObject().put(
            "servers",
            JSONArray().put(
                JSONObject()
                    .put("address", endpoint.host)
                    .put("port", endpoint.port)
                    .put("method", endpoint.method.ifBlank { "aes-256-gcm" })
                    .put("password", endpoint.password)
                    .put("level", 0)
            )
        )

        Protocol.SOCKS5 -> JSONObject().put(
            "servers",
            JSONArray().put(
                JSONObject()
                    .put("address", endpoint.host)
                    .put("port", endpoint.port)
                    .apply {
                        if (endpoint.userId.isNotBlank()) {
                            put(
                                "users",
                                JSONArray().put(
                                    JSONObject()
                                        .put("user", endpoint.userId)
                                        .put("pass", endpoint.password)
                                )
                            )
                        }
                    }
            )
        )

        else -> JSONObject()
    }

    /** Transport + security layer: the part Reality and xhttp actually need. */
    private fun buildStreamSettings(endpoint: ProxyEndpoint): JSONObject {
        val stream = JSONObject()
        // Recent Xray renamed the plain TCP transport to "raw" and newer feeds
        // publish it that way, so accept both spellings.
        val network = when (val n = endpoint.network.ifBlank { "tcp" }) {
            "raw" -> "tcp"
            else -> n
        }
        stream.put("network", network)

        when (endpoint.security) {
            "reality" -> {
                stream.put("security", "reality")
                stream.put(
                    "realitySettings",
                    JSONObject()
                        .put("serverName", endpoint.sni)
                        .put("fingerprint", endpoint.fingerprint.ifBlank { "chrome" })
                        .put("publicKey", endpoint.publicKey)
                        .put("shortId", endpoint.shortId)
                        .put("spiderX", "/")
                        .put("show", false)
                )
            }

            "tls" -> {
                stream.put("security", "tls")
                stream.put(
                    "tlsSettings",
                    JSONObject()
                        .put("serverName", endpoint.sni.ifBlank { endpoint.host })
                        .put("allowInsecure", endpoint.allowInsecure)
                        .put("fingerprint", endpoint.fingerprint.ifBlank { "chrome" })
                        .apply {
                            if (endpoint.alpn.isNotBlank()) {
                                put(
                                    "alpn",
                                    JSONArray().apply {
                                        endpoint.alpn.split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                            .forEach { put(it) }
                                    }
                                )
                            }
                        }
                )
            }

            else -> stream.put("security", "none")
        }

        when (network) {
            "ws" -> stream.put(
                "wsSettings",
                JSONObject()
                    .put("path", endpoint.path.ifBlank { "/" })
                    .put(
                        "headers",
                        JSONObject().put(
                            "Host",
                            // A fronted node dials an IP but must present the
                            // CDN hostname, so Host is not always the address.
                            endpoint.wsHost.ifBlank { endpoint.sni.ifBlank { endpoint.host } }
                        )
                    )
            )

            "httpupgrade" -> stream.put(
                "httpupgradeSettings",
                JSONObject()
                    .put("path", endpoint.path.ifBlank { "/" })
                    .put("host", endpoint.wsHost.ifBlank { endpoint.sni.ifBlank { endpoint.host } })
            )

            "xhttp", "splithttp" -> stream.put(
                "xhttpSettings",
                JSONObject()
                    .put("path", endpoint.path.ifBlank { "/" })
                    .put("host", endpoint.wsHost.ifBlank { endpoint.sni.ifBlank { endpoint.host } })
                    .put("mode", "auto")
            )

            "grpc", "gun" -> stream.put(
                "grpcSettings",
                JSONObject()
                    .put("serviceName", endpoint.serviceName.trim('/'))
                    .put("multiMode", endpoint.network == "gun")
            )

            "h2", "http" -> stream.put(
                "httpSettings",
                JSONObject()
                    .put("path", endpoint.path.ifBlank { "/" })
                    .put(
                        "host",
                        JSONArray().put(
                            endpoint.wsHost.ifBlank { endpoint.sni.ifBlank { endpoint.host } }
                        )
                    )
            )

        }

        return stream
    }

    private fun buildRouting(bypassLan: Boolean): JSONObject {
        val rules = JSONArray()

        if (bypassLan) {
            // Private ranges must never be tunnelled: doing so breaks local
            // network access and can black-hole the VPN's own uplink.
            rules.put(
                JSONObject()
                    .put("type", "field")
                    .put("outboundTag", "direct")
                    .put(
                        "ip",
                        JSONArray()
                            .put("geoip:private")
                            .put("127.0.0.0/8")
                            .put("10.0.0.0/8")
                            .put("172.16.0.0/12")
                            .put("192.168.0.0/16")
                    )
            )
        }

        // Everything else goes through the proxy.
        rules.put(
            JSONObject()
                .put("type", "field")
                .put("outboundTag", "proxy")
                .put("port", "0-65535")
        )

        return JSONObject()
            .put("domainStrategy", "IPIfNonMatch")
            .put("rules", rules)
    }
}
