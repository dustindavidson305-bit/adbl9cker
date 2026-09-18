package com.example.model

data class DnsQueryLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val domain: String,
    val isBlocked: Boolean,
    val category: String?,
    val timestamp: Long = System.currentTimeMillis()
)
