package dev.nag.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {

    @Test
    fun `no completions means streak zero`() {
        assertEquals(0, Streak.of(emptySet(), today = 100))
    }

    @Test
    fun `completion today only is a one-day streak`() {
        assertEquals(1, Streak.of(setOf(100), today = 100))
    }

    @Test
    fun `completion yesterday only is a one-day streak`() {
        assertEquals(1, Streak.of(setOf(99), today = 100))
    }

    @Test
    fun `today and yesterday are two consecutive days`() {
        assertEquals(2, Streak.of(setOf(99, 100), today = 100))
    }

    @Test
    fun `run ending yesterday ignores today being absent`() {
        assertEquals(3, Streak.of(setOf(97, 98, 99), today = 100))
    }

    @Test
    fun `gap between completions breaks the run`() {
        assertEquals(1, Streak.of(setOf(98, 100), today = 100))
    }

    @Test
    fun `streak older than yesterday does not count`() {
        assertEquals(0, Streak.of(setOf(96, 97, 98), today = 100))
    }

    @Test
    fun `duplicate irrelevant days outside the run do not inflate it`() {
        assertEquals(2, Streak.of(setOf(90, 99, 100), today = 100))
    }
}
