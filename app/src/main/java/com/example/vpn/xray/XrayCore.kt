package com.example.vpn.xray

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import java.io.File

/**
 * Thin wrapper around the prebuilt `libv2ray` Xray core.
 *
 * The core is reached entirely through reflection rather than a compile-time
 * dependency. That is a deliberate trade: the AAR is 59 MB and is fetched by CI
 * instead of being committed, so the source must compile whether or not it is
 * present. Binding directly to `libv2ray.CoreController` would make every build
 * without the artifact fail to compile, including local ones.
 *
 * All entry points degrade to a clear failure when the core is unavailable, and
 * [isAvailable] lets callers fall back to the built-in Kotlin engine.
 */
object XrayCore {

    private const val TAG = "XrayCore"

    /** Set once we have successfully reflected the core's classes. */
    @Volatile
    private var controller: Any? = null

    @Volatile
    private var running = false

    val isRunning: Boolean get() = running

    /**
     * Whether the Xray core is actually bundled in this build.
     *
     * Checks the build flag first — that is cheap and authoritative — then
     * confirms the class really loaded, which catches a stripped or corrupt AAR.
     */
    val isAvailable: Boolean by lazy {
        if (!BuildConfig.HAS_XRAY) {
            false
        } else {
            runCatching { Class.forName("libv2ray.CoreController") }
                .onFailure { Log.w(TAG, "HAS_XRAY set but the class is missing", it) }
                .isSuccess
        }
    }

    /**
     * Prepares the core's environment.
     *
     * No geo databases are unpacked: the generated config uses explicit CIDRs
     * and plain resolvers instead of geoip/geosite rules, which kept 27 MB of
     * lookup tables out of the APK. The asset directory is still registered
     * because the core expects a writable path for its certificate store.
     */
    fun initialise(context: Context) {
        if (!isAvailable) return

        val assetDir = File(context.filesDir, "xray").apply { mkdirs() }

        runCatching {
            val clazz = Class.forName("libv2ray.Libv2ray")
            val init = clazz.methods.firstOrNull {
                it.name.equals("initCoreEnv", ignoreCase = true) &&
                    it.parameterTypes.size == 2
            } ?: error("initCoreEnv not found in libv2ray")
            init.invoke(null, assetDir.absolutePath, "")
        }.onFailure { Log.w(TAG, "initCoreEnv failed", it) }
    }

    /**
     * Starts the core with [configJson], handing it the TUN descriptor.
     *
     * @param tunFd file descriptor from VpnService.Builder.establish(), or 0 to
     *   run without a TUN and serve the local SOCKS inbound only.
     * @return null on success, otherwise a human-readable reason.
     */
    fun start(configJson: String, tunFd: Int, onStatus: (String) -> Unit): String? {
        if (!isAvailable) return "هستهٔ Xray در این نسخه موجود نیست"
        if (running) return null

        return runCatching {
            val callbackClass = Class.forName("libv2ray.CoreCallbackHandler")
            val handler = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, args ->
                when (method.name) {
                    "onEmitStatus" -> {
                        val message = args?.getOrNull(1) as? String ?: ""
                        if (message.isNotBlank()) onStatus(message)
                        0L
                    }
                    // Startup/Shutdown must return an int; anything else makes
                    // the Go bridge throw.
                    else -> 0L
                }
            }

            val libClass = Class.forName("libv2ray.Libv2ray")
            val factory = libClass.methods.firstOrNull {
                it.name.equals("newCoreController", ignoreCase = true) &&
                    it.parameterTypes.size == 1
            } ?: error("newCoreController not found in libv2ray")
            val instance = factory.invoke(null, handler)
                ?: error("newCoreController returned null")

            val startLoop = instance.javaClass.methods.firstOrNull {
                it.name.equals("startLoop", ignoreCase = true) &&
                    it.parameterTypes.size == 2
            } ?: error("startLoop not found on CoreController")
            startLoop.invoke(instance, configJson, tunFd)

            controller = instance
            running = true
            null
        }.getOrElse { error ->
            running = false
            controller = null
            val cause = error.cause ?: error
            Log.e(TAG, "Xray start failed", cause)
            cause.message ?: cause::class.java.simpleName
        }
    }

    /** Stops the core. Safe to call when it was never started. */
    fun stop() {
        val instance = controller ?: return
        runCatching {
            val stopLoop = instance.javaClass.methods.firstOrNull {
                it.name.equals("stopLoop", ignoreCase = true) && it.parameterTypes.isEmpty()
            } ?: error("stopLoop not found on CoreController")
            stopLoop.invoke(instance)
        }.onFailure { Log.w(TAG, "stopLoop failed", it) }
        controller = null
        running = false
    }

    /** Core version string, or null when unavailable. */
    fun version(): String? = runCatching {
        val clazz = Class.forName("libv2ray.Libv2ray")
        clazz.methods.firstOrNull {
            it.name.equals("checkVersionX", ignoreCase = true) && it.parameterTypes.isEmpty()
        }?.invoke(null) as? String
    }.getOrNull()
}
