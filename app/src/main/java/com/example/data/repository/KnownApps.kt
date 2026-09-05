package com.example.data.repository

import com.example.data.model.BypassApp

/** Categorisation helper + offline fallback list for the split-tunnel screen. */
object KnownApps {

    private val banking = setOf(
        "com.bmi.mobilebank", "ir.tejaratbank.mobilebank", "com.mellat.mobilebank",
        "mob.banking.android.sepah", "com.pmb.mobile", "ir.bmi.mobilebanking",
        "com.isc.bmi", "ir.ba24.mobilebank", "com.parsian.mobilebank"
    )
    private val payment = setOf("com.sep.sepapp", "ir.zarinpal", "com.farashenasa", "ir.asanpardakht.android")
    private val transport = setOf("cab.snapp.passenger", "taxi.tap30.passenger", "ir.miare.courier")
    private val shopping = setOf("com.digikala", "ir.divar", "ir.basalam.app", "com.torob")
    private val messaging = setOf(
        "org.telegram.messenger", "com.whatsapp", "com.instagram.android",
        "org.thoughtcrime.securesms", "com.discord", "com.twitter.android"
    )

    fun categoryOf(packageName: String, isSystem: Boolean): String = when {
        packageName in banking -> "بانکی و مالی"
        packageName in payment -> "پرداخت و تراکنش"
        packageName in transport -> "حمل و نقل"
        packageName in shopping -> "خرید و نیازمندی"
        packageName in messaging -> "پیام‌رسان و شبکه اجتماعی"
        packageName.startsWith("ir.") || packageName.endsWith(".ir") -> "اپ داخلی"
        isSystem -> "سیستمی"
        else -> "سایر"
    }

    /** Used when the platform refuses to list packages (e.g. restricted profiles). */
    fun fallback(bypassed: Set<String>): List<BypassApp> = listOf(
        Triple("com.bmi.mobilebank", "همراه‌بانک ملی ایران (BAM)", "بانکی و مالی"),
        Triple("ir.tejaratbank.mobilebank", "همراه‌بانک تجارت", "بانکی و مالی"),
        Triple("com.mellat.mobilebank", "همراه‌بانک ملت", "بانکی و مالی"),
        Triple("com.sep.sepapp", "آپ / ۷۸۰", "پرداخت و تراکنش"),
        Triple("cab.snapp.passenger", "اسنپ", "حمل و نقل"),
        Triple("taxi.tap30.passenger", "تپسی", "حمل و نقل"),
        Triple("ir.divar", "دیوار", "خرید و نیازمندی"),
        Triple("com.digikala", "دیجی‌کالا", "خرید و نیازمندی")
    ).map { (pkg, name, category) ->
        BypassApp(pkg, name, category, isBypassed = pkg in bypassed)
    }
}
