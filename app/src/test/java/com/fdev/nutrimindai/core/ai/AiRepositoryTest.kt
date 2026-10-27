package com.fdev.nutrimindai.core.ai

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull

class AiRepositoryTest {

    private val metaAiService = mock(MetaAiService::class.java)
    private val preferenceManager = mock(PreferenceManager::class.java)
    private lateinit var repository: AiRepositoryImpl

    @Before
    fun setUp() {
        `when`(preferenceManager.activeAiProvider).thenReturn(kotlinx.coroutines.flow.flowOf(null))
        repository = AiRepositoryImpl(metaAiService, preferenceManager)
    }

    @Test
    fun `generateDailyReviewSummary returns fallback when API key is not configured`() = runBlocking {
        `when`(metaAiService.isApiKeyConfigured).thenReturn(false)

        val fallback = "Fallback daily review summary"
        val result = repository.generateDailyReviewSummary(
            userGoal = "Lose Weight",
            score = 85,
            calories = 1800,
            targetCalories = 2000,
            protein = 120.0,
            targetProtein = 130.0,
            carbs = 180.0,
            targetCarbs = 200.0,
            fat = 50.0,
            targetFat = 60.0,
            waterMl = 2000,
            targetWaterMl = 2500,
            exerciseMinutes = 30,
            targetExerciseMinutes = 30,
            fallbackSummary = fallback
        )

        assertEquals(fallback, result)
    }

    @Test
    fun `generateCoachingTip returns fallback when API key is not configured`() = runBlocking {
        `when`(metaAiService.isApiKeyConfigured).thenReturn(false)

        val fallbackTip = "Drink more water today!"
        val result = repository.generateCoachingTip(
            userGoal = "Gain Weight",
            consumedCalories = 1200,
            targetCalories = 2500,
            fallbackTip = fallbackTip
        )

        assertEquals(fallbackTip, result)
    }

    @Test
    fun `generateDailyReviewSummary returns generated content when Meta succeeds`() = runBlocking {
        `when`(metaAiService.isApiKeyConfigured).thenReturn(true)
        val expectedSummary = "Great progress on reaching your targets today with Meta AI!"
        `when`(metaAiService.generateContent(any(), anyOrNull())).thenReturn(Result.success(expectedSummary))

        val result = repository.generateDailyReviewSummary(
            userGoal = "Lose Weight",
            score = 85,
            calories = 1800,
            targetCalories = 2000,
            protein = 120.0,
            targetProtein = 130.0,
            carbs = 180.0,
            targetCarbs = 200.0,
            fat = 50.0,
            targetFat = 60.0,
            waterMl = 2000,
            targetWaterMl = 2500,
            exerciseMinutes = 30,
            targetExerciseMinutes = 30,
            fallbackSummary = "Fallback"
        )

        assertEquals(expectedSummary, result)
    }

    @Test
    fun `generateCoachingTip returns generated content when Meta succeeds`() = runBlocking {
        `when`(metaAiService.isApiKeyConfigured).thenReturn(true)
        val expectedTip = "Prioritize lean proteins for lunch to stay on track."
        `when`(metaAiService.generateContent(any(), anyOrNull())).thenReturn(Result.success(expectedTip))

        val result = repository.generateCoachingTip(
            userGoal = "Lose Weight",
            consumedCalories = 800,
            targetCalories = 1800,
            fallbackTip = "Fallback tip"
        )

        assertEquals(expectedTip, result)
    }

    @Test
    fun `estimateFoodNutrition delegates to metaAiService`() = runBlocking {
        val expected = FoodRecognitionResult(
            name = "Nasi Goreng",
            calories = 380,
            protein = 12.0,
            carbs = 48.0,
            fat = 14.0,
            fiber = 2.0
        )
        `when`(metaAiService.estimateFoodNutrition("Nasi Goreng")).thenReturn(Result.success(expected))

        val result = repository.estimateFoodNutrition("Nasi Goreng")
        assertEquals(Result.success(expected), result)
    }

    @Test
    fun `transcribeAudio delegates to metaAiService`() = runBlocking {
        val mockFile = java.io.File("test.wav")
        `when`(metaAiService.transcribeAudio(mockFile)).thenReturn(Result.success("Healthy salad with chicken"))

        val result = repository.transcribeAudio(mockFile)
        assertEquals(Result.success("Healthy salad with chicken"), result)
    }
}
