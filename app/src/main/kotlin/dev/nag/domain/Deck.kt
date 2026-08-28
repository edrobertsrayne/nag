package dev.nag.domain

object Deck {

    fun order(chores: List<Chore>, today: Long): List<Chore> = chores
        .filter { it.isDue(today) }
        .sortedWith(compareBy<Chore> { it.nextDueDay }.thenBy { it.creationOrder })
}
