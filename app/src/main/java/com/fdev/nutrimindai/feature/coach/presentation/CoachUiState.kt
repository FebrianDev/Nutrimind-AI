package com.fdev.nutrimindai.feature.coach.presentation

import android.graphics.Bitmap
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.feature.coach.domain.model.ChatMessage
import com.fdev.nutrimindai.feature.coach.domain.model.ContextualPrompt

data class CoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val inputText: String = "",
    val activeProvider: AiProvider = AiProvider.META_LLAMA,
    val contextualPrompts: List<ContextualPrompt> = emptyList(),
    val isRecordingVoice: Boolean = false,
    val isTranscribingVoice: Boolean = false,
    val voiceDurationSeconds: Int = 0,
    val voiceAmplitude: Float = 0f,
    val selectedImageUri: String? = null,
    val selectedImageBitmap: Bitmap? = null
)
