package com.nexa.offlineai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val scheduledAt: Long,
    val createdAt: Long,
    val status: String,
    val sourceText: String,
    val confidence: Float,
)
