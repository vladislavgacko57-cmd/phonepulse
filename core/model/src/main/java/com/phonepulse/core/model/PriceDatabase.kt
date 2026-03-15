package com.phonepulse.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PriceDatabase(
    val version: Int,
    val updated: String,
    val currency: String = "RUB",
    val models: List<ModelPrice>,
    val fallback_by_ram_storage: List<FallbackPrice> = emptyList()
)

@Serializable
data class ModelPrice(
    val pattern: String,
    val name: String,
    val price: Int
)

@Serializable
data class FallbackPrice(
    val min_ram_gb: Int,
    val min_storage_gb: Int,
    val price: Int
)
