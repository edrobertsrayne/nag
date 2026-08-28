package dev.nag.domain

object Deck {

    /** Most-overdue first; ties break oldest-added. */
    val ordering: Comparator<Chore> = compareBy<Chore> { it.nextDueDay }.thenBy { it.creationOrder }

    fun order(chores: List<Chore>, today: Long): List<Chore> = chores
        .filter { !it.archived && it.isDue(today) && !it.isHiddenOn(today) }
        .sortedWith(ordering)
}
