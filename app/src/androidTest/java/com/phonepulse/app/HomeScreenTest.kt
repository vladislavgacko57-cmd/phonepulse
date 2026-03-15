package com.phonepulse.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phonepulse.feature.home.HomeScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysAppName() {
        composeTestRule.setContent {
            HomeScreen(
                onStartDiagnostic = {},
                onScanQR = {},
                onHistory = {}
            )
        }

        composeTestRule.onNodeWithTag("home_app_name").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysSlogan() {
        composeTestRule.setContent {
            HomeScreen(
                onStartDiagnostic = {},
                onScanQR = {},
                onHistory = {}
            )
        }

        composeTestRule.onNodeWithTag("home_slogan").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysDiagnosticButton() {
        composeTestRule.setContent {
            HomeScreen(
                onStartDiagnostic = {},
                onScanQR = {},
                onHistory = {}
            )
        }

        composeTestRule.onNodeWithTag("home_start_button").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysScanButton() {
        composeTestRule.setContent {
            HomeScreen(
                onStartDiagnostic = {},
                onScanQR = {},
                onHistory = {}
            )
        }

        composeTestRule.onNodeWithTag("home_scan_button").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysHistoryButton() {
        composeTestRule.setContent {
            HomeScreen(
                onStartDiagnostic = {},
                onScanQR = {},
                onHistory = {}
            )
        }

        composeTestRule.onNodeWithTag("home_history_button").assertIsDisplayed()
    }

    @Test
    fun homeScreen_diagnosticButtonClickable() {
        var clicked = false

        composeTestRule.setContent {
            HomeScreen(
                onStartDiagnostic = { clicked = true },
                onScanQR = {},
                onHistory = {}
            )
        }

        composeTestRule.onNodeWithTag("home_start_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(clicked)
    }
}
