package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.vpn.VpnConnectionState
import kotlin.math.cos
import kotlin.math.sin

/**
 * The hero control: a layered energy orb with an orbiting handshake ring, an
 * animated conic aura and a pressure-responsive core. Its colour, ring speed and
 * glow intensity are all driven by the real tunnel state.
 */
@Composable
fun PowerButton3D(
    state: VpnConnectionState,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state == VpnConnectionState.CONNECTED
    val isBusy = state.isBusy
    val isFailed = state == VpnConnectionState.FAILED

    val target = when {
        isFailed -> MeelanoRedKillSwitch
        isConnected -> MeelanoGreenSuccess
        isBusy -> accent
        else -> Color(0xFF41567A)
    }
    val color by animateColorAsState(target, tween(600), label = "orbColor")

    val transition = rememberInfiniteTransition(label = "orb")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (isBusy) 1200 else if (isConnected) 6000 else 14000, easing = LinearEasing)
        ),
        label = "spin"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isConnected || isBusy) 1.06f else 0.98f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "ripple"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.93f else 1f, tween(120), label = "press")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(232.dp)
                    .scale(press)
                    .clip(CircleShape)
                    .selectable(
                        selected = isConnected,
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // outer expanding ripples while active
                if (isConnected || isBusy) {
                    listOf(ripple, (ripple + 0.5f) % 1f).forEach { t ->
                        drawCircle(
                            color = color.copy(alpha = (1f - t) * 0.22f),
                            radius = radius * (0.62f + 0.38f * t),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = 0.28f), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // conic aura ring
                rotate(spin, center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                color.copy(alpha = 0.85f),
                                Color.Transparent,
                                color.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.82f),
                        size = Size(radius * 1.64f, radius * 1.64f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // tick marks
                val ticks = 48
                for (i in 0 until ticks) {
                    val angle = Math.toRadians((360.0 / ticks) * i)
                    val active = isConnected || (isBusy && (i + (spin / 8).toInt()) % 6 == 0)
                    val inner = radius * 0.70f
                    val outer = radius * (if (active) 0.78f else 0.745f)
                    drawLine(
                        color = if (active) color.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.10f),
                        start = Offset(
                            center.x + inner * cos(angle).toFloat(),
                            center.y + inner * sin(angle).toFloat()
                        ),
                        end = Offset(
                            center.x + outer * cos(angle).toFloat(),
                            center.y + outer * sin(angle).toFloat()
                        ),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                // glass body
                val bodyRadius = radius * 0.60f * breathe
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF15243F), Color(0xFF0A1220)),
                        start = Offset(center.x - bodyRadius, center.y - bodyRadius),
                        end = Offset(center.x + bodyRadius, center.y + bodyRadius)
                    ),
                    radius = bodyRadius,
                    center = center
                )
                drawCircle(
                    color = color.copy(alpha = 0.55f),
                    radius = bodyRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                // top-left specular highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(center.x - bodyRadius * 0.4f, center.y - bodyRadius * 0.5f),
                        radius = bodyRadius * 0.9f
                    ),
                    radius = bodyRadius,
                    center = center
                )
                // inner energy core
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = if (isConnected) 0.45f else 0.20f), Color.Transparent),
                        center = center,
                        radius = bodyRadius * 0.85f
                    ),
                    radius = bodyRadius * 0.85f,
                    center = center
                )
            }

            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "اتصال",
                tint = color,
                modifier = Modifier
                    .size(56.dp)
                    .scale(press)
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = state.persName,
            color = if (isFailed) MeelanoRedKillSwitch else TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when (state) {
                VpnConnectionState.CONNECTED -> "ترافیک شما رمزنگاری شده است"
                VpnConnectionState.FAILED -> "برای تلاش دوباره لمس کنید"
                VpnConnectionState.DISCONNECTED -> "برای اتصال دکمه را لمس کنید"
                else -> "لطفاً چند لحظه صبر کنید…"
            },
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
