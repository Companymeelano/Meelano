package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Canvas ----
// Sampled directly from the app icon so the shell and the launcher artwork read
// as one object: deep indigo-navy rather than the previous neutral black.
val MeelanoBgDark = Color(0xFF0E102A)          // icon's dominant field
val MeelanoBgDarkSecondary = Color(0xFF141634)
val MeelanoBgMid = Color(0xFF212750)           // icon's secondary field
val MeelanoSurfaceCard = Color(0xFF191C3D)
val MeelanoSurfaceCardBorder = Color(0xFF40526F) // icon's chrome bevel
val MeelanoSurfaceElevated = Color(0xFF2A2F5C)
val MeelanoGlassTint = Color(0x14FFFFFF)

// ---- Accents ----
// The icon's two signature lights: the cyan neon of the "M" and the violet that
// wraps its right stroke. These now drive the default theme.
// Same hues as the icon, saturated up for emissive on-screen use.
val MeelanoIconCyan = Color(0xFF4FD8F5)
val MeelanoIconViolet = Color(0xFF9C6BFF)
val MeelanoChrome = Color(0xFF9AA3C3)

val MeelanoCyan = Color(0xFF00E5FF)
val MeelanoCyanGlow = Color(0xFF00B0FF)
val MeelanoElectricBlue = Color(0xFF2979FF)
val MeelanoGreenSuccess = Color(0xFF00E676)
val MeelanoGreenDark = Color(0xFF00B248)
val MeelanoGoldVip = Color(0xFFFFC53D)
val MeelanoGoldVipDark = Color(0xFFFF8F00)
val MeelanoRedKillSwitch = Color(0xFFFF3B5C)
val MeelanoPurpleActive = Color(0xFF7C4DFF)
val MeelanoPurpleDeep = Color(0xFF4A3B7E)
val MeelanoMagenta = Color(0xFFFF4DD8)
val MeelanoTeal = Color(0xFF1DE9B6)
val MeelanoRose = Color(0xFFFF6E9C)
val MeelanoAmber = Color(0xFFFFB74D)
val MeelanoIndigo = Color(0xFF536DFE)
val MeelanoAqua = Color(0xFF64FFDA)

// ---- Text ----
val TextPrimary = Color(0xFFF2F7FF)
val TextSecondary = Color(0xFF8FA3BF)
val TextMuted = Color(0xFF5B6E8A)

// ---- Status ----
val PingGreen = Color(0xFF00E676)
val PingYellow = Color(0xFFFFD54F)
val PingOrange = Color(0xFFFF9800)
val PingRed = Color(0xFFFF5252)

/** Accent presets the user can pick in settings. */
enum class AccentPreset(val key: String, val label: String, val primary: Color, val secondary: Color) {
    /** Matches the launcher icon exactly — cyan neon into violet. */
    SIGNATURE("signature", "امضای میلانو", MeelanoIconCyan, MeelanoIconViolet),
    CYAN("cyan", "آبی نئون", MeelanoCyan, MeelanoElectricBlue),
    EMERALD("emerald", "زمرد", MeelanoTeal, MeelanoGreenSuccess),
    VIOLET("violet", "بنفش کهکشانی", MeelanoPurpleActive, MeelanoMagenta),
    GOLD("gold", "طلایی VIP", MeelanoGoldVip, MeelanoGoldVipDark),
    SUNSET("sunset", "غروب", MeelanoRose, MeelanoAmber),
    AURORA("aurora", "شفق قطبی", MeelanoAqua, MeelanoIndigo);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: SIGNATURE
    }
}

fun pingColor(pingMs: Int): Color = when {
    pingMs <= 0 -> TextMuted
    pingMs < 90 -> PingGreen
    pingMs < 180 -> PingYellow
    pingMs < 320 -> PingOrange
    else -> PingRed
}
