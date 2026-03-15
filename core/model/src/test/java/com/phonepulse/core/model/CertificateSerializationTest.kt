package com.phonepulse.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CertificateSerializationTest {

    private val testCertificate = Certificate(
        certId = "PP-2025-TEST1234",
        timestamp = "2025-01-15T14:30:00Z",
        device = DeviceInfo(
            manufacturer = "Samsung",
            model = "Galaxy S23",
            androidVersion = "14",
            sdkLevel = 34,
            ramGb = 8.0,
            storageGb = 256.0,
            screenResolution = "1080x2340",
            cpuModel = "Snapdragon 8 Gen 2",
            cpuCores = 8
        ),
        results = listOf(
            TestResult("battery", 87, TestStatus.PASSED, mapOf("health_pct" to "87", "cycles" to "342")),
            TestResult("display", 95, TestStatus.PASSED, mapOf("dead_pixels" to "false")),
            TestResult("audio", 78, TestStatus.WARNING, mapOf("mic_snr_db" to "38.5"))
        ),
        overallScore = 87,
        grade = "A",
        recommendedPriceMin = 38000,
        recommendedPriceMax = 43000
    )

    @Test
    fun `certificate serializes to JSON`() {
        val json = Json.encodeToString(testCertificate)
        assertTrue(json.contains("PP-2025-TEST1234"))
        assertTrue(json.contains("Samsung"))
        assertTrue(json.contains("\"overallScore\":87"))
    }

    @Test
    fun `certificate deserializes from JSON`() {
        val json = Json.encodeToString(testCertificate)
        val restored = Json.decodeFromString<Certificate>(json)

        assertEquals(testCertificate.certId, restored.certId)
        assertEquals(testCertificate.overallScore, restored.overallScore)
        assertEquals(testCertificate.grade, restored.grade)
        assertEquals(testCertificate.device.manufacturer, restored.device.manufacturer)
        assertEquals(testCertificate.results.size, restored.results.size)
        assertEquals(testCertificate.recommendedPriceMin, restored.recommendedPriceMin)
    }

    @Test
    fun `certificate roundtrip preserves all data`() {
        val json = Json.encodeToString(testCertificate)
        val restored = Json.decodeFromString<Certificate>(json)
        assertEquals(testCertificate, restored)
    }

    @Test
    fun `TestResult details are preserved`() {
        val json = Json.encodeToString(testCertificate)
        val restored = Json.decodeFromString<Certificate>(json)

        val batteryResult = restored.results.find { it.moduleName == "battery" }
        assertNotNull(batteryResult)
        assertEquals("87", batteryResult!!.details["health_pct"])
        assertEquals("342", batteryResult.details["cycles"])
    }

    @Test
    fun `certificate with empty results serializes`() {
        val empty = testCertificate.copy(results = emptyList(), overallScore = 0, grade = "D")
        val json = Json.encodeToString(empty)
        val restored = Json.decodeFromString<Certificate>(json)
        assertEquals(0, restored.results.size)
        assertEquals(0, restored.overallScore)
    }

    @Test
    fun `certificate with null prices serializes`() {
        val noPrices = testCertificate.copy(recommendedPriceMin = null, recommendedPriceMax = null)
        val json = Json.encodeToString(noPrices)
        val restored = Json.decodeFromString<Certificate>(json)
        assertNull(restored.recommendedPriceMin)
        assertNull(restored.recommendedPriceMax)
    }
}
