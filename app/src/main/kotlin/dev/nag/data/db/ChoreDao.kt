package dev.nag.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreDao {

    @Insert
    suspend fun insert(chore: ChoreEntity): Long

    @Query("SELECT * FROM chores WHERE archived = 0 ORDER BY creation_order")
    fun observeActiveChores(): Flow<List<ChoreEntity>>
}
