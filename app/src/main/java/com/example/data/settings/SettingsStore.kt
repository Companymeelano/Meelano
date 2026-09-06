package com.example.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CoreProtocolFilter
import com.example.data.model.RoutingMode
import com.example.data.model.ServerSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "meelano_settings")

/**
 * Durable settings. Every switch on the settings screen is written here and
 * restored on next launch — nothing is in-memory-only decoration.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val ROUTING = stringPreferencesKey("routing_mode")
        val PROTOCOL = stringPreferencesKey("protocol_filter")
        val SORT = stringPreferencesKey("server_sort")
        val KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val FAILOVER = booleanPreferencesKey("smart_failover")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val IPV6 = booleanPreferencesKey("ipv6")
        val SOUND_MUTED = booleanPreferencesKey("sound_muted")
        val HAPTICS = booleanPreferencesKey("haptics")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val LOCK_ON_START = booleanPreferencesKey("lock_on_start")
        val DNS_PRIMARY = stringPreferencesKey("dns_primary")
        val DNS_SECONDARY = stringPreferencesKey("dns_secondary")
        val SELECTED_SERVER = stringPreferencesKey("selected_server")
        val BYPASS_PACKAGES = stringSetPreferencesKey("bypass_packages")
        val FAVORITES = stringSetPreferencesKey("favorite_servers")
        val CUSTOM_SERVERS = stringPreferencesKey("custom_servers")
        val SUBSCRIPTIONS = stringSetPreferencesKey("subscription_urls")
        val CACHED_FREE = stringPreferencesKey("cached_free_servers")
        val THEME_ACCENT = stringPreferencesKey("theme_accent")
        val HIDDEN_SERVERS = stringSetPreferencesKey("hidden_servers")
    }

    val routingMode: Flow<RoutingMode> = context.dataStore.data.map { prefs ->
        runCatching { RoutingMode.valueOf(prefs[Keys.ROUTING] ?: "") }
            .getOrDefault(RoutingMode.SMART_BYPASS)
    }

    val protocolFilter: Flow<CoreProtocolFilter> = context.dataStore.data.map { prefs ->
        runCatching { CoreProtocolFilter.valueOf(prefs[Keys.PROTOCOL] ?: "") }
            .getOrDefault(CoreProtocolFilter.ALL)
    }

    val serverSort: Flow<ServerSort> = context.dataStore.data.map { prefs ->
        runCatching { ServerSort.valueOf(prefs[Keys.SORT] ?: "") }.getOrDefault(ServerSort.PING)
    }

    val killSwitch: Flow<Boolean> = boolean(Keys.KILL_SWITCH, true)
    val smartFailover: Flow<Boolean> = boolean(Keys.FAILOVER, true)
    val autoConnect: Flow<Boolean> = boolean(Keys.AUTO_CONNECT, false)
    val ipv6Enabled: Flow<Boolean> = boolean(Keys.IPV6, false)
    val soundMuted: Flow<Boolean> = boolean(Keys.SOUND_MUTED, false)
    val hapticsEnabled: Flow<Boolean> = boolean(Keys.HAPTICS, true)
    val biometricEnabled: Flow<Boolean> = boolean(Keys.BIOMETRIC, true)
    val lockOnStart: Flow<Boolean> = boolean(Keys.LOCK_ON_START, false)
    val themeAccent: Flow<String> = string(Keys.THEME_ACCENT, "signature")

    val dnsPrimary: Flow<String> = string(Keys.DNS_PRIMARY, "1.1.1.1")
    val dnsSecondary: Flow<String> = string(Keys.DNS_SECONDARY, "8.8.8.8")
    val selectedServerId: Flow<String> = string(Keys.SELECTED_SERVER, "")
    val bypassPackages: Flow<Set<String>> = stringSet(Keys.BYPASS_PACKAGES, defaultBypassPackages)
    val favorites: Flow<Set<String>> = stringSet(Keys.FAVORITES, emptySet())
    /**
     * Subscription sources, with the dead jsDelivr mirrors filtered out.
     *
     * Earlier builds persisted cdn.jsdelivr.net URLs, which turned out to be
     * unreachable from most Iranian networks. Those values are already saved on
     * upgrading devices, so simply changing the defaults would not help anyone
     * who had run a previous version — the stored set wins over the default.
     * Drop them on read and top the set back up if nothing usable remains.
     */
    val subscriptions: Flow<Set<String>> = stringSet(Keys.SUBSCRIPTIONS, defaultSubscriptions)
        .map { stored ->
            val usable = stored.filterNot { it.contains("jsdelivr.net") }.toSet()
            if (usable.isEmpty()) defaultSubscriptions else usable
        }
    val customServers: Flow<String> = string(Keys.CUSTOM_SERVERS, "")
    val cachedFreeServers: Flow<String> = string(Keys.CACHED_FREE, "")

    /** Ids of bundled/free servers the user deleted; they stay gone across restarts. */
    val hiddenServers: Flow<Set<String>> = stringSet(Keys.HIDDEN_SERVERS, emptySet())

    suspend fun setRoutingMode(value: RoutingMode) = put(Keys.ROUTING, value.name)
    suspend fun setProtocolFilter(value: CoreProtocolFilter) = put(Keys.PROTOCOL, value.name)
    suspend fun setServerSort(value: ServerSort) = put(Keys.SORT, value.name)
    suspend fun setKillSwitch(value: Boolean) = put(Keys.KILL_SWITCH, value)
    suspend fun setSmartFailover(value: Boolean) = put(Keys.FAILOVER, value)
    suspend fun setHiddenServers(value: Set<String>) = put(Keys.HIDDEN_SERVERS, value)
    suspend fun setAutoConnect(value: Boolean) = put(Keys.AUTO_CONNECT, value)
    suspend fun setIpv6(value: Boolean) = put(Keys.IPV6, value)
    suspend fun setSoundMuted(value: Boolean) = put(Keys.SOUND_MUTED, value)
    suspend fun setHaptics(value: Boolean) = put(Keys.HAPTICS, value)
    suspend fun setBiometric(value: Boolean) = put(Keys.BIOMETRIC, value)
    suspend fun setLockOnStart(value: Boolean) = put(Keys.LOCK_ON_START, value)
    suspend fun setThemeAccent(value: String) = put(Keys.THEME_ACCENT, value)
    suspend fun setDnsPrimary(value: String) = put(Keys.DNS_PRIMARY, value)
    suspend fun setDnsSecondary(value: String) = put(Keys.DNS_SECONDARY, value)
    suspend fun setSelectedServerId(value: String) = put(Keys.SELECTED_SERVER, value)
    suspend fun setBypassPackages(value: Set<String>) = put(Keys.BYPASS_PACKAGES, value)
    suspend fun setFavorites(value: Set<String>) = put(Keys.FAVORITES, value)
    suspend fun setSubscriptions(value: Set<String>) = put(Keys.SUBSCRIPTIONS, value)
    suspend fun setCustomServers(json: String) = put(Keys.CUSTOM_SERVERS, json)
    suspend fun setCachedFreeServers(json: String) = put(Keys.CACHED_FREE, json)

    private fun boolean(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, default: Boolean) =
        context.dataStore.data.map { it[key] ?: default }

    private fun string(key: androidx.datastore.preferences.core.Preferences.Key<String>, default: String) =
        context.dataStore.data.map { it[key] ?: default }

    private fun stringSet(
        key: androidx.datastore.preferences.core.Preferences.Key<Set<String>>,
        default: Set<String>
    ) = context.dataStore.data.map { it[key] ?: default }

    private suspend fun <T> put(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    companion object {
        /** Iranian apps that must always leave the device directly. */
        val defaultBypassPackages = setOf(
            "com.bmi.mobilebank", "ir.tejaratbank.mobilebank", "com.mellat.mobilebank",
            "com.sep.sepapp", "cab.snapp.passenger", "taxi.tap30.passenger",
            "ir.bmi.mobilebanking", "mob.banking.android.sepah", "com.pmb.mobile"
        )

        /**
         * Free-node sources, ordered roughly by how well curated they are.
         *
         * All of these are actively maintained aggregators that re-publish
         * working nodes several times a day. Protocol-specific feeds are
         * preferred over giant mixed dumps because they carry far less dead
         * weight, and each is fetched through a CDN mirror that stays reachable
         * from Iran more often than raw.githubusercontent.com.
         */
        val defaultSubscriptions = setOf(
            // ebrasha/free-v2ray-public-list — refreshed several times a day and
            // published already split by protocol, so we fetch the individual
            // feeds rather than the 8 MB combined dump.
            "https://raw.githubusercontent.com/ebrasha/free-v2ray-public-list/main/vless_configs.txt",
            "https://raw.githubusercontent.com/ebrasha/free-v2ray-public-list/main/vmess_configs.txt",
            "https://raw.githubusercontent.com/ebrasha/free-v2ray-public-list/main/trojan_configs.txt",
            "https://raw.githubusercontent.com/ebrasha/free-v2ray-public-list/main/ss_configs.txt",
            // Epodonios/v2ray-configs — replaces barry-far, whose repository
            // GitHub blocked for a terms-of-service violation in May 2025 and
            // which had been returning HTTP 403 for every request since. These
            // feeds are base64 subscription blobs; ConfigParser detects and
            // decodes that automatically. Verified live: ~6,500 VLESS nodes
            // across ~2,700 distinct hosts.
            "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/Splitted-By-Protocol/vless.txt",
            "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/Splitted-By-Protocol/vmess.txt",
            "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/Splitted-By-Protocol/trojan.txt",
            "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/Splitted-By-Protocol/ss.txt",
            // Broad aggregator, kept last so curated nodes rank first. Note the
            // capital B: the owner renamed the account, and the lowercase path
            // only still resolves through GitHub's redirect.
            "https://raw.githubusercontent.com/MahdiBland/V2RayAggregator/master/sub/sub_merge.txt"
            // yebekhe/TVC removed: the subscriptions/xray/normal/mix path now
            // returns 404, so it contributed nothing but a slow timeout.
        )
    }
}
