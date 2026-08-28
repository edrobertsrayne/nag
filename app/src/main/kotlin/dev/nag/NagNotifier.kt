package dev.nag

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.nag.domain.NagLevel

class NagNotifier(private val context: Context) {

    /**
     * Posts one nag under the single fixed notification id, so each slot
     * replaces the previous. Standard template only; tap opens the deck; no
     * action buttons. Every post passes the are-notifications-enabled guard
     * here — the single choke point — because the user can revoke the
     * permission at any time.
     */
    fun post(level: NagLevel, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        val notification = Notification.Builder(context, level.channelId())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.nag_notification_title))
            .setContentText(text)
            .setContentIntent(deckIntent())
            .build()
        manager.notify(Constants.NOTIFICATION_ID, notification)
    }

    private fun deckIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

private fun NagLevel.channelId(): String = when (this) {
    NagLevel.GENTLE -> Constants.CHANNEL_GENTLE_ID
    NagLevel.FREQUENT -> Constants.CHANNEL_FREQUENT_ID
    NagLevel.LAST_CHANCE -> Constants.CHANNEL_LAST_CHANCE_ID
}
