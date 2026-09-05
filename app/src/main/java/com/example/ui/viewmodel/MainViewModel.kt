package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CoreProtocolFilter
import com.example.data.model.NetworkLiveStats
import com.example.data.model.RoutingMode
import com.example.data.model.ServerSort
import com.example.data.model.VpnServer
import com.example.data.repository.ServerRepository
import com.example.data.security.SecurityManager
import com.example.data.settings.SettingsStore
import com.example.util.SmartImportHelper
import com.example.core.SpeedTester
import com.example.core.ConfigParser
import com.example.core.ConnectionAdvisor
import com.example.util.SoundEngine
import com.example.vpn.MeelanoVpnService
import com.example.vpn.VpnConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ServerRepository,
    private val settings: SettingsStore,
    val securityManager: SecurityManager
) : ViewModel() {

    /** Application context remembered from the last connect, for auto-retry. */
    private var appContext: Context? = null

    /**
     * Learns which nodes work on this device and network.
     *
     * Created lazily because the ViewModel is constructed without a Context;
     * every caller already goes through [startVpn] or [attach], which set one.
     */
    private var advisorRef: ConnectionAdvisor? = null
    val advisor: ConnectionAdvisor?
        get() = advisorRef

    /** Gives the ViewModel a Context so device-local learning can start. */
    fun attach(context: Context) {
        appContext = context.applicationContext
        if (advisorRef == null) {
            advisorRef = ConnectionAdvisor(context.applicationContext)
        }
    }

    // ---- tunnel state ----
    val connectionState: StateFlow<VpnConnectionState> = MeelanoVpnService.connectionState
    val liveStats: StateFlow<NetworkLiveStats> = MeelanoVpnService.liveStats
    val logs: StateFlow<List<String>> = MeelanoVpnService.logs
    val lastError: StateFlow<String?> = MeelanoVpnService.lastError

    // ---- servers ----
    val activeServer: StateFlow<VpnServer> = repository.activeServer
    val vipServers: StateFlow<List<VpnServer>> = repository.vipServers
    val freeServers: StateFlow<List<VpnServer>> = repository.freeServers
    val customServers: StateFlow<List<VpnServer>> = repository.customServers
    val bypassApps = repository.bypassApps
    val updateProgress = repository.updateProgress

    // ---- persisted settings ----
    val routingMode = state(settings.routingMode, RoutingMode.SMART_BYPASS)
    val coreProtocolFilter = state(settings.protocolFilter, CoreProtocolFilter.ALL)
    val serverSort = state(settings.serverSort, ServerSort.PING)
    val killSwitchEnabled = state(settings.killSwitch, true)
    val smartFailoverEnabled = state(settings.smartFailover, true)
    val autoConnectEnabled = state(settings.autoConnect, false)
    val ipv6Enabled = state(settings.ipv6Enabled, false)
    val isSoundMuted = state(settings.soundMuted, false)
    val hapticsEnabled = state(settings.hapticsEnabled, true)
    val biometricEnabled = state(settings.biometricEnabled, true)
    val lockOnStart = state(settings.lockOnStart, false)
    val dnsPrimary = state(settings.dnsPrimary, "1.1.1.1")
    val dnsSecondary = state(settings.dnsSecondary, "8.8.8.8")
    val subscriptions = state(settings.subscriptions, emptySet<String>())
    val themeAccent = state(settings.themeAccent, "cyan")

    // ---- UI state ----
    private val _dashboardTab = MutableStateFlow(0)
    val dashboardTab: StateFlow<Int> = _dashboardTab.asStateFlow()

    private val _isServersModalOpen = MutableStateFlow(false)
    val isServersModalOpen: StateFlow<Boolean> = _isServersModalOpen.asStateFlow()

    private val _isSettingsModalOpen = MutableStateFlow(false)
    val isSettingsModalOpen: StateFlow<Boolean> = _isSettingsModalOpen.asStateFlow()

    private val _isLogsConsoleOpen = MutableStateFlow(false)
    val isLogsConsoleOpen: StateFlow<Boolean> = _isLogsConsoleOpen.asStateFlow()

    private val _isSplitTunnelingOpen = MutableStateFlow(false)
    val isSplitTunnelingOpen: StateFlow<Boolean> = _isSplitTunnelingOpen.asStateFlow()

    private val _isImportOpen = MutableStateFlow(false)
    val isImportOpen: StateFlow<Boolean> = _isImportOpen.asStateFlow()

    private val _selectedServerForQr = MutableStateFlow<VpnServer?>(null)
    val selectedServerForQr: StateFlow<VpnServer?> = _selectedServerForQr.asStateFlow()

    private val _smartImportFallbackOpen = MutableStateFlow(false)
    val smartImportFallbackOpen: StateFlow<Boolean> = _smartImportFallbackOpen.asStateFlow()

    private val _isUpdatingGitHub = MutableStateFlow(false)
    val isUpdatingGitHub: StateFlow<Boolean> = _isUpdatingGitHub.asStateFlow()

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing: StateFlow<Boolean> = _isTestingPing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Live throughput test state, null when no test is running. */
    private val _speedTest = MutableStateFlow<SpeedTestState?>(null)
    val speedTest: StateFlow<SpeedTestState?> = _speedTest.asStateFlow()

    /** Raised when connect is pressed with an empty server list. */
    private val _needsServers = MutableStateFlow(false)
    val needsServers: StateFlow<Boolean> = _needsServers.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** Set when the user asked to connect but the OS still needs to grant VPN consent. */
    private val _pendingConnect = MutableStateFlow(false)
    val pendingConnect: StateFlow<Boolean> = _pendingConnect.asStateFlow()

    init {
        viewModelScope.launch {
            repository.restore()
            if (settings.lockOnStart.first()) securityManager.lock()
            // Deliberately NOT refreshing here. A multi-stage subscription fetch
            // on first launch spends the user's data before they have asked for
            // anything and makes the app look stuck. Connecting without servers
            // now points them at the server screen instead.
        }
        watchForDrops()

        // Mirror the persisted mute preference into the sound engine.
        viewModelScope.launch {
            settings.soundMuted.collect { SoundEngine.muted = it }
        }
    }

    // region connection

    fun requestToggle(context: Context, onNeedsPermission: () -> Unit) {
        val state = connectionState.value
        if (state == VpnConnectionState.CONNECTED || state.isBusy) {
            stopVpn(context)
            return
        }

        // With no nodes at all there is nothing to dial, so send the user to the
        // server screen rather than failing with a vague error.
        if (!hasAnyServer()) {
            _needsServers.value = true
            return
        }
        val consent = android.net.VpnService.prepare(context)
        if (consent != null) {
            _pendingConnect.value = true
            onNeedsPermission()
        } else {
            startVpn(context)
        }
    }

    fun onPermissionResult(context: Context, granted: Boolean) {
        val wasPending = _pendingConnect.value
        _pendingConnect.value = false
        if (granted && wasPending) startVpn(context)
        else if (!granted) _toast.value = "بدون مجوز VPN امکان برقراری تونل وجود ندارد"
    }

    fun startVpn(context: Context) {
        attach(context)
        viewModelScope.launch {
            val server = activeServer.value
            val bypass = ArrayList(bypassApps.value.filter { it.isBypassed }.map { it.packageName })
            val intent = Intent(context, MeelanoVpnService::class.java).apply {
                action = MeelanoVpnService.ACTION_CONNECT
                putExtra(MeelanoVpnService.EXTRA_SERVER_NAME, server.name)
                putExtra(MeelanoVpnService.EXTRA_PROTOCOL, server.protocol)
                putExtra(MeelanoVpnService.EXTRA_CONFIG_LINK, server.configLink)
                putStringArrayListExtra(MeelanoVpnService.EXTRA_BYPASS_PACKAGES, bypass)
                putExtra(MeelanoVpnService.EXTRA_KILL_SWITCH, killSwitchEnabled.value)
                putExtra(MeelanoVpnService.EXTRA_WHITE_LABEL, server.isVip)
                putExtra(MeelanoVpnService.EXTRA_ROUTING_MODE, routingMode.value.name)
                putExtra(MeelanoVpnService.EXTRA_DNS_PRIMARY, dnsPrimary.value)
                putExtra(MeelanoVpnService.EXTRA_DNS_SECONDARY, dnsSecondary.value)
                putExtra(MeelanoVpnService.EXTRA_IPV6, ipv6Enabled.value)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopVpn(context: Context) {
        context.startService(
            Intent(context, MeelanoVpnService::class.java).apply {
                action = MeelanoVpnService.ACTION_DISCONNECT
            }
        )
    }

    /**
     * Smart failover: when a tunnel dies, walk the server list by latency and
     * actually *re-dial* until one of them carries traffic. This is what lets the
     * app stay online when individual nodes are blocked or overloaded.
     */
    private fun watchForDrops() {
        viewModelScope.launch {
            connectionState.collect { state ->
                // Watchdog: never let the UI sit in a transient state forever.
                // The service now caps its own handshake, but a wedged socket or
                // a missed callback must not leave the user staring at "در حال
                // اتصال" with no way forward.
                // Feed every outcome back so the ranking improves with use.
                when (state) {
                    VpnConnectionState.CONNECTED ->
                        advisorRef?.record(
                            nodeKey = activeServer.value.id,
                            success = true,
                            latencyMs = activeServer.value.pingMs
                        )

                    VpnConnectionState.FAILED ->
                        advisorRef?.record(nodeKey = activeServer.value.id, success = false)

                    VpnConnectionState.DISCONNECTED -> if (connectedSince > 0L) {
                        // Credit the node with how long it actually held.
                        advisorRef?.record(
                            nodeKey = activeServer.value.id,
                            success = true,
                            latencyMs = activeServer.value.pingMs,
                            holdSeconds = ((System.currentTimeMillis() - connectedSince) / 1000).toInt()
                        )
                        connectedSince = 0L
                    }

                    else -> Unit
                }

                when (state) {
                    VpnConnectionState.CONNECTED -> SoundEngine.play(SoundEngine.Cue.CONNECT)
                    VpnConnectionState.DISCONNECTED -> SoundEngine.play(SoundEngine.Cue.DISCONNECT)
                    VpnConnectionState.FAILED -> SoundEngine.play(SoundEngine.Cue.ERROR)
                    else -> Unit
                }

                if (state == VpnConnectionState.CONNECTING ||
                    state == VpnConnectionState.RECONNECTING
                ) {
                    val stamp = ++connectAttempt
                    viewModelScope.launch {
                        delay(CONNECT_WATCHDOG_MS)
                        val stillStuck = connectAttempt == stamp &&
                            (connectionState.value == VpnConnectionState.CONNECTING ||
                                connectionState.value == VpnConnectionState.RECONNECTING)
                        if (stillStuck) {
                            MeelanoVpnService.log(
                                "Watchdog: still connecting after " +
                                    "${CONNECT_WATCHDOG_MS / 1000}s — aborting"
                            )
                            _toast.value = "اتصال بیش از حد طول کشید؛ سرور دیگری را امتحان کنید"
                            appContext?.let { stopVpn(it) }
                        }
                    }
                }

                // Only forget the blacklist once a tunnel has held long enough to
                // be real. Clearing it the instant CONNECTED appears let the
                // cascade re-dial the very same dead node seconds later, which is
                // how failover ended up looping over one server forever.
                if (state == VpnConnectionState.CONNECTED) {
                    connectedSince = System.currentTimeMillis()
                    viewModelScope.launch {
                        delay(STABLE_CONNECTION_MS)
                        if (connectionState.value == VpnConnectionState.CONNECTED) {
                            triedServerIds.clear()
                        }
                    }
                }
                if (state == VpnConnectionState.FAILED && smartFailoverEnabled.value && !isFailingOver) {
                    failover()
                }
            }
        }
    }

    /**
     * Measures real throughput through the active server.
     *
     * Runs the transfer over the node's own protocol tunnel, so the number
     * reflects what the user would actually get rather than a guess derived
     * from latency.
     */
    fun runSpeedTest() {
        if (_speedTest.value?.running == true) return
        val server = activeServer.value
        val endpoint = ConfigParser.parse(server.configLink)
        if (endpoint == null) {
            _toast.value = "کانفیگ این سرور قابل تجزیه نیست"
            return
        }

        viewModelScope.launch {
            _speedTest.value = SpeedTestState(running = true, serverName = server.name)
            val report = SpeedTester.measure(endpoint) { bytes ->
                _speedTest.value = _speedTest.value?.copy(bytesTransferred = bytes)
            }
            _speedTest.value = SpeedTestState(
                running = false,
                serverName = server.name,
                mbps = report.mbps,
                bytesTransferred = report.bytesTransferred,
                error = report.error
            )
            MeelanoVpnService.log(
                if (report.success) {
                    "Speed test · ${server.name} · %.2f Mbps".format(report.mbps)
                } else {
                    "Speed test failed · ${report.error}"
                }
            )
        }
    }

    fun dismissNeedsServers() {
        _needsServers.value = false
    }

    /** True when any list holds at least one node. */
    fun hasAnyServer(): Boolean =
        vipServers.value.isNotEmpty() ||
            freeServers.value.isNotEmpty() ||
            customServers.value.isNotEmpty()

    fun dismissSpeedTest() {
        _speedTest.value = null
    }

    data class SpeedTestState(
        val running: Boolean,
        val serverName: String,
        val mbps: Double = 0.0,
        val bytesTransferred: Long = 0,
        val error: String? = null
    )

    /** Guards against re-entrancy while a cascade is already running. */
    private var isFailingOver = false
    private val triedServerIds = mutableSetOf<String>()
    private var connectedSince = 0L

    /** Bumped on every connect attempt so a stale watchdog cannot fire. */
    private var connectAttempt = 0L

    private suspend fun failover() {
        val context = appContext ?: return

        // Some faults repeat on every node — no VPN permission, no internet.
        // Cycling the whole list for those wastes time and battery and buries
        // the real message under a stream of retry toasts.
        val diagnosis = MeelanoVpnService.lastDiagnosis.value
        if (diagnosis != null && !diagnosis.tryAnotherServer) {
            MeelanoVpnService.log("Failover skipped: ${diagnosis.summary}")
            _toast.value = diagnosis.advice
            return
        }

        isFailingOver = true
        try {
            triedServerIds.add(activeServer.value.id)
            MeelanoVpnService.log("Smart Failover: searching for a healthy node…")

            // Order by learned reliability rather than latency alone. Raw ping
            // is a poor predictor here: a blocked node whose port still answers
            // looks like the fastest candidate, which is precisely how failover
            // used to keep picking dead servers.
            val reachable = repository.allServers()
                .filterNot { it.id in triedServerIds }
                .filter { it.pingMs > 0 && it.pingMs < com.example.core.PingTester.UNREACHABLE }

            val learner = advisorRef
            val ranked = if (learner != null) {
                val ordered = learner.rank(reachable.map { it.id })
                val byId = reachable.associateBy { it.id }
                ordered.mapNotNull { byId[it] }
            } else {
                reachable.sortedBy { it.pingMs }
            }

            val candidates = ranked.ifEmpty {
                repository.allServers().filterNot { it.id in triedServerIds }
            }

            if (candidates.isEmpty()) {
                // Every node has been tried and none held. Stop rather than
                // restarting the cascade — an endless retry loop drains the
                // battery and tells the user nothing.
                triedServerIds.clear()
                _toast.value = "هیچ سرور سالمی پیدا نشد؛ لطفاً لیست را به‌روزرسانی کنید"
                MeelanoVpnService.log("Smart Failover: exhausted every known node — stopping")
                return
            }

            val next = candidates.first()
            triedServerIds.add(next.id)
            repository.selectServer(next)
            _toast.value = "تلاش مجدد با ${next.name} (Failover)"
            MeelanoVpnService.log("Smart Failover → ${next.name} (${next.pingMs}ms)")
            delay(700)
            startVpn(context)
        } finally {
            isFailingOver = false
        }
    }

    /**
     * "Best effort" connect: pings everything, then tries nodes in order of
     * latency until one genuinely carries traffic.
     */
    fun connectWithBestEffort(context: Context, onNeedsPermission: () -> Unit) {
        viewModelScope.launch {
            triedServerIds.clear()
            _isTestingPing.value = true
            _toast.value = "در حال یافتن بهترین مسیر…"
            ServerRepository.ServerScope.entries.forEach { repository.testPings(it) }
            val fastest = repository.fastestServer { done, total ->
                repository.reportProgress("اعتبارسنجی سرورها…", done, total)
            }
            _isTestingPing.value = false
            repository.clearProgress()
            if (fastest == null) {
                _toast.value = "هیچ سرور سالمی پیدا نشد؛ فهرست را به‌روزرسانی کنید"
                return@launch
            }
            repository.selectServer(fastest)
            _toast.value = "اتصال به ${fastest.name} · ${fastest.pingMs}ms"
            requestToggle(context, onNeedsPermission)
        }
    }

    fun connectToFastest(context: Context, onNeedsPermission: () -> Unit) {
        viewModelScope.launch {
            _isTestingPing.value = true
            val fastest = repository.fastestServer { done, total ->
                repository.reportProgress("اعتبارسنجی سرورها…", done, total)
            }
            _isTestingPing.value = false
            repository.clearProgress()
            if (fastest == null) {
                _toast.value = "هیچ سرور سالمی پیدا نشد؛ فهرست را به‌روزرسانی کنید"
                return@launch
            }
            repository.selectServer(fastest)
            _toast.value = "سریع‌ترین سرور: ${fastest.name} · ${fastest.pingMs}ms"
            requestToggle(context, onNeedsPermission)
        }
    }

    // endregion

    // region servers

    fun selectServer(server: VpnServer, context: Context) {
        viewModelScope.launch {
            repository.selectServer(server)
            MeelanoVpnService.log("Selected ${server.name} · ${server.hostLabel}")
            if (connectionState.value == VpnConnectionState.CONNECTED) startVpn(context)
        }
    }

    fun toggleFavorite(server: VpnServer) {
        viewModelScope.launch { repository.toggleFavorite(server) }
    }

    fun deleteCustomServer(server: VpnServer) = deleteServer(server)

    /** Deletes any server — VIP, free or imported — permanently. */
    fun deleteServer(server: VpnServer) {
        viewModelScope.launch {
            repository.deleteServer(server)
            _toast.value = "«${server.name}» حذف شد"
        }
    }

    /** Sweeps every node that failed its last reachability test. */
    fun deleteUnreachableServers() {
        viewModelScope.launch {
            val removed = repository.deleteUnreachable()
            _toast.value = if (removed > 0) "$removed سرور خراب حذف شد" else "سرور خرابی پیدا نشد"
        }
    }

    /** Brings back the built-in servers the user previously deleted. */
    fun restoreDeletedServers() {
        viewModelScope.launch {
            repository.restoreDeleted()
            _toast.value = "سرورهای پیش‌فرض بازگردانده شد"
        }
    }

    fun testPings(scope: ServerRepository.ServerScope) {
        viewModelScope.launch {
            _isTestingPing.value = true
            MeelanoVpnService.log("Ping test started (${scope.name})")
            repository.testPings(scope)
            _isTestingPing.value = false
            MeelanoVpnService.log("Ping test finished")
        }
    }

    fun setSort(sort: ServerSort) {
        viewModelScope.launch { settings.setServerSort(sort) }
    }

    fun sortedServers(list: List<VpnServer>): List<VpnServer> {
        val filtered = list
            .filter { server ->
                val query = _searchQuery.value.trim()
                query.isEmpty() ||
                    server.name.contains(query, true) ||
                    server.countryName.contains(query, true) ||
                    server.protocol.contains(query, true)
            }
            .filter { server ->
                coreProtocolFilter.value == CoreProtocolFilter.ALL ||
                    server.protocol.equals(coreProtocolFilter.value.label, ignoreCase = true)
            }
        return repository.sorted(filtered, serverSort.value)
    }

    fun refreshSubscriptions(silent: Boolean = false) {
        if (!silent) SoundEngine.play(SoundEngine.Cue.SCAN)
        viewModelScope.launch {
            _isUpdatingGitHub.value = true
            if (!silent) MeelanoVpnService.log("Fetching subscriptions…")
            repository.refreshFreeServers()
                .onSuccess { list ->
                    MeelanoVpnService.log("Subscriptions updated: ${list.size} live nodes")
                    if (!silent) {
                        SoundEngine.play(SoundEngine.Cue.SUCCESS)
                        _toast.value = "${list.size} سرور فعال دریافت شد"
                    }
                }
                .onFailure { error ->
                    MeelanoVpnService.log("Subscription update failed: ${error.message}")
                    if (!silent) {
                        SoundEngine.play(SoundEngine.Cue.ERROR)
                        _toast.value = error.message ?: "به‌روزرسانی ناموفق بود"
                    }
                }
            _isUpdatingGitHub.value = false
        }
    }

    fun importConfigs(payload: String) {
        viewModelScope.launch {
            val count = repository.importConfigs(payload)
            _toast.value = if (count > 0) "$count کانفیگ وارد شد" else "هیچ کانفیگ معتبری پیدا نشد"
            if (count > 0) _isImportOpen.value = false
        }
    }

    fun importFromClipboard(context: Context) = importConfigs(SmartImportHelper.readClipboard(context))

    fun addSubscription(url: String) {
        viewModelScope.launch {
            if (repository.addSubscription(url.trim())) {
                _toast.value = "لینک اشتراک اضافه شد"
                refreshSubscriptions()
            } else {
                _toast.value = "آدرس اشتراک نامعتبر است"
            }
        }
    }

    fun removeSubscription(url: String) {
        viewModelScope.launch { repository.removeSubscription(url) }
    }

    // endregion

    // region settings mutations

    fun setRoutingMode(mode: RoutingMode) = viewModelScope.launch {
        settings.setRoutingMode(mode)
        MeelanoVpnService.log("Routing mode → ${mode.title}")
    }

    fun setCoreProtocolFilter(filter: CoreProtocolFilter) =
        viewModelScope.launch { settings.setProtocolFilter(filter) }

    fun toggleKillSwitch() = viewModelScope.launch {
        settings.setKillSwitch(!killSwitchEnabled.value)
        MeelanoVpnService.log("Kill Switch → ${!killSwitchEnabled.value}")
    }

    fun toggleSmartFailover() = viewModelScope.launch {
        settings.setSmartFailover(!smartFailoverEnabled.value)
    }

    fun toggleAutoConnect() = viewModelScope.launch { settings.setAutoConnect(!autoConnectEnabled.value) }
    fun toggleIpv6() = viewModelScope.launch { settings.setIpv6(!ipv6Enabled.value) }
    fun toggleSoundMute() = viewModelScope.launch { settings.setSoundMuted(!isSoundMuted.value) }
    fun toggleHaptics() = viewModelScope.launch { settings.setHaptics(!hapticsEnabled.value) }
    fun toggleBiometric() = viewModelScope.launch { settings.setBiometric(!biometricEnabled.value) }
    fun toggleLockOnStart() = viewModelScope.launch { settings.setLockOnStart(!lockOnStart.value) }
    fun setThemeAccent(key: String) = viewModelScope.launch { settings.setThemeAccent(key) }
    fun setDns(primary: String, secondary: String) = viewModelScope.launch {
        settings.setDnsPrimary(primary)
        settings.setDnsSecondary(secondary)
        MeelanoVpnService.log("DNS → $primary, $secondary")
    }

    fun toggleBypassApp(packageName: String) =
        viewModelScope.launch { repository.toggleBypassApp(packageName) }

    fun reloadInstalledApps() = viewModelScope.launch { repository.loadInstalledApps() }

    // endregion

    // region simple UI toggles

    fun setDashboardTab(index: Int) { _dashboardTab.value = index }
    fun openServersModal() { _isServersModalOpen.value = true }
    fun closeServersModal() { _isServersModalOpen.value = false }
    fun openSettingsModal() { _isSettingsModalOpen.value = true }
    fun closeSettingsModal() { _isSettingsModalOpen.value = false }
    fun openLogsConsole() { _isLogsConsoleOpen.value = true }
    fun closeLogsConsole() { _isLogsConsoleOpen.value = false }
    fun openSplitTunneling() { _isSplitTunnelingOpen.value = true; reloadInstalledApps() }
    fun closeSplitTunneling() { _isSplitTunnelingOpen.value = false }
    fun openImport() { _isImportOpen.value = true }
    fun closeImport() { _isImportOpen.value = false }
    fun showQrCode(server: VpnServer) { _selectedServerForQr.value = server }
    fun closeQrCode() { _selectedServerForQr.value = null }
    fun closeSmartImportFallback() { _smartImportFallbackOpen.value = false }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun clearLogs() = MeelanoVpnService.clearLogs()
    fun consumeToast() { _toast.value = null }

    fun triggerSmartImport(context: Context, configLink: String) {
        if (!SmartImportHelper.openInDestinationApp(context, configLink)) {
            _smartImportFallbackOpen.value = true
        }
    }

    // endregion

    private fun <T> state(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    class Factory(
        private val repository: ServerRepository,
        private val settings: SettingsStore,
        private val securityManager: SecurityManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, settings, securityManager) as T
    }

    private companion object {
        /** How long a tunnel must hold before we trust it and reset failover. */
        const val STABLE_CONNECTION_MS = 15_000L

        /** Upper bound on any single connect attempt before the UI gives up. */
        const val CONNECT_WATCHDOG_MS = 25_000L
    }
}
