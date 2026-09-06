package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextSecondary
import com.example.util.SoundEngine
import kotlin.math.cos
import kotlin.math.sin

/**
 * The "finding the best route" experience.
 *
 * A radar sweep over concentric range rings, with node blips that light up as
 * the beam passes them — turning an otherwise blank waiting period into the most
 * memorable animation in the app.
 */
@Composable
fun ServerScanOverlay(
    visible: Boolean,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    title: String = "در حال یافتن بهترین مسیر",
    caption: String = "تست هم‌زمان تمام نودها…",
    progress: Float? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(280), initialScale = 0.9f),
        exit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.94f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF061029).copy(alpha = 0.93f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // A real perspective-projected globe rather than a flat spinner,
                // and it answers touch: tapping spins it up, flares the nodes and
                // sends a shockwave out from the finger. Waiting on a scan now has
                // something to do instead of a frozen graphic.
                HoloGlobeLoader(
                    accent = accent,
                    secondary = secondary,
                    onTap = { SoundEngine.play(SoundEngine.Cue.TAP) }
                )

                Spacer(Modifier.height(26.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = caption,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))
                if (progress != null) {
                    Box(Modifier.width(190.dp)) {
                        GlowProgressBar(
                            fraction = progress,
                            accent = accent,
                            secondary = secondary
                        )
                    }
                } else {
                    ScanningDots(accent)
                }
            }
        }
    }
}

@Composable
private fun RadarSweep(accent: Color, secondary: Color) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "pulse"
    )

    // Fixed pseudo-random node positions, so blips stay put between frames.
    val nodes = remember {
        listOf(
            0.42f to 20f, 0.66f to 78f, 0.30f to 140f, 0.78f to 196f,
            0.54f to 250f, 0.36f to 300f, 0.70f to 336f, 0.86f to 112f
        )
    }

    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // Range rings.
            repeat(4) { i ->
                drawCircle(
                    color = accent.copy(alpha = 0.14f),
                    radius = maxRadius * (i + 1) / 4f,
                    center = centre,
                    style = Stroke(width = 1f)
                )
            }
            // Cross hairs.
            drawLine(
                accent.copy(alpha = 0.10f),
                Offset(centre.x - maxRadius, centre.y),
                Offset(centre.x + maxRadius, centre.y),
                strokeWidth = 1f
            )
            drawLine(
                accent.copy(alpha = 0.10f),
                Offset(centre.x, centre.y - maxRadius),
                Offset(centre.x, centre.y + maxRadius),
                strokeWidth = 1f
            )

            // Expanding ping ring.
            drawCircle(
                color = secondary.copy(alpha = (1f - pulse) * 0.35f),
                radius = maxRadius * pulse,
                center = centre,
                style = Stroke(width = 2f)
            )

            // The sweeping beam.
            rotate(sweep, centre) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.45f),
                            accent.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent
                        ),
                        center = centre
                    ),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(centre.x - maxRadius, centre.y - maxRadius),
                    size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
                )
                // Leading edge of the beam.
                drawLine(
                    color = accent,
                    start = centre,
                    end = Offset(centre.x + maxRadius, centre.y),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }

            // Node blips: brightest just after the beam passes over them.
            nodes.forEach { (distance, angleDeg) ->
                val delta = ((sweep - angleDeg) % 360f + 360f) % 360f
                val freshness = (1f - delta / 360f).coerceIn(0f, 1f)
                val position = Offset(
                    centre.x + maxRadius * distance * cos(Math.toRadians(angleDeg.toDouble())).toFloat(),
                    centre.y + maxRadius * distance * sin(Math.toRadians(angleDeg.toDouble())).toFloat()
                )
                drawCircle(
                    color = secondary.copy(alpha = 0.30f * freshness),
                    radius = 9f * freshness,
                    center = position
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.20f + 0.80f * freshness),
                    radius = 3f,
                    center = position
                )
            }

            // Centre marker: this device.
            drawCircle(accent, radius = 4f, center = centre)
            drawCircle(accent.copy(alpha = 0.25f), radius = 10f, center = centre)
        }
    }
}

/** Three dots that rise and fall in sequence. */
@Composable
private fun ScanningDots(accent: Color) {
    val transition = rememberInfiniteTransition(label = "dots")
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = index * 180, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(7.dp)
                    .background(accent.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
