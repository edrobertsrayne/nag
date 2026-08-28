package dev.nag.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "cadence_days") val cadenceDays: Int,
    @ColumnInfo(name = "next_due_day") val nextDueDay: Long,
    @ColumnInfo(name = "creation_order") val creationOrder: Long,
    @ColumnInfo(name = "archived") val archived: Boolean = false,
)
