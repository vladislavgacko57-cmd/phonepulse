package com.phonepulse.feature.diagnostic

import com.phonepulse.core.model.TestResult

interface DiagnosticModule {
    val moduleName: String
    suspend fun runAutomatic(context: android.content.Context): TestResult
}
