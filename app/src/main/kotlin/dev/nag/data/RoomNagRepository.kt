package dev.nag.data

import dev.nag.data.db.ChoreDao
import dev.nag.data.db.ChoreEntity
import dev.nag.data.db.CompletionDao
import dev.nag.domain.Chore
import dev.nag.domain.Deck
import dev.nag.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomNagRepository(
    private val choreDao: ChoreDao,
    private val completionDao: CompletionDao,
    private val today: () -> LocalDate = LocalDate::now,
) : NagRepository {

    override val streak: Flow<Int> =
        completionDao.observeCompletionDays().map { days ->
            Streak.of(days.toSet(), today().toEpochDay())
        }

    override val activeChores: Flow<List<Chore>> =
        choreDao.observeActiveChores().map { chores -> chores.map { it.toChore() } }

    override val deck: Flow<List<Chore>> =
        activeChores.map { Deck.order(it, today().toEpochDay()) }

    override suspend fun addChore(name: String, cadenceDays: Int) {
        choreDao.insertChoreWithCreationOrderFromId(
            Chore.create(name = name, cadenceDays = cadenceDays, today = today().toEpochDay()).toEntity(),
        )
    }
}

private fun ChoreEntity.toChore() = Chore(
    id = id,
    name = name,
    cadenceDays = cadenceDays,
    nextDueDay = nextDueDay,
    creationOrder = creationOrder,
)

private fun Chore.toEntity() = ChoreEntity(
    id = id,
    name = name,
    cadenceDays = cadenceDays,
    nextDueDay = nextDueDay,
    creationOrder = creationOrder,
)
