package com.fdev.nutrimindai.feature.coach.data.repository

import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.feature.coach.data.local.dao.ChatMessageDao
import com.fdev.nutrimindai.feature.coach.data.local.entity.ChatMessageEntity
import com.fdev.nutrimindai.feature.coach.domain.NutriCoachContextBuilder
import com.fdev.nutrimindai.feature.coach.domain.model.ChatMessage
import com.fdev.nutrimindai.feature.coach.domain.model.ChatRole
import com.fdev.nutrimindai.feature.coach.domain.repository.CoachRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import com.fdev.nutrimindai.core.ai.AiProvider
import kotlinx.coroutines.flow.StateFlow

@Singleton
class CoachRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val aiRepository: AiRepository,
    private val contextBuilder: NutriCoachContextBuilder
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
                    timestamp = entity.timestamp
                )
            }
        }
    }

    override suspend fun sendMessage(userText: String): Result<String> {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be blank"))
        }

        // 1. Save user message to Room DB
        val userEntity = ChatMessageEntity(
            role = "user",
            content = trimmed,
            timestamp = System.currentTimeMillis()
        )
        chatMessageDao.insertMessage(userEntity)

        // 2. Build live context system prompt with current provider's name
        val provider = aiRepository.getActiveProvider()
        val systemPrompt = try {
            contextBuilder.buildSystemPrompt(aiProviderName = provider.displayName)
        } catch (e: Exception) {
            "You are NutriCoach AI, a nutritionist and fitness coach powered by ${provider.displayName}."
        }

        // 3. Retrieve recent history for multi-turn conversational context
        val recentDesc = chatMessageDao.getRecentMessagesDesc(limit = 10)
        val conversationHistory = recentDesc.reversed().map { entity ->
            val role = if (entity.role.equals("user", ignoreCase = true)) "user" else "assistant"
            Pair(role, entity.content)
        }

        // 4. Send to Meta AI via AiRepository
        val result = aiRepository.chatWithCoach(
            history = conversationHistory,
            systemPrompt = systemPrompt
        )

        // 5. If successful, persist AI assistant response to Room DB
        return result.mapCatching { responseText ->
            val assistantEntity = ChatMessageEntity(
                role = "assistant",
                content = responseText,
                timestamp = System.currentTimeMillis()
            )
            chatMessageDao.insertMessage(assistantEntity)
            responseText
        }
    }

    override suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }
}
