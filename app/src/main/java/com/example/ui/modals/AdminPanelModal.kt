package com.example.ui.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.account.Account
import com.example.data.account.AccountStore
import com.example.data.account.Quota
import com.example.data.account.Role
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowProgressBar
import com.example.ui.theme.MeelanoGoldVip
import com.example.ui.theme.MeelanoGreenSuccess
import com.example.ui.theme.MeelanoRedKillSwitch
import com.example.ui.theme.MeelanoSurfaceCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

/**
 * Administrator console: issue accounts, set an allowance of traffic and days,
 * then suspend, renew or revoke them later.
 *
 * Quota and duration are entered in the units an operator actually thinks in —
 * gigabytes and days — and converted to bytes and an absolute deadline on the
 * way in, so nothing drifts when the device clock changes.
 */
@Composable
fun AdminPanelModal(
    accounts: List<Account>,
    accent: Color,
    secondary: Color,
    onCreate: (String, String, Long, Int, String) -> Result<Account>,
    onDelete: (String) -> Result<Unit>,
    onSuspend: (String, Boolean) -> Unit,
    onRenew: (String, Long, Int) -> Unit,
    onResetUsage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    var renewTarget by remember { mutableStateOf<Account?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }

    MeelanoModalScaffold(
        title = "مدیریت کاربران",
        subtitle = "${Quota.fa(accounts.size.toString())} حساب ثبت‌شده",
        onDismiss = onDismiss,
        accent = MeelanoGoldVip
    ) {
        AnimatedVisibility(visible = feedback != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(
                        (if (feedbackIsError) MeelanoRedKillSwitch else MeelanoGreenSuccess)
                            .copy(alpha = 0.15f),
                        RoundedCornerShape(11.dp)
                    )
                    .padding(horizontal = 13.dp, vertical = 9.dp)
            ) {
                Text(
                    feedback.orEmpty(),
                    color = if (feedbackIsError) MeelanoRedKillSwitch else MeelanoGreenSuccess,
                    fontSize = 12.sp
                )
            }
        }

        Button(
            onClick = { showCreate = true },
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MeelanoGoldVip,
                contentColor = Color(0xFF2A1B00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(7.dp))
            Text("ساخت حساب جدید", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(accounts, key = { it.username }) { account ->
                AccountRow(
                    account = account,
                    accent = accent,
                    secondary = secondary,
                    onSuspend = { onSuspend(account.username, !account.suspended) },
                    onRenew = { renewTarget = account },
                    onResetUsage = {
                        onResetUsage(account.username)
                        feedbackIsError = false
                        feedback = "مصرف ${account.username} صفر شد"
                    },
                    onDelete = { deleteTarget = account }
                )
            }
        }
    }

    if (showCreate) {
        CreateAccountDialog(
            accent = MeelanoGoldVip,
            onDismiss = { showCreate = false },
            onConfirm = { user, pass, gb, days, note ->
                val quota = if (gb <= 0) Account.UNLIMITED else gb * AccountStore.GIGABYTE
                val result = onCreate(user, pass, quota, days, note)
                result.fold(
                    onSuccess = {
                        feedbackIsError = false
                        feedback = "حساب $user ساخته شد"
                        showCreate = false
                    },
                    onFailure = {
                        feedbackIsError = true
                        feedback = it.message
                    }
                )
            }
        )
    }

    renewTarget?.let { target ->
        RenewDialog(
            account = target,
            accent = MeelanoGreenSuccess,
            onDismiss = { renewTarget = null },
            onConfirm = { gb, days ->
                onRenew(target.username, gb * AccountStore.GIGABYTE, days)
                feedbackIsError = false
                feedback = "سرویس ${target.username} تمدید شد"
                renewTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "حذف حساب",
            message = "حساب «${target.username}» برای همیشه حذف شود؟",
            confirmLabel = "حذف",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDelete(target.username).fold(
                    onSuccess = {
                        feedbackIsError = false
                        feedback = "حساب ${target.username} حذف شد"
                    },
                    onFailure = {
                        feedbackIsError = true
                        feedback = it.message
                    }
                )
                deleteTarget = null
            }
        )
    }
}

@Composable
private fun AccountRow(
    account: Account,
    accent: Color,
    secondary: Color,
    onSuspend: () -> Unit,
    onRenew: () -> Unit,
    onResetUsage: () -> Unit,
    onDelete: () -> Unit
) {
    val isAdmin = account.role == Role.ADMIN
    val locked = !account.canConnect()

    GlassCard(
        accent = when {
            isAdmin -> MeelanoGoldVip
            locked -> MeelanoRedKillSwitch
            else -> null
        },
        padding = 13.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            account.username,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.size(6.dp))
                        if (isAdmin) Tag("مدیر", MeelanoGoldVip)
                        if (account.suspended) Tag("غیرفعال", MeelanoRedKillSwitch)
                        else if (locked) Tag("پایان‌یافته", MeelanoRedKillSwitch)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "حجم ${Quota.bytesShort(account.remainingBytes)} · " +
                            "زمان ${Quota.durationShort(account.remainingMillis())}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (account.hasQuotaLimit) {
                Spacer(Modifier.height(8.dp))
                GlowProgressBar(
                    fraction = account.quotaFraction,
                    accent = if (account.quotaFraction > 0.9f) MeelanoRedKillSwitch else accent,
                    secondary = secondary
                )
            }

            // The last administrator has no management row: suspending or
            // deleting it would lock the panel away permanently.
            if (!isAdmin) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip(
                        if (account.suspended) "فعال‌سازی" else "غیرفعال",
                        if (account.suspended) Icons.Filled.PlayArrow else Icons.Filled.Block,
                        if (account.suspended) MeelanoGreenSuccess else MeelanoRedKillSwitch,
                        onSuspend
                    )
                    ActionChip("تمدید", Icons.Filled.Autorenew, MeelanoGreenSuccess, onRenew)
                    ActionChip("صفر کردن", Icons.Filled.RestartAlt, accent, onResetUsage)
                    ActionChip("حذف", Icons.Filled.Delete, MeelanoRedKillSwitch, onDelete)
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    // The whole chip is the touch target, not just the text inside it.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.13f))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.size(4.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CreateAccountDialog(
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Int, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gb by remember { mutableStateOf("50") }
    var days by remember { mutableStateOf("30") }
    var note by remember { mutableStateOf("") }

    SmallDialog(title = "حساب جدید", onDismiss = onDismiss) {
        Field("نام کاربری", username, accent) { username = it }
        Spacer(Modifier.height(9.dp))
        Field("گذرواژه", password, accent) { password = it }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.weight(1f)) {
                Field("حجم (گیگابایت)", gb, accent, numeric = true) { gb = it }
            }
            Box(Modifier.weight(1f)) {
                Field("مدت (روز)", days, accent, numeric = true) { days = it }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("عدد ۰ به معنای نامحدود است", color = TextMuted, fontSize = 10.sp)
        Spacer(Modifier.height(9.dp))
        Field("یادداشت (اختیاری)", note, accent) { note = it }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onConfirm(
                    username.trim(),
                    password,
                    gb.toLongOrNull() ?: 0L,
                    days.toIntOrNull() ?: 0,
                    note.trim()
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color(0xFF2A1B00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            Text("ساخت حساب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun RenewDialog(
    account: Account,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int) -> Unit
) {
    var gb by remember { mutableStateOf("50") }
    var days by remember { mutableStateOf("30") }

    SmallDialog(title = "تمدید ${account.username}", onDismiss = onDismiss) {
        Text(
            "مقادیر واردشده به سرویس فعلی اضافه می‌شود.",
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.weight(1f)) {
                Field("افزودن حجم (گیگ)", gb, accent, numeric = true) { gb = it }
            }
            Box(Modifier.weight(1f)) {
                Field("افزودن روز", days, accent, numeric = true) { days = it }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConfirm(gb.toLongOrNull() ?: 0L, days.toIntOrNull() ?: 0) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color(0xFF04220F)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            Text("تمدید", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SmallDialog(title = title, onDismiss = onDismiss) {
        Text(message, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("انصراف", color = TextSecondary, fontSize = 13.sp)
            }
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeelanoRedKillSwitch,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SmallDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(padding = 18.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    accent: Color,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            onChange(if (numeric) text.filter { it.isDigit() } else text)
        },
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = MeelanoSurfaceCardBorder,
            focusedLabelColor = accent,
            unfocusedLabelColor = TextMuted,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = accent
        ),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
