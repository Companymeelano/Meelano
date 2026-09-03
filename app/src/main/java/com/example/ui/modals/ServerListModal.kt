package com.example.ui.modals

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.VpnServer
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoCyan
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoSurfaceCard
import com.example.ui.theme.MeelanoSurfaceCardBorder
import com.example.ui.theme.MeelanoSurfaceElevated
import com.example.ui.theme.PingGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SmartImportHelper

@Composable
fun ServerListModal(
    vipServers: List<VpnServer>,
    freeServers: List<VpnServer>,
    activeServer: VpnServer,
    isTestingPing: Boolean,
    isUpdatingGitHub: Boolean,
    onClose: () -> Unit,
    onSelectServer: (VpnServer) -> Unit,
    onTestPing: (isVip: Boolean) -> Unit,
    onSortLowestPing: (isVip: Boolean) -> Unit,
    onUpdateGitHub: () -> Unit,
    onShowQr: (VpnServer) -> Unit,
    onSmartImport: (VpnServer) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: VIP, 1: Free
    var searchQuery by remember { mutableStateOf("") }

    val currentList = if (selectedTab == 0) vipServers else freeServers
    val filteredList = currentList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.countryName.contains(searchQuery, ignoreCase = true) ||
        it.protocol.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MeelanoBgDark),
            color = MeelanoBgDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Header (Close X, Title, Subtitle, Globe Icon)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MeelanoSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "سرورها",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MeelanoCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "لوکیشن‌های اختصاصی VIP و رایگان گیت‌هاب",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // VIP vs Free Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // VIP Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedTab == 0) Color(0xFF261D0C) else MeelanoSurfaceCard
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedTab == 0) MeelanoGoldVip else MeelanoSurfaceCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = 0 }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "👑",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سرورهای (${vipServers.size}) VIP",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) MeelanoGoldVip else TextSecondary
                            )
                        }
                    }

                    // Free Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedTab == 1) Color(0xFF0D2520) else MeelanoSurfaceCard
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedTab == 1) MeelanoGreenSuccess else MeelanoSurfaceCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTab = 1 }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (selectedTab == 1) MeelanoGreenSuccess else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "سرورهای رایگان (${freeServers.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) MeelanoGreenSuccess else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "جستجوی کشور یا پروتکل...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeelanoCyan,
                        unfocusedBorderColor = MeelanoSurfaceCardBorder,
                        focusedContainerColor = MeelanoSurfaceCard,
                        unfocusedContainerColor = MeelanoSurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar (کمترین پینگ / تست پینگ / بروزرسانی گیت‌هاب)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left action pills
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Sort Lowest Ping Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MeelanoSurfaceElevated)
                                .clickable { onSortLowestPing(selectedTab == 0) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.South,
                                    contentDescription = null,
                                    tint = MeelanoCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "کمترین پینگ",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Free Tab GitHub Update Button
                        if (selectedTab == 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MeelanoCyan)
                                    .clickable(enabled = !isUpdatingGitHub) { onUpdateGitHub() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isUpdatingGitHub) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "بروزرسانی گیت‌هاب",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // Test Ping Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MeelanoSurfaceElevated)
                            .clickable(enabled = !isTestingPing) { onTestPing(selectedTab == 0) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isTestingPing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = MeelanoGoldVip,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MeelanoGoldVip,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تست پینگ",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Server Cards List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { server ->
                        ServerCardItem(
                            server = server,
                            isActive = server.id == activeServer.id,
                            onSelect = { onSelectServer(server) },
                            onShowQr = { onShowQr(server) },
                            onSmartImport = { onSmartImport(server) },
                            onCopy = { SmartImportHelper.copyToClipboard(context, server.configLink) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServerCardItem(
    server: VpnServer,
    isActive: Boolean,
    onSelect: () -> Unit,
    onShowQr: () -> Unit,
    onSmartImport: () -> Unit,
    onCopy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MeelanoSurfaceCard)
            .border(
                width = 1.dp,
                color = if (isActive) MeelanoCyan else MeelanoSurfaceCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Title, Protocol badge, Flag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Protocol & VIP Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (server.isVip) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF332306))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VIP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MeelanoGoldVip
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (server.protocol) {
                                    "Reality" -> Color(0xFF381A4E)
                                    "Hysteria 2" -> Color(0xFF421528)
                                    else -> Color(0xFF0F2C47)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = server.protocol.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (server.protocol) {
                                "Reality" -> Color(0xFFCE93D8)
                                "Hysteria 2" -> Color(0xFFFF80AB)
                                else -> MeelanoCyan
                            }
                        )
                    }
                }

                // Name & Flag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = server.flagEmoji, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub info: Speed, Ping, Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left stats
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${server.speedMbps} Mbps",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${server.pingMs}ms",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PingGreen
                    )
                }

                // Right country subtitle
                Text(
                    text = "${server.countryName} • سرور اختصاصی",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: QR, Smart Transfer (phone), Copy, Select
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Utility Icons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // QR Code
                    IconButton(
                        onClick = onShowQr,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MeelanoSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Smart Transfer / Open Destination App (v2rayNG / V2Box)
                    IconButton(
                        onClick = onSmartImport,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MeelanoSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "انتقال هوشمند به v2rayNG",
                            tint = MeelanoCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy Config
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MeelanoSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی کانفیگ",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Select / Active Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) MeelanoCyan else MeelanoSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (isActive) MeelanoCyan else MeelanoSurfaceCardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect() }
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isActive) "فعال" else "انتخاب",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.Black else TextPrimary
                    )
                }
            }
        }
    }
}
