package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BypassApp
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalAccent
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Lists the apps actually installed on the device. Anything switched on is
 * passed to `VpnService.Builder.addDisallowedApplication`, so its traffic never
 * enters the tunnel.
 */
@Composable
fun SplitTunnelingDialog(
    bypassApps: List<BypassApp>,
    onToggleApp: (String) -> Unit,
    onClose: () -> Unit
) {
    val accent = LocalAccent.current.primary
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }

    val filtered = bypassApps
        .filter { showSystem || !it.isSystemApp }
        .filter {
            query.isBlank() || it.appName.contains(query, true) || it.packageName.contains(query, true)
        }
    val bypassedCount = bypassApps.count { it.isBypassed }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = MeelanoBgDark
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Icon(Icons.Default.Close, "بستن", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("تونل تفکیکی", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("$bypassedCount اپ مستقیم از تونل خارج است", fontSize = 9.sp, color = TextMuted)
                    }
                }

                Spacer(Modifier.height(10.dp))

                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجوی برنامه…", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("نمایش اپ‌های سیستمی", fontSize = 11.sp, color = TextSecondary)
                    Switch(
                        checked = showSystem,
                        onCheckedChange = { showSystem = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = accent,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                            uncheckedThumbColor = TextMuted
                        )
                    )
                }

                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        GlassCard(Modifier.fillMaxWidth(), corner = 12.dp, padding = 10.dp) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        app.appName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${app.category} · ${app.packageName}",
                                        fontSize = 8.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Switch(
                                    checked = app.isBypassed,
                                    onCheckedChange = { onToggleApp(app.packageName) },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MeelanoGreenSuccess,
                                        checkedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                                        uncheckedThumbColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    "تغییرات پس از اتصال بعدی اعمال می‌شود.",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
        }
    }
}
