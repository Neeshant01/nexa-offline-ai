@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nexa.offlineai.feature.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Snooze
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.core.time.TimeFormatters
import com.nexa.offlineai.coreui.components.EmptyStateCard
import com.nexa.offlineai.coreui.components.SectionCard
import com.nexa.offlineai.domain.model.ReminderStatus
import com.nexa.offlineai.domain.model.ReminderTask
import com.nexa.offlineai.domain.usecase.CreateReminderFromTextUseCase
import com.nexa.offlineai.domain.usecase.CreateReminderUseCase
import com.nexa.offlineai.domain.usecase.DeleteReminderUseCase
import com.nexa.offlineai.domain.usecase.ObserveRemindersUseCase
import com.nexa.offlineai.domain.usecase.SnoozeReminderUseCase
import com.nexa.offlineai.domain.usecase.UpdateReminderStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemindersUiState(
    val reminders: List<ReminderTask> = emptyList(),
    val naturalText: String = "",
    val titleDraft: String = "",
    val descriptionDraft: String = "",
    val scheduledAt: Long = System.currentTimeMillis() + 3_600_000L,
    val snackbarMessage: String? = null,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    observeRemindersUseCase: ObserveRemindersUseCase,
    private val createReminderFromTextUseCase: CreateReminderFromTextUseCase,
    private val createReminderUseCase: CreateReminderUseCase,
    private val updateReminderStatusUseCase: UpdateReminderStatusUseCase,
    private val snoozeReminderUseCase: SnoozeReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeRemindersUseCase().collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
    }

    fun updateNaturalText(text: String) = _uiState.update { it.copy(naturalText = text) }
    fun updateTitle(text: String) = _uiState.update { it.copy(titleDraft = text) }
    fun updateDescription(text: String) = _uiState.update { it.copy(descriptionDraft = text) }
    fun updateScheduledAt(epochMillis: Long) = _uiState.update { it.copy(scheduledAt = epochMillis) }
    fun consumeSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun createFromText() {
        viewModelScope.launch {
            when (val result = createReminderFromTextUseCase(_uiState.value.naturalText)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(naturalText = "", snackbarMessage = "Reminder created from natural language.")
                }

                is AppResult.Error -> _uiState.update { it.copy(snackbarMessage = result.error.message) }
            }
        }
    }

    fun createManual() {
        viewModelScope.launch {
            when (val result = createReminderUseCase(_uiState.value.titleDraft, _uiState.value.descriptionDraft, _uiState.value.scheduledAt)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(titleDraft = "", descriptionDraft = "", snackbarMessage = "Reminder scheduled locally.")
                }

                is AppResult.Error -> _uiState.update { it.copy(snackbarMessage = result.error.message) }
            }
        }
    }

    fun complete(reminderTask: ReminderTask) {
        viewModelScope.launch { updateReminderStatusUseCase(reminderTask, ReminderStatus.COMPLETED) }
    }

    fun snooze(reminderTask: ReminderTask) {
        viewModelScope.launch { snoozeReminderUseCase(reminderTask) }
    }

    fun delete(reminderId: String) {
        viewModelScope.launch { deleteReminderUseCase(reminderId) }
    }
}

@Composable
fun RemindersRoute(
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RemindersScreen(
        state = state,
        onNaturalTextChange = viewModel::updateNaturalText,
        onTitleChange = viewModel::updateTitle,
        onDescriptionChange = viewModel::updateDescription,
        onScheduledAtChange = viewModel::updateScheduledAt,
        onCreateFromText = viewModel::createFromText,
        onCreateManual = viewModel::createManual,
        onComplete = viewModel::complete,
        onSnooze = viewModel::snooze,
        onDelete = viewModel::delete,
        onConsumeSnackbar = viewModel::consumeSnackbar,
        modifier = modifier,
    )
}

@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onNaturalTextChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onScheduledAtChange: (Long) -> Unit,
    onCreateFromText: () -> Unit,
    onCreateManual: () -> Unit,
    onComplete: (ReminderTask) -> Unit,
    onSnooze: (ReminderTask) -> Unit,
    onDelete: (String) -> Unit,
    onConsumeSnackbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeSnackbar()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Reminders & Tasks") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionCard(
                    title = "Natural language",
                    subtitle = "Examples: remind me tomorrow at 7 pm to study physics, call mom on Sunday, wake me up at 6.",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.naturalText,
                            onValueChange = onNaturalTextChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Type a reminder request") },
                            minLines = 2,
                        )
                        TextButton(onClick = onCreateFromText, enabled = state.naturalText.isNotBlank()) {
                            Text("Create from text")
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Manual reminder", subtitle = "Create a local notification manually when needed.") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = state.titleDraft, onValueChange = onTitleChange, modifier = Modifier.fillMaxWidth(), label = { Text("Title") })
                        OutlinedTextField(value = state.descriptionDraft, onValueChange = onDescriptionChange, modifier = Modifier.fillMaxWidth(), label = { Text("Description") })
                        Text("Scheduled: ${TimeFormatters.formatDateTime(state.scheduledAt)}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    val current = Instant.ofEpochMilli(state.scheduledAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val updated = current.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                                            onScheduledAtChange(updated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                                        },
                                        current.year,
                                        current.monthValue - 1,
                                        current.dayOfMonth,
                                    ).show()
                                },
                            ) { Text("Pick date") }
                            TextButton(
                                onClick = {
                                    val current = Instant.ofEpochMilli(state.scheduledAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val updated = LocalDateTime.of(current.toLocalDate(), LocalTime.of(hour, minute))
                                            onScheduledAtChange(updated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                                        },
                                        current.hour,
                                        current.minute,
                                        false,
                                    ).show()
                                },
                            ) { Text("Pick time") }
                            TextButton(onClick = onCreateManual, enabled = state.titleDraft.isNotBlank()) { Text("Save") }
                        }
                    }
                }
            }
            if (state.reminders.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No reminders yet",
                        body = "Create reminders with natural language or schedule one manually to see them here.",
                    )
                }
            } else {
                items(state.reminders, key = { it.id }) { reminder ->
                    SectionCard(
                        title = reminder.title,
                        subtitle = "${TimeFormatters.formatDateTime(reminder.scheduledAt)} | ${reminder.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    ) {
                        Text(
                            reminder.description.ifBlank { "Scheduled from ${reminder.sourceText.ifBlank { "manual entry" }}." },
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onComplete(reminder) }) {
                                Icon(Icons.Rounded.CheckCircleOutline, contentDescription = "Complete")
                            }
                            IconButton(onClick = { onSnooze(reminder) }) {
                                Icon(Icons.Rounded.Snooze, contentDescription = "Snooze")
                            }
                            IconButton(onClick = { onDelete(reminder.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete")
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Rounded.Alarm, contentDescription = "Reminder")
                            }
                        }
                    }
                }
            }
        }
    }
}
