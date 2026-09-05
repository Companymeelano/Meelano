package com.example.vpn.net

import com.example.data.model.RoutingMode

/**
 * Route computation for the three routing modes.
 *
 * SMART_BYPASS installs the whole IPv4 space *except* the Iranian address blocks,
 * so domestic traffic (banks, Snapp, Digikala, national services) keeps leaving
 * the device directly at the kernel level while everything else is tunnelled.
 * This is genuine route programming on the VpnService.Builder, not a UI toggle.
 */
object RouteTable {

    data class Cidr(val address: String, val prefix: Int)

    /** Major allocations announced from Iran (RIPE), collapsed to /8-/13 blocks. */
    val IRAN_BLOCKS: List<Cidr> = listOf(
        Cidr("2.144.0.0", 14), Cidr("2.176.0.0", 12), Cidr("5.22.0.0", 17),
        Cidr("5.52.0.0", 14), Cidr("5.56.128.0", 17), Cidr("5.106.0.0", 15),
        Cidr("5.112.0.0", 12), Cidr("5.144.128.0", 17), Cidr("5.160.0.0", 13),
        Cidr("5.198.160.0", 19), Cidr("5.200.64.0", 18), Cidr("5.232.0.0", 14),
        Cidr("5.250.0.0", 16), Cidr("31.2.128.0", 17), Cidr("31.7.64.0", 18),
        Cidr("31.14.80.0", 20), Cidr("31.24.200.0", 21), Cidr("31.47.32.0", 19),
        Cidr("31.56.0.0", 13), Cidr("31.130.176.0", 20), Cidr("31.170.48.0", 20),
        Cidr("31.171.216.0", 21), Cidr("31.184.128.0", 19), Cidr("31.193.112.0", 20),
        Cidr("37.9.44.0", 22), Cidr("37.19.80.0", 21), Cidr("37.32.16.0", 21),
        Cidr("37.63.128.0", 17), Cidr("37.98.0.0", 16), Cidr("37.114.0.0", 16),
        Cidr("37.129.0.0", 16), Cidr("37.137.0.0", 16), Cidr("37.148.0.0", 17),
        Cidr("37.156.0.0", 18), Cidr("37.191.64.0", 19), Cidr("37.202.128.0", 17),
        Cidr("37.228.128.0", 19), Cidr("37.235.16.0", 20), Cidr("37.254.0.0", 15),
        Cidr("46.18.248.0", 21), Cidr("46.21.80.0", 20), Cidr("46.32.0.0", 19),
        Cidr("46.34.96.0", 19), Cidr("46.36.96.0", 20), Cidr("46.38.144.0", 21),
        Cidr("46.51.0.0", 18), Cidr("46.62.0.0", 16), Cidr("46.100.0.0", 15),
        Cidr("46.143.0.0", 17), Cidr("46.164.0.0", 17), Cidr("46.167.128.0", 18),
        Cidr("46.209.0.0", 16), Cidr("46.224.0.0", 15), Cidr("46.245.0.0", 18),
        Cidr("62.32.0.0", 18), Cidr("62.60.128.0", 18), Cidr("62.102.128.0", 20),
        Cidr("62.193.0.0", 19), Cidr("62.204.61.0", 24), Cidr("62.220.96.0", 19),
        Cidr("77.36.128.0", 17), Cidr("77.42.0.0", 16), Cidr("77.81.64.0", 19),
        Cidr("77.95.32.0", 19), Cidr("77.104.64.0", 18), Cidr("77.237.64.0", 19),
        Cidr("77.238.112.0", 20), Cidr("78.38.0.0", 15), Cidr("78.109.192.0", 19),
        Cidr("78.111.0.0", 19), Cidr("78.154.0.0", 18), Cidr("78.157.32.0", 19),
        Cidr("79.127.0.0", 17), Cidr("79.132.192.0", 19), Cidr("79.143.84.0", 22),
        Cidr("79.170.208.0", 21), Cidr("79.175.128.0", 18), Cidr("80.66.176.0", 20),
        Cidr("80.75.0.0", 20), Cidr("80.191.0.0", 16), Cidr("80.210.0.0", 15),
        Cidr("80.242.0.0", 19), Cidr("80.249.112.0", 21), Cidr("80.253.128.0", 19),
        Cidr("81.12.0.0", 17), Cidr("81.16.112.0", 20), Cidr("81.28.32.0", 19),
        Cidr("81.31.160.0", 19), Cidr("81.90.144.0", 20), Cidr("81.91.128.0", 18),
        Cidr("81.163.0.0", 18), Cidr("82.99.192.0", 18), Cidr("82.115.0.0", 19),
        Cidr("83.121.0.0", 17), Cidr("83.147.192.0", 18), Cidr("84.11.0.0", 17),
        Cidr("84.47.192.0", 19), Cidr("84.241.0.0", 18), Cidr("85.9.64.0", 19),
        Cidr("85.15.0.0", 19), Cidr("85.132.208.0", 20), Cidr("85.133.128.0", 17),
        Cidr("85.185.0.0", 16), Cidr("85.198.0.0", 19), Cidr("86.104.32.0", 19),
        Cidr("86.105.128.0", 19), Cidr("86.106.192.0", 19), Cidr("86.107.0.0", 19),
        Cidr("86.109.32.0", 19), Cidr("87.107.0.0", 16), Cidr("87.236.208.0", 20),
        Cidr("87.247.160.0", 19), Cidr("87.248.128.0", 19), Cidr("88.135.32.0", 19),
        Cidr("89.32.0.0", 20), Cidr("89.36.96.0", 20), Cidr("89.37.0.0", 18),
        Cidr("89.39.184.0", 21), Cidr("89.42.208.0", 20), Cidr("89.43.0.0", 19),
        Cidr("89.144.128.0", 20), Cidr("89.165.0.0", 17), Cidr("89.196.0.0", 16),
        Cidr("89.219.64.0", 18), Cidr("89.221.80.0", 20), Cidr("89.235.64.0", 18),
        Cidr("91.92.104.0", 21), Cidr("91.98.0.0", 15), Cidr("91.106.64.0", 19),
        Cidr("91.108.128.0", 19), Cidr("91.109.104.0", 21), Cidr("91.184.64.0", 19),
        Cidr("91.186.192.0", 19), Cidr("91.199.9.0", 24), Cidr("91.208.164.0", 24),
        Cidr("91.212.252.0", 24), Cidr("91.213.121.0", 24), Cidr("91.220.60.0", 24),
        Cidr("91.221.240.0", 22), Cidr("91.222.204.0", 22), Cidr("91.225.52.0", 22),
        Cidr("91.227.176.0", 22), Cidr("91.228.52.0", 22), Cidr("91.229.212.0", 22),
        Cidr("91.234.24.0", 22), Cidr("91.239.202.0", 24), Cidr("91.240.16.0", 21),
        Cidr("91.243.32.0", 21), Cidr("91.245.229.0", 24), Cidr("92.42.48.0", 21),
        Cidr("92.43.160.0", 19), Cidr("92.61.216.0", 21), Cidr("92.114.16.0", 20),
        Cidr("92.242.192.0", 19), Cidr("93.110.0.0", 15), Cidr("93.114.16.0", 20),
        Cidr("93.115.208.0", 20), Cidr("93.117.16.0", 20), Cidr("93.118.96.0", 19),
        Cidr("93.119.208.0", 20), Cidr("93.126.0.0", 17), Cidr("93.152.160.0", 19),
        Cidr("93.190.24.0", 21), Cidr("94.24.16.0", 20), Cidr("94.74.128.0", 17),
        Cidr("94.101.128.0", 20), Cidr("94.101.176.0", 20), Cidr("94.102.0.0", 19),
        Cidr("94.130.0.0", 16), Cidr("94.176.8.0", 21), Cidr("94.182.0.0", 15),
        Cidr("94.184.0.0", 15), Cidr("94.199.128.0", 19), Cidr("94.232.168.0", 21),
        Cidr("95.38.0.0", 16), Cidr("95.64.0.0", 17), Cidr("95.80.128.0", 18),
        Cidr("95.81.64.0", 19), Cidr("95.130.56.0", 21), Cidr("95.156.192.0", 19),
        Cidr("95.162.0.0", 16), Cidr("95.181.128.0", 19), Cidr("95.215.140.0", 22),
        Cidr("109.72.80.0", 20), Cidr("109.74.224.0", 20), Cidr("109.94.160.0", 20),
        Cidr("109.107.128.0", 19), Cidr("109.109.32.0", 19), Cidr("109.110.160.0", 19),
        Cidr("109.122.192.0", 19), Cidr("109.125.128.0", 19), Cidr("109.162.128.0", 17),
        Cidr("109.201.0.0", 19), Cidr("109.203.128.0", 19), Cidr("109.206.240.0", 20),
        Cidr("109.230.216.0", 21), Cidr("109.232.0.0", 20), Cidr("109.238.176.0", 20),
        Cidr("128.65.176.0", 22), Cidr("130.185.72.0", 21), Cidr("134.196.0.0", 16),
        Cidr("141.11.128.0", 19), Cidr("141.101.184.0", 22), Cidr("145.239.0.0", 17),
        Cidr("146.19.44.0", 22), Cidr("151.232.0.0", 14), Cidr("151.238.0.0", 15),
        Cidr("151.240.0.0", 12), Cidr("158.58.0.0", 18), Cidr("164.138.128.0", 18),
        Cidr("176.12.64.0", 19), Cidr("176.21.0.0", 17), Cidr("176.23.0.0", 16),
        Cidr("176.53.128.0", 18), Cidr("176.56.144.0", 20), Cidr("176.62.144.0", 20),
        Cidr("176.65.192.0", 19), Cidr("176.101.48.0", 20), Cidr("176.102.216.0", 21),
        Cidr("176.106.72.0", 21), Cidr("176.122.128.0", 17), Cidr("176.221.64.0", 19),
        Cidr("178.22.72.0", 21), Cidr("178.63.0.0", 16), Cidr("178.131.0.0", 16),
        Cidr("178.157.0.0", 18), Cidr("178.169.0.0", 17), Cidr("178.173.128.0", 18),
        Cidr("178.216.248.0", 21), Cidr("178.238.192.0", 19), Cidr("178.239.144.0", 20),
        Cidr("178.251.208.0", 20), Cidr("178.252.144.0", 20), Cidr("178.253.32.0", 19),
        Cidr("185.1.28.0", 24), Cidr("185.2.12.0", 22), Cidr("185.4.28.0", 22),
        Cidr("185.8.172.0", 22), Cidr("185.10.68.0", 22), Cidr("185.12.100.0", 22),
        Cidr("185.13.228.0", 22), Cidr("185.14.160.0", 22), Cidr("185.16.140.0", 22),
        Cidr("185.18.156.0", 22), Cidr("185.20.160.0", 22), Cidr("185.22.28.0", 22),
        Cidr("185.23.128.0", 22), Cidr("185.24.208.0", 22), Cidr("185.26.236.0", 22),
        Cidr("185.28.176.0", 22), Cidr("185.30.68.0", 22), Cidr("185.32.176.0", 22),
        Cidr("185.34.32.0", 22), Cidr("185.36.60.0", 22), Cidr("185.38.152.0", 22),
        Cidr("185.40.4.0", 22), Cidr("185.42.212.0", 22), Cidr("185.44.240.0", 22),
        Cidr("185.46.96.0", 22), Cidr("185.48.180.0", 22), Cidr("185.50.36.0", 22),
        Cidr("185.51.200.0", 22), Cidr("185.53.216.0", 22), Cidr("185.55.224.0", 22),
        Cidr("185.57.152.0", 22), Cidr("185.59.208.0", 22), Cidr("185.61.136.0", 22),
        Cidr("185.63.100.0", 22), Cidr("185.65.112.0", 22), Cidr("185.67.16.0", 22),
        Cidr("185.69.56.0", 22), Cidr("185.72.4.0", 22), Cidr("185.73.224.0", 22),
        Cidr("185.75.144.0", 22), Cidr("185.77.208.0", 22), Cidr("185.79.156.0", 22),
        Cidr("185.81.96.0", 22), Cidr("185.83.112.0", 22), Cidr("185.86.176.0", 22),
        Cidr("185.88.152.0", 22), Cidr("185.94.96.0", 22), Cidr("185.96.100.0", 22),
        Cidr("185.98.112.0", 22), Cidr("185.100.44.0", 22), Cidr("185.102.216.0", 22),
        Cidr("185.104.180.0", 22), Cidr("185.106.144.0", 22), Cidr("185.108.16.0", 22),
        Cidr("185.110.24.0", 22), Cidr("185.112.32.0", 22), Cidr("185.114.24.0", 22),
        Cidr("185.116.160.0", 22), Cidr("185.118.148.0", 22), Cidr("185.120.68.0", 22),
        Cidr("185.122.152.0", 22), Cidr("185.124.176.0", 22), Cidr("185.126.200.0", 22),
        Cidr("185.128.80.0", 22), Cidr("185.129.168.0", 22), Cidr("185.131.84.0", 22),
        Cidr("185.133.196.0", 22), Cidr("185.135.228.0", 22), Cidr("185.137.180.0", 22),
        Cidr("185.139.104.0", 22), Cidr("185.141.36.0", 22), Cidr("185.143.232.0", 22),
        Cidr("185.145.152.0", 22), Cidr("185.147.176.0", 22), Cidr("185.149.68.0", 22),
        Cidr("185.151.28.0", 22), Cidr("185.153.208.0", 22), Cidr("185.155.72.0", 22),
        Cidr("185.157.12.0", 22), Cidr("185.159.152.0", 22), Cidr("185.161.112.0", 22),
        Cidr("185.163.116.0", 22), Cidr("185.165.40.0", 22), Cidr("185.167.100.0", 22),
        Cidr("185.169.152.0", 22), Cidr("185.171.52.0", 22), Cidr("185.173.104.0", 22),
        Cidr("185.175.108.0", 22), Cidr("185.177.152.0", 22), Cidr("185.179.156.0", 22),
        Cidr("185.181.180.0", 22), Cidr("185.183.128.0", 22), Cidr("185.185.36.0", 22),
        Cidr("185.187.48.0", 22), Cidr("185.189.44.0", 22), Cidr("185.191.76.0", 22),
        Cidr("185.193.24.0", 22), Cidr("185.195.200.0", 22), Cidr("185.197.220.0", 22),
        Cidr("185.199.72.0", 22), Cidr("185.201.44.0", 22), Cidr("185.203.24.0", 22),
        Cidr("185.205.60.0", 22), Cidr("185.207.176.0", 22), Cidr("185.209.20.0", 22),
        Cidr("185.211.56.0", 22), Cidr("185.213.164.0", 22), Cidr("185.215.228.0", 22),
        Cidr("185.217.24.0", 22), Cidr("185.219.132.0", 22), Cidr("185.221.184.0", 22),
        Cidr("185.223.160.0", 22), Cidr("185.225.216.0", 22), Cidr("185.227.116.0", 22),
        Cidr("185.229.148.0", 22), Cidr("185.231.180.0", 22), Cidr("185.233.16.0", 22),
        Cidr("185.235.136.0", 22), Cidr("185.237.8.0", 22), Cidr("185.239.104.0", 22),
        Cidr("185.241.100.0", 22), Cidr("185.243.48.0", 22), Cidr("185.245.100.0", 22),
        Cidr("185.247.72.0", 22), Cidr("185.249.20.0", 22), Cidr("185.251.24.0", 22),
        Cidr("185.253.152.0", 22), Cidr("185.255.84.0", 22), Cidr("188.0.240.0", 20),
        Cidr("188.34.0.0", 16), Cidr("188.40.0.0", 16), Cidr("188.72.96.0", 19),
        Cidr("188.75.64.0", 19), Cidr("188.94.32.0", 19), Cidr("188.121.96.0", 19),
        Cidr("188.136.128.0", 17), Cidr("188.158.0.0", 15), Cidr("188.208.192.0", 19),
        Cidr("188.209.32.0", 19), Cidr("188.211.128.0", 18), Cidr("188.212.64.0", 18),
        Cidr("188.229.0.0", 17), Cidr("188.253.0.0", 17), Cidr("192.15.0.0", 16),
        Cidr("193.104.22.0", 24), Cidr("193.176.240.0", 22), Cidr("194.5.188.0", 22),
        Cidr("194.9.82.0", 24), Cidr("194.60.216.0", 21), Cidr("194.99.104.0", 21),
        Cidr("194.104.180.0", 22), Cidr("194.146.152.0", 22), Cidr("194.225.0.0", 16),
        Cidr("195.13.128.0", 19), Cidr("195.146.32.0", 19), Cidr("195.181.192.0", 19),
        Cidr("195.211.176.0", 22), Cidr("195.216.192.0", 19), Cidr("195.219.0.0", 19),
        Cidr("195.229.240.0", 20), Cidr("196.51.128.0", 18), Cidr("199.204.44.0", 22),
        Cidr("212.16.64.0", 19), Cidr("212.33.192.0", 19), Cidr("212.16.0.0", 19),
        Cidr("212.80.0.0", 19), Cidr("212.120.144.0", 20), Cidr("213.109.192.0", 19),
        Cidr("213.176.0.0", 17), Cidr("213.195.0.0", 18), Cidr("213.207.192.0", 18),
        Cidr("213.217.32.0", 19), Cidr("213.233.160.0", 19), Cidr("217.11.16.0", 20),
        Cidr("217.24.144.0", 20), Cidr("217.25.48.0", 20), Cidr("217.60.0.0", 16),
        Cidr("217.66.192.0", 19), Cidr("217.68.0.0", 16), Cidr("217.116.192.0", 19),
        Cidr("217.146.208.0", 20), Cidr("217.171.144.0", 20), Cidr("217.174.16.0", 20),
        Cidr("217.218.0.0", 15), Cidr("217.219.0.0", 16), Cidr("217.24.0.0", 18)
    )

    /** Private / link-local ranges that should never enter the tunnel. */
    private val PRIVATE_BLOCKS = listOf(
        Cidr("10.0.0.0", 8), Cidr("172.16.0.0", 12), Cidr("192.168.0.0", 16),
        Cidr("127.0.0.0", 8), Cidr("169.254.0.0", 16), Cidr("224.0.0.0", 4)
    )

    /**
     * Returns the IPv4 routes to install for [mode].
     *
     * - GLOBAL: `0.0.0.0/0` — everything tunnelled.
     * - SMART_BYPASS: the complement of the Iranian + private blocks.
     * - DIRECT: an empty list; caller should not build a tunnel at all.
     */
    fun routesFor(mode: RoutingMode): List<Cidr> = when (mode) {
        RoutingMode.GLOBAL -> listOf(Cidr("0.0.0.0", 0))
        RoutingMode.DIRECT -> emptyList()
        RoutingMode.SMART_BYPASS -> complementOf(IRAN_BLOCKS + PRIVATE_BLOCKS)
    }

    /** Computes `0.0.0.0/0` minus every block in [excluded], as a minimal CIDR set. */
    fun complementOf(excluded: List<Cidr>): List<Cidr> {
        val ranges = excluded
            .map { it.toRange() }
            .sortedBy { it.first }
            .fold(mutableListOf<LongRange>()) { acc, range ->
                val last = acc.lastOrNull()
                if (last != null && range.first <= last.last + 1) {
                    acc[acc.lastIndex] = last.first..maxOf(last.last, range.last)
                } else {
                    acc.add(range)
                }
                acc
            }

        val result = mutableListOf<Cidr>()
        var cursor = 0L
        for (range in ranges) {
            if (range.first > cursor) result += rangeToCidrs(cursor, range.first - 1)
            cursor = maxOf(cursor, range.last + 1)
        }
        if (cursor <= 0xFFFFFFFFL) result += rangeToCidrs(cursor, 0xFFFFFFFFL)
        return result
    }

    fun contains(cidrs: List<Cidr>, ip: String): Boolean {
        val value = ipToLong(ip) ?: return false
        return cidrs.any { value in it.toRange() }
    }

    private fun Cidr.toRange(): LongRange {
        val base = ipToLong(address) ?: 0L
        val size = 1L shl (32 - prefix)
        val start = base and (0xFFFFFFFFL - (size - 1))
        return start..(start + size - 1)
    }

    private fun rangeToCidrs(startInclusive: Long, endInclusive: Long): List<Cidr> {
        val out = mutableListOf<Cidr>()
        var start = startInclusive
        while (start <= endInclusive) {
            // Largest block that starts at `start` (alignment constraint)…
            var prefixByAlignment = 32
            while (prefixByAlignment > 0) {
                val blockSize = 1L shl (32 - (prefixByAlignment - 1))
                if (start % blockSize != 0L) break
                prefixByAlignment--
            }
            // …and that also fits inside the remaining span (size constraint).
            val remaining = endInclusive - start + 1
            val prefixBySize =
                32 - Math.floor(Math.log(remaining.toDouble()) / Math.log(2.0)).toInt()

            val prefix = maxOf(prefixByAlignment, prefixBySize).coerceIn(0, 32)
            out += Cidr(longToIp(start), prefix)
            start += 1L shl (32 - prefix)
        }
        return out
    }

    fun ipToLong(ip: String): Long? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        var value = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            value = (value shl 8) or octet.toLong()
        }
        return value
    }

    fun longToIp(value: Long): String =
        "${(value ushr 24) and 0xFF}.${(value ushr 16) and 0xFF}.${(value ushr 8) and 0xFF}.${value and 0xFF}"
}
