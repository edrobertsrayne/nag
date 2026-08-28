package dev.nag.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NagScheduleTest {

    private val monday = LocalDate.parse("2026-08-24").toEpochDay()
    private val tuesday = monday + 1
    private val wednesday = monday + 2
    private val thursday = monday + 3
    private val friday = monday + 4
    private val saturday = monday + 5
    private val sunday = monday + 6

    private val weekdayTable = listOf(
        NagSlot(18 * 60, NagLevel.GENTLE),
        NagSlot(19 * 60, NagLevel.FREQUENT),
        NagSlot(20 * 60, NagLevel.FREQUENT),
        NagSlot(21 * 60, NagLevel.LAST_CHANCE),
        NagSlot(21 * 60 + 30, NagLevel.LAST_CHANCE),
    )

    private val weekendTable = listOf(
        NagSlot(10 * 60, NagLevel.GENTLE),
        NagSlot(12 * 60, NagLevel.GENTLE),
        NagSlot(14 * 60, NagLevel.FREQUENT),
        NagSlot(16 * 60, NagLevel.FREQUENT),
        NagSlot(18 * 60, NagLevel.FREQUENT),
        NagSlot(20 * 60, NagLevel.FREQUENT),
        NagSlot(21 * 60, NagLevel.LAST_CHANCE),
        NagSlot(21 * 60 + 30, NagLevel.LAST_CHANCE),
    )

    @Test
    fun `monday through friday are weekdays`() {
        listOf(monday, tuesday, wednesday, thursday, friday).forEach {
            assertFalse(NagSchedule.isWeekend(it))
        }
    }

    @Test
    fun `saturday and sunday are weekend days`() {
        assertTrue(NagSchedule.isWeekend(saturday))
        assertTrue(NagSchedule.isWeekend(sunday))
    }

    @Test
    fun `weekday table is the escalating evening slots`() {
        assertEquals(weekdayTable, NagSchedule.slotsFor(monday))
    }

    @Test
    fun `weekend table is the steady daytime slots`() {
        assertEquals(weekendTable, NagSchedule.slotsFor(saturday))
    }

    @Test
    fun `every weekday uses the same table`() {
        assertEquals(NagSchedule.slotsFor(monday), NagSchedule.slotsFor(friday))
    }

    @Test
    fun `every weekend day uses the same table`() {
        assertEquals(NagSchedule.slotsFor(saturday), NagSchedule.slotsFor(sunday))
    }

    @Test
    fun `weekday slots resolve to their levels`() {
        assertEquals(NagLevel.GENTLE, NagSchedule.slotAt(monday, 18 * 60)?.level)
        assertEquals(NagLevel.FREQUENT, NagSchedule.slotAt(monday, 19 * 60)?.level)
        assertEquals(NagLevel.FREQUENT, NagSchedule.slotAt(monday, 20 * 60)?.level)
        assertEquals(NagLevel.LAST_CHANCE, NagSchedule.slotAt(monday, 21 * 60)?.level)
        assertEquals(NagLevel.LAST_CHANCE, NagSchedule.slotAt(monday, 21 * 60 + 30)?.level)
    }

    @Test
    fun `weekend slots resolve to their levels`() {
        assertEquals(NagLevel.GENTLE, NagSchedule.slotAt(saturday, 10 * 60)?.level)
        assertEquals(NagLevel.GENTLE, NagSchedule.slotAt(saturday, 12 * 60)?.level)
        assertEquals(NagLevel.FREQUENT, NagSchedule.slotAt(saturday, 14 * 60)?.level)
        assertEquals(NagLevel.FREQUENT, NagSchedule.slotAt(saturday, 16 * 60)?.level)
        assertEquals(NagLevel.FREQUENT, NagSchedule.slotAt(saturday, 18 * 60)?.level)
        assertEquals(NagLevel.FREQUENT, NagSchedule.slotAt(saturday, 20 * 60)?.level)
        assertEquals(NagLevel.LAST_CHANCE, NagSchedule.slotAt(saturday, 21 * 60)?.level)
        assertEquals(NagLevel.LAST_CHANCE, NagSchedule.slotAt(saturday, 21 * 60 + 30)?.level)
    }

    @Test
    fun `minutes without a slot resolve to nothing`() {
        assertNull(NagSchedule.slotAt(monday, 9 * 60))
        assertNull(NagSchedule.slotAt(monday, 11 * 60))
        assertNull(NagSchedule.slotAt(saturday, 11 * 60))
        assertNull(NagSchedule.slotAt(saturday, 13 * 60))
    }

    @Test
    fun `minute before 0900 is quiet`() {
        assertTrue(NagSchedule.isQuiet(9 * 60 - 1))
    }

    @Test
    fun `minute 0900 is not quiet`() {
        assertFalse(NagSchedule.isQuiet(9 * 60))
    }

    @Test
    fun `minute 2130 is not quiet because the slot itself posts`() {
        assertFalse(NagSchedule.isQuiet(21 * 60 + 30))
    }

    @Test
    fun `minute after 2130 is quiet`() {
        assertTrue(NagSchedule.isQuiet(21 * 60 + 31))
    }

    @Test
    fun `weekday 0800 starts the silent window`() {
        assertTrue(NagSchedule.isSilent(monday, 8 * 60))
    }

    @Test
    fun `weekday 0759 is outside the silent window but quiet`() {
        assertFalse(NagSchedule.isSilent(monday, 8 * 60 - 1))
        assertTrue(NagSchedule.isQuiet(8 * 60 - 1))
    }

    @Test
    fun `weekday 0900 is silent but not quiet`() {
        assertTrue(NagSchedule.isSilent(monday, 9 * 60))
        assertFalse(NagSchedule.isQuiet(9 * 60))
    }

    @Test
    fun `weekday 1759 is still silent`() {
        assertTrue(NagSchedule.isSilent(monday, 17 * 60 + 59))
    }

    @Test
    fun `weekday 1800 ends the silent window`() {
        assertFalse(NagSchedule.isSilent(monday, 18 * 60))
    }

    @Test
    fun `weekend days have no silent window`() {
        assertFalse(NagSchedule.isSilent(saturday, 10 * 60))
        assertFalse(NagSchedule.isSilent(sunday, 12 * 60))
        assertFalse(NagSchedule.isSilent(saturday, 17 * 60 + 59))
    }

    @Test
    fun `next slot is strictly future at the exact slot minute`() {
        assertEquals(
            ScheduledNag(monday, NagSlot(19 * 60, NagLevel.FREQUENT)),
            NagSchedule.nextSlot(monday, 18 * 60),
        )
    }

    @Test
    fun `next slot from a gap minute skips to the next slot`() {
        assertEquals(
            ScheduledNag(saturday, NagSlot(12 * 60, NagLevel.GENTLE)),
            NagSchedule.nextSlot(saturday, 11 * 60),
        )
    }

    @Test
    fun `next slot from weekday afternoon is the 1800 gentle slot`() {
        assertEquals(
            ScheduledNag(monday, NagSlot(18 * 60, NagLevel.GENTLE)),
            NagSchedule.nextSlot(monday, 15 * 60),
        )
    }

    @Test
    fun `next slot from weekend morning is the 1000 gentle slot`() {
        assertEquals(
            ScheduledNag(saturday, NagSlot(10 * 60, NagLevel.GENTLE)),
            NagSchedule.nextSlot(saturday, 9 * 60),
        )
    }

    @Test
    fun `next slot after the last weekday slot rolls to tomorrows table`() {
        assertEquals(
            ScheduledNag(tuesday, NagSlot(18 * 60, NagLevel.GENTLE)),
            NagSchedule.nextSlot(monday, 21 * 60 + 30),
        )
        assertEquals(
            ScheduledNag(tuesday, NagSlot(18 * 60, NagLevel.GENTLE)),
            NagSchedule.nextSlot(monday, 23 * 60 + 59),
        )
    }

    @Test
    fun `next slot from sunday night rolls onto the weekday table`() {
        assertEquals(
            ScheduledNag(monday + 7, NagSlot(18 * 60, NagLevel.GENTLE)),
            NagSchedule.nextSlot(sunday, 21 * 60 + 30),
        )
    }

    @Test
    fun `next slot from friday night rolls onto the weekend table`() {
        assertEquals(
            ScheduledNag(saturday, NagSlot(10 * 60, NagLevel.GENTLE)),
            NagSchedule.nextSlot(friday, 23 * 60 + 59),
        )
    }
}
