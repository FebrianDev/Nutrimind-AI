package com.fdev.nutrimindai.feature.progress.presentation.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.core.ai.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

data class DailyReviewDetailData(
    val dateStr: String = "",
    val displayDate: String = "",
    val score: Int = 0,
    val status: String = "Good",
    val calories: Int = 0,
    val targetCalories: Int = 2200,
    val protein: Double = 0.0,
    val targetProtein: Double = 140.0,
    val carbs: Double = 0.0,
    val targetCarbs: Double = 245.0,
    val fat: Double = 0.0,
    val targetFat: Double = 65.0,
    val water: Int = 0,
    val targetWater: Int = 2500,
    val exerciseMinutes: Int = 0,
    val targetExerciseMinutes: Int = 30,
    val aiSummary: String = "",
    val note: String = ""
)

sealed class DailyReviewUiState {
    data object Loading : DailyReviewUiState()
    data class Success(val data: DailyReviewDetailData) : DailyReviewUiState()
    data class Error(val message: String) : DailyReviewUiState()
}

@HiltViewModel
class DailyReviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyReviewUiState>(DailyReviewUiState.Loading)
    val uiState: StateFlow<DailyReviewUiState> = _uiState.asStateFlow()


    init {
        val dateParam: String? = savedStateHandle["date"]
        val targetDate = dateParam ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        loadReviewForDate(targetDate)
    }

    fun loadReviewForDate(dateStr: String) {
        viewModelScope.launch {
            try {
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(dateStr)

                combine(
                    profileRepository.getProfile(),
                    intakeRepository.getIntakesForDailyLog(dailyLogId),
                    waterRepository.getWaterRecordsForDailyLog(dailyLogId),
                    exerciseRepository.getExercisesForDailyLog(dailyLogId),
                    dailyLogRepository.getDailyLogByDate(dateStr)
                ) { profile, intakes, waters, exercises, dailyLog ->
                    val totalCalories = intakes.sumOf { it.calories }
                    val totalProtein = intakes.sumOf { it.protein }
                    val totalCarbs = intakes.sumOf { it.carbs }
                    val totalFat = intakes.sumOf { it.fat }
                    val totalWater = waters.sumOf { it.amountMl }
                    val totalExerciseMinutes = exercises.sumOf { it.durationMinute }

                    val targetCal = profile?.dailyCalories ?: 2200
                    val targetProtein = profile?.dailyProtein?.toDouble() ?: 140.0
                    val targetCarbs = profile?.dailyCarbs?.toDouble() ?: 245.0
                    val targetFat = profile?.dailyFat?.toDouble() ?: 65.0
                    val targetWater = NutritionSummary.DEFAULT_GOAL_WATER
                    val targetExercise = profile?.dailyExerciseMinutes ?: 30
                    val userGoalName = profile?.goal?.name ?: "Maintain Weight"

                    val score = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateHealthScore(
                        cals = totalCalories,
                        targetCal = targetCal,
                        protein = totalProtein,
                        targetProtein = targetProtein,
                        water = totalWater,
                        targetWater = targetWater,
                        exerciseMin = totalExerciseMinutes,
                        targetExerciseMin = targetExercise
                    )

                    val status = when {
                        score >= 90 -> "Excellent"
                        score >= 75 -> "Good"
                        score >= 60 -> "Fair"
                        else -> "Needs Attention"
                    }

                    val fallbackSummary = "Your AI-powered daily review will be generated once you complete your log for the day. " +
                            "Keep tracking your meals and activities to see your progress!"

                    val existingAiSummary = dailyLog?.aiSummary

                    val finalAiSummary = when {
                        !existingAiSummary.isNullOrBlank() -> {
                            // 1. Already generated and persisted in Room DB -> NO AI call!
                            existingAiSummary
                        }
                        dailyLog?.status == com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus.COMPLETED -> {
                            // 2. Completed log that hasn't cached AI summary yet -> generate once and store in DB
                            val generated = aiRepository.generateDailyReviewSummary(
                                userGoal = userGoalName,
                                score = score,
                                calories = totalCalories,
                                targetCalories = targetCal,
                                protein = totalProtein,
                                targetProtein = targetProtein,
                                carbs = totalCarbs,
                                targetCarbs = targetCarbs,
                                fat = totalFat,
                                targetFat = targetFat,
                                waterMl = totalWater,
                                targetWaterMl = targetWater,
                                exerciseMinutes = totalExerciseMinutes,
                                targetExerciseMinutes = targetExercise,
                                fallbackSummary = fallbackSummary
                            )
                            dailyLogRepository.saveAiSummary(dailyLogId, generated)
                            generated
                        }
                        else -> {
                            // 3. Draft log (not yet completed) -> DO NOT call Meta AI, use fast preview
                            fallbackSummary
                        }
                    }

                    val notesList = exercises.mapNotNull { it.note }.filter { it.isNotBlank() }
                    val noteText = if (notesList.isNotEmpty()) notesList.joinToString("; ") else "Completed daily log."

                    DailyReviewDetailData(
                        dateStr = dateStr,
                        displayDate = dateStr,
                        score = score,
                        status = status,
                        calories = totalCalories,
                        targetCalories = targetCal,
                        protein = totalProtein,
                        targetProtein = targetProtein,
                        carbs = totalCarbs,
                        targetCarbs = targetCarbs,
                        fat = totalFat,
                        targetFat = targetFat,
                        water = totalWater,
                        targetWater = targetWater,
                        exerciseMinutes = totalExerciseMinutes,
                        targetExerciseMinutes = targetExercise,
                        aiSummary = finalAiSummary,
                        note = noteText
                    )
                }.collect { detailData ->
                    _uiState.value = DailyReviewUiState.Success(detailData)
                }
            } catch (e: Exception) {
                _uiState.value = DailyReviewUiState.Error(e.message ?: "Failed to load review")
            }
        }
    }
}
