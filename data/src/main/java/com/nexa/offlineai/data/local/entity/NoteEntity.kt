package com.nexa.offlineai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val tags: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
