package com.nexa.offlineai.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
