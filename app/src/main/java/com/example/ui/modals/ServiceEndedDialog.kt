package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.account.LockReason
import com.example.data.account.Quota
import com.example.ui.components.GlassCard
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextSecondary

/**
 * Shown once when the user arrives at a session whose traffic or time has run
 * out. The tunnel has already been stopped by this point; this explains why,
 * so the failure does not read as a bug.
 */
@Composable
fun ServiceEndedDialog(
    reason: LockReason,
    accent: Color,
    onDismiss: () -> Unit
) {
    if (reason == LockReason.NONE) return

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(accent = MeelanoRedKillSwitch, padding = 22.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .background(MeelanoRedKillSwitch.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.HourglassBottom,
                        contentDescription = null,
                        tint = MeelanoRedKillSwitch,
                        modifier = Modifier.size(29.dp)
                    )
                }

                Spacer(Modifier.height(15.dp))

                Text(
                    Quota.lockTitle(reason),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    Quota.lockMessage(reason) +
                        ".\nاتصال متوقف شد. برای تمدید سرویس با مدیر تماس بگیرید.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color(0xFF04122B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("متوجه شدم", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
