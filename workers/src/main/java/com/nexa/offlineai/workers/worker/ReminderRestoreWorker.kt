package com.nexa.offlineai.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexa.offlineai.domain.model.ReminderStatus
import com.nexa.offlineai.domain.repository.ReminderRepository
import com.nexa.offlineai.domain.repository.ReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderRestoreWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        reminderRepository.observeReminders().first()
            .filter { it.status != ReminderStatus.COMPLETED && it.scheduledAt > System.currentTimeMillis() }
            .forEach { reminderScheduler.schedule(it) }
        return Result.success()
    }
}
