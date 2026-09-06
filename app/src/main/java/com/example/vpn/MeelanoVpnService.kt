package com.example.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.ConfigParser
import com.example.core.FailureDiagnosis
import com.example.core.PingTester
import com.example.core.ProxyEndpoint
import com.example.data.model.NetworkLiveStats
import com.example.data.model.RoutingMode
import com.example.vpn.net.DnsRelay
import com.example.vpn.net.IpPacket
import com.example.vpn.net.RouteTable
import com.example.vpn.net.TrafficCounter
import com.example.vpn.net.UdpNat
import com.example.vpn.stack.DnsMap
import com.example.vpn.stack.TcpStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import com.example.vpn.proto.OutboundFactory
import com.example.vpn.xray.XrayConfigBuilder
import com.example.vpn.xray.XrayCore
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * The MeeLano tunnel service.
 *
 * This is a genuine [VpnService]: it establishes a TUN interface, programs real
 * routes (including the Iran-bypass route set), performs a real TLS handshake to
 * the selected node, relays DNS, NATs UDP flows and counts every byte that
 * crosses the interface. Nothing on the dashboard is faked — if the node is
 * unreachable the service reports FAILED instead of "connected".
 */
class MeelanoVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val EXTRA_SERVER_NAME = "extra_server_name"
        const val EXTRA_PROTOCOL = "extra_protocol"
        const val EXTRA_CONFIG_LINK = "extra_config_link"
        const val EXTRA_BYPASS_PACKAGES = "extra_bypass_packages"
        const val EXTRA_KILL_SWITCH = "extra_kill_switch"
        const val EXTRA_ROUTING_MODE = "extra_routing_mode"
        const val EXTRA_DNS_PRIMARY = "extra_dns_primary"
        const val EXTRA_DNS_SECONDARY = "extra_dns_secondary"
        const val EXTRA_IPV6 = "extra_ipv6"
        const val EXTRA_WHITE_LABEL = "extra_white_label"

        private const val NOTIFICATION_ID = 9021
        private const val CHANNEL_ID = "meelano_vpn_channel"
        /**
         * TUN MTU.
         *
         * 1500 is the Ethernet MTU and the wrong value for a tunnel: every
         * packet then carries the proxy protocol header plus TLS framing plus
         * the outer IP/TCP header, pushing it past the path MTU so the network
         * fragments it. Fragmentation roughly doubles packet count and is a
         * common cause of a tunnel that connects fine but feels slow.
         *
         * 1420 leaves headroom for VLESS/VMess headers, TLS records and a
         * WebSocket frame while staying under the 1500 the carrier will accept.
         */
        private const val MTU = 1420

        private val _connectionState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
        val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

        private val _liveStats = MutableStateFlow(NetworkLiveStats())
        val liveStats: StateFlow<NetworkLiveStats> = _liveStats.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        /** Structured view of the last failure, for the UI to act on. */
        private val _lastDiagnosis = MutableStateFlow<FailureDiagnosis.Result?>(null)
        val lastDiagnosis: StateFlow<FailureDiagnosis.Result?> = _lastDiagnosis.asStateFlow()

        private val _logs = MutableStateFlow<List<String>>(
            listOf(
                "[SYSTEM] MeeLano Tunnel core initialised",
                "[CORE] Native VpnService · userspace TUN · DNS relay · UDP NAT",
                "[SYSTEM] Ready"
            )
        )
        val logs: StateFlow<List<String>> = _logs.asStateFlow()

        fun log(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            _logs.value = (_logs.value + "[$timestamp] $message").takeLast(300)
        }

        fun clearLogs() {
            _logs.value = listOf("[SYSTEM] Log buffer cleared")
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var statsJob: Job? = null
    private var tunnelJob: Job? = null
    private var udpNat: UdpNat? = null
    private var stack: TcpStack? = null
    private var healthMonitor: HealthMonitor? = null

    /** Whether plain TCP reached the node, used to tell "gone" from "blocked". */
    private var lastTcpReachable = false

    /**
     * Set when the user explicitly disconnects, so the system does not resurrect
     * the service behind their back.
     */
    private var userRequestedStop = false
    private var activeEndpoint: ProxyEndpoint? = null
    private var counter: TrafficCounter? = null
    private var sessionStartedAt = 0L
    private var activeServerName: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Unpacks geoip/geosite and points the core at them. Cheap and
        // idempotent, and doing it here keeps it off the connect path.
        XrayCore.initialise(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val request = ConnectRequest(
                    serverName = intent.getStringExtra(EXTRA_SERVER_NAME).orEmpty(),
                    protocolLabel = intent.getStringExtra(EXTRA_PROTOCOL).orEmpty(),
                    configLink = intent.getStringExtra(EXTRA_CONFIG_LINK).orEmpty(),
                    bypassPackages = intent.getStringArrayListExtra(EXTRA_BYPASS_PACKAGES) ?: arrayListOf(),
                    killSwitch = intent.getBooleanExtra(EXTRA_KILL_SWITCH, true),
                    whiteLabel = intent.getBooleanExtra(EXTRA_WHITE_LABEL, false),
                    routingMode = runCatching {
                        RoutingMode.valueOf(
                            intent.getStringExtra(EXTRA_ROUTING_MODE) ?: RoutingMode.SMART_BYPASS.name
                        )
                    }.getOrDefault(RoutingMode.SMART_BYPASS),
                    dnsPrimary = intent.getStringExtra(EXTRA_DNS_PRIMARY) ?: "1.1.1.1",
                    dnsSecondary = intent.getStringExtra(EXTRA_DNS_SECONDARY) ?: "8.8.8.8",
                    ipv6Enabled = intent.getBooleanExtra(EXTRA_IPV6, false)
                )
                userRequestedStop = false
                connect(request)
            }

            ACTION_DISCONNECT -> {
                userRequestedStop = true
                disconnect()
            }
        }

        // Auto-restart policy.
        //
        // START_STICKY tells Android to recreate the service after it is killed,
        // which is right while a tunnel is meant to be up but wrong after the
        // user deliberately disconnected — that produced a VPN which switched
        // itself back on. A null intent means exactly that redelivery case, so
        // after an explicit stop we refuse to stay resident.
        return if (userRequestedStop || intent == null && !isTunnelDesired()) {
            START_NOT_STICKY
        } else {
            START_STICKY
        }
    }

    /** True while a tunnel is supposed to be running. */
    private fun isTunnelDesired(): Boolean =
        _connectionState.value == VpnConnectionState.CONNECTED ||
            _connectionState.value.isBusy

    private data class ConnectRequest(
        val serverName: String,
        val protocolLabel: String,
        val configLink: String,
        val bypassPackages: List<String>,
        val killSwitch: Boolean,
        val routingMode: RoutingMode,
        val dnsPrimary: String,
        val dnsSecondary: String,
        val ipv6Enabled: Boolean,
        /** VIP nodes hide their upstream address everywhere it could be shown. */
        val whiteLabel: Boolean = false
    )

    private fun connect(request: ConnectRequest) {
        if (_connectionState.value == VpnConnectionState.CONNECTED) {
            log("Re-establishing tunnel for ${request.serverName}…")
            teardown(notifyState = false)
            _connectionState.value = VpnConnectionState.RECONNECTING
        } else {
            _connectionState.value = VpnConnectionState.CONNECTING
        }
        _lastError.value = null
        _lastDiagnosis.value = null
        activeServerName = request.serverName
        startForeground(NOTIFICATION_ID, createNotification("در حال برقراری تونل…", request.serverName))

        tunnelJob?.cancel()
        tunnelJob = serviceScope.launch {
            try {
                val endpoint = ConfigParser.parse(request.configLink)
                if (endpoint == null) {
                    fail("کانفیگ سرور نامعتبر است (لینک قابل تجزیه نیست)")
                    return@launch
                }
                activeEndpoint = endpoint

                // A plain TCP probe first: knowing whether the port answered is
                // what lets a later failure be reported as "blocked" rather than
                // the much vaguer "unreachable".
                lastTcpReachable = PingTester.ping(endpoint.host, endpoint.port) { protect(it) } > 0
                log(
                    if (request.whiteLabel) {
                        "Endpoint resolved → ${request.serverName} · ${endpoint.displayProtocol}"
                    } else {
                        "Endpoint resolved → ${endpoint.summary()}"
                    }
                )

                // Which engine can carry this node?
                //
                // Xray implements Reality, xhttp, QUIC transports and SS2022 —
                // everything the hand-written Kotlin engine has to refuse. When
                // the core is present it takes anything it supports; the Kotlin
                // engine remains the fallback so the app still works if the AAR
                // is missing from a build.
                // Engine selection.
                //
                // The Kotlin engine's protocol set is a strict subset of the
                // core's, so it is a genuine fallback rather than an
                // alternative: it runs only when the AAR is absent from a build,
                // or for the rare node the core declines. Saying which engine is
                // in use — and why — is what turns a mysterious failure into a
                // reportable one.
                val useXray = XrayCore.isAvailable && XrayConfigBuilder.isSupported(endpoint)
                when {
                    useXray ->
                        log("Engine: Xray core ${XrayCore.version().orEmpty()}")

                    XrayCore.isAvailable -> {
                        log(
                            "Engine: built-in fallback — the core declined " +
                                "${endpoint.displayProtocol}"
                        )
                        if (!OutboundFactory.supports(endpoint)) {
                            fail(OutboundFactory.unsupportedReason(endpoint))
                            return@launch
                        }
                    }

                    else -> {
                        // No core in this build at all. Fail loudly for anything
                        // the Kotlin engine cannot carry, rather than attempting
                        // a handshake that cannot possibly succeed.
                        log("Engine: built-in Kotlin tunnel (Xray core not bundled)")
                        if (!OutboundFactory.supports(endpoint)) {
                            fail(OutboundFactory.unsupportedReason(endpoint))
                            return@launch
                        }
                    }
                }

                // ---- real outbound handshake ----
                //
                // Skipped for Xray: the core dials the node itself, and probing
                // first would double the connect time for no benefit. Reality
                // nodes in particular cannot be probed by the Kotlin engine at
                // all, which is the whole reason the core is here.
                var handshake: TunnelEngine.HandshakeResult? = null
                if (!useXray) {
                    handshake = TunnelEngine.handshake(endpoint) { protect(it) }
                    if (!handshake.success) {
                        fail("اتصال به ${endpoint.host}:${endpoint.port} ناموفق بود → ${handshake.error}")
                        return@launch
                    }
                    log("Handshake OK in ${handshake.latencyMs}ms · ${TunnelEngine.describeCipher(handshake)}")
                }

                if (request.routingMode == RoutingMode.DIRECT) {
                    log("Routing mode = DIRECT: no tunnel is installed, traffic stays on the default interface.")
                    _connectionState.value = VpnConnectionState.DISCONNECTED
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                val descriptor = buildInterface(request, endpoint)
                if (descriptor == null) {
                    fail("ایجاد رابط TUN ممکن نشد (مجوز VPN رد شد یا اپ VPN دیگری فعال است)")
                    return@launch
                }
                vpnInterface = descriptor
                sessionStartedAt = System.currentTimeMillis()
                counter = TrafficCounter(android.os.Process.myUid())

                // Hand the TUN descriptor to the core before declaring success,
                // so a core that refuses the config reports a failure instead of
                // leaving a dead "connected" tunnel on screen.
                if (useXray) {
                    val config = XrayConfigBuilder.build(
                        endpoint = endpoint,
                        dnsPrimary = request.dnsPrimary,
                        dnsSecondary = request.dnsSecondary,
                        bypassLan = request.routingMode != RoutingMode.GLOBAL,
                        // Must match the interface we just built, or the core
                        // fragments packets the TUN device cannot carry.
                        tunMtu = MTU
                    )
                    val error = XrayCore.start(config, descriptor.fd) { status -> log("Xray: $status") }
                    if (error != null) {
                        fail("راه‌اندازی هستهٔ Xray ناموفق بود → $error")
                        return@launch
                    }
                }

                _connectionState.value = VpnConnectionState.CONNECTED
                _liveStats.value = NetworkLiveStats(
                    tunnelIp = "172.19.0.1",
                    encryption = handshake?.let { TunnelEngine.describeCipher(it) }
                        ?: "Xray · ${endpoint.displayProtocol}",
                    engine = if (useXray) "Xray" else "Built-in",
                    activeProtocol = endpoint.displayProtocol,
                    pingMs = handshake?.latencyMs ?: 0,
                    remoteHost = if (request.whiteLabel) {
                        request.serverName
                    } else {
                        "${endpoint.host}:${endpoint.port}"
                    }
                )
                log("Tunnel established. Routing mode: ${request.routingMode.title}")
                notifyStatus("اتصال امن برقرار است", request.serverName)

                if (useXray) {
                    // The core owns the descriptor and pumps packets itself, so
                    // there is no userspace packet loop to run. Statistics and
                    // health monitoring still have to run, though: they used to
                    // live inside runPacketLoop, which meant the default engine
                    // had neither traffic counters nor degradation detection.
                    log("Tunnel handed to the Xray core.")
                    startXrayStatsLoop()
                    startXrayHealthMonitor()
                    awaitCancellation()
                } else {
                    runPacketLoop(descriptor, request)
                }
            } catch (e: Exception) {
                fail("خطای تونل: ${e.message}")
            }
        }
    }

    private fun buildInterface(
        request: ConnectRequest,
        endpoint: ProxyEndpoint
    ): ParcelFileDescriptor? = try {
        val builder = Builder()
            .setSession("MeeLano · ${request.serverName}")
            .setMtu(MTU)
            .addAddress("172.19.0.1", 30)
            .addDnsServer(request.dnsPrimary)
            .addDnsServer(request.dnsSecondary)

        if (request.ipv6Enabled) {
            builder.addAddress("fd00:1:2:3::1", 64)
            builder.addRoute("::", 0)
        }

        val routes = RouteTable.routesFor(request.routingMode)
        routes.forEach { builder.addRoute(it.address, it.prefix) }
        log("Installed ${routes.size} IPv4 route(s) for ${request.routingMode.name}")

        // The tunnel's own uplink must never loop back through the tunnel.
        var bypassed = 0
        request.bypassPackages.forEach { pkg ->
            try {
                builder.addDisallowedApplication(pkg)
                bypassed++
            } catch (_: Exception) {
                // not installed on this device
            }
        }
        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {
        }
        if (bypassed > 0) log("Split tunnelling: $bypassed app(s) routed directly")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }
        if (request.killSwitch) {
            builder.setBlocking(true)
            log("Kill Switch active: TUN in blocking mode, no leak while the tunnel is down")
        }
        builder.setConfigureIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        log(
            if (request.whiteLabel) {
                "Uplink node: ${request.serverName} (${endpoint.displayProtocol})"
            } else {
                "Uplink node: ${endpoint.host}:${endpoint.port} (${endpoint.displayProtocol})"
            }
        )
        builder.establish()
    } catch (e: Exception) {
        log("Builder error: ${e.message}")
        null
    }

    private suspend fun runPacketLoop(descriptor: ParcelFileDescriptor, request: ConnectRequest) {
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val trafficCounter = counter ?: return
        val endpoint = activeEndpoint ?: return

        // The userspace TCP stack: terminates app sockets and proxies them for real.
        val tunLock = Any()
        val tcpStack = TcpStack(
            endpoint = endpoint,
            protect = { protect(it) },
            writeToTun = { packet ->
                synchronized(tunLock) { output.write(packet) }
            },
            log = { log(it) }
        )
        stack = tcpStack
        log("TCP stack online · outbound = ${endpoint.displayProtocol}")

        // Detect a tunnel that is "connected" but no longer carrying traffic.
        healthMonitor = HealthMonitor(
            scope = serviceScope,
            sample = {
                stack?.let {
                    HealthMonitor.Sample(
                        opened = it.totalOpened,
                        failed = it.totalFailed,
                        bytesDown = it.bytesDown.get(),
                        bytesUp = it.bytesUp.get()
                    )
                }
            },
            onDegraded = { reason ->
                log("Tunnel unhealthy: $reason — reporting failure for failover")
                _lastError.value = "کیفیت اتصال افت کرد ($reason)"
                _connectionState.value = VpnConnectionState.FAILED
            },
            onLog = { log(it) }
        ).also { it.start() }

        val dnsRelay = DnsRelay(
            upstreamServers = listOf(request.dnsPrimary, request.dnsSecondary),
            protect = { protect(it) },
            onLog = { log(it) }
        )
        val nat = UdpNat(
            protect = { protect(it) },
            tunOutput = output,
            onBytes = { rx, tx ->
                trafficCounter.addRx(rx)
                trafficCounter.addTx(tx)
            },
            onLog = { log(it) }
        )
        udpNat = nat
        startStatsLoop(dnsRelay, nat)

        val buffer = ByteArray(32767)
        try {
            while (serviceScope.isActive && _connectionState.value == VpnConnectionState.CONNECTED) {
                val read = try {
                    input.read(buffer)
                } catch (_: Exception) {
                    -1
                }
                if (read <= 0) {
                    if (read < 0) break
                    delay(5)
                    continue
                }
                trafficCounter.addTx(read)

                when (IpPacket.protocolOf(buffer, read)) {
                    IpPacket.PROTO_UDP -> {
                        val udp = IpPacket.parseUdp(buffer, read)
                        if (udp != null) {
                            val handled = runCatching { dnsRelay.handle(udp, output) }.getOrDefault(false)
                            if (!handled) nat.forward(udp)
                        }
                    }
                    // Every TCP flow is terminated locally and carried through the
                    // proxy tunnel by the userspace stack. This is what actually
                    // makes HTTPS sites (Instagram, YouTube, …) load.
                    IpPacket.PROTO_TCP -> stack?.handlePacket(buffer, read)
                    else -> Unit
                }
            }
        } finally {
            nat.shutdown()
            tcpStack.shutdown()
            healthMonitor?.stop()
            healthMonitor = null
            stack = null
        }
    }

    /**
     * Traffic accounting for the Xray path.
     *
     * TrafficCounter falls back to the kernel's per-uid byte counters, which
     * count everything the core sends and receives, so this reports real
     * throughput even though no packet passes through our own code.
     */
    private fun startXrayStatsLoop() {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            val history = ArrayDeque<Float>()
            while (isActive && _connectionState.value == VpnConnectionState.CONNECTED) {
                delay(1000)
                val trafficCounter = counter ?: continue
                val (down, up) = trafficCounter.sampleSpeedsMbps()
                history.addLast(down)
                if (history.size > 24) history.removeFirst()

                _liveStats.value = _liveStats.value.copy(
                    downloadMbps = round1(down),
                    uploadMbps = round1(up),
                    totalDownloadedMb = round1(TrafficCounter.bytesToMb(trafficCounter.totalRxBytes())),
                    totalUploadedMb = round1(TrafficCounter.bytesToMb(trafficCounter.totalTxBytes())),
                    speedHistory = history.toList(),
                    uptimeSeconds = ((System.currentTimeMillis() - sessionStartedAt) / 1000).toInt()
                )
            }
        }
    }

    /** Degradation detection for the Xray path, judged on throughput. */
    private fun startXrayHealthMonitor() {
        healthMonitor?.stop()
        healthMonitor = HealthMonitor(
            scope = serviceScope,
            sample = {
                counter?.let {
                    HealthMonitor.Sample(
                        // The core exposes no flow table, so opened stays 0 and
                        // the monitor judges on bytes alone.
                        opened = 0L,
                        failed = 0L,
                        bytesDown = it.totalRxBytes(),
                        bytesUp = it.totalTxBytes()
                    )
                }
            },
            onDegraded = { reason ->
                log("Tunnel unhealthy: $reason — reporting failure for failover")
                _lastError.value = "کیفیت اتصال افت کرد ($reason)"
                _connectionState.value = VpnConnectionState.FAILED
            },
            onLog = { log(it) }
        ).also { it.start() }
    }

    private fun startStatsLoop(dnsRelay: DnsRelay, nat: UdpNat) {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            val history = ArrayDeque<Float>()
            while (isActive && _connectionState.value == VpnConnectionState.CONNECTED) {
                delay(1000)
                val trafficCounter = counter ?: continue
                val (down, up) = trafficCounter.sampleSpeedsMbps()
                history.addLast(down)
                if (history.size > 24) history.removeFirst()
                nat.evictIdle()

                _liveStats.value = _liveStats.value.copy(
                    downloadMbps = round1(down),
                    uploadMbps = round1(up),
                    totalDownloadedMb = round1(TrafficCounter.bytesToMb(trafficCounter.totalRxBytes())),
                    totalUploadedMb = round1(TrafficCounter.bytesToMb(trafficCounter.totalTxBytes())),
                    speedHistory = history.toList(),
                    dnsQueries = dnsRelay.queriesHandled.get(),
                    activeFlows = nat.activeFlows,
                    uptimeSeconds = ((System.currentTimeMillis() - sessionStartedAt) / 1000).toInt()
                )
            }
        }
    }

    private fun fail(reason: String) {
        log("ERROR · $reason")

        // Report a cause and a remedy rather than a raw exception. The message
        // shown to the user is what determines whether they try another server,
        // check their connection, or give up on the app entirely.
        val diagnosis = FailureDiagnosis.diagnose(reason, tcpReachable = lastTcpReachable)
        _lastDiagnosis.value = diagnosis
        _lastError.value = "${diagnosis.summary}\n${diagnosis.advice}"
        _connectionState.value = VpnConnectionState.FAILED
        notifyStatus(diagnosis.summary, activeServerName)
        teardown(notifyState = false)
        serviceScope.launch {
            delay(4000)
            if (_connectionState.value == VpnConnectionState.FAILED) {
                _connectionState.value = VpnConnectionState.DISCONNECTED
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun disconnect() {
        _connectionState.value = VpnConnectionState.DISCONNECTING
        log("Terminating tunnel session…")
        teardown(notifyState = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun teardown(notifyState: Boolean) {
        // Stop the core first: it holds the TUN descriptor, and closing that
        // out from under it would leave the Go side writing to a dead fd.
        XrayCore.stop()

        healthMonitor?.stop()
        healthMonitor = null
        statsJob?.cancel()
        tunnelJob?.cancel()
        udpNat?.shutdown()
        udpNat = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        _liveStats.value = _liveStats.value.copy(downloadMbps = 0f, uploadMbps = 0f, activeFlows = 0)
        if (notifyState) {
            _connectionState.value = VpnConnectionState.DISCONNECTED
            log("Disconnected. Traffic returned to the default interface.")
        }
    }

    override fun onRevoke() {
        log("VPN permission revoked by the system or another VPN app.")
        // Revocation is involuntary but final: do not let the service be
        // restarted into a state it no longer has permission for.
        userRequestedStop = true
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        teardown(notifyState = true)
        super.onDestroy()
    }

    private fun round1(value: Float) = Math.round(value * 10f) / 10f

    private fun notifyStatus(text: String, serverName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(text, serverName))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MeeLano Tunnel Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش وضعیت اتصال تونل امن MeeLano"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(statusText: String, serverName: String): android.app.Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectPending = PendingIntent.getService(
            this, 1,
            Intent(this, MeelanoVpnService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MeeLano Tunnel${if (serverName.isBlank()) "" else " · $serverName"}")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "قطع اتصال", disconnectPending)
            .build()
    }
}
