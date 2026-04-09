package com.nexa.offlineai.coreui.preview

import com.nexa.offlineai.core.common.IdFactory
import com.nexa.offlineai.domain.model.ChatMessage
import com.nexa.offlineai.domain.model.MessageRole
import com.nexa.offlineai.domain.model.Note
import com.nexa.offlineai.domain.model.ReminderStatus
import com.nexa.offlineai.domain.model.ReminderTask

object PreviewFixtures {
    val messages = listOf(
        ChatMessage(
            id = IdFactory.newId(),
            conversationId = "preview",
            role = MessageRole.USER,
            content = "Summarize my study plan for tonight.",
            createdAt = System.currentTimeMillis(),
        ),
        ChatMessage(
            id = IdFactory.newId(),
            conversationId = "preview",
            role = MessageRole.ASSISTANT,
            content = "**Tonight**: revise physics numericals, finish chemistry notes, and set a reminder for 10 pm review.",
            createdAt = System.currentTimeMillis(),
        ),
    )

    val note = Note(
        id = IdFactory.newId(),
        title = "Physics sprint",
        content = "Finish wave optics chapter. Solve 15 numericals. Review derivations before sleep.",
        tags = listOf("study", "physics"),
        isPinned = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    val reminder = ReminderTask(
        id = IdFactory.newId(),
        title = "Study physics",
        description = "Wave optics revision block",
        scheduledAt = System.currentTimeMillis() + 2_700_000L,
        createdAt = System.currentTimeMillis(),
        status = ReminderStatus.UPCOMING,
        confidence = 0.95f,
    )
}
