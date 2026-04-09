package com.nexa.offlineai.workers.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nexa.offlineai.workers.worker.DailySummaryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedule(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(DAILY_SUMMARY_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            DAILY_SUMMARY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        const val DAILY_SUMMARY_WORK = "nexa_daily_summary_work"
    }
}
