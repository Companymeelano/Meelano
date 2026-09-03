package com.example.ui.modals

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MeelanoBgDark
import com.example.ui.theme.MeelanoCyan
import com.example.ui.theme.MeelanoCyanGlow
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.MeelanoSurfaceCard
import com.example.ui.theme.MeelanoSurfaceCardBorder
import com.example.ui.theme.MeelanoSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SecurityLockScreen(
    currentPin: String,
    pinError: String?,
    onDigitPress: (String) -> Unit,
    onDeleteDigit: () -> Unit,
    onBiometricPress: () -> Unit,
    onLoginWithCredentials: (username: String, pass: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: PIN/Biometric, 1: Username Login
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* Modal lock cannot be dismissed without auth */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
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
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Glowing Cyan Lock Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF0C2C47), Color(0xFF071424))
                            )
                        )
                        .border(1.5.dp, MeelanoCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MeelanoCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "قفل امنیتی MeeLano Tunnel",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "احراز هویت بیومتریک و رمز عددی جهت محافظت از حساب VIP",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Switch Tabs: PIN/Biometric vs Username
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MeelanoSurfaceCard)
                        .border(1.dp, MeelanoSurfaceCardBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    // Username Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) MeelanoSurfaceElevated else Color.Transparent)
                            .clickable { selectedTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ورود نام کاربری",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) TextPrimary else TextSecondary
                        )
                    }

                    // PIN & Biometric Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) MeelanoSurfaceElevated else Color.Transparent)
                            .clickable { selectedTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "پین‌کد و بیومتریک",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) MeelanoCyan else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (selectedTab == 0) {
                    // 4-Dot PIN Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..3) {
                            val isFilled = i < currentPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isFilled) MeelanoCyan else Color(0xFF142442))
                                    .border(
                                        width = 1.dp,
                                        color = if (isFilled) MeelanoCyanGlow else MeelanoSurfaceCardBorder,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (pinError != null) {
                        Text(
                            text = pinError,
                            fontSize = 11.sp,
                            color = MeelanoRedKillSwitch,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "پین‌کد پیش‌فرض: 1234",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3D Keypad Grid (1-9, Touch ID, 0, Backspace)
                    val keypadRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "DEL")
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (row in keypadRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (key in row) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (key) {
                                                    "BIO" -> Color(0xFF0F2D3D)
                                                    "DEL" -> Color(0xFF261824)
                                                    else -> MeelanoSurfaceCard
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = when (key) {
                                                    "BIO" -> MeelanoCyan
                                                    "DEL" -> Color(0xFF5A253A)
                                                    else -> MeelanoSurfaceCardBorder
                                                },
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                when (key) {
                                                    "BIO" -> onBiometricPress()
                                                    "DEL" -> onDeleteDigit()
                                                    else -> onDigitPress(key)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (key) {
                                            "BIO" -> Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "اثر انگشت",
                                                tint = MeelanoCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            "DEL" -> Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "پاک کردن",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            else -> Text(
                                                text = key,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // VIP Account Credentials Tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            placeholder = { Text("نام کاربری حساب VIP...", color = TextMuted, fontSize = 12.sp) },
                            trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MeelanoCyan,
                                unfocusedBorderColor = MeelanoSurfaceCardBorder,
                                focusedContainerColor = MeelanoSurfaceCard,
                                unfocusedContainerColor = MeelanoSurfaceCard,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("رمز عبور اختصاصی...", color = TextMuted, fontSize = 12.sp) },
                            visualTransformation = PasswordVisualTransformation(),
                            trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MeelanoCyan,
                                unfocusedBorderColor = MeelanoSurfaceCardBorder,
                                focusedContainerColor = MeelanoSurfaceCard,
                                unfocusedContainerColor = MeelanoSurfaceCard,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { onLoginWithCredentials(usernameInput, passwordInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MeelanoCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "ورود و آزادسازی اپلیکیشن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer Security Token
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MeelanoGreenSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "امنیت سخت‌افزاری بیومتریک و رمزنگاری AES-256 فعال است",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
