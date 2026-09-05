package com.example.vpn.proto

import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import java.io.IOException
import java.net.Socket

/** Creates the right protocol client for a node, or explains why it cannot. */
object OutboundFactory {

    /** Transports the carrier layer can actually establish. */
    private val SUPPORTED_NETWORKS = setOf("tcp", "ws", "httpupgrade", "grpc", "gun", "")

    fun supports(endpoint: ProxyEndpoint): Boolean {
        val protocolOk = when (endpoint.protocol) {
            Protocol.VLESS, Protocol.TROJAN, Protocol.SHADOWSOCKS,
            Protocol.VMESS, Protocol.SOCKS5 -> true
            // Hysteria2 is QUIC/UDP based and needs a QUIC stack we do not embed.
            Protocol.HYSTERIA2, Protocol.UNKNOWN -> false
        }
        if (!protocolOk) return false
        if (endpoint.network !in SUPPORTED_NETWORKS) return false
        // Shadowsocks nodes are only usable if we implement their cipher.
        if (endpoint.protocol == Protocol.SHADOWSOCKS &&
            !ShadowsocksOutbound.supportsMethod(endpoint.method)
        ) {
            return false
        }
        return true
    }

    fun unsupportedReason(endpoint: ProxyEndpoint): String = when {
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
