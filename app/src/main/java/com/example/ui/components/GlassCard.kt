package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MeelanoSurfaceCard
import com.example.ui.theme.MeelanoSurfaceCardBorder

/**
 * The signature MeeLano surface: dark glass with a soft inner sheen and a
 * gradient hairline border that can glow in the accent colour.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    accent: Color? = null,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val borderBrush = if (accent != null) {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = 0.55f),
                MeelanoSurfaceCardBorder,
                accent.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.linearGradient(listOf(MeelanoSurfaceCardBorder, Color(0xFF13203A)))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MeelanoSurfaceCard.copy(alpha = 0.82f), shape)
            .background(glassBrush(accent ?: MeelanoSurfaceCardBorder), shape)
            .border(1.dp, borderBrush, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(padding),
        content = content
    )
}
