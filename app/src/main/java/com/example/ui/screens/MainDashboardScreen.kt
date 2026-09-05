package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
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
import com.example.ui.components.GlassCard
import com.example.ui.components.HealthRing
import com.example.ui.components.MeelanoShieldLogo
import com.example.ui.components.Pill
import com.example.ui.components.ConnectOrb
import com.example.ui.components.GlowDot
import com.example.ui.components.ServerPortalButton
import com.example.ui.components.ServerScanOverlay
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Spacing
import com.example.ui.components.SignalBars
import com.example.ui.components.StatTile
import com.example.ui.components.TrafficLineChart
import com.example.ui.modals.ImportConfigDialog
import com.example.ui.modals.LiveLogConsoleDialog
import com.example.ui.modals.NeedsServersDialog
import com.example.ui.modals.QrCodeDialog
import com.example.ui.modals.SecurityLockScreen
import com.example.ui.modals.ServerListModal
import com.example.ui.modals.SettingsModal
import com.example.ui.modals.SpeedTestDialog
import com.example.ui.modals.SmartImportFallbackDialog
import com.example.ui.modals.SplitTunnelingDialog
import com.example.ui.theme.LocalAccent
import com.example.ui.theme.MeelanoIconCyan
import com.example.ui.theme.MeelanoIconViolet
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoPurpleActive
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.pingColor
import com.example.util.SoundEngine
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
    val secondary = accentPreset.secondary
    val scrollState = rememberScrollState()

    val isTestingPing by viewModel.isTestingPing.collectAsStateWithLifecycle()
    val refreshStage by viewModel.updateProgress.collectAsStateWithLifecycle()
    val speedTestState by viewModel.speedTest.collectAsStateWithLifecycle()
    val needsServers by viewModel.needsServers.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val liveStats by viewModel.liveStats.collectAsStateWithLifecycle()
    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val vipServerList by viewModel.vipServers.collectAsStateWithLifecycle()
    val freeServerList by viewModel.freeServers.collectAsStateWithLifecycle()
    val customServerList by viewModel.customServers.collectAsStateWithLifecycle()
    val totalServerCount = vipServerList.size + freeServerList.size + customServerList.size
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
                    secondary = secondary,
                    isSoundMuted = isSoundMuted,
                    onLock = { viewModel.securityManager.lock() },
                    onToggleSound = { viewModel.toggleSoundMute() }
                )

                Spacer(Modifier.height(Spacing.Large))

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

                // One control for the server: identity, live latency, node count
                // and the route into the list. Previously an ActiveServerCard and
                // a ServerPortalButton sat on the same screen doing the same job.
                ServerPortalButton(
                    serverName = activeServer.name,
                    country = activeServer.countryName,
                    flag = activeServer.flagEmoji,
                    protocol = activeServer.protocol,
                    serverCount = totalServerCount,
                    pingMs = if (isConnected && liveStats.pingMs > 0) liveStats.pingMs else activeServer.pingMs,
                    isVerified = activeServer.isVerified,
                    connected = isConnected,
                    accent = accent,
                    secondary = secondary,
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundEngine.play(SoundEngine.Cue.TAP)
                        viewModel.openServersModal()
                    },
                    onAutoSelect = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundEngine.play(SoundEngine.Cue.SCAN)
                        viewModel.connectWithBestEffort(context, onRequestVpnPermission)
                    }
                )

                Spacer(Modifier.height(Spacing.Large))

                ConnectOrb(
                    state = connectionState,
                    accent = accent,
                    secondary = secondary,
                    onClick = {
                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundEngine.play(SoundEngine.Cue.TAP)
                        viewModel.requestToggle(context, onRequestVpnPermission)
                    }
                )

                Spacer(Modifier.height(Spacing.Large))

                Spacer(Modifier.height(Spacing.Large))

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
                        secondary = secondary,
                        routingTitle = routingMode.title,
                        routingBadge = routingMode.badge,
                        killSwitch = killSwitchEnabled,
                        onOpenSettings = { viewModel.openSettingsModal() },
                        onOpenSplit = { viewModel.openSplitTunneling() },
                        onOpenLogs = { viewModel.openLogsConsole() },
                        onOpenImport = { viewModel.openImport() },
                        onOpenServers = { viewModel.openServersModal() },
                        onSpeedTest = { viewModel.runSpeedTest() },
                        onToggleKillSwitch = { viewModel.toggleKillSwitch() }
                    )

                    else -> TrafficLineChart(stats = liveStats, accent = accent)
                }

                Spacer(Modifier.height(Spacing.Large))
                Footer(accent = accent, connected = isConnected)
                Spacer(Modifier.height(Spacing.Large))
            }

            // Full-screen "finding the best route" experience.
            // Shown for the ping sweep and for the multi-stage subscription
            // refresh, which is the long one the user actually waits on.
            ServerScanOverlay(
                visible = isTestingPing || refreshStage != null,
                accent = accent,
                secondary = secondary,
                title = refreshStage?.stage ?: "در حال یافتن بهترین مسیر",
                caption = if (refreshStage != null) {
                    "${refreshStage!!.done} از ${refreshStage!!.total}"
                } else {
                    "تست هم‌زمان تمام نودها…"
                },
                progress = refreshStage?.fraction
            )

            if (needsServers) {
                NeedsServersDialog(
                    accent = accent,
                    secondary = secondary,
                    onOpenServers = {
                        viewModel.dismissNeedsServers()
                        viewModel.openServersModal()
                    },
                    onFetchNow = {
                        viewModel.dismissNeedsServers()
                        viewModel.refreshSubscriptions()
                    },
                    onDismiss = { viewModel.dismissNeedsServers() }
                )
            }

            // Live throughput readout, shown while a measurement runs and left
            // on screen with the result until dismissed.
            speedTestState?.let { speed ->
                SpeedTestDialog(
                    state = speed,
                    accent = accent,
                    secondary = secondary,
                    onDismiss = { viewModel.dismissSpeedTest() }
                )
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
    secondary: Color,
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

    // Brand on one side, a single grouped utility cluster on the other. The old
    // layout scattered three separate groups across the row, which read as
    // clutter and gave the icons no obvious home.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ---- utility cluster: one capsule, clearly a toolbar ----
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapsuleIcon(Icons.Default.Lock, "قفل امنیتی", accent, onLock)
            Box(
                Modifier
                    .size(width = 1.dp, height = 16.dp)
                    .background(Color.White.copy(alpha = 0.10f))
            )
            CapsuleIcon(
                if (isSoundMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                "صدا",
                if (isSoundMuted) TextMuted else accent,
                onToggleSound
            )
        }

        // ---- brand lockup: logo, name, and live status in one column ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The wordmark carries the icon's neon gradient rather than
                    // flat white, so the brand lockup and the launcher mark are
                    // recognisably the same identity.
                    Text(
                        "MEELANO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.2.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                listOf(MeelanoIconViolet, Color(0xFFEAF4FF), MeelanoIconCyan)
                            ),
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = accent.copy(alpha = 0.55f),
                                offset = Offset.Zero,
                                blurRadius = 18f
                            )
                        )
                    )
                    Spacer(Modifier.width(5.dp))
                    // A struck-metal badge rather than a flat pill: bevelled
                    // edge, gradient fill and a slow sheen that travels across
                    // the face, matching the chrome on the launcher icon.
                    VipBadge(accent = accent)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlowDot(
                        color = statusColor,
                        size = 5.dp,
                        pulsing = connectionState.isBusy
                    )
                    Text(
                        connectionState.persName,
                        fontSize = 9.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            MeelanoShieldLogo(
                size = 40.dp,
                glowing = connectionState == VpnConnectionState.CONNECTED,
                accent = accent
            )
        }
    }
}

/** A compact icon button sized for the header capsule. */
@Composable
private fun CapsuleIcon(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(17.dp))
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


// ---------------------------------------------------------------- speed row

/** Struck-metal VIP badge with a travelling sheen. */
@Composable
private fun VipBadge(accent: Color) {
    val transition = rememberInfiniteTransition(label = "vip")
    val sheen by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "sheen"
    )

    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .size(width = 38.dp, height = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.height / 2f
            val corner = androidx.compose.ui.geometry.CornerRadius(r, r)

            // Warm halo, so the badge reads as lit metal on the dark field.
            drawRoundRect(
                brush = Brush.radialGradient(
                    listOf(MeelanoGoldVip.copy(alpha = 0.35f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.75f
                ),
                cornerRadius = corner
            )
            // Body: bright along the top edge, falling to a deep amber below.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFE9A8),
                        MeelanoGoldVip,
                        Color(0xFF9A6510)
                    )
                ),
                cornerRadius = corner
            )
            // Travelling sheen.
            val x = size.width * (sheen * 1.8f - 0.4f)
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                    start = androidx.compose.ui.geometry.Offset(x - 14f, 0f),
                    end = androidx.compose.ui.geometry.Offset(x + 14f, size.height)
                ),
                cornerRadius = corner
            )
            // Bevel.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.75f), Color(0xFF6B4200).copy(alpha = 0.8f))
                ),
                cornerRadius = corner,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
            )
        }
        Text(
            "VIP",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = Color(0xFF3A2300)
        )
    }
}

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
            StatTile("مدت اتصال", uptime, Icons.Default.HourglassBottom, accent, Modifier.weight(1f))
            StatTile("جریان فعال", "$flows", Icons.Default.Lan, MeelanoPurpleActive, Modifier.weight(1f))
            StatTile("پرس‌وجوی DNS", "$dnsQueries", Icons.Default.TravelExplore, MeelanoGreenSuccess, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                "کل دریافت",
                "${"%.1f".format(downloadedMb)} MB",
                Icons.Default.CloudDownload,
                accent,
                Modifier.weight(1f)
            )
            StatTile(
                "کل ارسال",
                "${"%.1f".format(uploadedMb)} MB",
                Icons.Default.CloudUpload,
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
    secondary: Color,
    routingTitle: String,
    routingBadge: String,
    killSwitch: Boolean,
    onOpenSettings: () -> Unit,
    onOpenSplit: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenServers: () -> Unit,
    onSpeedTest: () -> Unit,
    onToggleKillSwitch: () -> Unit
) {
    // Grouped by intent so a user can predict where a control lives:
    //   مسیریابی  → where my traffic goes
    //   حفاظت     → what protects me
    //   مدیریت    → servers, configs, diagnostics
    Column(Modifier.fillMaxWidth()) {

        SectionHeader(title = "مسیریابی", accent = accent)
        Spacer(Modifier.height(Spacing.Small))

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

        Spacer(Modifier.height(Spacing.Large))

        SectionHeader(title = "حفاظت", accent = accent)
        Spacer(Modifier.height(Spacing.Small))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
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

        Spacer(Modifier.height(Spacing.Large))

        SectionHeader(title = "مدیریت", accent = accent)
        Spacer(Modifier.height(Spacing.Small))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            ToolCard(
                "سرورها",
                "انتخاب و تست نود",
                Icons.Default.Language,
                accent,
                Modifier.weight(1f),
                onOpenServers
            )
            ToolCard(
                "افزودن کانفیگ",
                "لینک یا اشتراک جدید",
                Icons.Default.VpnKey,
                secondary,
                Modifier.weight(1f),
                onOpenImport
            )
        }

        Spacer(Modifier.height(Spacing.Medium))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            ToolCard(
                "کنسول لاگ زنده",
                "رصد بسته‌ها و خطاها",
                Icons.Default.Terminal,
                MeelanoGreenSuccess,
                Modifier.weight(1f),
                onOpenLogs
            )
            ToolCard(
                "تنظیمات",
                "امنیت، DNS و ظاهر",
                Icons.Default.Tune,
                MeelanoPurpleActive,
                Modifier.weight(1f),
                onOpenSettings
            )
        }

        Spacer(Modifier.height(Spacing.Medium))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            ToolCard(
                "آزمایش سرعت",
                "اندازه‌گیری واقعی پهنای باند",
                Icons.Default.Speed,
                accent,
                Modifier.weight(1f),
                onSpeedTest
            )
            Spacer(Modifier.weight(1f))
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
    // Springy press feedback: the card dips and its icon tile lifts, which makes
    // every tap feel physical instead of instantaneous.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 620f),
        label = "toolPress"
    )
    val iconGlow by animateFloatAsState(
        targetValue = if (pressed) 0.34f else 0.15f,
        animationSpec = tween(180),
        label = "toolGlow"
    )

    GlassCard(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        corner = Spacing.CornerSmall,
        padding = 12.dp,
        interactionSource = interaction,
        onClick = onClick
    ) {
        Column {
            // The icon sits on a raised tile: a cast shadow beneath, a body lit
            // from the top-left, a bright bevel on the lit edges and a dark one
            // on the shaded edges. It reads as a key you can press.
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(38.dp)) {
                    val r = 11.dp.toPx()
                    val radius = androidx.compose.ui.geometry.CornerRadius(r, r)
                    val lift = if (pressed) 1f else 3f

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.40f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, lift),
                        size = size,
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                tint.copy(alpha = iconGlow + 0.30f),
                                tint.copy(alpha = iconGlow + 0.04f)
                            ),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        ),
                        size = size,
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.50f),
                                tint.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.32f)
                            ),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        ),
                        size = size,
                        cornerRadius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                    // Glossy top face.
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.17f), Color.Transparent)
                        ),
                        size = size.copy(height = size.height * 0.45f),
                        cornerRadius = radius
                    )
                }
                Icon(icon, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(Spacing.Small))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 9.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
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
                MeelanoShieldLogo(size = 16.dp, glowing = connected, accent = accent)
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
