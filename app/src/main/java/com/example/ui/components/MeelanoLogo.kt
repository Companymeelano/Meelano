package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MeelanoCyan
import com.example.ui.theme.MeelanoCyanGlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * The MeeLano mark: a machined hexagon shield with a beveled "M", an orbiting
 * light sweep when the tunnel is live, and a soft outer bloom.
 */
@Composable
fun MeelanoHexagonLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    glowing: Boolean = true,
    accent: Color = MeelanoCyan
) {
    val transition = rememberInfiniteTransition(label = "logo")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (glowing) 3200 else 9000, easing = LinearEasing)),
        label = "sweep"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width * 0.46f

            if (glowing) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = radius * 1.7f
                    ),
                    radius = radius * 1.7f,
                    center = center
                )
            }

            val hexPath = Path()
            for (i in 0..5) {
                val angle = Math.toRadians((60 * i - 30).toDouble())
                val x = center.x + (radius * cos(angle)).toFloat()
                val y = center.y + (radius * sin(angle)).toFloat()
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()

            drawPath(
                path = hexPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1E3A68), Color(0xFF0A1120)),
                    start = Offset(center.x - radius, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius)
                ),
                style = Fill
            )

            rotate(sweep, center) {
                drawPath(
                    path = hexPath,
                    brush = Brush.sweepGradient(
                        listOf(accent, MeelanoCyanGlow, Color(0xFF13345C), accent),
                        center
                    ),
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            val mPath = Path()
            val mWidth = radius * 1.02f
            val mHeight = radius * 0.88f
            val startX = center.x - mWidth / 2f
            val endX = center.x + mWidth / 2f
            val topY = center.y - mHeight / 2f + 2f
            val bottomY = center.y + mHeight / 2f - 2f

            mPath.moveTo(startX, bottomY)
            mPath.lineTo(startX, topY)
            mPath.lineTo(center.x, center.y + radius * 0.16f)
            mPath.lineTo(endX, topY)
            mPath.lineTo(endX, bottomY)

            drawPath(
                path = mPath,
                brush = Brush.verticalGradient(listOf(accent, Color(0xFFB3ECFF))),
                style = Stroke(
                    width = this.size.width * 0.085f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
