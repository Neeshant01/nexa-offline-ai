package com.nexa.offlineai.ai.mock

import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.AiConversationContext
import com.nexa.offlineai.domain.model.AiResponse
import com.nexa.offlineai.domain.model.ModelState
import com.nexa.offlineai.domain.model.ModelStatus
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.model.ReminderParseResult
import com.nexa.offlineai.domain.repository.AssistantEngine
import com.nexa.offlineai.domain.usecase.ReminderTextParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class MockAiProvider @Inject constructor() : AssistantEngine {
    override val providerType: ProviderType = ProviderType.MOCK
    override val modelStatus: Flow<ModelStatus> = flowOf(
        ModelStatus(
            providerType = providerType,
            state = ModelState.READY,
            detail = "Mock provider is active for demos, previews, and offline testing.",
            loadedModel = "mock-brain",
        ),
    )

    override suspend fun warmUp(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun cancelGeneration() = Unit

    override suspend fun sendMessage(
        userMessage: String,
        context: AiConversationContext,
        onPartial: suspend (String) -> Unit,
    ): AppResult<AiResponse> {
        val response = mockResponse(userMessage, context.settings.assistantName)
        val chunks = response.chunked(32)
        val builder = StringBuilder()
        chunks.forEachIndexed { index, chunk ->
            delay(40)
            builder.append(chunk)
            if (index != chunks.lastIndex) {
                onPartial(builder.toString())
            }
        }
        return AppResult.Success(
            AiResponse(
                text = response,
                providerType = providerType,
                finishReason = "completed",
                latencyMs = 350L,
                streamed = true,
            ),
        )
    }

    override suspend fun summarizeConversation(conversation: String): AppResult<String> =
        AppResult.Success("Summary: ${conversation.lines().takeLast(4).joinToString(" ").take(220)}")

    override suspend fun extractReminderIntent(text: String): AppResult<ReminderParseResult> =
        AppResult.Success(ReminderTextParser.parse(text).copy(source = "mock_ai", confidence = 0.9f))

    override suspend fun generateTaskSuggestions(text: String): AppResult<List<String>> =
        AppResult.Success(
            text.split("\n", ".", " and ")
                .map { it.trim().removePrefix("-").removePrefix("*") }
                .filter { it.length > 5 }
                .mapIndexed { index, item -> if (index < 3) item.replaceFirstChar { it.uppercase() } else "Review $item" }
                .take(5),
        )

    override suspend fun generateConversationTitle(messages: List<String>): AppResult<String> =
        AppResult.Success(
            messages.firstOrNull()
                ?.split(" ")
                ?.take(4)
                ?.joinToString(" ")
                ?.replaceFirstChar { it.uppercase() }
                ?: "Nexa chat",
        )

    override suspend fun summarizeNote(noteText: String): AppResult<String> =
        AppResult.Success(
            noteText.lines()
                .filter { it.isNotBlank() }
                .take(3)
                .joinToString(" ")
                .take(220)
                .ifBlank { "This note is short and ready for follow-up." },
        )

    private fun mockResponse(prompt: String, assistantName: String): String {
        val normalized = prompt.lowercase()
        return when {
            "plan" in normalized || "roadmap" in normalized ->
                "$assistantName suggests a three-step plan: clarify the outcome, break it into manageable actions, and start with the highest-leverage task."
            "note" in normalized ->
                "$assistantName can turn this into a cleaner note with a short summary, key takeaways, and action items."
            "remind" in normalized ->
                "$assistantName understood this as a reminder request. I can save it locally and schedule a notification on-device."
            "offline" in normalized ->
                "$assistantName is currently running in offline demo mode, so your data stays local and the UI remains fully usable."
            else ->
                "$assistantName is ready. Here’s the short answer: ${prompt.trim().replaceFirstChar { it.uppercase() }} can be handled locally, and I’ll keep the response concise and useful."
        }
    }
}
