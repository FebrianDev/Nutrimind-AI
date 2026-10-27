package com.fdev.nutrimindai.core.ai

import android.graphics.Bitmap
import android.util.Base64
import com.fdev.nutrimindai.BuildConfig
import com.fdev.nutrimindai.core.ai.model.ChatChoice
import com.fdev.nutrimindai.core.ai.model.ChatCompletionRequest
import com.fdev.nutrimindai.core.ai.model.ChatCompletionResponse
import com.fdev.nutrimindai.core.ai.model.ChatMessagePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaAiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    private val apiKey: String = BuildConfig.META_AI_API_KEY.trim()
    private val baseUrl: String = BuildConfig.META_AI_BASE_URL.trim().let {
        if (it.endsWith("/")) it else "$it/"
    }
    private val visionModel: String = BuildConfig.META_AI_VISION_MODEL.trim()
    private val textModel: String = BuildConfig.META_AI_TEXT_MODEL.trim()

    val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != "YOUR_API_KEY_HERE"

    private val candidateVisionModels = listOf(
        visionModel,
        "muse-spark-1.3",
        "muse-wonder-1.0",
        "accounts/fireworks/models/llama-v3p2-11b-vision-instruct",
        "meta/llama-3.2-11b-vision-instruct",
        "accounts/fireworks/models/llama-v3p2-90b-vision-instruct"
    ).filter { it.isNotBlank() }.distinct()

    private val candidateTextModels = listOf(
        textModel,
        "muse-spark-1.3",
        "muse-wonder-1.0",
        "accounts/fireworks/models/llama-v3p3-70b-instruct",
        "meta/llama-3.3-70b-instruct",
        "accounts/fireworks/models/llama-v3p2-3b-instruct"
    ).filter { it.isNotBlank() }.distinct()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(
        prompt: String,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Meta AI API key is not configured. Please set META_AI_API_KEY in local.properties.")
            )
        }

        var lastException: Exception? = null

        for (modelName in candidateTextModels) {
            try {
                Timber.d("MetaAiService: generating text with model: $modelName")
                val messages = mutableListOf<ChatMessagePayload>()
                if (!systemPrompt.isNullOrBlank()) {
                    messages.add(
                        ChatMessagePayload(
                            role = "system",
                            content = JsonPrimitive(systemPrompt)
                        )
                    )
                }
                messages.add(
                    ChatMessagePayload(
                        role = "user",
                        content = JsonPrimitive(prompt)
                    )
                )

                val requestPayload = ChatCompletionRequest(
                    model = modelName,
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 2048
                )

                val responseText = executeChatCompletion(requestPayload)
                if (responseText.isNotBlank()) {
                    return@withContext Result.success(responseText.trim())
                }
            } catch (e: Exception) {
                Timber.w("MetaAiService: text generation failed with model $modelName: ${e.message}")
                lastException = e
            }
        }

        val friendlyMessage = formatErrorMessage(lastException)
        Result.failure(IllegalStateException(friendlyMessage))
    }

    suspend fun chat(
        conversation: List<Pair<String, String>>, // (role, content)
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Meta AI API key is not configured. Please set META_AI_API_KEY in local.properties.")
            )
        }

        var lastException: Exception? = null

        for (modelName in candidateTextModels) {
            try {
                Timber.d("MetaAiService: multi-turn chat with model: $modelName")
                val messages = mutableListOf<ChatMessagePayload>()
                if (!systemPrompt.isNullOrBlank()) {
                    messages.add(
                        ChatMessagePayload(
                            role = "system",
                            content = JsonPrimitive(systemPrompt)
                        )
                    )
                }

                conversation.forEach { (role, content) ->
                    messages.add(
                        ChatMessagePayload(
                            role = role,
                            content = JsonPrimitive(content)
                        )
                    )
                }

                val requestPayload = ChatCompletionRequest(
                    model = modelName,
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 2048
                )

                val responseText = executeChatCompletion(requestPayload)
                if (responseText.isNotBlank()) {
                    return@withContext Result.success(responseText.trim())
                }
            } catch (e: Exception) {
                Timber.w("MetaAiService: chat failed with model $modelName: ${e.message}")
                lastException = e
            }
        }

        val friendlyMessage = formatErrorMessage(lastException)
        Result.failure(IllegalStateException(friendlyMessage))
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap): Result<FoodRecognitionResult> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Meta AI API key is not configured. Please set META_AI_API_KEY in local.properties.")
            )
        }

        var lastException: Exception? = null

        val base64Image = encodeBitmapToBase64(bitmap)
        val prompt = """
            You are an expert nutritionist AI powered by Meta Llama Vision. Analyze the food or meal image provided and return estimated nutritional info.

            IMPORTANT:
            If the image DOES NOT contain any recognizable food, meal, snack, beverage, or edible item (e.g., photo of a person, animal, electronic device, furniture, empty surface, document, or random object):
            Return ONLY this JSON strictly:
            {
                "name": "Non-Food Item",
                "calories": 0,
                "protein": 0.0,
                "carbs": 0.0,
                "fat": 0.0,
                "fiber": 0.0,
                "servingSize": 0.0,
                "servingUnit": "portion",
                "confidence": 0.0,
                "description": "No food detected in image"
            }

            Otherwise, if food IS present, return ONLY a valid JSON object matching this schema strictly:
            {
                "name": "Food Name in English",
                "calories": 450,
                "protein": 15.0,
                "carbs": 50.0,
                "fat": 12.0,
                "fiber": 3.0,
                "servingSize": 1.0,
                "servingUnit": "portion",
                "confidence": 0.92,
                "description": "Short description of detected food in English"
            }
            Do not include codeblock markers, markdown formatting, or any explanation text outside the JSON.
        """.trimIndent()

        val visionContent = buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", prompt)
            })
            add(buildJsonObject {
                put("type", "image_url")
                putJsonObject("image_url") {
                    put("url", "data:image/jpeg;base64,$base64Image")
                }
            })
        }

        for (modelName in candidateVisionModels) {
            try {
                Timber.d("MetaAiService: analyzing food image with model: $modelName")
                val requestPayload = ChatCompletionRequest(
                    model = modelName,
                    messages = listOf(
                        ChatMessagePayload(
                            role = "user",
                            content = visionContent
                        )
                    ),
                    temperature = 0.2,
                    maxTokens = 2048
                )

                val responseText = executeChatCompletion(requestPayload)
                if (responseText.isNotBlank()) {
                    val cleanJson = cleanJsonText(responseText)
                    if (cleanJson.isBlank() || !cleanJson.startsWith("{") || !cleanJson.endsWith("}")) {
                        Timber.w("MetaAiService: Non-food or non-JSON response from vision model: $responseText")
                        return@withContext Result.failure(
                            IllegalArgumentException("No food items detected in this image. Please take a clear, well-lit photo of your meal.")
                        )
                    }

                    val parsedResult = json.decodeFromString<FoodRecognitionResult>(cleanJson)

                    // Non-Food guard validation
                    if (parsedResult.confidence <= 0.0 ||
                        parsedResult.name.equals("Non-Food Item", ignoreCase = true) ||
                        (parsedResult.calories == 0 && parsedResult.description.contains("No food", ignoreCase = true))
                    ) {
                        return@withContext Result.failure(
                            IllegalArgumentException("No food items detected in this image. Please take a clear, well-lit photo of your meal.")
                        )
                    }

                    return@withContext Result.success(parsedResult)
                }
            } catch (e: IllegalArgumentException) {
                return@withContext Result.failure(e)
            } catch (e: Exception) {
                Timber.w("MetaAiService: vision analysis failed with model $modelName: ${e.message}")
                lastException = e
            }
        }

        val friendlyMessage = formatErrorMessage(lastException)
        Result.failure(IllegalStateException(friendlyMessage))
    }

    private fun executeChatCompletion(requestPayload: ChatCompletionRequest): String {
        val endpointUrl = "${baseUrl}chat/completions"
        val requestJson = json.encodeToString(requestPayload)

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Timber.e("MetaAiService HTTP ${response.code}: $responseBody")
                throw IOException("Meta AI HTTP ${response.code}: $responseBody")
            }

            val parsedResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
            if (parsedResponse.error != null) {
                throw IOException("Meta AI API Error: ${parsedResponse.error.message}")
            }

            return parsedResponse.choices.firstOrNull()?.message?.content ?: ""
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1024
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(width, height)
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun cleanJsonText(raw: String): String {
        var text = raw.trim()
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace >= firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        } else {
            if (text.startsWith("```")) {
                val firstLineEnd = text.indexOf('\n')
                if (firstLineEnd != -1) {
                    text = text.substring(firstLineEnd + 1)
                }
                if (text.endsWith("```")) {
                    text = text.substring(0, text.length - 3)
                }
            }
        }
        return text.trim()
    }

    private fun formatErrorMessage(exception: Exception?): String {
        val msg = exception?.message ?: ""
        return when {
            msg.contains("No food items detected") -> msg
            msg.contains("401") || msg.contains("Unauthorized") || msg.contains("unauthenticated") ->
                "Invalid Meta AI API Key. Please verify META_AI_API_KEY in local.properties."
            msg.contains("404") || msg.contains("NOT_FOUND") ->
                "Meta AI model endpoint not found. Please check model configuration or endpoint URL."
            msg.contains("429") || msg.contains("rate_limit") ->
                "Meta AI rate limit reached. Please wait a moment before trying again."
            msg.contains("timeout") || msg.contains("Timeout") ->
                "Request to Meta AI timed out. Please check your internet connection."
            else ->
                "Failed to connect to Meta AI. Please check your network connection and API Key."
        }
    }
}
