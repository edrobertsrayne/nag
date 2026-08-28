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
}
