package dev.nag.data

import dev.nag.domain.Chore
import dev.nag.domain.Deck
import dev.nag.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNagRepository(initialCompletionDays: Set<Long> = emptySet()) : NagRepository {

    var today: Long = 0

    private var nextId = 1L
    private val chores = MutableStateFlow<List<Chore>>(emptyList())
    private val completionDays = MutableStateFlow(initialCompletionDays)

    override val streak = completionDays.map { Streak.of(it, today) }

    override val activeChores: Flow<List<Chore>> = chores

    override val deck: Flow<List<Chore>> = chores.map { Deck.order(it, today) }

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

    override suspend fun completeChore(choreId: Long) {
        completionDays.value = completionDays.value + today
        chores.value = chores.value.map { chore ->
            if (chore.id == choreId) chore.completedOn(today) else chore
        }
    }
}
