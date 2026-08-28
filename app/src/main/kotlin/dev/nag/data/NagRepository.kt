package dev.nag.data

import dev.nag.domain.Chore
import kotlinx.coroutines.flow.Flow

interface NagRepository {

    val streak: Flow<Int>

    val activeChores: Flow<List<Chore>>

    val deck: Flow<List<Chore>>

    suspend fun addChore(name: String, cadenceDays: Int)
}
