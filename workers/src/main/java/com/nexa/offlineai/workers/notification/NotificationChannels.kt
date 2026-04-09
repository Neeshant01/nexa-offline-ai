package com.nexa.offlineai.workers.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val REMINDERS = "nexa_reminders"
    const val DAILY_SUMMARY = "nexa_daily_summary"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Local reminders and task notifications"
            },
            NotificationChannel(DAILY_SUMMARY, "Daily summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily private summary generated on device"
            },
        )
        notificationManager.createNotificationChannels(channels)
    }
}
