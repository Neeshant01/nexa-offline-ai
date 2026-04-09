package com.nexa.offlineai.domain.repository

import com.nexa.offlineai.domain.model.ChatMessage
import com.nexa.offlineai.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeRecentConversations(limit: Int = 4): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun getConversation(conversationId: String): Conversation?
    suspend fun getMessages(conversationId: String): List<ChatMessage>
    suspend fun upsertConversation(conversation: Conversation)
    suspend fun upsertMessage(message: ChatMessage)
    suspend fun deleteMessage(messageId: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun conversationCount(): Int
    suspend fun clearAll()
}
