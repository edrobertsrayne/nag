package dev.nag.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nag.data.FakeNagRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditArchiveChoreFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContentWithNagApp(repository: FakeNagRepository) {
        composeTestRule.setContent {
            MaterialTheme {
                NagApp(repository = repository)
            }
        }
    }

    private fun repositoryWithChore(today: Long): FakeNagRepository {
        val repository = FakeNagRepository()
        repository.today = today
        runBlocking { repository.addChore(name = "dishes", cadenceDays = 1) }
        return repository
    }

    @Test
    fun editedChoreShowsNewNameAndCadenceInDeck() {
        val repository = repositoryWithChore(today = 100)
        setContentWithNagApp(repository)
        composeTestRule.onNodeWithText("dishes").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open queue").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Name").performTextClearance()
        composeTestRule.onNodeWithText("Name").performTextInput("washing up")
        composeTestRule.onNodeWithText("Cadence (days)").performTextClearance()
        composeTestRule.onNodeWithText("Cadence (days)").performTextInput("3")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("washing up").assertIsDisplayed()
        composeTestRule.onNodeWithText("every 3 days").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back to deck").performClick()
        composeTestRule.onNodeWithText("washing up").assertIsDisplayed()
    }

    @Test
    fun archivedChoreLeavesQueueAndDeckImmediately() {
        val repository = repositoryWithChore(today = 100)
        setContentWithNagApp(repository)
        composeTestRule.onNodeWithContentDescription("Open queue").performClick()
        composeTestRule.onNodeWithText("dishes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Archive").performClick()
        composeTestRule.onNodeWithText("dishes").assertDoesNotExist()
        composeTestRule.onNodeWithText("No chores yet.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back to deck").performClick()
        composeTestRule.onNodeWithText("Nothing due.").assertIsDisplayed()
    }

    @Test
    fun archivedChoresCompletionsStillCountTowardStreak() {
        val repository = FakeNagRepository(initialCompletionDays = setOf(98, 99, 100)).apply {
            today = 100
        }
        runBlocking { repository.addChore(name = "dishes", cadenceDays = 1) }
        setContentWithNagApp(repository)
        composeTestRule.onNodeWithContentDescription("Open queue").performClick()
        composeTestRule.onNodeWithText("Archive").performClick()
        composeTestRule.onNodeWithContentDescription("Back to deck").performClick()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("day streak").assertIsDisplayed()
    }
}
