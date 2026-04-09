package com.nexa.offlineai.domain.model

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: Long,
    val state: MessageState = MessageState.COMPLETE,
)
