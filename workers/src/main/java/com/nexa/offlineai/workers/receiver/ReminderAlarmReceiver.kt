package com.nexa.offlineai.workers.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexa.offlineai.workers.notification.NotificationPublisher

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationPublisher.showReminder(
            context = context,
            reminderId = intent.getStringExtra(EXTRA_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reminder" },
            description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
        )
    }

    companion object {
        const val EXTRA_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_DESCRIPTION = "extra_reminder_description"
    }
}
