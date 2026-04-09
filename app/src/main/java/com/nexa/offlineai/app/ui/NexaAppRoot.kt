package com.nexa.offlineai.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import com.nexa.offlineai.app.MainUiState
import com.nexa.offlineai.coreui.components.AssistantOrb
import com.nexa.offlineai.coreui.components.GradientScreen
import com.nexa.offlineai.coreui.components.SectionCard
import com.nexa.offlineai.coreui.theme.NexaTheme
import com.nexa.offlineai.domain.model.AccentOption
import com.nexa.offlineai.domain.model.ProviderType
import com.nexa.offlineai.domain.model.ThemeMode
import com.nexa.offlineai.navigation.NexaNavShell
import kotlinx.coroutines.launch

@Composable
fun NexaAppRoot(
    state: MainUiState,
    onBootstrap: () -> Unit,
    onFinishOnboarding: (String, AccentOption, ProviderType) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val darkTheme = when (state.settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    NexaTheme(accentOption = state.settings.accentOption, darkTheme = darkTheme) {
        if (state.settings.onboardingCompleted) {
            LaunchedEffect(Unit) {
                onBootstrap()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            NexaNavShell()
        } else {
            OnboardingScreen(onComplete = onFinishOnboarding, initialName = state.settings.assistantName)
        }
    }
}

@Composable
private fun OnboardingScreen(
    onComplete: (String, AccentOption, ProviderType) -> Unit,
    initialName: String,
) {
    var assistantName by rememberSaveable { mutableStateOf(initialName.ifBlank { "Nexa" }) }
    var accent by rememberSaveable { mutableStateOf(AccentOption.AURORA) }
    var provider by rememberSaveable { mutableStateOf(ProviderType.MOCK) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp > 700

    GradientScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide) 72.dp else 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Nexa Offline AI", style = MaterialTheme.typography.headlineMedium)
                AssistantOrb(modifier = Modifier.size(76.dp))
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> IntroPage(
                        title = "Private by design",
                        body = "No login for core use. Your chats, notes, reminders, and settings stay on your device by default.",
                    )
                    1 -> IntroPage(
                        title = "Offline first",
                        body = "Local Gemma is supported through a runtime bridge. Until that is connected, Nexa stays fully usable with offline storage, reminders, notes, and the built-in mock assistant.",
                    )
                    else -> SectionCard(
                        title = "Personalize your assistant",
                        subtitle = "Choose how Nexa looks and which AI engine to start with.",
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = assistantName,
                                onValueChange = { assistantName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Assistant name") },
                            )
                            Text("Accent color")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AccentOption.entries.forEach { option ->
                                    FilterChip(
                                        selected = accent == option,
                                        onClick = { accent = option },
                                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    )
                                }
                            }
                            Text("AI mode")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProviderType.entries.forEach { option ->
                                    FilterChip(
                                        selected = provider == option,
                                        onClick = { provider = option },
                                        label = { Text(option.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Step ${pagerState.currentPage + 1} of 3", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f))
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete(assistantName, accent, provider)
                        }
                    },
                ) {
                    Text(if (pagerState.currentPage < 2) "Next" else "Enter Nexa")
                }
            }
        }
    }
}

@Composable
private fun IntroPage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {
        AssistantOrb(modifier = Modifier.size(96.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChipPill("Offline ready")
            ChipPill("Privacy first")
            ChipPill("Gemma-ready")
        }
    }
}

@Composable
private fun ChipPill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                    ),
                ),
                MaterialTheme.shapes.large,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
