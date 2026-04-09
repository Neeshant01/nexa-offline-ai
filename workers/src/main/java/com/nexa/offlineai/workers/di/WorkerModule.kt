package com.nexa.offlineai.workers.di

import com.nexa.offlineai.domain.repository.ReminderScheduler
import com.nexa.offlineai.workers.scheduler.AndroidReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {
    @Binds
    abstract fun bindReminderScheduler(impl: AndroidReminderScheduler): ReminderScheduler
}
