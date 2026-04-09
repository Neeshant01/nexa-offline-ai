package com.nexa.offlineai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexa.offlineai.data.local.dao.ChatMessageDao
import com.nexa.offlineai.data.local.dao.ConversationDao
import com.nexa.offlineai.data.local.dao.NoteDao
import com.nexa.offlineai.data.local.dao.ReminderTaskDao
import com.nexa.offlineai.data.local.entity.ChatMessageEntity
import com.nexa.offlineai.data.local.entity.ConversationEntity
import com.nexa.offlineai.data.local.entity.NoteEntity
import com.nexa.offlineai.data.local.entity.ReminderTaskEntity

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        NoteEntity::class,
        ReminderTaskEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NexaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderTaskDao(): ReminderTaskDao
}
