package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val LocalAccent = staticCompositionLocalOf { AccentPreset.CYAN }

@Composable
fun MyApplicationTheme(
    accent: AccentPreset = AccentPreset.CYAN,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.Black,
        primaryContainer = MeelanoSurfaceElevated,
        onPrimaryContainer = accent.primary,
        secondary = accent.secondary,
        onSecondary = Color.White,
        secondaryContainer = MeelanoPurpleDeep,
        onSecondaryContainer = Color.White,
        tertiary = MeelanoGoldVip,
        background = MeelanoBgDark,
        onBackground = TextPrimary,
        surface = MeelanoSurfaceCard,
        onSurface = TextPrimary,
        surfaceVariant = MeelanoSurfaceElevated,
        onSurfaceVariant = TextSecondary,
        outline = MeelanoSurfaceCardBorder,
        error = MeelanoRedKillSwitch
    )

    CompositionLocalProvider(
        LocalAccent provides accent,
        // The whole product is Persian: lay it out right-to-left everywhere.
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
