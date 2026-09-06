package com.example.desktop.core

import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import java.net.URI
import java.net.URLDecoder
import java.util.Base64

/**
 * Subscription-link parser for the desktop build.
 *
 * The Android parser cannot be shared: it is built on `android.net.Uri` and
 * `org.json`, neither of which exists on a plain JVM. This is the same grammar
 * re-expressed over `java.net.URI`, and it produces the identical
 * [ProxyEndpoint] the shared outbounds consume, so the engine below it is
 * genuinely the same code as the phone.
 *
 * `java.net.URI` is stricter than Android's parser, which matters in practice:
 * real-world subscription links routinely carry unencoded characters in the
 * fragment. Everything therefore goes through a tolerant manual split rather
 * than trusting `URI` with the whole string.
 */
object DesktopConfigParser {

    /** Parses a whole subscription body, plain or base64, into endpoints. */
    fun parseSubscription(body: String): List<ProxyEndpoint> {
        val decoded = if (looksLikeBase64Blob(body)) decodeBase64(body) ?: body else body
        return decoded
            .split('\n', '\r')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains("://") }
            .mapNotNull { runCatching { parse(it) }.getOrNull() }
    }

    fun parse(link: String): ProxyEndpoint? {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vmess://") -> parseVmess(trimmed)
            trimmed.startsWith("vless://") -> parseStandard(trimmed, Protocol.VLESS)
            trimmed.startsWith("trojan://") -> parseStandard(trimmed, Protocol.TROJAN)
            trimmed.startsWith("ss://") -> parseShadowsocks(trimmed)
            trimmed.startsWith("socks://") || trimmed.startsWith("socks5://") ->
                parseStandard(trimmed, Protocol.SOCKS5)
            else -> null
        }?.takeIf { it.isValid() }
    }

    // region protocol specific

    /**
     * `vmess://` carries a base64 JSON blob rather than a URI.
     *
     * Parsed with a small hand-rolled reader instead of a JSON library: it is a
     * flat object of string/number fields, and this keeps the desktop build
     * free of a dependency added for one call site.
     */
    private fun parseVmess(link: String): ProxyEndpoint? {
        val payload = link.removePrefix("vmess://").substringBefore('#')
        val json = decodeBase64(payload) ?: return null
        val f = flatJsonFields(json)

        val host = f["add"].orEmpty()
        val port = f["port"]?.toIntOrNull() ?: 443
        val net = f["net"].orEmpty().ifBlank { "tcp" }
        val tls = when (f["tls"].orEmpty().lowercase()) {
            "1", "true", "tls" -> "tls"
            else -> ""
        }
        if (host.isBlank()) return null

        return ProxyEndpoint(
            protocol = Protocol.VMESS,
            host = host,
            port = port,
            remark = f["ps"].orEmpty().ifBlank { host },
            userId = f["id"].orEmpty(),
            security = tls,
            sni = f["sni"].orEmpty(),
            network = net,
            path = f["path"].orEmpty(),
            serviceName = if (net == "grpc") f["path"].orEmpty() else "",
            wsHost = f["host"].orEmpty(),
            alpn = f["alpn"].orEmpty(),
            raw = link
        )
    }

    private fun parseStandard(link: String, protocol: Protocol): ProxyEndpoint? {
        val parts = splitLink(link) ?: return null
        val q = parts.query
        val net = q("type").ifBlank { "tcp" }

        return ProxyEndpoint(
            protocol = protocol,
            host = parts.host,
            port = if (parts.port > 0) parts.port else defaultPort(protocol),
            remark = parts.fragment.ifBlank { parts.host },
            userId = if (protocol == Protocol.VLESS) parts.userInfo else "",
            password = if (protocol != Protocol.VLESS) {
                parts.userInfo.substringAfter(':', parts.userInfo)
            } else "",
            security = q("security").takeIf { it.isNotBlank() && it != "none" }.orEmpty(),
            sni = q("sni").ifBlank { q("peer") },
            wsHost = q("host"),
            alpn = q("alpn"),
            network = net,
            path = q("path"),
            serviceName = q("serviceName")
                .ifBlank { if (net == "grpc" || net == "gun") q("path") else "" },
            fingerprint = q("fp"),
            publicKey = q("pbk"),
            shortId = q("sid"),
            allowInsecure = q("allowInsecure") == "1" || q("insecure") == "1",
            raw = link
        )
    }

    /**
     * Two shapes in the wild: `ss://base64(method:pass)@host:port#tag` and the
     * fully base64-encoded `ss://base64(method:pass@host:port)#tag`.
     */
    private fun parseShadowsocks(link: String): ProxyEndpoint? {
        val withoutScheme = link.removePrefix("ss://")
        val fragment = withoutScheme.substringAfter('#', "").let { decodeComponent(it) }
        val core = withoutScheme.substringBefore('#').substringBefore('?')

        val (credentials, address) = if (core.contains('@')) {
            val raw = core.substringBeforeLast('@')
            (decodeBase64(raw) ?: raw) to core.substringAfterLast('@')
        } else {
            val decoded = decodeBase64(core) ?: return null
            decoded.substringBeforeLast('@') to decoded.substringAfterLast('@')
        }

        val host = address.substringBeforeLast(':')
        val port = address.substringAfterLast(':').toIntOrNull() ?: return null
        if (host.isBlank()) return null

        return ProxyEndpoint(
            protocol = Protocol.SHADOWSOCKS,
            host = host,
            port = port,
            remark = fragment.ifBlank { host },
            password = credentials.substringAfter(':', ""),
            method = credentials.substringBefore(':', ""),
            raw = link
        )
    }

    // endregion

    // region helpers

    private class LinkParts(
        val userInfo: String,
        val host: String,
        val port: Int,
        val fragment: String,
        private val params: Map<String, String>
    ) {
        val query: (String) -> String = { key -> params[key].orEmpty() }
    }

    /**
     * Splits `scheme://userinfo@host:port?query#fragment` tolerantly.
     *
     * Deliberately not `URI.create`: subscription feeds carry unencoded spaces
     * and non-ASCII in the fragment often enough that a strict parse would
     * discard otherwise-valid nodes.
     */
    private fun splitLink(link: String): LinkParts? {
        val afterScheme = link.substringAfter("://", "").ifBlank { return null }
        val fragment = decodeComponent(afterScheme.substringAfter('#', ""))
        val beforeFragment = afterScheme.substringBefore('#')
        val queryString = beforeFragment.substringAfter('?', "")
        val authority = beforeFragment.substringBefore('?')

        val userInfo = if (authority.contains('@')) {
            decodeComponent(authority.substringBeforeLast('@'))
        } else ""
        val hostPort = authority.substringAfterLast('@')

        // IPv6 literals are bracketed, so the last colon is not the port
        // separator unless it falls outside the brackets.
        val host: String
        val port: Int
        if (hostPort.startsWith("[")) {
            host = hostPort.substringAfter('[').substringBefore(']')
            port = hostPort.substringAfterLast("]:", "").toIntOrNull() ?: -1
        } else {
            host = hostPort.substringBefore(':')
            port = hostPort.substringAfter(':', "").toIntOrNull() ?: -1
        }
        if (host.isBlank()) return null

        val params = queryString.split('&')
            .filter { it.contains('=') }
            .associate {
                decodeComponent(it.substringBefore('=')) to decodeComponent(it.substringAfter('='))
            }

        return LinkParts(userInfo, host, port, fragment, params)
    }

    /** Reads the flat string/number fields of a small JSON object. */
    private fun flatJsonFields(json: String): Map<String, String> {
        val out = HashMap<String, String>()
        val regex = Regex("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+))")
        regex.findAll(json).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].ifBlank { match.groupValues[3] }
            out[key] = value
        }
        return out
    }

    private fun defaultPort(protocol: Protocol): Int = when (protocol) {
        Protocol.SOCKS5 -> 1080
        else -> 443
    }

    internal fun decodeComponent(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    internal fun decodeBase64(value: String): String? = runCatching {
        val normalised = value.trim()
            .replace('-', '+')
            .replace('_', '/')
            .replace("\n", "")
            .replace("\r", "")
        val padded = normalised.padEnd(
            normalised.length + (4 - normalised.length % 4) % 4,
            '='
        )
        String(Base64.getDecoder().decode(padded), Charsets.UTF_8)
    }.getOrNull()

    internal fun looksLikeBase64Blob(body: String): Boolean {
        val compact = body.trim().replace("\n", "").replace("\r", "")
        if (compact.length < 24) return false
        if (compact.contains("://")) return false
        return compact.all {
            it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' || it == '-' || it == '_'
        }
    }

    // endregion
}
