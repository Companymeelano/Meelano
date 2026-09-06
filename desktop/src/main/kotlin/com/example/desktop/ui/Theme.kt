package com.example.desktop.ui

import androidx.compose.ui.graphics.Color

/**
 * The MeeLano palette, sampled from the launcher artwork.
 *
 * These values are copied deliberately rather than shared: the Android theme
 * file imports `androidx.compose.ui.graphics.Color` from the Android artifact,
 * and the desktop build resolves the same class from the JetBrains artifact.
 * The hex values are the contract, and DesktopThemeTest asserts they still
 * match the phone build so the two cannot drift apart unnoticed.
 */
object MeelanoColors {
    // ---- Canvas ----
    val BgDark = Color(0xFF040A22)
    val BgDarkSecondary = Color(0xFF090418)
    val BgMid = Color(0xFF261149)
    val SurfaceCard = Color(0xFF181331)
    val SurfaceCardBorder = Color(0xFF40526F)
    val SurfaceElevated = Color(0xFF241B4E)

    // ---- Accents: the icon's two signature lights ----
    val IconCyan = Color(0xFF1FEAF7)
    val IconViolet = Color(0xFFB44BFF)
    val Chrome = Color(0xFFC9D2E6)
    val ChromeDim = Color(0xFF3D4A72)
    val GlowCyan = Color(0xFF6FD9FF)
    val GlowViolet = Color(0xFFB57BFF)

    val GreenSuccess = Color(0xFF00E676)
    val GoldVip = Color(0xFFFFC53D)
    val RedKillSwitch = Color(0xFFFF3B5C)

    // ---- Text ----
    val TextPrimary = Color(0xFFF2F7FF)
    val TextSecondary = Color(0xFF9FB2D4)
    val TextMuted = Color(0xFF6D7EA3)

    // ---- Status ----
    val PingGreen = Color(0xFF00E676)
    val PingYellow = Color(0xFFFFD54F)
    val PingOrange = Color(0xFFFF9800)
    val PingRed = Color(0xFFFF5252)

    /** Latency colour, matching the phone build's thresholds. */
    fun forPing(ms: Int): Color = when {
        ms <= 0 -> TextMuted
        ms < 120 -> PingGreen
        ms < 250 -> PingYellow
        ms < 500 -> PingOrange
        else -> PingRed
    }
}

/** Connection states the desktop shell can be in. */
enum class DesktopConnectionState(val persian: String) {
    DISCONNECTED("قطع"),
    CONNECTING("در حال اتصال"),
    TESTING("در حال آزمایش"),
    CONNECTED("متصل"),
    FAILED("ناموفق");

    val isBusy: Boolean get() = this == CONNECTING || this == TESTING
}
