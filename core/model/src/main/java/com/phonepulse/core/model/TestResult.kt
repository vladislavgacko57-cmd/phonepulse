package com.phonepulse.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TestStatus { PASSED, WARNING, FAILED, SKIPPED }

@Serializable
data class TestResult(
    val moduleName: String,
    val score: Int,
    val status: TestStatus,
    val details: Map<String, String> = emptyMap(),
    val summary: String = ""
)
