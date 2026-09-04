package dev.nag.domain

object Deck {

    fun order(chores: List<Chore>, today: Long): List<Chore> = chores
        .filter { !it.archived && it.isDue(today) && !it.isHiddenOn(today) }
        .sortedWith(orderingOn(today))

    /** Highest overdue-ratio first; ties break shortest-cadence, then most-recently-added. */
    fun orderingOn(today: Long): Comparator<Chore> = compareByDescending<Chore> { it.overdueRatio(today) }
        .thenBy { it.cadenceDays }
        .thenByDescending { it.creationOrder }
}
