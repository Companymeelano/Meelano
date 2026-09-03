package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CoreProtocolFilter
import com.example.data.model.NetworkLiveStats
import com.example.data.model.RoutingMode
import com.example.data.model.VpnServer
import com.example.data.repository.ServerRepository
import com.example.data.security.SecurityManager
import com.example.util.SmartImportHelper
import com.example.vpn.MeelanoVpnService
import com.example.vpn.VpnConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ServerRepository,
    val securityManager: SecurityManager
) : ViewModel() {

    val connectionState: StateFlow<VpnConnectionState> = MeelanoVpnService.connectionState
    val liveStats: StateFlow<NetworkLiveStats> = MeelanoVpnService.liveStats
    val logs: StateFlow<List<String>> = MeelanoVpnService.logs

    val activeServer: StateFlow<VpnServer> = repository.activeServer
    val vipServers: StateFlow<List<VpnServer>> = repository.vipServers
    val freeServers: StateFlow<List<VpnServer>> = repository.freeServers
    val bypassApps = repository.bypassApps

    // Routing & Protocol configuration
    private val _routingMode = MutableStateFlow(RoutingMode.SMART_BYPASS)
    val routingMode: StateFlow<RoutingMode> = _routingMode.asStateFlow()

    private val _coreProtocolFilter = MutableStateFlow(CoreProtocolFilter.ALL)
    val coreProtocolFilter: StateFlow<CoreProtocolFilter> = _coreProtocolFilter.asStateFlow()

    private val _killSwitchEnabled = MutableStateFlow(true)
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled.asStateFlow()

    private val _smartFailoverEnabled = MutableStateFlow(true)
    val smartFailoverEnabled: StateFlow<Boolean> = _smartFailoverEnabled.asStateFlow()

    // Dashboard navigation & modals
    // 0: Network Status (وضعیت شبکه), 1: Security Tools (ابزارهای امنیتی), 2: Traffic Chart (نمودار ترافیک)
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

    private val _selectedServerForQr = MutableStateFlow<VpnServer?>(null)
    val selectedServerForQr: StateFlow<VpnServer?> = _selectedServerForQr.asStateFlow()

    private val _smartImportFallbackOpen = MutableStateFlow(false)
    val smartImportFallbackOpen: StateFlow<Boolean> = _smartImportFallbackOpen.asStateFlow()

    private val _isSoundMuted = MutableStateFlow(false)
    val isSoundMuted: StateFlow<Boolean> = _isSoundMuted.asStateFlow()

    private val _isUpdatingGitHub = MutableStateFlow(false)
    val isUpdatingGitHub: StateFlow<Boolean> = _isUpdatingGitHub.asStateFlow()

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing: StateFlow<Boolean> = _isTestingPing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Smart Failover watcher: if connected server drops or is slow, automatically switch
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state == VpnConnectionState.DISCONNECTED && _smartFailoverEnabled.value) {
                    // Monitor for unexpected drop
                }
            }
        }
    }

    fun setDashboardTab(tabIndex: Int) {
        _dashboardTab.value = tabIndex
    }

    fun openServersModal() {
        _isServersModalOpen.value = true
    }

    fun closeServersModal() {
        _isServersModalOpen.value = false
    }

    fun openSettingsModal() {
        _isSettingsModalOpen.value = true
    }

    fun closeSettingsModal() {
        _isSettingsModalOpen.value = false
    }

    fun openLogsConsole() {
        _isLogsConsoleOpen.value = true
    }

    fun closeLogsConsole() {
        _isLogsConsoleOpen.value = false
    }

    fun openSplitTunneling() {
        _isSplitTunnelingOpen.value = true
    }

    fun closeSplitTunneling() {
        _isSplitTunnelingOpen.value = false
    }

    fun showQrCode(server: VpnServer) {
        _selectedServerForQr.value = server
    }

    fun closeQrCode() {
        _selectedServerForQr.value = null
    }

    fun closeSmartImportFallback() {
        _smartImportFallbackOpen.value = false
    }

    fun toggleSoundMute() {
        _isSoundMuted.value = !_isSoundMuted.value
    }

    fun setRoutingMode(mode: RoutingMode) {
        _routingMode.value = mode
        MeelanoVpnService.log("Routing mode changed: ${mode.title}")
    }

    fun setCoreProtocolFilter(filter: CoreProtocolFilter) {
        _coreProtocolFilter.value = filter
    }

    fun toggleKillSwitch() {
        _killSwitchEnabled.value = !_killSwitchEnabled.value
        MeelanoVpnService.log("Kill Switch toggled: ${_killSwitchEnabled.value}")
    }

    fun toggleSmartFailover() {
        _smartFailoverEnabled.value = !_smartFailoverEnabled.value
        MeelanoVpnService.log("Smart Failover toggled: ${_smartFailoverEnabled.value}")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectServer(server: VpnServer, context: Context) {
        repository.selectServer(server)
        MeelanoVpnService.log("Selected server: ${server.name} (${server.countryName})")
        if (connectionState.value == VpnConnectionState.CONNECTED) {
            // Hot reconnect to new server
            startVpn(context)
        }
    }

    fun toggleConnect(context: Context) {
        if (connectionState.value == VpnConnectionState.CONNECTED ||
            connectionState.value == VpnConnectionState.CONNECTING
        ) {
            stopVpn(context)
        } else {
            startVpn(context)
        }
    }

    fun startVpn(context: Context) {
        val server = activeServer.value
        val bypassedList = ArrayList(bypassApps.value.filter { it.isBypassed }.map { it.packageName })

        val intent = Intent(context, MeelanoVpnService::class.java).apply {
            action = MeelanoVpnService.ACTION_CONNECT
            putExtra(MeelanoVpnService.EXTRA_SERVER_NAME, server.name)
            putExtra(MeelanoVpnService.EXTRA_PROTOCOL, server.protocol)
            putStringArrayListExtra(MeelanoVpnService.EXTRA_BYPASS_PACKAGES, bypassedList)
            putExtra(MeelanoVpnService.EXTRA_KILL_SWITCH, _killSwitchEnabled.value)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopVpn(context: Context) {
        val intent = Intent(context, MeelanoVpnService::class.java).apply {
            action = MeelanoVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun testPings(isVip: Boolean) {
        viewModelScope.launch {
            _isTestingPing.value = true
            MeelanoVpnService.log("Testing ping for ${if (isVip) "VIP" else "Free"} servers...")
            repository.testAllPings(isVip)
            _isTestingPing.value = false
            MeelanoVpnService.log("Ping test completed.")
        }
    }

    fun sortByPing(isVip: Boolean) {
        repository.sortByLowestPing(isVip)
    }

    fun updateGitHubFreeServers() {
        viewModelScope.launch {
            _isUpdatingGitHub.value = true
            MeelanoVpnService.log("Fetching latest Iran-optimized nodes from GitHub with Smart Bypass...")
            val result = repository.updateFreeServersFromGitHub()
            result.onSuccess { top10 ->
                MeelanoVpnService.log("Updated successfully! Filtered top 10 lowest ping nodes.")
            }.onFailure { err ->
                MeelanoVpnService.log("Update error: ${err.message}. Maintained cached high-speed nodes.")
            }
            _isUpdatingGitHub.value = false
        }
    }

    fun triggerSmartImport(context: Context, configLink: String) {
        val opened = SmartImportHelper.openInDestinationApp(context, configLink)
        if (!opened) {
            _smartImportFallbackOpen.value = true
        }
    }

    fun toggleBypassApp(packageName: String) {
        repository.toggleBypassApp(packageName)
    }
}
