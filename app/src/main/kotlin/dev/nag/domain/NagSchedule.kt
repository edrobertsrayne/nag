package dev.nag.domain

/**
 * Everything about when nags may fire: the fixed slot table with escalation
 * levels, the global quiet hours, the weekday silent window, and the
 * next-future-slot question the scheduler asks. Times are minutes since local
 * midnight; days are epoch days.
 */
enum class NagLevel {
    GENTLE,
    FREQUENT,
    LAST_CHANCE,
}

data class NagSlot(val minuteOfDay: Int, val level: NagLevel)

/** A slot armed on a specific day: the answer to "which nag fires next?". */
data class ScheduledNag(val epochDay: Long, val slot: NagSlot)

object NagSchedule {

    // Global quiet: nothing posted before 09:00 or after 21:30, any day
    // (the 21:30 slot itself posts).
    const val QUIET_BEFORE_MINUTE = 9 * 60
    const val QUIET_AFTER_MINUTE = 21 * 60 + 30

    // Weekday silent window: nothing posted at all 08:00-17:59 Mon-Fri.
    const val WEEKDAY_SILENT_START_MINUTE = 8 * 60
    const val WEEKDAY_SILENT_END_MINUTE = 17 * 60 + 59

    val WEEKDAY_SLOTS: List<NagSlot> = listOf(
        NagSlot(18 * 60, NagLevel.GENTLE),
        NagSlot(19 * 60, NagLevel.FREQUENT),
        NagSlot(20 * 60, NagLevel.FREQUENT),
        NagSlot(21 * 60, NagLevel.LAST_CHANCE),
        NagSlot(21 * 60 + 30, NagLevel.LAST_CHANCE),
    )

    val WEEKEND_SLOTS: List<NagSlot> = listOf(
        NagSlot(10 * 60, NagLevel.GENTLE),
        NagSlot(12 * 60, NagLevel.GENTLE),
        NagSlot(14 * 60, NagLevel.FREQUENT),
        NagSlot(16 * 60, NagLevel.FREQUENT),
        NagSlot(18 * 60, NagLevel.FREQUENT),
        NagSlot(20 * 60, NagLevel.FREQUENT),
        NagSlot(21 * 60, NagLevel.LAST_CHANCE),
        NagSlot(21 * 60 + 30, NagLevel.LAST_CHANCE),
    )

    /** Epoch day 0 (1970-01-01) was a Thursday; indices 5 and 6 are Sat and Sun. */
    fun isWeekend(epochDay: Long): Boolean = Math.floorMod(epochDay + 3, 7) >= 5

    fun slotsFor(epochDay: Long): List<NagSlot> =
        if (isWeekend(epochDay)) WEEKEND_SLOTS else WEEKDAY_SLOTS

    /** The slot firing at exactly this minute, or null when no slot fires. */
    fun slotAt(epochDay: Long, minuteOfDay: Int): NagSlot? =
        slotsFor(epochDay).firstOrNull { it.minuteOfDay == minuteOfDay }

    fun isQuiet(minuteOfDay: Int): Boolean =
        minuteOfDay < QUIET_BEFORE_MINUTE || minuteOfDay > QUIET_AFTER_MINUTE

    fun isSilent(epochDay: Long, minuteOfDay: Int): Boolean =
        !isWeekend(epochDay) &&
            minuteOfDay >= WEEKDAY_SILENT_START_MINUTE &&
            minuteOfDay <= WEEKDAY_SILENT_END_MINUTE

    /**
     * The next slot strictly after this minute, rolling over to the next day's
     * table when today's is exhausted. Always answers: every day has a table.
     */
    fun nextSlot(epochDay: Long, minuteOfDay: Int): ScheduledNag {
        val laterToday = slotsFor(epochDay).firstOrNull { it.minuteOfDay > minuteOfDay }
        return laterToday?.let { ScheduledNag(epochDay, it) }
            ?: ScheduledNag(epochDay + 1, slotsFor(epochDay + 1).first())
    }
}
