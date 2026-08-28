package dev.nag.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreDao {

    @Insert
    suspend fun insert(chore: ChoreEntity): Long

    @Insert
    suspend fun insertCompletion(completion: CompletionEntity): Long

    @Query("UPDATE chores SET creation_order = :creationOrder WHERE id = :id")
    suspend fun setCreationOrder(id: Long, creationOrder: Long)

    @Query("UPDATE chores SET next_due_day = :nextDueDay WHERE id = :id")
    suspend fun setNextDueDay(id: Long, nextDueDay: Long)

    @Query("UPDATE chores SET name = :name, cadence_days = :cadenceDays WHERE id = :id")
    suspend fun setChoreDetails(id: Long, name: String, cadenceDays: Int)

    @Query("UPDATE chores SET archived = 1 WHERE id = :id")
    suspend fun setArchived(id: Long)

    /**
     * Inserts a new chore and sets its creation order to the row id, so deck
     * ties always break oldest-added first.
     */
    @Transaction
    suspend fun insertChoreWithCreationOrderFromId(chore: ChoreEntity) {
        val id = insert(chore)
        setCreationOrder(id, id)
    }

    @Transaction
    suspend fun recordCompletion(completion: CompletionEntity, nextDueDay: Long) {
        insertCompletion(completion)
        setNextDueDay(completion.choreId, nextDueDay)
    }

    @Upsert
    suspend fun upsertDiscardBudget(budget: DiscardBudgetEntity)

    @Query("UPDATE chores SET last_discarded_day = :day WHERE id = :id")
    suspend fun setLastDiscardedDay(id: Long, day: Long)

    /**
     * Hides the chore for the rest of [day] and spends one discard from the
     * day-keyed budget row, atomically.
     */
    @Transaction
    suspend fun recordDiscard(choreId: Long, day: Long, budget: DiscardBudgetEntity) {
        setLastDiscardedDay(choreId, day)
        upsertDiscardBudget(budget)
    }

    @Query("SELECT * FROM chores WHERE archived = 0 ORDER BY creation_order")
    fun observeActiveChores(): Flow<List<ChoreEntity>>

    @Query("SELECT * FROM chores WHERE id = :id")
    suspend fun getChore(id: Long): ChoreEntity?
}
