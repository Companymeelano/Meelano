package com.example.core

import android.net.Uri
import android.util.Base64
import org.json.JSONObject

/**
 * Real parser for the share-link formats used by V2Ray / Xray / sing-box clients.
 *
 * Supports:
 *  - `vmess://<base64 json>`
 *  - `vless://uuid@host:port?params#remark`
 *  - `trojan://password@host:port?params#remark`
 *  - `ss://base64(method:password)@host:port#remark` and the fully base64 variant
 *  - `hy2://` / `hysteria2://password@host:port?params#remark`
 *  - `socks://` (with optional base64 userinfo)
 *
 * Everything here is pure Kotlin/`android.net.Uri`, so it is unit-testable under
 * Robolectric and used for: connecting, real TCP ping, QR generation and import.
 */
object ConfigParser {

    fun parse(link: String): ProxyEndpoint? {
        val trimmed = link.trim()
        if (trimmed.isEmpty()) return null
        val scheme = trimmed.substringBefore("://", "").lowercase()
        return try {
            when (Protocol.fromScheme(scheme)) {
                Protocol.VMESS -> parseVmess(trimmed)
                Protocol.VLESS -> parseStandard(trimmed, Protocol.VLESS)
                Protocol.TROJAN -> parseStandard(trimmed, Protocol.TROJAN)
                Protocol.HYSTERIA2 -> parseHysteria2(trimmed)
                Protocol.TUIC -> parseTuic(trimmed)
                Protocol.WIREGUARD -> parseWireGuard(trimmed)
                Protocol.SHADOWSOCKS -> parseShadowsocks(trimmed)
                Protocol.SOCKS5 -> parseStandard(trimmed, Protocol.SOCKS5)
                Protocol.UNKNOWN -> null
            }?.takeIf { it.isValid() }
        } catch (_: Exception) {
            null
        }
    }

    /** Parses a whole subscription body (plain text or base64 blob) into endpoints. */
    fun parseSubscription(body: String): List<ProxyEndpoint> {
        val decoded = if (looksLikeBase64Blob(body)) decodeBase64(body) ?: body else body
        return decoded
            .split('\n', '\r')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains("://") }
            .mapNotNull { parse(it) }
    }

    // region protocol specific

    private fun parseVmess(link: String): ProxyEndpoint? {
        val payload = link.removePrefix("vmess://").substringBefore('#')
        val json = decodeBase64(payload) ?: return null
        val obj = JSONObject(json)
        val host = obj.optString("add")
        val port = obj.optString("port").toIntOrNull() ?: obj.optInt("port")
        val net = obj.optString("net", "tcp").ifBlank { "tcp" }
        val tlsRaw = obj.optString("tls")
        val tls = when (tlsRaw.lowercase()) {
            "1", "true" -> "tls"
            "0", "false", "none", "" -> ""
            else -> tlsRaw
        }
        return ProxyEndpoint(
            protocol = Protocol.VMESS,
            host = host,
            port = port,
            remark = obj.optString("ps").ifBlank { host },
            userId = obj.optString("id"),
            security = tls,
            sni = obj.optString("sni").ifBlank { obj.optString("host") },
            wsHost = obj.optString("host"),
            alpn = obj.optString("alpn"),
            network = net,
            path = obj.optString("path"),
            serviceName = if (net == "grpc" || net == "gun") {
                obj.optString("path").ifBlank { obj.optString("serviceName") }
            } else {
                ""
            },
            raw = link
        )
    }

    private fun parseStandard(link: String, protocol: Protocol): ProxyEndpoint? {
        val uri = Uri.parse(link)
        val host = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else defaultPort(protocol)
        val userInfo = uri.userInfo.orEmpty()
        val q = { key: String -> uri.getQueryParameter(key).orEmpty() }
        val net = q("type").ifBlank { "tcp" }
        return ProxyEndpoint(
            protocol = protocol,
            host = host,
            port = port,
            remark = uri.fragment?.let { decodeComponent(it) }.orEmpty().ifBlank { host },
            userId = if (protocol == Protocol.VLESS) userInfo else "",
            password = if (protocol != Protocol.VLESS) userInfo.substringAfter(':', userInfo) else "",
            security = q("security").takeIf { it.isNotBlank() && it != "none" }.orEmpty(),
            sni = q("sni").ifBlank { q("peer") },
            wsHost = q("host"),
            alpn = decodeComponent(q("alpn")),
            network = net,
            path = decodeComponent(q("path")),
            serviceName = q("serviceName")
                .ifBlank { if (net == "grpc" || net == "gun") decodeComponent(q("path")) else "" },
            fingerprint = q("fp"),
            publicKey = q("pbk"),
            shortId = q("sid"),
            allowInsecure = q("allowInsecure") == "1" || q("insecure") == "1",
            raw = link
        )
    }

    /**
     * TUIC v5: `tuic://uuid:password@host:port?...`
     *
     * The UUID and password are both carried in the userinfo, unlike Hysteria2
     * where a lone secret is the whole credential.
     */
    private fun parseTuic(link: String): ProxyEndpoint? {
        val uri = Uri.parse(link.replaceFirst("tuic://", "https://"))
        val host = uri.host ?: return null
        val userInfo = uri.userInfo.orEmpty()
        val q = { key: String -> uri.getQueryParameter(key).orEmpty() }
        return ProxyEndpoint(
            protocol = Protocol.TUIC,
            host = host,
            port = if (uri.port > 0) uri.port else 443,
            remark = uri.fragment?.let { decodeComponent(it) }.orEmpty().ifBlank { host },
            userId = userInfo.substringBefore(':'),
            password = userInfo.substringAfter(':', ""),
            security = "tls",
            sni = q("sni"),
            alpn = decodeComponent(q("alpn")).ifBlank { "h3" },
            congestion = q("congestion_control").ifBlank { "bbr" },
            network = "quic",
            allowInsecure = q("allow_insecure") == "1" || q("insecure") == "1",
            raw = link
        )
    }

    /**
     * WireGuard: `wireguard://privateKey@host:port?publickey=...&address=...`
     *
     * There is no single official URI scheme, so accept the common spellings
     * different clients emit for the same fields.
     */
    private fun parseWireGuard(link: String): ProxyEndpoint? {
        val normalized = link.replaceFirst("wg://", "wireguard://")
        val uri = Uri.parse(normalized.replaceFirst("wireguard://", "https://"))
        val host = uri.host ?: return null
        val q = { key: String -> uri.getQueryParameter(key).orEmpty() }
        return ProxyEndpoint(
            protocol = Protocol.WIREGUARD,
            host = host,
            port = if (uri.port > 0) uri.port else 51820,
            remark = uri.fragment?.let { decodeComponent(it) }.orEmpty().ifBlank { host },
            // The client secret arrives in the userinfo on every variant seen.
            password = decodeComponent(uri.userInfo.orEmpty()),
            publicKey = decodeComponent(q("publickey").ifBlank { q("public_key") }.ifBlank { q("pbk") }),
            localAddress = decodeComponent(q("address").ifBlank { q("ip") }),
            reserved = q("reserved"),
            network = "udp",
            raw = link
        )
    }

    private fun parseHysteria2(link: String): ProxyEndpoint? {
        val normalized = link
            .replaceFirst("hysteria2://", "hy2://")
            .replaceFirst("hysteria://", "hy2://")
        val uri = Uri.parse(normalized.replaceFirst("hy2://", "https://"))
        val host = uri.host ?: return null
        val userInfo = uri.userInfo.orEmpty()
        return ProxyEndpoint(
            protocol = Protocol.HYSTERIA2,
            host = host,
            port = if (uri.port > 0) uri.port else 443,
            remark = uri.fragment?.let { decodeComponent(it) }.orEmpty().ifBlank { host },
            password = if (userInfo.contains(':')) userInfo.substringAfter(':') else userInfo,
            userId = userInfo.substringBefore(':'),
            security = "tls",
            sni = uri.getQueryParameter("sni").orEmpty(),
            network = "quic",
            allowInsecure = uri.getQueryParameter("insecure") == "1",
            raw = link
        )
    }

    private fun parseShadowsocks(link: String): ProxyEndpoint? {
        val body = link.removePrefix("ss://")
        val remark = body.substringAfter('#', "").let { decodeComponent(it) }
        val beforeFragment = body.substringBefore('#')
        val query = beforeFragment.substringAfter('?', "")
        val main = beforeFragment.substringBefore('?')

        val (userPart, hostPart) = if (main.contains('@')) {
            main.substringBeforeLast('@') to main.substringAfterLast('@')
        } else {
            // Fully base64 encoded: base64(method:password@host:port)
            val decoded = decodeBase64(main) ?: return null
            decoded.substringBeforeLast('@') to decoded.substringAfterLast('@')
        }

        // SIP002 allows the userinfo to be base64 *or* percent-encoded plaintext.
        val credentials = when {
            userPart.contains(':') -> decodeComponent(userPart)
            else -> decodeBase64(userPart) ?: decodeComponent(userPart)
        }

        // IPv6 literals arrive as [::1]:8388.
        val host: String
        val port: Int
        if (hostPart.startsWith("[")) {
            host = hostPart.substringAfter('[').substringBefore(']')
            port = hostPart.substringAfterLast(':').toIntOrNull() ?: return null
        } else {
            host = hostPart.substringBeforeLast(':')
            port = hostPart.substringAfterLast(':').toIntOrNull() ?: return null
        }

        // A v2ray-plugin in websocket mode turns this into a ws-transported node.
        val params = parseQuery(query)
        val plugin = params["plugin"].orEmpty()
        val pluginOpts = plugin.substringAfter(';', "")
        val isWebSocketPlugin = plugin.startsWith("v2ray-plugin") && pluginOpts.contains("mode=websocket") ||
            plugin.startsWith("obfs") && pluginOpts.contains("obfs=websocket")

        return ProxyEndpoint(
            protocol = Protocol.SHADOWSOCKS,
            host = host,
            port = port,
            remark = remark.ifBlank { host },
            method = credentials.substringBefore(':').trim(),
            password = credentials.substringAfter(':', ""),
            security = if (pluginOpts.contains("tls")) "tls" else "",
            sni = optionValue(pluginOpts, "host"),
            wsHost = optionValue(pluginOpts, "host"),
            network = if (isWebSocketPlugin) "ws" else "tcp",
            path = optionValue(pluginOpts, "path"),
            raw = link
        )
    }

    /** Reads `key=value;key=value` plugin options. */
    private fun optionValue(options: String, key: String): String =
        options.split(';')
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            .orEmpty()

    /** Parses a raw `a=b&c=d` query string with percent-decoding. */
    private fun parseQuery(query: String): Map<String, String> =
        query.split('&')
            .filter { it.contains('=') }
            .associate {
                decodeComponent(it.substringBefore('=')) to decodeComponent(it.substringAfter('='))
            }

    // endregion

    private fun defaultPort(protocol: Protocol) = when (protocol) {
        Protocol.SOCKS5 -> 1080
        else -> 443
    }

    private fun decodeComponent(value: String): String = try {
        Uri.decode(value)
    } catch (_: Exception) {
        value
    }

    fun decodeBase64(value: String): String? = try {
        val normalized = value.trim().replace('-', '+').replace('_', '/')
        val padded = normalized.padEnd((normalized.length + 3) / 4 * 4, '=')
        String(Base64.decode(padded, Base64.DEFAULT), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    private fun looksLikeBase64Blob(body: String): Boolean {
        val compact = body.trim().replace("\n", "").replace("\r", "")
        if (compact.length < 24) return false
        if (compact.contains("://")) return false
        return compact.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' || it == '-' || it == '_' }
    }
}
