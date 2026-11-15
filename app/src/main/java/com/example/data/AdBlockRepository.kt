package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.DnsQueryLog
import com.example.model.FilterCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class AdBlockState(
    val isVpnActive: Boolean = false,
    val blockedCount: Int = 0,
    val totalQueriesCount: Int = 0,
    val estimatedBytesSaved: Long = 0L,
    val categories: List<FilterCategory> = emptyList(),
    val customBlockedDomains: Set<String> = emptySet(),
    val whitelistedDomains: Set<String> = emptySet(),
    val recentLogs: List<DnsQueryLog> = emptyList(),
    val blocklistSize: Int = 0
)

object AdBlockRepository {

    private val defaultCategories = listOf(
        FilterCategory(
            id = "gaming",
            name = "Mobile Game Ad Networks",
            description = "Blocks interstitial, rewarded, and banner ads inside mobile games",
            domains = listOf(
                "unityads.unity3d.com",
                "applovin.com",
                "applvn.com",
                "admob.com",
                "googleads.g.doubleclick.net",
                "vungle.com",
                "ironsource.com",
                "supersonicads.com",
                "chartboost.com",
                "mintegral.com",
                "pangle.io",
                "tapjoy.com",
                "adcolony.com",
                "inmobi.com",
                "fyber.com",
                "startapp.com",
                "smaato.com"
            ),
            isEnabled = true
        ),
        FilterCategory(
            id = "trackers",
            name = "Tracking & User Profiling",
            description = "Blocks behavioral trackers, telemetry, and conversion pixels",
            domains = listOf(
                "graph.facebook.com",
                "adjust.com",
                "appsflyer.com",
                "branch.io",
                "crashlytics.com",
                "flurry.com",
                "bytedance.com",
                "snssdk.com",
                "scorecardresearch.com",
                "quantserve.com",
                "moatads.com"
            ),
            isEnabled = true
        ),
        FilterCategory(
            id = "web_ads",
            name = "Web & Interstitial Ad Networks",
            description = "Stops popup, banner, video, and programmatic ad exchange networks",
            domains = listOf(
                "doubleclick.net",
                "adservice.google.com",
                "pagead2.googlesyndication.com",
                "googlesyndication.com",
                "ads.mopub.com",
                "outbrain.com",
                "taboola.com",
                "criteo.com",
                "pubmatic.com",
                "openx.net",
                "rubiconproject.com",
                "casalemedia.com",
                "advertising.com",
                "adtechus.com",
                "yieldmo.com",
                "sovrn.com",
                "indexexchange.com",
                "media.net",
                "amazon-adsystem.com",
                "adsystem.com",
                "adnxs.com",
                "adsrvr.org",
                "bidswitch.net",
                "smartadserver.com",
                "adform.net",
                "spotxchange.com",
                "teads.tv",
                "triplelift.com",
                "sharethrough.com",
                "conversantmedia.com",
                "contextweb.com",
                "districtm.io",
                "gumgum.com",
                "loopme.com"
            ),
            isEnabled = true
        )
    )

    private val _state = MutableStateFlow(
        AdBlockState(
            categories = defaultCategories
        )
    )
    val state: StateFlow<AdBlockState> = _state.asStateFlow()

    // ------------------------------------------------------------------
    // Persistence + bundled core blocklist
    // ------------------------------------------------------------------

    private const val TAG = "AdBlockRepository"
    private const val PREFS_NAME = "adblock_prefs"
    private const val KEY_CUSTOM_BLOCKED = "custom_blocked"
    private const val KEY_WHITELISTED = "whitelisted"
    private const val KEY_DISABLED_CATEGORIES = "disabled_categories"
    private const val BLOCKLIST_ASSET = "blocklist.txt"

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var coreBlocklist: Set<String> = emptySet()

    @Volatile
    private var initialized = false

    /**
     * Load persisted rules and the bundled core blocklist. Safe to call
     * from both the activity and the VPN service; only the first call
     * does work. The blocklist (77k+ domains from the StevenBlack hosts
     * project, MIT-licensed) loads on a background thread so startup
     * never blocks.
     */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val savedBlocked = sp.getStringSet(KEY_CUSTOM_BLOCKED, emptySet()) ?: emptySet()
        val savedWhitelist = sp.getStringSet(KEY_WHITELISTED, emptySet()) ?: emptySet()
        val disabledCats = sp.getStringSet(KEY_DISABLED_CATEGORIES, emptySet()) ?: emptySet()

        _state.update { current ->
            current.copy(
                customBlockedDomains = savedBlocked.toSet(),
                whitelistedDomains = savedWhitelist.toSet(),
                categories = current.categories.map { cat ->
                    if (cat.id in disabledCats) cat.copy(isEnabled = false) else cat
                }
            )
        }

        Thread({
            try {
                val domains = HashSet<String>(90_000)
                appContext.assets.open(BLOCKLIST_ASSET).bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val d = line.trim()
                        if (d.isNotEmpty()) domains.add(d)
                    }
                }
                coreBlocklist = domains
                _state.update { it.copy(blocklistSize = domains.size) }
                Log.i(TAG, "Core blocklist loaded: ${domains.size} domains")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load core blocklist", e)
            }
        }, "BlocklistLoader").start()
    }

    private fun persist() {
        val sp = prefs ?: return
        val current = _state.value
        sp.edit()
            .putStringSet(KEY_CUSTOM_BLOCKED, current.customBlockedDomains)
            .putStringSet(KEY_WHITELISTED, current.whitelistedDomains)
            .putStringSet(
                KEY_DISABLED_CATEGORIES,
                current.categories.filter { !it.isEnabled }.map { it.id }.toSet()
            )
            .apply()
    }

    fun setVpnActive(active: Boolean) {
        _state.update { it.copy(isVpnActive = active) }
    }

    private fun matchesDomain(domain: String, rule: String): Boolean =
        domain == rule || domain.endsWith(".$rule")

    fun isDomainBlocked(rawDomain: String): Pair<Boolean, String?> {
        val domain = rawDomain.lowercase(Locale.ROOT).trim().trimEnd('.')
        if (domain.isEmpty()) return Pair(false, null)

        val currentState = _state.value

        // Check whitelist first
        for (whitelisted in currentState.whitelistedDomains) {
            if (matchesDomain(domain, whitelisted)) {
                return Pair(false, null)
            }
        }

        // Check custom blocked domains
        for (blocked in currentState.customBlockedDomains) {
            if (matchesDomain(domain, blocked)) {
                return Pair(true, "Custom Rule")
            }
        }

        // Check active categories
        for (category in currentState.categories) {
            if (category.isEnabled) {
                for (adDomain in category.domains) {
                    if (matchesDomain(domain, adDomain)) {
                        return Pair(true, category.name)
                    }
                }
            }
        }

        // Check the bundled core blocklist: the domain itself or any
        // parent domain of it may be listed.
        val blocklist = coreBlocklist
        if (blocklist.isNotEmpty()) {
            var candidate = domain
            while (true) {
                if (blocklist.contains(candidate)) {
                    return Pair(true, "Core Blocklist")
                }
                val dot = candidate.indexOf('.')
                if (dot < 0) break
                candidate = candidate.substring(dot + 1)
                // Stop before bare TLDs.
                if (!candidate.contains('.')) {
                    if (blocklist.contains(candidate)) return Pair(true, "Core Blocklist")
                    break
                }
            }
        }

        return Pair(false, null)
    }

    fun recordQuery(domain: String, isBlocked: Boolean, category: String?) {
        val newLog = DnsQueryLog(
            domain = domain,
            isBlocked = isBlocked,
            category = category,
            timestamp = System.currentTimeMillis()
        )

        _state.update { current ->
            val updatedLogs = (listOf(newLog) + current.recentLogs).take(100)
            val newBlocked = if (isBlocked) current.blockedCount + 1 else current.blockedCount
            val newTotal = current.totalQueriesCount + 1
            // Average ad script/asset request saved estimate ~ 42 KB
            val newBytesSaved = if (isBlocked) current.estimatedBytesSaved + (42 * 1024L) else current.estimatedBytesSaved

            current.copy(
                blockedCount = newBlocked,
                totalQueriesCount = newTotal,
                estimatedBytesSaved = newBytesSaved,
                recentLogs = updatedLogs
            )
        }
    }

    fun toggleCategory(categoryId: String, enabled: Boolean) {
        _state.update { current ->
            val updated = current.categories.map { cat ->
                if (cat.id == categoryId) cat.copy(isEnabled = enabled) else cat
            }
            current.copy(categories = updated)
        }
        persist()
    }

    fun addCustomBlocked(rawDomain: String): Boolean {
        val domain = rawDomain.trim().lowercase(Locale.ROOT).trimEnd('.')
            .removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (domain.isBlank()) return false
        _state.update { current ->
            current.copy(
                customBlockedDomains = current.customBlockedDomains + domain,
                whitelistedDomains = current.whitelistedDomains - domain
            )
        }
        persist()
        return true
    }

    fun removeCustomBlocked(domain: String) {
        _state.update { current ->
            current.copy(customBlockedDomains = current.customBlockedDomains - domain)
        }
        persist()
    }

    fun addWhitelist(rawDomain: String): Boolean {
        val domain = rawDomain.trim().lowercase(Locale.ROOT).trimEnd('.')
            .removePrefix("https://").removePrefix("http://").substringBefore('/')
        if (domain.isBlank()) return false
        _state.update { current ->
            current.copy(
                whitelistedDomains = current.whitelistedDomains + domain,
                customBlockedDomains = current.customBlockedDomains - domain
            )
        }
        persist()
        return true
    }

    fun removeWhitelist(domain: String) {
        _state.update { current ->
            current.copy(whitelistedDomains = current.whitelistedDomains - domain)
        }
        persist()
    }

    fun clearStats() {
        _state.update { current ->
            current.copy(
                blockedCount = 0,
                totalQueriesCount = 0,
                estimatedBytesSaved = 0L,
                recentLogs = emptyList()
            )
        }
    }

    fun simulateTestQuery(domain: String) {
        val (isBlocked, category) = isDomainBlocked(domain)
        recordQuery(domain, isBlocked, category)
    }
}
