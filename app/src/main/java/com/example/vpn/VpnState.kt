package com.example.vpn

enum class VpnConnectionState(val persName: String) {
    DISCONNECTED("قطع اتصال"),
    CONNECTING("در حال اتصال..."),
    CONNECTED("متصل شد"),
    DISCONNECTING("در حال قطع..."),
    RECONNECTING("در حال تغییر سرور هوشمند...")
}
