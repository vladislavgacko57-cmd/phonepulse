package com.phonepulse.feature.diagnostic

import android.content.Context
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

    // Replace with your own GitHub Pages URL:
    // https://<username>.github.io/<repo>/prices/v1/prices.json
    private val remoteUrl =
        "https://raw.githubusercontent.com/vladislavgacko57-cmd/phonepulse-data/main/prices/v1/prices.json"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var cachedDb: PriceDatabase? = null

    suspend fun getPriceDatabase(context: Context): PriceDatabase {
        cachedDb?.let { return it }

        val remote = tryLoadFromNetwork(context)
        if (remote != null) {
            cachedDb = remote
            return remote
        }

        val cached = tryLoadFromCache(context)
        if (cached != null) {
            cachedDb = cached
            return cached
        }

        val fallback = loadFromAssets(context)
        cachedDb = fallback
        return fallback
    }

    private suspend fun tryLoadFromNetwork(context: Context): PriceDatabase? =
        withContext(Dispatchers.IO) {
            try {
                val raw = URL(remoteUrl).readText()
                val db = json.decodeFromString<PriceDatabase>(raw)
                File(context.cacheDir, "prices_cache.json").writeText(raw)
                db
            } catch (_: Exception) {
                null
            }
        }

    private fun tryLoadFromCache(context: Context): PriceDatabase? {
        return try {
            val cacheFile = File(context.cacheDir, "prices_cache.json")
            if (cacheFile.exists()) {
                json.decodeFromString<PriceDatabase>(cacheFile.readText())
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadFromAssets(context: Context): PriceDatabase {
        val raw = context.assets.open("prices_fallback.json").bufferedReader().readText()
        return json.decodeFromString(raw)
    }
}
