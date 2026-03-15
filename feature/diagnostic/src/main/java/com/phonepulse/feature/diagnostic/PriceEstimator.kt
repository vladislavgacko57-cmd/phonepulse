package com.phonepulse.feature.diagnostic

import android.content.Context
import com.phonepulse.core.model.DeviceInfo
import com.phonepulse.core.model.PriceDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceEstimator @Inject constructor(
    private val priceRepository: PriceRepository
) {
    private var priceDb: PriceDatabase? = null
    private var matchedModelName: String? = null
    private var priceSource: String = "fallback"

    suspend fun loadPrices(context: Context) {
        priceDb = priceRepository.getPriceDatabase(context)
    }

    fun estimate(device: DeviceInfo, overallScore: Int): PriceEstimation {
        val basePrice = findBasePrice(device)
        val conditionMultiplier = calculateConditionMultiplier(overallScore)
        val ageMultiplier = calculateAgeMultiplier(device.sdkLevel)

        val estimated = (basePrice * conditionMultiplier * ageMultiplier).toInt()

        val minPrice = (estimated * 0.92).toInt()
        val maxPrice = (estimated * 1.08).toInt()

        val roundedMin = (minPrice / 500) * 500
        val roundedMax = ((maxPrice + 499) / 500) * 500

        return PriceEstimation(
            minPrice = roundedMin,
            maxPrice = roundedMax,
            basePrice = basePrice,
            conditionMultiplier = conditionMultiplier,
            ageMultiplier = ageMultiplier,
            matchedModel = matchedModelName,
            source = priceSource,
            dbVersion = priceDb?.version ?: 0,
            dbUpdated = priceDb?.updated ?: "built-in"
        )
    }

    private fun calculateConditionMultiplier(score: Int): Double {
        return when {
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
    }

    private fun calculateAgeMultiplier(sdkLevel: Int): Double {
        return when {
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
    }

    private fun findBasePrice(device: DeviceInfo): Int {
        val db = priceDb

        if (db != null) {
            for (model in db.models) {
                if (device.model.equals(model.pattern, ignoreCase = true)) {
                    matchedModelName = model.name
                    priceSource = "exact_match"
                    return model.price
                }
            }

            for (model in db.models) {
                if (device.model.contains(model.pattern, ignoreCase = true) ||
                    model.pattern.contains(device.model, ignoreCase = true)
                ) {
                    matchedModelName = model.name
                    priceSource = "partial_match"
                    return model.price
                }
            }

            val searchTerms = listOf(
                device.model,
                device.model.substringBefore(" "),
                device.model.replace(" ", "")
            )

            for (term in searchTerms) {
                for (model in db.models) {
                    if (term.length >= 4 && model.pattern.contains(term, ignoreCase = true)) {
                        matchedModelName = model.name
                        priceSource = "fuzzy_match"
                        return model.price
                    }
                }
            }

            val ramGb = device.ramGb.toInt()
            val storageGb = device.storageGb.toInt()
            for (fb in db.fallback_by_ram_storage) {
                if (ramGb >= fb.min_ram_gb && storageGb >= fb.min_storage_gb) {
                    matchedModelName = null
                    priceSource = "ram_storage_fallback"
                    return fb.price
                }
            }
        }

        matchedModelName = null
        priceSource = "default_fallback"
        return 10000
    }

    fun getLastUpdated(): String = priceDb?.updated ?: "built-in"

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
