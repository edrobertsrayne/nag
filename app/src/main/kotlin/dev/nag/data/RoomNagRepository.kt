package dev.nag.data

import dev.nag.data.db.CompletionDao
import dev.nag.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomNagRepository(
    private val completionDao: CompletionDao,
    private val today: () -> LocalDate = LocalDate::now,
) : NagRepository {

    override val streak: Flow<Int> =
        completionDao.observeCompletionDays().map { days ->
            Streak.of(days.toSet(), today().toEpochDay())
        }
}
