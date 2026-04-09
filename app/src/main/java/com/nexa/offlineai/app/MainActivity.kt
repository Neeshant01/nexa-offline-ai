package com.nexa.offlineai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexa.offlineai.app.ui.NexaAppRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val state = mainViewModel.uiState.collectAsStateWithLifecycle().value
            splash.setKeepOnScreenCondition { state.isLoading }
            NexaAppRoot(
                state = state,
                onBootstrap = mainViewModel::bootstrapIfNeeded,
                onFinishOnboarding = mainViewModel::completeOnboarding,
            )
        }
    }
}
