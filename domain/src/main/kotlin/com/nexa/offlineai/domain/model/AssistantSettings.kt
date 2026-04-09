package com.nexa.offlineai.domain.model

data class AssistantSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentOption: AccentOption = AccentOption.AURORA,
    val assistantName: String = "Nexa",
    val providerType: ProviderType = ProviderType.MOCK,
    val ttsEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val modelPath: String = "",
    val quantizedModel: String = "gemma4-int4.task",
    val contextLength: Int = 2048,
    val maxOutputTokens: Int = 384,
    val temperature: Float = 0.7f,
    val offlineOnly: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val dailySummaryEnabled: Boolean = true,
    val demoSeeded: Boolean = false,
)
