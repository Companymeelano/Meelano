package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object SmartImportHelper {

    private const val PACKAGE_V2RAYNG = "com.v2ray.ang"
    private const val PACKAGE_V2BOX = "dev.v2box.app"

    fun openInDestinationApp(context: Context, configLink: String): Boolean {
        // Copy link to clipboard first to ensure seamless import in all apps
        copyToClipboard(context, configLink, showToast = false)

        val pm = context.packageManager
        val v2rayNgIntent = pm.getLaunchIntentForPackage(PACKAGE_V2RAYNG)
        val v2boxIntent = pm.getLaunchIntentForPackage(PACKAGE_V2BOX)

        if (v2rayNgIntent != null) {
            v2rayNgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(v2rayNgIntent)
            Toast.makeText(context, "در حال باز کردن v2rayNG... کانفیگ در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
            return true
        } else if (v2boxIntent != null) {
            v2boxIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(v2boxIntent)
            Toast.makeText(context, "در حال باز کردن V2Box... کانفیگ در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
            return true
        } else {
            // Try generic view intent with config URI
            try {
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(configLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
                return true
            } catch (_: Exception) {
                // Not installed, return false to trigger Play Store fallback dialog
                return false
            }
        }
    }

    fun openPlayStore(context: Context, packageName: String = PACKAGE_V2RAYNG) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    fun copyToClipboard(context: Context, text: String, showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MeeLano Config", text)
        clipboard.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(context, "کانفیگ با موفقیت کپی شد ✓", Toast.LENGTH_SHORT).show()
        }
    }
}
