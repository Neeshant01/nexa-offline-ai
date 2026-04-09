package com.nexa.offlineai.domain.usecase

import com.nexa.offlineai.core.common.AppError
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.core.common.IdFactory
import com.nexa.offlineai.domain.model.AiConversationContext
import com.nexa.offlineai.domain.model.AiConversationTurn
import com.nexa.offlineai.domain.model.ChatMessage
import com.nexa.offlineai.domain.model.Conversation
import com.nexa.offlineai.domain.model.MessageRole
import com.nexa.offlineai.domain.model.MessageState
import com.nexa.offlineai.domain.repository.AssistantGateway
import com.nexa.offlineai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(): Flow<List<Conversation>> = chatRepository.observeConversations()
}

class ObserveRecentConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(limit: Int = 4): Flow<List<Conversation>> = chatRepository.observeRecentConversations(limit)
}

class ObserveMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(conversationId: String): Flow<List<ChatMessage>> = chatRepository.observeMessages(conversationId)
}

class EnsureConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(conversationId: String? = null, suggestedTitle: String = "New conversation"): Conversation {
        val existing = conversationId?.let { id -> chatRepository.getConversation(id) }
        if (existing != null) return existing
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = IdFactory.newId(),
            title = suggestedTitle,
            createdAt = now,
            updatedAt = now,
        )
        chatRepository.upsertConversation(conversation)
        return conversation
    }
}

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val ensureConversationUseCase: EnsureConversationUseCase,
    private val assistantGateway: AssistantGateway,
) {
    suspend operator fun invoke(
        conversationId: String?,
        userText: String,
        onPartial: suspend (String) -> Unit = {},
    ): AppResult<String> {
        if (userText.isBlank()) {
            return AppResult.Error(AppError.Validation("Message cannot be empty"))
        }

        val conversation = ensureConversationUseCase(conversationId)
        val now = System.currentTimeMillis()
        val userMessage = ChatMessage(
            id = IdFactory.newId(),
            conversationId = conversation.id,
            role = MessageRole.USER,
            content = userText.trim(),
            createdAt = now,
        )
        val assistantMessage = ChatMessage(
            id = IdFactory.newId(),
            conversationId = conversation.id,
            role = MessageRole.ASSISTANT,
            content = "",
            createdAt = now + 1,
            state = MessageState.STREAMING,
        )
        chatRepository.upsertMessage(userMessage)
        chatRepository.upsertMessage(assistantMessage)

        val context = AiConversationContext(
            conversationId = conversation.id,
            turns = chatRepository.getMessages(conversation.id)
                .filter { it.id != assistantMessage.id }
                .map { AiConversationTurn(role = it.role, content = it.content) },
        )

        val response = assistantGateway.sendMessage(userText.trim(), context) { partial ->
            onPartial(partial)
            chatRepository.upsertMessage(
                assistantMessage.copy(
                    content = partial,
                    state = MessageState.STREAMING,
                ),
            )
        }

        return when (response) {
            is AppResult.Success -> {
                val content = response.value.text.trim()
                chatRepository.upsertMessage(assistantMessage.copy(content = content, state = MessageState.COMPLETE))
                val title = if (conversation.title == "New conversation") {
                    when (val result = assistantGateway.generateConversationTitle(listOf(userText, content))) {
                        is AppResult.Success -> result.value
                        is AppResult.Error -> conversation.title
                    }
                } else {
                    conversation.title
                }
                chatRepository.upsertConversation(
                    conversation.copy(
                        title = title,
                        updatedAt = System.currentTimeMillis(),
                        lastMessagePreview = content.take(120),
                    ),
                )
                AppResult.Success(conversation.id)
            }

            is AppResult.Error -> {
                chatRepository.upsertMessage(
                    assistantMessage.copy(
                        content = response.error.message,
                        state = MessageState.FAILED,
                    ),
                )
                AppResult.Error(response.error)
            }
        }
    }
}

class DeleteMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(messageId: String) = chatRepository.deleteMessage(messageId)
}

class DeleteConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(conversationId: String) = chatRepository.deleteConversation(conversationId)
}

class SummarizeConversationUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
) {
    suspend operator fun invoke(messages: List<ChatMessage>): AppResult<String> =
        assistantGateway.summarizeConversation(messages.joinToString("\n") { "${it.role.name}: ${it.content}" })
}
