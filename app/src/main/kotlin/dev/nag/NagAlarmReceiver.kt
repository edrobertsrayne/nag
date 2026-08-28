package dev.nag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.nag.data.NagRepository
import dev.nag.domain.NagFire
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NagAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                fire(appContext, (appContext as NagApplication).repository)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * The fire path: post (or skip) for the moment the clock reads, then
     * always reschedule. Whether to post is one domain question —
     * [NagFire.at] — answered from the same state the deck uses; quieting is
     * re-derived here, never persisted, so it survives reboots for free.
     * Missed slots arrive on a minute with no slot and fall through to the
     * reschedule without posting.
     */
    private suspend fun fire(context: Context, repository: NagRepository) {
        val now = LocalDateTime.now()
        val post = NagFire.at(
            chores = repository.activeChoresNow(),
            completionDays = repository.completionDaysNow(),
            epochDay = now.toLocalDate().toEpochDay(),
            minuteOfDay = now.hour * 60 + now.minute,
        )
        if (post != null) {
            NagNotifier(context).post(post.level, post.text)
        }
        NagScheduler(context).scheduleNextFromNow()
    }
}
