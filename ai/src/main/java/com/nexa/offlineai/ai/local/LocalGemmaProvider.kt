package com.nexa.offlineai.ai.local

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
import com.nexa.offlineai.domain.usecase.ReminderTextParser
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class LocalGemmaProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val tokenizer: GemmaTokenizer,
    private val runtimeAdapter: LocalModelRuntimeAdapter,
) : AssistantEngine {

    override val providerType: ProviderType = ProviderType.LOCAL_GEMMA

    private val statusFlow = MutableStateFlow(
        ModelStatus(
            providerType = providerType,
            state = ModelState.UNKNOWN,
            detail = "Waiting to inspect local Gemma model files.",
        ),
    )
    override val modelStatus: Flow<ModelStatus> = statusFlow.asStateFlow()

    private val sessionMutex = Mutex()
    private var activeSession: LocalInferenceSession? = null

    override suspend fun warmUp(): AppResult<Unit> = sessionMutex.withLock {
        val settings = settingsRepository.getSettings()
        val config = settings.toLocalModelConfig()
        statusFlow.value = statusFlow.value.copy(state = ModelState.CHECKING_FILES, detail = "Checking model path and runtime compatibility.")

        if (config.modelPath.isBlank()) {
            val error = AppError.Model("No local model path is configured yet.")
            statusFlow.value = statusFlow.value.copy(state = ModelState.UNAVAILABLE, detail = error.message)
            return AppResult.Error(error)
        }

        val modelFile = File(config.modelPath)
        if (!modelFile.exists()) {
            val error = AppError.Model("Configured model path does not exist on this device.")
            statusFlow.value = statusFlow.value.copy(state = ModelState.UNAVAILABLE, detail = error.message)
            return AppResult.Error(error)
        }

        if (!modelFile.canRead()) {
            val error = AppError.Model("Model file exists but cannot be read. Check SAF permissions or storage location.")
            statusFlow.value = statusFlow.value.copy(state = ModelState.UNAVAILABLE, detail = error.message)
            return AppResult.Error(error)
        }

        val capability = runtimeAdapter.checkSupport(config)
        if (!capability.supported) {
            val error = AppError.Model(capability.reason.ifBlank { "Device or runtime is unsupported." })
            statusFlow.value = statusFlow.value.copy(state = ModelState.UNSUPPORTED_DEVICE, detail = error.message)
            return AppResult.Error(error)
        }

        statusFlow.value = statusFlow.value.copy(state = ModelState.LOADING, detail = capability.reason.ifBlank { "Preparing local inference session." })
        when (val result = runtimeAdapter.loadModel(config)) {
            is AppResult.Success -> {
                activeSession = result.value
                statusFlow.value = statusFlow.value.copy(
                    state = ModelState.READY,
                    detail = "Local Gemma session loaded successfully.",
                    loadedModel = modelFile.name,
                )
                AppResult.Success(Unit)
            }

            is AppResult.Error -> {
                statusFlow.value = statusFlow.value.copy(
                    state = when {
                        result.error.message.contains("memory", ignoreCase = true) -> ModelState.INSUFFICIENT_MEMORY
                        else -> ModelState.UNAVAILABLE
                    },
                    detail = result.error.message,
                    loadedModel = modelFile.name,
                )
                result
            }
        }
    }

    override suspend fun cancelGeneration() {
        statusFlow.value = statusFlow.value.copy(detail = "Generation cancelled.")
    }

    override suspend fun sendMessage(
        userMessage: String,
        context: AiConversationContext,
        onPartial: suspend (String) -> Unit,
    ): AppResult<AiResponse> = withContext(Dispatchers.Default) {
        val session = activeSession ?: run {
            when (val warmUp = warmUp()) {
                is AppResult.Success -> activeSession
                is AppResult.Error -> null
            }
        }

        if (session == null) {
            return@withContext AppResult.Error(AppError.Model("Local Gemma is unavailable. Configure a valid model path and runtime bridge first."))
        }

        val prompt = tokenizer.buildPrompt(
            systemPrompt = buildSystemPrompt(context.settings.assistantName),
            history = context.turns.takeLast(12).map { it.role.name.lowercase() to it.content },
            userMessage = userMessage,
        )

        val startedAt = System.currentTimeMillis()
        when (val generation = runtimeAdapter.generate(session, prompt, onPartial)) {
            is AppResult.Success -> AppResult.Success(
                AiResponse(
                    text = generation.value,
                    providerType = providerType,
                    finishReason = "completed",
                    latencyMs = System.currentTimeMillis() - startedAt,
                    streamed = true,
                ),
            )

            is AppResult.Error -> {
                statusFlow.value = statusFlow.value.copy(state = ModelState.INFERENCE_ERROR, detail = generation.error.message)
                generation
            }
        }
    }

    override suspend fun summarizeConversation(conversation: String): AppResult<String> =
        sendMessage(
            userMessage = "Summarize this conversation into a concise daily recap:\n$conversation",
            context = AiConversationContext(settings = settingsRepository.getSettings()),
        ).let { result ->
            when (result) {
                is AppResult.Success -> AppResult.Success(result.value.text)
                is AppResult.Error -> result
            }
        }

    override suspend fun extractReminderIntent(text: String): AppResult<ReminderParseResult> =
        AppResult.Success(ReminderTextParser.parse(text).copy(source = "local_regex_fallback"))

    override suspend fun generateTaskSuggestions(text: String): AppResult<List<String>> =
        AppResult.Success(
            text.split(".", "\n", " and ")
                .map { it.trim() }
                .filter { it.length > 4 }
                .take(5),
        )

    override suspend fun generateConversationTitle(messages: List<String>): AppResult<String> =
        AppResult.Success(messages.firstOrNull()?.split(" ")?.take(5)?.joinToString(" ")?.replaceFirstChar { it.uppercase() } ?: "New conversation")

    override suspend fun summarizeNote(noteText: String): AppResult<String> =
        AppResult.Success(noteText.lines().filter { it.isNotBlank() }.take(3).joinToString(" ").take(220))

    private fun buildSystemPrompt(assistantName: String): String =
        "$assistantName is a privacy-first assistant running on-device. Keep replies grounded, brief, and actionable."

    private fun com.nexa.offlineai.domain.model.AssistantSettings.toLocalModelConfig(): LocalModelConfig =
        LocalModelConfig(
            modelPath = modelPath,
            quantizedModel = quantizedModel,
            contextLength = contextLength.coerceIn(1024, 8192),
            maxOutputTokens = maxOutputTokens.coerceIn(64, 1024),
            temperature = temperature.coerceIn(0.1f, 1.2f),
        )
}
