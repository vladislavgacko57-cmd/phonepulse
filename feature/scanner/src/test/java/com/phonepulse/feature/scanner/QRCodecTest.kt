package com.phonepulse.feature.scanner

import com.phonepulse.core.model.Certificate
import com.phonepulse.core.model.DeviceInfo
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream

class QRCodecTest {

    private val testCertificate = Certificate(
        certId = "PP-2025-QRTEST01",
        timestamp = "2025-06-01T12:00:00Z",
        device = DeviceInfo(
            manufacturer = "Google",
            model = "Pixel 8",
            androidVersion = "14",
            sdkLevel = 34,
            ramGb = 8.0,
            storageGb = 128.0,
            screenResolution = "1080x2400",
            cpuModel = "Tensor G3",
            cpuCores = 8
        ),
        results = listOf(
            TestResult("battery", 90, TestStatus.PASSED),
            TestResult("display", 85, TestStatus.PASSED)
        ),
        overallScore = 88,
        grade = "A",
        recommendedPriceMin = 30000,
        recommendedPriceMax = 35000
    )

    private fun encode(cert: Certificate): String {
        val json = Json.encodeToString(cert)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(json.toByteArray()) }
        val encoded = Base64.getEncoder().encodeToString(bos.toByteArray())
        return "PP1:$encoded"
    }

    @Test
    fun `encode produces PP1 prefix`() {
        val qr = encode(testCertificate)
        assertTrue(qr.startsWith("PP1:"))
    }

    @Test
    fun `decodeCertificateFromQR restores certificate`() {
        val qr = encode(testCertificate)
        val decoded = decodeCertificateFromQR(qr)
        assertNotNull(decoded)
        assertEquals(testCertificate.certId, decoded!!.certId)
        assertEquals(testCertificate.overallScore, decoded.overallScore)
        assertEquals(testCertificate.grade, decoded.grade)
        assertEquals(testCertificate.device.model, decoded.device.model)
    }

    @Test
    fun `decodeCertificateFromQR supports web hash payload`() {
        val payload = encode(testCertificate).removePrefix("PP1:")
        val qr = "https://demo.github.io/phonepulse-web/#$payload"
        val decoded = decodeCertificateFromQR(qr)
        assertNotNull(decoded)
        assertEquals(testCertificate.certId, decoded!!.certId)
    }

    @Test
    fun `decodeCertificateFromQR returns null for non-PP1 content`() {
        val result = decodeCertificateFromQR("https://example.com")
        assertNull(result)
    }

    @Test
    fun `decodeCertificateFromQR returns null for corrupted data`() {
        val result = decodeCertificateFromQR("PP1:not_valid_base64!!!")
        assertNull(result)
    }

    @Test
    fun `decodeCertificateFromQR returns null for empty string`() {
        assertNull(decodeCertificateFromQR(""))
    }

    @Test
    fun `decodeCertificateFromQR returns null for PP1 with empty payload`() {
        assertNull(decodeCertificateFromQR("PP1:"))
    }

    @Test
    fun `encoded QR content fits in QR code limit`() {
        val qr = encode(testCertificate)
        val payload = qr.removePrefix("PP1:")
        assertTrue(
            "Payload size ${payload.length} should be < 2953",
            payload.length < 2953
        )
    }

    @Test
    fun `roundtrip with full certificate preserves all results`() {
        val fullCert = testCertificate.copy(
            results = listOf(
                TestResult("battery", 82, TestStatus.PASSED, mapOf("health_pct" to "87")),
                TestResult("display", 95, TestStatus.PASSED, mapOf("dead_pixels" to "false")),
                TestResult("audio", 78, TestStatus.WARNING, mapOf("snr" to "38")),
                TestResult("camera", 90, TestStatus.PASSED),
                TestResult("sensors", 100, TestStatus.PASSED),
                TestResult("connectivity", 95, TestStatus.PASSED),
                TestResult("storage", 88, TestStatus.PASSED),
                TestResult("controls", 100, TestStatus.PASSED)
            )
        )
        val qr = encode(fullCert)
        val decoded = decodeCertificateFromQR(qr)
        assertNotNull(decoded)
        assertEquals(8, decoded!!.results.size)
        assertEquals("87", decoded.results[0].details["health_pct"])
    }
}
