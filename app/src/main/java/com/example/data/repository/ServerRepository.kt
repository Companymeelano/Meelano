package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.core.ConfigParser
import com.example.core.NodeValidator
import com.example.core.PingTester
import com.example.vpn.proto.OutboundFactory
import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import com.example.data.model.BypassApp
import com.example.data.model.ServerSort
import com.example.data.model.VpnServer
import com.example.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Owns every server list in the app.
 *
 * Free nodes come from real GitHub subscription URLs, are parsed with
 * [ConfigParser], TCP-pinged with [PingTester], deduplicated, geo-labelled and
 * only the fastest *reachable* ones survive. Custom nodes the user imports and
 * all favourites/selection state are persisted through [SettingsStore].
 */
class ServerRepository(
    private val context: Context,
    private val settings: SettingsStore
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _vipServers = MutableStateFlow(BundledServers.vip)
    val vipServers: StateFlow<List<VpnServer>> = _vipServers.asStateFlow()

    private val _freeServers = MutableStateFlow<List<VpnServer>>(emptyList())
    val freeServers: StateFlow<List<VpnServer>> = _freeServers.asStateFlow()

    private val _customServers = MutableStateFlow<List<VpnServer>>(emptyList())
    val customServers: StateFlow<List<VpnServer>> = _customServers.asStateFlow()

    private val _activeServer = MutableStateFlow(BundledServers.vip.first())
    val activeServer: StateFlow<VpnServer> = _activeServer.asStateFlow()

    private val _bypassApps = MutableStateFlow<List<BypassApp>>(emptyList())
    val bypassApps: StateFlow<List<BypassApp>> = _bypassApps.asStateFlow()

    /** Server ids the user deleted; filtered out of every list and never re-added. */
    private var hidden: MutableSet<String> = mutableSetOf()

    private val _updateProgress = MutableStateFlow<UpdateProgress?>(null)
    val updateProgress: StateFlow<UpdateProgress?> = _updateProgress.asStateFlow()

    /** No more than this many surviving nodes may share one host address. */
    private val MAX_NODES_PER_HOST = 3

    data class UpdateProgress(val stage: String, val done: Int, val total: Int) {
        val fraction: Float get() = if (total <= 0) 0f else done.toFloat() / total
    }

    // region bootstrap / persistence

    suspend fun restore() {
        val favorites = settings.favorites.first()
        hidden = settings.hiddenServers.first().toMutableSet()
        _customServers.value = decodeServers(settings.customServers.first(), isVip = false)
            .filterNot { it.id in hidden }
            .map { it.copy(isFavorite = it.id in favorites) }
        _freeServers.value = decodeServers(settings.cachedFreeServers.first(), isVip = false)
            .filterNot { it.id in hidden }
            .map { it.copy(isFavorite = it.id in favorites) }
        _vipServers.value = _vipServers.value
            .filterNot { it.id in hidden }
            .map { it.copy(isFavorite = it.id in favorites) }

        val selectedId = settings.selectedServerId.first()
        allServers().firstOrNull { it.id == selectedId }?.let { selectServerInternal(it) }
        loadInstalledApps()
    }

    fun allServers(): List<VpnServer> = _vipServers.value + _customServers.value + _freeServers.value

    suspend fun selectServer(server: VpnServer) {
        selectServerInternal(server)
        settings.setSelectedServerId(server.id)
    }

    private fun selectServerInternal(server: VpnServer) {
        _vipServers.value = _vipServers.value.map { it.copy(isSelected = it.id == server.id) }
        _freeServers.value = _freeServers.value.map { it.copy(isSelected = it.id == server.id) }
        _customServers.value = _customServers.value.map { it.copy(isSelected = it.id == server.id) }
        _activeServer.value = server.copy(isSelected = true)
    }

    suspend fun toggleFavorite(server: VpnServer) {
        val current = settings.favorites.first().toMutableSet()
        if (!current.add(server.id)) current.remove(server.id)
        settings.setFavorites(current)
        val mark: (VpnServer) -> VpnServer = { it.copy(isFavorite = it.id in current) }
        _vipServers.value = _vipServers.value.map(mark)
        _freeServers.value = _freeServers.value.map(mark)
        _customServers.value = _customServers.value.map(mark)
        _activeServer.value = mark(_activeServer.value)
    }

    // endregion

    // region ping

    /** Real TCP ping over every server in the given tab; dead nodes are marked. */
    suspend fun testPings(scope: ServerScope) {
        val list = listFor(scope)
        if (list.isEmpty()) return
        _updateProgress.value = UpdateProgress("در حال تست پینگ…", 0, list.size)

        val results = PingTester.pingAll(
            items = list,
            keyOf = { it.id },
            addressOf = { server -> server.endpoint?.let { it.host to it.port } }
        )
        val now = System.currentTimeMillis()
        val updated = list.map { server ->
            val latency = results[server.id] ?: PingTester.UNREACHABLE
            server.copy(
                pingMs = latency,
                lastTestedAt = now,
                speedMbps = estimateThroughput(latency)
            )
        }
        setList(scope, updated)
        updated.firstOrNull { it.id == _activeServer.value.id }?.let { _activeServer.value = it }
        _updateProgress.value = null
    }

    /** Returns the fastest reachable server across all lists, or null. */
    suspend fun fastestServer(): VpnServer? {
        val candidates = allServers()
        if (candidates.isEmpty()) return null
        val results = PingTester.pingAll(
            items = candidates,
            keyOf = { it.id },
            addressOf = { s -> s.endpoint?.let { it.host to it.port } }
        )
        return candidates
            .map { it.copy(pingMs = results[it.id] ?: PingTester.UNREACHABLE) }
            .filter { it.isReachable }
            .minByOrNull { it.pingMs }
    }

    private fun estimateThroughput(latencyMs: Int): Float = when {
        latencyMs <= 0 -> 0f
        latencyMs < 60 -> 240f - latencyMs
        latencyMs < 120 -> 180f - latencyMs
        latencyMs < 250 -> 120f - latencyMs / 2f
        else -> (60f - latencyMs / 10f).coerceAtLeast(1f)
    }

    // endregion

    // region subscriptions

    /**
     * Downloads every configured subscription, parses all links, pings them in
     * parallel and keeps the [keep] fastest live nodes. Returns a failure only
     * when nothing at all could be fetched or resolved.
     */
    suspend fun refreshFreeServers(keep: Int = 20): Result<List<VpnServer>> = withContext(Dispatchers.IO) {
        try {
            val sources = settings.subscriptions.first().toList()
            _updateProgress.value = UpdateProgress("دریافت اشتراک‌ها…", 0, sources.size)

            val endpoints = LinkedHashMap<String, ProxyEndpoint>()
            var reachedAnySource = false

            // Fetch sources in parallel — serially this took minutes when several
            // mirrors were blocked and each had to time out in turn.
            coroutineScope {
                sources.mapIndexed { index, url ->
                    async(Dispatchers.IO) {
                        val body = fetchWithMirrors(url)
                        synchronized(endpoints) {
                            if (body != null) {
                                reachedAnySource = true
                                ConfigParser.parseSubscription(body).forEach { endpoint ->
                                    endpoints.putIfAbsent(
                                        "${endpoint.host}:${endpoint.port}",
                                        endpoint
                                    )
                                }
                            }
                            _updateProgress.value =
                                UpdateProgress("دریافت اشتراک‌ها…", index + 1, sources.size)
                        }
                    }
                }.awaitAll()
            }

            if (endpoints.isEmpty()) {
                _updateProgress.value = null
                return@withContext Result.failure(
                    IllegalStateException(
                        if (reachedAnySource) {
                            "اشتراک‌ها خالی بودند؛ بعداً دوباره تلاش کنید"
                        } else {
                            "دسترسی به منابع برقرار نشد؛ اینترنت یا فیلترشکن را بررسی کنید"
                        }
                    )
                )
            }

            // Stage 1 — structural filter. Drop anything this build cannot carry
            // and anything the user deleted, before spending time on the network.
            val eligible = endpoints.values
                .filter { OutboundFactory.supports(it) }
                .filterNot { "free_${it.host}_${it.port}" in hidden }
                .toList()

            // Stage 2 — cheap TCP reachability, to shrink the field fast.
            _updateProgress.value = UpdateProgress("تست دسترسی ${eligible.size} نود…", 0, eligible.size)
            val reachable = PingTester.pingAll(
                items = eligible.take(400),
                parallelism = 32,
                timeoutMs = 1500,
                keyOf = { "${it.host}:${it.port}" },
                addressOf = { it.host to it.port }
            ).let { latencies ->
                eligible.mapNotNull { endpoint ->
                    val latency = latencies["${endpoint.host}:${endpoint.port}"]
                        ?: PingTester.UNREACHABLE
                    if (latency <= 0 || latency >= PingTester.UNREACHABLE) null else endpoint to latency
                }
                    .sortedBy { it.second }
                    // Only the most responsive candidates earn a full handshake.
                    .take(keep * 5)
            }
            val reachableEndpoints = reachable.map { it.first }

            if (reachable.isEmpty()) {
                _updateProgress.value = null
                return@withContext Result.failure(
                    IllegalStateException("هیچ نودی در دسترس نبود؛ اتصال اینترنت را بررسی کنید")
                )
            }

            // Stage 3 — the strict test: a real protocol handshake plus a live
            // HTTP request proxied through the node. Only nodes that genuinely
            // relayed traffic survive this, which is what makes the list usable.
            _updateProgress.value = UpdateProgress("اعتبارسنجی واقعی ${reachableEndpoints.size} نود…", 0, reachable.size)
            val validated = NodeValidator.validateAll(
                endpoints = reachableEndpoints,
                parallelism = 12,
                probeTimeoutMs = 6_000,
                onProgress = { done, total ->
                    _updateProgress.value = UpdateProgress("اعتبارسنجی واقعی…", done, total)
                }
            )

            // Strict validation is the goal, but it must never leave the user with
            // an empty list. If the deep probe cleared nothing — which happens when
            // the phone is behind a captive portal or heavy DPI — fall back to the
            // nodes that at least answered TCP, clearly marked as unverified.
            val verifiedFirst: List<Pair<ProxyEndpoint, Int>> = if (validated.isNotEmpty()) {
                validated.map { it.endpoint to it.latencyMs }
            } else {
                reachable
            }
            val strictlyVerified = validated.isNotEmpty()

            // Stage 4 — diversity: cap how many nodes come from one host so the
            // list is not twelve entries pointing at the same overloaded server.
            val perHost = HashMap<String, Int>()
            val alive = verifiedFirst
                .filter { (endpoint, _) ->
                    val count = perHost.getOrDefault(endpoint.host, 0)
                    if (count >= MAX_NODES_PER_HOST) false else {
                        perHost[endpoint.host] = count + 1
                        true
                    }
                }
                .take(keep)

            val favorites = settings.favorites.first()
            val now = System.currentTimeMillis()
            // Number nodes per country so names read "MeeLano آلمان ۲", matching
            // the VIP list instead of an opaque running index.
            val perCountry = HashMap<String, Int>()
            val servers = alive.map { (endpoint, latency) ->
                val geo = GeoLabeler.of(endpoint.host, endpoint.remark)
                val ordinal = perCountry.getOrDefault(geo.country, 0) + 1
                perCountry[geo.country] = ordinal
                val id = "free_${endpoint.host}_${endpoint.port}"
                VpnServer(
                    id = id,
                    name = if (strictlyVerified) {
                        "MeeLano ${geo.country} $ordinal"
                    } else {
                        "MeeLano ${geo.country} $ordinal ○"
                    },
                    countryName = geo.country,
                    flagEmoji = geo.flag,
                    protocol = endpoint.displayProtocol,
                    isVip = false,
                    pingMs = latency,
                    speedMbps = estimateThroughput(latency),
                    configLink = endpoint.raw,
                    isFavorite = id in favorites,
                    lastTestedAt = now
                )
            }

            _freeServers.value = servers
            settings.setCachedFreeServers(encodeServers(servers))
            _updateProgress.value = null
            Result.success(servers)
        } catch (e: Exception) {
            _updateProgress.value = null
            Result.failure(e)
        }
    }

    /**
     * Fetches a subscription, retrying through alternative CDN mirrors.
     *
     * raw.githubusercontent.com and jsDelivr are blocked on many Iranian
     * networks — and blocked differently from one ISP to the next — so a single
     * URL failing says nothing about the others. Rewrites the same underlying
     * repository path across every mirror we know and returns the first body
     * that actually arrives.
     */
    private suspend fun fetchWithMirrors(url: String): String? = withContext(Dispatchers.IO) {
        for (candidate in mirrorsFor(url)) {
            val body = runCatching {
                httpClient.newCall(
                    Request.Builder()
                        .url(candidate)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                        .build()
                ).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull()

            if (!body.isNullOrBlank() && body.contains("://")) return@withContext body
        }
        null
    }

    /** Expands a subscription URL into every equivalent mirror we can try. */
    private fun mirrorsFor(url: String): List<String> {
        // Recognise the two shapes our defaults use and recover owner/ref/path.
        val jsdelivr = Regex("""https://cdn\.jsdelivr\.net/gh/([^@/]+)/([^@/]+)@([^/]+)/(.+)""")
            .find(url)
        val raw = Regex("""https://raw\.githubusercontent\.com/([^/]+)/([^/]+)/([^/]+)/(.+)""")
            .find(url)
        val parts = jsdelivr?.destructured ?: raw?.destructured ?: return listOf(url)
        val (owner, repo, ref, path) = parts

        return listOf(
            url,
            "https://cdn.jsdelivr.net/gh/$owner/$repo@$ref/$path",
            "https://fastly.jsdelivr.net/gh/$owner/$repo@$ref/$path",
            "https://gcore.jsdelivr.net/gh/$owner/$repo@$ref/$path",
            "https://raw.githubusercontent.com/$owner/$repo/$ref/$path",
            "https://ghproxy.net/https://raw.githubusercontent.com/$owner/$repo/$ref/$path",
            "https://raw.gitmirror.com/$owner/$repo/$ref/$path"
        ).distinct()
    }

    suspend fun addSubscription(url: String): Boolean {
        if (!url.startsWith("http")) return false
        settings.setSubscriptions(settings.subscriptions.first() + url)
        return true
    }

    suspend fun removeSubscription(url: String) {
        settings.setSubscriptions(settings.subscriptions.first() - url)
    }

    // endregion

    // region manual import

    /** Imports one or many config links (or a whole subscription blob) pasted by the user. */
    suspend fun importConfigs(payload: String): Int {
        val endpoints = ConfigParser.parseSubscription(payload).ifEmpty {
            listOfNotNull(ConfigParser.parse(payload))
        }
        if (endpoints.isEmpty()) return 0

        val existing = _customServers.value.associateBy { it.id }.toMutableMap()
        endpoints.forEach { endpoint ->
            val geo = GeoLabeler.of(endpoint.host, endpoint.remark)
            val id = "custom_${endpoint.host}_${endpoint.port}"
            existing[id] = VpnServer(
                id = id,
                name = endpoint.remark.ifBlank { "${geo.country} · ${endpoint.displayProtocol}" },
                countryName = geo.country,
                flagEmoji = geo.flag,
                protocol = endpoint.displayProtocol,
                isVip = false,
                pingMs = 0,
                speedMbps = 0f,
                configLink = endpoint.raw
            )
        }
        val merged = existing.values.toList()
        _customServers.value = merged
        settings.setCustomServers(encodeServers(merged))
        return endpoints.size
    }

    suspend fun deleteCustomServer(server: VpnServer) = deleteServer(server)

    /**
     * Removes a server from whichever list it lives in — VIP, free or imported —
     * and remembers the deletion so a later refresh does not resurrect it.
     */
    suspend fun deleteServer(server: VpnServer) {
        hidden.add(server.id)
        settings.setHiddenServers(hidden)

        _customServers.value = _customServers.value.filterNot { it.id == server.id }
        _freeServers.value = _freeServers.value.filterNot { it.id == server.id }
        _vipServers.value = _vipServers.value.filterNot { it.id == server.id }

        settings.setCustomServers(encodeServers(_customServers.value))
        settings.setCachedFreeServers(encodeServers(_freeServers.value))

        // If the deleted node was selected, fall back to any remaining server.
        if (_activeServer.value.id == server.id) {
            allServers().firstOrNull()?.let { selectServer(it) }
        }
    }

    /** Deletes every server that failed its last reachability test. */
    suspend fun deleteUnreachable(): Int {
        val dead = allServers().filter { it.pingMs >= PingTester.UNREACHABLE }
        dead.forEach { deleteServer(it) }
        return dead.size
    }

    /** Restores every previously deleted bundled/free server. */
    suspend fun restoreDeleted() {
        hidden.clear()
        settings.setHiddenServers(emptySet())
        _vipServers.value = BundledServers.vip
    }

    // endregion

    // region split tunnelling

    /** Reads the real launchable app list off the device for the split-tunnel screen. */
    suspend fun loadInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val bypassed = settings.bypassPackages.first()
        val apps = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    BypassApp(
                        packageName = info.packageName,
                        appName = pm.getApplicationLabel(info).toString(),
                        category = KnownApps.categoryOf(info.packageName, isSystem),
                        isBypassed = info.packageName in bypassed,
                        isSystemApp = isSystem
                    )
                }
                .sortedWith(compareByDescending<BypassApp> { it.isBypassed }.thenBy { it.appName })
        }.getOrDefault(emptyList())

        _bypassApps.value = if (apps.isEmpty()) KnownApps.fallback(bypassed) else apps
    }

    suspend fun toggleBypassApp(packageName: String) {
        val current = settings.bypassPackages.first().toMutableSet()
        if (!current.add(packageName)) current.remove(packageName)
        settings.setBypassPackages(current)
        _bypassApps.value = _bypassApps.value.map {
            if (it.packageName == packageName) it.copy(isBypassed = packageName in current) else it
        }
    }

    // endregion

    fun sorted(list: List<VpnServer>, sort: ServerSort): List<VpnServer> = when (sort) {
        ServerSort.PING -> list.sortedWith(
            compareByDescending<VpnServer> { it.isFavorite }
                .thenBy { if (it.isReachable) 0 else 1 }
                .thenBy { if (it.isReachable) it.pingMs else Int.MAX_VALUE }
        )
        ServerSort.SPEED -> list.sortedByDescending { it.speedMbps }
        ServerSort.NAME -> list.sortedBy { it.name }
        ServerSort.COUNTRY -> list.sortedBy { it.countryName }
    }

    enum class ServerScope { VIP, FREE, CUSTOM }

    private fun listFor(scope: ServerScope) = when (scope) {
        ServerScope.VIP -> _vipServers.value
        ServerScope.FREE -> _freeServers.value
        ServerScope.CUSTOM -> _customServers.value
    }

    private fun setList(scope: ServerScope, value: List<VpnServer>) {
        when (scope) {
            ServerScope.VIP -> _vipServers.value = value
            ServerScope.FREE -> _freeServers.value = value
            ServerScope.CUSTOM -> _customServers.value = value
        }
    }

    // region (de)serialisation

    private fun encodeServers(servers: List<VpnServer>): String {
        val array = JSONArray()
        servers.forEach { server ->
            array.put(
                JSONObject().apply {
                    put("id", server.id)
                    put("name", server.name)
                    put("country", server.countryName)
                    put("flag", server.flagEmoji)
                    put("protocol", server.protocol)
                    put("ping", server.pingMs)
                    put("speed", server.speedMbps.toDouble())
                    put("link", server.configLink)
                    put("testedAt", server.lastTestedAt)
                }
            )
        }
        return array.toString()
    }

    private fun decodeServers(json: String, isVip: Boolean): List<VpnServer> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                VpnServer(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    countryName = obj.optString("country"),
                    flagEmoji = obj.optString("flag", "🌐"),
                    protocol = obj.optString("protocol", Protocol.VLESS.label),
                    isVip = isVip,
                    pingMs = obj.optInt("ping", 0),
                    speedMbps = obj.optDouble("speed", 0.0).toFloat(),
                    configLink = obj.getString("link"),
                    lastTestedAt = obj.optLong("testedAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    // endregion
}
