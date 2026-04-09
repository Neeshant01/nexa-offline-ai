package com.nexa.offlineai.data.local.mappers

import com.nexa.offlineai.data.local.entity.ChatMessageEntity
import com.nexa.offlineai.data.local.entity.ConversationEntity
import com.nexa.offlineai.data.local.entity.NoteEntity
import com.nexa.offlineai.data.local.entity.ReminderTaskEntity
import com.nexa.offlineai.domain.model.ChatMessage
import com.nexa.offlineai.domain.model.Conversation
import com.nexa.offlineai.domain.model.MessageRole
import com.nexa.offlineai.domain.model.MessageState
import com.nexa.offlineai.domain.model.Note
import com.nexa.offlineai.domain.model.ReminderStatus
import com.nexa.offlineai.domain.model.ReminderTask

fun ConversationEntity.asDomain() = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    lastMessagePreview = lastMessagePreview,
)

fun Conversation.asEntity() = ConversationEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    lastMessagePreview = lastMessagePreview,
)

fun ChatMessageEntity.asDomain() = ChatMessage(
    id = id,
    conversationId = conversationId,
    role = MessageRole.valueOf(role),
    content = content,
    createdAt = createdAt,
    state = MessageState.valueOf(state),
)

fun ChatMessage.asEntity() = ChatMessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    createdAt = createdAt,
    state = state.name,
)

fun NoteEntity.asDomain() = Note(
    id = id,
    title = title,
    content = content,
    tags = tags.split("|").filter { it.isNotBlank() },
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Note.asEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    tags = tags.joinToString("|"),
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ReminderTaskEntity.asDomain() = ReminderTask(
    id = id,
    title = title,
    description = description,
    scheduledAt = scheduledAt,
    createdAt = createdAt,
    status = ReminderStatus.valueOf(status),
    sourceText = sourceText,
    confidence = confidence,
)

fun ReminderTask.asEntity() = ReminderTaskEntity(
    id = id,
    title = title,
    description = description,
    scheduledAt = scheduledAt,
    createdAt = createdAt,
    status = status.name,
    sourceText = sourceText,
    confidence = confidence,
)
