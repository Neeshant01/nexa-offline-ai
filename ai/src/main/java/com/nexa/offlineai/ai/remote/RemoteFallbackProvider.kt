package com.nexa.offlineai.ai.remote

import com.nexa.offlineai.core.common.AppError
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.AiConversationContext
import com.nexa.offlineai.domain.model.AiResponse
import com.nexa.offlineai.domain.model.ModelState
import com.nexa.offlineai.domain.model.ModelStatus
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.model.ReminderParseResult
import com.nexa.offlineai.domain.repository.AssistantEngine
import com.nexa.offlineai.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class RemoteFallbackProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : AssistantEngine {
    override val providerType: ProviderType = ProviderType.REMOTE_FALLBACK

    override val modelStatus: Flow<ModelStatus> = flow {
        val settings = settingsRepository.getSettings()
        emit(
            ModelStatus(
                providerType = providerType,
                state = if (settings.offlineOnly) ModelState.REMOTE_ONLY else ModelState.UNKNOWN,
                detail = if (settings.offlineOnly) {
                    "Offline-only mode blocks remote AI usage."
                } else {
                    "Remote fallback is available architecturally but not connected to an API client yet."
                },
                loadedModel = "future-remote-endpoint",
            ),
        )
    }

    override suspend fun warmUp(): AppResult<Unit> = AppResult.Error(
        AppError.Network("Remote fallback is disabled until an API client is integrated."),
    )

    override suspend fun cancelGeneration() = Unit

    override suspend fun sendMessage(
        userMessage: String,
        context: AiConversationContext,
        onPartial: suspend (String) -> Unit,
    ): AppResult<AiResponse> = AppResult.Error(
        AppError.Network("Remote fallback is intentionally left disconnected. Add your hosted provider in ai/remote/RemoteFallbackProvider.kt."),
    )

    override suspend fun summarizeConversation(conversation: String): AppResult<String> = AppResult.Error(
        AppError.Network("Remote summary is unavailable without a hosted connector."),
    )

    override suspend fun extractReminderIntent(text: String): AppResult<ReminderParseResult> = AppResult.Error(
        AppError.Network("Remote intent extraction is unavailable without a hosted connector."),
    )

    override suspend fun generateTaskSuggestions(text: String): AppResult<List<String>> = AppResult.Error(
        AppError.Network("Remote task suggestions are unavailable without a hosted connector."),
    )

    override suspend fun generateConversationTitle(messages: List<String>): AppResult<String> = AppResult.Error(
        AppError.Network("Remote title generation is unavailable without a hosted connector."),
    )

    override suspend fun summarizeNote(noteText: String): AppResult<String> = AppResult.Error(
        AppError.Network("Remote note summary is unavailable without a hosted connector."),
    )
}
