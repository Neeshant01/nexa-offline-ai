package com.nexa.offlineai.data.repository

import com.nexa.offlineai.data.local.dao.NoteDao
import com.nexa.offlineai.data.local.mappers.asDomain
import com.nexa.offlineai.data.local.mappers.asEntity
import com.nexa.offlineai.domain.model.Note
import com.nexa.offlineai.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> = noteDao.observeNotes().map { entities -> entities.map { it.asDomain() } }

    override suspend fun getNote(noteId: String): Note? = noteDao.getNote(noteId)?.asDomain()

    override suspend fun upsertNote(note: Note) {
        noteDao.upsertNote(note.asEntity())
    }

    override suspend fun deleteNote(noteId: String) {
        noteDao.deleteNote(noteId)
    }

    override suspend fun noteCount(): Int = noteDao.count()

    override suspend fun clearAll() {
        noteDao.clear()
    }
}
