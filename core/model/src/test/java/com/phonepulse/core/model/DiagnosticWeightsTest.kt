package com.phonepulse.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticWeightsTest {

    @Test
    fun `gradeFromScore returns S for 95 and above`() {
        assertEquals("S", DiagnosticWeights.gradeFromScore(95))
        assertEquals("S", DiagnosticWeights.gradeFromScore(100))
    }

    @Test
    fun `gradeFromScore returns A for 85-94`() {
        assertEquals("A", DiagnosticWeights.gradeFromScore(85))
        assertEquals("A", DiagnosticWeights.gradeFromScore(94))
    }

    @Test
    fun `gradeFromScore returns B for 70-84`() {
        assertEquals("B", DiagnosticWeights.gradeFromScore(70))
        assertEquals("B", DiagnosticWeights.gradeFromScore(84))
    }

    @Test
    fun `gradeFromScore returns C for 50-69`() {
        assertEquals("C", DiagnosticWeights.gradeFromScore(50))
        assertEquals("C", DiagnosticWeights.gradeFromScore(69))
    }

    @Test
    fun `gradeFromScore returns D for below 50`() {
        assertEquals("D", DiagnosticWeights.gradeFromScore(49))
        assertEquals("D", DiagnosticWeights.gradeFromScore(0))
    }

    @Test
    fun `calculateOverall with all perfect scores returns 100`() {
        val results = listOf(
            TestResult("battery", 100, TestStatus.PASSED),
            TestResult("display", 100, TestStatus.PASSED),
            TestResult("camera", 100, TestStatus.PASSED),
            TestResult("storage", 100, TestStatus.PASSED),
            TestResult("audio", 100, TestStatus.PASSED),
            TestResult("sensors", 100, TestStatus.PASSED),
            TestResult("connectivity", 100, TestStatus.PASSED),
            TestResult("controls", 100, TestStatus.PASSED)
        )
        assertEquals(100, DiagnosticWeights.calculateOverall(results))
    }

    @Test
    fun `calculateOverall with all zero scores returns 0`() {
        val results = listOf(
            TestResult("battery", 0, TestStatus.FAILED),
            TestResult("display", 0, TestStatus.FAILED),
            TestResult("camera", 0, TestStatus.FAILED),
            TestResult("storage", 0, TestStatus.FAILED),
            TestResult("audio", 0, TestStatus.FAILED),
            TestResult("sensors", 0, TestStatus.FAILED),
            TestResult("connectivity", 0, TestStatus.FAILED),
            TestResult("controls", 0, TestStatus.FAILED)
        )
        assertEquals(0, DiagnosticWeights.calculateOverall(results))
    }

    @Test
    fun `calculateOverall with mixed scores respects weights`() {
        val results = listOf(
            TestResult("battery", 80, TestStatus.PASSED),
            TestResult("display", 90, TestStatus.PASSED),
            TestResult("camera", 70, TestStatus.WARNING),
            TestResult("storage", 85, TestStatus.PASSED),
            TestResult("audio", 60, TestStatus.WARNING),
            TestResult("sensors", 100, TestStatus.PASSED),
            TestResult("connectivity", 95, TestStatus.PASSED),
            TestResult("controls", 100, TestStatus.PASSED)
        )
        val overall = DiagnosticWeights.calculateOverall(results)
        assertEquals(82, overall)
    }

    @Test
    fun `calculateOverall with empty list returns 0`() {
        assertEquals(0, DiagnosticWeights.calculateOverall(emptyList()))
    }

    @Test
    fun `calculateOverall with unknown module uses default weight`() {
        val results = listOf(
            TestResult("unknown_module", 80, TestStatus.PASSED)
        )
        val overall = DiagnosticWeights.calculateOverall(results)
        assertEquals(80, overall)
    }

    @Test
    fun `weights sum to 1`() {
        val sum = DiagnosticWeights.WEIGHTS.values.sum()
        assertEquals(1.0, sum, 0.001)
    }
}
