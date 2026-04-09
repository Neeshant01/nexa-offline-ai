package com.nexa.offlineai.workers.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationPublisher {
    fun showReminder(context: Context, reminderId: String, title: String, description: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
            .setContentTitle(title)
            .setContentText(description.ifBlank { "Reminder from Nexa Offline AI" })
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(reminderId.hashCode(), notification)
    }

    fun showDailySummary(context: Context, summary: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, NotificationChannels.DAILY_SUMMARY)
            .setContentTitle("Nexa daily summary")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        manager.notify(88_221, notification)
    }
}
