package com.nexa.offlineai.domain.model

data class HomeSnapshot(
    val settings: AssistantSettings,
    val modelStatus: ModelStatus,
    val recentConversations: List<Conversation>,
    val reminders: List<ReminderTask>,
    val pinnedNote: Note?,
    val greeting: String,
)
