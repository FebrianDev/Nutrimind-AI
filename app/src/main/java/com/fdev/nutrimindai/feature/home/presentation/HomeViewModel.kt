package com.fdev.nutrimindai.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.feature.dailyintake.data.seeder.DemoDataSeeder
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.model.TodaySummary
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.core.sync.SyncManager
import com.fdev.nutrimindai.core.util.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

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
    private val weightLogRepository: WeightLogRepository,
    private val demoDataSeeder: DemoDataSeeder,
    private val aiRepository: AiRepository,
    private val healthConnectManager: com.fdev.nutrimindai.core.health.HealthConnectManager,
    private val syncManager: SyncManager
) : ViewModel() {

    val isHealthConnectAvailable: Boolean
        get() = healthConnectManager.isAvailable

    val healthConnectPermissions: Set<String>
        get() = healthConnectManager.permissions

    suspend fun hasHealthConnectPermissions(): Boolean {
        return healthConnectManager.hasAllPermissions()
    }

    fun syncHealthConnect(onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (!healthConnectManager.isAvailable) {
                onResult("Google Health Connect is not available on this device.")
                return@launch
            }

            if (!healthConnectManager.hasAllPermissions()) {
                onResult("Health Connect permission is required.")
                return@launch
            }

            try {
                val metrics = healthConnectManager.readTodayMetrics()
                val currentDailyLogId = (uiState.value as? HomeUiState.Success)?.summary?.dailyLogId
                    ?: dailyLogRepository.getOrCreateDailyLog(currentDate)
                if (currentDailyLogId.isNotBlank()) {
                    val existing = exerciseRepository.getExercisesForDailyLogDirect(currentDailyLogId)
                        .find { it.exerciseType == com.fdev.nutrimindai.core.health.HealthConnectManager.HEALTH_CONNECT_EXERCISE_TYPE }

                    if (metrics.activeCalories > 0 || metrics.steps > 0) {
                        exerciseRepository.addExercise(
                            Exercise(
                                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                dailyLogId = currentDailyLogId,
                                exerciseType = com.fdev.nutrimindai.core.health.HealthConnectManager.HEALTH_CONNECT_EXERCISE_TYPE,
                                durationMinute = (metrics.steps / 100).toInt().coerceAtLeast(1),
                                caloriesBurned = metrics.activeCalories,
                                note = "Synced from Google Health Connect (${metrics.steps} steps)"
                            )
                        )
                        syncManager.triggerManualSync()
                        onResult("Successfully synced ${metrics.steps} steps & ${metrics.activeCalories} active kcal!")
                    } else if (existing != null) {
                        exerciseRepository.deleteExercise(existing)
                        syncManager.triggerManualSync()
                        onResult("Health Connect: No steps or active calorie data logged today.")
                    } else {
                        onResult("Health Connect: No steps or active calorie data logged today.")
                    }
                }
            } catch (e: Exception) {
                onResult("Failed to sync Health Connect: ${e.localizedMessage}")
            }
        }
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val activeProvider: StateFlow<AiProvider> = aiRepository.activeProviderFlow

    private val _coachingTip = MutableStateFlow<String>("")
    val coachingTip: StateFlow<String> = _coachingTip.asStateFlow()

    private val _isGeneratingTip = MutableStateFlow<Boolean>(false)
    val isGeneratingTip: StateFlow<Boolean> = _isGeneratingTip.asStateFlow()

    private var hasInitiatedTipGeneration = false

    private var lastLoadedDate: String = ""
    private var loadDataJob: Job? = null

    private val currentDate: String
        get() = LocalDate.now().toString()

    init {
        startObservingDateAndData()
    }

    fun checkDateAndReloadIfNeeded() {
        val today = currentDate
        if (today != lastLoadedDate) {
            startObservingDateAndData()
        }
    }

    private fun startObservingDateAndData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            val today = currentDate
            lastLoadedDate = today
            loadData(today)
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

    private suspend fun loadData(dateToLoad: String) {
        try {
            demoDataSeeder.seedIfNeeded()
            val dailyLogId = dailyLogRepository.getOrCreateDailyLog(dateToLoad)
            if (healthConnectManager.isAvailable && healthConnectManager.hasAllPermissions()) {
                syncHealthConnect()
            }

            combine(
                dailyLogRepository.getDailyLogByDate(dateToLoad),
                intakeRepository.getIntakesForDailyLog(dailyLogId),
                waterRepository.getWaterRecordsForDailyLog(dailyLogId),
                exerciseRepository.getExercisesForDailyLog(dailyLogId),
                profileRepository.getProfile(),
                weightLogRepository.getWeightLogByDate(dateToLoad)
            ) { args: Array<Any?> ->
                val dailyLog = args[0] as DailyLog?
                val intakes = @Suppress("UNCHECKED_CAST") (args[1] as List<Intake>)
                val waters = @Suppress("UNCHECKED_CAST") (args[2] as List<Water>)
                val exercises = @Suppress("UNCHECKED_CAST") (args[3] as List<Exercise>)
                val profile = args[4] as ProfileEntity?
                val weightLog = args[5] as WeightLogEntity?

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
                val totalExerciseMinutes = exercises.sumOf { it.durationMinute }
                val currentWeightKg = weightLog?.weightKg ?: profile?.currentWeight
                val isWeightLoggedToday = weightLog != null

                val dynamicWaterGoal = NutritionCalculator.calculateDynamicWaterTarget(
                    weightKg = currentWeightKg ?: 70f,
                    exerciseMinutes = totalExerciseMinutes
                )

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
                    goalFat = profile?.dailyFat ?: NutritionSummary.DEFAULT_GOAL_FAT,
                    goalWater = dynamicWaterGoal
                )

                TodaySummary(
                    date = dateToLoad,
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
                    nutritionSummary = nutritionSummary,
                    currentWeightKg = currentWeightKg,
                    isWeightLoggedToday = isWeightLoggedToday
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error loading home data")
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

                val score = NutritionCalculator.calculateHealthScore(
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

                // 1. Complete instantly with the local fallback summary (no network wait)
                dailyLogRepository.completeToday(fallbackSummary)
                syncManager.triggerManualSync()
                onCompleted()

                // 2. Generate the AI summary in the background and replace the fallback when ready
                val dailyLogId = currentSummary?.dailyLogId
                if (!dailyLogId.isNullOrBlank()) {
                    generateAiSummaryInBackground(
                        dailyLogId = dailyLogId,
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
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error completing today")
                dailyLogRepository.completeToday(null)
                syncManager.triggerManualSync()
                onCompleted()
            }
        }
    }

    private fun generateAiSummaryInBackground(
        dailyLogId: String,
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
    ) {
        viewModelScope.launch {
            try {
                val generated = aiRepository.generateDailyReviewSummary(
                    userGoal = userGoal,
                    score = score,
                    calories = calories,
                    targetCalories = targetCalories,
                    protein = protein,
                    targetProtein = targetProtein,
                    carbs = carbs,
                    targetCarbs = targetCarbs,
                    fat = fat,
                    targetFat = targetFat,
                    waterMl = waterMl,
                    targetWaterMl = targetWaterMl,
                    exerciseMinutes = exerciseMinutes,
                    targetExerciseMinutes = targetExerciseMinutes,
                    fallbackSummary = fallbackSummary
                )
                if (generated.isNotBlank() && generated != fallbackSummary) {
                    dailyLogRepository.saveAiSummary(dailyLogId, generated)
                    syncManager.triggerManualSync()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Background AI daily summary failed, keeping fallback")
            }
        }
    }

    fun reopenToday() {
        viewModelScope.launch {
            dailyLogRepository.reopenToday()
            syncManager.triggerManualSync()
        }
    }
}
