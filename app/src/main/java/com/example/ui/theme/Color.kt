package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Canvas ----
// Sampled directly from the app icon so the shell and the launcher artwork read
// as one object: deep indigo-navy rather than the previous neutral black.
val MeelanoBgDark = Color(0xFF040A22)          // icon top-left cosmos
val MeelanoBgDarkSecondary = Color(0xFF090418)  // icon bottom-right cosmos
val MeelanoBgMid = Color(0xFF261149)           // icon violet nebula
val MeelanoSurfaceCard = Color(0xFF181331)
val MeelanoSurfaceCardBorder = Color(0xFF40526F) // icon's chrome bevel
val MeelanoSurfaceElevated = Color(0xFF241B4E)
val MeelanoGlassTint = Color(0x14FFFFFF)

// ---- Accents ----
// The icon's two signature lights: the cyan neon of the "M" and the violet that
// wraps its right stroke. These now drive the default theme.
// Same hues as the icon, saturated up for emissive on-screen use.
val MeelanoIconCyan = Color(0xFF1FEAF7)        // neon M, cyan stroke
val MeelanoIconViolet = Color(0xFFB44BFF)      // neon M, magenta stroke
val MeelanoChrome = Color(0xFFC9D2E6)          // shield chrome highlight
val MeelanoChromeDim = Color(0xFF3D4A72)       // shield chrome shadow
val MeelanoIconGlowCyan = Color(0xFF6FD9FF)    // halo around the cyan stroke
val MeelanoIconGlowViolet = Color(0xFFB57BFF)  // halo around the violet stroke

val MeelanoCyan = Color(0xFF00E5FF)
val MeelanoCyanGlow = Color(0xFF00B0FF)
val MeelanoElectricBlue = Color(0xFF2979FF)
val MeelanoGreenSuccess = Color(0xFF00E676)
val MeelanoGreenDark = Color(0xFF00B248)
val MeelanoGoldVip = Color(0xFFFFC53D)
val MeelanoGoldVipDark = Color(0xFFFF8F00)
val MeelanoRedKillSwitch = Color(0xFFFF3B5C)
val MeelanoPurpleActive = Color(0xFF8B5CFF)
val MeelanoPurpleDeep = Color(0xFF4A3B7E)
val MeelanoMagenta = Color(0xFFFF3DD4)
val MeelanoTeal = Color(0xFF00F5C4)
val MeelanoRose = Color(0xFFFF5C8A)
val MeelanoAmber = Color(0xFFFFA62B)
val MeelanoIndigo = Color(0xFF6C5CFF)
val MeelanoAqua = Color(0xFF5CFFE1)

// ---- Text ----
val TextPrimary = Color(0xFFF2F7FF)
val TextSecondary = Color(0xFF9FB2D4)
val TextMuted = Color(0xFF6D7EA3)

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
    AURORA("aurora", "شفق قطبی", MeelanoAqua, MeelanoIndigo),
    PLASMA("plasma", "پلاسما", Color(0xFFFF4D6D), Color(0xFF7B2FFF)),
    ICE("ice", "یخ قطبی", Color(0xFF7FE3FF), Color(0xFFB8C6FF));

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
