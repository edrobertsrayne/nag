package dev.nag.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscardsTest {

    @Test
    fun `fresh day has full budget of two`() {
        assertEquals(2, Discards.left(null, today = 100))
    }

    @Test
    fun `one discard leaves one`() {
        assertEquals(1, Discards.left(DiscardBudget(day = 100, usedCount = 1), today = 100))
    }

    @Test
    fun `budget spent after two discards`() {
        assertEquals(0, Discards.left(DiscardBudget(day = 100, usedCount = 2), today = 100))
    }

    @Test
    fun `budget resets when stored day is not today`() {
        assertEquals(2, Discards.left(DiscardBudget(day = 99, usedCount = 2), today = 100))
    }

    @Test
    fun `recording on a fresh day starts at one`() {
        assertEquals(DiscardBudget(day = 100, usedCount = 1), Discards.record(null, today = 100))
    }

    @Test
    fun `recording twice on the same day spends the budget`() {
        val once = Discards.record(null, today = 100)
        assertEquals(DiscardBudget(day = 100, usedCount = 2), Discards.record(once, today = 100))
    }

    @Test
    fun `recording after a spent budget rolls over to the new day`() {
        val yesterday = DiscardBudget(day = 99, usedCount = 2)
        assertEquals(DiscardBudget(day = 100, usedCount = 1), Discards.record(yesterday, today = 100))
    }
}
