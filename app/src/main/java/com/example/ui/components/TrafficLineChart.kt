package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkLiveStats
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Live throughput graph. The series is the *measured* per-second download rate
 * reported by the tunnel's byte counters, drawn as a smoothed spline with a
 * gradient fill, a moving peak marker and a grid annotated in Mb/s.
 */
@Composable
fun TrafficLineChart(
    stats: NetworkLiveStats,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val series = stats.speedHistory.takeLast(24)
    val peak = (series.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val animatedPeak by animateFloatAsState(peak, tween(600), label = "peak")

    GlassCard(modifier = modifier.fillMaxWidth(), accent = accent, padding = 14.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("نمودار ترافیک زنده", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("اندازه‌گیری واقعی از رابط TUN", color = TextMuted, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${stats.downloadMbps} Mb/s", color = accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("اوج: ${"%.1f".format(peak)}", color = TextSecondary, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val w = size.width
                    val h = size.height

                    // grid
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
                    for (i in 0..4) {
                        val y = h * i / 4f
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = dash
                        )
                    }

                    if (series.size < 2) return@Canvas

                    val stepX = w / (series.size - 1).toFloat()
                    fun pointAt(index: Int): Offset {
                        val value = series[index].coerceAtLeast(0f)
                        return Offset(stepX * index, h - (value / animatedPeak).coerceIn(0f, 1f) * (h * 0.88f))
                    }

                    val line = Path().apply {
                        moveTo(pointAt(0).x, pointAt(0).y)
                        for (i in 0 until series.size - 1) {
                            val current = pointAt(i)
                            val next = pointAt(i + 1)
                            val midX = (current.x + next.x) / 2f
                            cubicTo(midX, current.y, midX, next.y, next.x, next.y)
                        }
                    }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    // Volumetric fill: several stacked stops rather than one
                    // fade, so the body of the graph has depth instead of
                    // dissolving straight to nothing.
                    drawPath(
                        path = area,
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to accent.copy(alpha = 0.48f),
                                0.35f to accent.copy(alpha = 0.22f),
                                0.75f to accent.copy(alpha = 0.06f),
                                1.00f to Color.Transparent
                            )
                        )
                    )

                    // The trace is drawn three times — a wide soft bloom, the
                    // body, then a fine white core — the same neon-tube
                    // construction used by the logo and the connect orb.
                    val neon = Brush.horizontalGradient(listOf(accent, MeelanoGreenSuccess))
                    drawPath(
                        path = line,
                        brush = neon,
                        alpha = 0.20f,
                        style = Stroke(width = 8f.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = line,
                        brush = neon,
                        style = Stroke(width = 2.6f.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = line,
                        color = Color.White.copy(alpha = 0.55f),
                        style = Stroke(width = 0.9f.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Leading marker, built as a lit bead so it sits above the
                    // trace rather than on it.
                    val last = pointAt(series.size - 1)
                    drawCircle(accent.copy(alpha = 0.22f), radius = 15f, center = last)
                    drawCircle(accent.copy(alpha = 0.38f), radius = 8f, center = last)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White, accent),
                            center = Offset(last.x - 1.4f, last.y - 1.4f),
                            radius = 7f
                        ),
                        radius = 4.6f,
                        center = last
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendDot("دانلود ${stats.downloadMbps} Mb/s", accent)
                LegendDot("آپلود ${stats.uploadMbps} Mb/s", MeelanoGreenSuccess)
                Text("مدت: ${stats.uptimeLabel}", color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.padding(end = 4.dp).size(8.dp)) {
            drawCircle(color, radius = size.minDimension / 2f)
        }
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}
