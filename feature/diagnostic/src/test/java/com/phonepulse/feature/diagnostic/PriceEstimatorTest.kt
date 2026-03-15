package com.phonepulse.feature.diagnostic

import com.phonepulse.core.model.DeviceInfo
import com.phonepulse.core.model.FallbackPrice
import com.phonepulse.core.model.ModelPrice
import com.phonepulse.core.model.PriceDatabase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceEstimatorTest {

    private fun makeDevice(
        manufacturer: String = "Samsung",
        model: String = "SM-S918",
        ramGb: Double = 8.0,
        storageGb: Double = 256.0
    ) = DeviceInfo(
        manufacturer = manufacturer,
        model = model,
        androidVersion = "14",
        sdkLevel = 34,
        ramGb = ramGb,
        storageGb = storageGb,
        screenResolution = "1080x2400",
        cpuModel = "Snapdragon",
        cpuCores = 8
    )

    private fun makeEstimator(): PriceEstimator {
        val estimator = PriceEstimator(PriceRepository())
        estimator.setPriceDatabaseForTests(
            PriceDatabase(
                version = 2,
                updated = "2025-01-15",
                models = listOf(
                    ModelPrice("SM-S928", "Samsung Galaxy S24 Ultra", 95000),
                    ModelPrice("SM-S918", "Samsung Galaxy S23 Ultra", 75000)
                ),
                fallback_by_ram_storage = listOf(
                    FallbackPrice(12, 256, 35000),
                    FallbackPrice(8, 256, 25000),
                    FallbackPrice(8, 128, 20000),
                    FallbackPrice(6, 128, 15000),
                    FallbackPrice(4, 64, 8000),
                    FallbackPrice(0, 0, 6000)
                )
            )
        )
        return estimator
    }

    @Test
    fun `estimate returns non-null prices`() {
        val estimator = makeEstimator()
        val (min, max) = estimator.estimate(makeDevice(), 85)
        assertNotNull(min)
        assertNotNull(max)
    }

    @Test
    fun `estimate max is greater than min`() {
        val estimator = makeEstimator()
        val (min, max) = estimator.estimate(makeDevice(), 85)
        assertTrue("max ($max) should be > min ($min)", max!! > min!!)
    }

    @Test
    fun `higher score gives higher price`() {
        val estimator = makeEstimator()
        val device = makeDevice()
        val (_, maxHigh) = estimator.estimate(device, 95)
        val (_, maxLow) = estimator.estimate(device, 50)
        assertTrue("Score 95 price ($maxHigh) should be > score 50 price ($maxLow)", maxHigh!! > maxLow!!)
    }

    @Test
    fun `known model returns model-specific price`() {
        val estimator = makeEstimator()
        val samsung = makeDevice(model = "SM-S928")
        val unknown = makeDevice(model = "UNKNOWN_MODEL_XYZ", ramGb = 8.0, storageGb = 256.0)

        val (_, maxSamsung) = estimator.estimate(samsung, 90)
        val (_, maxUnknown) = estimator.estimate(unknown, 90)

        assertTrue("Known model ($maxSamsung) should be > unknown ($maxUnknown)", maxSamsung!! > maxUnknown!!)
    }

    @Test
    fun `score 0 returns lowest price tier`() {
        val estimator = makeEstimator()
        val (min, _) = estimator.estimate(makeDevice(), 0)
        assertTrue("Min price at score 0 should be > 0", min!! > 0)
    }

    @Test
    fun `fallback works for low RAM device`() {
        val estimator = makeEstimator()
        val device = makeDevice(model = "UNKNOWN", ramGb = 3.0, storageGb = 32.0)
        val (min, max) = estimator.estimate(device, 80)
        assertNotNull(min)
        assertNotNull(max)
        assertTrue(min!! > 0)
    }
}
