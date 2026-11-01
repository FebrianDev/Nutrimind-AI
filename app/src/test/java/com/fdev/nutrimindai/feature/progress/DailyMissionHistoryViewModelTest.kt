package com.fdev.nutrimindai.feature.progress

import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyMissionHistoryUiState
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyMissionHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class DailyMissionHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val profileRepository = mock(ProfileRepository::class.java)
    private val dailyLogRepository = mock(DailyLogRepository::class.java)
    private val intakeRepository = mock(IntakeRepository::class.java)
    private val waterRepository = mock(WaterRepository::class.java)
    private val exerciseRepository = mock(ExerciseRepository::class.java)

    private val mockProfile = ProfileEntity(
        id = "user_profile",
        name = "Champion",
        gender = Gender.Male,
        age = 25,
        height = 175,
        currentWeight = 70f,
        targetWeight = 65f,
        activityLevel = ActivityLevel.ModeratelyActive,
        goal = Goal.LoseWeight,
        dailyCalories = 1800,
        dailyProtein = 120,
        dailyCarbs = 200,
        dailyFat = 60,
        dailyExerciseMinutes = 30
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadHistory produces success state with 7 days history initially`() = runTest(testDispatcher) {
        `when`(profileRepository.getProfileDirect()).thenReturn(mockProfile)
        `when`(dailyLogRepository.getDailyLogsBetweenDatesDirect(any(), any())).thenReturn(emptyList())
        `when`(intakeRepository.getIntakesForDailyLogsDirect(any())).thenReturn(emptyList())
        `when`(waterRepository.getWaterRecordsForDailyLogsDirect(any())).thenReturn(emptyList())
        `when`(exerciseRepository.getExercisesForDailyLogsDirect(any())).thenReturn(emptyList())

        val viewModel = DailyMissionHistoryViewModel(
            profileRepository = profileRepository,
            dailyLogRepository = dailyLogRepository,
            intakeRepository = intakeRepository,
            waterRepository = waterRepository,
            exerciseRepository = exerciseRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DailyMissionHistoryUiState.Success)
        val success = state as DailyMissionHistoryUiState.Success
        assertEquals(7, success.displayedHistory.size)
        assertEquals(0, success.selectedPeriodTab)
    }

    @Test
    fun `selectPeriodTab changes displayed history period correctly`() = runTest(testDispatcher) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val mockLog = DailyLog(
            id = "log-1",
            date = todayStr,
            status = DailyLogStatus.COMPLETED
        )

        val mockIntakes = listOf(
            Intake(
                id = "1",
                dailyLogId = "log-1",
                mealType = MealType.BREAKFAST,
                foodName = "Spinach eggs",
                servingSize = 1.0,
                servingUnit = "portion",
                calories = 400,
                protein = 40.0,
                carbs = 10.0,
                fat = 15.0,
                fiber = 4.0,
                imagePath = null,
                foodSource = FoodSource.MANUAL
            )
        )

        `when`(profileRepository.getProfileDirect()).thenReturn(mockProfile)
        `when`(dailyLogRepository.getDailyLogsBetweenDatesDirect(any(), any())).thenReturn(listOf(mockLog))
        `when`(intakeRepository.getIntakesForDailyLogsDirect(any())).thenReturn(mockIntakes)
        `when`(waterRepository.getWaterRecordsForDailyLogsDirect(any())).thenReturn(emptyList())
        `when`(exerciseRepository.getExercisesForDailyLogsDirect(any())).thenReturn(emptyList())

        val viewModel = DailyMissionHistoryViewModel(
            profileRepository = profileRepository,
            dailyLogRepository = dailyLogRepository,
            intakeRepository = intakeRepository,
            waterRepository = waterRepository,
            exerciseRepository = exerciseRepository
        )

        advanceUntilIdle()

        var state = viewModel.uiState.value as DailyMissionHistoryUiState.Success
        assertEquals(7, state.displayedHistory.size)
        assertEquals(0, state.selectedPeriodTab)

        viewModel.selectPeriodTab(1)
        advanceUntilIdle()

        state = viewModel.uiState.value as DailyMissionHistoryUiState.Success
        assertEquals(30, state.displayedHistory.size)
        assertEquals(1, state.selectedPeriodTab)
    }
}
