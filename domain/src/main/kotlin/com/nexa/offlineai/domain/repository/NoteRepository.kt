package com.nexa.offlineai.domain.repository

import com.nexa.offlineai.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(noteId: String): Note?
    suspend fun upsertNote(note: Note)
    suspend fun deleteNote(noteId: String)
    suspend fun noteCount(): Int
    suspend fun clearAll()
}
