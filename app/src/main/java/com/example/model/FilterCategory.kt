package com.example.model

data class FilterCategory(
    val id: String,
    val name: String,
    val description: String,
    val domains: List<String>,
    val isEnabled: Boolean = true
)
