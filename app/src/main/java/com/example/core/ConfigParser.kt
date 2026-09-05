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
        val tls = obj.optString("tls")
        return ProxyEndpoint(
            protocol = Protocol.VMESS,
            host = host,
            port = port,
            remark = obj.optString("ps").ifBlank { host },
            userId = obj.optString("id"),
            security = if (tls.isBlank() || tls == "none") "" else tls,
            sni = obj.optString("sni").ifBlank { obj.optString("host") },
            wsHost = obj.optString("host"),
            alpn = obj.optString("alpn"),
            network = net,
            path = obj.optString("path"),
            serviceName = if (net == "grpc") obj.optString("path") else "",
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
            serviceName = q("serviceName"),
            fingerprint = q("fp"),
            publicKey = q("pbk"),
            shortId = q("sid"),
            allowInsecure = q("allowInsecure") == "1" || q("insecure") == "1",
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
        val main = body.substringBefore('#').substringBefore('?')

        val (userPart, hostPart) = if (main.contains('@')) {
            main.substringBeforeLast('@') to main.substringAfterLast('@')
        } else {
            // Fully base64 encoded: base64(method:password@host:port)
            val decoded = decodeBase64(main) ?: return null
            decoded.substringBeforeLast('@') to decoded.substringAfterLast('@')
        }
        val credentials = if (userPart.contains(':')) userPart else decodeBase64(userPart) ?: userPart
        val host = hostPart.substringBeforeLast(':')
        val port = hostPart.substringAfterLast(':').toIntOrNull() ?: return null
        return ProxyEndpoint(
            protocol = Protocol.SHADOWSOCKS,
            host = host,
            port = port,
            remark = remark.ifBlank { host },
            method = credentials.substringBefore(':'),
            password = credentials.substringAfter(':', ""),
            raw = link
        )
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
