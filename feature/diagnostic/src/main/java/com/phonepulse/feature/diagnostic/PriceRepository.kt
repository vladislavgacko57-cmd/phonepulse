package com.phonepulse.feature.diagnostic

import android.content.Context
import android.util.Log
import com.phonepulse.core.model.FallbackPrice
import com.phonepulse.core.model.ModelPrice
import com.phonepulse.core.model.PriceDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceRepository @Inject constructor() {

    companion object {
        private const val TAG = "PriceRepository"
        private const val REMOTE_URL =
            "https://raw.githubusercontent.com/vladislavgacko57-cmd/phonepulse-data/main/prices/v1/prices.json"
        private const val CACHE_FILE = "prices_cache.json"
        private const val ASSETS_FILE = "prices_fallback.json"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getPriceDatabase(context: Context): PriceDatabase {
        Log.d(TAG, "Loading price database...")

        val remote = tryLoadFromNetwork(context)
        if (remote != null) {
            Log.d(TAG, "Loaded from network: ${remote.models.size} models, v${remote.version}")
            return remote
        }

        val cached = tryLoadFromCache(context)
        if (cached != null) {
            Log.d(TAG, "Loaded from cache: ${cached.models.size} models, v${cached.version}")
            return cached
        }

        val assets = loadFromAssets(context)
        Log.d(TAG, "Loaded from assets: ${assets.models.size} models, v${assets.version}")
        return assets
    }

    private suspend fun tryLoadFromNetwork(context: Context): PriceDatabase? =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Trying network: $REMOTE_URL")
                val connection = URL(REMOTE_URL).openConnection().apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "PhonePulse/1.0")
                }
                val rawText = connection.getInputStream().bufferedReader().readText()
                val text = normalizeJson(rawText)
                Log.d(TAG, "Network response: ${text.length} chars")

                val db = json.decodeFromString<PriceDatabase>(text)

                if (db.models.isNotEmpty()) {
                    try {
                        File(context.cacheDir, CACHE_FILE).writeText(text)
                        Log.d(TAG, "Cached to $CACHE_FILE")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to cache: ${e.message}")
                    }
                    db
                } else {
                    Log.w(TAG, "Network returned empty models list")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network failed: ${e.javaClass.simpleName}: ${e.message}")
                null
            }
        }

    private fun tryLoadFromCache(context: Context): PriceDatabase? {
        return try {
            val cacheFile = File(context.cacheDir, CACHE_FILE)
            if (!cacheFile.exists()) {
                Log.d(TAG, "No cache file")
                return null
            }
            val rawText = cacheFile.readText()
            val text = normalizeJson(rawText)
            if (text.isBlank()) {
                Log.w(TAG, "Cache file empty")
                return null
            }
            val db = json.decodeFromString<PriceDatabase>(text)
            if (db.models.isEmpty()) {
                Log.w(TAG, "Cache has empty models")
                null
            } else {
                db
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache read failed: ${e.message}")
            null
        }
    }

    private fun loadFromAssets(context: Context): PriceDatabase {
        return try {
            val text = tryReadAsset(context, ASSETS_FILE)
                ?: tryReadAsset(context, "prices/$ASSETS_FILE")
                ?: tryReadAsset(context, "prices/v1/prices.json")

            if (text == null) {
                Log.e(TAG, "Cannot find prices JSON in assets! Listing assets:")
                listAssets(context, "")
                return createHardcodedFallback()
            }

            Log.d(TAG, "Read from assets: ${text.length} chars")
            val db = json.decodeFromString<PriceDatabase>(text)

            if (db.models.isEmpty()) {
                Log.e(TAG, "Assets JSON has 0 models, using hardcoded fallback")
                return createHardcodedFallback()
            }

            db
        } catch (e: Exception) {
            Log.e(TAG, "Assets load failed: ${e.message}", e)
            createHardcodedFallback()
        }
    }

    private fun tryReadAsset(context: Context, path: String): String? {
        return try {
            val rawText = context.assets.open(path).bufferedReader().readText()
            val text = normalizeJson(rawText)
            Log.d(TAG, "Found asset: $path (${text.length} chars)")
            if (text.isNotBlank()) text else null
        } catch (e: Exception) {
            Log.d(TAG, "Asset not found: $path")
            null
        }
    }

    private fun normalizeJson(text: String): String {
        // Handles UTF-8 BOM (EF BB BF), often produced by Windows editors.
        return text.removePrefix("\uFEFF").trimStart()
    }

    private fun listAssets(context: Context, path: String) {
        try {
            val list = context.assets.list(path) ?: return
            for (item in list) {
                val fullPath = if (path.isEmpty()) item else "$path/$item"
                Log.d(TAG, "  Asset: $fullPath")
                listAssets(context, fullPath)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Cannot list assets: ${e.message}")
        }
    }

    /**
     * Last-resort fallback used only if network/cache/assets all fail.
     */
    private fun createHardcodedFallback(): PriceDatabase {
        Log.w(TAG, "Using hardcoded fallback prices")
        return PriceDatabase(
            version = 1,
            updated = "hardcoded",
            currency = "RUB",
            models = listOf(
                ModelPrice("SM-S928", "Samsung Galaxy S24 Ultra", 90000),
                ModelPrice("SM-S926", "Samsung Galaxy S24+", 72000),
                ModelPrice("SM-S921", "Samsung Galaxy S24", 58000),
                ModelPrice("SM-S918", "Samsung Galaxy S23 Ultra", 65000),
                ModelPrice("SM-S916", "Samsung Galaxy S23+", 50000),
                ModelPrice("SM-S911", "Samsung Galaxy S23", 42000),
                ModelPrice("SM-S908", "Samsung Galaxy S22 Ultra", 48000),
                ModelPrice("SM-S906", "Samsung Galaxy S22+", 35000),
                ModelPrice("SM-S901", "Samsung Galaxy S22", 30000),
                ModelPrice("SM-G998", "Samsung Galaxy S21 Ultra", 32000),
                ModelPrice("SM-G996", "Samsung Galaxy S21+", 25000),
                ModelPrice("SM-G991", "Samsung Galaxy S21", 22000),
                ModelPrice("SM-A556", "Samsung Galaxy A55", 25000),
                ModelPrice("SM-A546", "Samsung Galaxy A54", 20000),
                ModelPrice("SM-A536", "Samsung Galaxy A53", 16000),
                ModelPrice("SM-A346", "Samsung Galaxy A34", 15000),
                ModelPrice("SM-A256", "Samsung Galaxy A25", 12000),
                ModelPrice("SM-A156", "Samsung Galaxy A15", 9000),
                ModelPrice("SM-F946", "Samsung Galaxy Z Fold6", 120000),
                ModelPrice("SM-F936", "Samsung Galaxy Z Fold5", 95000),
                ModelPrice("SM-F741", "Samsung Galaxy Z Flip6", 65000),
                ModelPrice("SM-F731", "Samsung Galaxy Z Flip5", 48000),
                ModelPrice("Pixel 9 Pro", "Google Pixel 9 Pro", 65000),
                ModelPrice("Pixel 9", "Google Pixel 9", 50000),
                ModelPrice("Pixel 8 Pro", "Google Pixel 8 Pro", 45000),
                ModelPrice("Pixel 8", "Google Pixel 8", 38000),
                ModelPrice("Pixel 7", "Google Pixel 7", 25000),
                ModelPrice("2401116C", "Xiaomi 14", 42000),
                ModelPrice("2210132C", "Xiaomi 13", 32000),
                ModelPrice("23078", "Redmi Note 12 Pro+", 18000),
                ModelPrice("23053", "Redmi Note 12", 11000),
                ModelPrice("24069", "Redmi Note 13 Pro+", 22000),
                ModelPrice("22101316G", "Poco X5 Pro", 16000),
                ModelPrice("23049", "Poco F5", 20000),
                ModelPrice("CPH2581", "OnePlus 12", 48000),
                ModelPrice("CPH2449", "OnePlus 11", 35000),
                ModelPrice("RMX3630", "Realme 11 Pro+", 20000),
                ModelPrice("NTH-NX9", "Nothing Phone (2)", 28000)
            ),
            fallback_by_ram_storage = listOf(
                FallbackPrice(16, 512, 55000),
                FallbackPrice(12, 256, 35000),
                FallbackPrice(8, 256, 25000),
                FallbackPrice(8, 128, 18000),
                FallbackPrice(6, 128, 14000),
                FallbackPrice(4, 64, 7000),
                FallbackPrice(0, 0, 5000)
            )
        )
    }
}
