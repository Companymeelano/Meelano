package com.example.ui.modals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.security.SecurityManager
import com.example.ui.components.AuroraBackground
import com.example.ui.components.MeelanoHexagonLogo
import com.example.ui.theme.LocalAccent
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Full-screen app lock. The PIN is verified against a salted hash and the
 * fingerprint button drives the platform BiometricPrompt — success is only
 * granted by the OS callback.
 */
@Composable
fun SecurityLockScreen(
    currentPin: String,
    pinError: String?,
    biometricAvailable: Boolean,
    onDigitPress: (String) -> Unit,
    onDeleteDigit: () -> Unit,
    onBiometricPress: () -> Unit
) {
    val accentPreset = LocalAccent.current
    val accent = accentPreset.primary

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        AuroraBackground(
            accent = accent,
            secondary = accentPreset.secondary,
            energised = false,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MeelanoHexagonLogo(size = 78.dp, glowing = true, accent = accent)
                Spacer(Modifier.height(16.dp))
                Text("MeeLano Tunnel", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("برای ادامه، پین‌کد را وارد کنید", fontSize = 11.sp, color = TextSecondary)

                Spacer(Modifier.height(28.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(SecurityManager.PIN_LENGTH) { index ->
                        val filled = index < currentPin.length
                        val scale by animateFloatAsState(if (filled) 1.15f else 1f, tween(150), label = "dot")
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (pinError != null) MeelanoRedKillSwitch
                                    else if (filled) accent
                                    else Color.White.copy(alpha = 0.12f)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = pinError ?: " ",
                    fontSize = 11.sp,
                    color = MeelanoRedKillSwitch,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(18.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { key ->
                            KeypadKey(
                                key = key,
                                accent = accent,
                                enabled = key != "BIO" || biometricAvailable,
                                onClick = {
                                    when (key) {
                                        "BIO" -> onBiometricPress()
                                        "DEL" -> onDeleteDigit()
                                        else -> onDigitPress(key)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    if (biometricAvailable) "می‌توانید با اثر انگشت هم وارد شوید"
                    else "بیومتریک روی این دستگاه در دسترس نیست",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun KeypadKey(key: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    val isAction = key == "BIO" || key == "DEL"
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(
                if (isAction) Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                ) else Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.03f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.10f else 0.03f), CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "BIO" -> Icon(
                Icons.Default.Fingerprint,
                "بیومتریک",
                tint = if (enabled) MeelanoGreenSuccess else TextMuted,
                modifier = Modifier.size(28.dp)
            )
            "DEL" -> Icon(
                Icons.Default.Backspace,
                "حذف",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            else -> Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
