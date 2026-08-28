package dev.nag.data

import dev.nag.domain.Chore
import dev.nag.domain.Deck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNagRepository(initialStreak: Int = 0) : NagRepository {

    var today: Long = 0

    private var nextCreationOrder = 1L
    private val chores = MutableStateFlow<List<Chore>>(emptyList())

    override val streak = MutableStateFlow(initialStreak)

    override val activeChores: Flow<List<Chore>> = chores

    override val deck: Flow<List<Chore>> = chores.map { Deck.order(it, today) }

    override suspend fun addChore(name: String, cadenceDays: Int) {
        chores.value = chores.value +
            Chore.create(
                name = name,
                cadenceDays = cadenceDays,
                today = today,
                creationOrder = nextCreationOrder++,
            )
    }
}
