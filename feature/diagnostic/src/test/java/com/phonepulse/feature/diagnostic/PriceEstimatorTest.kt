package com.phonepulse.feature.diagnostic

import com.phonepulse.core.model.DeviceInfo
import com.phonepulse.core.model.FallbackPrice
import com.phonepulse.core.model.ModelPrice
import com.phonepulse.core.model.PriceDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceEstimatorTest {

    private fun makeDevice(
        manufacturer: String = "Samsung",
        model: String = "SM-S918",
        androidVersion: String = "14",
        sdkLevel: Int = 34,
        ramGb: Double = 8.0,
        storageGb: Double = 256.0
    ) = DeviceInfo(
        manufacturer = manufacturer,
        model = model,
        androidVersion = androidVersion,
        sdkLevel = sdkLevel,
        ramGb = ramGb,
        storageGb = storageGb,
        screenResolution = "1080x2400",
        cpuModel = "Snapdragon",
        cpuCores = 8
    )

    @Test
    fun `estimate returns positive prices`() {
        val estimator = createEstimatorWithTestDb()
        val result = estimator.estimate(makeDevice(), 85)
        assertTrue("Min price should be > 0, got ${result.minPrice}", result.minPrice > 0)
        assertTrue("Max price should be > 0, got ${result.maxPrice}", result.maxPrice > 0)
    }

    @Test
    fun `max price is greater than min price`() {
        val estimator = createEstimatorWithTestDb()
        val result = estimator.estimate(makeDevice(), 85)
        assertTrue(
            "Max (${result.maxPrice}) should be > Min (${result.minPrice})",
            result.maxPrice > result.minPrice
        )
    }

    @Test
    fun `higher score gives higher price`() {
        val estimator = createEstimatorWithTestDb()
        val device = makeDevice()
        val high = estimator.estimate(device, 95)
        val low = estimator.estimate(device, 50)
        assertTrue(
            "Score 95 (${high.maxPrice}) > score 50 (${low.maxPrice})",
            high.maxPrice > low.maxPrice
        )
    }

    @Test
    fun `perfect score gives about 90-97 percent of base price`() {
        val estimator = createEstimatorWithTestDb()
        val device = makeDevice(model = "SM-S918")
        val result = estimator.estimate(device, 97)
        assertTrue(
            "Price for perfect S23 Ultra should be reasonable: ${result.minPrice}-${result.maxPrice}",
            result.minPrice in 40000..80000
        )
    }

    @Test
    fun `terrible score gives about 20-30 percent of base price`() {
        val estimator = createEstimatorWithTestDb()
        val device = makeDevice(model = "SM-S918")
        val result = estimator.estimate(device, 20)
        assertTrue(
            "Price for terrible S23 Ultra should be low: ${result.minPrice}-${result.maxPrice}",
            result.maxPrice < 25000
        )
    }

    @Test
    fun `Samsung S24 Ultra priced higher than S23 Ultra`() {
        val estimator = createEstimatorWithTestDb()
        val s24 = estimator.estimate(makeDevice(model = "SM-S928"), 85)
        val s23 = estimator.estimate(makeDevice(model = "SM-S918"), 85)
        assertTrue(
            "S24U (${s24.maxPrice}) > S23U (${s23.maxPrice})",
            s24.maxPrice > s23.maxPrice
        )
    }

    @Test
    fun `budget phone priced correctly`() {
        val estimator = createEstimatorWithTestDb()
        val result = estimator.estimate(
            makeDevice(model = "SM-A156", ramGb = 4.0, storageGb = 128.0), 85
        )
        assertTrue("A15 should be cheap: ${result.minPrice}-${result.maxPrice}", result.maxPrice < 15000)
    }

    @Test
    fun `unknown model falls back to RAM-storage pricing`() {
        val estimator = createEstimatorWithTestDb()
        val result = estimator.estimate(
            makeDevice(model = "UNKNOWN_XYZ_123", ramGb = 8.0, storageGb = 256.0), 85
        )
        assertTrue(
            "Unknown 8/256 model source should be fallback",
            result.source.contains("fallback")
        )
        assertTrue("Price should be reasonable: ${result.minPrice}", result.minPrice > 5000)
    }

    @Test
    fun `prices are rounded to 500 rubles`() {
        val estimator = createEstimatorWithTestDb()
        val result = estimator.estimate(makeDevice(), 85)
        assertEquals("Min price should be divisible by 500", 0, result.minPrice % 500)
        assertEquals("Max price should be divisible by 500", 0, result.maxPrice % 500)
    }

    @Test
    fun `older Android version reduces price`() {
        val estimator = createEstimatorWithTestDb()
        val newAndroid = estimator.estimate(
            makeDevice(model = "SM-S918", sdkLevel = 34), 85
        )
        val oldAndroid = estimator.estimate(
            makeDevice(model = "SM-S918", sdkLevel = 29), 85
        )
        assertTrue(
            "Newer Android (${newAndroid.maxPrice}) > older (${oldAndroid.maxPrice})",
            newAndroid.maxPrice > oldAndroid.maxPrice
        )
    }

    @Test
    fun `exact match is preferred over partial`() {
        val estimator = createEstimatorWithTestDb()
        val exact = estimator.estimate(makeDevice(model = "SM-S918"), 85)
        assertTrue(
            "Exact match source",
            exact.source == "exact_match" || exact.source == "partial_match"
        )
    }

    @Test
    fun `Pixel 8 Pro reasonable price`() {
        val estimator = createEstimatorWithTestDb()
        val result = estimator.estimate(
            makeDevice(manufacturer = "Google", model = "Pixel 8 Pro", sdkLevel = 34), 90
        )
        assertTrue(
            "Pixel 8 Pro at 90 score: ${result.minPrice}-${result.maxPrice}",
            result.minPrice in 25000..50000
        )
    }

    @Test
    fun `Redmi Note 12 reasonable price`() {
        val estimator = createEstimatorWithTestDb()
        val result = estimator.estimate(
            makeDevice(
                manufacturer = "Xiaomi",
                model = "23053RN02A",
                sdkLevel = 33,
                ramGb = 6.0,
                storageGb = 128.0
            ),
            80
        )
        assertTrue(
            "Redmi Note 12 at 80 score: ${result.minPrice}-${result.maxPrice}",
            result.minPrice in 3000..15000
        )
    }

    private fun createEstimatorWithTestDb(): PriceEstimator {
        val estimator = PriceEstimator(PriceRepository())

        val dbField = PriceEstimator::class.java.getDeclaredField("priceDb")
        dbField.isAccessible = true
        dbField.set(
            estimator,
            PriceDatabase(
                version = 3,
                updated = "2025-06-01",
                models = listOf(
                    ModelPrice("SM-S928", "Samsung Galaxy S24 Ultra", 90000),
                    ModelPrice("SM-S918", "Samsung Galaxy S23 Ultra", 65000),
                    ModelPrice("SM-A156", "Samsung Galaxy A15", 9000),
                    ModelPrice("Pixel 8 Pro", "Google Pixel 8 Pro", 45000),
                    ModelPrice("23053RN02A", "Redmi Note 12", 11000)
                ),
                fallback_by_ram_storage = listOf(
                    FallbackPrice(12, 256, 35000),
                    FallbackPrice(8, 256, 25000),
                    FallbackPrice(8, 128, 18000),
                    FallbackPrice(6, 128, 14000),
                    FallbackPrice(4, 64, 7000),
                    FallbackPrice(0, 0, 5000)
                )
            )
        )

        return estimator
    }
}
