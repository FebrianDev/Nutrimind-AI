package com.fdev.nutrimindai.feature.dailyintake

import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.SmartRecipe
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.presentation.nutrichef.NutriChefEvent
import com.fdev.nutrimindai.feature.dailyintake.presentation.nutrichef.NutriChefViewModel
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NutriChefViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var aiRepository: AiRepository
    private lateinit var intakeRepository: IntakeRepository
    private lateinit var dailyLogRepository: DailyLogRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var syncManager: com.fdev.nutrimindai.core.sync.SyncManager
    private lateinit var viewModel: NutriChefViewModel

    private val sampleRecipe = SmartRecipe(
        id = "recipe_1",
        title = "Dada Ayam Panggang Rosemary",
        description = "Menu sehat rendah kalori tinggi protein",
        prepTimeMinutes = 15,
        calories = 380,
        protein = 35.0,
        carbs = 10.0,
        fat = 8.0,
        fiber = 3.0,
        mealType = MealType.DINNER,
        ingredients = listOf("150g dada ayam", "1 sdt rosemary"),
        instructions = listOf("Panggang ayam hingga matang"),
        chefTip = "Gunakan perasan jeruk lemon"
    )

    private val sampleProfile = ProfileEntity(
        id = "user_profile",
        name = "Test User",
        age = 25,
        gender = Gender.Male,
        height = 175,
        currentWeight = 70f,
        activityLevel = ActivityLevel.ModeratelyActive,
        goal = Goal.MaintainWeight,
        targetWeight = 70f,
        dailyCalories = 2000,
        dailyProtein = 120,
        dailyCarbs = 250,
        dailyFat = 65,
        dailyExerciseMinutes = 30
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiRepository = mock()
        intakeRepository = mock()
        dailyLogRepository = mock()
        profileRepository = mock()
        syncManager = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDailyNutritionBudget calculates remaining calories and triggers recipe generation`() = runTest(testDispatcher) {
        val dailyLog = DailyLog(id = "log-1", date = "2026-10-28", status = DailyLogStatus.DRAFT)
        val existingIntakes = listOf(
            Intake(
                id = "intake-1",
                dailyLogId = "log-1",
                mealType = MealType.BREAKFAST,
                foodName = "Oatmeal",
                servingSize = 1.0,
                servingUnit = "bowl",
                calories = 500,
                protein = 20.0,
                carbs = 80.0,
                fat = 10.0,
                fiber = 5.0,
                imagePath = null,
                foodSource = FoodSource.MANUAL
            )
        )

        whenever(dailyLogRepository.getDailyLogByDate(any())).thenReturn(flowOf(dailyLog))
        whenever(dailyLogRepository.getOrCreateDailyLog(any())).thenReturn("log-1")
        whenever(intakeRepository.getIntakesForDailyLogDirect("log-1")).thenReturn(existingIntakes)
        whenever(profileRepository.getProfile()).thenReturn(flowOf(sampleProfile))
        whenever(aiRepository.generateSmartRecipes(any(), any(), any(), any(), any(), any()))
            .thenReturn(Result.success(listOf(sampleRecipe)))

        viewModel = NutriChefViewModel(aiRepository, intakeRepository, dailyLogRepository, profileRepository, syncManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1500, state.remainingCalories) // 2000 - 500
        assertEquals(100.0, state.remainingProtein, 0.1) // 120 - 20
        assertFalse(state.isLoading)
        assertEquals(1, state.recipes.size)
        assertEquals("Dada Ayam Panggang Rosemary", state.recipes.first().title)
    }

    @Test
    fun `logRecipeToIntake successfully saves Intake record with FoodSource AI_CHEF`() = runTest(testDispatcher) {
        val dailyLog = DailyLog(id = "log-10", date = "2026-10-28", status = DailyLogStatus.DRAFT)
        whenever(dailyLogRepository.getDailyLogByDate(any())).thenReturn(flowOf(dailyLog))
        whenever(dailyLogRepository.getOrCreateDailyLog(any())).thenReturn("log-10")
        whenever(intakeRepository.getIntakesForDailyLogDirect(any())).thenReturn(emptyList())
        whenever(profileRepository.getProfile()).thenReturn(flowOf(sampleProfile))
        whenever(aiRepository.generateSmartRecipes(any(), any(), any(), any(), any(), any()))
            .thenReturn(Result.success(listOf(sampleRecipe)))

        viewModel = NutriChefViewModel(aiRepository, intakeRepository, dailyLogRepository, profileRepository, syncManager)
        advanceUntilIdle()

        var onCompleteCalled = false
        viewModel.logRecipeToIntake(sampleRecipe) {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        val intakeCaptor = argumentCaptor<Intake>()
        verify(intakeRepository).addIntake(intakeCaptor.capture())
        verify(syncManager).triggerManualSync()

        val savedIntake = intakeCaptor.firstValue
        assertEquals("Dada Ayam Panggang Rosemary", savedIntake.foodName)
        assertEquals(380, savedIntake.calories)
        assertEquals(FoodSource.AI_CHEF, savedIntake.foodSource)
        assertTrue(onCompleteCalled)
    }
}
