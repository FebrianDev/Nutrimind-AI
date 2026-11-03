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

    suspend fun estimateFoodNutrition(foodDescription: String): Result<FoodRecognitionResult>

    suspend fun chatWithCoach(
        history: List<Pair<String, String>>,
        systemPrompt: String? = null,
        imageBitmap: Bitmap? = null
    ): Result<String>

    suspend fun transcribeAudio(audioFile: java.io.File): Result<String>

    suspend fun generateSmartRecipes(
        remainingCalories: Int,
        remainingProtein: Double,
        remainingCarbs: Double,
        remainingFat: Double,
        mealType: com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType = com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType.DINNER,
        userGoal: String = "Maintenance"
    ): Result<List<com.fdev.nutrimindai.feature.dailyintake.domain.model.SmartRecipe>>
}
