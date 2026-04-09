package com.nexa.offlineai.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nexa.offlineai.data.local.entity.ReminderTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderTaskDao {
    @Query("SELECT * FROM reminders ORDER BY scheduledAt ASC")
    fun observeReminders(): Flow<List<ReminderTaskEntity>>

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminder(reminderId: String): ReminderTaskEntity?

    @Upsert
    suspend fun upsertReminder(reminderTask: ReminderTaskEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: String)

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun count(): Int

    @Query("DELETE FROM reminders")
    suspend fun clear()
}
