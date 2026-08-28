package dev.nag.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NagCopyTest {

    @Test
    fun `gentle names the single due chore`() {
        assertEquals(
            "dishes is due today",
            NagCopy.text(NagContent("dishes", dueCount = 1, streak = 3), NagLevel.GENTLE),
        )
    }

    @Test
    fun `gentle counts k more`() {
        assertEquals(
            "dishes + 2 more are due today",
            NagCopy.text(NagContent("dishes", dueCount = 3, streak = 3), NagLevel.GENTLE),
        )
    }

    @Test
    fun `gentle has no streak-zero variant`() {
        assertEquals(
            "dishes is due today",
            NagCopy.text(NagContent("dishes", dueCount = 1, streak = 0), NagLevel.GENTLE),
        )
    }

    @Test
    fun `frequent puts the streak on the line`() {
        assertEquals(
            "dishes is still waiting — 3-day streak on the line",
            NagCopy.text(NagContent("dishes", dueCount = 1, streak = 3), NagLevel.FREQUENT),
        )
    }

    @Test
    fun `frequent counts k more`() {
        assertEquals(
            "dishes + 1 more still waiting — 5-day streak on the line",
            NagCopy.text(NagContent("dishes", dueCount = 2, streak = 5), NagLevel.FREQUENT),
        )
    }

    @Test
    fun `frequent streak-zero starts a new streak today`() {
        assertEquals(
            "dishes is still waiting — start a new streak today",
            NagCopy.text(NagContent("dishes", dueCount = 1, streak = 0), NagLevel.FREQUENT),
        )
    }

    @Test
    fun `frequent streak-zero also counts k more`() {
        assertEquals(
            "dishes + 3 more still waiting — start a new streak today",
            NagCopy.text(NagContent("dishes", dueCount = 4, streak = 0), NagLevel.FREQUENT),
        )
    }

    @Test
    fun `last chance warns the streak dies at midnight`() {
        assertEquals(
            "dishes left — 7-day streak dies at midnight",
            NagCopy.text(NagContent("dishes", dueCount = 1, streak = 7), NagLevel.LAST_CHANCE),
        )
    }

    @Test
    fun `last chance counts k more`() {
        assertEquals(
            "dishes + 1 more left — 7-day streak dies at midnight",
            NagCopy.text(NagContent("dishes", dueCount = 2, streak = 7), NagLevel.LAST_CHANCE),
        )
    }

    @Test
    fun `last chance streak-zero says complete one before midnight`() {
        assertEquals(
            "dishes left — complete one before midnight",
            NagCopy.text(NagContent("dishes", dueCount = 1, streak = 0), NagLevel.LAST_CHANCE),
        )
    }

    @Test
    fun `last chance streak-zero also counts k more`() {
        assertEquals(
            "dishes + 2 more left — complete one before midnight",
            NagCopy.text(NagContent("dishes", dueCount = 3, streak = 0), NagLevel.LAST_CHANCE),
        )
    }
}
