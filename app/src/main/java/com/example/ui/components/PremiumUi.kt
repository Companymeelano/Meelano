package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

/**
 * A small library of polished, reusable visual elements shared across the app —
 * the pieces that make the interface feel like a finished product rather than a
 * collection of boxes.
 */

/** A pill badge with a soft accent wash and a matching hairline border. */
@Composable
fun AccentBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) accent else accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = if (filled) 0f else 0.45f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (filled) Color(0xFF04121F) else accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * A gradient divider that fades out at both ends — far softer than a flat rule.
 */
@Composable
fun FadingDivider(
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * A shimmering placeholder bar, used while pings and subscriptions load so the
 * UI never shows a dead empty space.
 */
@Composable
fun ShimmerBar(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    corner: Dp = 7.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmerProgress"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        startX = progress * 260f,
                        endX = progress * 260f + 260f
                    )
                )
        )
    }
}

/**
 * A slim animated progress track with a glowing leading edge — used for
 * subscription refreshes and ping sweeps.
 */
@Composable
fun GlowProgressBar(
    fraction: Float,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(secondary, accent)))
        )
    }
}

/**
 * An animated equaliser used as a "live traffic" indicator: five bars that
 * breathe while the tunnel is up and rest flat when it is down.
 */
@Composable
fun LiveEqualizer(
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 5
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "eqPhase"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        repeat(barCount) { index ->
            val wave = if (active) {
                0.35f + 0.65f * ((sin(phase + index * 0.9f) + 1f) / 2f)
            } else {
                0.22f
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16.dp * wave).coerceAtLeast(3.dp))
                    .clip(CircleShape)
                    .background(
                        if (active) accent.copy(alpha = 0.55f + 0.45f * wave)
                        else Color.White.copy(alpha = 0.18f)
                    )
            )
        }
    }
}

/**
 * A circular gauge with a glowing sweep — a far more expressive way to show a
 * score (connection quality, signal strength) than a number alone.
 */
@Composable
fun CircularGauge(
    value: Float,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    label: String = "",
    caption: String = ""
) {
    val animated by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "gauge"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.085f
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke,
                size.height - stroke
            )

            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Glow underlay
            drawArc(
                brush = Brush.sweepGradient(listOf(secondary, accent, secondary)),
                startAngle = 135f,
                sweepAngle = 270f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke * 2.1f, cap = StrokeCap.Round),
                alpha = 0.16f
            )
            // Value arc
            drawArc(
                brush = Brush.sweepGradient(listOf(secondary, accent, secondary)),
                startAngle = 135f,
                sweepAngle = 270f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (caption.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = caption, color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

/** A soft coloured halo placed behind an element to make it feel lit. */
@Composable
fun GlowDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    pulsing: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "dot")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dotPulse"
    )
    val alpha = if (pulsing) pulse else 1f
    Box(modifier = modifier.size(size * 2.4f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size * 2.4f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f * alpha))
        )
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
    }
}

/**
 * A section heading: a short accent rule, the title, and an optional trailing
 * caption. Used to break a long scroll into scannable groups instead of an
 * undifferentiated stack of cards.
 */
@Composable
fun SectionHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    caption: String = ""
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.25f))
                    )
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        if (caption.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = caption,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
