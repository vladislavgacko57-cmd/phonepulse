package com.phonepulse.feature.diagnostic

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.phonepulse.core.model.DeviceInfo
import com.phonepulse.core.model.FallbackPrice
import com.phonepulse.core.model.PriceDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceEstimator @Inject constructor(
    private val priceRepository: PriceRepository
) {
    private var priceDb: PriceDatabase? = null

    suspend fun loadPrices(context: Context) {
        priceDb = priceRepository.getPriceDatabase(context)
    }

    fun estimate(device: DeviceInfo, overallScore: Int): Pair<Int?, Int?> {
        val basePrice = findBasePrice(device)

        val conditionMultiplier = when {
            overallScore >= 95 -> 0.95
            overallScore >= 85 -> 0.82
            overallScore >= 70 -> 0.68
            overallScore >= 50 -> 0.50
            else -> 0.30
        }

        val estimated = (basePrice * conditionMultiplier).toInt()
        val minPrice = (estimated * 0.90).toInt()
        val maxPrice = (estimated * 1.10).toInt()

        return minPrice to maxPrice
    }

    private fun findBasePrice(device: DeviceInfo): Int {
        val db = priceDb

        if (db != null) {
            db.models.firstOrNull { device.model.contains(it.pattern, ignoreCase = true) }?.let {
                return it.price
            }

            val ramGb = device.ramGb.toInt()
            val storageGb = device.storageGb.toInt()
            return sortedFallback(db.fallback_by_ram_storage).firstOrNull {
                ramGb >= it.min_ram_gb && storageGb >= it.min_storage_gb
            }?.price ?: 12000
        }

        return 12000
    }

    fun getLastUpdated(): String = priceDb?.updated ?: "встроенная"

    fun getModelsCount(): Int = priceDb?.models?.size ?: 0

    @VisibleForTesting
    internal fun setPriceDatabaseForTests(db: PriceDatabase) {
        priceDb = db
    }

    private fun sortedFallback(source: List<FallbackPrice>): List<FallbackPrice> {
        return source.sortedWith(compareByDescending<FallbackPrice> { it.min_ram_gb }
            .thenByDescending { it.min_storage_gb })
    }
}
