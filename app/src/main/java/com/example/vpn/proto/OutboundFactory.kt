package com.example.vpn.proto

import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import java.io.IOException
import java.net.Socket

/** Creates the right protocol client for a node, or explains why it cannot. */
object OutboundFactory {

    fun supports(endpoint: ProxyEndpoint): Boolean = when (endpoint.protocol) {
        Protocol.VLESS, Protocol.TROJAN, Protocol.SHADOWSOCKS,
        Protocol.VMESS, Protocol.SOCKS5 -> true
        // Hysteria2 is QUIC/UDP based and needs a QUIC stack we do not embed.
        Protocol.HYSTERIA2, Protocol.UNKNOWN -> false
    }

    fun unsupportedReason(endpoint: ProxyEndpoint): String = when (endpoint.protocol) {
        Protocol.HYSTERIA2 -> "Hysteria 2 به پشتهٔ QUIC نیاز دارد و در این نسخه پشتیبانی نمی‌شود"
        else -> "پروتکل ناشناخته است"
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
