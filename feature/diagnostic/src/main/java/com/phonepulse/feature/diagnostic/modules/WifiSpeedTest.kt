package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * Wi-Fi speed test with region-safe endpoints.
 */
class WifiSpeedTest @Inject constructor() : DiagnosticModule {
    override val moduleName = "wifi_speed"

    private val testUrls = listOf(
        // File in GitHub (usually reachable)
        "https://raw.githubusercontent.com/vladislavgacko57-cmd/phonepulse-data/main/prices/v1/prices.json",
        // Yandex
        "https://yandex.ru/",
        // Google
        "https://www.google.com/",
        // Cloudflare
        "https://speed.cloudflare.com/__down?bytes=1000000",
        // Hetzner
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
                details = details + mapOf("skip_reason" to "Not connected to Wi-Fi"),
                summary = "Speed test failed (Not connected to Wi-Fi)"
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
                    details["test_url"] = url
                    break
                }
            } catch (e: Exception) {
                Log.w("WifiSpeedTest", "Speed check failed for $url: ${e.message}")
            }
        }

        if (!success) {
            details["error"] = "download_servers_unavailable"

            return@withContext TestResult(
                moduleName,
                70,
                TestStatus.WARNING,
                details,
                "Internet connected, speed test servers unavailable"
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

        val speedLabel = when {
            downloadSpeed >= 100 -> "Отлично"
            downloadSpeed >= 50 -> "Очень хорошо"
            downloadSpeed >= 25 -> "Хорошо"
            downloadSpeed >= 10 -> "Средне"
            else -> "Медленно"
        }
        details["speed_label"] = speedLabel

        val status = when {
            score >= 80 -> TestStatus.PASSED
            score >= 50 -> TestStatus.WARNING
            else -> TestStatus.FAILED
        }

        TestResult(
            moduleName = moduleName,
            score = score,
            status = status,
            details = details,
            summary = "Download: ${"%.1f".format(downloadSpeed)} Mbps ($speedLabel)"
        )
    }

    private fun measureDownloadSpeed(urlString: String): Double {
        val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 6000
        connection.setRequestProperty("User-Agent", "PhonePulse/1.0")
        connection.setRequestProperty("Accept", "*/*")
        connection.instanceFollowRedirects = true

        try {
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode !in 200..399) {
                return 0.0
            }

            val startTime = System.nanoTime()
            var totalBytes = 0L
            val buffer = ByteArray(8192)

            connection.inputStream.use { input ->
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    totalBytes += bytesRead
                    if (totalBytes > 2 * 1024 * 1024) break
                }
            }

            val elapsedSec = (System.nanoTime() - startTime) / 1_000_000_000.0

            return if (elapsedSec > 0.01 && totalBytes > 100) {
                (totalBytes * 8.0) / (elapsedSec * 1_000_000)
            } else 0.0
        } finally {
            connection.disconnect()
        }
    }
}
