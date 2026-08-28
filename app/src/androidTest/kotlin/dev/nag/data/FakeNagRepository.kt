package dev.nag.data

import dev.nag.domain.Chore
import dev.nag.domain.Deck
import dev.nag.domain.DiscardBudget
import dev.nag.domain.Discards
import dev.nag.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNagRepository(initialCompletionDays: Set<Long> = emptySet()) : NagRepository {

    var today: Long = 0

    private var nextId = 1L
    private val chores = MutableStateFlow<List<Chore>>(emptyList())
    private val completionDays = MutableStateFlow(initialCompletionDays)
    private val discardBudget = MutableStateFlow<DiscardBudget?>(null)

    override val streak = completionDays.map { Streak.of(it, today) }

    override val activeChores: Flow<List<Chore>> =
        chores.map { list -> list.filter { !it.archived } }

    override val deck: Flow<List<Chore>> = chores.map { Deck.order(it, today) }

    override val discardsLeft = discardBudget.map { Discards.left(it, today) }

    override suspend fun addChore(name: String, cadenceDays: Int) {
        val id = nextId++
        chores.value = chores.value + Chore(
            id = id,
            name = name,
            cadenceDays = cadenceDays,
            nextDueDay = today,
            creationOrder = id,
        )
    }

    override suspend fun editChore(choreId: Long, name: String, cadenceDays: Int) {
        chores.value = chores.value.map { chore ->
            if (chore.id == choreId) chore.editedTo(name = name, cadenceDays = cadenceDays) else chore
        }
    }

    override suspend fun archiveChore(choreId: Long) {
        chores.value = chores.value.map { chore ->
            if (chore.id == choreId) chore.archivedOn() else chore
        }
    }

    override suspend fun completeChore(choreId: Long) {
        completionDays.value = completionDays.value + today
        chores.value = chores.value.map { chore ->
            if (chore.id == choreId) chore.completedOn(today) else chore
        }
    }

    override suspend fun discardChore(choreId: Long): Boolean {
        if (Discards.left(discardBudget.value, today) == 0) return false
        discardBudget.value = Discards.record(discardBudget.value, today)
        chores.value = chores.value.map { chore ->
            if (chore.id == choreId) chore.discardedOn(today) else chore
        }
        return true
    }
}
