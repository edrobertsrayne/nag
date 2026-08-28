package dev.nag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the nag alarm whenever the system disturbs the schedule: boot
 * (BOOT_COMPLETED), an app update (MY_PACKAGE_REPLACED), a clock change
 * (TIME_SET), or a timezone change (TIMEZONE_CHANGED). It does nothing but
 * re-run the schedule-next function — the next slot is always computed in
 * the future, so a slot missed while the disturbance happened is never
 * posted late. No foreground service is started from here.
 */
class NagRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NagScheduler(context.applicationContext).scheduleNextFromNow()
    }
}
