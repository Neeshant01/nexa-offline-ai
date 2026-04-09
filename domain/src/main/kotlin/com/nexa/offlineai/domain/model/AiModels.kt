package com.nexa.offlineai.domain.model

data class AiConversationTurn(
    val role: MessageRole,
    val content: String,
)

data class AiRequest(
    val userMessage: String,
    val context: AiConversationContext,
)

data class AiConversationContext(
    val conversationId: String? = null,
    val turns: List<AiConversationTurn> = emptyList(),
    val settings: AssistantSettings = AssistantSettings(),
)

data class AiResponse(
    val text: String,
    val providerType: ProviderType,
    val finishReason: String,
    val latencyMs: Long,
    val streamed: Boolean,
)
