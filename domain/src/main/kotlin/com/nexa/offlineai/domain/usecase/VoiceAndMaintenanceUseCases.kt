package com.nexa.offlineai.domain.usecase

import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.AiConversationContext
import com.nexa.offlineai.domain.repository.AssistantGateway
import com.nexa.offlineai.domain.repository.ChatRepository
import com.nexa.offlineai.domain.repository.NoteRepository
import com.nexa.offlineai.domain.repository.ReminderRepository
import com.nexa.offlineai.domain.repository.ReminderScheduler
import com.nexa.offlineai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class QuickAssistantReplyUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
) {
    suspend operator fun invoke(transcript: String): AppResult<String> =
        when (val result = assistantGateway.sendMessage(transcript, AiConversationContext())) {
            is AppResult.Success -> AppResult.Success(result.value.text)
            is AppResult.Error -> result
        }
}

class ClearAllDataUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        reminderRepository.observeReminders().first().forEach { reminder ->
            reminderScheduler.cancel(reminder.id)
        }
        chatRepository.clearAll()
        noteRepository.clearAll()
        reminderRepository.clearAll()
        val current = settingsRepository.getSettings()
        settingsRepository.updateSettings(current.copy(demoSeeded = false))
    }
}
