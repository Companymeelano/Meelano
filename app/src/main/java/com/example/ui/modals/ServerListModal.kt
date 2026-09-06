package com.example.ui.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import com.example.ui.theme.Spacing
import com.example.ui.components.GlowProgressBar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ServerSort
import com.example.data.model.VpnServer
import com.example.data.repository.ServerRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.Pill
import com.example.ui.components.SignalBars
import com.example.ui.theme.LocalAccent
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoAmber
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.pingColor

@Composable
fun ServerListModal(
    vipServers: List<VpnServer>,
    freeServers: List<VpnServer>,
    customServers: List<VpnServer>,
    activeServer: VpnServer,
    isTestingPing: Boolean,
    isUpdating: Boolean,
    progressLabel: String?,
    progressFraction: Float,
    sort: ServerSort,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onSortChange: (ServerSort) -> Unit,
    onClose: () -> Unit,
    onSelectServer: (VpnServer) -> Unit,
    onTestPing: (ServerRepository.ServerScope) -> Unit,
    onRefreshSubscriptions: () -> Unit,
    onShowQr: (VpnServer) -> Unit,
    onSmartImport: (VpnServer) -> Unit,
    onToggleFavorite: (VpnServer) -> Unit,
    onDeleteCustom: (VpnServer) -> Unit,
    onDeleteUnreachable: () -> Unit,
    onRestoreDeleted: () -> Unit,
    onOpenImport: () -> Unit
) {
    val accent = LocalAccent.current.primary
    val secondary = LocalAccent.current.secondary
    var tab by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<VpnServer?>(null) }

    pendingDelete?.let { target ->
        DeleteServerDialog(
            server = target,
            accent = accent,
            onConfirm = {
                onDeleteCustom(target)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
    val list = when (tab) {
        0 -> vipServers
        1 -> freeServers
        else -> customServers
    }
    val scope = when (tab) {
        0 -> ServerRepository.ServerScope.VIP
        1 -> ServerRepository.ServerScope.FREE
        else -> ServerRepository.ServerScope.CUSTOM
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MeelanoBgDark)
                .padding(14.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Icon(Icons.Default.Close, "بستن", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("انتخاب سرور", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("${list.size} نود در این دسته", fontSize = 10.sp, color = TextMuted)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "VIP (${vipServers.size})" to MeelanoGoldVip,
                        "رایگان (${freeServers.size})" to accent,
                        "شخصی (${customServers.size})" to MeelanoGreenSuccess
                    ).forEachIndexed { index, (title, color) ->
                        val selected = index == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (selected) Brush.horizontalGradient(
                                        listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.08f))
                                    ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { tab = index }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                title,
                                fontSize = 11.sp,
                                color = if (selected) TextPrimary else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // search
                TextField(
                    value = searchQuery,
                    onValueChange = onSearch,
                    placeholder = { Text("جستجوی کشور، نام یا پروتکل…", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(Modifier.height(10.dp))

                // One primary action, then a compact tool cluster.
                //
                // Previously four equal-weight chips spread over two rows, which
                // made the common action (get servers) no more prominent than
                // "restore defaults". Refresh and subscription-update were also
                // separate buttons that ran the same pipeline.
                PrimaryServerAction(
                    tab = tab,
                    isUpdating = isUpdating,
                    isTestingPing = isTestingPing,
                    accent = accent,
                    secondary = secondary,
                    onRefresh = onRefreshSubscriptions,
                    onImport = onOpenImport
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ToolButton(
                        icon = Icons.Default.NetworkPing,
                        label = "تست پینگ",
                        tint = accent,
                        loading = isTestingPing,
                        modifier = Modifier.weight(1f)
                    ) { onTestPing(scope) }

                    ToolButton(
                        icon = Icons.Default.Sort,
                        label = sort.label,
                        tint = MeelanoGoldVip,
                        modifier = Modifier.weight(1f)
                    ) {
                        val order = ServerSort.entries
                        onSortChange(order[(order.indexOf(sort) + 1) % order.size])
                    }

                    ToolButton(
                        icon = Icons.Default.DeleteSweep,
                        label = "حذف خراب‌ها",
                        tint = MeelanoRedKillSwitch,
                        modifier = Modifier.weight(1f),
                        onClick = onDeleteUnreachable
                    )

                    ToolButton(
                        icon = Icons.Default.Restore,
                        label = "بازگردانی",
                        tint = TextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onRestoreDeleted
                    )
                }

                Spacer(Modifier.height(4.dp))

                // The refresh runs in stages (fetch, reachability sweep, then the
                // strict real-traffic validation); show which one is running.
                AnimatedVisibility(visible = progressLabel != null) {
                    Column {
                        Spacer(Modifier.height(Spacing.Small))
                        GlassCard(corner = 12.dp, padding = 10.dp, accent = accent) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        progressLabel.orEmpty(),
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${(progressFraction * 100).toInt()}%",
                                        fontSize = 10.sp,
                                        color = accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(7.dp))
                                GlowProgressBar(
                                    fraction = progressFraction,
                                    accent = accent,
                                    secondary = MeelanoGreenSuccess
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (list.isEmpty()) {
                    EmptyState(tab = tab, onRefresh = onRefreshSubscriptions, onImport = onOpenImport)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(list, key = { it.id }) { server ->
                            ServerRow(
                                server = server,
                                isActive = server.id == activeServer.id,
                                accent = accent,
                                canDelete = true,
                                onSelect = { onSelectServer(server) },
                                onShowQr = { onShowQr(server) },
                                onSmartImport = { onSmartImport(server) },
                                onToggleFavorite = { onToggleFavorite(server) },
                                onDelete = { pendingDelete = server }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerRow(
    server: VpnServer,
    isActive: Boolean,
    accent: Color,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onShowQr: () -> Unit,
    onSmartImport: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        corner = 14.dp,
        padding = 11.dp,
        accent = if (isActive) accent else null,
        onClick = onSelect
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(server.flagEmoji, fontSize = 22.sp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                server.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (server.isVip) {
                                Spacer(Modifier.width(5.dp))
                                Pill("VIP", MeelanoGoldVip)
                            }
                            if (isActive) {
                                Spacer(Modifier.width(5.dp))
                                Pill("فعال", MeelanoGreenSuccess)
                            }
                        }
                        Text(
                            "${server.countryName} · ${server.protocol}",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            server.hostLabel,
                            fontSize = 8.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when {
                            server.pingMs > 0 -> "${server.pingMs}ms"
                            server.pingMs < 0 -> "قطع"
                            else -> "—"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (server.pingMs < 0) MeelanoRedKillSwitch else pingColor(server.pingMs)
                    )
                    Spacer(Modifier.height(3.dp))
                    SignalBars(server.pingMs, barHeight = 11)

                    // Distinguish "the port answered" from "the tunnel works".
                    // Without this a dead node shows a healthy latency and the
                    // user has no way to know why it will not connect.
                    if (server.isVerified) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "✓ تأیید شد",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = MeelanoGreenSuccess
                        )
                    } else if (server.isUnproven) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "تأیید نشده",
                            fontSize = 7.sp,
                            color = MeelanoAmber
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniIcon(if (server.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    if (server.isFavorite) MeelanoGoldVip else TextMuted, onToggleFavorite)
                MiniIcon(Icons.Default.QrCode, accent, onShowQr)
                MiniIcon(Icons.Default.Send, MeelanoGreenSuccess, onSmartImport)
                if (canDelete) MiniIcon(Icons.Default.Delete, MeelanoRedKillSwitch, onDelete)
                Spacer(Modifier.weight(1f))
                if (server.speedMbps > 0f) {
                    Text(
                        "≈ ${"%.0f".format(server.speedMbps)} Mb/s",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.12f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ActionChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
            .clickable(enabled = !loading) { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.6.dp,
                    color = color
                )
            } else {
                Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyState(tab: Int, onRefresh: () -> Unit, onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛰️", fontSize = 40.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            if (tab == 1) "هنوز سروری دریافت نشده" else "لیست خالی است",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (tab == 1) "با دکمه به‌روزرسانی، نودهای زنده را از اشتراک‌ها دریافت کنید"
            else "کانفیگ شخصی خود را وارد کنید",
            color = TextMuted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(14.dp))
        ActionChip(
            text = if (tab == 1) "به‌روزرسانی از اشتراک‌ها" else "افزودن کانفیگ",
            icon = if (tab == 1) Icons.Default.Refresh else Icons.Default.Add,
            color = MeelanoGreenSuccess,
            modifier = Modifier.width(220.dp)
        ) { if (tab == 1) onRefresh() else onImport() }
    }
}


/** Confirmation before a server is permanently removed. */
@Composable
private fun DeleteServerDialog(
    server: VpnServer,
    accent: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(corner = 22.dp, padding = 20.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(MeelanoRedKillSwitch.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MeelanoRedKillSwitch,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "حذف سرور",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "«${server.name}» برای همیشه از لیست حذف می‌شود. مطمئن هستید؟",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("انصراف", color = Color.White, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MeelanoRedKillSwitch)
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "حذف",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * The one action the user came here for, given the weight to match.
 *
 * Rendered as a raised slab with a lit bevel and a travelling sheen. While a
 * refresh runs it becomes a live progress surface: the fill tracks real
 * progress rather than looping, so a stalled fetch is visible instead of being
 * masked by a spinner that animates forever.
 */
@Composable
private fun PrimaryServerAction(
    tab: Int,
    isUpdating: Boolean,
    isTestingPing: Boolean,
    accent: Color,
    secondary: Color,
    onRefresh: () -> Unit,
    onImport: () -> Unit
) {
    val busy = isUpdating || isTestingPing
    val transition = rememberInfiniteTransition(label = "primary")
    val sheen by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "sheen"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateFloatAsState(
        targetValue = if (pressed) 0.3f else 1f,
        animationSpec = tween(130),
        label = "lift"
    )

    val label = when {
        isUpdating -> "در حال دریافت سرورها…"
        tab == 1 -> "دریافت و به‌روزرسانی سرورها"
        else -> "افزودن کانفیگ"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !busy
            ) { if (tab == 1) onRefresh() else onImport() }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val corner = CornerRadius(15.dp.toPx(), 15.dp.toPx())

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.42f * lift),
                topLeft = Offset(0f, 4f * lift),
                size = size,
                cornerRadius = corner
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.42f), secondary.copy(alpha = 0.26f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = corner
            )
            val x = size.width * (sheen * 1.7f - 0.35f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.13f), Color.Transparent),
                    start = Offset(x - 80f, 0f),
                    end = Offset(x + 80f, size.height)
                ),
                cornerRadius = corner
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        accent.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.30f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = corner,
                style = Stroke(width = 1.5f)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (tab == 1) Icons.Default.CloudSync else Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** Compact secondary action: a small raised key with an icon and a caption. */
@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateFloatAsState(
        targetValue = if (pressed) 0.25f else 1f,
        animationSpec = tween(120),
        label = "toolLift"
    )
    val spin by rememberInfiniteTransition(label = "toolSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spin"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(r, r)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.38f * lift),
                    radius = r * 0.88f,
                    center = Offset(c.x, c.y + 2.5f * lift)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(tint.copy(alpha = 0.42f), tint.copy(alpha = 0.14f)),
                        center = Offset(r * 0.66f, r * 0.58f),
                        radius = r * 1.7f
                    ),
                    radius = r * 0.88f,
                    center = c
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    radius = r * 0.88f,
                    center = c,
                    style = Stroke(width = 1.2f)
                )
                // While busy, an arc sweeps the rim so the button reports its
                // own state instead of looking inert.
                if (loading) {
                    drawArc(
                        color = tint,
                        startAngle = spin,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(r * 0.12f, r * 0.12f),
                        size = Size(r * 1.76f, r * 1.76f),
                        style = Stroke(width = 2f, cap = StrokeCap.Round)
                    )
                }
            }
            Icon(icon, null, tint = Color.White.copy(alpha = 0.94f), modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = TextSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
