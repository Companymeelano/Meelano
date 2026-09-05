package com.example.ui.components

import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vpn.VpnConnectionState
import kotlin.math.cos
import kotlin.math.sin

/**
 * The hero control of the app: a layered energy orb whose every visual property
 * is driven by the *real* tunnel state.
 *
 * Layers, outermost first:
 *  1. expanding shock rings that pulse outward while connected;
 *  2. a slowly counter-rotating dashed containment ring;
 *  3. a sweeping radar arc that accelerates dramatically while connecting;
 *  4. orbiting energy particles whose radius breathes with the state;
 *  5. a glass dome with a specular highlight, matching the launcher icon;
 *  6. the power glyph, which morphs colour and glow with the state.
 */
@Composable
fun ConnectOrb(
    state: VpnConnectionState,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val connected = state == VpnConnectionState.CONNECTED
    val busy = state.isBusy
    val failed = state == VpnConnectionState.FAILED

    val stateColor = when {
        connected -> accent
        failed -> Color(0xFFFF3B5C)
        busy -> secondary
        else -> Color(0xFF7C8BA8)
    }

    val transition = rememberInfiniteTransition(label = "orb")

    // Continuous rotation drives the ring and the particles.
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (busy) 1400 else if (connected) 7000 else 18000, easing = LinearEasing)
        ),
        label = "spin"
    )

    // Shock rings: three staggered expansions.
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "ripple"
    )

    // Breathing scale of the whole orb.
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Press feedback.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(140, easing = EaseOutQuart),
        label = "press"
    )

    val glow by animateFloatAsState(
        targetValue = if (connected) 1f else if (busy) 0.7f else 0.28f,
        animationSpec = tween(600),
        label = "glow"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(232.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val centre = Offset(size.width / 2f, size.height / 2f)
                val base = size.minDimension / 2f
                val scale = press * if (connected || busy) breathe else 1f

                // ---- 1. expanding shock rings ----
                if (connected || busy) {
                    repeat(3) { index ->
                        val progress = (ripple + index / 3f) % 1f
                        val radius = base * (0.42f + progress * 0.58f) * scale
                        drawCircle(
                            color = stateColor.copy(alpha = (1f - progress) * 0.30f * glow),
                            radius = radius,
                            center = centre,
                            style = Stroke(width = (2.5f - progress * 1.8f).coerceAtLeast(0.6f))
                        )
                    }
                }

                // ---- 2. ambient bloom ----
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            stateColor.copy(alpha = 0.26f * glow),
                            stateColor.copy(alpha = 0.07f * glow),
                            Color.Transparent
                        ),
                        center = centre,
                        radius = base * 0.95f * scale
                    ),
                    radius = base * 0.95f * scale,
                    center = centre
                )

                // ---- 3. dashed containment ring (counter rotating) ----
                rotate(-spin * 0.6f, centre) {
                    val ringRadius = base * 0.82f * scale
                    val segments = 48
                    repeat(segments) { i ->
                        if (i % 2 == 0) {
                            val a0 = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                            val a1 = ((i + 0.85f) / segments) * 2f * Math.PI.toFloat()
                            drawLine(
                                color = stateColor.copy(alpha = 0.30f + 0.35f * glow),
                                start = Offset(
                                    centre.x + ringRadius * cos(a0),
                                    centre.y + ringRadius * sin(a0)
                                ),
                                end = Offset(
                                    centre.x + ringRadius * cos(a1),
                                    centre.y + ringRadius * sin(a1)
                                ),
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // ---- 4. sweeping radar arc ----
                rotate(spin, centre) {
                    val arcRadius = base * 0.70f * scale
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                stateColor.copy(alpha = 0.15f * glow),
                                stateColor.copy(alpha = 0.85f * glow)
                            ),
                            center = centre
                        ),
                        startAngle = 0f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(centre.x - arcRadius, centre.y - arcRadius),
                        size = Size(arcRadius * 2, arcRadius * 2),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                // ---- 5. orbiting particles ----
                if (connected || busy) {
                    val count = 7
                    repeat(count) { i ->
                        val angle = Math.toRadians(
                            (spin * (if (i % 2 == 0) 1f else -1.4f) + i * (360f / count)).toDouble()
                        ).toFloat()
                        val orbit = base * (0.60f + 0.10f * sin(spin / 40f + i)) * scale
                        val position = Offset(
                            centre.x + orbit * cos(angle),
                            centre.y + orbit * sin(angle)
                        )
                        // Halo then core, so each particle reads as a light source.
                        drawCircle(
                            color = stateColor.copy(alpha = 0.20f * glow),
                            radius = 7f,
                            center = position
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f * glow),
                            radius = 2.2f,
                            center = position
                        )
                    }
                }

                // ---- 6. glass dome ----
                val domeRadius = base * 0.52f * scale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.13f),
                            stateColor.copy(alpha = 0.22f),
                            Color(0xFF141634).copy(alpha = 0.96f)
                        ),
                        center = Offset(centre.x - domeRadius * 0.3f, centre.y - domeRadius * 0.4f),
                        radius = domeRadius * 1.7f
                    ),
                    radius = domeRadius,
                    center = centre
                )
                // Chrome bevel, echoing the launcher icon's shield edge.
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            stateColor.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.10f)
                        ),
                        start = Offset(centre.x - domeRadius, centre.y - domeRadius),
                        end = Offset(centre.x + domeRadius, centre.y + domeRadius)
                    ),
                    radius = domeRadius,
                    center = centre,
                    style = Stroke(width = 2.2f)
                )
                // Specular highlight.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(centre.x - domeRadius * 0.35f, centre.y - domeRadius * 0.5f),
                        radius = domeRadius * 0.65f
                    ),
                    radius = domeRadius * 0.65f,
                    center = Offset(centre.x - domeRadius * 0.35f, centre.y - domeRadius * 0.5f)
                )

                // ---- 7. power glyph ----
                drawPowerGlyph(centre, domeRadius * 0.52f, stateColor, glow)
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = when (state) {
                VpnConnectionState.CONNECTED -> "متصل"
                VpnConnectionState.CONNECTING -> "در حال اتصال…"
                VpnConnectionState.RECONNECTING -> "اتصال مجدد…"
                VpnConnectionState.DISCONNECTING -> "در حال قطع…"
                VpnConnectionState.FAILED -> "اتصال ناموفق"
                VpnConnectionState.DISCONNECTED -> "برای اتصال ضربه بزنید"
            },
            color = stateColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** The classic power symbol: a broken ring with a vertical stem. */
private fun DrawScope.drawPowerGlyph(
    centre: Offset,
    radius: Float,
    color: Color,
    glow: Float
) {
    val stroke = radius * 0.22f
    // Soft glow pass underneath.
    drawArc(
        color = color.copy(alpha = 0.30f * glow),
        startAngle = -65f,
        sweepAngle = 310f,
        useCenter = false,
        topLeft = Offset(centre.x - radius, centre.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke * 2.4f, cap = StrokeCap.Round)
    )
    drawArc(
        color = color,
        startAngle = -65f,
        sweepAngle = 310f,
        useCenter = false,
        topLeft = Offset(centre.x - radius, centre.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    drawLine(
        color = color.copy(alpha = 0.30f * glow),
        start = Offset(centre.x, centre.y - radius * 1.28f),
        end = Offset(centre.x, centre.y - radius * 0.12f),
        strokeWidth = stroke * 2.4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(centre.x, centre.y - radius * 1.28f),
        end = Offset(centre.x, centre.y - radius * 0.12f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}
