package com.nexa.offlineai.domain.repository

import com.nexa.offlineai.domain.model.ReminderTask
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminders(): Flow<List<ReminderTask>>
    suspend fun getReminder(reminderId: String): ReminderTask?
    suspend fun upsertReminder(reminderTask: ReminderTask)
    suspend fun deleteReminder(reminderId: String)
    suspend fun reminderCount(): Int
    suspend fun clearAll()
}
