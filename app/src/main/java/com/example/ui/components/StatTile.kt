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
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
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
