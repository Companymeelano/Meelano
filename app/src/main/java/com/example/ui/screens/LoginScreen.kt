package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.account.LockReason
import com.example.data.account.Quota
import com.example.ui.components.AuroraBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.MeelanoShieldLogo
import com.example.ui.theme.MeelanoIconCyan
import com.example.ui.theme.MeelanoIconViolet
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.MeelanoSurfaceCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

/**
 * The gate in front of the dashboard.
 *
 * Two ways through: sign in with credentials issued by the administrator for
 * full access, or continue as a guest and get the free server list only. The
 * guest path is a first-class button rather than fine print, because most
 * people opening a VPN for the first time have no account yet and a hard wall
 * would simply lose them.
 */
@Composable
fun LoginScreen(
    accent: Color,
    secondary: Color,
    errorMessage: String?,
    lockReason: LockReason,
    onSignIn: (String, String) -> Unit,
    onGuest: () -> Unit,
    onDismissError: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }

    AuroraBackground(accent = accent, secondary = secondary, energised = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MeelanoShieldLogo(size = 92.dp, glowing = true, accent = accent)

            Spacer(Modifier.height(18.dp))

            Text(
                "MEELANO",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(MeelanoIconViolet, Color(0xFFEAF4FF), MeelanoIconCyan)
                    )
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "برای دسترسی کامل وارد شوید",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            GlassCard(accent = accent, padding = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            onDismissError()
                        },
                        label = { Text("نام کاربری") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Person, null, tint = accent, modifier = Modifier.size(19.dp))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = fieldColors(accent),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            onDismissError()
                        },
                        label = { Text("گذرواژه") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = accent, modifier = Modifier.size(19.dp))
                        },
                        trailingIcon = {
                            TextButton(onClick = { revealed = !revealed }) {
                                Icon(
                                    if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (revealed) "پنهان کردن" else "نمایش",
                                    tint = TextMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        },
                        visualTransformation = if (revealed) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = fieldColors(accent),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Failure feedback sits directly under the fields, where the
                    // eye already is, rather than in a toast that can be missed.
                    AnimatedVisibility(
                        visible = errorMessage != null || lockReason != LockReason.NONE,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MeelanoRedKillSwitch.copy(alpha = 0.14f),
                                        RoundedCornerShape(11.dp)
                                    )
                                    .padding(horizontal = 13.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = errorMessage
                                        ?: Quota.lockMessage(lockReason),
                                    color = MeelanoRedKillSwitch,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = { onSignIn(username, password) },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color(0xFF04122B),
                            disabledContainerColor = MeelanoSurfaceCardBorder.copy(alpha = 0.4f),
                            disabledContentColor = TextMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("ورود", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(MeelanoSurfaceCardBorder.copy(alpha = 0.5f))
                        .height(1.dp)
                        .size(width = 60.dp, height = 1.dp)
                )
                Text(
                    "  یا  ",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Box(
                    Modifier
                        .background(MeelanoSurfaceCardBorder.copy(alpha = 0.5f))
                        .size(width = 60.dp, height = 1.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            TextButton(onClick = onGuest) {
                Text(
                    "ورود به‌عنوان مهمان",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "دسترسی فقط به سرورهای رایگان",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun fieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = MeelanoSurfaceCardBorder,
    focusedLabelColor = accent,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = accent
)
