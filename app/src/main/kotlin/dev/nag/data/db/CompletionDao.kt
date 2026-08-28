package dev.nag.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Insert
    suspend fun insert(completion: CompletionEntity): Long

    @Query("SELECT DISTINCT completion_day FROM completions")
    fun observeCompletionDays(): Flow<List<Long>>
}
