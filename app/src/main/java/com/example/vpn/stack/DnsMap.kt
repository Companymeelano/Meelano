package com.example.vpn.stack

import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which hostname produced which IP address, learned by observing the
 * DNS answers that flow through the tunnel.
 *
 * This matters because the TUN device only ever shows us destination *IPs*.
 * Proxy protocols work far better when they are handed the original **domain**:
 * the exit node then resolves it itself, which defeats DNS poisoning and makes
 * CDN-hosted sites (Instagram, WhatsApp, Telegram) land on a working edge.
 */
object DnsMap {

    private const val MAX_ENTRIES = 4096

    private val ipToHost = ConcurrentHashMap<String, String>()

    fun remember(host: String, ip: String) {
        if (host.isBlank() || ip.isBlank()) return
        if (ipToHost.size > MAX_ENTRIES) ipToHost.clear()
        ipToHost[ip] = host.trimEnd('.').lowercase()
    }

    /** Returns the hostname last seen for [ip], or null when unknown. */
    fun hostFor(ip: String): String? = ipToHost[ip]

    fun clear() = ipToHost.clear()

    val size: Int get() = ipToHost.size
}
