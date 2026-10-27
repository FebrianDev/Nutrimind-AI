package com.fdev.nutrimindai.feature.coach.domain.model

enum class ChatRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: Long = 0,
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
