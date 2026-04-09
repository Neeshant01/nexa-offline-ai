package com.nexa.offlineai.domain.repository

import com.nexa.offlineai.domain.model.AssistantSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AssistantSettings>
    suspend fun getSettings(): AssistantSettings
    suspend fun updateSettings(settings: AssistantSettings)
    suspend fun clearAll()
}
