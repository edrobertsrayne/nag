package dev.nag.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoreTest {

    private fun overdueChore() = Chore(
        id = 1,
        name = "dishes",
        cadenceDays = 1,
        nextDueDay = 97,
        creationOrder = 1,
    )

    @Test
    fun `new chore is due immediately`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100)
        assertEquals(100, chore.nextDueDay)
        assertTrue(chore.isDue(today = 100))
    }

    @Test
    fun `new chore keeps its cadence and name`() {
        val chore = Chore.create(name = "laundry", cadenceDays = 3, today = 100)
        assertEquals("laundry", chore.name)
        assertEquals(3, chore.cadenceDays)
    }

    @Test
    fun `completing sets next due to completion day plus cadence`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 3, today = 100)
        assertEquals(103, chore.completedOn(completionDay = 100).nextDueDay)
    }

    @Test
    fun `completing a daily chore makes it due tomorrow`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100)
        assertEquals(101, chore.completedOn(completionDay = 100).nextDueDay)
    }

    @Test
    fun `late completion anchors cadence to completion day not old due date`() {
        val chore = Chore(
            id = 1,
            name = "bins",
            cadenceDays = 3,
            nextDueDay = 98,
            creationOrder = 1,
        )
        assertEquals(108, chore.completedOn(completionDay = 105).nextDueDay)
    }

    @Test
    fun `discarding leaves next-due unchanged`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100)
        assertEquals(100, chore.discardedOn(day = 100).nextDueDay)
    }

    @Test
    fun `discarded chore stays due`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100)
        assertTrue(chore.discardedOn(day = 100).isDue(today = 100))
    }

    @Test
    fun `discarded chore is hidden for the rest of the discard day`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100).discardedOn(day = 100)
        assertTrue(chore.isHiddenOn(day = 100))
    }

    @Test
    fun `discarded chore is visible again the next day`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100).discardedOn(day = 100)
        assertTrue(!chore.isHiddenOn(day = 101))
    }

    @Test
    fun `editing changes name and cadence`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100)
        val edited = chore.editedTo(name = "washing up", cadenceDays = 3)
        assertEquals("washing up", edited.name)
        assertEquals(3, edited.cadenceDays)
    }

    @Test
    fun `editing leaves next-due untouched`() {
        assertEquals(97, overdueChore().editedTo(name = "dishes", cadenceDays = 7).nextDueDay)
    }

    @Test
    fun `completing after an edit anchors the new cadence from the completion day`() {
        val edited = overdueChore().editedTo(name = "dishes", cadenceDays = 7)
        assertEquals(112, edited.completedOn(completionDay = 105).nextDueDay)
    }

    @Test
    fun `editing keeps an overdue chore due`() {
        assertTrue(overdueChore().editedTo(name = "dishes", cadenceDays = 7).isDue(today = 100))
    }

    @Test
    fun `archiving marks the chore archived`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 1, today = 100)
        assertTrue(chore.archivedOn().archived)
    }

    @Test
    fun `archiving leaves next-due and history untouched`() {
        val chore = Chore.create(name = "dishes", cadenceDays = 3, today = 100).discardedOn(day = 100)
        val archived = chore.archivedOn()
        assertEquals(100, archived.nextDueDay)
        assertEquals(100, archived.lastDiscardedDay)
        assertEquals("dishes", archived.name)
    }
}
