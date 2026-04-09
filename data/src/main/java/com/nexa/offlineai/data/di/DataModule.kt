package com.nexa.offlineai.data.di

import android.content.Context
import androidx.room.Room
import com.nexa.offlineai.data.local.NexaDatabase
import com.nexa.offlineai.data.local.dao.ChatMessageDao
import com.nexa.offlineai.data.local.dao.ConversationDao
import com.nexa.offlineai.data.local.dao.NoteDao
import com.nexa.offlineai.data.local.dao.ReminderTaskDao
import com.nexa.offlineai.data.preferences.SettingsRepositoryImpl
import com.nexa.offlineai.data.repository.ChatRepositoryImpl
import com.nexa.offlineai.data.repository.NoteRepositoryImpl
import com.nexa.offlineai.data.repository.ReminderRepositoryImpl
import com.nexa.offlineai.domain.repository.ChatRepository
import com.nexa.offlineai.domain.repository.NoteRepository
import com.nexa.offlineai.domain.repository.ReminderRepository
import com.nexa.offlineai.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NexaDatabase =
        Room.databaseBuilder(context, NexaDatabase::class.java, "nexa_ai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideConversationDao(database: NexaDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideChatMessageDao(database: NexaDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideNoteDao(database: NexaDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideReminderTaskDao(database: NexaDatabase): ReminderTaskDao = database.reminderTaskDao()
}
