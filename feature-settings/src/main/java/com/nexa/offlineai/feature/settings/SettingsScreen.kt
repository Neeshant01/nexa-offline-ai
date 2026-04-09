@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nexa.offlineai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nexa.offlineai.domain.model.AccentOption
import com.nexa.offlineai.domain.model.AssistantSettings
import com.nexa.offlineai.domain.model.ModelStatus
import com.nexa.offlineai.domain.model.ThemeMode
import com.nexa.offlineai.domain.usecase.ClearAllDataUseCase
import com.nexa.offlineai.domain.usecase.ObserveModelStatusUseCase
import com.nexa.offlineai.domain.usecase.ObserveSettingsUseCase
import com.nexa.offlineai.domain.usecase.UpdateSettingsUseCase
import com.nexa.offlineai.coreui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AssistantSettings = AssistantSettings(),
    val modelStatus: ModelStatus? = null,
    val snackbarMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    observeModelStatusUseCase: ObserveModelStatusUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val clearAllDataUseCase: ClearAllDataUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            observeModelStatusUseCase().collect { modelStatus ->
                _uiState.update { it.copy(modelStatus = modelStatus) }
            }
        }
    }

    fun updateSettings(transform: (AssistantSettings) -> AssistantSettings) {
        viewModelScope.launch {
            updateSettingsUseCase(transform(_uiState.value.settings))
        }
    }

    fun clearData() {
        viewModelScope.launch {
            clearAllDataUseCase()
            _uiState.update { it.copy(snackbarMessage = "Local conversations, notes, and reminders were cleared.") }
        }
    }

    fun showExportPlaceholder() {
        _uiState.update { it.copy(snackbarMessage = "Export placeholder: connect encrypted local export or SAF document writer here.") }
    }

    fun consumeSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
}

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onUpdate = viewModel::updateSettings,
        onClearData = viewModel::clearData,
        onExport = viewModel::showExportPlaceholder,
        onConsumeSnackbar = viewModel::consumeSnackbar,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onUpdate: ((AssistantSettings) -> AssistantSettings) -> Unit,
    onClearData: () -> Unit,
    onExport: () -> Unit,
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
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionCard(title = "Personalization", subtitle = "Make Nexa feel like your daily assistant.") {
                    OutlinedTextField(
                        value = state.settings.assistantName,
                        onValueChange = { onUpdate { current -> current.copy(assistantName = it) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Assistant name") },
                    )
                    Text("Theme mode")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { theme ->
                            FilterChip(
                                selected = state.settings.themeMode == theme,
                                onClick = { onUpdate { current -> current.copy(themeMode = theme) } },
                                label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    Text("Accent")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentOption.entries.forEach { accent ->
                            FilterChip(
                                selected = state.settings.accentOption == accent,
                                onClick = { onUpdate { current -> current.copy(accentOption = accent) } },
                                label = { Text(accent.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Speech", subtitle = "Voice input and spoken replies stay on device when supported.") {
                    SettingToggle("Enable TTS", state.settings.ttsEnabled) { checked ->
                        onUpdate { current -> current.copy(ttsEnabled = checked) }
                    }
                    Text("Speech rate: ${"%.2f".format(state.settings.speechRate)}")
                    Slider(
                        value = state.settings.speechRate,
                        onValueChange = { value -> onUpdate { current -> current.copy(speechRate = value) } },
                        valueRange = 0.5f..1.5f,
                    )
                }
            }
            item {
                SectionCard(title = "AI Provider", subtitle = "Use Mock for instant offline use, then switch to Local Gemma after connecting a real runtime.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.nexa.offlineai.domain.model.ProviderType.entries.forEach { provider ->
                            FilterChip(
                                selected = state.settings.providerType == provider,
                                onClick = { onUpdate { current -> current.copy(providerType = provider) } },
                                label = { Text(provider.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.settings.modelPath,
                        onValueChange = { onUpdate { current -> current.copy(modelPath = it) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model path") },
                        supportingText = { Text("Point this to your quantized Gemma bundle or task file after wiring a runtime bridge.") },
                    )
                    OutlinedTextField(
                        value = state.settings.quantizedModel,
                        onValueChange = { onUpdate { current -> current.copy(quantizedModel = it) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Quantized model preset") },
                    )
                    OutlinedTextField(
                        value = state.settings.contextLength.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { onUpdate { current -> current.copy(contextLength = it) } } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Context length") },
                    )
                    OutlinedTextField(
                        value = state.settings.maxOutputTokens.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { onUpdate { current -> current.copy(maxOutputTokens = it) } } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Max output tokens") },
                    )
                    Text("Temperature: ${"%.2f".format(state.settings.temperature)}")
                    Slider(
                        value = state.settings.temperature,
                        onValueChange = { value -> onUpdate { current -> current.copy(temperature = value) } },
                        valueRange = 0.1f..1.2f,
                    )
                    SettingToggle("Offline-only mode", state.settings.offlineOnly) { checked ->
                        onUpdate { current -> current.copy(offlineOnly = checked) }
                    }
                    if (state.settings.providerType == com.nexa.offlineai.domain.model.ProviderType.LOCAL_GEMMA) {
                        Text(
                            text = "Local Gemma needs both a readable model file and a connected runtime adapter. If either one is missing, Nexa will use Mock replies so the app stays usable.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
            }
            item {
                SectionCard(title = "Diagnostics", subtitle = "Useful while connecting a real on-device Gemma runtime.") {
                    Text("Provider: ${state.settings.providerType.name}")
                    Text("Model state: ${state.modelStatus?.state ?: "Unknown"}")
                    Text(
                        state.modelStatus?.detail ?: "Waiting for provider status.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
            item {
                SectionCard(title = "Privacy & Data", subtitle = "No login is required for the core assistant.") {
                    Text("Core data stays local: conversations, reminders, notes, and settings are persisted on-device.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onExport) { Text("Export conversations") }
                        TextButton(onClick = onClearData) { Text("Clear local data") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
