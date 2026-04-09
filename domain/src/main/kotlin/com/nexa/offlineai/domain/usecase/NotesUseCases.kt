package com.nexa.offlineai.domain.usecase

import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.core.common.IdFactory
import com.nexa.offlineai.domain.model.Note
import com.nexa.offlineai.domain.repository.AssistantGateway
import com.nexa.offlineai.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(): Flow<List<Note>> = noteRepository.observeNotes()
}

class UpsertNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(
        noteId: String?,
        title: String,
        content: String,
        tags: List<String>,
        pinned: Boolean,
    ): Note {
        val current = noteId?.let { id -> noteRepository.getNote(id) }
        val now = System.currentTimeMillis()
        val note = Note(
            id = current?.id ?: IdFactory.newId(),
            title = title.ifBlank { "Untitled note" },
            content = content,
            tags = tags.filter { it.isNotBlank() },
            isPinned = pinned,
            createdAt = current?.createdAt ?: now,
            updatedAt = now,
        )
        noteRepository.upsertNote(note)
        return note
    }
}

class DeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(noteId: String) = noteRepository.deleteNote(noteId)
}

class ToggleNotePinUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(note: Note) =
        noteRepository.upsertNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
}

class SummarizeNoteUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
) {
    suspend operator fun invoke(noteText: String): AppResult<String> = assistantGateway.summarizeNote(noteText)
}

class ExtractNoteActionItemsUseCase @Inject constructor(
    private val assistantGateway: AssistantGateway,
) {
    suspend operator fun invoke(noteText: String): AppResult<List<String>> =
        assistantGateway.generateTaskSuggestions(noteText)
}
