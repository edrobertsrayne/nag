package dev.nag.domain

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NagFireTest {

    private val monday = LocalDate.parse("2026-08-24").toEpochDay()
    private val saturday = monday + 5

    private fun choreDueToday(name: String = "dishes") = Chore(
        id = 1,
        name = name,
        cadenceDays = 1,
        nextDueDay = monday,
        creationOrder = 1,
    )

    private fun choreNotDue(name: String = "laundry") = Chore(
        id = 2,
        name = name,
        cadenceDays = 1,
        nextDueDay = monday + 1,
        creationOrder = 2,
    )

    @Test
    fun `weekday 1800 slot posts a gentle nag naming the due chore`() {
        val post = NagFire.at(listOf(choreDueToday()), emptySet(), monday, 18 * 60)
        assertEquals(NagLevel.GENTLE, post?.level)
        assertEquals("dishes is due today", post?.text)
    }

    @Test
    fun `weekday 2100 slot posts a last chance nag with the streak`() {
        val post = NagFire.at(listOf(choreDueToday()), setOf(monday - 1), monday, 21 * 60)
        assertEquals(NagLevel.LAST_CHANCE, post?.level)
        assertEquals("dishes left — 1-day streak dies at midnight", post?.text)
    }

    @Test
    fun `weekend 1000 slot posts a gentle nag`() {
        val post = NagFire.at(listOf(choreDueToday()), emptySet(), saturday, 10 * 60)
        assertEquals(NagLevel.GENTLE, post?.level)
        assertEquals("dishes is due today", post?.text)
    }

    @Test
    fun `minute with no slot posts nothing`() {
        assertNull(NagFire.at(listOf(choreDueToday()), emptySet(), monday, 15 * 60))
    }

    @Test
    fun `late delivery within the grace still posts the slot`() {
        val post = NagFire.at(listOf(choreDueToday()), emptySet(), monday, 18 * 60 + 7)
        assertEquals(NagLevel.GENTLE, post?.level)
        assertEquals("dishes is due today", post?.text)
    }

    @Test
    fun `delivery past the grace skips and never posts late`() {
        assertNull(NagFire.at(listOf(choreDueToday()), emptySet(), monday, 18 * 60 + NagSchedule.LATE_DELIVERY_GRACE_MINUTES + 1))
    }

    @Test
    fun `the 2130 slot posts within grace even though the minute is past quiet after`() {
        val lateBoundary = 21 * 60 + 30 + NagSchedule.LATE_DELIVERY_GRACE_MINUTES
        val post = NagFire.at(listOf(choreDueToday()), emptySet(), monday, lateBoundary)
        assertEquals(NagLevel.LAST_CHANCE, post?.level)
        assertNull(NagFire.at(listOf(choreDueToday()), emptySet(), monday, lateBoundary + 1))
    }

    @Test
    fun `quiet hours have no slot so nothing posts`() {
        assertNull(NagFire.at(listOf(choreDueToday()), emptySet(), monday, 9 * 60))
        assertNull(NagFire.at(listOf(choreDueToday()), emptySet(), monday, 22 * 60))
    }

    @Test
    fun `weekday silent window has no slot so nothing posts`() {
        assertNull(NagFire.at(listOf(choreDueToday()), emptySet(), monday, 12 * 60))
    }

    @Test
    fun `nothing due posts nothing`() {
        assertNull(NagFire.at(listOf(choreNotDue()), emptySet(), monday, 18 * 60))
    }

    @Test
    fun `no chores at all posts nothing`() {
        assertNull(NagFire.at(emptyList(), emptySet(), monday, 18 * 60))
    }

    @Test
    fun `a completion today quiets the nag`() {
        assertNull(
            NagFire.at(listOf(choreDueToday()), setOf(monday), monday, 19 * 60),
        )
    }

    @Test
    fun `a completion yesterday does not quiet the nag`() {
        val post = NagFire.at(listOf(choreDueToday()), setOf(monday - 1), monday, 18 * 60)
        assertEquals(NagLevel.GENTLE, post?.level)
    }

    @Test
    fun `copy counts k more including discarded but due chores`() {
        val discarded = choreDueToday().copy(id = 3, name = "bins", creationOrder = 3)
            .discardedOn(monday)
        val post = NagFire.at(
            listOf(choreDueToday(), choreNotDue(), discarded),
            setOf(monday - 1, monday - 2),
            monday,
            20 * 60,
        )
        assertEquals(NagLevel.FREQUENT, post?.level)
        assertEquals("dishes + 1 more still waiting — 2-day streak on the line", post?.text)
    }

    @Test
    fun `zero streak swaps in the start again line`() {
        val post = NagFire.at(listOf(choreDueToday()), emptySet(), monday, 19 * 60)
        assertEquals("dishes is still waiting — start a new streak today", post?.text)
    }

    @Test
    fun `alarm trigger millis land on the slot minute`() {
        assertEquals(
            LocalDate.parse("2026-08-24").atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
            NagSchedule.triggerAtMillis(monday, 18 * 60, ZoneOffset.UTC),
        )
        assertEquals(
            LocalDate.parse("2026-08-24").atTime(21, 30).toInstant(ZoneOffset.UTC).toEpochMilli(),
            NagSchedule.triggerAtMillis(monday, 21 * 60 + 30, ZoneOffset.UTC),
        )
    }
}
