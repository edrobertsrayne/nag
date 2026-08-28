package dev.nag.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoreTest {

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
}
