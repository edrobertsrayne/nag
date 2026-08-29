package dev.nag.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nag.data.FakeNagRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddChoreFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContentWithNagApp(today: Long) {
        val repository = FakeNagRepository()
        repository.today = today
        composeTestRule.setContent {
            MaterialTheme {
                NagApp(repository = repository)
            }
        }
    }

    @Test
    fun addedChoreAppearsInTodaysDeck() {
        setContentWithNagApp(today = 100)
        composeTestRule.onNodeWithText("Nothing due.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open queue").performClick()
        composeTestRule.onNodeWithText("Name").performTextInput("dishes")
        composeTestRule.onNodeWithText("Cadence (days)").performTextClearance()
        composeTestRule.onNodeWithText("Cadence (days)").performTextInput("1")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("dishes").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back to deck").performClick()
        composeTestRule.onNodeWithText("dishes").assertIsDisplayed()
    }

    @Test
    fun addFormRejectsCadenceBelowOne() {
        setContentWithNagApp(today = 100)
        composeTestRule.onNodeWithContentDescription("Open queue").performClick()
        composeTestRule.onNodeWithText("Name").performTextInput("dishes")
        composeTestRule.onNodeWithText("Cadence (days)").performTextClearance()
        composeTestRule.onNodeWithText("Cadence (days)").performTextInput("0")
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()
    }
}
