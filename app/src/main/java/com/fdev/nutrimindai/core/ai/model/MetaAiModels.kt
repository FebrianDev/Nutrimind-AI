package com.fdev.nutrimindai.core.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    val temperature: Double? = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int? = 1024
)

@Serializable
data class ChatMessagePayload(
    val role: String,
    val content: JsonElement
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    val error: ChatError? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatResponseMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class ChatResponseMessage(
    val role: String = "assistant",
    val content: String? = null
)

@Serializable
data class ChatError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
