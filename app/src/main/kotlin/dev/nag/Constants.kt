package dev.nag

import androidx.compose.animation.core.Spring
import dev.nag.domain.Discards

object Constants {
    const val MIN_SDK = 33
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37

    const val CADENCE_MIN_DAYS = 1

    const val DISCARD_BUDGET_PER_DAY = Discards.DAILY_BUDGET

    const val SWIPE_COMMIT_FRACTION = 0.4f
    const val SWIPE_FLING_VELOCITY_DP_PER_S = 800f
    const val SWIPE_MAX_TILT_DEGREES = 12f
    const val SWIPE_SPRING_DAMPING_RATIO = Spring.DampingRatioMediumBouncy
    const val SWIPE_SPRING_STIFFNESS = Spring.StiffnessMedium

    const val NOTIFICATION_ID = 1

    const val NAG_FALLBACK_WINDOW_MILLIS = 10 * 60 * 1000L

    const val CHANNEL_GENTLE_ID = "nag_gentle"
    const val CHANNEL_FREQUENT_ID = "nag_frequent"
    const val CHANNEL_LAST_CHANCE_ID = "nag_last_chance"
    const val CHANNEL_GENTLE_NAME = "Nag: gentle"
    const val CHANNEL_FREQUENT_NAME = "Nag: frequent"
    const val CHANNEL_LAST_CHANCE_NAME = "Nag: last-chance"
}
