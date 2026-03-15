package com.phonepulse.feature.diagnostic

import android.content.Context
import android.util.Log
import com.phonepulse.core.model.DeviceInfo
import com.phonepulse.core.model.PriceDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceEstimator @Inject constructor(
    private val priceRepository: PriceRepository
) {
    companion object {
        private const val TAG = "PriceEstimator"
    }

    private var priceDb: PriceDatabase? = null
    private var matchedModelName: String? = null
    private var priceSource: String = "not_loaded"

    suspend fun loadPrices(context: Context) {
        try {
            priceDb = priceRepository.getPriceDatabase(context)
            Log.d(TAG, "Prices loaded: ${priceDb?.models?.size ?: 0} models, source: ${priceDb?.updated}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load prices: ${e.message}", e)
        }
    }

    fun estimate(device: DeviceInfo, overallScore: Int): PriceEstimation {
        Log.d(TAG, "Estimating price for: ${device.manufacturer} ${device.model} (Build.MODEL)")
        Log.d(TAG, "RAM: ${device.ramGb}GB, Storage: ${device.storageGb}GB, Score: $overallScore")
        Log.d(TAG, "Price DB loaded: ${priceDb != null}, models: ${priceDb?.models?.size ?: 0}")

        val basePrice = findBasePrice(device)
        Log.d(TAG, "Base price: $basePrice ($priceSource${matchedModelName?.let { ": $it" } ?: ""})")

        val conditionMultiplier = calculateConditionMultiplier(overallScore)
        val ageMultiplier = calculateAgeMultiplier(device.sdkLevel)
        Log.d(TAG, "Condition mult: $conditionMultiplier, Age mult: $ageMultiplier")

        val estimated = (basePrice * conditionMultiplier * ageMultiplier).toInt()
        val minPrice = ((estimated * 0.92).toInt() / 500) * 500
        val maxPrice = (((estimated * 1.08).toInt() + 499) / 500) * 500

        Log.d(TAG, "Final price: $minPrice - $maxPrice RUB")

        return PriceEstimation(
            minPrice = minPrice,
            maxPrice = maxPrice,
            basePrice = basePrice,
            conditionMultiplier = conditionMultiplier,
            ageMultiplier = ageMultiplier,
            matchedModel = matchedModelName,
            source = priceSource,
            dbVersion = priceDb?.version ?: 0,
            dbUpdated = priceDb?.updated ?: "not_loaded"
        )
    }

    private fun calculateConditionMultiplier(score: Int): Double = when {
        score >= 97 -> 0.97
        score >= 93 -> 0.92
        score >= 88 -> 0.85
        score >= 83 -> 0.78
        score >= 78 -> 0.72
        score >= 73 -> 0.65
        score >= 68 -> 0.58
        score >= 63 -> 0.52
        score >= 55 -> 0.45
        score >= 45 -> 0.38
        score >= 35 -> 0.30
        else -> 0.22
    }

    private fun calculateAgeMultiplier(sdkLevel: Int): Double = when {
        sdkLevel >= 35 -> 1.0
        sdkLevel >= 34 -> 0.97
        sdkLevel >= 33 -> 0.93
        sdkLevel >= 32 -> 0.88
        sdkLevel >= 31 -> 0.82
        sdkLevel >= 30 -> 0.75
        sdkLevel >= 29 -> 0.68
        sdkLevel >= 28 -> 0.60
        else -> 0.50
    }

    private fun findBasePrice(device: DeviceInfo): Int {
        val db = priceDb

        if (db == null || db.models.isEmpty()) {
            Log.w(TAG, "Price DB is null or empty!")
            matchedModelName = null
            priceSource = "no_database"
            return estimateByRamStorage(device.ramGb.toInt(), device.storageGb.toInt())
        }

        val model = device.model.trim()
        Log.d(TAG, "Searching for model: '$model' in ${db.models.size} entries")

        // 1. Точное совпадение
        for (m in db.models) {
            if (model.equals(m.pattern, ignoreCase = true)) {
                matchedModelName = m.name
                priceSource = "exact_match"
                Log.d(TAG, "EXACT match: ${m.pattern} -> ${m.name} = ${m.price}")
                return m.price
            }
        }

        // 2. Model содержит pattern
        for (m in db.models) {
            if (model.contains(m.pattern, ignoreCase = true)) {
                matchedModelName = m.name
                priceSource = "partial_match"
                Log.d(TAG, "PARTIAL match: '$model' contains '${m.pattern}' -> ${m.name} = ${m.price}")
                return m.price
            }
        }

        // 3. Pattern содержит model
        for (m in db.models) {
            if (m.pattern.contains(model, ignoreCase = true)) {
                matchedModelName = m.name
                priceSource = "reverse_match"
                Log.d(TAG, "REVERSE match: '${m.pattern}' contains '$model' -> ${m.name} = ${m.price}")
                return m.price
            }
        }

        // 4. Первые N символов совпадают (для Samsung SM-XXXX)
        if (model.length >= 6) {
            val prefix = model.take(6)
            for (m in db.models) {
                if (m.pattern.startsWith(prefix, ignoreCase = true) ||
                    prefix.startsWith(m.pattern.take(6), ignoreCase = true)
                ) {
                    matchedModelName = m.name
                    priceSource = "prefix_match"
                    Log.d(TAG, "PREFIX match: '${prefix}...' -> ${m.name} = ${m.price}")
                    return m.price
                }
            }
        }

        // 5. Manufacturer + keywords (для Pixel "Pixel 8 Pro" в модели)
        val words = model.split(" ", "_", "-").filter { it.length >= 2 }
        for (m in db.models) {
            val patternWords = m.pattern.split(" ", "_", "-").filter { it.length >= 2 }
            val matchCount = patternWords.count { pw -> words.any { w -> w.equals(pw, ignoreCase = true) } }
            if (matchCount >= 2 || (matchCount >= 1 && patternWords.size == 1)) {
                matchedModelName = m.name
                priceSource = "keyword_match"
                Log.d(TAG, "KEYWORD match: $matchCount words matched -> ${m.name} = ${m.price}")
                return m.price
            }
        }

        Log.d(TAG, "No model match found, trying RAM/Storage fallback")

        // 6. Fallback по RAM/Storage из базы
        val ramGb = device.ramGb.toInt()
        val storageGb = device.storageGb.toInt()
        for (fb in db.fallback_by_ram_storage) {
            if (ramGb >= fb.min_ram_gb && storageGb >= fb.min_storage_gb) {
                matchedModelName = null
                priceSource = "ram_storage_fallback"
                Log.d(TAG, "RAM/Storage fallback: ${ramGb}GB/${storageGb}GB -> ${fb.price}")
                return fb.price
            }
        }

        // 7. Последний fallback
        matchedModelName = null
        priceSource = "default_fallback"
        return estimateByRamStorage(ramGb, storageGb)
    }

    private fun estimateByRamStorage(ramGb: Int, storageGb: Int): Int = when {
        ramGb >= 12 && storageGb >= 256 -> 35000
        ramGb >= 8 && storageGb >= 256 -> 25000
        ramGb >= 8 && storageGb >= 128 -> 18000
        ramGb >= 6 && storageGb >= 128 -> 14000
        ramGb >= 4 && storageGb >= 64 -> 7000
        else -> 5000
    }

    fun getLastUpdated(): String = priceDb?.updated ?: "not loaded"
    fun getModelsCount(): Int = priceDb?.models?.size ?: 0
}

data class PriceEstimation(
    val minPrice: Int,
    val maxPrice: Int,
    val basePrice: Int,
    val conditionMultiplier: Double,
    val ageMultiplier: Double,
    val matchedModel: String?,
    val source: String,
    val dbVersion: Int,
    val dbUpdated: String
)
