package com.nexa.offlineai.domain.repository

import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.AiConversationContext
import com.nexa.offlineai.domain.model.AiResponse
import com.nexa.offlineai.domain.model.ModelStatus
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.model.ReminderParseResult
import kotlinx.coroutines.flow.Flow

interface AssistantEngine {
    val providerType: ProviderType
    val modelStatus: Flow<ModelStatus>

    suspend fun warmUp(): AppResult<Unit>
    suspend fun cancelGeneration()
    suspend fun sendMessage(
        userMessage: String,
        context: AiConversationContext,
        onPartial: suspend (String) -> Unit = {},
    ): AppResult<AiResponse>

    suspend fun summarizeConversation(conversation: String): AppResult<String>
    suspend fun extractReminderIntent(text: String): AppResult<ReminderParseResult>
    suspend fun generateTaskSuggestions(text: String): AppResult<List<String>>
    suspend fun generateConversationTitle(messages: List<String>): AppResult<String>
    suspend fun summarizeNote(noteText: String): AppResult<String>
}

interface AssistantGateway {
    val activeProviderType: Flow<ProviderType>
    val modelStatus: Flow<ModelStatus>

    suspend fun warmUp(): AppResult<Unit>
    suspend fun cancelGeneration()
    suspend fun sendMessage(
        userMessage: String,
        context: AiConversationContext,
        onPartial: suspend (String) -> Unit = {},
    ): AppResult<AiResponse>

    suspend fun summarizeConversation(conversation: String): AppResult<String>
    suspend fun extractReminderIntent(text: String): AppResult<ReminderParseResult>
    suspend fun generateTaskSuggestions(text: String): AppResult<List<String>>
    suspend fun generateConversationTitle(messages: List<String>): AppResult<String>
    suspend fun summarizeNote(noteText: String): AppResult<String>
}
