package com.nexa.offlineai.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nexa.offlineai.core.time.TimeFormatters
import com.nexa.offlineai.coreui.components.AssistantOrb
import com.nexa.offlineai.coreui.components.EmptyStateCard
import com.nexa.offlineai.coreui.components.GradientScreen
import com.nexa.offlineai.coreui.components.LoadingPlaceholder
import com.nexa.offlineai.coreui.components.SectionCard
import com.nexa.offlineai.coreui.components.StatusChip
import com.nexa.offlineai.domain.model.HomeSnapshot
import com.nexa.offlineai.domain.usecase.ObserveHomeSnapshotUseCase
import com.nexa.offlineai.domain.usecase.WarmUpAssistantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val snapshot: HomeSnapshot? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeSnapshotUseCase: ObserveHomeSnapshotUseCase,
    private val warmUpAssistantUseCase: WarmUpAssistantUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeHomeSnapshotUseCase().collect { snapshot ->
                _uiState.update { it.copy(loading = false, snapshot = snapshot) }
            }
        }
    }

    fun refreshModel() {
        viewModelScope.launch { warmUpAssistantUseCase() }
    }
}

@Composable
fun HomeRoute(
    onOpenChat: (String?) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenVoice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshModel() }
    HomeScreen(
        state = state,
        onRefreshModel = viewModel::refreshModel,
        onOpenChat = onOpenChat,
        onOpenNotes = onOpenNotes,
        onOpenReminders = onOpenReminders,
        onOpenVoice = onOpenVoice,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefreshModel: () -> Unit,
    onOpenChat: (String?) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GradientScreen(modifier = modifier) {
        if (state.loading || state.snapshot == null) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
            ) {
                repeat(4) {
                    item { LoadingPlaceholder() }
                }
            }
        } else {
            val snapshot = state.snapshot
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
            ) {
                item {
                    SectionCard(title = snapshot.greeting, subtitle = "Private by default. On-device whenever possible.") {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AssistantOrb()
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatusChip(
                                    text = if (snapshot.settings.offlineOnly) "Offline ready" else "Hybrid ready",
                                    brush = Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary,
                                        ),
                                    ),
                                )
                                Text(
                                    text = snapshot.modelStatus.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                )
                                Button(onClick = onRefreshModel) {
                                    Text(if (snapshot.modelStatus.isReady) "Reload model" else "Check local model")
                                }
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = "Quick actions", subtitle = "Jump back into your daily assistant flow.") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                QuickAction("Chat", Icons.Rounded.Chat) { onOpenChat(null) }
                                QuickAction("Voice", Icons.Rounded.Mic, onOpenVoice)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                QuickAction("Notes", Icons.Rounded.NoteAlt, onOpenNotes)
                                QuickAction("Reminders", Icons.Rounded.NotificationsActive, onOpenReminders)
                            }
                        }
                    }
                }
                item {
                    BoxWithConstraints {
                        val wide = maxWidth > 680.dp
                        if (wide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    RecentChatsSection(snapshot = snapshot, onOpenChat = onOpenChat)
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    RemindersSection(snapshot = snapshot)
                                    PinnedNoteSection(snapshot = snapshot, onOpenNotes = onOpenNotes)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                RecentChatsSection(snapshot = snapshot, onOpenChat = onOpenChat)
                                RemindersSection(snapshot = snapshot)
                                PinnedNoteSection(snapshot = snapshot, onOpenNotes = onOpenNotes)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RecentChatsSection(snapshot: HomeSnapshot, onOpenChat: (String?) -> Unit) {
    if (snapshot.recentConversations.isEmpty()) {
        EmptyStateCard(title = "No chats yet", body = "Start a local conversation and Nexa will keep the history on-device.")
        return
    }
    SectionCard(title = "Recent chats", subtitle = "Persistent local history with auto-titled threads.") {
        snapshot.recentConversations.forEach { conversation ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenChat(conversation.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(conversation.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        conversation.lastMessagePreview.ifBlank { "Tap to continue the conversation." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindersSection(snapshot: HomeSnapshot) {
    SectionCard(title = "Today's reminders", subtitle = "Local notifications stay active even offline.") {
        if (snapshot.reminders.isEmpty()) {
            Text("No reminders due soon. Add one from natural language or create it manually.")
        } else {
            snapshot.reminders.forEach { reminder ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            TimeFormatters.formatDateTime(reminder.scheduledAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PinnedNoteSection(snapshot: HomeSnapshot, onOpenNotes: () -> Unit) {
    val note = snapshot.pinnedNote
    if (note == null) {
        EmptyStateCard(
            title = "Pin a note",
            body = "Keep a priority note on the home screen for quick access.",
        )
        return
    }
    SectionCard(
        title = "Pinned note",
        subtitle = note.tags.joinToString(" | ").ifBlank { "Smart Notes" },
        modifier = Modifier.clickable(onClick = onOpenNotes),
    ) {
        Text(note.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = note.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
    }
}
