package com.nexa.offlineai.domain.usecase

import com.nexa.offlineai.domain.model.AssistantSettings
import com.nexa.offlineai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AssistantSettings> = settingsRepository.settings
}

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): AssistantSettings = settingsRepository.getSettings()
}

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(settings: AssistantSettings) = settingsRepository.updateSettings(settings)
}
