package dev.nag.data

import dev.nag.data.db.ChoreDao
import dev.nag.data.db.ChoreEntity
import dev.nag.data.db.CompletionDao
import dev.nag.data.db.CompletionEntity
import dev.nag.data.db.DiscardBudgetDao
import dev.nag.data.db.DiscardBudgetEntity
import dev.nag.domain.Chore
import dev.nag.domain.Deck
import dev.nag.domain.DiscardBudget
import dev.nag.domain.Discards
import dev.nag.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomNagRepository(
    private val choreDao: ChoreDao,
    private val completionDao: CompletionDao,
    private val discardBudgetDao: DiscardBudgetDao,
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

    override val discardsLeft: Flow<Int> =
        discardBudgetDao.observeForDay(today().toEpochDay()).map { row ->
            Discards.left(row?.toDiscardBudget(), today().toEpochDay())
        }

    override suspend fun addChore(name: String, cadenceDays: Int) {
        choreDao.insertChoreWithCreationOrderFromId(
            Chore.create(name = name, cadenceDays = cadenceDays, today = today().toEpochDay()).toEntity(),
        )
    }

    override suspend fun editChore(choreId: Long, name: String, cadenceDays: Int) {
        choreDao.setChoreDetails(id = choreId, name = name, cadenceDays = cadenceDays)
    }

    override suspend fun archiveChore(choreId: Long) {
        choreDao.setArchived(choreId)
    }

    override suspend fun completeChore(choreId: Long) {
        val completionDay = today().toEpochDay()
        val chore = choreDao.getChore(choreId) ?: return
        choreDao.recordCompletion(
            completion = CompletionEntity(choreId = choreId, completionDay = completionDay),
            nextDueDay = chore.toChore().completedOn(completionDay).nextDueDay,
        )
    }

    override suspend fun discardChore(choreId: Long): Boolean {
        val today = today().toEpochDay()
        choreDao.getChore(choreId) ?: return false
        val stored = discardBudgetDao.getForDay(today)?.toDiscardBudget()
        if (Discards.left(stored, today) == 0) return false
        val budget = Discards.record(stored, today)
        choreDao.recordDiscard(
            choreId = choreId,
            day = today,
            budget = DiscardBudgetEntity(day = budget.day, usedCount = budget.usedCount),
        )
        return true
    }
}

private fun ChoreEntity.toChore() = Chore(
    id = id,
    name = name,
    cadenceDays = cadenceDays,
    nextDueDay = nextDueDay,
    creationOrder = creationOrder,
    lastDiscardedDay = lastDiscardedDay,
    archived = archived,
)

private fun DiscardBudgetEntity.toDiscardBudget() = DiscardBudget(
    day = day,
    usedCount = usedCount,
)

private fun Chore.toEntity() = ChoreEntity(
    id = id,
    name = name,
    cadenceDays = cadenceDays,
    nextDueDay = nextDueDay,
    creationOrder = creationOrder,
)
