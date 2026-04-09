package com.nexa.offlineai.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexa.offlineai.core.common.IdFactory
import com.nexa.offlineai.domain.model.AccentOption
import com.nexa.offlineai.domain.model.AssistantSettings
import com.nexa.offlineai.domain.model.ChatMessage
import com.nexa.offlineai.domain.model.Conversation
import com.nexa.offlineai.domain.model.MessageRole
import com.nexa.offlineai.domain.model.Note
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.model.ReminderStatus
import com.nexa.offlineai.domain.model.ReminderTask
import com.nexa.offlineai.domain.repository.ChatRepository
import com.nexa.offlineai.domain.repository.NoteRepository
import com.nexa.offlineai.domain.repository.ReminderRepository
import com.nexa.offlineai.domain.repository.ReminderScheduler
import com.nexa.offlineai.domain.usecase.ObserveSettingsUseCase
import com.nexa.offlineai.domain.usecase.UpdateSettingsUseCase
import com.nexa.offlineai.domain.usecase.WarmUpAssistantUseCase
import com.nexa.offlineai.workers.scheduler.DailySummaryWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoading: Boolean = true,
    val settings: AssistantSettings = AssistantSettings(),
)

@HiltViewModel
class MainViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val warmUpAssistantUseCase: WarmUpAssistantUseCase,
    private val chatRepository: ChatRepository,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val dailySummaryWorkScheduler: DailySummaryWorkScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var bootstrapped = false

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(isLoading = false, settings = settings) }
            }
        }
    }

    fun completeOnboarding(name: String, accent: AccentOption, providerType: ProviderType) {
        viewModelScope.launch {
            updateSettingsUseCase(
                _uiState.value.settings.copy(
                    assistantName = name.ifBlank { "Nexa" },
                    accentOption = accent,
                    providerType = providerType,
                    onboardingCompleted = true,
                ),
            )
        }
    }

    fun bootstrapIfNeeded() {
        if (bootstrapped || !_uiState.value.settings.onboardingCompleted) return
        bootstrapped = true
        viewModelScope.launch {
            val settings = normalizeStartupSettings(_uiState.value.settings)
            warmUpAssistantUseCase()
            dailySummaryWorkScheduler.schedule(settings.dailySummaryEnabled)
            if (!settings.demoSeeded) {
                seedDemoContent(settings)
                updateSettingsUseCase(settings.copy(demoSeeded = true))
            }
        }
    }

    private suspend fun normalizeStartupSettings(settings: AssistantSettings): AssistantSettings {
        if (settings.providerType != ProviderType.LOCAL_GEMMA || settings.modelPath.isNotBlank()) {
            return settings
        }
        val updated = settings.copy(providerType = ProviderType.MOCK)
        updateSettingsUseCase(updated)
        return updated
    }

    private suspend fun seedDemoContent(settings: AssistantSettings) {
        if (chatRepository.conversationCount() == 0) {
            val conversationId = IdFactory.newId()
            val now = System.currentTimeMillis()
            chatRepository.upsertConversation(
                Conversation(
                    id = conversationId,
                    title = "Welcome to ${settings.assistantName}",
                    createdAt = now,
                    updatedAt = now,
                    lastMessagePreview = "Everything below is private and stored locally.",
                ),
            )
            chatRepository.upsertMessage(
                ChatMessage(
                    id = IdFactory.newId(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = "I’m ready in offline-first mode. Try chat, notes, reminders, or switch to Mock provider until Gemma is connected.",
                    createdAt = now,
                ),
            )
        }

        if (noteRepository.noteCount() == 0) {
            noteRepository.upsertNote(
                Note(
                    id = IdFactory.newId(),
                    title = "Welcome note",
                    content = "Point Settings > Model path to your quantized Gemma bundle, then keep using the app locally for notes, chat, and reminders.",
                    tags = listOf("welcome", "setup"),
                    isPinned = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        if (reminderRepository.reminderCount() == 0) {
            val reminder = ReminderTask(
                id = IdFactory.newId(),
                title = "Connect local model",
                description = "Set your Gemma runtime path in Settings when you’re ready.",
                scheduledAt = System.currentTimeMillis() + 7_200_000L,
                createdAt = System.currentTimeMillis(),
                status = ReminderStatus.UPCOMING,
                sourceText = "seeded",
                confidence = 1f,
            )
            reminderRepository.upsertReminder(reminder)
            reminderScheduler.schedule(reminder)
        }
    }
}
