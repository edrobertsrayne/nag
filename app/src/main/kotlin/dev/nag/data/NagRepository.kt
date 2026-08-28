package dev.nag.data

import dev.nag.domain.Chore
import kotlinx.coroutines.flow.Flow

interface NagRepository {

    val streak: Flow<Int>

    val activeChores: Flow<List<Chore>>

    val deck: Flow<List<Chore>>

    val discardsLeft: Flow<Int>

    suspend fun addChore(name: String, cadenceDays: Int)

    suspend fun completeChore(choreId: Long)

    /**
     * Puts the chore off for the rest of the day. Returns false when the
     * discard budget is spent; nothing is written in that case.
     */
    suspend fun discardChore(choreId: Long): Boolean
}
