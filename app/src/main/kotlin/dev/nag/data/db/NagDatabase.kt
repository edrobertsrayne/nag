package dev.nag.data.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [ChoreEntity::class, CompletionEntity::class, DiscardBudgetEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class NagDatabase : RoomDatabase() {

    abstract fun choreDao(): ChoreDao

    abstract fun completionDao(): CompletionDao

    abstract fun discardBudgetDao(): DiscardBudgetDao

    companion object {
        const val NAME = "nag.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE chores ADD COLUMN last_discarded_day INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
