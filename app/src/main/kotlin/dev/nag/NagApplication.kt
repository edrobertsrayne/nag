package dev.nag

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.room3.Room
import dev.nag.data.NagRepository
import dev.nag.data.RoomNagRepository
import dev.nag.data.db.NagDatabase

class NagApplication : Application() {

    lateinit var repository: NagRepository
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        val database = Room.databaseBuilder(this, NagDatabase::class.java, NagDatabase.NAME).build()
        repository = RoomNagRepository(database.completionDao())
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    Constants.CHANNEL_GENTLE_ID,
                    Constants.CHANNEL_GENTLE_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ),
                NotificationChannel(
                    Constants.CHANNEL_FREQUENT_ID,
                    Constants.CHANNEL_FREQUENT_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    Constants.CHANNEL_LAST_CHANCE_ID,
                    Constants.CHANNEL_LAST_CHANCE_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            ),
        )
    }
}
