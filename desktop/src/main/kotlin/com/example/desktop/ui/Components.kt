package com.example.desktop.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The signature backdrop: deep indigo cosmos with two slowly drifting nebulae
 * in the icon's cyan and violet. Same construction as the phone build.
 */
@Composable
fun AuroraBackground(
    accent: Color = MeelanoColors.IconCyan,
    secondary: Color = MeelanoColors.IconViolet,
    energised: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (energised) 9000 else 16000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(Modifier.fillMaxSize().background(MeelanoColors.BgDark)) {
        Canvas(Modifier.fillMaxSize()) {
            // Base wash, corner to corner, as in the icon's cosmos.
            drawRect(
                brush = Brush.linearGradient(
                    listOf(MeelanoColors.BgDark, MeelanoColors.BgMid, MeelanoColors.BgDarkSecondary),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )

            val glowAlpha = if (energised) 0.30f else 0.19f

            // Violet nebula, upper left.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(secondary.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(size.width * (0.20f + drift * 0.10f), size.height * 0.22f),
                    radius = size.minDimension * 0.72f
                ),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * (0.20f + drift * 0.10f), size.height * 0.22f)
            )
            // Cyan nebula, lower right, drifting against it.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = glowAlpha * 0.85f), Color.Transparent),
                    center = Offset(size.width * (0.82f - drift * 0.10f), size.height * 0.78f),
                    radius = size.minDimension * 0.66f
                ),
                radius = size.minDimension * 0.66f,
                center = Offset(size.width * (0.82f - drift * 0.10f), size.height * 0.78f)
            )
        }
        content()
    }
}

/** Dark glass with a gradient hairline border that can glow in the accent. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    accent: Color? = null,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val borderBrush = if (accent != null) {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = 0.55f),
                MeelanoColors.SurfaceCardBorder,
                accent.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.linearGradient(listOf(MeelanoColors.SurfaceCardBorder, MeelanoColors.BgMid))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MeelanoColors.SurfaceCard.copy(alpha = 0.82f), shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent,
                        (accent ?: MeelanoColors.SurfaceCardBorder).copy(alpha = 0.05f)
                    )
                ),
                shape
            )
            .border(1.dp, borderBrush, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

/**
 * The rotating shield mark.
 *
 * Drawn rather than bitmapped so it stays crisp at any window size and can
 * pulse with the connection, exactly as it does on the phone.
 */
@Composable
fun MeelanoShieldLogo(
    size: Dp = 44.dp,
    glowing: Boolean = true,
    accent: Color = MeelanoColors.IconCyan,
    secondary: Color = MeelanoColors.IconViolet
) {
    val transition = rememberInfiniteTransition(label = "logo")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            if (glowing) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.30f * pulse), Color.Transparent),
                        center = centre,
                        radius = radius
                    ),
                    radius = radius,
                    center = centre
                )
            }

            // Two counter-rotating orbit rings, cyan and violet.
            rotate(spin, centre) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(accent, Color.Transparent, accent), centre),
                    startAngle = 0f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(centre.x - radius * 0.92f, centre.y - radius * 0.92f),
                    size = Size(radius * 1.84f, radius * 1.84f),
                    style = Stroke(width = radius * 0.07f, cap = StrokeCap.Round)
                )
            }
            rotate(-spin * 0.7f, centre) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(secondary, Color.Transparent, secondary), centre),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(centre.x - radius * 0.74f, centre.y - radius * 0.74f),
                    size = Size(radius * 1.48f, radius * 1.48f),
                    style = Stroke(width = radius * 0.055f, cap = StrokeCap.Round)
                )
            }

            // The shield body.
            val w = radius * 0.62f
            val h = radius * 0.78f
            val shield = androidx.compose.ui.graphics.Path().apply {
                moveTo(centre.x, centre.y - h)
                lineTo(centre.x + w, centre.y - h * 0.52f)
                lineTo(centre.x + w, centre.y + h * 0.18f)
                quadraticBezierTo(
                    centre.x + w * 0.86f, centre.y + h * 0.78f,
                    centre.x, centre.y + h
                )
                quadraticBezierTo(
                    centre.x - w * 0.86f, centre.y + h * 0.78f,
                    centre.x - w, centre.y + h * 0.18f
                )
                lineTo(centre.x - w, centre.y - h * 0.52f)
                close()
            }
            drawPath(
                shield,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1B2340), Color(0xFF05070F)),
                    startY = centre.y - h,
                    endY = centre.y + h
                )
            )
            drawPath(
                shield,
                brush = Brush.linearGradient(listOf(MeelanoColors.Chrome, MeelanoColors.ChromeDim)),
                style = Stroke(width = radius * 0.062f)
            )

            // The neon "M".
            val mw = w * 0.62f
            val mh = h * 0.42f
            val stroke = Stroke(width = radius * 0.085f, cap = StrokeCap.Round)
            val m = androidx.compose.ui.graphics.Path().apply {
                moveTo(centre.x - mw, centre.y + mh)
                lineTo(centre.x - mw * 0.72f, centre.y - mh)
                lineTo(centre.x, centre.y + mh * 0.30f)
                lineTo(centre.x + mw * 0.72f, centre.y - mh)
                lineTo(centre.x + mw, centre.y + mh)
            }
            // Halo first, then the core stroke on top.
            drawPath(
                m,
                brush = Brush.horizontalGradient(listOf(accent, secondary)),
                style = Stroke(width = radius * 0.20f, cap = StrokeCap.Round),
                alpha = 0.28f * (if (glowing) pulse else 0.6f)
            )
            drawPath(m, brush = Brush.horizontalGradient(listOf(accent, secondary)), style = stroke)
        }
    }
}

/**
 * The connect button: concentric rings that spin while busy and rest when idle,
 * with a power glyph at the centre.
 */
@Composable
fun ConnectOrb(
    state: DesktopConnectionState,
    accent: Color = MeelanoColors.IconCyan,
    secondary: Color = MeelanoColors.IconViolet,
    size: Dp = 168.dp,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "orbspin"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    val ringColor = when (state) {
        DesktopConnectionState.CONNECTED -> MeelanoColors.GreenSuccess
        DesktopConnectionState.FAILED -> MeelanoColors.RedKillSwitch
        DesktopConnectionState.DISCONNECTED -> MeelanoColors.TextMuted
        else -> accent
    }
    val glow by animateFloatAsState(
        targetValue = if (state == DesktopConnectionState.CONNECTED) 1f else 0.55f,
        animationSpec = tween(600),
        label = "glow"
    )

    Box(
        Modifier.size(size).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ringColor.copy(alpha = 0.26f * glow), Color.Transparent),
                    center = centre,
                    radius = radius
                ),
                radius = radius,
                center = centre
            )

            // Outer ring: a full circle at rest, a chasing arc while busy, so
            // motion means "working" rather than being permanent decoration.
            if (state.isBusy) {
                rotate(spin, centre) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, accent, secondary, Color.Transparent),
                            centre
                        ),
                        startAngle = 0f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(centre.x - radius * 0.90f, centre.y - radius * 0.90f),
                        size = Size(radius * 1.80f, radius * 1.80f),
                        style = Stroke(width = radius * 0.055f, cap = StrokeCap.Round)
                    )
                }
            } else {
                drawCircle(
                    color = ringColor.copy(alpha = 0.45f),
                    radius = radius * 0.90f,
                    center = centre,
                    style = Stroke(width = radius * 0.05f)
                )
            }

            // Inner disc.
            val discScale = if (state == DesktopConnectionState.CONNECTED) breathe else 0.92f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(MeelanoColors.SurfaceElevated, MeelanoColors.BgDarkSecondary),
                    center = centre,
                    radius = radius * 0.72f
                ),
                radius = radius * 0.72f * discScale,
                center = centre
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.65f),
                radius = radius * 0.72f * discScale,
                center = centre,
                style = Stroke(width = radius * 0.018f)
            )

            // Power glyph: a broken ring with a vertical bar.
            val pr = radius * 0.30f
            drawArc(
                color = ringColor,
                startAngle = -65f,
                sweepAngle = 310f,
                useCenter = false,
                topLeft = Offset(centre.x - pr, centre.y - pr + radius * 0.03f),
                size = Size(pr * 2, pr * 2),
                style = Stroke(width = radius * 0.055f, cap = StrokeCap.Round)
            )
            drawLine(
                color = ringColor,
                start = Offset(centre.x, centre.y - pr * 1.30f),
                end = Offset(centre.x, centre.y - pr * 0.08f),
                strokeWidth = radius * 0.055f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Small pulsing status dot. */
@Composable
fun GlowDot(color: Color, size: Dp = 8.dp, pulsing: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "dot")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "dotpulse"
    )
    val alpha = if (pulsing) pulse else 1f
    Canvas(Modifier.size(size * 2.4f)) {
        val centre = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color.copy(alpha = 0.22f * alpha), this.size.minDimension / 2f, centre)
        drawCircle(color.copy(alpha = alpha), this.size.minDimension / 5f, centre)
    }
}

/** Horizontal meter with a soft glow, used for traffic and quota. */
@Composable
fun GlowProgressBar(
    fraction: Float,
    accent: Color = MeelanoColors.IconCyan,
    secondary: Color = MeelanoColors.IconViolet,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "bar"
    )
    Canvas(modifier.fillMaxSize()) {
        val h = size.height
        drawRoundRect(
            color = Color.White.copy(alpha = 0.07f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f)
        )
        if (animated > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(secondary, accent)),
                size = Size(size.width * animated, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f)
            )
        }
    }
}

/** Live throughput sparkline. */
@Composable
fun Sparkline(
    values: List<Float>,
    accent: Color = MeelanoColors.IconCyan,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val peak = (values.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
        val step = size.width / (values.size - 1)
        val path = androidx.compose.ui.graphics.Path()
        val fill = androidx.compose.ui.graphics.Path()

        values.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - (value / peak) * size.height * 0.92f
            if (index == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()

        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.30f), Color.Transparent)
            )
        )
        drawPath(path, color = accent, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

/**
 * A globe of latitude/longitude arcs with node markers, shown while the app is
 * testing servers. Tapping it kicks the spin, as on the phone.
 */
@Composable
fun HoloGlobe(
    size: Dp = 150.dp,
    accent: Color = MeelanoColors.IconCyan,
    secondary: Color = MeelanoColors.IconViolet
) {
    val transition = rememberInfiniteTransition(label = "globe")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "globespin"
    )

    Box(Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension * 0.40f
            val tilt = -0.42f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.16f)),
                    center = centre,
                    radius = radius * 1.25f
                ),
                radius = radius * 1.25f,
                center = centre
            )
            drawCircle(
                color = accent.copy(alpha = 0.40f),
                radius = radius,
                center = centre,
                style = Stroke(width = 1.4f)
            )

            // Latitude rings, squashed by the tilt to read as a sphere.
            for (i in 1..4) {
                val phi = (i / 5.0) * PI
                val y = cos(phi).toFloat() * radius
                val r = sin(phi).toFloat() * radius
                drawOval(
                    color = secondary.copy(alpha = 0.22f),
                    topLeft = Offset(centre.x - r, centre.y + y * tilt - r * 0.30f),
                    size = Size(r * 2, r * 0.60f),
                    style = Stroke(width = 1f)
                )
            }
            // Longitude arcs sweeping with the spin.
            for (i in 0 until 6) {
                val angle = spin + (i * PI / 6).toFloat()
                val w = kotlin.math.abs(cos(angle)) * radius
                drawOval(
                    color = accent.copy(alpha = 0.18f),
                    topLeft = Offset(centre.x - w, centre.y - radius),
                    size = Size(w * 2, radius * 2),
                    style = Stroke(width = 1f)
                )
            }
            // Node markers on the surface.
            val nodes = 18
            for (i in 0 until nodes) {
                val golden = PI * (3.0 - kotlin.math.sqrt(5.0))
                val yy = 1.0 - (i / (nodes - 1.0)) * 2.0
                val rr = kotlin.math.sqrt(1.0 - yy * yy)
                val theta = golden * i + spin
                val x3 = (cos(theta) * rr).toFloat()
                val z3 = (sin(theta) * rr).toFloat()
                val depth = (z3 + 1f) / 2f
                val px = centre.x + x3 * radius
                val py = centre.y + yy.toFloat() * radius * 0.92f + x3 * radius * tilt * 0.18f
                drawCircle(
                    color = (if (depth > 0.5f) accent else secondary)
                        .copy(alpha = 0.25f + 0.65f * depth),
                    radius = 1.2f + 2.2f * depth,
                    center = Offset(px, py)
                )
            }
        }
    }
}
