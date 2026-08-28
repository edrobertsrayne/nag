package dev.nag.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscardBudgetDao {

    @Upsert
    suspend fun upsert(row: DiscardBudgetEntity)

    @Query("SELECT * FROM discard_budget WHERE day = :day")
    fun observeForDay(day: Long): Flow<DiscardBudgetEntity?>
}
