@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nexa.offlineai.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.core.time.TimeFormatters
import com.nexa.offlineai.coreui.components.EmptyStateCard
import com.nexa.offlineai.coreui.components.MarkdownText
import com.nexa.offlineai.coreui.components.SectionCard
import com.nexa.offlineai.domain.model.Note
import com.nexa.offlineai.domain.usecase.DeleteNoteUseCase
import com.nexa.offlineai.domain.usecase.ExtractNoteActionItemsUseCase
import com.nexa.offlineai.domain.usecase.ObserveNotesUseCase
import com.nexa.offlineai.domain.usecase.SummarizeNoteUseCase
import com.nexa.offlineai.domain.usecase.ToggleNotePinUseCase
import com.nexa.offlineai.domain.usecase.UpsertNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val allNotes: List<Note> = emptyList(),
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val editorVisible: Boolean = false,
    val editingId: String? = null,
    val titleDraft: String = "",
    val contentDraft: String = "",
    val tagsDraft: String = "",
    val pinnedDraft: Boolean = false,
    val aiSummary: String? = null,
    val actionItems: List<String> = emptyList(),
    val snackbarMessage: String? = null,
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    observeNotesUseCase: ObserveNotesUseCase,
    private val upsertNoteUseCase: UpsertNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val toggleNotePinUseCase: ToggleNotePinUseCase,
    private val summarizeNoteUseCase: SummarizeNoteUseCase,
    private val extractNoteActionItemsUseCase: ExtractNoteActionItemsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeNotesUseCase().collect { notes ->
                _uiState.update { state ->
                    state.copy(
                        allNotes = notes,
                        notes = filterNotes(notes, state.query),
                    )
                }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                query = query,
                notes = filterNotes(state.allNotes, query),
            )
        }
    }

    fun startCreate() {
        _uiState.update {
            it.copy(
                editorVisible = true,
                editingId = null,
                titleDraft = "",
                contentDraft = "",
                tagsDraft = "",
                pinnedDraft = false,
                aiSummary = null,
                actionItems = emptyList(),
            )
        }
    }

    fun edit(note: Note) {
        _uiState.update {
            it.copy(
                editorVisible = true,
                editingId = note.id,
                titleDraft = note.title,
                contentDraft = note.content,
                tagsDraft = note.tags.joinToString(", "),
                pinnedDraft = note.isPinned,
                aiSummary = null,
                actionItems = emptyList(),
            )
        }
    }

    fun updateTitle(text: String) = _uiState.update { it.copy(titleDraft = text) }
    fun updateContent(text: String) = _uiState.update { it.copy(contentDraft = text) }
    fun updateTags(text: String) = _uiState.update { it.copy(tagsDraft = text) }
    fun updatePinned(value: Boolean) = _uiState.update { it.copy(pinnedDraft = value) }
    fun dismissEditor() = _uiState.update { it.copy(editorVisible = false) }
    fun consumeSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun save() {
        viewModelScope.launch {
            upsertNoteUseCase(
                noteId = _uiState.value.editingId,
                title = _uiState.value.titleDraft,
                content = _uiState.value.contentDraft,
                tags = _uiState.value.tagsDraft.split(",").map { it.trim() },
                pinned = _uiState.value.pinnedDraft,
            )
            _uiState.update { it.copy(editorVisible = false, snackbarMessage = "Note saved locally.") }
        }
    }

    fun delete(noteId: String) {
        viewModelScope.launch {
            deleteNoteUseCase(noteId)
            _uiState.update { it.copy(snackbarMessage = "Note deleted.") }
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { toggleNotePinUseCase(note) }
    }

    fun summarize() {
        viewModelScope.launch {
            when (val summary = summarizeNoteUseCase(_uiState.value.contentDraft)) {
                is AppResult.Success -> _uiState.update { it.copy(aiSummary = summary.value) }
                is AppResult.Error -> _uiState.update { it.copy(snackbarMessage = summary.error.message) }
            }
        }
    }

    fun extractTasks() {
        viewModelScope.launch {
            when (val tasks = extractNoteActionItemsUseCase(_uiState.value.contentDraft)) {
                is AppResult.Success -> _uiState.update { it.copy(actionItems = tasks.value) }
                is AppResult.Error -> _uiState.update { it.copy(snackbarMessage = tasks.error.message) }
            }
        }
    }

    private fun filterNotes(notes: List<Note>, query: String): List<Note> {
        val ordered = notes.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
        if (query.isBlank()) return ordered
        return ordered.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }
}

@Composable
fun NotesRoute(
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotesScreen(
        state = state,
        onQueryChange = viewModel::updateQuery,
        onCreate = viewModel::startCreate,
        onEdit = viewModel::edit,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onDismissEditor = viewModel::dismissEditor,
        onTitleChange = viewModel::updateTitle,
        onContentChange = viewModel::updateContent,
        onTagsChange = viewModel::updateTags,
        onPinnedChange = viewModel::updatePinned,
        onTogglePin = viewModel::togglePin,
        onSummarize = viewModel::summarize,
        onExtractTasks = viewModel::extractTasks,
        onConsumeSnackbar = viewModel::consumeSnackbar,
        modifier = modifier,
    )
}

@Composable
fun NotesScreen(
    state: NotesUiState,
    onQueryChange: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Note) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onDismissEditor: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onPinnedChange: (Boolean) -> Unit,
    onTogglePin: (Note) -> Unit,
    onSummarize: () -> Unit,
    onExtractTasks: () -> Unit,
    onConsumeSnackbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeSnackbar()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Smart Notes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Rounded.Add, contentDescription = "Create note")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search notes, tags, or content") },
                )
            }
            if (state.editorVisible) {
                item {
                    SectionCard(
                        title = if (state.editingId == null) "New note" else "Edit note",
                        subtitle = "Stored locally in Room and ready for offline AI actions.",
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = state.titleDraft, onValueChange = onTitleChange, modifier = Modifier.fillMaxWidth(), label = { Text("Title") })
                            OutlinedTextField(
                                value = state.contentDraft,
                                onValueChange = onContentChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Content") },
                                minLines = 4,
                            )
                            OutlinedTextField(value = state.tagsDraft, onValueChange = onTagsChange, modifier = Modifier.fillMaxWidth(), label = { Text("Tags (comma separated)") })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onPinnedChange(!state.pinnedDraft) }) {
                                    Text(if (state.pinnedDraft) "Pinned to home" else "Pin to home")
                                }
                                TextButton(onClick = onSummarize, enabled = state.contentDraft.isNotBlank()) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                    Text("Summarize")
                                }
                                TextButton(onClick = onExtractTasks, enabled = state.contentDraft.isNotBlank()) {
                                    Text("Extract actions")
                                }
                            }
                            state.aiSummary?.let {
                                Text("AI Summary", fontWeight = FontWeight.SemiBold)
                                MarkdownText(it)
                            }
                            if (state.actionItems.isNotEmpty()) {
                                Text("Action items", fontWeight = FontWeight.SemiBold)
                                state.actionItems.forEach { task -> Text("- $task") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onDismissEditor) { Text("Cancel") }
                                TextButton(onClick = onSave) { Text("Save note") }
                            }
                        }
                    }
                }
            }
            if (state.notes.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No notes yet",
                        body = "Capture ideas, pin key notes, and summarize them with AI later.",
                    )
                }
            } else {
                items(state.notes, key = { it.id }) { note ->
                    SectionCard(
                        title = note.title,
                        subtitle = "${TimeFormatters.formatDateTime(note.updatedAt)} | ${note.tags.joinToString(" | ").ifBlank { "No tags" }}",
                    ) {
                        Text(note.content, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEdit(note) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onTogglePin(note) }) {
                                Icon(Icons.Rounded.PushPin, contentDescription = "Pin")
                            }
                            IconButton(onClick = { onDelete(note.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
