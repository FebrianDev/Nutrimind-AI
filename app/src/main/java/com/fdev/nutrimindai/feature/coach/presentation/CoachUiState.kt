package com.fdev.nutrimindai.feature.coach.presentation

import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.feature.coach.domain.model.ChatMessage

data class CoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val inputText: String = "",
    val activeProvider: AiProvider = AiProvider.META_LLAMA,
    val quickPromptResIds: List<Int> = listOf(
        R.string.coach_prompt_dinner,
        R.string.coach_prompt_macros,
        R.string.coach_prompt_hydration,
        R.string.coach_prompt_workout,
        R.string.coach_prompt_goal
    )
)
