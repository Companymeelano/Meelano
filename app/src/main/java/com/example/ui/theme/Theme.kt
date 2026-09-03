package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MeelanoColorScheme = darkColorScheme(
    primary = MeelanoCyan,
    onPrimary = Color.Black,
    primaryContainer = MeelanoSurfaceElevated,
    onPrimaryContainer = MeelanoCyan,
    secondary = MeelanoPurpleActive,
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
    outline = MeelanoSurfaceCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MeelanoColorScheme,
        typography = Typography,
        content = content
    )
}
