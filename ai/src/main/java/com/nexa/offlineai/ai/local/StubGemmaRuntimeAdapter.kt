package com.nexa.offlineai.ai.local

import com.nexa.offlineai.core.common.AppError
import com.nexa.offlineai.core.common.AppResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleGemmaTokenizer @Inject constructor() : GemmaTokenizer {
    override fun buildPrompt(systemPrompt: String, history: List<Pair<String, String>>, userMessage: String): String =
        buildString {
            appendLine("<system>")
            appendLine(systemPrompt)
            appendLine("</system>")
            history.forEach { (role, content) ->
                appendLine("<$role>")
                appendLine(content)
                appendLine("</$role>")
            }
            appendLine("<user>")
            appendLine(userMessage)
            appendLine("</user>")
            appendLine("<assistant>")
        }
}

@Singleton
class StubGemmaRuntimeAdapter @Inject constructor() : LocalModelRuntimeAdapter {
    override suspend fun checkSupport(config: LocalModelConfig): LocalRuntimeCapability =
        LocalRuntimeCapability(
            supported = true,
            reason = "Bridge placeholder is ready. Connect MediaPipe, TFLite, JNI, or a bound local service here.",
        )

    override suspend fun loadModel(config: LocalModelConfig): AppResult<LocalInferenceSession> =
        AppResult.Error(
            AppError.Model(
                "Model files were found, but no local Gemma runtime adapter is connected yet. " +
                    "Implement this in ai/local/StubGemmaRuntimeAdapter.kt.",
            ),
        )

    override suspend fun generate(
        session: LocalInferenceSession,
        prompt: String,
        onChunk: suspend (String) -> Unit,
    ): AppResult<String> = AppResult.Error(
        AppError.Model("No active Gemma runtime session is available for generation."),
    )

    override suspend fun unload(session: LocalInferenceSession) = Unit
}
