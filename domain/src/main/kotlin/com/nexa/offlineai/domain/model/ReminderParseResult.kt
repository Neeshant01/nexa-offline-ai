package com.nexa.offlineai.domain.model

data class ReminderParseResult(
    val title: String,
    val description: String = "",
    val scheduledAt: Long?,
    val confidence: Float,
    val matched: Boolean,
    val source: String,
)
