package dev.nag.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NagTest {

    private fun chore(
        name: String,
        nextDueDay: Long,
        creationOrder: Long = 0,
        lastDiscardedDay: Long = 0,
        archived: Boolean = false,
    ) = Chore(
        id = 0,
        name = name,
        cadenceDays = 1,
        nextDueDay = nextDueDay,
        creationOrder = creationOrder,
        lastDiscardedDay = lastDiscardedDay,
        archived = archived,
    )

    @Test
    fun `first card is the most overdue chore`() {
        val content = Nag.content(
            listOf(
                chore("laundry", nextDueDay = 99),
                chore("dishes", nextDueDay = 98),
            ),
            completionDays = emptySet(),
            today = 100,
        )
        assertEquals("dishes", content.firstCardTitle)
    }

    @Test
    fun `first card breaks ties by oldest-added`() {
        val content = Nag.content(
            listOf(
                chore("laundry", nextDueDay = 99, creationOrder = 2),
                chore("dishes", nextDueDay = 99, creationOrder = 1),
            ),
            completionDays = emptySet(),
            today = 100,
        )
        assertEquals("dishes", content.firstCardTitle)
    }

    @Test
    fun `due count includes discarded-but-due chores`() {
        val content = Nag.content(
            listOf(
                chore("dishes", nextDueDay = 98, lastDiscardedDay = 100),
                chore("laundry", nextDueDay = 99),
            ),
            completionDays = emptySet(),
            today = 100,
        )
        assertEquals(2, content.dueCount)
        assertEquals("laundry", content.firstCardTitle)
    }

    @Test
    fun `fully discarded deck still names the leading due chore`() {
        val content = Nag.content(
            listOf(
                chore("dishes", nextDueDay = 98, lastDiscardedDay = 100),
                chore("laundry", nextDueDay = 99, lastDiscardedDay = 100),
            ),
            completionDays = emptySet(),
            today = 100,
        )
        assertEquals("dishes", content.firstCardTitle)
        assertEquals(2, content.dueCount)
    }

    @Test
    fun `archived and not-yet-due chores are not counted`() {
        val content = Nag.content(
            listOf(
                chore("old", nextDueDay = 90, archived = true),
                chore("future", nextDueDay = 101),
                chore("dishes", nextDueDay = 98),
            ),
            completionDays = emptySet(),
            today = 100,
        )
        assertEquals(1, content.dueCount)
        assertEquals("dishes", content.firstCardTitle)
    }

    @Test
    fun `nothing due gives an empty content`() {
        val content = Nag.content(emptyList(), completionDays = emptySet(), today = 100)
        assertEquals(NagContent(firstCardTitle = null, dueCount = 0, streak = 0), content)
    }

    @Test
    fun `streak comes from the completion days`() {
        val content = Nag.content(
            listOf(chore("dishes", nextDueDay = 98)),
            completionDays = setOf(99, 100),
            today = 100,
        )
        assertEquals(2, content.streak)
    }
}
