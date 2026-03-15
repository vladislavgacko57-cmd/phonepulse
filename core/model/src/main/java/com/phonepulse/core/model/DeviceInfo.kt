package com.phonepulse.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkLevel: Int,
    val ramGb: Double,
    val storageGb: Double,
    val screenResolution: String,
    val cpuModel: String,
    val cpuCores: Int
)
