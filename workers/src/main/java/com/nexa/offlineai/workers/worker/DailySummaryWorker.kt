package com.nexa.offlineai.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexa.offlineai.domain.usecase.GenerateDailySummaryUseCase
import com.nexa.offlineai.workers.notification.NotificationPublisher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val generateDailySummaryUseCase: GenerateDailySummaryUseCase,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        val summary = generateDailySummaryUseCase()
        NotificationPublisher.showDailySummary(applicationContext, summary)
        Result.success()
    }.getOrElse {
        Result.retry()
    }
}
