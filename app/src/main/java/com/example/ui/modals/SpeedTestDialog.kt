package com.example.ui.modals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.GlassCard
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.min

/**
 * Live throughput readout.
 *
 * Shows a real measurement taken through the node's own tunnel — the needle
 * tracks bytes as they actually arrive, so the dial is reporting rather than
 * decorating.
 */
@Composable
fun SpeedTestDialog(
    state: MainViewModel.SpeedTestState,
    accent: Color,
    secondary: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(corner = 24.dp, padding = 22.dp, accent = accent) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "آزمایش سرعت",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    state.serverName,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                SpeedGauge(
                    mbps = state.mbps,
                    running = state.running,
                    accent = accent,
                    secondary = secondary
                )

                Spacer(Modifier.height(16.dp))

                when {
                    state.error != null -> Text(
                        state.error,
                        color = MeelanoRedKillSwitch,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    state.running -> Text(
                        "دریافت ${formatBytes(state.bytesTransferred)}…",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    else -> Text(
                        "${formatBytes(state.bytesTransferred)} منتقل شد",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    "اندازه‌گیری واقعی از طریق تونل همین سرور",
                    color = TextMuted,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        if (state.running) "بستن" else "تمام",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * An arc dial. The scale is logarithmic because connection speeds span orders of
 * magnitude — on a linear dial every realistic result would crowd the low end.
 */
@Composable
private fun SpeedGauge(mbps: Double, running: Boolean, accent: Color, secondary: Color) {
    val transition = rememberInfiniteTransition(label = "gauge")
    val sweepPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "sweep"
    )

    // 0 Mbps -> 0, 100 Mbps -> 1, spread logarithmically.
    val fraction = if (mbps <= 0) 0f else {
        (kotlin.math.log10(1.0 + mbps) / kotlin.math.log10(101.0)).toFloat().coerceIn(0f, 1f)
    }
    val needle by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(600),
        label = "needle"
    )

    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2 + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val startAngle = 140f
            val fullSweep = 260f

            // Track.
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = startAngle,
                sweepAngle = fullSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (running && mbps <= 0.0) {
                // Indeterminate: a comet travels the track while we wait for the
                // first bytes, so the dial never looks frozen.
                val head = startAngle + fullSweep * sweepPhase
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, accent, Color.Transparent)),
                    startAngle = head - 40f,
                    sweepAngle = 40f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    brush = Brush.linearGradient(listOf(secondary, accent)),
                    startAngle = startAngle,
                    sweepAngle = fullSweep * needle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            // Tick marks at the decade boundaries the log scale implies.
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { t ->
                val angle = Math.toRadians((startAngle + fullSweep * t).toDouble())
                val outer = min(size.width, size.height) / 2 - inset + stroke / 2
                val inner = outer - stroke * 0.55f
                val centre = Offset(size.width / 2, size.height / 2)
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(
                        centre.x + (inner * kotlin.math.cos(angle)).toFloat(),
                        centre.y + (inner * kotlin.math.sin(angle)).toFloat()
                    ),
                    end = Offset(
                        centre.x + (outer * kotlin.math.cos(angle)).toFloat(),
                        centre.y + (outer * kotlin.math.sin(angle)).toFloat()
                    ),
                    strokeWidth = 2f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (mbps > 0) "%.1f".format(mbps) else "—",
                color = TextPrimary,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Mbps", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
