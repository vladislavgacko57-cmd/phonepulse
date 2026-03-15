package com.phonepulse.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Certificate(
    val version: String = "1.0",
    val certId: String,
    val timestamp: String,
    val device: DeviceInfo,
    val results: List<TestResult>,
    val overallScore: Int,
    val grade: String,
    val recommendedPriceMin: Int?,
    val recommendedPriceMax: Int?,
    val currency: String = "RUB",
    val matchedModel: String? = null,
    val priceSource: String? = null,
    val priceDbUpdated: String? = null,
    val appSignature: String = ""
)
