package com.fdev.nutrimindai.feature.coach.data.repository

import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.feature.coach.data.local.dao.ChatMessageDao
import com.fdev.nutrimindai.feature.coach.data.local.entity.ChatMessageEntity
import com.fdev.nutrimindai.feature.coach.domain.CoachActionExecutor
import com.fdev.nutrimindai.feature.coach.domain.CoachActionParser
import com.fdev.nutrimindai.feature.coach.domain.NutriCoachContextBuilder
import com.fdev.nutrimindai.feature.coach.domain.model.ChatMessage
import com.fdev.nutrimindai.feature.coach.domain.model.ChatRole
import com.fdev.nutrimindai.feature.coach.domain.repository.CoachRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val aiRepository: AiRepository,
    private val contextBuilder: NutriCoachContextBuilder,
    private val actionParser: CoachActionParser,
    private val actionExecutor: CoachActionExecutor
) : CoachRepository {

    override val activeProviderFlow: StateFlow<AiProvider> = aiRepository.activeProviderFlow

    override fun getActiveProvider(): AiProvider = aiRepository.getActiveProvider()

    override fun setActiveProvider(provider: AiProvider) {
        aiRepository.setActiveProvider(provider)
    }

    override fun getAllMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages().map { list ->
            list.map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = if (entity.role.equals("user", ignoreCase = true)) ChatRole.USER else ChatRole.ASSISTANT,
                    content = entity.content,
                    timestamp = entity.timestamp,
                    actionSummary = entity.actionSummary,
                    imageUri = entity.imageUri
                )
            }
        }
    }

    override suspend fun sendMessage(
        userText: String,
        imageUri: String?,
        imageBitmap: android.graphics.Bitmap?
    ): Result<String> {
        val trimmed = userText.trim()
        if (trimmed.isBlank() && imageBitmap == null) {
            return Result.failure(IllegalArgumentException("Message cannot be blank"))
        }

        val effectiveContent = if (trimmed.isBlank() && imageBitmap != null) {
            "Please analyze this meal/menu image for nutrition and advice."
        } else {
            trimmed
        }

        // 1. Save user message to Room DB
        val userEntity = ChatMessageEntity(
            role = "user",
            content = effectiveContent,
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri
        )
        chatMessageDao.insertMessage(userEntity)

        // 2. Build live context system prompt with current provider's name
        val provider = aiRepository.getActiveProvider()
        val systemPrompt = try {
            contextBuilder.buildSystemPrompt(aiProviderName = provider.displayName)
        } catch (e: Exception) {
            "You are NutriMind AI, an autonomous personal nutrition and activity tracking agent powered by ${provider.displayName}."
        }

        // 3. Retrieve recent history for multi-turn conversational context
        val recentDesc = chatMessageDao.getRecentMessagesDesc(limit = 10)
        val conversationHistory = recentDesc.reversed().map { entity ->
            val role = if (entity.role.equals("user", ignoreCase = true)) "user" else "assistant"
            Pair(role, entity.content)
        }

        // 4. Send to Meta AI via AiRepository (including imageBitmap if present)
        val result = aiRepository.chatWithCoach(
            history = conversationHistory,
            systemPrompt = systemPrompt,
            imageBitmap = imageBitmap
        )

        // 5. If successful, parse agentic actions, execute them against Room DB, and persist response
        return result.mapCatching { responseText ->
            val (cleanText, actions) = actionParser.parse(responseText)
            val actionSummary = if (actions.isNotEmpty()) {
                actionExecutor.executeActions(actions)
            } else null

            val assistantEntity = ChatMessageEntity(
                role = "assistant",
                content = cleanText,
                timestamp = System.currentTimeMillis(),
                actionSummary = actionSummary
            )
            chatMessageDao.insertMessage(assistantEntity)
            cleanText
        }
    }

    override suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }

    override suspend fun transcribeVoice(audioFile: java.io.File): Result<String> {
        return aiRepository.transcribeAudio(audioFile)
    }
}
