package com.example.ui.modals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.GlassCard
import com.example.ui.components.HoloGlobeLoader
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Shown when connect is pressed with an empty server list.
 *
 * The app no longer downloads subscriptions on first launch — that spent the
 * user's data before they had asked for anything and made startup look stuck.
 * This explains the situation and offers the two ways forward instead of
 * failing with a vague error.
 */
@Composable
fun NeedsServersDialog(
    accent: Color,
    secondary: Color,
    onOpenServers: () -> Unit,
    onFetchNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(corner = 24.dp, padding = 22.dp, accent = accent) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HoloGlobeLoader(
                    accent = accent,
                    secondary = secondary,
                    size = 128.dp,
                    nodes = 18
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "هنوز سروری ندارید",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "برای اتصال، ابتدا باید فهرست سرورها را دریافت کنید. " +
                        "این کار به‌صورت خودکار انجام نمی‌شود تا بدون اجازهٔ شما " +
                        "از اینترنت‌تان استفاده نشود.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(18.dp))

                ActionTile(
                    icon = Icons.Default.CloudDownload,
                    title = "دریافت سرورهای رایگان",
                    subtitle = "از منابع به‌روزشونده",
                    tint = accent,
                    secondary = secondary,
                    onClick = onFetchNow
                )

                Spacer(Modifier.height(9.dp))

                ActionTile(
                    icon = Icons.Default.Language,
                    title = "رفتن به بخش سرورها",
                    subtitle = "افزودن کانفیگ یا اشتراک دلخواه",
                    tint = secondary,
                    secondary = accent,
                    onClick = onOpenServers
                )

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("بعداً", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * A raised action slab: cast shadow, lit body and a bevel, so the two choices
 * read as physical buttons consistent with the rest of the shell.
 */
@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    secondary: Color,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "tile")
    val sheen by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing)),
        label = "sheen"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val corner = CornerRadius(15.dp.toPx(), 15.dp.toPx())

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.40f),
                topLeft = Offset(0f, 3f),
                size = size,
                cornerRadius = corner
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = 0.34f),
                        secondary.copy(alpha = 0.14f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = corner
            )
            val x = size.width * (sheen * 1.7f - 0.35f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
                    start = Offset(x - 70f, 0f),
                    end = Offset(x + 70f, size.height)
                ),
                cornerRadius = corner
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.34f),
                        tint.copy(alpha = 0.16f),
                        Color.Black.copy(alpha = 0.28f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = corner,
                style = Stroke(width = 1.4f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val c = Offset(r, r)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.42f), tint.copy(alpha = 0.30f)),
                            center = Offset(r * 0.66f, r * 0.58f),
                            radius = r * 1.6f
                        ),
                        radius = r * 0.9f,
                        center = c
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.34f),
                        radius = r * 0.9f,
                        center = c,
                        style = Stroke(width = 1.1f)
                    )
                }
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }

            Column {
                Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 9.sp)
            }
        }
    }
}
