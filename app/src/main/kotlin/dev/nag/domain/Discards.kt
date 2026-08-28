package dev.nag.domain

/**
 * The global daily discard budget: at most [DAILY_BUDGET] cards put off per day,
 * across all chores. The budget resets whenever the stored day is not today
 * (local midnight).
 */
data class DiscardBudget(
    val day: Long,
    val usedCount: Int,
)

object Discards {

    const val DAILY_BUDGET = 2

    fun left(budget: DiscardBudget?, today: Long): Int = when {
        budget == null || budget.day != today -> DAILY_BUDGET
        else -> (DAILY_BUDGET - budget.usedCount).coerceAtLeast(0)
    }

    fun record(budget: DiscardBudget?, today: Long): DiscardBudget = DiscardBudget(
        day = today,
        usedCount = if (budget != null && budget.day == today) budget.usedCount + 1 else 1,
    )
}
