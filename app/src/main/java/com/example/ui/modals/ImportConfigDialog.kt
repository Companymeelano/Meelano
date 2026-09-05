package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalAccent
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Paste one link, many links, or a whole base64 subscription blob. */
@Composable
fun ImportConfigDialog(
    onImport: (String) -> Unit,
    onImportClipboard: () -> Unit,
    onClose: () -> Unit
) {
    val accent = LocalAccent.current.primary
    var payload by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            color = MeelanoBgDark
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
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
                    Text("افزودن کانفیگ", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "لینک vmess / vless / trojan / ss / hy2 را وارد کنید — چند خط هم پشتیبانی می‌شود.",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(10.dp))

                TextField(
                    value = payload,
                    onValueChange = { payload = it },
                    placeholder = { Text("vless://…", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextPrimary
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onImportClipboard() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentPaste, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("از کلیپ‌بورد", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MeelanoGreenSuccess.copy(alpha = 0.16f))
                            .border(1.dp, MeelanoGreenSuccess.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onImport(payload) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("افزودن", fontSize = 12.sp, color = MeelanoGreenSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
