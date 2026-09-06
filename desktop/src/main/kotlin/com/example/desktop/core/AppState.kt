package com.example.desktop.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.core.ProxyEndpoint
import com.example.desktop.ui.DesktopConnectionState
import com.example.vpn.proto.Destination
import com.example.vpn.proto.OutboundFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.prefs.Preferences

/** A server as the desktop UI shows it. */
data class DesktopServer(
    val id: String,
    val name: String,
    val country: String,
    val flag: String,
    val endpoint: ProxyEndpoint,
    val pingMs: Int = 0,
    val isVip: Boolean = false
)

/**
 * Everything the window binds to: server list, connection lifecycle and live
 * counters.
 *
 * Kept deliberately small and explicit rather than mirroring the Android
 * ViewModel, because the desktop shell has no VpnService, no permissions dance
 * and no failover state machine to reproduce.
 */
class AppState(private val scope: CoroutineScope) {

    private val prefs = Preferences.userRoot().node("com/example/meelano")

    val servers = mutableStateListOf<DesktopServer>()

    var connectionState by mutableStateOf(DesktopConnectionState.DISCONNECTED)
        private set
    var activeServer by mutableStateOf<DesktopServer?>(null)
        private set
    var statusMessage by mutableStateOf("آماده اتصال")
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    var downloadMbps by mutableStateOf(0f)
        private set
    var uploadMbps by mutableStateOf(0f)
        private set
    var totalDownBytes by mutableStateOf(0L)
        private set
    var totalUpBytes by mutableStateOf(0L)
        private set
    var uptimeSeconds by mutableStateOf(0)
        private set
    var systemProxyOn by mutableStateOf(false)
        private set

    val speedHistory = mutableStateListOf<Float>().apply { repeat(40) { add(0f) } }

    val logs = mutableStateListOf<String>()

    val proxy = LocalProxyServer(onLog = ::log)

    private var meterJob: Job? = null
    private var testJob: Job? = null

    init {
        loadServers()
    }

    // region servers

    private fun loadServers() {
        servers.clear()
        DesktopVipServers.all.forEachIndexed { index, node ->
            DesktopConfigParser.parse(node.link)?.let { endpoint ->
                servers.add(
                    DesktopServer(
                        id = "vip_$index",
                        name = node.name,
                        country = node.country,
                        flag = node.flag,
                        endpoint = endpoint,
                        isVip = true
                    )
                )
            }
        }
        // Restore whichever node the user last chose, else the first.
        val savedId = prefs.get(KEY_SERVER, null)
        activeServer = servers.firstOrNull { it.id == savedId } ?: servers.firstOrNull()
        log("${servers.size} سرور بارگذاری شد")
    }

    /** Adds nodes from a pasted subscription body or a single share link. */
    fun importLinks(text: String): Int {
        val parsed = DesktopConfigParser.parseSubscription(text)
        var added = 0
        parsed.forEach { endpoint ->
            val id = "imp_${endpoint.host}_${endpoint.port}"
            if (servers.none { it.id == id }) {
                servers.add(
                    DesktopServer(
                        id = id,
                        name = endpoint.remark.ifBlank { endpoint.host },
                        country = "وارد شده",
                        flag = "🌐",
                        endpoint = endpoint,
                        isVip = false
                    )
                )
                added++
            }
        }
        log(if (added > 0) "$added سرور اضافه شد" else "سرور تازه‌ای یافت نشد")
        return added
    }

    fun select(server: DesktopServer) {
        activeServer = server
        prefs.put(KEY_SERVER, server.id)
        if (connectionState == DesktopConnectionState.CONNECTED) {
            // Re-point the live proxy without tearing the listener down, so
            // existing applications keep their configuration.
            proxy.setEndpoint(server.endpoint)
            log("سرور به ${server.name} تغییر کرد")
        }
    }

    /** Measures TCP latency to every server, in parallel. */
    fun testAll() {
        testJob?.cancel()
        testJob = scope.launch {
            statusMessage = "در حال آزمایش ${servers.size} سرور"
            val previous = connectionState
            if (previous == DesktopConnectionState.DISCONNECTED) {
                connectionState = DesktopConnectionState.TESTING
            }

            val results = servers.map { server ->
                scope.launch(Dispatchers.IO) {
                    // Each probe is isolated: one bad host must not cancel the
                    // sweep, the same defect that broke the Android refresh.
                    val latency = runCatching { probe(server.endpoint) }.getOrDefault(-1)
                    val index = servers.indexOfFirst { it.id == server.id }
                    if (index >= 0) {
                        withContext(Dispatchers.Main) {
                            servers[index] = servers[index].copy(pingMs = latency)
                        }
                    }
                }
            }
            results.forEach { it.join() }

            val alive = servers.count { it.pingMs > 0 }
            statusMessage = "$alive سرور از ${servers.size} پاسخ داد"
            if (connectionState == DesktopConnectionState.TESTING) {
                connectionState = DesktopConnectionState.DISCONNECTED
            }
        }
    }

    private fun probe(endpoint: ProxyEndpoint, timeoutMs: Int = 2500): Int {
        val started = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(endpoint.host, endpoint.port), timeoutMs)
                (System.currentTimeMillis() - started).toInt().coerceAtLeast(1)
            }
        } catch (e: Exception) {
            -1
        }
    }

    // endregion

    // region connection

    fun toggle() {
        if (connectionState == DesktopConnectionState.CONNECTED || connectionState.isBusy) {
            disconnect()
        } else {
            connect()
        }
    }

    fun connect() {
        val server = activeServer ?: run {
            lastError = "ابتدا یک سرور انتخاب کنید"
            return
        }
        lastError = null
        connectionState = DesktopConnectionState.CONNECTING
        statusMessage = "در حال اتصال به ${server.name}"

        scope.launch {
            // Prove the node really carries traffic before claiming success.
            val ok = withContext(Dispatchers.IO) {
                runCatching { handshake(server) }.getOrElse { false }
            }
            if (!ok) {
                connectionState = DesktopConnectionState.FAILED
                statusMessage = "اتصال ناموفق"
                lastError = "سرور ${server.name} پاسخ نداد. سرور دیگری را امتحان کنید."
                log("اتصال به ${server.name} ناموفق بود")
                return@launch
            }

            proxy.resetCounters()
            proxy.setEndpoint(server.endpoint)
            val started = proxy.start()
            if (started.isFailure) {
                connectionState = DesktopConnectionState.FAILED
                lastError = "پورت ${proxy.listenPort} در دسترس نیست"
                return@launch
            }

            if (WindowsProxy.isWindows) {
                WindowsProxy.enable(proxy.listenPort)
                systemProxyOn = true
                log("پروکسی سیستم ویندوز فعال شد")
            } else {
                log("پروکسی محلی آماده است: 127.0.0.1:${proxy.listenPort}")
            }

            connectionState = DesktopConnectionState.CONNECTED
            statusMessage = "متصل به ${server.name}"
            startMeter()
        }
    }

    fun disconnect() {
        meterJob?.cancel()
        if (systemProxyOn) {
            WindowsProxy.disable()
            systemProxyOn = false
            log("پروکسی سیستم غیرفعال شد")
        }
        proxy.stop()
        connectionState = DesktopConnectionState.DISCONNECTED
        statusMessage = "قطع شد"
        downloadMbps = 0f
        uploadMbps = 0f
        uptimeSeconds = 0
        repeat(speedHistory.size) { speedHistory[it] = 0f }
    }

    /** Opens a real tunnel and fetches a known URL through it. */
    private fun handshake(server: DesktopServer): Boolean {
        val destination = Destination.of("www.gstatic.com", 80)
        val outbound = OutboundFactory.create(server.endpoint, destination) { true }
        return try {
            outbound.output.write(
                ("HEAD /generate_204 HTTP/1.1\r\n" +
                    "Host: www.gstatic.com\r\n" +
                    "User-Agent: Mozilla/5.0\r\n" +
                    "Connection: close\r\n\r\n").toByteArray()
            )
            outbound.output.flush()
            val buffer = ByteArray(256)
            val read = outbound.input.read(buffer)
            // Anything that comes back as an HTTP status line proves the node
            // relayed the request; a node that merely accepts TCP does not.
            read > 0 && String(buffer, 0, read).startsWith("HTTP/")
        } catch (e: Exception) {
            false
        } finally {
            runCatching { outbound.close() }
        }
    }

    private fun startMeter() {
        meterJob?.cancel()
        meterJob = scope.launch {
            var lastDown = 0L
            var lastUp = 0L
            var seconds = 0
            while (isActive && connectionState == DesktopConnectionState.CONNECTED) {
                delay(1000)
                seconds++
                uptimeSeconds = seconds

                val down = proxy.bytesDown.get()
                val up = proxy.bytesUp.get()
                totalDownBytes = down
                totalUpBytes = up

                downloadMbps = ((down - lastDown) * 8f) / 1_000_000f
                uploadMbps = ((up - lastUp) * 8f) / 1_000_000f
                lastDown = down
                lastUp = up

                speedHistory.removeAt(0)
                speedHistory.add(downloadMbps)
            }
        }
    }

    // endregion

    fun log(message: String) {
        val stamp = java.time.LocalTime.now().withNano(0).toString()
        logs.add(0, "[$stamp] $message")
        if (logs.size > 200) logs.removeAt(logs.lastIndex)
    }

    fun shutdown() {
        if (systemProxyOn) WindowsProxy.disable()
        proxy.stop()
    }

    private companion object {
        const val KEY_SERVER = "selected_server"
    }
}
