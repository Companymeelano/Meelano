package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MeelanoHexagonLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    glowing: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width * 0.46f

            // Outer Hexagon Points
            val hexPath = Path()
            for (i in 0..5) {
                val angle = Math.toRadians((60 * i - 30).toDouble())
                val x = center.x + (radius * cos(angle)).toFloat()
                val y = center.y + (radius * sin(angle)).toFloat()
                if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
            }
            hexPath.close()

            // Hexagon background fill with dark metallic gradient
            drawPath(
                path = hexPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E3A68), Color(0xFF0D172B)),
                    center = center,
                    radius = radius
                ),
                style = Fill
            )

            // Glowing border
            drawPath(
                path = hexPath,
                brush = Brush.linearGradient(
                    colors = listOf(MeelanoCyan, MeelanoCyanGlow, Color(0xFF1A5288))
                ),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Inner Stylized 'M' symbol
            val mPath = Path()
            val mWidth = radius * 1.05f
            val mHeight = radius * 0.9f
            val startX = center.x - mWidth / 2f
            val endX = center.x + mWidth / 2f
            val topY = center.y - mHeight / 2f + 2f
            val bottomY = center.y + mHeight / 2f - 2f

            mPath.moveTo(startX, bottomY)
            mPath.lineTo(startX, topY)
            mPath.lineTo(center.x, center.y + 4f)
            mPath.lineTo(endX, topY)
            mPath.lineTo(endX, bottomY)

            drawPath(
                path = mPath,
                brush = Brush.verticalGradient(
                    colors = listOf(MeelanoCyan, Color(0xFF80D8FF))
                ),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
