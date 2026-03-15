package com.phonepulse.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phonepulse.feature.onboarding.OnboardingScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboarding_showsFirstPage() {
        composeTestRule.setContent {
            OnboardingScreen(onComplete = {})
        }

        composeTestRule.onNodeWithTag("onboarding_title").assertIsDisplayed()
    }

    @Test
    fun onboarding_showsSkipButton() {
        composeTestRule.setContent {
            OnboardingScreen(onComplete = {})
        }

        composeTestRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed()
    }

    @Test
    fun onboarding_showsNextButton() {
        composeTestRule.setContent {
            OnboardingScreen(onComplete = {})
        }

        composeTestRule.onNodeWithTag("onboarding_next_button").assertIsDisplayed()
    }

    @Test
    fun onboarding_skipTriggersComplete() {
        var completed = false

        composeTestRule.setContent {
            OnboardingScreen(onComplete = { completed = true })
        }

        composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(completed)
    }
}
