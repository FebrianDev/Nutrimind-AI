package com.fdev.nutrimindai.feature.coach.domain.model

/**
 * Represents a dynamic, context-aware suggestion chip for the NutriCoach chat.
 *
 * @property id Unique identifier for the prompt
 * @property title Concise display title shown inside the chip or card
 * @property promptText Complete, conversational question or command sent to Meta AI
 * @property emoji Contextual emoji icon representing the topic
 */
data class ContextualPrompt(
    val id: String,
    val title: String,
    val promptText: String,
    val emoji: String = "✨"
)
