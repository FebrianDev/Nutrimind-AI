package com.fdev.nutrimindai.core.ai

import android.graphics.Bitmap
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val metaAiService: MetaAiService,
    @Suppress("unused") private val preferenceManager: PreferenceManager
) : AiRepository {

    private val _activeProvider = MutableStateFlow(AiProvider.META_LLAMA)
    override val activeProviderFlow: StateFlow<AiProvider> = _activeProvider.asStateFlow()

    override fun getActiveProvider(): AiProvider = _activeProvider.value

    override fun setActiveProvider(provider: AiProvider) {
        _activeProvider.value = provider
        Timber.i("AiRepository: Active AI provider set to ${provider.displayName}")
    }

    override suspend fun generateDailyReviewSummary(
        userGoal: String,
        score: Int,
        calories: Int,
        targetCalories: Int,
        protein: Double,
        targetProtein: Double,
        carbs: Double,
        targetCarbs: Double,
        fat: Double,
        targetFat: Double,
        waterMl: Int,
        targetWaterMl: Int,
        exerciseMinutes: Int,
        targetExerciseMinutes: Int,
        fallbackSummary: String
    ): String {
        if (!metaAiService.isApiKeyConfigured) {
            return fallbackSummary
        }

        val systemPrompt = "You are NutriMind AI, an empathetic, expert nutritionist coach powered by Meta AI."
        val prompt = """
            Provide a concise, highly motivating daily nutrition and activity review (maximum 3 sentences) for a user with the goal: '$userGoal'.
            
            Daily Data:
            - Overall Health Score: $score / 100
            - Calories Consumed: $calories kcal (Target: $targetCalories kcal)
            - Protein: ${protein.toInt()}g (Target: ${targetProtein.toInt()}g)
            - Carbs: ${carbs.toInt()}g (Target: ${targetCarbs.toInt()}g)
            - Fat: ${fat.toInt()}g (Target: ${targetFat.toInt()}g)
            - Water Intake: ${waterMl}ml (Target: ${targetWaterMl}ml)
            - Exercise Duration: ${exerciseMinutes} min (Target: ${targetExerciseMinutes} min)
            
            Provide actionable feedback and praise what went well, highlighting any minor improvement area for tomorrow.
        """.trimIndent()

        val result = metaAiService.generateContent(prompt = prompt, systemPrompt = systemPrompt)
        return result.getOrElse { error ->
            Timber.w("Meta AI failed for daily review, returning fallback: ${error.message}")
            fallbackSummary
        }
    }

    override suspend fun generateCoachingTip(
        userGoal: String,
        consumedCalories: Int,
        targetCalories: Int,
        fallbackTip: String
    ): String {
        if (!metaAiService.isApiKeyConfigured) {
            return fallbackTip
        }

        val systemPrompt = "You are NutriMind AI, a nutritionist coach powered by Meta AI."
        val prompt = """
            Give a 1-sentence quick actionable tip for today based on:
            Goal: $userGoal
            Calories consumed so far: $consumedCalories kcal / $targetCalories kcal
        """.trimIndent()

        val result = metaAiService.generateContent(prompt = prompt, systemPrompt = systemPrompt)
        return result.getOrElse { error ->
            Timber.w("Meta AI failed for coaching tip, returning fallback: ${error.message}")
            fallbackTip
        }
    }

    override suspend fun analyzeFoodImage(bitmap: Bitmap): Result<FoodRecognitionResult> {
        return metaAiService.analyzeFoodImage(bitmap)
    }

    override suspend fun estimateFoodNutrition(foodDescription: String): Result<FoodRecognitionResult> {
        return metaAiService.estimateFoodNutrition(foodDescription)
    }

    override suspend fun chatWithCoach(
        history: List<Pair<String, String>>,
        systemPrompt: String?
    ): Result<String> {
        return metaAiService.chat(conversation = history, systemPrompt = systemPrompt)
    }

    override suspend fun transcribeAudio(audioFile: java.io.File): Result<String> {
        return metaAiService.transcribeAudio(audioFile)
    }
}
