package com.nexa.offlineai.domain.model

data class ReminderTask(
    val id: String,
    val title: String,
    val description: String,
    val scheduledAt: Long,
    val createdAt: Long,
    val status: ReminderStatus,
    val sourceText: String = "",
    val confidence: Float = 0f,
)
