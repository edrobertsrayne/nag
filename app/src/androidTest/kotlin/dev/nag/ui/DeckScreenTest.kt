package dev.nag.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nag.data.FakeNagRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeckScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyDeckShowsStreakAndCalmMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = FakeNagRepository(), onOpenQueue = {})
            }
        }
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("day streak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nothing due.").assertIsDisplayed()
    }

    @Test
    fun deckShowsCurrentStreak() {
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(
                    repository = FakeNagRepository(
                        initialCompletionDays = setOf(96, 97, 98, 99),
                    ).apply { today = 100 },
                    onOpenQueue = {},
                )
            }
        }
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun swipeRightCompletesAndAdvances() {
        val repository = deckWith("dishes" to 1, "laundry" to 2)
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = repository, onOpenQueue = {})
            }
        }
        composeTestRule.onNodeWithTag(DECK_CARD_TAG).performTouchInput {
            down(center)
            moveTo(center + Offset(x = width * 0.6f, y = 0f), delayMillis = 300)
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("laundry").assertIsDisplayed()
        composeTestRule.onNodeWithText("dishes").assertDoesNotExist()
    }

    @Test
    fun quickFlingCompletesAndAdvances() {
        val repository = deckWith("dishes" to 1, "laundry" to 2)
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = repository, onOpenQueue = {})
            }
        }
        composeTestRule.onNodeWithTag(DECK_CARD_TAG).performTouchInput {
            down(center)
            moveTo(center + Offset(x = width * 0.25f, y = 0f), delayMillis = 50)
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("laundry").assertIsDisplayed()
        composeTestRule.onNodeWithText("dishes").assertDoesNotExist()
    }

    @Test
    fun shortSwipeSpringsBackAndKeepsCard() {
        val repository = deckWith("dishes" to 1, "laundry" to 2)
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = repository, onOpenQueue = {})
            }
        }
        composeTestRule.onNodeWithTag(DECK_CARD_TAG).performTouchInput {
            down(center)
            moveTo(center + Offset(x = width * 0.1f, y = 0f), delayMillis = 300)
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("dishes").assertIsDisplayed()
        composeTestRule.onNodeWithText("laundry").assertDoesNotExist()
    }

    @Test
    fun completingOnlyCardShowsEmptyStateWithStreakOne() {
        val repository = deckWith("dishes" to 1)
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = repository, onOpenQueue = {})
            }
        }
        composeTestRule.onNodeWithTag(DECK_CARD_TAG).performTouchInput {
            down(center)
            moveTo(center + Offset(x = width * 0.6f, y = 0f), delayMillis = 300)
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("day streak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nothing due.").assertIsDisplayed()
    }

    private fun deckWith(vararg nameToCadence: Pair<String, Int>): FakeNagRepository =
        FakeNagRepository().apply {
            today = 100
            runBlocking {
                nameToCadence.forEach { (name, cadenceDays) -> addChore(name, cadenceDays) }
            }
        }
}
