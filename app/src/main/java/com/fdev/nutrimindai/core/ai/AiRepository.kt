package com.fdev.nutrimindai.core.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

interface AiRepository {
    fun getActiveProvider(): AiProvider
    fun setActiveProvider(provider: AiProvider)
    val activeProviderFlow: StateFlow<AiProvider>

    suspend fun generateDailyReviewSummary(
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
    ): String

    suspend fun generateCoachingTip(
        userGoal: String,
        consumedCalories: Int,
        targetCalories: Int,
        fallbackTip: String
    ): String

    suspend fun analyzeFoodImage(bitmap: Bitmap): Result<FoodRecognitionResult>

    suspend fun chatWithCoach(
        history: List<Pair<String, String>>,
        systemPrompt: String? = null
    ): Result<String>
}
