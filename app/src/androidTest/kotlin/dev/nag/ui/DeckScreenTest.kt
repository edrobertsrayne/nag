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
        flingCard(direction = 1f)
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

    @Test
    fun swipeLeftDiscardsAndAdvances() {
        val repository = deckWith("dishes" to 1, "laundry" to 2)
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = repository, onOpenQueue = {})
            }
        }
        swipeCardLeft()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("laundry").assertIsDisplayed()
        composeTestRule.onNodeWithText("dishes").assertDoesNotExist()
    }

    @Test
    fun quickFlingLeftDiscardsAndAdvances() {
        val repository = deckWith("dishes" to 1, "laundry" to 2)
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = repository, onOpenQueue = {})
            }
        }
        flingCard(direction = -1f)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("laundry").assertIsDisplayed()
        composeTestRule.onNodeWithText("dishes").assertDoesNotExist()
    }

    @Test
    fun thirdLeftSwipeSpringsBackWithNoDiscardsLeft() {
        val repository = deckWith("dishes" to 1, "laundry" to 2, "bins" to 3)
        composeTestRule.setContent {
            MaterialTheme {
                DeckScreen(repository = repository, onOpenQueue = {})
            }
        }
        swipeCardLeft()
        composeTestRule.waitForIdle()
        swipeCardLeft()
        composeTestRule.waitForIdle()
        swipeCardLeft()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No discards left").assertIsDisplayed()
        composeTestRule.onNodeWithText("bins").assertIsDisplayed()
    }

    /** Full drag past the commit threshold: +1f completes, -1f discards. */
    private fun swipeCard(direction: Float) {
        composeTestRule.onNodeWithTag(DECK_CARD_TAG).performTouchInput {
            down(center)
            moveTo(center + Offset(x = direction * width * 0.6f, y = 0f), delayMillis = 300)
            up()
        }
    }

    private fun swipeCardLeft() = swipeCard(direction = -1f)

    /** Quick flick: a run-up then a fast segment, so VelocityTracker sees ≥3 samples ≤40ms apart. */
    private fun flingCard(direction: Float) {
        composeTestRule.onNodeWithTag(DECK_CARD_TAG).performTouchInput {
            down(center)
            moveTo(center + Offset(x = width * 0.05f * direction, y = 0f))
            moveTo(center + Offset(x = width * 0.15f * direction, y = 0f))
            moveTo(center + Offset(x = width * 0.25f * direction, y = 0f), delayMillis = 20)
            up()
        }
    }

    private fun deckWith(vararg nameToCadence: Pair<String, Int>): FakeNagRepository =
        FakeNagRepository().apply {
            today = 100
            runBlocking {
                nameToCadence.forEach { (name, cadenceDays) -> addChore(name, cadenceDays) }
            }
        }
}
