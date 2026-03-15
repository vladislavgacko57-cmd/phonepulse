package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URL
import javax.inject.Inject

/**
 * Wi-Fi speed test that downloads a tiny public CDN file.
 */
class WifiSpeedTest @Inject constructor() : DiagnosticModule {
    override val moduleName = "wifi_speed"

    private val testUrls = listOf(
        "https://speed.cloudflare.com/__down?bytes=5000000",
        "https://proof.ovh.net/files/1Mb.dat",
        "https://ash-speed.hetzner.com/1MB.bin"
    )

    override suspend fun runAutomatic(context: Context): TestResult = withContext(Dispatchers.IO) {
        val details = mutableMapOf<String, String>()

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

        details["connected_via_wifi"] = "$isWifi"

        if (!isWifi) {
            details["note"] = "not_on_wifi"
            return@withContext TestResult(
                moduleName = moduleName,
                score = 50,
                status = TestStatus.WARNING,
                details = details + mapOf("skip_reason" to "Не подключено к Wi-Fi")
            )
        }

        var downloadSpeed = 0.0
        var success = false

        for (url in testUrls) {
            try {
                val result = withTimeout(10_000L) { measureDownloadSpeed(url) }
                if (result > 0) {
                    downloadSpeed = result
                    success = true
                    details["test_url"] = url.substringBefore("?").substringAfterLast("/")
                    break
                }
            } catch (_: Exception) {
                // Try next CDN.
            }
        }

        if (!success) {
            return@withContext TestResult(
                moduleName = moduleName,
                score = 30,
                status = TestStatus.WARNING,
                details = details + mapOf("error" to "all_download_servers_failed")
            )
        }

        details["download_mbps"] = String.format("%.1f", downloadSpeed)

        val score = when {
            downloadSpeed >= 100 -> 100
            downloadSpeed >= 50 -> 90
            downloadSpeed >= 25 -> 75
            downloadSpeed >= 10 -> 60
            downloadSpeed >= 5 -> 45
            else -> 30
        }

        details["speed_label"] = when {
            downloadSpeed >= 100 -> "Отлично"
            downloadSpeed >= 50 -> "Очень хорошо"
            downloadSpeed >= 25 -> "Хорошо"
            downloadSpeed >= 10 -> "Средне"
            else -> "Медленно"
        }

        val status = when {
            score >= 80 -> TestStatus.PASSED
            score >= 50 -> TestStatus.WARNING
            else -> TestStatus.FAILED
        }

        TestResult(moduleName, score, status, details)
    }

    private fun measureDownloadSpeed(urlString: String): Double {
        val connection = URL(urlString).openConnection().apply {
            connectTimeout = 5000
            readTimeout = 8000
            setRequestProperty("User-Agent", "PhonePulse/1.0")
        }

        val startTime = System.nanoTime()
        var totalBytes = 0L
        val buffer = ByteArray(8192)

        connection.getInputStream().use { input ->
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                totalBytes += bytesRead
            }
        }

        val elapsedSec = (System.nanoTime() - startTime) / 1_000_000_000.0
        return if (elapsedSec > 0) (totalBytes * 8.0) / (elapsedSec * 1_000_000) else 0.0
    }
}
