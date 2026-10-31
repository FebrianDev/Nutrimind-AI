package com.fdev.nutrimindai.feature.coach.domain.model

import java.util.UUID

enum class ChatRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSummary: String? = null,
    val imageUri: String? = null
)
