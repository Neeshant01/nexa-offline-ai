package com.nexa.offlineai.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexa.offlineai.feature.chat.ChatRoute
import com.nexa.offlineai.feature.home.HomeRoute
import com.nexa.offlineai.feature.notes.NotesRoute
import com.nexa.offlineai.feature.reminders.RemindersRoute
import com.nexa.offlineai.feature.settings.SettingsRoute
import com.nexa.offlineai.feature.voice.VoiceRoute

sealed class NexaDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : NexaDestination("home", "Home", Icons.Rounded.Home)
    data object Chat : NexaDestination("chat", "Chat", Icons.Rounded.Chat) {
        const val conversationArg = "conversationId"
        const val routePattern = "chat?conversationId={conversationId}"
        fun createRoute(conversationId: String? = null): String =
            if (conversationId.isNullOrBlank()) route else "$route?$conversationArg=$conversationId"
    }
    data object Notes : NexaDestination("notes", "Notes", Icons.Rounded.NoteAlt)
    data object Reminders : NexaDestination("reminders", "Reminders", Icons.Rounded.NotificationsActive)
    data object Voice : NexaDestination("voice", "Voice", Icons.Rounded.Mic)
    data object Settings : NexaDestination("settings", "Settings", Icons.Rounded.Settings)
}

private val bottomDestinations = listOf(
    NexaDestination.Home,
    NexaDestination.Chat,
    NexaDestination.Notes,
    NexaDestination.Reminders,
    NexaDestination.Settings,
)

@Composable
fun NexaNavShell(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NexaDestination.Home.route

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute.startsWith(destination.route),
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NexaDestination.Home.route,
            modifier = Modifier,
        ) {
            composable(NexaDestination.Home.route) {
                HomeRoute(
                    modifier = Modifier.padding(innerPadding),
                    onOpenChat = { conversationId -> navController.navigate(NexaDestination.Chat.createRoute(conversationId)) },
                    onOpenNotes = { navController.navigate(NexaDestination.Notes.route) },
                    onOpenReminders = { navController.navigate(NexaDestination.Reminders.route) },
                    onOpenVoice = { navController.navigate(NexaDestination.Voice.route) },
                )
            }
            composable(
                route = NexaDestination.Chat.routePattern,
                arguments = listOf(
                    navArgument(NexaDestination.Chat.conversationArg) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                ChatRoute(
                    modifier = Modifier.padding(innerPadding),
                    onOpenVoice = { navController.navigate(NexaDestination.Voice.route) },
                )
            }
            composable(NexaDestination.Notes.route) {
                NotesRoute(modifier = Modifier.padding(innerPadding))
            }
            composable(NexaDestination.Reminders.route) {
                RemindersRoute(modifier = Modifier.padding(innerPadding))
            }
            composable(NexaDestination.Voice.route) {
                VoiceRoute(modifier = Modifier.padding(innerPadding))
            }
            composable(NexaDestination.Settings.route) {
                SettingsRoute(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
