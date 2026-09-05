package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionQuality
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.pingColor

/** Compact metric tile with an icon chip, value and caption. */
@Composable
fun StatTile(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    GlassCard(modifier = modifier, corner = 14.dp, padding = 10.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The icon sits in a spherical bead rather than a flat square:
                // a lit dome with a specular highlight and a contact shadow, so
                // the tile has a tangible object in it instead of a coloured box.
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(30.dp)) {
                        val r = size.minDimension / 2f
                        val c = androidx.compose.ui.geometry.Offset(r, r)

                        // Ambient glow, grounding the bead on the card.
                        drawCircle(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(tint.copy(alpha = 0.30f), Color.Transparent),
                                center = c,
                                radius = r * 1.55f
                            ),
                            radius = r * 1.55f,
                            center = c
                        )
                        // Body, lit from the upper-left.
                        drawCircle(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(
                                    tint.copy(alpha = 0.55f),
                                    tint.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.30f)
                                ),
                                center = androidx.compose.ui.geometry.Offset(r * 0.62f, r * 0.58f),
                                radius = r * 1.7f
                            ),
                            radius = r * 0.88f,
                            center = c
                        )
                        // Bevel.
                        drawCircle(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    tint.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                            ),
                            radius = r * 0.88f,
                            center = c,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3f)
                        )
                        // Specular highlight.
                        drawCircle(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.40f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(r * 0.66f, r * 0.55f),
                                radius = r * 0.55f
                            ),
                            radius = r * 0.55f,
                            center = androidx.compose.ui.geometry.Offset(r * 0.66f, r * 0.55f)
                        )
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(title, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (caption != null) {
                Text(caption, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Four-bar signal indicator driven by the measured latency. */
@Composable
fun SignalBars(pingMs: Int, modifier: Modifier = Modifier, barHeight: Int = 14) {
    val quality = when {
        pingMs <= 0 -> ConnectionQuality.UNKNOWN
        pingMs < 90 -> ConnectionQuality.EXCELLENT
        pingMs < 180 -> ConnectionQuality.GOOD
        pingMs < 320 -> ConnectionQuality.FAIR
        else -> ConnectionQuality.POOR
    }
    val color = pingColor(pingMs)
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((barHeight * (0.4f + 0.2f * i)).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= quality.bars) color else Color.White.copy(alpha = 0.12f))
            )
        }
    }
}

/** Circular gauge used for the connection health ring. */
@Composable
fun HealthRing(
    fraction: Float,
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "ring")
    Box(modifier = modifier.size(86.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(86.dp)) {
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 7.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * animated,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 7.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = TextMuted, fontSize = 9.sp)
        }
    }
}

/** Small pill badge. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}
