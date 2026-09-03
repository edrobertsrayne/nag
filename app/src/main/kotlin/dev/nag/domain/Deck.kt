package dev.nag.domain

object Deck {

    /** Shortest cadence first; ties break most-overdue, then most-recently-added. */
    val ordering: Comparator<Chore> = compareBy<Chore> { it.cadenceDays }
        .thenBy { it.nextDueDay }
        .thenByDescending { it.creationOrder }

    fun order(chores: List<Chore>, today: Long): List<Chore> = chores
        .filter { !it.archived && it.isDue(today) && !it.isHiddenOn(today) }
        .sortedWith(ordering)
}
