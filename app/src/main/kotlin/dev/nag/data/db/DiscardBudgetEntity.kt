package dev.nag.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "discard_budget")
data class DiscardBudgetEntity(
    @PrimaryKey @ColumnInfo(name = "day") val day: Long,
    @ColumnInfo(name = "used_count") val usedCount: Int,
)
