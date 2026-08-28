package dev.nag.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.nag.data.FakeNagRepository
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
                DeckScreen(repository = FakeNagRepository(initialStreak = 0))
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
                DeckScreen(repository = FakeNagRepository(initialStreak = 4))
            }
        }
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
    }
}
