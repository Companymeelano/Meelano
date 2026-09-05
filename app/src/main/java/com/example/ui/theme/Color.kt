package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Canvas ----
val MeelanoBgDark = Color(0xFF05080F)
val MeelanoBgDarkSecondary = Color(0xFF080E1C)
val MeelanoBgMid = Color(0xFF0A1120)
val MeelanoSurfaceCard = Color(0xFF0E1729)
val MeelanoSurfaceCardBorder = Color(0xFF1B2B4C)
val MeelanoSurfaceElevated = Color(0xFF142442)
val MeelanoGlassTint = Color(0x14FFFFFF)

// ---- Accents ----
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
    CYAN("cyan", "آبی نئون", MeelanoCyan, MeelanoElectricBlue),
    EMERALD("emerald", "زمرد", MeelanoTeal, MeelanoGreenSuccess),
    VIOLET("violet", "بنفش کهکشانی", MeelanoPurpleActive, MeelanoMagenta),
    GOLD("gold", "طلایی VIP", MeelanoGoldVip, MeelanoGoldVipDark);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: CYAN
    }
}

fun pingColor(pingMs: Int): Color = when {
    pingMs <= 0 -> TextMuted
    pingMs < 90 -> PingGreen
    pingMs < 180 -> PingYellow
    pingMs < 320 -> PingOrange
    else -> PingRed
}
