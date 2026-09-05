package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.example.data.model.ConnectionQuality
import com.example.data.repository.ServerRepository
import com.example.ui.components.AuroraBackground
import com.example.ui.components.ConnectionRadar
import com.example.ui.components.GlassCard
import com.example.ui.components.HealthRing
import com.example.ui.components.MeelanoHexagonLogo
import com.example.ui.components.Pill
import com.example.ui.components.PowerButton3D
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Spacing
import com.example.ui.components.SignalBars
import com.example.ui.components.StatTile
import com.example.ui.components.TrafficLineChart
import com.example.ui.modals.ImportConfigDialog
import com.example.ui.modals.LiveLogConsoleDialog
import com.example.ui.modals.QrCodeDialog
import com.example.ui.modals.SecurityLockScreen
import com.example.ui.modals.ServerListModal
import com.example.ui.modals.SettingsModal
import com.example.ui.modals.SmartImportFallbackDialog
import com.example.ui.modals.SplitTunnelingDialog
import com.example.ui.theme.LocalAccent
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoPurpleActive
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.pingColor
import com.example.ui.viewmodel.MainViewModel
import com.example.vpn.VpnConnectionState

@Composable
fun MainDashboardScreen(
    viewModel: MainViewModel,
    onRequestVpnPermission: () -> Unit,
    onRequestBiometric: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val accentPreset = LocalAccent.current
    val accent = accentPreset.primary
    val scrollState = rememberScrollState()

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val liveStats by viewModel.liveStats.collectAsStateWithLifecycle()
    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val routingMode by viewModel.routingMode.collectAsStateWithLifecycle()
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
    val dashboardTab by viewModel.dashboardTab.collectAsStateWithLifecycle()
    val isSoundMuted by viewModel.isSoundMuted.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val isLocked by viewModel.securityManager.isLocked.collectAsStateWithLifecycle()

    val isConnected = connectionState == VpnConnectionState.CONNECTED

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        AuroraBackground(
            accent = accent,
            secondary = accentPreset.secondary,
            energised = isConnected,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.Screen),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(Spacing.Small))

                TopBar(
                    connectionState = connectionState,
                    accent = accent,
                    isSoundMuted = isSoundMuted,
                    onLock = { viewModel.securityManager.lock() },
                    onToggleSound = { viewModel.toggleSoundMute() }
                )

                Spacer(Modifier.height(Spacing.Large))

                ActiveServerCard(
                    name = activeServer.name,
                    country = activeServer.countryName,
                    flag = activeServer.flagEmoji,
                    protocol = activeServer.protocol,
                    host = activeServer.hostLabel,
                    pingMs = if (isConnected && liveStats.pingMs > 0) liveStats.pingMs else activeServer.pingMs,
                    accent = accent,
                    onClick = { viewModel.openServersModal() },
                    onFastest = {
                        viewModel.connectWithBestEffort(context, onRequestVpnPermission)
                    }
                )

                AnimatedVisibility(
                    visible = connectionState == VpnConnectionState.FAILED && lastError != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(Spacing.Medium))
                        GlassCard(accent = MeelanoRedKillSwitch, corner = 14.dp, padding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MeelanoRedKillSwitch,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    lastError.orEmpty(),
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.Large))

                PowerButton3D(
                    state = connectionState,
                    accent = accent,
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.requestToggle(context, onRequestVpnPermission)
                    }
                )

                Spacer(Modifier.height(Spacing.Medium))

                ConnectionRadar(
                    connected = isConnected,
                    accent = accent,
                    throughputMbps = liveStats.downloadMbps
                )

                Spacer(Modifier.height(Spacing.Small))

                LiveSpeedRow(
                    down = liveStats.downloadMbps,
                    up = liveStats.uploadMbps,
                    accent = accent
                )

                Spacer(Modifier.height(Spacing.Large))

                SectionHeader(
                    title = "پایش زنده",
                    accent = accent,
                    caption = if (isConnected) "در حال انتقال" else "غیرفعال"
                )

                Spacer(Modifier.height(Spacing.Medium))

                DashboardTabs(
                    selected = dashboardTab,
                    accent = accent,
                    onSelect = { viewModel.setDashboardTab(it) }
                )

                Spacer(Modifier.height(Spacing.Medium))

                when (dashboardTab) {
                    0 -> NetworkStatusPanel(
                        accent = accent,
                        connected = isConnected,
                        pingMs = if (isConnected) liveStats.pingMs else activeServer.pingMs,
                        tunnelIp = liveStats.tunnelIp,
                        remoteHost = liveStats.remoteHost,
                        protocol = if (isConnected) liveStats.activeProtocol else activeServer.protocol,
                        encryption = liveStats.encryption,
                        dnsQueries = liveStats.dnsQueries,
                        flows = liveStats.activeFlows,
                        uptime = liveStats.uptimeLabel,
                        downloadedMb = liveStats.totalDownloadedMb,
                        uploadedMb = liveStats.totalUploadedMb
                    )

                    1 -> SecurityToolsPanel(
                        accent = accent,
                        routingTitle = routingMode.title,
                        routingBadge = routingMode.badge,
                        killSwitch = killSwitchEnabled,
                        onOpenSettings = { viewModel.openSettingsModal() },
                        onOpenSplit = { viewModel.openSplitTunneling() },
                        onOpenLogs = { viewModel.openLogsConsole() },
                        onOpenImport = { viewModel.openImport() },
                        onToggleKillSwitch = { viewModel.toggleKillSwitch() }
                    )

                    else -> TrafficLineChart(stats = liveStats, accent = accent)
                }

                Spacer(Modifier.height(Spacing.Large))

                SectionHeader(title = "دسترسی سریع", accent = accent)

                Spacer(Modifier.height(Spacing.Medium))

                QuickActionsRow(
                    accent = accent,
                    onSettings = { viewModel.openSettingsModal() },
                    onServers = { viewModel.openServersModal() },
                    onLogs = { viewModel.openLogsConsole() }
                )

                Spacer(Modifier.height(Spacing.Large))
                Footer(accent = accent, connected = isConnected)
                Spacer(Modifier.height(Spacing.Large))
            }

            Modals(
                viewModel = viewModel,
                onRequestBiometric = onRequestBiometric,
                isLocked = isLocked
            )
        }
    }
}

// ---------------------------------------------------------------- top bar

@Composable
private fun TopBar(
    connectionState: VpnConnectionState,
    accent: Color,
    isSoundMuted: Boolean,
    onLock: () -> Unit,
    onToggleSound: () -> Unit
) {
    val statusColor by animateColorAsState(
        when (connectionState) {
            VpnConnectionState.CONNECTED -> MeelanoGreenSuccess
            VpnConnectionState.FAILED -> MeelanoRedKillSwitch
            VpnConnectionState.DISCONNECTED -> TextSecondary
            else -> accent
        },
        tween(500),
        label = "status"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIconButton(Icons.Default.Lock, "قفل امنیتی", onLock)
            CircleIconButton(
                if (isSoundMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                "صدا",
                onToggleSound
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(MeelanoGoldVip.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .border(1.dp, MeelanoGoldVip.copy(alpha = 0.45f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("VIP 👑", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MeelanoGoldVip)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "MEELANO PRO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(connectionState.persName, fontSize = 10.sp, color = statusColor)
                }
            }
            MeelanoHexagonLogo(
                size = 42.dp,
                glowing = connectionState == VpnConnectionState.CONNECTED,
                accent = accent
            )
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Icon(icon, contentDescription = description, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

// ---------------------------------------------------------------- server card

@Composable
private fun ActiveServerCard(
    name: String,
    country: String,
    flag: String,
    protocol: String,
    host: String,
    pingMs: Int,
    accent: Color,
    onClick: () -> Unit,
    onFastest: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), accent = accent, onClick = onClick, padding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(flag, fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$country · $protocol",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(host, fontSize = 9.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (pingMs > 0) "$pingMs" else "—",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pingColor(pingMs)
                    )
                    Text("ms", fontSize = 8.sp, color = TextMuted)
                }
                Spacer(Modifier.width(8.dp))
                SignalBars(pingMs)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f))
                        .clickable { onFastest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bolt, "سریع‌ترین سرور", tint = accent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- speed row

@Composable
private fun LiveSpeedRow(down: Float, up: Float, accent: Color) {
    val animatedDown by animateFloatAsState(down, tween(500), label = "down")
    val animatedUp by animateFloatAsState(up, tween(500), label = "up")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            title = "دانلود",
            value = "${"%.1f".format(animatedDown)} Mb/s",
            icon = Icons.Default.Download,
            tint = accent,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            title = "آپلود",
            value = "${"%.1f".format(animatedUp)} Mb/s",
            icon = Icons.Default.Upload,
            tint = MeelanoGreenSuccess,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------- tabs

@Composable
private fun DashboardTabs(selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    val titles = listOf("وضعیت شبکه", "ابزار امنیتی", "نمودار ترافیک")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        titles.forEachIndexed { index, title ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.10f))
                        ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextSecondary
                )
            }
        }
    }
}

// ---------------------------------------------------------------- panels

@Composable
private fun NetworkStatusPanel(
    accent: Color,
    connected: Boolean,
    pingMs: Int,
    tunnelIp: String,
    remoteHost: String,
    protocol: String,
    encryption: String,
    dnsQueries: Long,
    flows: Int,
    uptime: String,
    downloadedMb: Float,
    uploadedMb: Float
) {
    val quality = when {
        !connected -> ConnectionQuality.UNKNOWN
        pingMs <= 0 -> ConnectionQuality.UNKNOWN
        pingMs < 90 -> ConnectionQuality.EXCELLENT
        pingMs < 180 -> ConnectionQuality.GOOD
        pingMs < 320 -> ConnectionQuality.FAIR
        else -> ConnectionQuality.POOR
    }

    Column(Modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth(), accent = accent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthRing(
                    fraction = quality.bars / 4f,
                    color = pingColor(pingMs),
                    label = quality.label,
                    value = if (pingMs > 0) "$pingMs" else "—"
                )
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    InfoRow("IP تونل", tunnelIp)
                    InfoRow("نود خروجی", remoteHost)
                    InfoRow("پروتکل", protocol)
                    InfoRow("رمزنگاری", encryption)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("مدت اتصال", uptime, Icons.Default.Timer, accent, Modifier.weight(1f))
            StatTile("جریان فعال", "$flows", Icons.Default.Hub, MeelanoPurpleActive, Modifier.weight(1f))
            StatTile("پرس‌وجوی DNS", "$dnsQueries", Icons.Default.Dns, MeelanoGreenSuccess, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                "کل دریافت",
                "${"%.1f".format(downloadedMb)} MB",
                Icons.Default.Download,
                accent,
                Modifier.weight(1f)
            )
            StatTile(
                "کل ارسال",
                "${"%.1f".format(uploadedMb)} MB",
                Icons.Default.Upload,
                MeelanoGreenSuccess,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Text(
            value,
            fontSize = 11.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SecurityToolsPanel(
    accent: Color,
    routingTitle: String,
    routingBadge: String,
    killSwitch: Boolean,
    onOpenSettings: () -> Unit,
    onOpenSplit: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenImport: () -> Unit,
    onToggleKillSwitch: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        GlassCard(modifier = Modifier.fillMaxWidth(), accent = accent, onClick = onOpenSettings) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Router, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("حالت مسیریابی", fontSize = 10.sp, color = TextSecondary)
                        Text(routingTitle, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Pill(routingBadge, accent)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard(
                "Kill Switch",
                if (killSwitch) "فعال — بدون نشتی" else "غیرفعال",
                Icons.Default.Shield,
                if (killSwitch) MeelanoRedKillSwitch else TextMuted,
                Modifier.weight(1f),
                onToggleKillSwitch
            )
            ToolCard(
                "تونل تفکیکی",
                "اپ‌های داخلی مستقیم",
                Icons.Default.Security,
                MeelanoPurpleActive,
                Modifier.weight(1f),
                onOpenSplit
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard(
                "کنسول لاگ زنده",
                "رصد بسته‌ها و خطاها",
                Icons.Default.Terminal,
                MeelanoGreenSuccess,
                Modifier.weight(1f),
                onOpenLogs
            )
            ToolCard(
                "افزودن کانفیگ",
                "لینک یا اشتراک جدید",
                Icons.Default.VpnKey,
                accent,
                Modifier.weight(1f),
                onOpenImport
            )
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier, corner = 14.dp, padding = 12.dp, onClick = onClick) {
        Column {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 9.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickActionsRow(
    accent: Color,
    onSettings: () -> Unit,
    onServers: () -> Unit,
    onLogs: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ToolCard("تنظیمات", "امنیت و مسیریابی", Icons.Default.Tune, MeelanoPurpleActive, Modifier.weight(1f), onSettings)
        ToolCard("سرورها", "انتخاب نود", Icons.Default.Language, accent, Modifier.weight(1f), onServers)
        ToolCard("لاگ", "خروجی هسته", Icons.Default.NetworkCheck, MeelanoGreenSuccess, Modifier.weight(1f), onLogs)
    }
}

@Composable
private fun Footer(accent: Color, connected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MeelanoHexagonLogo(size = 16.dp, glowing = connected, accent = accent)
                Spacer(Modifier.width(6.dp))
                Text(
                    "MEELANO STUDIO DESIGN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Designed by Milad Yaghoobi", fontSize = 9.sp, color = TextMuted, modifier = Modifier.alpha(0.8f))
    }
}

// ---------------------------------------------------------------- modals

@Composable
private fun Modals(
    viewModel: MainViewModel,
    onRequestBiometric: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    isLocked: Boolean
) {
    val context = LocalContext.current

    val isServersModalOpen by viewModel.isServersModalOpen.collectAsStateWithLifecycle()
    val isSettingsModalOpen by viewModel.isSettingsModalOpen.collectAsStateWithLifecycle()
    val isLogsConsoleOpen by viewModel.isLogsConsoleOpen.collectAsStateWithLifecycle()
    val isSplitTunnelingOpen by viewModel.isSplitTunnelingOpen.collectAsStateWithLifecycle()
    val isImportOpen by viewModel.isImportOpen.collectAsStateWithLifecycle()
    val selectedServerForQr by viewModel.selectedServerForQr.collectAsStateWithLifecycle()
    val smartImportFallbackOpen by viewModel.smartImportFallbackOpen.collectAsStateWithLifecycle()

    if (isServersModalOpen) {
        val vip by viewModel.vipServers.collectAsStateWithLifecycle()
        val free by viewModel.freeServers.collectAsStateWithLifecycle()
        val custom by viewModel.customServers.collectAsStateWithLifecycle()
        val active by viewModel.activeServer.collectAsStateWithLifecycle()
        val testing by viewModel.isTestingPing.collectAsStateWithLifecycle()
        val updating by viewModel.isUpdatingGitHub.collectAsStateWithLifecycle()
        val progress by viewModel.updateProgress.collectAsStateWithLifecycle()
        val sort by viewModel.serverSort.collectAsStateWithLifecycle()
        val query by viewModel.searchQuery.collectAsStateWithLifecycle()

        ServerListModal(
            vipServers = viewModel.sortedServers(vip),
            freeServers = viewModel.sortedServers(free),
            customServers = viewModel.sortedServers(custom),
            activeServer = active,
            isTestingPing = testing,
            isUpdating = updating,
            progressLabel = progress?.stage,
            progressFraction = progress?.fraction ?: 0f,
            sort = sort,
            searchQuery = query,
            onSearch = { viewModel.setSearchQuery(it) },
            onSortChange = { viewModel.setSort(it) },
            onClose = { viewModel.closeServersModal() },
            onSelectServer = {
                viewModel.selectServer(it, context)
                viewModel.closeServersModal()
            },
            onTestPing = { viewModel.testPings(it) },
            onRefreshSubscriptions = { viewModel.refreshSubscriptions() },
            onShowQr = { viewModel.showQrCode(it) },
            onSmartImport = { viewModel.triggerSmartImport(context, it.configLink) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDeleteCustom = { viewModel.deleteServer(it) },
            onDeleteUnreachable = { viewModel.deleteUnreachableServers() },
            onRestoreDeleted = { viewModel.restoreDeletedServers() },
            onOpenImport = { viewModel.openImport() }
        )
    }

    if (isSettingsModalOpen) {
        val routing by viewModel.routingMode.collectAsStateWithLifecycle()
        val protocol by viewModel.coreProtocolFilter.collectAsStateWithLifecycle()
        val kill by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
        val failover by viewModel.smartFailoverEnabled.collectAsStateWithLifecycle()
        val autoConnect by viewModel.autoConnectEnabled.collectAsStateWithLifecycle()
        val ipv6 by viewModel.ipv6Enabled.collectAsStateWithLifecycle()
        val haptics by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
        val biometric by viewModel.biometricEnabled.collectAsStateWithLifecycle()
        val lockStart by viewModel.lockOnStart.collectAsStateWithLifecycle()
        val themeAccent by viewModel.themeAccent.collectAsStateWithLifecycle()
        val dns1 by viewModel.dnsPrimary.collectAsStateWithLifecycle()
        val dns2 by viewModel.dnsSecondary.collectAsStateWithLifecycle()
        val subs by viewModel.subscriptions.collectAsStateWithLifecycle()
        val active by viewModel.activeServer.collectAsStateWithLifecycle()

        SettingsModal(
            routingMode = routing,
            protocolFilter = protocol,
            killSwitchEnabled = kill,
            smartFailoverEnabled = failover,
            autoConnectEnabled = autoConnect,
            ipv6Enabled = ipv6,
            hapticsEnabled = haptics,
            biometricEnabled = biometric,
            lockOnStart = lockStart,
            themeAccent = themeAccent,
            dnsPrimary = dns1,
            dnsSecondary = dns2,
            subscriptions = subs.toList(),
            activeConfigLink = active.configLink,
            onClose = { viewModel.closeSettingsModal() },
            onRoutingModeChange = { viewModel.setRoutingMode(it) },
            onProtocolFilterChange = { viewModel.setCoreProtocolFilter(it) },
            onToggleKillSwitch = { viewModel.toggleKillSwitch() },
            onToggleSmartFailover = { viewModel.toggleSmartFailover() },
            onToggleAutoConnect = { viewModel.toggleAutoConnect() },
            onToggleIpv6 = { viewModel.toggleIpv6() },
            onToggleHaptics = { viewModel.toggleHaptics() },
            onToggleBiometric = { viewModel.toggleBiometric() },
            onToggleLockOnStart = { viewModel.toggleLockOnStart() },
            onAccentChange = { viewModel.setThemeAccent(it) },
            onDnsChange = { a, b -> viewModel.setDns(a, b) },
            onAddSubscription = { viewModel.addSubscription(it) },
            onRemoveSubscription = { viewModel.removeSubscription(it) },
            onOpenLogs = { viewModel.openLogsConsole() },
            onOpenSplitTunneling = { viewModel.openSplitTunneling() },
            onLockApp = {
                viewModel.closeSettingsModal()
                viewModel.securityManager.lock()
            }
        )
    }

    if (isLogsConsoleOpen) {
        val logs by viewModel.logs.collectAsStateWithLifecycle()
        LiveLogConsoleDialog(
            logs = logs,
            onClear = { viewModel.clearLogs() },
            onClose = { viewModel.closeLogsConsole() }
        )
    }

    if (isSplitTunnelingOpen) {
        val apps by viewModel.bypassApps.collectAsStateWithLifecycle()
        SplitTunnelingDialog(
            bypassApps = apps,
            onToggleApp = { viewModel.toggleBypassApp(it) },
            onClose = { viewModel.closeSplitTunneling() }
        )
    }

    if (isImportOpen) {
        ImportConfigDialog(
            onImport = { viewModel.importConfigs(it) },
            onImportClipboard = { viewModel.importFromClipboard(context) },
            onClose = { viewModel.closeImport() }
        )
    }

    selectedServerForQr?.let { server ->
        QrCodeDialog(server = server, onClose = { viewModel.closeQrCode() })
    }

    if (smartImportFallbackOpen) {
        SmartImportFallbackDialog(onClose = { viewModel.closeSmartImportFallback() })
    }

    if (isLocked) {
        val pin by viewModel.securityManager.currentPinInput.collectAsStateWithLifecycle()
        val error by viewModel.securityManager.pinError.collectAsStateWithLifecycle()
        SecurityLockScreen(
            currentPin = pin,
            pinError = error,
            biometricAvailable = viewModel.securityManager.biometricAvailable,
            onDigitPress = { viewModel.securityManager.appendPinDigit(it) },
            onDeleteDigit = { viewModel.securityManager.deletePinDigit() },
            onBiometricPress = {
                onRequestBiometric(
                    { viewModel.securityManager.onBiometricSucceeded() },
                    { message -> viewModel.securityManager.onBiometricFailed(message) }
                )
            }
        )
    }
}
