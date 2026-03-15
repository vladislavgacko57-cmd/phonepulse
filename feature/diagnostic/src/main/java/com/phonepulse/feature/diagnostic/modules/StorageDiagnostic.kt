package com.phonepulse.feature.diagnostic.modules

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class StorageDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "storage"

    override suspend fun runAutomatic(context: Context): TestResult = try {
        withContext(Dispatchers.IO) {
            val details = mutableMapOf<String, String>()
            var score = 100

            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            details["total_gb"] = String.format("%.1f", totalBytes / 1_073_741_824.0)
            details["free_gb"] = String.format("%.1f", freeBytes / 1_073_741_824.0)
            details["used_pct"] = "${((totalBytes - freeBytes) * 100 / totalBytes).toInt()}"

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            details["ram_total_gb"] = String.format("%.1f", memInfo.totalMem / 1_073_741_824.0)
            details["ram_available_gb"] = String.format("%.1f", memInfo.availMem / 1_073_741_824.0)

            try {
                val cpuInfo = File("/proc/cpuinfo").readText()
                val model = cpuInfo.lines()
                    .find { it.startsWith("Hardware") || it.startsWith("model name") }
                    ?.substringAfter(":")
                    ?.trim()
                    ?: "unknown"
                details["cpu_model"] = model
                details["cpu_cores"] = "${Runtime.getRuntime().availableProcessors()}"
            } catch (_: Exception) {
                details["cpu_model"] = "unknown"
                details["cpu_cores"] = "${Runtime.getRuntime().availableProcessors()}"
            }

            val testFile = File(context.cacheDir, "speed_test.bin")
            val testSize = 20 * 1024 * 1024
            val testData = ByteArray(testSize)
            try {
                val writeStart = System.nanoTime()
                testFile.outputStream().use { it.write(testData) }
                val writeTime = (System.nanoTime() - writeStart) / 1_000_000_000.0
                val writeMbps = (testSize / 1_048_576.0) / writeTime
                details["write_speed_mbps"] = String.format("%.0f", writeMbps)

                val readStart = System.nanoTime()
                testFile.inputStream().use { it.read(testData) }
                val readTime = (System.nanoTime() - readStart) / 1_000_000_000.0
                val readMbps = (testSize / 1_048_576.0) / readTime
                details["read_speed_mbps"] = String.format("%.0f", readMbps)

                if (writeMbps < 100) score -= 15
                if (readMbps < 200) score -= 10
            } catch (e: Exception) {
                details["speed_test_error"] = e.message ?: "unknown"
                score -= 10
            } finally {
                testFile.delete()
            }

            val status = when {
                score >= 80 -> TestStatus.PASSED
                score >= 50 -> TestStatus.WARNING
                else -> TestStatus.FAILED
            }

            TestResult(moduleName, score.coerceIn(0, 100), status, details)
        }
    } catch (e: SecurityException) {
        TestResult(
            moduleName,
            0,
            TestStatus.SKIPPED,
            mapOf("error" to "permission_denied", "message" to (e.message ?: ""))
        )
    } catch (e: Exception) {
        TestResult(
            moduleName,
            0,
            TestStatus.FAILED,
            mapOf("error" to (e.message ?: "unknown_error"))
        )
    }
}
