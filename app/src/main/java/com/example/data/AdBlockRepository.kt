package com.example.data

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
    val recentLogs: List<DnsQueryLog> = emptyList()
)

object AdBlockRepository {

    private val defaultCategories = listOf(
        FilterCategory(
            id = "gaming",
            name = "Mobile Game Ad Networks",
            description = "Blocks interstitial, rewarded, and banner ads inside mobile games",
            domains = listOf(
                "unityads.unity3d.com",
                "unity3d.com",
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
                "facebook.com/tr",
                "graph.facebook.com",
                "adjust.com",
                "appsflyer.com",
                "branch.io",
                "crashlytics.com",
                "flurry.com",
                "tiktok.com",
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

    fun setVpnActive(active: Boolean) {
        _state.update { it.copy(isVpnActive = active) }
    }

    fun isDomainBlocked(rawDomain: String): Pair<Boolean, String?> {
        val domain = rawDomain.lowercase(Locale.ROOT).trim()

        val currentState = _state.value

        // Check whitelist first
        for (whitelisted in currentState.whitelistedDomains) {
            if (domain == whitelisted || domain.endsWith(".$whitelisted")) {
                return Pair(false, null)
            }
        }

        // Check custom blocked domains
        for (blocked in currentState.customBlockedDomains) {
            if (domain == blocked || domain.endsWith(".$blocked") || domain.contains(blocked)) {
                return Pair(true, "Custom Rule")
            }
        }

        // Check active categories
        for (category in currentState.categories) {
            if (category.isEnabled) {
                for (adDomain in category.domains) {
                    if (domain == adDomain || domain.endsWith(".$adDomain") || domain.contains(adDomain)) {
                        return Pair(true, category.name)
                    }
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
    }

    fun addCustomBlocked(rawDomain: String): Boolean {
        val domain = rawDomain.trim().lowercase(Locale.ROOT)
        if (domain.isBlank()) return false
        _state.update { current ->
            current.copy(
                customBlockedDomains = current.customBlockedDomains + domain,
                whitelistedDomains = current.whitelistedDomains - domain
            )
        }
        return true
    }

    fun removeCustomBlocked(domain: String) {
        _state.update { current ->
            current.copy(customBlockedDomains = current.customBlockedDomains - domain)
        }
    }

    fun addWhitelist(rawDomain: String): Boolean {
        val domain = rawDomain.trim().lowercase(Locale.ROOT)
        if (domain.isBlank()) return false
        _state.update { current ->
            current.copy(
                whitelistedDomains = current.whitelistedDomains + domain,
                customBlockedDomains = current.customBlockedDomains - domain
            )
        }
        return true
    }

    fun removeWhitelist(domain: String) {
        _state.update { current ->
            current.copy(whitelistedDomains = current.whitelistedDomains - domain)
        }
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
