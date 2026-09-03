package com.example.data.model

data class VpnServer(
    val id: String,
    val name: String,
    val countryName: String,
    val flagEmoji: String,
    val protocol: String, // "VMess", "VLESS", "Hysteria 2", "Reality", "Shadowsocks"
    val isVip: Boolean,
    var pingMs: Int,
    var speedMbps: Float,
    val configLink: String,
    var isSelected: Boolean = false,
    val dataRemainingGb: Float = 18.08f,
    val daysRemaining: Int = 29,
    val tunnelIp: String = "172.19.0.1",
    val encryption: String = "TLS 1.3 / AES-256",
    val packetLossPercent: Int = 0
)

enum class RoutingMode(val title: String, val badge: String, val description: String) {
    SMART_BYPASS(
        title = "مسیریابی هوشمند (دورزدن ایران)",
        badge = "پیش‌فرض",
        description = "عبور ترافیک فیلترشده از تونل و بازشدن مستقیم سایت‌ها و بانک‌های داخلی."
    ),
    GLOBAL(
        title = "پروکسی سراسری (Global)",
        badge = "حداکثر امنیت",
        description = "عبور تمام ترافیک دستگاه از بستر رمزنگاری‌شده."
    ),
    DIRECT(
        title = "مستقیم (Direct)",
        badge = "Direct",
        description = "اتصال عادی بدون فیلترشکن، مناسب دانلودهای حجیم داخلی."
    )
}

enum class CoreProtocolFilter(val label: String) {
    ALL("همه"),
    REALITY("Reality"),
    HYSTERIA_2("Hysteria 2"),
    VLESS("VLESS"),
    VMESS("VMess")
}

data class NetworkLiveStats(
    val downloadMbps: Float = 0.0f,
    val uploadMbps: Float = 0.0f,
    val totalDownloadedMb: Float = 143.0f,
    val totalUploadedMb: Float = 28.0f,
    val pingMs: Int = 48,
    val packetLossPercent: Int = 0,
    val tunnelIp: String = "172.19.0.1",
    val encryption: String = "TLS 1.3 / AES-256",
    val activeProtocol: String = "VMess",
    val speedHistory: List<Float> = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
)

data class BypassApp(
    val packageName: String,
    val appName: String,
    val category: String,
    var isBypassed: Boolean = true
)
