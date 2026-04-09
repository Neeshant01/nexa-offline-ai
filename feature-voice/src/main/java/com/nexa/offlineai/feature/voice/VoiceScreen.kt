@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nexa.offlineai.feature.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nexa.offlineai.core.common.AppResult
import com.nexa.offlineai.coreui.components.AssistantOrb
import com.nexa.offlineai.coreui.components.SectionCard
import com.nexa.offlineai.domain.usecase.ObserveSettingsUseCase
import com.nexa.offlineai.domain.usecase.QuickAssistantReplyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceUiState(
    val transcript: String = "",
    val reply: String = "",
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val snackbarMessage: String? = null,
    val ttsEnabled: Boolean = true,
    val speechRate: Float = 1f,
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val quickAssistantReplyUseCase: QuickAssistantReplyUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(ttsEnabled = settings.ttsEnabled, speechRate = settings.speechRate)
                }
            }
        }
    }

    fun setListening(value: Boolean) = _uiState.update { it.copy(isListening = value) }
    fun updateTranscript(text: String) = _uiState.update { it.copy(transcript = text) }
    fun consumeSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun sendTranscript() {
        val transcript = _uiState.value.transcript.trim()
        if (transcript.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, reply = "") }
            when (val result = quickAssistantReplyUseCase(transcript)) {
                is AppResult.Success -> _uiState.update { it.copy(isProcessing = false, reply = result.value) }
                is AppResult.Error -> _uiState.update { it.copy(isProcessing = false, snackbarMessage = result.error.message) }
            }
        }
    }
}

@Composable
fun VoiceRoute(
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VoiceScreen(
        state = state,
        onListeningChange = viewModel::setListening,
        onTranscript = viewModel::updateTranscript,
        onSendTranscript = viewModel::sendTranscript,
        onConsumeSnackbar = viewModel::consumeSnackbar,
        modifier = modifier,
    )
}

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onListeningChange: (Boolean) -> Unit,
    onTranscript: (String) -> Unit,
    onSendTranscript: () -> Unit,
    onConsumeSnackbar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer?.startListening(recognizerIntent)
            onListeningChange(true)
        } else {
            onListeningChange(false)
            onTranscript("Microphone permission is required for voice input.")
        }
    }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        onListeningChange(false)
                    }

                    override fun onError(error: Int) {
                        onListeningChange(false)
                    }

                    override fun onResults(results: Bundle?) {
                        val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        onTranscript(transcript)
                        onListeningChange(false)
                        onSendTranscript()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val transcript = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        onTranscript(transcript)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }

        onDispose {
            recognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        }
    }

    LaunchedEffect(state.reply, state.ttsEnabled, state.speechRate) {
        if (state.reply.isNotBlank() && state.ttsEnabled) {
            tts?.setSpeechRate(state.speechRate)
            tts?.speak(state.reply, TextToSpeech.QUEUE_FLUSH, null, "nexa-reply")
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeSnackbar()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Voice Assistant") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.isListening) {
                        recognizer?.stopListening()
                        onListeningChange(false)
                    } else {
                        when {
                            !SpeechRecognizer.isRecognitionAvailable(context) -> onTranscript("Speech recognition is unavailable on this device.")
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                                recognizer?.startListening(recognizerIntent)
                                onListeningChange(true)
                            }
                            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
            ) {
                Icon(
                    if (state.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = "Toggle listening",
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ListeningOrb(isListening = state.isListening)
            SectionCard(title = "Transcript", subtitle = "Offline speech recognition when supported by the device.") {
                Text(state.transcript.ifBlank { "Tap the mic and start speaking." })
            }
            SectionCard(
                title = "Assistant reply",
                subtitle = if (state.isProcessing) "Generating response..." else "Spoken aloud when TTS is enabled.",
            ) {
                if (state.isProcessing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                        Text("Thinking locally...")
                    }
                } else {
                    Text(state.reply.ifBlank { "Your voice reply will appear here." })
                }
            }
        }
    }
}

@Composable
private fun ListeningOrb(isListening: Boolean) {
    val transition = rememberInfiniteTransition(label = "voice-orb")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = if (isListening) 1f else 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "voice-alpha",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.48f), MaterialTheme.shapes.large)
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.alpha(alpha)) {
                AssistantOrb(modifier = Modifier.size(110.dp), pulse = isListening)
            }
            Text(if (isListening) "Listening..." else "Tap the mic to start")
        }
    }
}
