@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nexa.offlineai.feature.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.core.time.TimeFormatters
import com.nexa.offlineai.coreui.components.MarkdownText
import com.nexa.offlineai.domain.model.ChatMessage
import com.nexa.offlineai.domain.model.MessageRole
import com.nexa.offlineai.domain.model.MessageState
import com.nexa.offlineai.domain.usecase.DeleteMessageUseCase
import com.nexa.offlineai.domain.usecase.ObserveMessagesUseCase
import com.nexa.offlineai.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversationId: String? = null,
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
) : ViewModel() {
    private val conversationId = MutableStateFlow(savedStateHandle.get<String?>("conversationId"))
    private val _uiState = MutableStateFlow(ChatUiState(conversationId = conversationId.value))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            conversationId
                .flatMapLatest { id -> id?.let(observeMessagesUseCase::invoke) ?: emptyFlow<List<ChatMessage>>() }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages, conversationId = conversationId.value) }
                }
        }
    }

    fun updateDraft(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    fun sendCurrent() {
        val message = _uiState.value.draft.trim()
        if (message.isBlank()) return
        sendMessage(message)
    }

    fun retryLast() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.role == MessageRole.USER } ?: return
        sendMessage(lastUserMessage.content)
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { deleteMessageUseCase(messageId) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun sendMessage(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, draft = "", errorMessage = null) }
            when (val result = sendMessageUseCase(_uiState.value.conversationId, text)) {
                is AppResult.Success -> {
                    conversationId.value = result.value
                    savedStateHandle["conversationId"] = result.value
                    _uiState.update { it.copy(isSending = false, conversationId = result.value) }
                }

                is AppResult.Error -> {
                    _uiState.update { it.copy(isSending = false, errorMessage = result.error.message) }
                }
            }
        }
    }
}

@Composable
fun ChatRoute(
    onOpenVoice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onDraftChange = viewModel::updateDraft,
        onSend = viewModel::sendCurrent,
        onRetry = viewModel::retryLast,
        onDelete = viewModel::deleteMessage,
        onOpenVoice = onOpenVoice,
        onClearError = viewModel::clearError,
        modifier = modifier,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onDelete: (String) -> Unit,
    onOpenVoice: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.conversationId == null) "New chat" else "Assistant chat") },
                actions = {
                    IconButton(onClick = onOpenVoice) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Voice")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Nexa anything...") },
                    shape = RoundedCornerShape(20.dp),
                )
                IconButton(onClick = onOpenVoice) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Voice")
                }
                IconButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.isSending) {
                    Icon(Icons.Rounded.Send, contentDescription = "Send")
                }
            }
        },
    ) { innerPadding ->
        if (state.messages.isEmpty() && !state.isSending) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    "Start a private conversation. Messages are stored locally and routed to the selected AI provider.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        onCopy = { clipboard.setText(AnnotatedString(message.content)) },
                        onShare = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, message.content)
                                    },
                                    "Share reply",
                                ),
                            )
                        },
                        onDelete = { onDelete(message.id) },
                        onRetry = onRetry,
                    )
                }
                if (state.isSending) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Generating response...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
) {
    val isUser = message.role == MessageRole.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.94f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                },
            ),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isUser) {
                    Text(message.content, style = MaterialTheme.typography.bodyLarge)
                } else {
                    MarkdownText(text = message.content.ifBlank { "Thinking..." })
                }
                Text(
                    text = TimeFormatters.formatTime(message.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                if (!isUser) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallAction(Icons.Rounded.ContentCopy, "Copy", onCopy)
                        SmallAction(Icons.Rounded.Share, "Share", onShare)
                        SmallAction(Icons.Rounded.DeleteOutline, "Delete", onDelete)
                        if (message.state == MessageState.FAILED) {
                            SmallAction(Icons.Rounded.Refresh, "Retry", onRetry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    var showLabel by remember { mutableStateOf(false) }
    TextButton(onClick = {
        showLabel = !showLabel
        onClick()
    }) {
        Icon(icon, contentDescription = label)
        if (showLabel) {
            Text(label, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
