package dev.nag

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.nag.domain.NagSchedule
import dev.nag.domain.ScheduledNag
import java.time.LocalDateTime
import java.time.ZoneId

class NagScheduler(private val context: Context) {

    /**
     * Arms the alarm for the next future slot from the slot table. Called on
     * app start and after every alarm fire, so the schedule is always exactly
     * one alarm deep: post (or skip), then reschedule.
     */
    fun scheduleNextFromNow() {
        val now = LocalDateTime.now()
        val epochDay = now.toLocalDate().toEpochDay()
        val minuteOfDay = now.hour * 60 + now.minute
        schedule(NagSchedule.nextSlot(epochDay, minuteOfDay))
    }

    private fun schedule(slot: ScheduledNag) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = NagSchedule.triggerAtMillis(
            slot.epochDay,
            slot.slot.minuteOfDay,
            ZoneId.systemDefault(),
        )
        val pendingIntent = alarmPendingIntent()
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            // Exact alarms are unavailable (not expected under
            // USE_EXACT_ALARM): fall back to an inexact window rather than
            // crash. The nag may drift inside the window; the fire path
            // posts it while it lands within the slot's late-delivery grace
            // and skips anything later.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                Constants.NAG_FALLBACK_WINDOW_MILLIS,
                pendingIntent,
            )
        }
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NagAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
