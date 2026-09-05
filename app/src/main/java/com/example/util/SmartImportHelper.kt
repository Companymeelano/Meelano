package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Hands a config link to whichever compatible client is installed, and offers
 * generic share / clipboard fallbacks. Also reads links back off the clipboard
 * for the in-app import flow.
 */
object SmartImportHelper {

    val SUPPORTED_CLIENTS = listOf(
        "com.v2ray.ang" to "v2rayNG",
        "dev.v2box.app" to "V2Box",
        "app.hiddify.com" to "Hiddify",
        "com.github.kr328.clash" to "Clash Meta",
        "io.nekohasekai.sfa" to "sing-box"
    )

    fun installedClients(context: Context): List<Pair<String, String>> =
        SUPPORTED_CLIENTS.filter { (pkg, _) ->
            context.packageManager.getLaunchIntentForPackage(pkg) != null
        }

    fun openInDestinationApp(context: Context, configLink: String): Boolean {
        copyToClipboard(context, configLink, showToast = false)

        // 1. Deep-link straight into a known client with the config URI.
        for ((pkg, label) in SUPPORTED_CLIENTS) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: continue
            val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse(configLink)).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val opened = runCatching { context.startActivity(deepLink); true }.getOrElse {
                runCatching {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                    true
                }.getOrDefault(false)
            }
            if (opened) {
                Toast.makeText(context, "در حال باز کردن $label — کانفیگ در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
                return true
            }
        }

        // 2. Any app registered for the scheme.
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(configLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        }.getOrDefault(false)
    }

    fun shareConfig(context: Context, configLink: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, configLink)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری کانفیگ").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun openPlayStore(context: Context, packageName: String = "com.v2ray.ang") {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MeeLano Config", text))
        if (showToast) {
            Toast.makeText(context, "کانفیگ کپی شد ✓", Toast.LENGTH_SHORT).show()
        }
    }

    fun readClipboard(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
    }
}
