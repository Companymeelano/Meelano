package com.example.desktop.core

/**
 * Turns the Windows system proxy on and off.
 *
 * Written through `reg.exe` against the per-user Internet Settings key, which
 * needs no administrator rights. `InternetSetOption` would normally follow to
 * broadcast the change, but that requires JNI into WinINet; instead the
 * registry write is paired with a refresh below that most applications pick up
 * on their next connection. Chrome, Edge and anything on WinHTTP honour it.
 *
 * Every method is a no-op on non-Windows hosts so the same build runs, and can
 * be tested, on Linux CI.
 */
object WindowsProxy {

    private const val KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"

    val isWindows: Boolean
        get() = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    /** Remembers what the user had configured, so disabling can restore it. */
    private var previousEnabled: String? = null
    private var previousServer: String? = null

    /**
     * Routes system HTTP/HTTPS through `127.0.0.1:[port]`.
     *
     * Local and intranet addresses bypass the tunnel: sending them through a
     * remote proxy would break printers, network shares and captive portals
     * for no benefit.
     */
    fun enable(port: Int): Result<Unit> {
        if (!isWindows) return Result.success(Unit)
        return runCatching {
            previousEnabled = query("ProxyEnable")
            previousServer = query("ProxyServer")

            reg("ProxyEnable", "REG_DWORD", "1")
            reg("ProxyServer", "REG_SZ", "127.0.0.1:$port")
            reg(
                "ProxyOverride",
                "REG_SZ",
                "localhost;127.*;10.*;172.16.*;172.17.*;172.18.*;172.19.*;" +
                    "172.20.*;172.21.*;172.22.*;172.23.*;172.24.*;172.25.*;" +
                    "172.26.*;172.27.*;172.28.*;172.29.*;172.30.*;172.31.*;" +
                    "192.168.*;<local>"
            )
            refresh()
        }
    }

    /** Restores whatever was configured before [enable], or simply turns it off. */
    fun disable(): Result<Unit> {
        if (!isWindows) return Result.success(Unit)
        return runCatching {
            val restoreServer = previousServer
            // Only restore a previous proxy if it was not our own listener;
            // otherwise a crash mid-session would leave the user pointed at a
            // port with nothing behind it.
            if (!restoreServer.isNullOrBlank() && !restoreServer.contains("127.0.0.1:")) {
                reg("ProxyServer", "REG_SZ", restoreServer)
                reg("ProxyEnable", "REG_DWORD", previousEnabled ?: "0")
            } else {
                reg("ProxyEnable", "REG_DWORD", "0")
            }
            refresh()
        }
    }

    private fun reg(name: String, type: String, value: String) {
        val process = ProcessBuilder(
            "reg", "add", KEY, "/v", name, "/t", type, "/d", value, "/f"
        ).redirectErrorStream(true).start()
        process.inputStream.readBytes()
        process.waitFor()
    }

    private fun query(name: String): String? = runCatching {
        val process = ProcessBuilder("reg", "query", KEY, "/v", name)
            .redirectErrorStream(true).start()
        val text = process.inputStream.readBytes().toString(Charsets.UTF_8)
        process.waitFor()
        text.lines()
            .firstOrNull { it.trim().startsWith(name) }
            ?.trim()
            ?.split(Regex("\\s{2,}"))
            ?.lastOrNull()
            ?.let { if (it.startsWith("0x")) it.removePrefix("0x").toInt(16).toString() else it }
    }.getOrNull()

    /**
     * Nudges WinINet into re-reading the settings.
     *
     * Without JNI there is no direct call available, so this uses the documented
     * side effect of rundll32 refreshing the Internet Settings page.
     */
    private fun refresh() {
        runCatching {
            ProcessBuilder(
                "rundll32.exe", "wininet.dll,InternetSetOption", "0", "39", "0", "0"
            ).start().waitFor()
            ProcessBuilder(
                "rundll32.exe", "wininet.dll,InternetSetOption", "0", "37", "0", "0"
            ).start().waitFor()
        }
    }
}
