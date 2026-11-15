package com.example.model

data class DnsQueryLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val domain: String,
    val isBlocked: Boolean,
    val category: String?,
    val timestamp: Long = System.currentTimeMillis(),
    /** True when this call goes to a known ad-network event/verification host. */
    val isBeacon: Boolean = false,
    /** Display name of the ad network the beacon belongs to, e.g. "UnityAds". */
    val beaconLabel: String? = null
)
