package com.nexa.offlineai.data.repository

import com.nexa.offlineai.data.local.dao.ChatMessageDao
import com.nexa.offlineai.data.local.dao.ConversationDao
import com.nexa.offlineai.data.local.mappers.asDomain
import com.nexa.offlineai.data.local.mappers.asEntity
import com.nexa.offlineai.domain.model.ChatMessage
import com.nexa.offlineai.domain.model.Conversation
import com.nexa.offlineai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
) : ChatRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeConversations().map { entities -> entities.map { it.asDomain() } }

    override fun observeRecentConversations(limit: Int): Flow<List<Conversation>> =
        conversationDao.observeRecentConversations(limit).map { entities -> entities.map { it.asDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        chatMessageDao.observeMessages(conversationId).map { entities -> entities.map { it.asDomain() } }

    override suspend fun getConversation(conversationId: String): Conversation? =
        conversationDao.getConversation(conversationId)?.asDomain()

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        chatMessageDao.getMessages(conversationId).map { it.asDomain() }

    override suspend fun upsertConversation(conversation: Conversation) {
        conversationDao.upsertConversation(conversation.asEntity())
    }

    override suspend fun upsertMessage(message: ChatMessage) {
        chatMessageDao.upsertMessage(message.asEntity())
    }

    override suspend fun deleteMessage(messageId: String) {
        chatMessageDao.deleteMessage(messageId)
    }

    override suspend fun deleteConversation(conversationId: String) {
        chatMessageDao.deleteByConversation(conversationId)
        conversationDao.deleteConversation(conversationId)
    }

    override suspend fun conversationCount(): Int = conversationDao.count()

    override suspend fun clearAll() {
        conversationDao.clear()
    }
}
