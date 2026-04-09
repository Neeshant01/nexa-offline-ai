package com.nexa.offlineai.ai.engine

import com.nexa.offlineai.ai.local.LocalGemmaProvider
import com.nexa.offlineai.ai.mock.MockAiProvider
import com.nexa.offlineai.ai.remote.RemoteFallbackProvider
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.AiConversationContext
import com.nexa.offlineai.domain.model.AiResponse
import com.nexa.offlineai.domain.model.ModelStatus
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.model.ReminderParseResult
import com.nexa.offlineai.domain.repository.AssistantEngine
import com.nexa.offlineai.domain.repository.AssistantGateway
import com.nexa.offlineai.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

@Singleton
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantGatewayImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localGemmaProvider: LocalGemmaProvider,
    private val mockAiProvider: MockAiProvider,
    private val remoteFallbackProvider: RemoteFallbackProvider,
) : AssistantGateway {

    override val activeProviderType: Flow<ProviderType> = settingsRepository.settings
        .map { it.providerType }
        .distinctUntilChanged()

    override val modelStatus: Flow<ModelStatus> = settingsRepository.settings
        .mapLatest { settings ->
            when (settings.providerType) {
                ProviderType.LOCAL_GEMMA -> {
                    val localStatus = localGemmaProvider.modelStatus.map { status ->
                        if (status.isReady) {
                            status
                        } else {
                            status.copy(
                                detail = "${status.detail} Mock fallback stays available for chat, notes, and summaries until a Gemma runtime bridge is connected.",
                            )
                        }
                    }
                    localStatus
                }

                ProviderType.MOCK -> mockAiProvider.modelStatus
                ProviderType.REMOTE_FALLBACK -> remoteFallbackProvider.modelStatus
            }
        }
        .flatMapLatest { it }

    override suspend fun warmUp(): AppResult<Unit> = withFallback { settings, provider ->
        provider.warmUp().recoverWithMock(settings.providerType) {
            mockAiProvider.warmUp()
        }
    }

    override suspend fun cancelGeneration() = provider(settingsRepository.getSettings().providerType).cancelGeneration()

    override suspend fun sendMessage(
        userMessage: String,
        context: AiConversationContext,
        onPartial: suspend (String) -> Unit,
    ): AppResult<AiResponse> {
        return withFallback { settings, provider ->
            provider.sendMessage(
                userMessage = userMessage,
                context = context.copy(settings = settings),
                onPartial = onPartial,
            ).recoverWithMock(settings.providerType) {
                mockAiProvider.sendMessage(
                    userMessage = userMessage,
                    context = context.copy(settings = settings.copy(providerType = ProviderType.MOCK)),
                    onPartial = onPartial,
                )
            }
        }
    }

    override suspend fun summarizeConversation(conversation: String): AppResult<String> =
        withFallback { settings, provider ->
            provider.summarizeConversation(conversation).recoverWithMock(settings.providerType) {
                mockAiProvider.summarizeConversation(conversation)
            }
        }

    override suspend fun extractReminderIntent(text: String): AppResult<ReminderParseResult> =
        withFallback { settings, provider ->
            provider.extractReminderIntent(text).recoverWithMock(settings.providerType) {
                mockAiProvider.extractReminderIntent(text)
            }
        }

    override suspend fun generateTaskSuggestions(text: String): AppResult<List<String>> =
        withFallback { settings, provider ->
            provider.generateTaskSuggestions(text).recoverWithMock(settings.providerType) {
                mockAiProvider.generateTaskSuggestions(text)
            }
        }

    override suspend fun generateConversationTitle(messages: List<String>): AppResult<String> =
        withFallback { settings, provider ->
            provider.generateConversationTitle(messages).recoverWithMock(settings.providerType) {
                mockAiProvider.generateConversationTitle(messages)
            }
        }

    override suspend fun summarizeNote(noteText: String): AppResult<String> =
        withFallback { settings, provider ->
            provider.summarizeNote(noteText).recoverWithMock(settings.providerType) {
                mockAiProvider.summarizeNote(noteText)
            }
        }

    private fun provider(type: ProviderType): AssistantEngine = when (type) {
        ProviderType.LOCAL_GEMMA -> localGemmaProvider
        ProviderType.MOCK -> mockAiProvider
        ProviderType.REMOTE_FALLBACK -> remoteFallbackProvider
    }

    private suspend fun <T> withFallback(
        block: suspend (settings: com.nexa.offlineai.domain.model.AssistantSettings, provider: AssistantEngine) -> AppResult<T>,
    ): AppResult<T> {
        val settings = settingsRepository.getSettings()
        return block(settings, provider(settings.providerType))
    }

    private suspend fun <T> AppResult<T>.recoverWithMock(
        providerType: ProviderType,
        fallback: suspend () -> AppResult<T>,
    ): AppResult<T> {
        return when {
            providerType == ProviderType.LOCAL_GEMMA && this is AppResult.Error -> fallback()
            else -> this
        }
    }
}
