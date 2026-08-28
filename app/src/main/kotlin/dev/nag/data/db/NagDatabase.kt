package dev.nag.data.db

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [ChoreEntity::class, CompletionEntity::class, DiscardBudgetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NagDatabase : RoomDatabase() {

    abstract fun choreDao(): ChoreDao

    abstract fun completionDao(): CompletionDao

    abstract fun discardBudgetDao(): DiscardBudgetDao

    companion object {
        const val NAME = "nag.db"
    }
}
