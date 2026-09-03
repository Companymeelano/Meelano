package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkLiveStats
import com.example.ui.theme.MeelanoCyan
import com.example.ui.theme.MeelanoSurfaceCard
import com.example.ui.theme.MeelanoSurfaceCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TrafficLineChart(
    stats: NetworkLiveStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MeelanoSurfaceCard)
            .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مانیتورینگ زنده ترافیک شبکه",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MeelanoCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats.downloadMbps} Mbps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MeelanoCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val history = stats.speedHistory.ifEmpty { listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) }
            val maxSpeed = (history.maxOrNull() ?: 10f).coerceAtLeast(30f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val w = size.width
                val h = size.height

                // Draw subtle horizontal gridlines
                val gridY1 = h * 0.25f
                val gridY2 = h * 0.65f
                drawLine(Color(0xFF1B2B4C), Offset(0f, gridY1), Offset(w, gridY1), strokeWidth = 1f)
                drawLine(Color(0xFF1B2B4C), Offset(0f, gridY2), Offset(w, gridY2), strokeWidth = 1f)

                if (history.size > 1) {
                    val stepX = w / (history.size - 1)
                    val strokePath = Path()
                    val fillPath = Path()

                    val firstY = h - (history[0] / maxSpeed) * h
                    strokePath.moveTo(0f, firstY)
                    fillPath.moveTo(0f, h)
                    fillPath.lineTo(0f, firstY)

                    for (i in 1 until history.size) {
                        val prevX = (i - 1) * stepX
                        val prevY = h - (history[i - 1] / maxSpeed) * h
                        val curX = i * stepX
                        val curY = h - (history[i] / maxSpeed) * h

                        val cx = (prevX + curX) / 2f
                        strokePath.cubicTo(cx, prevY, cx, curY, curX, curY)
                        fillPath.cubicTo(cx, prevY, cx, curY, curX, curY)
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Gradient under line
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(MeelanoCyan.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )

                    // Curve stroke
                    drawPath(
                        path = strokePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0091EA), MeelanoCyan)
                        ),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
