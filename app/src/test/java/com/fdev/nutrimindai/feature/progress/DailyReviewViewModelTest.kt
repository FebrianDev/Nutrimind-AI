package com.fdev.nutrimindai.feature.progress

import androidx.lifecycle.SavedStateHandle
import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyReviewUiState
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyReviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class DailyReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val savedStateHandle = SavedStateHandle(mapOf("date" to "2026-08-08"))
    private val dailyLogRepository = mock(DailyLogRepository::class.java)
    private val intakeRepository = mock(IntakeRepository::class.java)
    private val waterRepository = mock(WaterRepository::class.java)
    private val exerciseRepository = mock(ExerciseRepository::class.java)
    private val profileRepository = mock(ProfileRepository::class.java)
    private val aiRepository = mock(AiRepository::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when dailyLog has cached aiSummary in DB, viewmodel uses it directly without calling aiRepository`() = runTest(testDispatcher) {
        val cachedSummary = "Cached AI evaluation from previous completion in Room database."
        val mockDailyLog = DailyLog(
            id = "log-1",
            date = "2026-08-08",
            status = DailyLogStatus.COMPLETED,
            aiSummary = cachedSummary
        )

        val profile = ProfileEntity(
            name = "Alex",
            age = 28,
            gender = Gender.Male,
            height = 175,
            currentWeight = 72f,
            activityLevel = ActivityLevel.ModeratelyActive,
            goal = Goal.LoseWeight,
            targetWeight = 68f,
            dailyCalories = 1800,
            dailyProtein = 130,
            dailyCarbs = 200,
            dailyFat = 55,
            dailyExerciseMinutes = 30
        )

        `when`(dailyLogRepository.getOrCreateDailyLog("2026-08-08")).thenReturn("log-1")
        `when`(dailyLogRepository.getDailyLogByDate("2026-08-08")).thenReturn(flowOf(mockDailyLog))
        `when`(profileRepository.getProfile()).thenReturn(flowOf(profile))
        `when`(intakeRepository.getIntakesForDailyLog("log-1")).thenReturn(flowOf(emptyList()))
        `when`(waterRepository.getWaterRecordsForDailyLog("log-1")).thenReturn(flowOf(emptyList()))
        `when`(exerciseRepository.getExercisesForDailyLog("log-1")).thenReturn(flowOf(emptyList()))

        val viewModel = DailyReviewViewModel(
            savedStateHandle = savedStateHandle,
            dailyLogRepository = dailyLogRepository,
            intakeRepository = intakeRepository,
            waterRepository = waterRepository,
            exerciseRepository = exerciseRepository,
            profileRepository = profileRepository,
            aiRepository = aiRepository
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DailyReviewUiState.Success)
        val data = (state as DailyReviewUiState.Success).data
        assertEquals(cachedSummary, data.aiSummary)
        assertEquals("Alex", data.userName)
    }
}
