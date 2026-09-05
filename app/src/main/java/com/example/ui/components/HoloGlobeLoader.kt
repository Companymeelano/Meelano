package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * A genuinely three-dimensional holographic globe, used while the app is
 * searching for servers.
 *
 * Rather than faking depth with a flat spinner, every element is a real point in
 * 3-D space that is rotated by matrix maths and then perspective-projected onto
 * the canvas. Depth is expressed the way the eye expects it:
 *
 *  - points further away are smaller, dimmer and cooler in hue;
 *  - the near hemisphere occludes the far one, drawn back-to-front;
 *  - the wireframe's latitude rings compress toward the poles under projection;
 *  - node markers scale with true perspective divide, not a fudge factor.
 *
 * @param nodes number of server markers scattered over the sphere.
 */
@Composable
fun HoloGlobeLoader(
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    nodes: Int = 26
) {
    val transition = rememberInfiniteTransition(label = "globe")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "spin"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "sweep"
    )

    // Fixed positions on the sphere, distributed evenly by the Fibonacci method
    // so the markers never clump the way uniform random angles do.
    val markers = remember(nodes) {
        val random = Random(7)
        val golden = PI * (3.0 - kotlin.math.sqrt(5.0))
        List(nodes) { i ->
            val y = 1.0 - (i / (nodes - 1.0)) * 2.0
            val radius = kotlin.math.sqrt(1.0 - y * y)
            val theta = golden * i
            Marker(
                x = (cos(theta) * radius).toFloat(),
                y = y.toFloat(),
                z = (sin(theta) * radius).toFloat(),
                phase = random.nextFloat()
            )
        }
    }

    Box(modifier = modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension * 0.38f

            // Tilt the globe slightly so we look at it from above the equator —
            // a dead-on view reads as a flat circle and loses the 3-D effect.
            val tilt = -0.42f

            drawAtmosphere(centre, radius, accent)
            drawWireframe(centre, radius, spin, tilt, accent, secondary)
            drawMarkers(centre, radius, spin, tilt, markers, sweep, accent, secondary)
        }
    }
}

private data class Marker(val x: Float, val y: Float, val z: Float, val phase: Float)

/** A projected point plus the depth information needed to shade it. */
private data class Projected(val position: Offset, val scale: Float, val depth: Float)

/**
 * Rotates a unit-sphere point about the Y then X axis and applies a perspective
 * divide. [depth] comes back in 0..1, where 1 is nearest the viewer.
 */
private fun project(
    x: Float,
    y: Float,
    z: Float,
    spin: Float,
    tilt: Float,
    centre: Offset,
    radius: Float
): Projected {
    // Yaw around the vertical axis.
    val cosSpin = cos(spin)
    val sinSpin = sin(spin)
    val x1 = x * cosSpin - z * sinSpin
    val z1 = x * sinSpin + z * cosSpin

    // Pitch, to tilt the pole toward the viewer.
    val cosTilt = cos(tilt)
    val sinTilt = sin(tilt)
    val y2 = y * cosTilt - z1 * sinTilt
    val z2 = y * sinTilt + z1 * cosTilt

    // Perspective divide: the camera sits CAMERA units down the +Z axis.
    val perspective = CAMERA / (CAMERA - z2)
    return Projected(
        position = Offset(
            centre.x + x1 * radius * perspective,
            centre.y + y2 * radius * perspective
        ),
        scale = perspective,
        depth = (z2 + 1f) / 2f
    )
}

/** Distance from the camera to the sphere centre, in sphere radii. */
private const val CAMERA = 2.6f

/** Soft glow so the globe sits in light rather than on a flat background. */
private fun DrawScope.drawAtmosphere(centre: Offset, radius: Float, accent: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = 0.20f),
                accent.copy(alpha = 0.06f),
                Color.Transparent
            ),
            center = centre,
            radius = radius * 1.75f
        ),
        radius = radius * 1.75f,
        center = centre
    )
    // Rim light along the limb of the sphere.
    drawCircle(
        color = accent.copy(alpha = 0.28f),
        radius = radius,
        center = centre,
        style = Stroke(width = 1.4f)
    )
}

/**
 * The latitude/longitude cage. Segments are drawn individually so each can be
 * shaded by its own depth, which is what sells the rotation.
 */
private fun DrawScope.drawWireframe(
    centre: Offset,
    radius: Float,
    spin: Float,
    tilt: Float,
    accent: Color,
    secondary: Color
) {
    val steps = 48

    // Latitude rings.
    for (ring in 1 until LATITUDES) {
        val phi = PI * ring / LATITUDES
        val y = cos(phi).toFloat()
        val ringRadius = sin(phi).toFloat()

        var previous: Projected? = null
        for (step in 0..steps) {
            val theta = 2 * PI * step / steps
            val point = project(
                x = (cos(theta) * ringRadius).toFloat(),
                y = y,
                z = (sin(theta) * ringRadius).toFloat(),
                spin = spin, tilt = tilt, centre = centre, radius = radius
            )
            previous?.let { from ->
                val depth = (from.depth + point.depth) / 2f
                drawLine(
                    color = accent.copy(alpha = 0.05f + 0.30f * depth * depth),
                    start = from.position,
                    end = point.position,
                    strokeWidth = 0.7f + 1.1f * depth
                )
            }
            previous = point
        }
    }

    // Longitude meridians.
    for (meridian in 0 until MERIDIANS) {
        val theta = 2 * PI * meridian / MERIDIANS
        var previous: Projected? = null
        for (step in 0..steps) {
            val phi = PI * step / steps
            val point = project(
                x = (sin(phi) * cos(theta)).toFloat(),
                y = cos(phi).toFloat(),
                z = (sin(phi) * sin(theta)).toFloat(),
                spin = spin, tilt = tilt, centre = centre, radius = radius
            )
            previous?.let { from ->
                val depth = (from.depth + point.depth) / 2f
                drawLine(
                    color = secondary.copy(alpha = 0.04f + 0.24f * depth * depth),
                    start = from.position,
                    end = point.position,
                    strokeWidth = 0.6f + 0.9f * depth
                )
            }
            previous = point
        }
    }
}

/**
 * Server markers. Drawn back-to-front so near nodes correctly overlap far ones,
 * with a scan pulse that travels around the globe and lights each node as it
 * passes — the visual promise that testing is really happening.
 */
private fun DrawScope.drawMarkers(
    centre: Offset,
    radius: Float,
    spin: Float,
    tilt: Float,
    markers: List<Marker>,
    sweep: Float,
    accent: Color,
    secondary: Color
) {
    val projected = markers
        .map { it to project(it.x, it.y, it.z, spin, tilt, centre, radius) }
        // Painter's algorithm: far nodes first, so near ones draw over them.
        .sortedBy { it.second.depth }

    projected.forEach { (marker, point) ->
        // Each node lights up as the scan pulse sweeps past its own phase.
        val distance = abs(((sweep + marker.phase) % 1f) - 0.5f) * 2f
        val lit = (1f - distance).coerceIn(0f, 1f)
        val pulse = lit * lit * lit

        val depth = point.depth
        // Far side stays dim and cool; the near side is bright and saturated.
        val tone = lerpColor(secondary, accent, depth)
        val alpha = (0.18f + 0.62f * depth) * (0.55f + 0.45f * pulse)
        val dot = (1.1f + 2.4f * depth) * point.scale * (1f + 0.5f * pulse)

        if (pulse > 0.05f) {
            drawCircle(
                color = tone.copy(alpha = alpha * 0.30f * pulse),
                radius = dot * 4.2f,
                center = point.position
            )
        }
        drawCircle(color = tone.copy(alpha = alpha), radius = dot, center = point.position)
    }

    // Connection threads between nearby near-side nodes, hinting at a mesh.
    val near = projected.filter { it.second.depth > 0.62f }
    for (i in near.indices) {
        for (j in i + 1 until near.size) {
            val a = near[i].second
            val b = near[j].second
            val span = hypot(a.position.x - b.position.x, a.position.y - b.position.y)
            if (span < radius * 0.42f) {
                val strength = 1f - span / (radius * 0.42f)
                drawLine(
                    color = accent.copy(alpha = 0.16f * strength),
                    start = a.position,
                    end = b.position,
                    strokeWidth = 0.9f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun lerpColor(from: Color, to: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * clamped,
        green = from.green + (to.green - from.green) * clamped,
        blue = from.blue + (to.blue - from.blue) * clamped,
        alpha = 1f
    )
}

private const val LATITUDES = 9
private const val MERIDIANS = 12
