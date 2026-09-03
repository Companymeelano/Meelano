package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeelanoCyan
import com.example.ui.theme.MeelanoCyanGlow
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.vpn.VpnConnectionState

@Composable
fun PowerButton3D(
    state: VpnConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state == VpnConnectionState.CONNECTED
    val isConnecting = state == VpnConnectionState.CONNECTING || state == VpnConnectionState.RECONNECTING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnecting || isConnected) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isConnecting) 0.8f else if (isConnected) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(160.dp)
            .scale(pulseScale)
            .testTag("power_connect_button")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric glow ring
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringRadius = size.width * 0.48f

            val ringColor = when {
                isConnected -> MeelanoCyan
                isConnecting -> MeelanoCyanGlow
                else -> Color(0xFF1E2D4A)
            }

            drawCircle(
                color = ringColor.copy(alpha = glowAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Second concentric inner rim
            drawCircle(
                color = Color(0xFF121F38),
                radius = ringRadius - 6f,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        // Inner 3D Button Body
        Box(
            modifier = Modifier
                .size(132.dp)
                .shadow(
                    elevation = if (isConnected) 16.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isConnected) MeelanoCyan else Color.Black,
                    spotColor = if (isConnected) MeelanoCyanGlow else Color.Black
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isConnected) {
                            listOf(Color(0xFF0F2C4A), Color(0xFF0A182E), Color(0xFF050E1F))
                        } else {
                            listOf(Color(0xFF192A45), Color(0xFF111E36), Color(0xFF091224))
                        },
                        radius = 180f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Button Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "اتصال",
                    tint = when {
                        isConnected -> MeelanoCyan
                        isConnecting -> MeelanoCyanGlow
                        else -> Color(0xFF8BA2C7)
                    },
                    modifier = Modifier.size(42.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (state) {
                        VpnConnectionState.DISCONNECTED -> "اتصال"
                        VpnConnectionState.CONNECTING -> "درحال اتصال..."
                        VpnConnectionState.CONNECTED -> "متصل شد"
                        VpnConnectionState.DISCONNECTING -> "قطع اتصال..."
                        VpnConnectionState.RECONNECTING -> "سوییچ هوشمند..."
                    },
                    color = when {
                        isConnected -> MeelanoCyan
                        isConnecting -> MeelanoCyanGlow
                        else -> Color(0xFFCDD9EC)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
