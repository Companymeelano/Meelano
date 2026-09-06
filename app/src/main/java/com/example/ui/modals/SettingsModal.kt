package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.MeelanoIconCyan
import com.example.ui.theme.MeelanoIconViolet
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CoreProtocolFilter
import com.example.data.model.RoutingMode
import com.example.ui.components.GlassCard
import com.example.ui.components.Pill
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.LocalAccent
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoPurpleActive
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SmartImportHelper

@Composable
fun SettingsModal(
    routingMode: RoutingMode,
    protocolFilter: CoreProtocolFilter,
    killSwitchEnabled: Boolean,
    smartFailoverEnabled: Boolean,
    autoConnectEnabled: Boolean,
    ipv6Enabled: Boolean,
    hapticsEnabled: Boolean,
    biometricEnabled: Boolean,
    lockOnStart: Boolean,
    themeAccent: String,
    dnsPrimary: String,
    dnsSecondary: String,
    subscriptions: List<String>,
    activeConfigLink: String,
    onClose: () -> Unit,
    onRoutingModeChange: (RoutingMode) -> Unit,
    onProtocolFilterChange: (CoreProtocolFilter) -> Unit,
    onToggleKillSwitch: () -> Unit,
    onToggleSmartFailover: () -> Unit,
    onToggleAutoConnect: () -> Unit,
    onToggleIpv6: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleBiometric: () -> Unit,
    onToggleLockOnStart: () -> Unit,
    onAccentChange: (String) -> Unit,
    onDnsChange: (String, String) -> Unit,
    onAddSubscription: (String) -> Unit,
    onRemoveSubscription: (String) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSplitTunneling: () -> Unit,
    onLockApp: () -> Unit
) {
    val accent = LocalAccent.current.primary
    val context = LocalContext.current
    val scroll = rememberScrollState()

    var dns1 by remember { mutableStateOf(dnsPrimary) }
    var dns2 by remember { mutableStateOf(dnsSecondary) }
    var newSubscription by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MeelanoBgDark)
                .padding(14.dp)
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Icon(Icons.Default.Close, "بستن", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Text("تنظیمات پیشرفته", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }

                Spacer(Modifier.height(14.dp))

                SectionTitle("مسیریابی", Icons.Default.AltRoute, MeelanoIconCyan)
                RoutingMode.entries.forEach { mode ->
                    val selected = mode == routingMode
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        corner = 14.dp,
                        padding = 12.dp,
                        accent = if (selected) accent else null,
                        onClick = { onRoutingModeChange(mode) }
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(mode.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(mode.description, fontSize = 9.sp, color = TextMuted)
                            }
                            Spacer(Modifier.width(8.dp))
                            Pill(mode.badge, if (selected) accent else TextMuted)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                SectionTitle("فیلتر پروتکل هسته", Icons.Default.FilterAlt, MeelanoIconViolet)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CoreProtocolFilter.entries.take(5).forEach { filter ->
                        val selected = filter == protocolFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    1.dp,
                                    if (selected) accent.copy(alpha = 0.5f) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onProtocolFilterChange(filter) }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                filter.label,
                                fontSize = 10.sp,
                                color = if (selected) accent else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionTitle("امنیت", Icons.Default.Shield, MeelanoGreenSuccess)
                SettingSwitch(
                    "Kill Switch",
                    "قطع کامل ترافیک هنگام افت تونل (رابط در حالت blocking)",
                    killSwitchEnabled,
                    MeelanoRedKillSwitch,
                    onToggleKillSwitch
                )
                SettingSwitch(
                    "Smart Failover",
                    "در صورت خرابی نود، خودکار به سریع‌ترین نود سالم سوییچ کن",
                    smartFailoverEnabled,
                    MeelanoGreenSuccess,
                    onToggleSmartFailover
                )
                SettingSwitch(
                    "احراز هویت بیومتریک",
                    "باز کردن قفل با اثر انگشت یا چهره",
                    biometricEnabled,
                    MeelanoPurpleActive,
                    onToggleBiometric
                )
                SettingSwitch(
                    "قفل هنگام اجرا",
                    "هر بار که اپ باز می‌شود، صفحه قفل نمایش داده شود",
                    lockOnStart,
                    MeelanoGoldVip,
                    onToggleLockOnStart
                )

                Spacer(Modifier.height(16.dp))
                SectionTitle("اتصال", Icons.Default.Bolt, MeelanoIconCyan)
                SettingSwitch("اتصال خودکار", "با باز شدن اپ به‌صورت خودکار وصل شو", autoConnectEnabled, accent, onToggleAutoConnect)
                SettingSwitch("پشتیبانی IPv6", "افزودن مسیر IPv6 به تونل", ipv6Enabled, accent, onToggleIpv6)
                SettingSwitch("بازخورد لمسی", "لرزش هنگام تغییر وضعیت اتصال", hapticsEnabled, accent, onToggleHaptics)

                Spacer(Modifier.height(16.dp))
                SectionTitle("رنگ‌بندی برنامه", Icons.Default.Palette, MeelanoIconViolet)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentPreset.entries.forEach { preset ->
                        val selected = preset.key == themeAccent
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(preset.primary.copy(alpha = if (selected) 0.20f else 0.06f))
                                .border(
                                    1.dp,
                                    if (selected) preset.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onAccentChange(preset.key) }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(preset.primary, preset.secondary)
                                        )
                                    )
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(preset.label, fontSize = 9.sp, color = if (selected) TextPrimary else TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionTitle("DNS", Icons.Default.TravelExplore, MeelanoGreenSuccess)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactField(dns1, { dns1 = it }, "DNS اول", Modifier.weight(1f))
                    CompactField(dns2, { dns2 = it }, "DNS دوم", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Cloudflare" to ("1.1.1.1" to "1.0.0.1"),
                        "Google" to ("8.8.8.8" to "8.8.4.4"),
                        "Shecan" to ("178.22.122.100" to "185.51.200.2")
                    ).forEach { (label, pair) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    dns1 = pair.first
                                    dns2 = pair.second
                                    onDnsChange(pair.first, pair.second)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton("ذخیره DNS", accent) { onDnsChange(dns1.trim(), dns2.trim()) }

                Spacer(Modifier.height(16.dp))
                SectionTitle("اشتراک‌های سرور (${subscriptions.size})", Icons.Default.CloudSync, MeelanoIconCyan)
                subscriptions.forEach { url ->
                    GlassCard(Modifier.fillMaxWidth().padding(bottom = 6.dp), corner = 12.dp, padding = 10.dp) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                url,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Delete,
                                "حذف",
                                tint = MeelanoRedKillSwitch,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onRemoveSubscription(url) }
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CompactField(newSubscription, { newSubscription = it }, "https://…", Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.18f))
                            .clickable {
                                onAddSubscription(newSubscription)
                                newSubscription = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, "افزودن", tint = accent, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionTitle("ابزارها", Icons.Default.Construction, MeelanoIconViolet)
                ToolRow("تونل تفکیکی (Split Tunneling)", MeelanoPurpleActive, onOpenSplitTunneling)
                ToolRow("کنسول لاگ زنده", MeelanoGreenSuccess, onOpenLogs)
                ToolRow("قفل کردن برنامه", MeelanoGoldVip, onLockApp)
                ToolRow("کپی کانفیگ سرور فعال", accent) {
                    SmartImportHelper.copyToClipboard(context, activeConfigLink)
                }
                ToolRow("اشتراک‌گذاری کانفیگ فعال", accent) {
                    SmartImportHelper.shareConfig(context, activeConfigLink)
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "MeeLano Tunnel · نسخه ۱۹٫۰ · MEELANO STUDIO DESIGN",
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    icon: ImageVector? = null,
    accent: Color = MeelanoIconCyan
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 9.dp)
    ) {
        // A short gradient bar anchors each section to the app's palette and
        // gives the list a visual rhythm that plain text headings lacked.
        Box(
            Modifier
                .size(width = 3.dp, height = 15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.25f)))
                )
        )
        Spacer(Modifier.width(8.dp))
        if (icon != null) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), corner = 14.dp, padding = 12.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 9.sp, color = TextMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = color,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                )
            )
        }
    }
}

@Composable
private fun ToolRow(title: String, color: Color, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), corner = 14.dp, padding = 12.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(title, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 11.sp, color = TextMuted) },
        singleLine = true,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
private fun PrimaryButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
    }
}
