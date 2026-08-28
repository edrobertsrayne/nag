package dev.nag.domain

object Deck {

    fun order(chores: List<Chore>, today: Long): List<Chore> = chores
        .filter { !it.archived && it.isDue(today) && !it.isHiddenOn(today) }
        .sortedWith(compareBy<Chore> { it.nextDueDay }.thenBy { it.creationOrder })
}
