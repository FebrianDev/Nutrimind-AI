package com.fdev.nutrimindai.feature.activity.presentation.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

data class ActivityTimelineItemData(
    val id: String,
    val time: String,
    val title: String,
    val subtitle: String,
    val value: String,
    val type: String, // "BREAKFAST", "LUNCH", "DINNER", "OTHERS", "WATER", "EXERCISE"
    val isLocked: Boolean
)

data class ActivityDetailData(
    val dateStr: String,
    val displayDate: String,
    val isToday: Boolean,
    val status: String,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalWaterMl: Int,
    val exerciseCalories: Int,
    val healthScore: Int,
    val timelineItems: List<ActivityTimelineItemData>
)

sealed class ActivityDetailUiState {
    data object Loading : ActivityDetailUiState()
    data class Success(val data: ActivityDetailData) : ActivityDetailUiState()
    data class Error(val message: String) : ActivityDetailUiState()
}

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityDetailUiState>(ActivityDetailUiState.Loading)
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val currentDate: String = isoDateFormat.format(Date())

    val targetDate: String

    init {
        val rawDate: String? = savedStateHandle["date"]
        targetDate = parseOrNormalizeDate(rawDate)
        loadActivityDetail()
    }

    private fun parseOrNormalizeDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return currentDate
        // If already yyyy-MM-dd
        if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return rawDate

        // Try parsing display date e.g. "July 25, 2026"
        return runCatching {
            val dateObj = displayDateFormat.parse(rawDate)
            if (dateObj != null) isoDateFormat.format(dateObj) else rawDate
        }.getOrDefault(rawDate)
    }

    private fun loadActivityDetail() {
        viewModelScope.launch {
            try {
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(targetDate)

                combine(
                    dailyLogRepository.getDailyLogByDate(targetDate),
                    intakeRepository.getIntakesForDailyLog(dailyLogId),
                    waterRepository.getWaterRecordsForDailyLog(dailyLogId),
                    exerciseRepository.getExercisesForDailyLog(dailyLogId),
                    profileRepository.getProfile()
                ) { dailyLog, intakes, waters, exercises, profile ->
                    val isToday = targetDate == currentDate
                    val statusText = if (dailyLog?.status == DailyLogStatus.COMPLETED) "Completed" else "Draft"

                    val totalCalories = intakes.sumOf { it.calories }
                    val totalProtein = intakes.sumOf { it.protein }
                    val totalWater = waters.sumOf { it.amountMl }
                    val exerciseCalories = exercises.sumOf { it.caloriesBurned }
                    val exerciseMinutes = exercises.sumOf { it.durationMinute }

                    val parsedDate = runCatching { isoDateFormat.parse(targetDate) }.getOrNull()
                    val displayDate = if (parsedDate != null) displayDateFormat.format(parsedDate) else targetDate

                    val targetCal = com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary.DEFAULT_GOAL_CALORIES
                    val targetProtein = profile?.dailyProtein?.toDouble() ?: 140.0
                    val targetWater = com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary.DEFAULT_GOAL_WATER
                    val targetExercise = profile?.dailyExerciseMinutes ?: 30

                    val score = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateHealthScore(
                        cals = totalCalories,
                        targetCal = profile?.dailyCalories ?: targetCal,
                        protein = totalProtein,
                        targetProtein = targetProtein,
                        water = totalWater,
                        targetWater = targetWater,
                        exerciseMin = exerciseMinutes,
                        targetExerciseMin = targetExercise
                    )

                    val timelineItems = mutableListOf<ActivityTimelineItemData>()
                    val isLocked = dailyLog?.status == DailyLogStatus.COMPLETED

                    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

                    // Group Intakes by MealType
                    MealType.entries.forEach { mealType ->
                        val mealIntakes = intakes.filter { it.mealType == mealType }
                        if (mealIntakes.isNotEmpty()) {
                            val latestCreatedAt = mealIntakes.maxOfOrNull { it.createdAt } ?: 0L
                            val timeStr = if (latestCreatedAt > 0L) {
                                timeFormatter.format(Date(latestCreatedAt))
                            } else {
                                when (mealType) {
                                    MealType.BREAKFAST -> "07:30 AM"
                                    MealType.LUNCH -> "12:30 PM"
                                    MealType.DINNER -> "07:00 PM"
                                    MealType.OTHERS -> "04:00 PM"
                                }
                            }
                            val mealCalories = mealIntakes.sumOf { it.calories }
                            val subtitleStr = mealIntakes.joinToString(", ") { it.foodName }
                            val titleStr = mealType.name.lowercase().replaceFirstChar { it.uppercase() }

                            timelineItems.add(
                                ActivityTimelineItemData(
                                    id = "meal_${mealType.name}",
                                    time = timeStr,
                                    title = titleStr,
                                    subtitle = subtitleStr,
                                    value = "$mealCalories kcal",
                                    type = mealType.name,
                                    isLocked = isLocked
                                )
                            )
                        }
                    }

                    // Water items
                    if (waters.isNotEmpty()) {
                        val waterTime = waters.lastOrNull()?.time ?: "02:00 PM"
                        timelineItems.add(
                            ActivityTimelineItemData(
                                id = "water_summary",
                                time = waterTime,
                                title = "Water Intake",
                                subtitle = "${waters.size} log(s)",
                                value = "+$totalWater ml",
                                type = "WATER",
                                isLocked = isLocked
                            )
                        )
                    }

                    // Exercise items
                    exercises.forEachIndexed { idx, ex ->
                        val exTimeStr = if (ex.createdAt > 0L) {
                            timeFormatter.format(Date(ex.createdAt))
                        } else {
                            "05:30 PM"
                        }
                        timelineItems.add(
                            ActivityTimelineItemData(
                                id = "exercise_$idx",
                                time = exTimeStr,
                                title = ex.exerciseType,
                                subtitle = "${ex.durationMinute} min • ${ex.note ?: "Exercise"}",
                                value = "${ex.caloriesBurned} kcal",
                                type = "EXERCISE",
                                isLocked = isLocked
                            )
                        )
                    }

                    ActivityDetailData(
                        dateStr = targetDate,
                        displayDate = displayDate,
                        isToday = isToday,
                        status = statusText,
                        totalCalories = totalCalories,
                        totalProtein = totalProtein,
                        totalWaterMl = totalWater,
                        exerciseCalories = exerciseCalories,
                        healthScore = score,
                        timelineItems = timelineItems
                    )
                }.collect { detailData ->
                    _uiState.value = ActivityDetailUiState.Success(detailData)
                }
            } catch (e: Exception) {
                _uiState.value = ActivityDetailUiState.Error(e.message ?: "Failed to load activity details.")
            }
        }
    }

    fun completeToday() {
        viewModelScope.launch {
            dailyLogRepository.completeToday()
        }
    }

    fun reopenToday() {
        viewModelScope.launch {
            dailyLogRepository.reopenToday()
        }
    }
}
