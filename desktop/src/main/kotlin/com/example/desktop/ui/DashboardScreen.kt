package com.example.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktop.core.AppState

/**
 * The desktop dashboard.
 *
 * Deliberately the same vertical rhythm as the phone: brand lockup, the orb,
 * the server control beneath it, then live figures. Someone who uses both
 * should not have to relearn anything.
 */
@Composable
fun DashboardScreen(state: AppState) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = MeelanoColors.IconCyan,
            secondary = MeelanoColors.IconViolet,
            background = MeelanoColors.BgDark,
            surface = MeelanoColors.SurfaceCard
        )
    ) {
        var showServers by remember { mutableStateOf(false) }
        var showImport by remember { mutableStateOf(false) }

        AuroraBackground(energised = state.connectionState == DesktopConnectionState.CONNECTED) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeader(state)

                Spacer(Modifier.height(22.dp))

                ConnectOrb(state = state.connectionState) { state.toggle() }

                Spacer(Modifier.height(14.dp))

                StatusLine(state)

                Spacer(Modifier.height(18.dp))

                ServerButton(state) { showServers = true }

                Spacer(Modifier.height(14.dp))

                SpeedRow(state)

                Spacer(Modifier.height(14.dp))

                ThroughputCard(state)

                Spacer(Modifier.height(14.dp))

                ToolRow(
                    onTest = { state.testAll() },
                    onImport = { showImport = true }
                )

                Spacer(Modifier.height(14.dp))

                ProxyInfoCard(state)

                Spacer(Modifier.height(18.dp))
            }
        }

        if (showServers) {
            ServerListDialog(state) { showServers = false }
        }
        if (showImport) {
            ImportDialog(state) { showImport = false }
        }
    }
}

@Composable
private fun BrandHeader(state: AppState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MeelanoShieldLogo(
            size = 46.dp,
            glowing = state.connectionState == DesktopConnectionState.CONNECTED
        )
        Spacer(Modifier.width(11.dp))
        Text(
            "MEELANO",
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.5.sp,
            style = TextStyle(
                brush = Brush.horizontalGradient(
                    listOf(MeelanoColors.IconViolet, Color(0xFFEAF4FF), MeelanoColors.IconCyan)
                )
            )
        )
    }
}

@Composable
private fun StatusLine(state: AppState) {
    val color by animateColorAsState(
        when (state.connectionState) {
            DesktopConnectionState.CONNECTED -> MeelanoColors.GreenSuccess
            DesktopConnectionState.FAILED -> MeelanoColors.RedKillSwitch
            DesktopConnectionState.DISCONNECTED -> MeelanoColors.TextSecondary
            else -> MeelanoColors.IconCyan
        },
        tween(500),
        label = "status"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowDot(color = color, size = 6.dp, pulsing = state.connectionState.isBusy)
            Spacer(Modifier.width(4.dp))
            Text(
                state.connectionState.persian,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            state.statusMessage,
            color = MeelanoColors.TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        AnimatedVisibility(
            visible = state.lastError != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier
                    .padding(top = 9.dp)
                    .background(
                        MeelanoColors.RedKillSwitch.copy(alpha = 0.14f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    state.lastError.orEmpty(),
                    color = MeelanoColors.RedKillSwitch,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ServerButton(state: AppState, onClick: () -> Unit) {
    val server = state.activeServer

    GlassCard(
        accent = MeelanoColors.IconCyan,
        padding = 14.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(MeelanoColors.IconCyan.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(server?.flag ?: "🌐", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    server?.name ?: "سروری انتخاب نشده",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    buildString {
                        append(server?.country ?: "—")
                        append(" · ")
                        append(server?.endpoint?.displayProtocol ?: "—")
                        append(" · ")
                        append("${state.servers.size} سرور")
                    },
                    color = MeelanoColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
            val ping = server?.pingMs ?: 0
            Text(
                if (ping > 0) "$ping ms" else "—",
                color = MeelanoColors.forPing(ping),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SpeedRow(state: AppState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.ArrowDownward,
            label = "دریافت",
            value = "%.1f".format(state.downloadMbps),
            unit = "Mbps",
            color = MeelanoColors.IconCyan
        )
        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.ArrowUpward,
            label = "ارسال",
            value = "%.1f".format(state.uploadMbps),
            unit = "Mbps",
            color = MeelanoColors.IconViolet
        )
        StatTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Timer,
            label = "زمان",
            value = formatUptime(state.uptimeSeconds),
            unit = "",
            color = MeelanoColors.GreenSuccess
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    GlassCard(padding = 11.dp, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.height(5.dp))
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotBlank()) {
                Text(unit, color = MeelanoColors.TextMuted, fontSize = 8.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(label, color = MeelanoColors.TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ThroughputCard(state: AppState) {
    GlassCard(padding = 13.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "پایش زنده",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "↓ ${formatBytes(state.totalDownBytes)}  ↑ ${formatBytes(state.totalUpBytes)}",
                    color = MeelanoColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Sparkline(
                values = state.speedHistory.toList(),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    }
}

@Composable
private fun ToolRow(onTest: () -> Unit, onImport: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        ToolButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Speed,
            label = "آزمایش سرورها",
            color = MeelanoColors.IconCyan,
            onClick = onTest
        )
        ToolButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.ContentPaste,
            label = "افزودن کانفیگ",
            color = MeelanoColors.IconViolet,
            onClick = onImport
        )
    }
}

@Composable
private fun ToolButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    GlassCard(padding = 12.dp, onClick = onClick, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Tells the user exactly how their traffic is being carried.
 *
 * On Windows the system proxy is set automatically; everywhere else the address
 * has to be entered by hand, so it is shown rather than hidden.
 */
@Composable
private fun ProxyInfoCard(state: AppState) {
    GlassCard(padding = 13.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Public,
                    null,
                    tint = if (state.systemProxyOn) MeelanoColors.GreenSuccess
                    else MeelanoColors.TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (state.systemProxyOn) "پروکسی سیستم فعال است"
                    else "پروکسی محلی",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "SOCKS5 و HTTP روی 127.0.0.1:${state.proxy.listenPort}",
                color = MeelanoColors.TextSecondary,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.systemProxyOn) {
                    "مرورگرها و برنامه‌هایی که از تنظیمات پروکسی ویندوز پیروی می‌کنند از تونل عبور می‌کنند."
                } else {
                    "برنامه‌هایی که تنظیمات پروکسی سیستم را نادیده می‌گیرند مستقیم متصل می‌شوند."
                },
                color = MeelanoColors.TextMuted,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }
    }
}

private fun formatUptime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

internal fun formatBytes(value: Long): String {
    val gb = value / 1024.0 / 1024 / 1024
    val mb = value / 1024.0 / 1024
    val kb = value / 1024.0
    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.0f KB".format(kb)
        else -> "$value B"
    }
}
