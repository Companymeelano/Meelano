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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SmartImportHelper

/** Terminal-style live view of the tunnel core log, colour-coded by severity. */
@Composable
fun LiveLogConsoleDialog(
    logs: List<String>,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(20.dp)),
            color = MeelanoBgDark
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                        ) {
                            Icon(Icons.Default.Close, "بستن", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(MeelanoRedKillSwitch.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                "پاک کردن",
                                tint = MeelanoRedKillSwitch,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                SmartImportHelper.copyToClipboard(context, logs.joinToString("\n"))
                            },
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(MeelanoGreenSuccess.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                "کپی لاگ",
                                tint = MeelanoGreenSuccess,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("کنسول زنده هسته", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("${logs.size} رویداد", fontSize = 9.sp, color = TextMuted)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF05070C))
                        .padding(10.dp)
                ) {
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        itemsIndexed(logs) { _, line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = when {
                                    line.contains("ERROR") || line.contains("failed", true) -> MeelanoRedKillSwitch
                                    line.contains("OK") || line.contains("established") ||
                                        line.contains("success", true) -> MeelanoGreenSuccess
                                    line.contains("[SYSTEM]") || line.contains("[CORE]") -> MeelanoGoldVip
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "این خروجی مستقیماً از سرویس VpnService تولید می‌شود.",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
        }
    }
}
