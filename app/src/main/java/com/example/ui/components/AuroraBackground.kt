package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoBgDarkSecondary
import com.example.ui.theme.MeelanoBgMid
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Living background: drifting aurora blooms, a subtle hex grid and a slow star
 * field. Everything is drawn procedurally on the GPU-backed Canvas, so it costs
 * no assets and adapts to the active accent colour and connection state.
 */
@Composable
fun AuroraBackground(
    accent: Color,
    secondary: Color,
    energised: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (energised) 9000 else 18000, easing = LinearEasing)),
        label = "phase"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "shimmer"
    )

    val stars = remember {
        val random = Random(42)
        List(70) {
            Triple(random.nextFloat(), random.nextFloat(), random.nextFloat() * 1.6f + 0.4f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // The icon's defining gesture is a diagonal sweep from navy in the
            // top-left to violet-black in the bottom-right, so the app field is
            // drawn on the same axis rather than as a vertical gradient.
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to MeelanoBgDark,
                        0.45f to MeelanoBgMid,
                        1.00f to MeelanoBgDarkSecondary
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            drawStars(stars, shimmer)
            drawBloom(
                center = Offset(size.width * (0.22f + 0.08f * cos(phase)), size.height * 0.18f),
                radius = size.minDimension * 0.75f,
                color = accent.copy(alpha = if (energised) 0.20f else 0.11f)
            )
            drawBloom(
                center = Offset(size.width * (0.82f + 0.06f * sin(phase * 0.8f)), size.height * 0.42f),
                radius = size.minDimension * 0.62f,
                color = secondary.copy(alpha = if (energised) 0.16f else 0.09f)
            )
            drawBloom(
                center = Offset(size.width * 0.5f, size.height * (0.92f + 0.03f * sin(phase))),
                radius = size.minDimension * 0.8f,
                color = accent.copy(alpha = 0.07f)
            )
            drawHexGrid(accent.copy(alpha = 0.045f))
        }
        content()
    }
}

private fun DrawScope.drawBloom(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawStars(stars: List<Triple<Float, Float, Float>>, shimmer: Float) {
    stars.forEachIndexed { index, (x, y, r) ->
        val twinkle = if (index % 3 == 0) shimmer else 1f - shimmer * 0.6f
        drawCircle(
            color = Color.White.copy(alpha = 0.10f * twinkle + 0.03f),
            radius = r,
            center = Offset(size.width * x, size.height * y)
        )
    }
}

private fun DrawScope.drawHexGrid(color: Color) {
    val radius = 46f
    val horizontal = radius * 1.5f
    val vertical = radius * 1.732f
    var row = 0
    var y = -vertical
    while (y < size.height + vertical) {
        var x = if (row % 2 == 0) 0f else horizontal * 0.5f
        while (x < size.width + horizontal) {
            drawHexagon(Offset(x, y), radius * 0.52f, color)
            x += horizontal
        }
        y += vertical * 0.5f
        row++
    }
}

private fun DrawScope.drawHexagon(center: Offset, radius: Float, color: Color) {
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0..5) {
        val angle = Math.toRadians((60.0 * i) - 30.0)
        val px = center.x + radius * cos(angle).toFloat()
        val py = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8f))
}

/** Reusable frosted-glass surface used by every card in the app. */
fun glassBrush(accent: Color): Brush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.055f),
        accent.copy(alpha = 0.02f),
        Color.White.copy(alpha = 0.02f)
    ),
    start = Offset.Zero,
    end = Offset(600f, 600f)
)

internal fun Size.minSide(): Float = minOf(width, height)
