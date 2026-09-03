package dev.nag.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckTest {

    private fun chore(nextDueDay: Long, creationOrder: Long = 0, cadenceDays: Int = 1) = Chore(
        id = 0,
        name = "chore",
        cadenceDays = cadenceDays,
        nextDueDay = nextDueDay,
        creationOrder = creationOrder,
    )

    @Test
    fun `chore is due when today equals next-due`() {
        assertTrue(chore(nextDueDay = 100).isDue(today = 100))
    }

    @Test
    fun `overdue chore stays due`() {
        assertTrue(chore(nextDueDay = 97).isDue(today = 100))
    }

    @Test
    fun `chore is not due before next-due`() {
        assertFalse(chore(nextDueDay = 101).isDue(today = 100))
    }

    @Test
    fun `empty chores produce an empty deck`() {
        assertEquals(emptyList<Chore>(), Deck.order(emptyList(), today = 100))
    }

    @Test
    fun `deck excludes chores that are not due`() {
        val deck = Deck.order(listOf(chore(nextDueDay = 99), chore(nextDueDay = 101)), today = 100)
        assertEquals(listOf(99L), deck.map { it.nextDueDay })
    }

    @Test
    fun `deck keeps overdue chores`() {
        val deck = Deck.order(listOf(chore(nextDueDay = 97)), today = 100)
        assertEquals(listOf(97L), deck.map { it.nextDueDay })
    }

    @Test
    fun `deck orders most-overdue first`() {
        val deck = Deck.order(
            listOf(chore(nextDueDay = 100), chore(nextDueDay = 98), chore(nextDueDay = 99)),
            today = 100,
        )
        assertEquals(listOf(98L, 99L, 100L), deck.map { it.nextDueDay })
    }

    @Test
    fun `ties break most-recently-added first`() {
        val deck = Deck.order(
            listOf(
                chore(nextDueDay = 99, creationOrder = 3),
                chore(nextDueDay = 99, creationOrder = 1),
                chore(nextDueDay = 99, creationOrder = 2),
            ),
            today = 100,
        )
        assertEquals(listOf(3L, 2L, 1L), deck.map { it.creationOrder })
    }

    @Test
    fun `deck orders by cadence before overdue-ness`() {
        val deck = Deck.order(
            listOf(
                chore(nextDueDay = 90, cadenceDays = 16),
                chore(nextDueDay = 99, cadenceDays = 3),
            ),
            today = 100,
        )
        assertEquals(listOf(3, 16), deck.map { it.cadenceDays })
    }

    @Test
    fun `deck hides chores discarded today`() {
        val discarded = chore(nextDueDay = 100).discardedOn(day = 100)
        val deck = Deck.order(listOf(discarded, chore(nextDueDay = 101)), today = 100)
        assertEquals(emptyList<Chore>(), deck)
    }

    @Test
    fun `discarded chore returns to the deck the next day`() {
        val discarded = chore(nextDueDay = 100).discardedOn(day = 100)
        val deck = Deck.order(listOf(discarded), today = 101)
        assertEquals(listOf(discarded), deck)
    }

    @Test
    fun `deck excludes archived chores`() {
        val archived = chore(nextDueDay = 99).archivedOn()
        val deck = Deck.order(listOf(archived, chore(nextDueDay = 100)), today = 100)
        assertEquals(listOf(100L), deck.map { it.nextDueDay })
    }

    @Test
    fun `archived overdue chore stays out of the deck`() {
        val archived = chore(nextDueDay = 97).archivedOn()
        assertEquals(emptyList<Chore>(), Deck.order(listOf(archived), today = 100))
    }
}
