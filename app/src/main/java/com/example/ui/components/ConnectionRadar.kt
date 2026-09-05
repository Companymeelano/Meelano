package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Secure route" visualiser: your device on the left, the exit node on the
 * right, and packets travelling along an arc between them. When the tunnel is
 * down the arc is dashed and grey; when it is up, energised packets flow at a
 * rate proportional to the *measured* throughput.
 */
@Composable
fun ConnectionRadar(
    connected: Boolean,
    accent: Color,
    throughputMbps: Float,
    originLabelDrawn: Boolean = true,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val flow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((2600 - (throughputMbps.coerceIn(0f, 60f) * 30)).toInt().coerceAtLeast(700), easing = LinearEasing)
        ),
        label = "flow"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "sweep"
    )

    val noise = remember { List(14) { Random(it).nextFloat() } }

    Box(modifier = modifier.fillMaxWidth().height(120.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val left = Offset(size.width * 0.13f, size.height * 0.62f)
            val right = Offset(size.width * 0.87f, size.height * 0.62f)
            val control = Offset(size.width * 0.5f, size.height * 0.04f)
            val color = if (connected) accent else Color(0xFF41567A)

            // radar sweep around the origin node
            if (connected) {
                for (r in listOf(0.35f, 0.62f, 0.9f)) {
                    drawCircle(
                        color = color.copy(alpha = 0.10f),
                        radius = size.height * r * 0.4f,
                        center = left,
                        style = Stroke(width = 1f)
                    )
                }
                val rad = Math.toRadians(sweep.toDouble())
                drawLine(
                    brush = Brush.linearGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent)),
                    start = left,
                    end = Offset(
                        left.x + (size.height * 0.36f * cos(rad)).toFloat(),
                        left.y + (size.height * 0.36f * sin(rad)).toFloat()
                    ),
                    strokeWidth = 2f
                )
            }

            val path = Path().apply {
                moveTo(left.x, left.y)
                quadraticBezierTo(control.x, control.y, right.x, right.y)
            }
            drawPath(
                path = path,
                color = color.copy(alpha = if (connected) 0.55f else 0.28f),
                style = Stroke(
                    width = 2f,
                    pathEffect = if (connected) null else PathEffect.dashPathEffect(floatArrayOf(10f, 12f)),
                    cap = StrokeCap.Round
                )
            )

            // travelling packets along the quadratic curve
            if (connected) {
                noise.forEachIndexed { index, jitter ->
                    val t = ((flow + index / noise.size.toFloat() + jitter * 0.03f) % 1f)
                    val inv = 1 - t
                    val x = inv * inv * left.x + 2 * inv * t * control.x + t * t * right.x
                    val y = inv * inv * left.y + 2 * inv * t * control.y + t * t * right.y
                    val alpha = (1f - kotlin.math.abs(t - 0.5f) * 1.4f).coerceIn(0.15f, 1f)
                    drawCircle(color.copy(alpha = alpha * 0.5f), radius = 7f, center = Offset(x, y))
                    drawCircle(color.copy(alpha = alpha), radius = 3f, center = Offset(x, y))
                }
            }

            // endpoints
            drawEndpoint(left, color, filled = true)
            drawEndpoint(right, if (connected) color else Color(0xFF41567A), filled = connected)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEndpoint(
    center: Offset,
    color: Color,
    filled: Boolean
) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), center, 34f),
        radius = 34f,
        center = center
    )
    drawCircle(Color(0xFF0B1526), radius = 15f, center = center)
    drawCircle(color, radius = 15f, center = center, style = Stroke(width = 2f))
    if (filled) drawCircle(color, radius = 6f, center = center)
}
