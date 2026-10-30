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
import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.core.sync.SyncManager
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyReviewDetailData
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
    val targetCalories: Int = 2200,
    val totalProtein: Double,
    val targetProtein: Double = 140.0,
    val totalCarbs: Double = 0.0,
    val targetCarbs: Double = 250.0,
    val totalFat: Double = 0.0,
    val targetFat: Double = 65.0,
    val totalWaterMl: Int,
    val targetWaterMl: Int = 2500,
    val exerciseCalories: Int,
    val exerciseMinutes: Int = 0,
    val targetExerciseMinutes: Int = 30,
    val healthScore: Int,
    val aiSummary: String = "",
    val timelineItems: List<ActivityTimelineItemData>,
    val userName: String = "User"
) {
    fun toDailyReviewDetailData(): DailyReviewDetailData {
        return DailyReviewDetailData(
            dateStr = dateStr,
            displayDate = displayDate,
            userName = userName,
            score = healthScore,
            status = if (healthScore >= 90) "Excellent" else if (healthScore >= 75) "Good" else if (healthScore >= 60) "Fair" else "Needs Attention",
            calories = totalCalories,
            targetCalories = targetCalories,
            protein = totalProtein,
            targetProtein = targetProtein,
            carbs = totalCarbs,
            targetCarbs = targetCarbs,
            fat = totalFat,
            targetFat = targetFat,
            water = totalWaterMl,
            targetWater = targetWaterMl,
            exerciseMinutes = exerciseMinutes,
            targetExerciseMinutes = targetExerciseMinutes,
            aiSummary = aiSummary
        )
    }
}

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
    private val profileRepository: ProfileRepository,
    private val aiRepository: AiRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityDetailUiState>(ActivityDetailUiState.Loading)
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    private val generatingAiSummaryForLogIds = mutableSetOf<Long>()

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val currentDate: String
        get() = isoDateFormat.format(Date())

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

    private fun triggerBackgroundAiSummaryIfNeeded(
        dailyLogId: Long,
        userGoal: String,
        score: Int,
        totalCalories: Int,
        targetCal: Int,
        totalProtein: Double,
        targetProtein: Double,
        totalCarbs: Double,
        targetCarbs: Double,
        totalFat: Double,
        targetFat: Double,
        totalWater: Int,
        targetWater: Int,
        exerciseMinutes: Int,
        targetExercise: Int
    ) {
        if (dailyLogId <= 0L || !generatingAiSummaryForLogIds.add(dailyLogId)) return

        viewModelScope.launch {
            val fallback = "Great job completing your daily journal! Your nutritional intake has been successfully logged."
            try {
                val generated = aiRepository.generateDailyReviewSummary(
                    userGoal = userGoal,
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
                    exerciseMinutes = exerciseMinutes,
                    targetExerciseMinutes = targetExercise,
                    fallbackSummary = fallback
                )
                dailyLogRepository.saveAiSummary(dailyLogId, generated)
                syncManager.triggerManualSync()
            } catch (e: Exception) {
                dailyLogRepository.saveAiSummary(dailyLogId, fallback)
                syncManager.triggerManualSync()
            } finally {
                generatingAiSummaryForLogIds.remove(dailyLogId)
            }
        }
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
                    val totalCarbs = intakes.sumOf { it.carbs }
                    val totalFat = intakes.sumOf { it.fat }
                    val totalWater = waters.sumOf { it.amountMl }
                    val exerciseCalories = exercises.sumOf { it.caloriesBurned }
                    val exerciseMinutes = exercises.sumOf { it.durationMinute }

                    val parsedDate = runCatching { isoDateFormat.parse(targetDate) }.getOrNull()
                    val displayDate = if (parsedDate != null) displayDateFormat.format(parsedDate) else targetDate

                    val targetCal = profile?.dailyCalories ?: com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary.DEFAULT_GOAL_CALORIES
                    val targetProtein = profile?.dailyProtein?.toDouble() ?: 140.0
                    val targetCarbs = profile?.dailyCarbs?.toDouble() ?: 250.0
                    val targetFat = profile?.dailyFat?.toDouble() ?: 65.0
                    val targetWater = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateDynamicWaterTarget(
                        weightKg = profile?.currentWeight ?: 70f,
                        exerciseMinutes = exerciseMinutes
                    )
                    val targetExercise = profile?.dailyExerciseMinutes ?: 30

                    val score = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateHealthScore(
                        cals = totalCalories,
                        targetCal = targetCal,
                        protein = totalProtein,
                        targetProtein = targetProtein,
                        water = totalWater,
                        targetWater = targetWater,
                        exerciseMin = exerciseMinutes,
                        targetExerciseMin = targetExercise
                    )

                    val existingAiSummary = dailyLog?.aiSummary
                    val finalAiSummary = when {
                        !existingAiSummary.isNullOrBlank() -> existingAiSummary
                        dailyLog?.status == DailyLogStatus.COMPLETED -> {
                            triggerBackgroundAiSummaryIfNeeded(
                                dailyLogId = dailyLogId,
                                userGoal = profile?.goal?.name ?: "Maintain Weight",
                                score = score,
                                totalCalories = totalCalories,
                                targetCal = targetCal,
                                totalProtein = totalProtein,
                                targetProtein = targetProtein,
                                totalCarbs = totalCarbs,
                                targetCarbs = targetCarbs,
                                totalFat = totalFat,
                                targetFat = targetFat,
                                totalWater = totalWater,
                                targetWater = targetWater,
                                exerciseMinutes = exerciseMinutes,
                                targetExercise = targetExercise
                            )
                            "Generating your personalized Meta AI dietitian evaluation..."
                        }
                        totalCalories > 0 -> {
                            "Daily journal is in progress. Complete your day to generate a personalized Meta AI dietitian evaluation."
                        }
                        else -> {
                            "No meals logged yet today. Track your breakfast, lunch, or dinner to receive AI nutritional evaluation."
                        }
                    }

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
                        targetCalories = targetCal,
                        totalProtein = totalProtein,
                        targetProtein = targetProtein,
                        totalCarbs = totalCarbs,
                        targetCarbs = targetCarbs,
                        totalFat = totalFat,
                        targetFat = targetFat,
                        totalWaterMl = totalWater,
                        targetWaterMl = targetWater,
                        exerciseCalories = exerciseCalories,
                        exerciseMinutes = exerciseMinutes,
                        targetExerciseMinutes = targetExercise,
                        healthScore = score,
                        aiSummary = finalAiSummary,
                        timelineItems = timelineItems,
                        userName = profile?.name ?: "User"
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
            val currentData = (_uiState.value as? ActivityDetailUiState.Success)?.data
            val profile = profileRepository.getProfile().first()
            val fallback = "Great job completing your daily journal! Your nutritional intake has been successfully logged."

            val generatedSummary = if (currentData != null) {
                try {
                    aiRepository.generateDailyReviewSummary(
                        userGoal = profile?.goal?.name ?: "Maintain Weight",
                        score = currentData.healthScore,
                        calories = currentData.totalCalories,
                        targetCalories = currentData.targetCalories,
                        protein = currentData.totalProtein,
                        targetProtein = currentData.targetProtein,
                        carbs = currentData.totalCarbs,
                        targetCarbs = currentData.targetCarbs,
                        fat = currentData.totalFat,
                        targetFat = currentData.targetFat,
                        waterMl = currentData.totalWaterMl,
                        targetWaterMl = currentData.targetWaterMl,
                        exerciseMinutes = currentData.exerciseMinutes,
                        targetExerciseMinutes = currentData.targetExerciseMinutes,
                        fallbackSummary = fallback
                    )
                } catch (e: Exception) {
                    fallback
                }
            } else {
                fallback
            }

            dailyLogRepository.completeToday(generatedSummary)
            syncManager.triggerManualSync()
        }
    }

    fun reopenToday() {
        viewModelScope.launch {
            dailyLogRepository.reopenToday()
            syncManager.triggerManualSync()
        }
    }
}
