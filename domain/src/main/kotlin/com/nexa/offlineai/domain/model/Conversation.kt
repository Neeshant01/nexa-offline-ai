package com.nexa.offlineai.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val lastMessagePreview: String = "",
)
