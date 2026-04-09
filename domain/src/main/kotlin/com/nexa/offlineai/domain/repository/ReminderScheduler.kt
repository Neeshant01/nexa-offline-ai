package com.nexa.offlineai.domain.repository

import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.ReminderTask

interface ReminderScheduler {
    suspend fun schedule(reminderTask: ReminderTask): AppResult<Unit>
    suspend fun cancel(reminderId: String): AppResult<Unit>
}
