package com.fdev.nutrimindai.feature.coach.domain.repository

import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.feature.coach.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CoachRepository {
    fun getAllMessages(): Flow<List<ChatMessage>>
    suspend fun sendMessage(
        userText: String,
        imageUri: String? = null,
        imageBitmap: android.graphics.Bitmap? = null
    ): Result<String>
    suspend fun clearHistory()
    fun getActiveProvider(): AiProvider
    fun setActiveProvider(provider: AiProvider)
    val activeProviderFlow: StateFlow<AiProvider>
    suspend fun transcribeVoice(audioFile: java.io.File): Result<String>
}
