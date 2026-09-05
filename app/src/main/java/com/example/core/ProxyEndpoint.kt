package com.example.core

/**
 * A fully parsed, machine-usable representation of a proxy configuration link.
 *
 * This is what turns a `vmess://` / `vless://` / `trojan://` / `ss://` / `hy2://`
 * string into something the app can actually connect to, ping and validate.
 */
data class ProxyEndpoint(
    val protocol: Protocol,
    val host: String,
    val port: Int,
    val remark: String,
    val userId: String = "",
    val password: String = "",
    val security: String = "",      // tls / reality / none
    val sni: String = "",
    val network: String = "tcp",    // tcp / ws / grpc / quic
    val path: String = "",
    val serviceName: String = "",
    val fingerprint: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val method: String = "",        // shadowsocks cipher
    val allowInsecure: Boolean = false,
    val raw: String = ""
) {
    val isUdpBased: Boolean get() = protocol == Protocol.HYSTERIA2 || network == "quic"

    val displayProtocol: String
        get() = when {
            protocol == Protocol.VLESS && security == "reality" -> "Reality"
            else -> protocol.label
        }

    /** Human readable, e.g. `de.meelano.pro:443 · VLESS/Reality (ws)`. */
    fun summary(): String = buildString {
        append(host).append(':').append(port)
        append(" · ").append(displayProtocol)
        if (network.isNotBlank() && network != "tcp") append(" (").append(network).append(')')
    }

    fun isValid(): Boolean = host.isNotBlank() && port in 1..65535
}

enum class Protocol(val label: String, val scheme: String) {
    VMESS("VMess", "vmess"),
    VLESS("VLESS", "vless"),
    TROJAN("Trojan", "trojan"),
    SHADOWSOCKS("Shadowsocks", "ss"),
    HYSTERIA2("Hysteria 2", "hy2"),
    SOCKS5("SOCKS5", "socks"),
    UNKNOWN("Unknown", "");

    companion object {
        fun fromScheme(scheme: String): Protocol = when (scheme.lowercase()) {
            "vmess" -> VMESS
            "vless" -> VLESS
            "trojan", "trojan-go" -> TROJAN
            "ss", "shadowsocks" -> SHADOWSOCKS
            "hy2", "hysteria2", "hysteria" -> HYSTERIA2
            "socks", "socks5" -> SOCKS5
            else -> UNKNOWN
        }

        fun fromLabel(label: String): Protocol =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
                ?: when (label.lowercase()) {
                    "reality" -> VLESS
                    else -> UNKNOWN
                }
    }
}
