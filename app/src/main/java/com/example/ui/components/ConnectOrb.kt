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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeelanoBgDarkSecondary
import com.example.ui.theme.MeelanoBgMid
import com.example.ui.theme.MeelanoRedKillSwitch
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
 *  6. the shield crest and neon M from the launcher icon, whose glow
 *     tracks the state.
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
        failed -> MeelanoRedKillSwitch
        busy -> secondary
        else -> Color(0xFF7C8BA8)
    }

    // Rings alternate between the two accent tones for depth separation.
    val secondaryTone = if (connected) secondary else stateColor

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

                // ---- 5. three-dimensional orbital gyroscope ----
                //
                // Three rings inclined on different axes, each a real circle in
                // 3-D space that is rotated and perspective-projected. Points
                // swinging toward the viewer grow and brighten; those passing
                // behind the shield shrink, dim and are drawn first so the
                // shield genuinely occludes them. That front-to-back ordering is
                // what makes this read as a solid object rather than a drawing.
                val orbitScale = base * 0.74f * scale
                val rings = listOf(
                    Triple(0.00f, 0.62f, 1.00f),
                    Triple(1.15f, 0.48f, -0.75f),
                    Triple(2.30f, 0.70f, 0.55f)
                )

                rings.forEachIndexed { ringIndex, (tiltAxis, inclination, speed) ->
                    val samples = 72
                    val points = ArrayList<Triple<Offset, Float, Float>>(samples)
                    val phase = Math.toRadians((spin * speed).toDouble()).toFloat()

                    for (i in 0 until samples) {
                        val t = (i.toFloat() / samples) * 2f * Math.PI.toFloat()
                        // A unit circle in the XY plane...
                        var px = cos(t)
                        var py = sin(t)
                        var pz = 0f

                        // ...inclined about X to give it a tilt...
                        val ci = cos(inclination)
                        val si = sin(inclination)
                        val y1 = py * ci - pz * si
                        val z1 = py * si + pz * ci
                        py = y1
                        pz = z1

                        // ...rotated about Z so each ring sits on its own axis...
                        val ca = cos(tiltAxis)
                        val sa = sin(tiltAxis)
                        val x2 = px * ca - py * sa
                        val y2 = px * sa + py * ca
                        px = x2
                        py = y2

                        // ...and finally spun about Y over time.
                        val cp = cos(phase)
                        val sp = sin(phase)
                        val x3 = px * cp - pz * sp
                        val z3 = px * sp + pz * cp

                        // Perspective divide: nearer points spread out and grow.
                        val depth = (z3 + 1f) / 2f
                        val perspective = 2.4f / (2.4f - z3)
                        points.add(
                            Triple(
                                Offset(
                                    centre.x + x3 * orbitScale * perspective,
                                    centre.y + py * orbitScale * perspective
                                ),
                                depth,
                                perspective
                            )
                        )
                    }

                    // Painter's algorithm along the ring.
                    val ordered = points.sortedBy { it.second }
                    ordered.forEach { (position, depth, perspective) ->
                        val tone = if (ringIndex == 1) secondaryTone else stateColor
                        val alpha = (0.06f + 0.55f * depth * depth) * (0.35f + 0.65f * glow)
                        drawCircle(
                            color = tone.copy(alpha = alpha),
                            radius = (0.7f + 1.9f * depth) * perspective,
                            center = position
                        )
                    }

                    // A travelling light bead per ring, so motion is legible even
                    // when the app is idle and the rings are dim.
                    if (connected || busy) {
                        val head = points[
                            ((spin * speed / 360f * samples).toInt().mod(samples))
                        ]
                        val (position, depth, perspective) = head
                        val tone = if (ringIndex == 1) secondaryTone else stateColor
                        drawCircle(
                            color = tone.copy(alpha = 0.28f * glow * depth),
                            radius = 9f * perspective,
                            center = position
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = (0.35f + 0.6f * depth) * glow),
                            radius = 2.6f * perspective,
                            center = position
                        )
                    }
                }

                // ---- 6. the globe the shield sits on ----
                //
                // A real sphere rather than a flat disc: the graticule is
                // projected in perspective so its rings bow and compress toward
                // the limb, the terminator shades the lower-right into shadow,
                // and a rim light traces the edge. The shield then sits in front
                // of it, so the crest reads as guarding the planet.
                val domeRadius = base * 0.52f * scale
                drawGlobe(centre, domeRadius, spin, stateColor, secondaryTone, glow)

                // ---- 7. the shield + neon M, straight from the launcher icon ----
                drawShieldCrest(centre, domeRadius * 0.86f, stateColor, glow, connected)
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

/**
 * Draws the launcher icon's crest: a chrome-rimmed shield carrying the neon "M".
 *
 * This is the app's signature mark, so the hero control renders it rather than a
 * generic power symbol — the home screen and the launcher icon now show the same
 * object. The M keeps the icon's cyan-to-violet stroke gradient, while the
 * shield rim picks up the current connection colour so state is still legible at
 * a glance.
 */
private fun DrawScope.drawShieldCrest(
    centre: Offset,
    height: Float,
    stateColor: Color,
    glow: Float,
    connected: Boolean
) {
    val halfWidth = height * 0.40f
    val top = centre.y - height * 0.50f
    val bottom = centre.y + height * 0.50f
    val shoulder = top + height * 0.30f

    // Classic heater-shield silhouette: square shoulders tapering to a point.
    val shield = Path().apply {
        moveTo(centre.x - halfWidth, top)
        lineTo(centre.x + halfWidth, top)
        lineTo(centre.x + halfWidth, shoulder)
        cubicTo(
            centre.x + halfWidth, bottom - height * 0.22f,
            centre.x + halfWidth * 0.55f, bottom - height * 0.05f,
            centre.x, bottom
        )
        cubicTo(
            centre.x - halfWidth * 0.55f, bottom - height * 0.05f,
            centre.x - halfWidth, bottom - height * 0.22f,
            centre.x - halfWidth, shoulder
        )
        close()
    }

    // Dark glass interior, brighter toward the top-left like the icon.
    drawPath(
        path = shield,
        brush = Brush.linearGradient(
            listOf(
                Color(0xFF243056).copy(alpha = 0.95f),
                Color(0xFF0B0A1F).copy(alpha = 0.98f)
            ),
            start = Offset(centre.x - halfWidth, top),
            end = Offset(centre.x + halfWidth, bottom)
        )
    )

    // Chrome rim. Tinted by state so the crest still reads as a status light.
    drawPath(
        path = shield,
        brush = Brush.linearGradient(
            listOf(
                Color(0xFFE8EEFB).copy(alpha = 0.92f),
                stateColor.copy(alpha = 0.55f),
                Color(0xFF3D4A72).copy(alpha = 0.85f),
                Color(0xFFC9D2E6).copy(alpha = 0.60f)
            ),
            start = Offset(centre.x - halfWidth, top),
            end = Offset(centre.x + halfWidth, bottom)
        ),
        style = Stroke(width = height * 0.055f)
    )

    drawNeonM(centre, height * 0.34f, glow, connected)
}

/** The icon's neon "M", stroked with its cyan-to-violet gradient. */
private fun DrawScope.drawNeonM(centre: Offset, size: Float, glow: Float, connected: Boolean) {
    val halfWidth = size * 0.62f
    val halfHeight = size * 0.52f
    val topY = centre.y - halfHeight
    val bottomY = centre.y + halfHeight

    val m = Path().apply {
        moveTo(centre.x - halfWidth, bottomY)
        lineTo(centre.x - halfWidth * 0.62f, topY)
        lineTo(centre.x, centre.y + halfHeight * 0.22f)
        lineTo(centre.x + halfWidth * 0.62f, topY)
        lineTo(centre.x + halfWidth, bottomY)
    }

    val strokeBrush = Brush.linearGradient(
        listOf(Color(0xFF1FEAF7), Color(0xFF7A9BFF), Color(0xFFB44BFF)),
        start = Offset(centre.x - halfWidth, centre.y),
        end = Offset(centre.x + halfWidth, centre.y)
    )

    // Two soft passes underneath give the tube its neon bloom.
    val intensity = if (connected) glow else glow * 0.55f
    drawPath(
        path = m,
        brush = strokeBrush,
        alpha = 0.18f * intensity,
        style = Stroke(width = size * 0.46f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = m,
        brush = strokeBrush,
        alpha = 0.34f * intensity,
        style = Stroke(width = size * 0.26f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = m,
        brush = strokeBrush,
        style = Stroke(width = size * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // A white core is what makes a neon tube read as lit rather than merely coloured.
    drawPath(
        path = m,
        color = Color.White.copy(alpha = 0.55f * intensity),
        style = Stroke(width = size * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * A three-dimensional Earth.
 *
 * Every graticule point is a coordinate on a unit sphere that is rotated about
 * the polar axis and perspective-projected, so the lines genuinely bow toward
 * the limb instead of being drawn as ellipses. Lighting is a single lamp at the
 * upper-left: a radial body gradient establishes form, a terminator darkens the
 * far side, and a thin rim light separates the sphere from the background.
 */
private fun DrawScope.drawGlobe(
    centre: Offset,
    radius: Float,
    spin: Float,
    accent: Color,
    secondary: Color,
    glow: Float
) {
    val tilt = -0.38f
    val camera = 2.7f
    val phase = Math.toRadians(spin.toDouble()).toFloat()

    fun project(x: Float, y: Float, z: Float): Triple<Offset, Float, Float> {
        val cs = cos(phase)
        val sn = sin(phase)
        val x1 = x * cs - z * sn
        val z1 = x * sn + z * cs
        val ct = cos(tilt)
        val st = sin(tilt)
        val y2 = y * ct - z1 * st
        val z2 = y * st + z1 * ct
        val p = camera / (camera - z2)
        return Triple(
            Offset(centre.x + x1 * radius * p, centre.y + y2 * radius * p),
            (z2 + 1f) / 2f,
            p
        )
    }

    // Body: lit from the upper-left, falling into shadow at the lower-right.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = 0.30f),
                MeelanoBgMid.copy(alpha = 0.94f),
                MeelanoBgDarkSecondary
            ),
            center = Offset(centre.x - radius * 0.38f, centre.y - radius * 0.44f),
            radius = radius * 1.85f
        ),
        radius = radius,
        center = centre
    )

    // Graticule. Latitudes first, then meridians, each segment shaded by depth
    // so the far side of the sphere recedes.
    for (ring in 1 until 7) {
        val phi = (Math.PI * ring / 7).toFloat()
        val y = cos(phi)
        val rr = sin(phi)
        var prev: Triple<Offset, Float, Float>? = null
        for (step in 0..44) {
            val theta = (2 * Math.PI * step / 44).toFloat()
            val point = project(cos(theta) * rr, y, sin(theta) * rr)
            prev?.let { from ->
                val depth = (from.second + point.second) / 2f
                drawLine(
                    color = accent.copy(alpha = (0.04f + 0.30f * depth * depth) * (0.5f + 0.5f * glow)),
                    start = from.first,
                    end = point.first,
                    strokeWidth = 0.6f + 1.0f * depth
                )
            }
            prev = point
        }
    }
    for (m in 0 until 9) {
        val theta = (2 * Math.PI * m / 9).toFloat()
        var prev: Triple<Offset, Float, Float>? = null
        for (step in 0..36) {
            val phi = (Math.PI * step / 36).toFloat()
            val point = project(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta))
            prev?.let { from ->
                val depth = (from.second + point.second) / 2f
                drawLine(
                    color = secondary.copy(alpha = (0.03f + 0.22f * depth * depth) * (0.5f + 0.5f * glow)),
                    start = from.first,
                    end = point.first,
                    strokeWidth = 0.5f + 0.8f * depth
                )
            }
            prev = point
        }
    }

    // Terminator: a soft shadow washing the unlit limb.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            center = Offset(centre.x - radius * 0.30f, centre.y - radius * 0.34f),
            radius = radius * 1.55f
        ),
        radius = radius,
        center = centre
    )

    // Atmospheric rim light.
    drawCircle(
        color = accent.copy(alpha = 0.30f + 0.35f * glow),
        radius = radius,
        center = centre,
        style = Stroke(width = 1.8f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, accent.copy(alpha = 0.18f * glow)),
            center = centre,
            radius = radius * 1.16f
        ),
        radius = radius * 1.16f,
        center = centre
    )
}
