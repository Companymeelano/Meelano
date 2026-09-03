package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MeelanoHexagonLogo
import com.example.ui.components.PowerButton3D
import com.example.ui.components.TrafficLineChart
import com.example.ui.modals.LiveLogConsoleDialog
import com.example.ui.modals.QrCodeDialog
import com.example.ui.modals.SecurityLockScreen
import com.example.ui.modals.ServerListModal
import com.example.ui.modals.SettingsModal
import com.example.ui.modals.SmartImportFallbackDialog
import com.example.ui.modals.SplitTunnelingDialog
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoBgDarkSecondary
import com.example.ui.theme.MeelanoCyan
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoPurpleActive
import com.example.ui.theme.MeelanoPurpleDeep
import com.example.ui.theme.MeelanoSurfaceCard
import com.example.ui.theme.MeelanoSurfaceCardBorder
import com.example.ui.theme.MeelanoSurfaceElevated
import com.example.ui.theme.PingGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.vpn.VpnConnectionState

@Composable
fun MainDashboardScreen(
    viewModel: MainViewModel,
    onRequestVpnPermission: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val connectionState by viewModel.connectionState.collectAsState()
    val liveStats by viewModel.liveStats.collectAsState()
    val activeServer by viewModel.activeServer.collectAsState()
    val vipServers by viewModel.vipServers.collectAsState()
    val freeServers by viewModel.freeServers.collectAsState()
    val routingMode by viewModel.routingMode.collectAsState()
    val protocolFilter by viewModel.coreProtocolFilter.collectAsState()
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsState()
    val smartFailoverEnabled by viewModel.smartFailoverEnabled.collectAsState()
    val dashboardTab by viewModel.dashboardTab.collectAsState()
    val bypassApps by viewModel.bypassApps.collectAsState()

    val isServersModalOpen by viewModel.isServersModalOpen.collectAsState()
    val isSettingsModalOpen by viewModel.isSettingsModalOpen.collectAsState()
    val isLogsConsoleOpen by viewModel.isLogsConsoleOpen.collectAsState()
    val isSplitTunnelingOpen by viewModel.isSplitTunnelingOpen.collectAsState()
    val selectedServerForQr by viewModel.selectedServerForQr.collectAsState()
    val smartImportFallbackOpen by viewModel.smartImportFallbackOpen.collectAsState()
    val isSoundMuted by viewModel.isSoundMuted.collectAsState()

    val isLocked by viewModel.securityManager.isLocked.collectAsState()
    val currentPin by viewModel.securityManager.currentPinInput.collectAsState()
    val pinError by viewModel.securityManager.pinError.collectAsState()

    val isTestingPing by viewModel.isTestingPing.collectAsState()
    val isUpdatingGitHub by viewModel.isUpdatingGitHub.collectAsState()
    val logs by viewModel.logs.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Scaffold(
        containerColor = MeelanoBgDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MeelanoBgDark, Color(0xFF091222), MeelanoBgDarkSecondary)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ==========================================
                // 1. TOP APP BAR (Lock, Mute, VIP badge, Pro title, 3D Hexagon Logo)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Action buttons: Lock & Sound
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.securityManager.lock() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MeelanoSurfaceCard)
                                .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل امنیتی",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleSoundMute() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MeelanoSurfaceCard)
                                .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = if (isSoundMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "صدا",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // VIP Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2B1F08))
                            .border(1.dp, Color(0xFF5E4513), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VIP 1.42/18.08G",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeelanoGoldVip
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "👑", fontSize = 11.sp)
                        }
                    }

                    // Right Brand & Hexagon Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PRO MEELANO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (connectionState) {
                                                VpnConnectionState.CONNECTED -> PingGreen
                                                VpnConnectionState.CONNECTING -> MeelanoCyan
                                                else -> Color(0xFF00E5FF)
                                            }
                                        )
                                        .alpha(dotAlpha)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (connectionState) {
                                        VpnConnectionState.CONNECTED -> "متصل شد"
                                        VpnConnectionState.CONNECTING -> "درحال اتصال..."
                                        else -> "آماده اتصال"
                                    },
                                    fontSize = 10.sp,
                                    color = when (connectionState) {
                                        VpnConnectionState.CONNECTED -> PingGreen
                                        VpnConnectionState.CONNECTING -> MeelanoCyan
                                        else -> TextSecondary
                                    }
                                )
                            }
                        }

                        MeelanoHexagonLogo(
                            size = 40.dp,
                            glowing = connectionState == VpnConnectionState.CONNECTED
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // 2. ACTIVE SERVER SELECTOR CARD
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MeelanoSurfaceCard)
                        .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.openServersModal() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Chevron Arrow
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )

                        // Left-middle: Fastest badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C273D))
                                .border(1.dp, Color(0xFF13507D), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "سریع‌ترین",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MeelanoCyan
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Center: Server name & ping subtext
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = activeServer.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "سرور اختصاصی ${activeServer.countryName} • ${activeServer.pingMs}ms 📶 • ${activeServer.protocol}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        // Right: Flag
                        Text(
                            text = activeServer.flagEmoji,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Routing Indicator Pill (e.g. مسیریابی: دورزدن ایران)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MeelanoSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MeelanoCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مسیریابی: ${routingMode.title}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ==========================================
                // 3. CENTERPIECE CONNECTION METERS & 3D BUTTON
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upload Meter Card (Left)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(76.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F2D3D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "آپلود",
                                tint = MeelanoCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "آپلود",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Text(
                            text = "${liveStats.uploadMbps} Mb/s",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Text(
                            text = "MB ${liveStats.totalUploadedMb.toInt()}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    // Giant 3D Power Button (Center)
                    PowerButton3D(
                        state = connectionState,
                        onClick = {
                            onRequestVpnPermission()
                        }
                    )

                    // Download Meter Card (Right)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(76.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E45)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "دانلود",
                                tint = Color(0xFF82B1FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "دانلود",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Text(
                            text = "${liveStats.downloadMbps} Mb/s",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Text(
                            text = "MB ${liveStats.totalDownloadedMb.toInt()}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (connectionState) {
                        VpnConnectionState.CONNECTED -> "تونل فعال • کل ترافیک رمزنگاری شده است"
                        VpnConnectionState.CONNECTING -> "در حال برقراری هندشیک TLS 1.3..."
                        else -> "آماده اتصال با یک لمس"
                    },
                    fontSize = 11.sp,
                    color = if (connectionState == VpnConnectionState.CONNECTED) PingGreen else TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // 4. SEGMENTED TAB BAR (Traffic Chart, Security Tools, Network Status)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MeelanoSurfaceCard)
                        .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(14.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 2: Traffic Chart (نمودار ترافیک)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (dashboardTab == 2) MeelanoPurpleDeep else Color.Transparent)
                            .clickable { viewModel.setDashboardTab(2) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "نمودار ترافیک",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dashboardTab == 2) Color.White else TextSecondary
                        )
                    }

                    // Tab 1: Security Tools (ابزارهای امنیتی)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (dashboardTab == 1) MeelanoPurpleDeep else Color.Transparent)
                            .clickable { viewModel.setDashboardTab(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ابزارهای امنیتی",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dashboardTab == 1) Color.White else TextSecondary
                        )
                    }

                    // Tab 0: Network Status (وضعیت شبکه)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (dashboardTab == 0) MeelanoPurpleDeep else Color.Transparent)
                            .clickable { viewModel.setDashboardTab(0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "وضعیت شبکه",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dashboardTab == 0) Color.White else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // 5. TAB CONTENT AREA
                // ==========================================
                when (dashboardTab) {
                    0 -> {
                        // 2x2 Network Status Grid
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Encryption
                                StatusGridItem(
                                    title = "رمزنگاری داده",
                                    value = "TLS 1.3 / AES-256",
                                    valueColor = PingGreen,
                                    modifier = Modifier.weight(1f)
                                )

                                // Tunnel IP
                                StatusGridItem(
                                    title = "IP اختصاصی تونل",
                                    value = liveStats.tunnelIp,
                                    valueColor = MeelanoCyan,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Protocol
                                StatusGridItem(
                                    title = "پروتکل فعال",
                                    value = activeServer.protocol,
                                    valueColor = MeelanoGoldVip,
                                    modifier = Modifier.weight(1f)
                                )

                                // Packet Loss
                                StatusGridItem(
                                    title = "پکت‌لاس (نشت بسته)",
                                    value = "${liveStats.packetLossPercent}%",
                                    valueColor = PingGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    1 -> {
                        // Security Tools Overview
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusGridItem(
                                title = "قطع اضطراری (Kill Switch)",
                                value = if (killSwitchEnabled) "فعال • حفاظت نشت داده" else "غیرفعال",
                                valueColor = if (killSwitchEnabled) Color(0xFFFF5252) else TextMuted
                            )
                            StatusGridItem(
                                title = "DNS ایمن و ضد فیلتر",
                                value = "1.1.1.1 Cloudflare Encrypted",
                                valueColor = MeelanoCyan
                            )
                            StatusGridItem(
                                title = "تفکیک ترافیک (Split Tunneling)",
                                value = "${bypassApps.count { it.isBypassed }} اپلیکیشن بانکی و اسنپ دورزده شد",
                                valueColor = PingGreen
                            )
                        }
                    }
                    2 -> {
                        // Live Canvas Line Chart
                        TrafficLineChart(stats = liveStats)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // 6. BOTTOM ACTION CARDS (تنظیمات & سرورها)
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Settings Card (Left)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MeelanoSurfaceCard)
                            .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(14.dp))
                            .clickable { viewModel.openSettingsModal() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E1B4E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "تنظیمات",
                                    tint = Color(0xFFBA68C8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "تنظیمات",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "امنیت و مسیریابی",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Servers Card (Right)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MeelanoSurfaceCard)
                            .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(14.dp))
                            .clickable { viewModel.openServersModal() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F2D3D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "سرورها",
                                    tint = MeelanoCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "سرورها",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "انتخاب نود و لوکیشن",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // 7. FOOTER BRANDING (MEELANO STUDIO DESIGN)
                // ==========================================
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0A1224))
                        .border(1.dp, Color(0xFF162544), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MEELANO STUDIO DESIGN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        MeelanoHexagonLogo(size = 14.dp, glowing = false)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Designed by Milad Yaghoobi",
                    fontSize = 9.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ==========================================
            // MODALS & DIALOGS
            // ==========================================
            if (isServersModalOpen) {
                ServerListModal(
                    vipServers = vipServers,
                    freeServers = freeServers,
                    activeServer = activeServer,
                    isTestingPing = isTestingPing,
                    isUpdatingGitHub = isUpdatingGitHub,
                    onClose = { viewModel.closeServersModal() },
                    onSelectServer = { server ->
                        viewModel.selectServer(server, context)
                        viewModel.closeServersModal()
                    },
                    onTestPing = { isVip -> viewModel.testPings(isVip) },
                    onSortLowestPing = { isVip -> viewModel.sortByPing(isVip) },
                    onUpdateGitHub = { viewModel.updateGitHubFreeServers() },
                    onShowQr = { server -> viewModel.showQrCode(server) },
                    onSmartImport = { server -> viewModel.triggerSmartImport(context, server.configLink) }
                )
            }

            if (isSettingsModalOpen) {
                SettingsModal(
                    routingMode = routingMode,
                    protocolFilter = protocolFilter,
                    killSwitchEnabled = killSwitchEnabled,
                    smartFailoverEnabled = smartFailoverEnabled,
                    onClose = { viewModel.closeSettingsModal() },
                    onRoutingModeChange = { viewModel.setRoutingMode(it) },
                    onProtocolFilterChange = { viewModel.setCoreProtocolFilter(it) },
                    onToggleKillSwitch = { viewModel.toggleKillSwitch() },
                    onToggleSmartFailover = { viewModel.toggleSmartFailover() },
                    onOpenLogs = { viewModel.openLogsConsole() },
                    onOpenSplitTunneling = { viewModel.openSplitTunneling() },
                    onLockApp = {
                        viewModel.closeSettingsModal()
                        viewModel.securityManager.lock()
                    },
                    activeConfigLink = activeServer.configLink
                )
            }

            if (isLogsConsoleOpen) {
                LiveLogConsoleDialog(
                    logs = logs,
                    onClose = { viewModel.closeLogsConsole() }
                )
            }

            if (isSplitTunnelingOpen) {
                SplitTunnelingDialog(
                    bypassApps = bypassApps,
                    onToggleApp = { pkg -> viewModel.toggleBypassApp(pkg) },
                    onClose = { viewModel.closeSplitTunneling() }
                )
            }

            selectedServerForQr?.let { server ->
                QrCodeDialog(
                    server = server,
                    onClose = { viewModel.closeQrCode() }
                )
            }

            if (smartImportFallbackOpen) {
                SmartImportFallbackDialog(
                    onClose = { viewModel.closeSmartImportFallback() }
                )
            }

            if (isLocked) {
                SecurityLockScreen(
                    currentPin = currentPin,
                    pinError = pinError,
                    onDigitPress = { digit -> viewModel.securityManager.appendPinDigit(digit) },
                    onDeleteDigit = { viewModel.securityManager.deletePinDigit() },
                    onBiometricPress = {
                        viewModel.securityManager.authenticateWithBiometricSuccess()
                    },
                    onLoginWithCredentials = { user, pass ->
                        viewModel.securityManager.authenticateWithCredentials(user, pass)
                    }
                )
            }
        }
    }
}

@Composable
fun StatusGridItem(
    title: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MeelanoSurfaceCard)
            .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
