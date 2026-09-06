package com.example.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.account.Account
import com.example.data.account.LockReason
import com.example.data.account.Quota
import com.example.data.account.Session
import com.example.ui.components.CircularGauge
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowProgressBar
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The user's own panel: who they are, how much traffic is left and how long the
 * subscription still runs.
 *
 * Both allowances are shown twice over — as a gauge for the glance and as an
 * exact figure for the answer — because "how much do I have left" is the one
 * question this screen exists to settle.
 */
@Composable
fun AccountPanelModal(
    session: Session,
    accent: Color,
    secondary: Color,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    MeelanoModalScaffold(
        title = "پنل کاربری",
        onDismiss = onDismiss,
        accent = accent
    ) {
        val account = session.account

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ---- identity ----
            GlassCard(accent = accent, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (session.isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.size(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            session.displayName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when {
                                session.isAdmin -> "مدیر سیستم"
                                session.isSignedIn -> "کاربر ثبت‌شده"
                                else -> "دسترسی محدود به سرورهای رایگان"
                            },
                            color = if (session.isAdmin) MeelanoGoldVip else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (account == null) {
                // Guest: no quota to report, so explain what signing in buys.
                GlassCard(padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "شما به‌عنوان مهمان وارد شده‌اید",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "در این حالت تنها سرورهای رایگان در دسترس هستند. " +
                                "برای استفاده از سرورهای اختصاصی، با نام کاربری وارد شوید.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                val lock = account.lockReason()
                if (lock != LockReason.NONE) {
                    LockBanner(lock)
                    Spacer(Modifier.height(14.dp))
                }

                // ---- the two allowances, side by side ----
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AllowanceGauge(
                        modifier = Modifier.weight(1f),
                        title = "حجم",
                        icon = Icons.Filled.DataUsage,
                        // Gauges show what REMAINS, not what is spent: a full
                        // ring reading "empty account" would be backwards.
                        fraction = if (account.hasQuotaLimit) 1f - account.quotaFraction else 1f,
                        label = Quota.bytesShort(account.remainingBytes),
                        caption = "باقی‌مانده",
                        accent = accent,
                        secondary = secondary
                    )
                    AllowanceGauge(
                        modifier = Modifier.weight(1f),
                        title = "زمان",
                        icon = Icons.Filled.CalendarMonth,
                        fraction = if (account.hasExpiry) 1f - account.timeFraction() else 1f,
                        label = Quota.durationShort(account.remainingMillis()),
                        caption = "باقی‌مانده",
                        accent = accent,
                        secondary = secondary
                    )
                }

                Spacer(Modifier.height(14.dp))

                GlassCard(padding = 15.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        DetailRow("حجم کل", Quota.bytes(account.quotaBytes))
                        DetailRow("حجم مصرف‌شده", Quota.bytes(account.usedBytes))
                        DetailRow("حجم باقی‌مانده", Quota.bytes(account.remainingBytes))

                        if (account.hasQuotaLimit) {
                            Spacer(Modifier.height(9.dp))
                            GlowProgressBar(
                                fraction = account.quotaFraction,
                                accent = if (account.quotaFraction > 0.9f) MeelanoRedKillSwitch else accent,
                                secondary = secondary
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        DetailRow(
                            "تاریخ انقضا",
                            if (account.hasExpiry) formatDate(account.expiresAt) else "نامحدود"
                        )
                        DetailRow("زمان باقی‌مانده", Quota.duration(account.remainingMillis()))

                        if (account.hasExpiry) {
                            Spacer(Modifier.height(9.dp))
                            GlowProgressBar(
                                fraction = account.timeFraction(),
                                accent = if (account.timeFraction() > 0.9f) MeelanoRedKillSwitch else accent,
                                secondary = secondary
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        DetailRow("تاریخ ساخت", formatDate(account.createdAt))
                        if (account.lastLoginAt > 0) {
                            DetailRow("آخرین ورود", formatDate(account.lastLoginAt))
                        }
                        if (account.note.isNotBlank()) {
                            DetailRow("یادداشت", account.note)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (session.isAdmin) {
                GlassCard(
                    accent = MeelanoGoldVip,
                    padding = 14.dp,
                    onClick = onOpenAdmin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            null,
                            tint = MeelanoGoldVip,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.size(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "مدیریت کاربران",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "ساخت حساب، تعیین حجم و زمان",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.Logout,
                    null,
                    tint = MeelanoRedKillSwitch,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    if (session.isSignedIn) "خروج از حساب" else "خروج از حالت مهمان",
                    color = MeelanoRedKillSwitch,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LockBanner(reason: LockReason) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MeelanoRedKillSwitch.copy(alpha = 0.15f), RoundedCornerShape(13.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                Quota.lockTitle(reason),
                color = MeelanoRedKillSwitch,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                Quota.lockMessage(reason) + ". برای تمدید با مدیر تماس بگیرید.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun AllowanceGauge(
    modifier: Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fraction: Float,
    label: String,
    caption: String,
    accent: Color,
    secondary: Color
) {
    // Running low turns the ring red well before it empties, so the warning
    // arrives while there is still time to act on it.
    val tint = if (fraction < 0.15f) MeelanoRedKillSwitch else accent

    GlassCard(padding = 13.dp, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(5.dp))
                Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            CircularGauge(
                value = fraction,
                accent = tint,
                secondary = secondary,
                label = label,
                caption = caption,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

internal fun formatDate(millis: Long): String {
    if (millis == Account.UNLIMITED) return "نامحدود"
    return Quota.fa(
        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
    )
}
