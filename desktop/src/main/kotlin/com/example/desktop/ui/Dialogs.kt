package com.example.desktop.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.desktop.core.AppState
import com.example.desktop.core.DesktopServer

/** Full server picker, mirroring the phone's list. */
@Composable
fun ServerListDialog(state: AppState, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .width(420.dp)
                .heightIn(max = 620.dp)
                .background(MeelanoColors.BgDark, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.06f), CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(7.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            "بستن",
                            tint = MeelanoColors.TextSecondary,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "انتخاب سرور",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${state.servers.size} سرور",
                            color = MeelanoColors.TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(state.servers, key = { it.id }) { server ->
                        ServerRow(
                            server = server,
                            selected = server.id == state.activeServer?.id,
                            onClick = {
                                state.select(server)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("بستن", color = MeelanoColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ServerRow(server: DesktopServer, selected: Boolean, onClick: () -> Unit) {
    GlassCard(
        accent = if (selected) MeelanoColors.IconCyan else null,
        padding = 11.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(server.flag, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    server.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${server.country} · ${server.endpoint.displayProtocol}",
                    color = MeelanoColors.TextSecondary,
                    fontSize = 9.sp
                )
            }
            if (server.pingMs != 0) {
                Text(
                    if (server.pingMs > 0) "${server.pingMs} ms" else "بی‌پاسخ",
                    color = MeelanoColors.forPing(server.pingMs),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (selected) {
                Spacer(Modifier.width(7.dp))
                Icon(
                    Icons.Filled.Check,
                    null,
                    tint = MeelanoColors.IconCyan,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/** Paste a share link or a whole subscription body. */
@Composable
fun ImportDialog(state: AppState, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .width(420.dp)
                .background(MeelanoColors.BgDark, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "افزودن کانفیگ",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "لینک vless / vmess / trojan / ss یا محتوای یک اشتراک را بچسبانید.",
                    color = MeelanoColors.TextSecondary,
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("کانفیگ", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeelanoColors.IconCyan,
                        unfocusedBorderColor = MeelanoColors.SurfaceCardBorder,
                        focusedLabelColor = MeelanoColors.IconCyan,
                        unfocusedLabelColor = MeelanoColors.TextMuted,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MeelanoColors.IconCyan
                    ),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )

                result?.let {
                    Spacer(Modifier.height(9.dp))
                    Text(it, color = MeelanoColors.GreenSuccess, fontSize = 11.sp)
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("انصراف", color = MeelanoColors.TextSecondary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val added = state.importLinks(text)
                            result = if (added > 0) "$added سرور اضافه شد" else "سروری یافت نشد"
                            if (added > 0) text = ""
                        },
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MeelanoColors.IconCyan,
                            contentColor = Color(0xFF04122B)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("افزودن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
