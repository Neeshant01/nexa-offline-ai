package com.nexa.offlineai.data.repository

import com.nexa.offlineai.data.local.dao.ReminderTaskDao
import com.nexa.offlineai.data.local.mappers.asDomain
import com.nexa.offlineai.data.local.mappers.asEntity
import com.nexa.offlineai.domain.model.ReminderTask
import com.nexa.offlineai.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderTaskDao: ReminderTaskDao,
) : ReminderRepository {
    override fun observeReminders(): Flow<List<ReminderTask>> =
        reminderTaskDao.observeReminders().map { entities -> entities.map { it.asDomain() } }

    override suspend fun getReminder(reminderId: String): ReminderTask? =
        reminderTaskDao.getReminder(reminderId)?.asDomain()

    override suspend fun upsertReminder(reminderTask: ReminderTask) {
        reminderTaskDao.upsertReminder(reminderTask.asEntity())
    }

    override suspend fun deleteReminder(reminderId: String) {
        reminderTaskDao.deleteReminder(reminderId)
    }

    override suspend fun reminderCount(): Int = reminderTaskDao.count()

    override suspend fun clearAll() {
        reminderTaskDao.clear()
    }
}
