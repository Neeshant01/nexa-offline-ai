package com.nexa.offlineai.domain.usecase

import com.nexa.offlineai.core.common.AppError
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.core.common.IdFactory
import com.nexa.offlineai.domain.model.ReminderParseResult
import com.nexa.offlineai.domain.model.ReminderStatus
import com.nexa.offlineai.domain.model.ReminderTask
import com.nexa.offlineai.domain.repository.AssistantGateway
import com.nexa.offlineai.domain.repository.ReminderRepository
import com.nexa.offlineai.domain.repository.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRemindersUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
) {
    operator fun invoke(): Flow<List<ReminderTask>> = reminderRepository.observeReminders()
}

class ParseReminderTextUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
) {
    suspend operator fun invoke(text: String): ReminderParseResult {
        val aiResult = assistantGateway.extractReminderIntent(text)
        return when (aiResult) {
            is AppResult.Success -> aiResult.value.takeIf { it.matched && it.scheduledAt != null } ?: ReminderTextParser.parse(text)
            is AppResult.Error -> ReminderTextParser.parse(text)
        }
    }
}

class CreateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        scheduledAt: Long,
        sourceText: String = "",
        confidence: Float = 1f,
    ): AppResult<ReminderTask> {
        if (title.isBlank()) {
            return AppResult.Error(AppError.Validation("Reminder title cannot be blank"))
        }
        val reminder = ReminderTask(
            id = IdFactory.newId(),
            title = title,
            description = description,
            scheduledAt = scheduledAt,
            createdAt = System.currentTimeMillis(),
            status = ReminderStatus.UPCOMING,
            sourceText = sourceText,
            confidence = confidence,
        )
        reminderRepository.upsertReminder(reminder)
        return when (val schedule = reminderScheduler.schedule(reminder)) {
            is AppResult.Success -> AppResult.Success(reminder)
            is AppResult.Error -> schedule
        }
    }
}

class CreateReminderFromTextUseCase @Inject constructor(
    private val parseReminderTextUseCase: ParseReminderTextUseCase,
    private val createReminderUseCase: CreateReminderUseCase,
) {
    suspend operator fun invoke(text: String): AppResult<ReminderTask> {
        val parsed = parseReminderTextUseCase(text)
        val whenAt = parsed.scheduledAt ?: return AppResult.Error(AppError.Validation("Couldn't detect a valid time"))
        return createReminderUseCase(
            title = parsed.title,
            description = parsed.description,
            scheduledAt = whenAt,
            sourceText = text,
            confidence = parsed.confidence,
        )
    }
}

class UpdateReminderStatusUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminderTask: ReminderTask, status: ReminderStatus) {
        val updated = reminderTask.copy(status = status)
        reminderRepository.upsertReminder(updated)
        if (status == ReminderStatus.COMPLETED) {
            reminderScheduler.cancel(reminderTask.id)
        }
    }
}

class SnoozeReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminderTask: ReminderTask, minutes: Long = 15) {
        val updated = reminderTask.copy(
            scheduledAt = reminderTask.scheduledAt + minutes * 60_000L,
            status = ReminderStatus.SNOOZED,
        )
        reminderRepository.upsertReminder(updated)
        reminderScheduler.schedule(updated)
    }
}

class DeleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminderId: String) {
        reminderRepository.deleteReminder(reminderId)
        reminderScheduler.cancel(reminderId)
    }
}
