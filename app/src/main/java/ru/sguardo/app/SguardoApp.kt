package ru.sguardo.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SguardoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("messages", getString(R.string.channel_messages), NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel("reminders", getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT))
        }
    }
}
