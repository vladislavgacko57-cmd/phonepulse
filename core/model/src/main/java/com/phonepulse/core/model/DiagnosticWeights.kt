package com.phonepulse.core.model

object DiagnosticWeights {
    val WEIGHTS = mapOf(
        "battery" to 0.25,
        "display" to 0.20,
        "camera" to 0.15,
        "storage" to 0.10,
        "audio" to 0.10,
        "sensors" to 0.07,
        "connectivity" to 0.05,
        "controls" to 0.05,
        "wifi_speed" to 0.03
    )

    fun calculateOverall(results: List<TestResult>): Int {
        var total = 0.0
        var weightSum = 0.0
        for (result in results) {
            val weight = WEIGHTS[result.moduleName] ?: 0.05
            total += result.score * weight
            weightSum += weight
        }
        return if (weightSum > 0) (total / weightSum).toInt() else 0
    }

    fun gradeFromScore(score: Int): String = when {
        score >= 95 -> "S"
        score >= 85 -> "A"
        score >= 70 -> "B"
        score >= 50 -> "C"
        else -> "D"
    }
}
