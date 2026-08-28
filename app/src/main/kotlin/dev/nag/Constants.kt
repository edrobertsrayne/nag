package dev.nag

import androidx.compose.animation.core.Spring

enum class NagLevel {
    GENTLE,
    FREQUENT,
    LAST_CHANCE,
}

data class NagSlot(val minuteOfDay: Int, val level: NagLevel)

object Constants {
    const val MIN_SDK = 33
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37

    const val CADENCE_MIN_DAYS = 1

    const val DISCARD_BUDGET_PER_DAY = 2

    const val SWIPE_COMMIT_FRACTION = 0.4f
    const val SWIPE_FLING_VELOCITY_DP_PER_S = 800f
    const val SWIPE_SPRING_DAMPING_RATIO = Spring.DampingRatioMediumBouncy
    const val SWIPE_SPRING_STIFFNESS = Spring.StiffnessMedium

    const val QUIET_BEFORE_MINUTE = 9 * 60
    const val QUIET_AFTER_MINUTE = 21 * 60 + 30
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

    const val NOTIFICATION_ID = 1

    const val CHANNEL_GENTLE_ID = "nag_gentle"
    const val CHANNEL_FREQUENT_ID = "nag_frequent"
    const val CHANNEL_LAST_CHANCE_ID = "nag_last_chance"
    const val CHANNEL_GENTLE_NAME = "Nag: gentle"
    const val CHANNEL_FREQUENT_NAME = "Nag: frequent"
    const val CHANNEL_LAST_CHANCE_NAME = "Nag: last-chance"
}
