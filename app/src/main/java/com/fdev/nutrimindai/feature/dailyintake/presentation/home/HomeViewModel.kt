package com.fdev.nutrimindai.feature.dailyintake.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.data.seeder.DemoDataSeeder
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.model.TodaySummary
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.onboarding.data.repository.ProfileRepository
import com.fdev.nutrimindai.feature.onboarding.util.Goal
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

import com.fdev.nutrimindai.core.ai.AiRepository
import timber.log.Timber

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val summary: TodaySummary) : HomeUiState()
    data object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val demoDataSeeder: DemoDataSeeder,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val activeProvider: StateFlow<com.fdev.nutrimindai.core.ai.AiProvider> = aiRepository.activeProviderFlow

    private val _coachingTip = MutableStateFlow<String>("")
    val coachingTip: StateFlow<String> = _coachingTip.asStateFlow()

    private val _isGeneratingTip = MutableStateFlow<Boolean>(false)
    val isGeneratingTip: StateFlow<Boolean> = _isGeneratingTip.asStateFlow()

    private var hasInitiatedTipGeneration = false

    private val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        viewModelScope.launch {
            demoDataSeeder.seedIfNeeded()
            loadData()
        }
    }

    private fun buildBaselineTip(goal: Goal, consumed: Int, target: Int): String {
        val remaining = target - consumed
        return when {
            consumed == 0 -> "Start your day with balanced nutrition! Log your meals to keep your goals on track."
            remaining > 500 -> "You have $remaining kcal remaining today. Make sure to prioritize protein and fresh vegetables for your next meal!"
            remaining in 0..500 -> "You're nicely on pace with your calorie target today! Remember to drink plenty of water."
            else -> "You've fulfilled your daily calorie target. Keep hydrated and focus on a restful recovery evening!"
        }
    }

    fun refreshCoachingTip(force: Boolean = true) {
        if (_isGeneratingTip.value) return
        val currentSummary = (_uiState.value as? HomeUiState.Success)?.summary ?: return
        viewModelScope.launch {
            _isGeneratingTip.value = true
            val goal = currentSummary.userGoal
            val consumed = currentSummary.nutritionSummary.calories
            val target = currentSummary.nutritionSummary.goalCalories
            val fallback = buildBaselineTip(goal, consumed, target)
            try {
                val tip = aiRepository.generateCoachingTip(
                    userGoal = goal.name,
                    consumedCalories = consumed,
                    targetCalories = target,
                    fallbackTip = fallback
                )
                if (tip.isNotBlank()) {
                    _coachingTip.value = tip
                } else {
                    _coachingTip.value = fallback
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to generate coaching tip")
                if (_coachingTip.value.isBlank()) {
                    _coachingTip.value = fallback
                }
            } finally {
                _isGeneratingTip.value = false
            }
        }
    }

    private suspend fun loadData() {
        try {
            val dailyLogId = dailyLogRepository.getOrCreateDailyLog(currentDate)

            combine(
                dailyLogRepository.getDailyLogByDate(currentDate),
                intakeRepository.getIntakesForDailyLog(dailyLogId),
                waterRepository.getWaterRecordsForDailyLog(dailyLogId),
                exerciseRepository.getExercisesForDailyLog(dailyLogId),
                profileRepository.getProfile()
            ) { dailyLog, intakes, waters, exercises, profile ->
                val status = dailyLog?.status ?: DailyLogStatus.DRAFT

                val breakfast = intakes.filter { it.mealType == MealType.BREAKFAST }
                val lunch = intakes.filter { it.mealType == MealType.LUNCH }
                val dinner = intakes.filter { it.mealType == MealType.DINNER }
                val others = intakes.filter { it.mealType == MealType.OTHERS }

                val totalWater = waters.sumOf { it.amountMl }
                val totalCalories = intakes.sumOf { it.calories }
                val totalProtein = intakes.sumOf { it.protein }
                val totalCarbs = intakes.sumOf { it.carbs }
                val totalFat = intakes.sumOf { it.fat }
                val totalFiber = intakes.sumOf { it.fiber }
                val totalExerciseCalories = exercises.sumOf { it.caloriesBurned }

                val nutritionSummary = NutritionSummary(
                    calories = totalCalories,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    fiber = totalFiber,
                    water = totalWater,
                    exerciseCalories = totalExerciseCalories,
                    goalCalories = profile?.dailyCalories ?: NutritionSummary.DEFAULT_GOAL_CALORIES,
                    goalProtein = profile?.dailyProtein ?: NutritionSummary.DEFAULT_GOAL_PROTEIN,
                    goalCarbs = profile?.dailyCarbs ?: NutritionSummary.DEFAULT_GOAL_CARBS,
                    goalFat = profile?.dailyFat ?: NutritionSummary.DEFAULT_GOAL_FAT
                )

                TodaySummary(
                    date = currentDate,
                    dailyLogId = dailyLogId,
                    userName = profile?.name ?: "User",
                    userPhotoUrl = profile?.photoUrl,
                    userGoal = profile?.goal ?: Goal.MaintainWeight,
                    status = status,
                    breakfastList = breakfast,
                    lunchList = lunch,
                    dinnerList = dinner,
                    othersList = others,
                    waterList = waters,
                    waterTotalMl = totalWater,
                    exerciseList = exercises,
                    nutritionSummary = nutritionSummary
                )
            }.collect { summary ->
                _uiState.value = HomeUiState.Success(summary)
                if (!hasInitiatedTipGeneration) {
                    hasInitiatedTipGeneration = true
                    val baseline = buildBaselineTip(summary.userGoal, summary.nutritionSummary.calories, summary.nutritionSummary.goalCalories)
                    _coachingTip.value = baseline
                    refreshCoachingTip(force = false)
                }
            }
        } catch (e: Exception) {
            _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun completeToday(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val currentSummary = (_uiState.value as? HomeUiState.Success)?.summary
                val userGoalName = currentSummary?.userGoal?.name ?: "Maintain Weight"
                val totalCalories = currentSummary?.nutritionSummary?.calories ?: 0
                val targetCal = currentSummary?.nutritionSummary?.goalCalories ?: 2200
                val totalProtein = currentSummary?.nutritionSummary?.protein ?: 0.0
                val targetProtein = currentSummary?.nutritionSummary?.goalProtein?.toDouble() ?: 140.0
                val totalCarbs = currentSummary?.nutritionSummary?.carbs ?: 0.0
                val targetCarbs = currentSummary?.nutritionSummary?.goalCarbs?.toDouble() ?: 245.0
                val totalFat = currentSummary?.nutritionSummary?.fat ?: 0.0
                val targetFat = currentSummary?.nutritionSummary?.goalFat?.toDouble() ?: 65.0
                val totalWater = currentSummary?.nutritionSummary?.water ?: 0
                val targetWater = currentSummary?.nutritionSummary?.goalWater ?: 2500
                val totalExerciseMinutes = currentSummary?.exerciseList?.sumOf { it.durationMinute } ?: 0
                val targetExercise = 30

                val score = com.fdev.nutrimindai.feature.onboarding.util.NutritionCalculator.calculateHealthScore(
                    cals = totalCalories,
                    targetCal = targetCal,
                    protein = totalProtein,
                    targetProtein = targetProtein,
                    water = totalWater,
                    targetWater = targetWater,
                    exerciseMin = totalExerciseMinutes,
                    targetExerciseMin = targetExercise
                )

                val fallbackSummary = when {
                    score >= 90 -> "Outstanding day! You accomplished all nutrition targets and maintained optimal balance."
                    score >= 75 -> "Great work today! You stayed close to your targets. Hydrate well for tomorrow!"
                    else -> "Solid effort today. Consistent daily tracking builds long-term health."
                }

                val generatedAiSummary = aiRepository.generateDailyReviewSummary(
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

                dailyLogRepository.completeToday(generatedAiSummary)
                onCompleted()
            } catch (e: Exception) {
                Timber.e(e, "Error completing today with AI summary")
                dailyLogRepository.completeToday(null)
                onCompleted()
            }
        }
    }

    fun reopenToday() {
        viewModelScope.launch {
            dailyLogRepository.reopenToday()
        }
    }
}
