package com.example.data.model

import com.example.core.ConfigParser
import com.example.core.PingTester

data class VpnServer(
    val id: String,
    val name: String,
    val countryName: String,
    val flagEmoji: String,
    val protocol: String, // "VMess", "VLESS", "Hysteria 2", "Reality", "Shadowsocks", "Trojan"
    val isVip: Boolean,
    val pingMs: Int,
    val speedMbps: Float,
    val configLink: String,
    val isSelected: Boolean = false,
    val isFavorite: Boolean = false,
    val dataRemainingGb: Float = 0f,
    val daysRemaining: Int = 0,
    val tunnelIp: String = "172.19.0.1",
    val encryption: String = "TLS 1.3 / AES-256",
    val packetLossPercent: Int = 0,
    val lastTestedAt: Long = 0L
) {
    val endpoint get() = ConfigParser.parse(configLink)
    val isReachable: Boolean get() = pingMs > 0
    val isUntested: Boolean get() = pingMs == 0 || lastTestedAt == 0L
    val hostLabel: String get() = endpoint?.summary() ?: "نامشخص"

    val quality: ConnectionQuality
        get() = when {
            pingMs == PingTester.UNREACHABLE -> ConnectionQuality.DEAD
            pingMs == 0 -> ConnectionQuality.UNKNOWN
            pingMs < 90 -> ConnectionQuality.EXCELLENT
            pingMs < 180 -> ConnectionQuality.GOOD
            pingMs < 320 -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
}

enum class ConnectionQuality(val label: String, val bars: Int) {
    EXCELLENT("عالی", 4),
    GOOD("خوب", 3),
    FAIR("متوسط", 2),
    POOR("ضعیف", 1),
    DEAD("در دسترس نیست", 0),
    UNKNOWN("تست نشده", 0)
}

enum class RoutingMode(val title: String, val badge: String, val description: String) {
    SMART_BYPASS(
        title = "مسیریابی هوشمند (دورزدن ایران)",
        badge = "پیش‌فرض",
        description = "ترافیک بین‌الملل از تونل عبور می‌کند و رِنج‌های IP ایران مستقیم و خارج از تونل باقی می‌مانند."
    ),
    GLOBAL(
        title = "پروکسی سراسری (Global)",
        badge = "حداکثر امنیت",
        description = "کل ترافیک دستگاه (0.0.0.0/0) از بستر رمزنگاری‌شده عبور می‌کند."
    ),
    DIRECT(
        title = "مستقیم (Direct)",
        badge = "Direct",
        description = "هیچ تونلی نصب نمی‌شود؛ اتصال کاملاً مستقیم است."
    )
}

enum class CoreProtocolFilter(val label: String) {
    ALL("همه"),
    REALITY("Reality"),
    HYSTERIA_2("Hysteria 2"),
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan"),
    SHADOWSOCKS("Shadowsocks")
}

enum class ServerSort(val label: String) {
    PING("کمترین پینگ"),
    SPEED("بیشترین سرعت"),
    NAME("نام"),
    COUNTRY("کشور")
}

data class NetworkLiveStats(
    val downloadMbps: Float = 0.0f,
    val uploadMbps: Float = 0.0f,
    val totalDownloadedMb: Float = 0f,
    val totalUploadedMb: Float = 0f,
    val pingMs: Int = 0,
    val packetLossPercent: Int = 0,
    val tunnelIp: String = "-",
    val encryption: String = "-",
    val activeProtocol: String = "-",
    val remoteHost: String = "-",
    val dnsQueries: Long = 0,
    val activeFlows: Int = 0,
    val uptimeSeconds: Int = 0,
    val speedHistory: List<Float> = List(12) { 0f }
) {
    val uptimeLabel: String
        get() {
            val h = uptimeSeconds / 3600
            val m = (uptimeSeconds % 3600) / 60
            val s = uptimeSeconds % 60
            return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
            else String.format("%02d:%02d", m, s)
        }

    val totalTransferredMb: Float get() = totalDownloadedMb + totalUploadedMb
}

data class BypassApp(
    val packageName: String,
    val appName: String,
    val category: String,
    val isBypassed: Boolean = true,
    val isSystemApp: Boolean = false
)
