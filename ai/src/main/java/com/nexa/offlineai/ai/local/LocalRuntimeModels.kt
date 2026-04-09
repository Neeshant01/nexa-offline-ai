package com.nexa.offlineai.ai.local

data class LocalModelConfig(
    val modelPath: String,
    val quantizedModel: String,
    val contextLength: Int,
    val maxOutputTokens: Int,
    val temperature: Float,
)

data class LocalRuntimeCapability(
    val supported: Boolean,
    val reason: String = "",
)

data class LocalInferenceSession(
    val sessionId: String,
    val modelPath: String,
)

interface GemmaTokenizer {
    fun buildPrompt(systemPrompt: String, history: List<Pair<String, String>>, userMessage: String): String
}

interface LocalModelRuntimeAdapter {
    suspend fun checkSupport(config: LocalModelConfig): LocalRuntimeCapability
    suspend fun loadModel(config: LocalModelConfig): com.nexa.offlineai.core.common.AppResult<LocalInferenceSession>
    suspend fun generate(
        session: LocalInferenceSession,
        prompt: String,
        onChunk: suspend (String) -> Unit,
    ): com.nexa.offlineai.core.common.AppResult<String>

    suspend fun unload(session: LocalInferenceSession)
}
