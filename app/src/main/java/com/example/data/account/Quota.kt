package com.example.data.account

import java.util.concurrent.TimeUnit

/** Persian-facing formatting for quota and subscription time. */
object Quota {

    private val digits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** Converts ASCII digits in [value] to Persian ones. */
    fun fa(value: String): String = buildString {
        value.forEach { append(if (it in '0'..'9') digits[it - '0'] else it) }
    }

    /** "۱٫۵ گیگابایت" style traffic size. */
    fun bytes(value: Long): String {
        if (value == Account.UNLIMITED || value == Long.MAX_VALUE) return "نامحدود"
        if (value <= 0) return "۰"
        val gb = value / 1024.0 / 1024 / 1024
        val mb = value / 1024.0 / 1024
        return when {
            gb >= 1 -> fa(String.format("%.1f", gb).replace('.', '٫')) + " گیگابایت"
            mb >= 1 -> fa(String.format("%.0f", mb)) + " مگابایت"
            else -> fa((value / 1024).toString()) + " کیلوبایت"
        }
    }

    /** Compact form for tight spaces: "۱٫۵ گیگ". */
    fun bytesShort(value: Long): String {
        if (value == Account.UNLIMITED || value == Long.MAX_VALUE) return "∞"
        val gb = value / 1024.0 / 1024 / 1024
        val mb = value / 1024.0 / 1024
        return when {
            gb >= 1 -> fa(String.format("%.1f", gb).replace('.', '٫')) + "G"
            mb >= 1 -> fa(String.format("%.0f", mb)) + "M"
            else -> fa((value / 1024).coerceAtLeast(0).toString()) + "K"
        }
    }

    /** "۱۲ روز و ۴ ساعت" style remaining time. */
    fun duration(millis: Long): String {
        if (millis == Long.MAX_VALUE) return "نامحدود"
        if (millis <= 0) return "پایان یافته"
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            days > 0 -> "${fa(days.toString())} روز و ${fa(hours.toString())} ساعت"
            hours > 0 -> "${fa(hours.toString())} ساعت و ${fa(minutes.toString())} دقیقه"
            else -> "${fa(minutes.toString())} دقیقه"
        }
    }

    /** Compact remaining time: "۱۲ روز". */
    fun durationShort(millis: Long): String {
        if (millis == Long.MAX_VALUE) return "∞"
        if (millis <= 0) return "۰"
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        return if (days > 0) "${fa(days.toString())} روز" else "${fa(hours.toString())} ساعت"
    }

    /** Human sentence explaining why an account is locked. */
    fun lockMessage(reason: LockReason): String = when (reason) {
        LockReason.EXPIRED -> "زمان سرویس شما به پایان رسیده است"
        LockReason.QUOTA_EXHAUSTED -> "حجم سرویس شما به پایان رسیده است"
        LockReason.SUSPENDED -> "حساب شما توسط مدیر موقتاً غیرفعال شده است"
        LockReason.NONE -> ""
    }

    fun lockTitle(reason: LockReason): String = when (reason) {
        LockReason.EXPIRED -> "پایان زمان سرویس"
        LockReason.QUOTA_EXHAUSTED -> "پایان حجم سرویس"
        LockReason.SUSPENDED -> "حساب غیرفعال"
        LockReason.NONE -> ""
    }
}
