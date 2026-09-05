package com.example.vpn.proto

import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import java.io.IOException
import java.net.Socket

/** Creates the right protocol client for a node, or explains why it cannot. */
object OutboundFactory {

    /** Transports the carrier layer can actually establish. */
    // "raw" is Xray's newer name for the plain TCP transport.
    private val SUPPORTED_NETWORKS = setOf("tcp", "raw", "ws", "httpupgrade", "grpc", "gun", "")

    fun supports(endpoint: ProxyEndpoint): Boolean {
        val protocolOk = when (endpoint.protocol) {
            Protocol.VLESS, Protocol.TROJAN, Protocol.SHADOWSOCKS,
            Protocol.VMESS, Protocol.SOCKS5 -> true
            // These need a QUIC or kernel-datagram stack the Kotlin engine does
            // not embed. The bundled Xray core handles Hysteria2 and WireGuard,
            // so callers check XrayConfigBuilder.isSupported before giving up.
            Protocol.HYSTERIA2, Protocol.TUIC,
            Protocol.WIREGUARD, Protocol.UNKNOWN -> false
        }
        if (!protocolOk) return false
        if (endpoint.network !in SUPPORTED_NETWORKS) return false

        // Reality is impossible for THIS engine, but not for the app: when the
        // Xray core is bundled it handles Reality natively, so callers must ask
        // XrayConfigBuilder.isSupported first. What follows only decides whether
        // the hand-written Kotlin outbounds can carry the node.
        //
        // VLESS Reality cannot be faked here.
        //
        // Reality is not "TLS with a borrowed certificate": the client must
        // perform an X25519 key exchange against the server's public key and
        // embed an authentication tag in the ClientHello. A server configured
        // for Reality inspects that tag and, when it is absent, silently
        // forwards the connection to the real site it is masquerading as.
        //
        // The result is the worst possible failure mode: the TCP connect
        // succeeds, the TLS handshake succeeds against the fronted site's real
        // certificate, so a ping test goes green — and then not one byte of
        // proxied traffic is ever carried. Declaring these unsupported is the
        // honest answer until an X25519 Reality handshake is implemented.
        if (endpoint.security == "reality") return false
        // Shadowsocks nodes are only usable if we implement their cipher.
        if (endpoint.protocol == Protocol.SHADOWSOCKS &&
            !ShadowsocksOutbound.supportsMethod(endpoint.method)
        ) {
            return false
        }
        return true
    }

    fun unsupportedReason(endpoint: ProxyEndpoint): String = when {
        endpoint.security == "reality" ->
            "VLESS Reality نیازمند تبادل کلید X25519 است و هنوز پشتیبانی نمی‌شود"

        endpoint.protocol == Protocol.TUIC ->
            "TUIC نیازمند پشتهٔ QUIC است و هسته‌های همراه این نسخه آن را ندارند"

        endpoint.protocol == Protocol.WIREGUARD ->
            "WireGuard تنها با هستهٔ Xray کار می‌کند"

        endpoint.protocol == Protocol.HYSTERIA2 ->
            "Hysteria 2 به پشتهٔ QUIC نیاز دارد و در این نسخه پشتیبانی نمی‌شود"
        endpoint.protocol == Protocol.UNKNOWN -> "پروتکل ناشناخته است"
        endpoint.network !in SUPPORTED_NETWORKS ->
            "ترنسپورت «${endpoint.network}» پشتیبانی نمی‌شود"
        endpoint.protocol == Protocol.SHADOWSOCKS &&
            !ShadowsocksOutbound.supportsMethod(endpoint.method) ->
            "رمز «${endpoint.method}» در شدوساکس پشتیبانی نمی‌شود"
        else -> "این گره پشتیبانی نمی‌شود"
    }

    @Throws(IOException::class)
    fun create(
        endpoint: ProxyEndpoint,
        destination: Destination,
        protect: (Socket) -> Boolean
    ): Outbound = when (endpoint.protocol) {
        Protocol.VLESS -> VlessOutbound(endpoint, destination, protect)
        Protocol.TROJAN -> TrojanOutbound(endpoint, destination, protect)
        Protocol.SHADOWSOCKS -> ShadowsocksOutbound(endpoint, destination, protect)
        Protocol.VMESS -> VmessOutbound(endpoint, destination, protect)
        Protocol.SOCKS5 -> Socks5Outbound(endpoint, destination, protect)
        else -> throw IOException(unsupportedReason(endpoint))
    }
}
