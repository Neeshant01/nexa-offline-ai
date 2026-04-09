package com.nexa.offlineai.workers.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nexa.offlineai.core.common.AppError
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.ReminderTask
import com.nexa.offlineai.domain.repository.ReminderScheduler
import com.nexa.offlineai.workers.receiver.ReminderAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(reminderTask: ReminderTask): AppResult<Unit> = try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTask.scheduledAt,
            reminderPendingIntent(reminderTask.id, reminderTask.title, reminderTask.description),
        )
        AppResult.Success(Unit)
    } catch (exception: SecurityException) {
        AppResult.Error(AppError.Permission("Exact alarm permission is unavailable on this device."))
    } catch (exception: Exception) {
        AppResult.Error(AppError.Unknown(exception.message ?: "Unable to schedule reminder"))
    }

    override suspend fun cancel(reminderId: String): AppResult<Unit> = try {
        alarmManager.cancel(reminderPendingIntent(reminderId, "", ""))
        AppResult.Success(Unit)
    } catch (exception: Exception) {
        AppResult.Error(AppError.Unknown(exception.message ?: "Unable to cancel reminder"))
    }

    private fun reminderPendingIntent(reminderId: String, title: String, description: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_DESCRIPTION, description)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
