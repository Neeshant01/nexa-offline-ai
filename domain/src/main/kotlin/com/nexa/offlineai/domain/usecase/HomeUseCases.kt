package com.nexa.offlineai.domain.usecase

import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.domain.model.HomeSnapshot
import com.nexa.offlineai.domain.model.ModelState
import com.nexa.offlineai.domain.model.ModelStatus
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.repository.AssistantGateway
import com.nexa.offlineai.domain.repository.ChatRepository
import com.nexa.offlineai.domain.repository.NoteRepository
import com.nexa.offlineai.domain.repository.ReminderRepository
import com.nexa.offlineai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import javax.inject.Inject

class ObserveHomeSnapshotUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val assistantGateway: AssistantGateway,
    private val chatRepository: ChatRepository,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
) {
    operator fun invoke(): Flow<HomeSnapshot> = combine(
        settingsRepository.settings,
        assistantGateway.modelStatus,
        chatRepository.observeRecentConversations(),
        noteRepository.observeNotes(),
        reminderRepository.observeReminders(),
    ) { settings, modelStatus, conversations, notes, reminders ->
        HomeSnapshot(
            settings = settings,
            modelStatus = modelStatus,
            recentConversations = conversations,
            reminders = reminders.sortedBy { it.scheduledAt }.take(3),
            pinnedNote = notes.firstOrNull { it.isPinned },
            greeting = buildGreeting(settings.assistantName),
        )
    }
}

class ObserveModelStatusUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
) {
    operator fun invoke(): Flow<ModelStatus> = assistantGateway.modelStatus
}

class WarmUpAssistantUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
) {
    suspend operator fun invoke() = assistantGateway.warmUp()
}

class GenerateDailySummaryUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
    private val chatRepository: ChatRepository,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
) {
    suspend operator fun invoke(): String {
        val latestChats = chatRepository.observeRecentConversations().first()
        val notes = noteRepository.observeNotes().first().take(3)
        val reminders = reminderRepository.observeReminders().first().take(5)
        val prompt = buildString {
            appendLine("Recent chats:")
            latestChats.forEach { appendLine("- ${it.title}: ${it.lastMessagePreview}") }
            appendLine("Pinned notes:")
            notes.forEach { appendLine("- ${it.title}: ${it.content.take(120)}") }
            appendLine("Upcoming reminders:")
            reminders.forEach { appendLine("- ${it.title} at ${it.scheduledAt}") }
        }
        return when (val result = assistantGateway.summarizeConversation(prompt)) {
            is AppResult.Success -> result.value
            is AppResult.Error -> "Today looks focused. You have ${reminders.size} reminders and ${notes.size} active notes."
        }
    }
}

val defaultModelStatus = ModelStatus(
    providerType = ProviderType.LOCAL_GEMMA,
    state = ModelState.UNKNOWN,
    detail = "Model status has not been checked yet.",
)

private fun buildGreeting(assistantName: String): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good morning. $assistantName is offline and ready."
        in 12..16 -> "Good afternoon. $assistantName is keeping everything local."
        in 17..21 -> "Good evening. $assistantName is ready for your next task."
        else -> "Late-night mode enabled. $assistantName is here whenever you need it."
    }
}
