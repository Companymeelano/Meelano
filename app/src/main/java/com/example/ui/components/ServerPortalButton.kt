package com.example.ui.components

import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The gateway to the server list, built as a physical object rather than a row.
 *
 * The left side holds a miniature rotating globe drawn with the same real
 * perspective projection as the loader: latitude rings that compress toward the
 * poles, markers that scale and dim with depth, and a scan bead that orbits it.
 * The card itself is lit from the top-left with a raised bevel, and it presses
 * inward when touched, so the whole control reads as a three-dimensional slab
 * sitting above the background.
 *
 * @param serverName currently selected node, shown so the button doubles as a
 *   status readout instead of a bare navigation affordance.
 * @param serverCount how many nodes are available, which is the number that
 *   actually tempts a user to open the list.
 */
@Composable
fun ServerPortalButton(
    serverName: String,
    country: String,
    flag: String,
    protocol: String,
    serverCount: Int,
    pingMs: Int,
    isVerified: Boolean,
    connected: Boolean,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAutoSelect: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "portal")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "spin"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "shimmer"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(130, easing = EaseOutQuart),
        label = "press"
    )
    val lift by animateFloatAsState(
        targetValue = if (pressed) 0.35f else 1f,
        animationSpec = tween(130),
        label = "lift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val corner = 22.dp.toPx()
            val inset = 1.5f

            // Drop shadow, which collapses on press so the slab appears to sink.
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.45f * lift),
                topLeft = Offset(0f, 6f * lift),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
            )

            // Body, lit from the top-left.
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF232049).copy(alpha = 0.97f),
                        Color(0xFF13102C).copy(alpha = 0.99f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                topLeft = Offset(0f, 0f),
                size = size.copy(height = size.height * press),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
            )

            // Bevel: bright along the top-left edge, dark along the bottom-right.
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f * lift),
                        accent.copy(alpha = 0.10f),
                        Color.Black.copy(alpha = 0.30f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                topLeft = Offset(inset, inset),
                size = size.copy(
                    width = size.width - inset * 2,
                    height = size.height * press - inset * 2
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                style = Stroke(width = 1.6f)
            )

            // A slow specular sweep across the face, so the surface looks glossy.
            val sweepX = size.width * (shimmer * 1.6f - 0.3f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.07f), Color.Transparent),
                    start = Offset(sweepX - 90f, 0f),
                    end = Offset(sweepX + 90f, size.height)
                ),
                size = size.copy(height = size.height * press),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
            )

            // The globe, inset on the leading edge.
            val globeCentre = Offset(size.width - 62.dp.toPx(), size.height * press / 2f)
            drawMiniGlobe(globeCentre, 30.dp.toPx(), spin, accent, secondary)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 104.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (flag.isNotBlank()) {
                        Text(flag, fontSize = 15.sp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        serverName.ifBlank { "سروری انتخاب نشده" },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (country.isNotBlank()) {
                        Text(country, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                        Text("·", color = TextMuted, fontSize = 10.sp)
                    }
                    Text(protocol, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                    if (pingMs in 1..9_998) {
                        Text("·", color = TextMuted, fontSize = 10.sp)
                        Text(
                            "$pingMs ms",
                            color = if (connected) MeelanoGreenSuccess else accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isVerified) {
                        Text("✓", color = MeelanoGreenSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(7.dp))

                // Auto-select lives inside the same object rather than as a
                // separate card, which is what made the old layout feel like two
                // competing entry points to the same screen.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.30f),
                                        secondary.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            .clickable(onClick = onAutoSelect)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "⚡ انتخاب خودکار",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "$serverCount نود",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * A small perspective-projected globe. Shares the maths of [HoloGlobeLoader] but
 * is stripped down to what stays legible at 60 dp.
 */
private fun DrawScope.drawMiniGlobe(
    centre: Offset,
    radius: Float,
    spin: Float,
    accent: Color,
    secondary: Color
) {
    val tilt = -0.4f
    val camera = 2.8f

    fun project(x: Float, y: Float, z: Float): Triple<Offset, Float, Float> {
        val cs = cos(spin)
        val sn = sin(spin)
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

    // Halo.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(accent.copy(alpha = 0.26f), Color.Transparent),
            center = centre,
            radius = radius * 1.9f
        ),
        radius = radius * 1.9f,
        center = centre
    )
    drawCircle(
        color = accent.copy(alpha = 0.34f),
        radius = radius,
        center = centre,
        style = Stroke(width = 1.2f)
    )

    // Latitude rings.
    for (ring in 1 until 5) {
        val phi = PI * ring / 5
        val y = cos(phi).toFloat()
        val rr = sin(phi).toFloat()
        var prev: Triple<Offset, Float, Float>? = null
        for (step in 0..30) {
            val theta = 2 * PI * step / 30
            val point = project(
                (cos(theta) * rr).toFloat(),
                y,
                (sin(theta) * rr).toFloat()
            )
            prev?.let { from ->
                val depth = (from.second + point.second) / 2f
                drawLine(
                    color = accent.copy(alpha = 0.06f + 0.34f * depth * depth),
                    start = from.first,
                    end = point.first,
                    strokeWidth = 0.6f + 0.9f * depth
                )
            }
            prev = point
        }
    }

    // Meridians.
    for (m in 0 until 6) {
        val theta = 2 * PI * m / 6
        var prev: Triple<Offset, Float, Float>? = null
        for (step in 0..24) {
            val phi = PI * step / 24
            val point = project(
                (sin(phi) * cos(theta)).toFloat(),
                cos(phi).toFloat(),
                (sin(phi) * sin(theta)).toFloat()
            )
            prev?.let { from ->
                val depth = (from.second + point.second) / 2f
                drawLine(
                    color = secondary.copy(alpha = 0.05f + 0.26f * depth * depth),
                    start = from.first,
                    end = point.first,
                    strokeWidth = 0.5f + 0.7f * depth
                )
            }
            prev = point
        }
    }

    // A bead orbiting the equator, drawn last so it always reads on top.
    val bead = project(cos(spin * 2f), 0.12f, sin(spin * 2f))
    drawCircle(
        color = accent.copy(alpha = 0.30f * bead.second),
        radius = 6f * bead.third,
        center = bead.first
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.45f + 0.5f * bead.second),
        radius = 2f * bead.third,
        center = bead.first,
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}
