package com.example.vpn

enum class VpnConnectionState(val persName: String) {
    DISCONNECTED("قطع اتصال"),
    CONNECTING("در حال اتصال..."),
    CONNECTED("متصل شد"),
    DISCONNECTING("در حال قطع..."),
    RECONNECTING("در حال تغییر سرور هوشمند..."),
    FAILED("اتصال ناموفق");

    val isBusy: Boolean get() = this == CONNECTING || this == DISCONNECTING || this == RECONNECTING
    val isActive: Boolean get() = this == CONNECTED
}
