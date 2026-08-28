package dev.nag.data

import dev.nag.domain.Chore
import kotlinx.coroutines.flow.Flow

interface NagRepository {

    val streak: Flow<Int>

    val activeChores: Flow<List<Chore>>

    val deck: Flow<List<Chore>>

    val discardsLeft: Flow<Int>

    suspend fun addChore(name: String, cadenceDays: Int)

    /**
     * Edits the chore's name and cadence. Next-due is untouched; the new
     * cadence applies from the chore's next completion.
     */
    suspend fun editChore(choreId: Long, name: String, cadenceDays: Int)

    /**
     * Archives the chore immediately, with no confirmation and no undo: it
     * leaves the queue and the deck, while its completion records persist and
     * keep counting toward the streak.
     */
    suspend fun archiveChore(choreId: Long)

    suspend fun completeChore(choreId: Long)

    /**
     * Puts the chore off for the rest of the day. Returns false when the
     * discard budget is spent; nothing is written in that case.
     */
    suspend fun discardChore(choreId: Long): Boolean
}
