package com.phonepulse.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestResultTest {

    @Test
    fun `TestResult created with defaults`() {
        val result = TestResult("battery", 85, TestStatus.PASSED)
        assertEquals("battery", result.moduleName)
        assertEquals(85, result.score)
        assertEquals(TestStatus.PASSED, result.status)
        assertTrue(result.details.isEmpty())
    }

    @Test
    fun `TestResult with details`() {
        val details = mapOf("key1" to "value1", "key2" to "value2")
        val result = TestResult("display", 90, TestStatus.PASSED, details)
        assertEquals(2, result.details.size)
        assertEquals("value1", result.details["key1"])
    }

    @Test
    fun `TestStatus enum values`() {
        assertEquals(4, TestStatus.entries.size)
        assertTrue(TestStatus.entries.contains(TestStatus.PASSED))
        assertTrue(TestStatus.entries.contains(TestStatus.WARNING))
        assertTrue(TestStatus.entries.contains(TestStatus.FAILED))
        assertTrue(TestStatus.entries.contains(TestStatus.SKIPPED))
    }
}
