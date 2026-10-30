package com.fdev.nutrimindai.core.ai

import android.graphics.Bitmap
import android.util.Base64
import com.fdev.nutrimindai.BuildConfig
import com.fdev.nutrimindai.core.ai.model.ChatChoice
import com.fdev.nutrimindai.core.ai.model.ChatCompletionRequest
import com.fdev.nutrimindai.core.ai.model.ChatCompletionResponse
import com.fdev.nutrimindai.core.ai.model.ChatMessagePayload
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.SmartRecipe
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
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
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
            } catch (e: MetaAiAuthException) {
                Timber.e("MetaAiService: auth failure, terminating candidate search: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: MetaAiRateLimitException) {
                Timber.w("MetaAiService: rate limit reached, terminating candidate search: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: Exception) {
                Timber.w("MetaAiService: text generation failed with model $modelName: ${e.message}")
                lastException = e
                if (!shouldTryNextModel(e)) break
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
            } catch (e: MetaAiAuthException) {
                Timber.e("MetaAiService: auth failure, terminating candidate search: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: MetaAiRateLimitException) {
                Timber.w("MetaAiService: rate limit reached, terminating candidate search: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: Exception) {
                Timber.w("MetaAiService: chat failed with model $modelName: ${e.message}")
                lastException = e
                if (!shouldTryNextModel(e)) break
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

            MULTICULTURAL & MULTI-FOOD VISION CAPABILITIES:
            - Accurately recognize single items or complex multi-component plates and bowls (e.g., rice with protein and vegetables, mixed salads, noodle dishes).
            - Expertly identify international and local regional cuisines (e.g., Indonesian dishes like Nasi Goreng, Gado-Gado, Satay, Rendang, Soto, Tempeh, Tofu, Pecel, as well as Asian, Mediterranean, Latin, and Western foods).
            - In the "description", provide a concise breakdown of detected components and estimated portion size (e.g., "1 plate of steamed white rice (~150g) with grilled chicken breast (~120g) and steamed vegetables").

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
                "description": "Short description with component breakdown and estimated portion in English"
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
            } catch (e: MetaAiAuthException) {
                Timber.e("MetaAiService: vision auth failure, terminating candidate search: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: MetaAiRateLimitException) {
                Timber.w("MetaAiService: vision rate limit reached, terminating candidate search: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: Exception) {
                Timber.w("MetaAiService: vision analysis failed with model $modelName: ${e.message}")
                lastException = e
                if (!shouldTryNextModel(e)) break
            }
        }

        val friendlyMessage = formatErrorMessage(lastException)
        Result.failure(IllegalStateException(friendlyMessage))
    }

    suspend fun estimateFoodNutrition(foodDescription: String): Result<FoodRecognitionResult> = withContext(Dispatchers.IO) {
        val trimmed = foodDescription.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Food description cannot be empty."))
        }

        if (!isApiKeyConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Meta AI API key is not configured. Please set META_AI_API_KEY in local.properties.")
            )
        }

        val prompt = """
            You are an expert nutritionist AI powered by Meta Llama 3.3.
            The user wants nutritional information for: "$trimmed".

            MULTICULTURAL & MULTI-FOOD ESTIMATION CAPABILITIES:
            - Accurately estimate typical standard portions, calories, and macronutrients (protein, carbs, fat, fiber) for single items, snacks, beverages, or complex plates.
            - Expertly identify international and local regional cuisines (e.g., Indonesian dishes like Nasi Goreng, Gado-Gado, Sate, Rendang, Soto, Tempeh, Tahu, Pecel, as well as Asian, Mediterranean, Latin, and Western foods).

            IMPORTANT:
            If the input text "$trimmed" IS NOT a recognizable food, meal, snack, beverage, or edible ingredient (e.g. random letters, electronic device, furniture, name of a person, vehicle, clothing):
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
                "description": "No food detected in text"
            }

            Otherwise, if it IS a valid edible item or meal, return ONLY a valid JSON object matching this schema strictly:
            {
                "name": "Proper Food Name in English",
                "calories": 350,
                "protein": 22.0,
                "carbs": 40.0,
                "fat": 10.0,
                "fiber": 4.0,
                "servingSize": 1.0,
                "servingUnit": "portion",
                "confidence": 0.95,
                "description": "Concise breakdown of ingredients and portion estimated by Meta AI"
            }
            Do not include markdown codeblocks, notes, or any conversational text outside the JSON.
        """.trimIndent()

        var lastException: Exception? = null

        for (modelName in candidateTextModels) {
            try {
                Timber.d("MetaAiService: estimating food nutrition with model: $modelName")
                val requestPayload = ChatCompletionRequest(
                    model = modelName,
                    messages = listOf(
                        ChatMessagePayload(
                            role = "user",
                            content = JsonPrimitive(prompt)
                        )
                    ),
                    temperature = 0.2,
                    maxTokens = 1024
                )

                val responseText = executeChatCompletion(requestPayload)
                if (responseText.isNotBlank()) {
                    val cleanJson = cleanJsonText(responseText)
                    if (cleanJson.isBlank() || !cleanJson.startsWith("{") || !cleanJson.endsWith("}")) {
                        Timber.w("MetaAiService: Non-JSON response from text model: $responseText")
                        return@withContext Result.failure(
                            IllegalArgumentException("Could not estimate nutrition for '$trimmed'. Please try with more specific food terms.")
                        )
                    }

                    val parsedResult = json.decodeFromString<FoodRecognitionResult>(cleanJson)

                    // Non-Food guard validation
                    if (parsedResult.confidence <= 0.0 ||
                        parsedResult.name.equals("Non-Food Item", ignoreCase = true) ||
                        (parsedResult.calories == 0 && parsedResult.description.contains("No food", ignoreCase = true))
                    ) {
                        return@withContext Result.failure(
                            IllegalArgumentException("'$trimmed' does not appear to be a recognizable food or beverage.")
                        )
                    }

                    return@withContext Result.success(parsedResult)
                }
            } catch (e: IllegalArgumentException) {
                return@withContext Result.failure(e)
            } catch (e: MetaAiAuthException) {
                Timber.e("MetaAiService: text auth failure: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: MetaAiRateLimitException) {
                Timber.w("MetaAiService: text rate limit reached: ${e.message}")
                return@withContext Result.failure(IllegalStateException(formatErrorMessage(e)))
            } catch (e: Exception) {
                Timber.w("MetaAiService: estimation failed with model $modelName: ${e.message}")
                lastException = e
                if (!shouldTryNextModel(e)) break
            }
        }

        val friendlyMessage = formatErrorMessage(lastException)
        Result.failure(IllegalStateException(friendlyMessage))
    }

    private fun shouldTryNextModel(e: Exception): Boolean {
        return when (e) {
            is MetaAiAuthException,
            is MetaAiRateLimitException,
            is java.net.UnknownHostException,
            is java.net.ConnectException,
            is java.net.SocketTimeoutException,
            is java.io.InterruptedIOException,
            is javax.net.ssl.SSLException -> false
            is MetaAiHttpException -> e.statusCode in setOf(400, 404, 422, 500, 502, 503)
            else -> true
        }
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
                when (response.code) {
                    401, 403 -> throw MetaAiAuthException(response.code, responseBody)
                    429 -> throw MetaAiRateLimitException(responseBody)
                    else -> throw MetaAiHttpException(response.code, "Meta AI HTTP ${response.code}: $responseBody")
                }
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
            val newWidth = (width * scale).toInt().coerceAtLeast(1)
            val newHeight = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream(64 * 1024)
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

    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Audio recording file is empty or not found"))
        }

        if (!isApiKeyConfigured) {
            return@withContext Result.failure(
                MetaAiAuthException(401, "Meta AI API Key is not configured. Please set META_AI_API_KEY in local.properties.")
            )
        }

        val audioBytes = audioFile.readBytes()
        val audioRequestBody = audioBytes.toRequestBody("audio/wav".toMediaType())

        // 1. Primary: Meta Model API official ASR transcribe endpoint (Muse Voice Transcribe / Meta MMS)
        // URL: https://api.meta.ai/v1/asr/transcribe
        val asrUrl = if (baseUrl.contains("api.meta.ai")) {
            "${baseUrl}asr/transcribe"
        } else {
            "${baseUrl}audio/transcriptions"
        }

        val requestPayload = buildJsonObject {
            put("model", "muse-voice-transcribe-1.0")
            put("audioEncoding", "WAV")
            put("mode", "PUSH_TO_TALK")
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "request",
                null,
                requestPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .addFormDataPart(
                "audio",
                audioFile.name,
                audioRequestBody
            )
            .build()

        val request = Request.Builder()
            .url(asrUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(multipartBody)
            .build()

        try {
            val (isSuccess, code, responseBody) = okHttpClient.newCall(request).execute().use { response ->
                Triple(response.isSuccessful, response.code, response.body?.string().orEmpty())
            }

            if (isSuccess && responseBody.isNotBlank()) {
                val transcript = parseTranscriptionResponse(responseBody)
                if (transcript.isNotBlank()) {
                    Timber.d("Meta AI ASR Transcribe success: $transcript")
                    return@withContext Result.success(transcript)
                }
            } else {
                Timber.w("Primary Meta ASR transcribe endpoint returned $code: $responseBody")
            }
        } catch (e: Exception) {
            Timber.w(e, "Primary Meta ASR transcribe failed, trying fallback transcription endpoint")
        }

        // 2. Fallback: Standard Meta / OpenAI / Fireworks audio transcription endpoint
        // POST ${baseUrl}audio/transcriptions
        val fallbackUrl = "${baseUrl}audio/transcriptions"
        val fallbackMultipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioRequestBody
            )
            .addFormDataPart("model", "muse-voice-transcribe-1.0")
            .build()

        val fallbackRequest = Request.Builder()
            .url(fallbackUrl)
            .header("Authorization", "Bearer $apiKey")
            .post(fallbackMultipart)
            .build()

        try {
            val (isSuccess, responseBody) = okHttpClient.newCall(fallbackRequest).execute().use { response ->
                Pair(response.isSuccessful, response.body?.string().orEmpty())
            }

            if (isSuccess && responseBody.isNotBlank()) {
                val transcript = parseTranscriptionResponse(responseBody)
                if (transcript.isNotBlank()) {
                    return@withContext Result.success(transcript)
                }
            }
            Result.failure(IOException("Failed to transcribe voice with Meta AI: $responseBody"))
        } catch (e: Exception) {
            Timber.e(e, "Fallback audio transcription failed")
            Result.failure(e)
        }
    }

    private fun parseTranscriptionResponse(responseBody: String): String {
        val trimmed = responseBody.trim()
        if (!trimmed.startsWith("{")) {
            return trimmed
        }
        return try {
            val jsonElement = json.parseToJsonElement(trimmed)
            if (jsonElement is kotlinx.serialization.json.JsonObject) {
                val textCandidate = jsonElement["text"]?.let {
                    if (it is JsonPrimitive) it.content else null
                } ?: jsonElement["transcription"]?.let {
                    if (it is JsonPrimitive) it.content else null
                } ?: jsonElement["transcript"]?.let {
                    if (it is JsonPrimitive) it.content else null
                }

                if (!textCandidate.isNullOrBlank()) {
                    return textCandidate.trim()
                }

                val results = jsonElement["results"] ?: jsonElement["turns"]
                if (results != null && results is kotlinx.serialization.json.JsonArray && results.isNotEmpty()) {
                    val first = results[0]
                    if (first is kotlinx.serialization.json.JsonObject) {
                        val turnText = first["transcript"] ?: first["text"] ?: first["content"]
                        if (turnText is JsonPrimitive) {
                            return turnText.content.trim()
                        }
                    }
                }
            }
            trimmed
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun formatErrorMessage(exception: Exception?): String {
        val msg = exception?.message ?: ""
        return when {
            msg.contains("No food items detected") -> msg
            exception is MetaAiAuthException || msg.contains("401") || msg.contains("403") || msg.contains("Unauthorized") || msg.contains("unauthenticated") ->
                "Invalid or unauthorized Meta AI API Key. Please verify META_AI_API_KEY in local.properties."
            exception is MetaAiRateLimitException || msg.contains("429") || msg.contains("rate_limit") || msg.contains("Rate Limit") ->
                "Meta AI rate limit reached. Please wait a moment before trying again."
            msg.contains("404") || msg.contains("NOT_FOUND") ->
                "Meta AI model endpoint not found. Please check model configuration or endpoint URL."
            msg.contains("timeout") || msg.contains("Timeout") ->
                "Request to Meta AI timed out. Please check your internet connection."
            else ->
                "Failed to connect to Meta AI. Please check your network connection and API Key."
        }
    }

    private fun cleanJsonArray(raw: String): String {
        var text = raw.trim()
        val firstBracket = text.indexOf('[')
        val lastBracket = text.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket != -1 && lastBracket >= firstBracket) {
            text = text.substring(firstBracket, lastBracket + 1)
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

    suspend fun generateSmartRecipes(
        remainingCalories: Int,
        remainingProtein: Double,
        remainingCarbs: Double,
        remainingFat: Double,
        mealType: MealType,
        userGoal: String
    ): Result<List<SmartRecipe>> = withContext(Dispatchers.IO) {
        val targetCals = remainingCalories.coerceIn(150, 1500)

        if (!isApiKeyConfigured) {
            return@withContext Result.success(getFallbackRecipes(targetCals, mealType))
        }

        val prompt = """
            You are NutriChef AI, a Michelin-star clinical dietitian chef powered by Meta AI.
            The user has $targetCals kcal remaining for today (Target: ${remainingProtein.toInt()}g protein, ${remainingCarbs.toInt()}g carbs, ${remainingFat.toInt()}g fat).
            Meal type: ${mealType.name}.
            User Goal: $userGoal.

            Recommend exactly 2 to 3 delicious, healthy, easy-to-cook recipes that fit comfortably within this remaining calorie and macro budget.
            Return ONLY a valid JSON array matching this exact schema:
            [
              {
                "title": "Recipe Name in Indonesian",
                "description": "Short tempting description highlighting health benefits",
                "prepTimeMinutes": 15,
                "calories": 380,
                "protein": 32.0,
                "carbs": 25.0,
                "fat": 11.0,
                "fiber": 4.0,
                "ingredients": ["100g dada ayam", "1 butir telur", "1 siung bawang putih"],
                "instructions": ["Tumis bumbu hingga harum...", "Masukkan ayam...", "Sajikan hangat."],
                "chefTip": "NutriChef tip for cooking or substitution"
              }
            ]
            Do not include markdown codeblocks or conversational text outside the JSON.
        """.trimIndent()

        var lastException: Exception? = null

        for (modelName in candidateTextModels) {
            try {
                Timber.d("MetaAiService: generating smart recipes with model: $modelName")
                val requestPayload = ChatCompletionRequest(
                    model = modelName,
                    messages = listOf(
                        ChatMessagePayload(
                            role = "user",
                            content = JsonPrimitive(prompt)
                        )
                    ),
                    temperature = 0.5,
                    maxTokens = 1200
                )

                val responseText = executeChatCompletion(requestPayload)
                if (responseText.isNotBlank()) {
                    val cleanJson = cleanJsonArray(responseText)
                    if (cleanJson.isNotBlank() && cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
                        val parsedRecipes = json.decodeFromString<List<SmartRecipe>>(cleanJson)
                        if (parsedRecipes.isNotEmpty()) {
                            val enriched = parsedRecipes.map { it.copy(mealType = mealType) }
                            return@withContext Result.success(enriched)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w("MetaAiService: recipe generation failed with model $modelName: ${e.message}")
                lastException = e
                if (!shouldTryNextModel(e)) break
            }
        }

        Timber.w("MetaAiService: falling back to curated recipes due to: ${lastException?.message}")
        Result.success(getFallbackRecipes(targetCals, mealType))
    }

    fun getFallbackRecipes(targetCals: Int, mealType: MealType): List<SmartRecipe> {
        return when {
            targetCals <= 350 -> listOf(
                SmartRecipe(
                    title = "Dada Ayam Panggang Rosemary & Salad",
                    description = "Menu rendah kalori tinggi protein, cocok untuk dinner ringan bernutrisi.",
                    prepTimeMinutes = 15,
                    calories = 290,
                    protein = 34.0,
                    carbs = 8.0,
                    fat = 7.0,
                    fiber = 4.0,
                    mealType = mealType,
                    ingredients = listOf("120g dada ayam fillet", "1 sdt minyak zaitun", "1/2 sdt rosemary & lada hitam", "Salad selada & tomat ceri"),
                    instructions = listOf("Bumbui ayam dengan rosemary, garam, dan lada.", "Panggang di teflon anti lengket 5 menit tiap sisi.", "Sajikan dengan salad segar."),
                    chefTip = "Gunakan perasan lemon agar daging ayam tetap juicy dan segar."
                ),
                SmartRecipe(
                    title = "Omelet Putih Telur Bayam Jamur",
                    description = "Kaya mikronutrien dan protein murni tanpa kelebihan lemak jenuh.",
                    prepTimeMinutes = 10,
                    calories = 230,
                    protein = 25.0,
                    carbs = 7.0,
                    fat = 6.0,
                    fiber = 3.0,
                    mealType = mealType,
                    ingredients = listOf("3 butir putih telur", "1 butir telur utuh", "50g bayam segar", "4 buah jamur champignon iris"),
                    instructions = listOf("Kocok telur bersama sejumput lada dan garam.", "Tumis bayam dan jamur dengan sedikit minyak.", "Tuang telur, masak dengan api kecil hingga matang merata."),
                    chefTip = "Tambahkan sedikit bubuk oregano untuk aroma ala bistro Italia."
                )
            )
            targetCals <= 600 -> listOf(
                SmartRecipe(
                    title = "Nasi Merah Salmon Teriyaki & Brokoli",
                    description = "Kombinasi asam lemak omega-3 dan karbohidrat kompleks indeks glikemik rendah.",
                    prepTimeMinutes = 20,
                    calories = 460,
                    protein = 38.0,
                    carbs = 44.0,
                    fat = 13.0,
                    fiber = 5.0,
                    mealType = mealType,
                    ingredients = listOf("100g nasi beras merah matang", "120g fillet salmon", "1 sdm saus teriyaki rendah sodium", "100g brokoli kukus"),
                    instructions = listOf("Panaskan wajan dengan sedikit olive oil, panggang salmon hingga golden brown.", "Tuang saus teriyaki, masak 1 menit hingga meresap.", "Tata salmon di atas nasi merah bersama brokoli kukus."),
                    chefTip = "Jangan memasak salmon terlalu lama agar teksturnya tetap lembut dan omega-3 tidak rusak."
                ),
                SmartRecipe(
                    title = "Tumis Sapi Lada Hitam Paprika & Nasi",
                    description = "Zat besi tinggi untuk stamina optimal dan pembentukan sel darah merah.",
                    prepTimeMinutes = 15,
                    calories = 490,
                    protein = 36.0,
                    carbs = 48.0,
                    fat = 14.0,
                    fiber = 4.0,
                    mealType = mealType,
                    ingredients = listOf("100g daging sapi tenderloin iris tipis", "1/2 buah paprika merah & hijau", "1 sdm saus lada hitam", "100g nasi putih matang"),
                    instructions = listOf("Tumis bawang bombay dan paprika hingga harum layu.", "Masukkan irisan sapi tenderloin, aduk cepat dengan api besar.", "Tambahkan saus lada hitam dan sajikan bersama nasi."),
                    chefTip = "Iris daging sapi melawan serat agar teksturnya empuk saat dimakan."
                )
            )
            else -> listOf(
                SmartRecipe(
                    title = "Double Protein Chicken Steak & Sweet Potato",
                    description = "Bahan bakar pemulihan otot intensif dengan karbohidrat sehat kaya serat.",
                    prepTimeMinutes = 25,
                    calories = 650,
                    protein = 54.0,
                    carbs = 62.0,
                    fat = 16.0,
                    fiber = 7.0,
                    mealType = mealType,
                    ingredients = listOf("200g dada ayam panggang", "150g ubi madu panggang", "Buncis & wortel tumis bawang putih", "1 sdm BBQ sauce light"),
                    instructions = listOf("Bumbui ayam dan panggang hingga matang sempurna.", "Panggang ubi madu dalam oven/air fryer 180°C selama 15 menit.", "Sajikan hangat dengan saus BBQ."),
                    chefTip = "Kulit ubi jangan dikupas, cukup cuci bersih karena mengandung serat tinggi."
                ),
                SmartRecipe(
                    title = "Nasi Goreng Beras Merah Dada Ayam & Telur",
                    description = "Versi sehat dari menu nusantara dengan kalori terkontrol dan protein melimpah.",
                    prepTimeMinutes = 15,
                    calories = 610,
                    protein = 45.0,
                    carbs = 68.0,
                    fat = 15.0,
                    fiber = 6.0,
                    mealType = mealType,
                    ingredients = listOf("150g nasi merah dingin", "120g dada ayam suwir", "1 butir telur", "Bawang merah, bawang putih, cabai, kecap manis secukupnya"),
                    instructions = listOf("Tumis bumbu halus dengan 1 sdt olive oil.", "Masukkan telur dan orak-arik.", "Masukkan nasi merah dan ayam suwir, bumbui dengan kecap dan garam hingga wangi."),
                    chefTip = "Gunakan nasi merah dingin semalam agar butirannya tidak menggumpal saat digoreng."
                )
            )
        }
    }
}

class MetaAiAuthException(val statusCode: Int, rawBody: String) : IOException("Meta AI Authentication Error ($statusCode): $rawBody")
class MetaAiRateLimitException(rawBody: String) : IOException("Meta AI Rate Limit Exceeded (429): $rawBody")
class MetaAiHttpException(val statusCode: Int, message: String) : IOException(message)
