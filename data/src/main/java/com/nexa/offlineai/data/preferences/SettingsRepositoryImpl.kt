package com.nexa.offlineai.data.preferences

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexa.offlineai.domain.model.AccentOption
import com.nexa.offlineai.domain.model.AssistantSettings
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.model.ThemeMode
import com.nexa.offlineai.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "nexa_preferences")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    override val settings: Flow<AssistantSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw exception
        }
        .map(::preferencesToSettings)

    override suspend fun getSettings(): AssistantSettings = settings.first()

    override suspend fun updateSettings(settings: AssistantSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.themeMode] = settings.themeMode.name
            preferences[Keys.accentOption] = settings.accentOption.name
            preferences[Keys.assistantName] = settings.assistantName
            preferences[Keys.providerType] = settings.providerType.name
            preferences[Keys.ttsEnabled] = settings.ttsEnabled
            preferences[Keys.speechRate] = settings.speechRate
            preferences[Keys.modelPath] = settings.modelPath
            preferences[Keys.quantizedModel] = settings.quantizedModel
            preferences[Keys.contextLength] = settings.contextLength
            preferences[Keys.maxOutputTokens] = settings.maxOutputTokens
            preferences[Keys.temperature] = settings.temperature
            preferences[Keys.offlineOnly] = settings.offlineOnly
            preferences[Keys.onboardingCompleted] = settings.onboardingCompleted
            preferences[Keys.dailySummaryEnabled] = settings.dailySummaryEnabled
            preferences[Keys.demoSeeded] = settings.demoSeeded
        }
    }

    override suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun preferencesToSettings(preferences: Preferences): AssistantSettings = AssistantSettings(
        themeMode = preferences[Keys.themeMode]?.let(ThemeMode::valueOf) ?: AssistantSettings().themeMode,
        accentOption = preferences[Keys.accentOption]?.let(AccentOption::valueOf) ?: AssistantSettings().accentOption,
        assistantName = preferences[Keys.assistantName] ?: AssistantSettings().assistantName,
        providerType = preferences[Keys.providerType]?.let(ProviderType::valueOf) ?: AssistantSettings().providerType,
        ttsEnabled = preferences[Keys.ttsEnabled] ?: AssistantSettings().ttsEnabled,
        speechRate = preferences[Keys.speechRate] ?: AssistantSettings().speechRate,
        modelPath = preferences[Keys.modelPath] ?: AssistantSettings().modelPath,
        quantizedModel = preferences[Keys.quantizedModel] ?: AssistantSettings().quantizedModel,
        contextLength = preferences[Keys.contextLength] ?: AssistantSettings().contextLength,
        maxOutputTokens = preferences[Keys.maxOutputTokens] ?: AssistantSettings().maxOutputTokens,
        temperature = preferences[Keys.temperature] ?: AssistantSettings().temperature,
        offlineOnly = preferences[Keys.offlineOnly] ?: AssistantSettings().offlineOnly,
        onboardingCompleted = preferences[Keys.onboardingCompleted] ?: AssistantSettings().onboardingCompleted,
        dailySummaryEnabled = preferences[Keys.dailySummaryEnabled] ?: AssistantSettings().dailySummaryEnabled,
        demoSeeded = preferences[Keys.demoSeeded] ?: AssistantSettings().demoSeeded,
    )

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accentOption = stringPreferencesKey("accent_option")
        val assistantName = stringPreferencesKey("assistant_name")
        val providerType = stringPreferencesKey("provider_type")
        val ttsEnabled = booleanPreferencesKey("tts_enabled")
        val speechRate = floatPreferencesKey("speech_rate")
        val modelPath = stringPreferencesKey("model_path")
        val quantizedModel = stringPreferencesKey("quantized_model")
        val contextLength = intPreferencesKey("context_length")
        val maxOutputTokens = intPreferencesKey("max_output_tokens")
        val temperature = floatPreferencesKey("temperature")
        val offlineOnly = booleanPreferencesKey("offline_only")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val dailySummaryEnabled = booleanPreferencesKey("daily_summary_enabled")
        val demoSeeded = booleanPreferencesKey("demo_seeded")
    }
}
