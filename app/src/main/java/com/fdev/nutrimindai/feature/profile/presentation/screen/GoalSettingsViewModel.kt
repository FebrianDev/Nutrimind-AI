package com.fdev.nutrimindai.feature.profile.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.onboarding.data.entity.ProfileEntity
import com.fdev.nutrimindai.feature.onboarding.data.repository.ProfileRepository
import com.fdev.nutrimindai.feature.onboarding.util.ActivityLevel
import com.fdev.nutrimindai.feature.onboarding.util.Gender
import com.fdev.nutrimindai.feature.onboarding.util.Goal
import com.fdev.nutrimindai.feature.onboarding.util.NutritionCalculator
import com.fdev.nutrimindai.feature.onboarding.util.NutritionPlanResult
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

data class GoalSettingsUiState(
    val name: String = "",
    val ageText: String = "25",
    val gender: Gender = Gender.Male,
    val heightText: String = "170",
    val currentWeightText: String = "70.0",
    val targetWeightText: String = "65.0",
    val activityLevel: ActivityLevel = ActivityLevel.ModeratelyActive,
    val goal: Goal = Goal.LoseWeight,
    val calculatedPlan: NutritionPlanResult? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val startWeight: Float? = null
) {
    val currentWeight: Float
        get() = currentWeightText.toFloatOrNull() ?: 70.0f

    val targetWeight: Float
        get() = targetWeightText.toFloatOrNull() ?: 65.0f

    val height: Int
        get() = heightText.toIntOrNull() ?: 170

    val age: Int
        get() = ageText.toIntOrNull() ?: 25

    val progressRatio: Float
        get() = when (goal) {
            Goal.MaintainWeight -> 1.0f
            Goal.LoseWeight -> {
                if (currentWeight <= targetWeight) 1.0f
                else {
                    val baseline = startWeight?.takeIf { it > targetWeight } ?: (currentWeight + 2.0f)
                    if (baseline <= targetWeight) 0.5f
                    else ((baseline - currentWeight) / (baseline - targetWeight)).coerceIn(0.05f, 1.0f)
                }
            }
            Goal.GainWeight -> {
                if (currentWeight >= targetWeight) 1.0f
                else {
                    val baseline = startWeight?.takeIf { it < targetWeight } ?: (currentWeight - 2.0f)
                    if (baseline >= targetWeight) 0.5f
                    else ((currentWeight - baseline) / (targetWeight - baseline)).coerceIn(0.05f, 1.0f)
                }
            }
        }

    val progressPercent: Int
        get() = (progressRatio * 100).roundToInt().coerceIn(0, 100)

    val targetDateFormatted: String
        get() {
            if (goal == Goal.MaintainWeight) return "Ongoing Maintenance"
            val diff = abs(targetWeight - currentWeight)
            if (diff < 0.1f) return "Target Reached 🎉"
            val weeks = (diff / 0.5f).roundToInt().coerceAtLeast(1)
            val cal = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, weeks)
            }
            return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(cal.time)
        }

    val dailyWaterLiters: Float
        get() = (currentWeight * 0.035f).coerceIn(2.0f, 4.5f)

    val dailyFiberGrams: Int
        get() = (((calculatedPlan?.dailyCalories ?: 2100) / 1000.0) * 14).roundToInt().coerceIn(20, 45)

    val isWeightGoalValid: Boolean
        get() {
            val cur = currentWeightText.toFloatOrNull() ?: return false
            val tgt = targetWeightText.toFloatOrNull() ?: return false
            if (cur <= 0f || tgt <= 0f) return false
            return when (goal) {
                Goal.LoseWeight -> tgt < cur
                Goal.GainWeight -> tgt > cur
                Goal.MaintainWeight -> true
            }
        }

    val isInputValid: Boolean
        get() {
            val cur = currentWeightText.toFloatOrNull() ?: return false
            val tgt = targetWeightText.toFloatOrNull() ?: return false
            val h = heightText.toIntOrNull() ?: return false
            val a = ageText.toIntOrNull() ?: return false
            if (cur <= 0f || tgt <= 0f || h <= 0 || a <= 0) return false
            return isWeightGoalValid
        }

    val goalWarningText: String?
        get() = when (goal) {
            Goal.LoseWeight -> if (targetWeight >= currentWeight && currentWeight > 0f) {
                "For weight loss, target weight (${targetWeightText} kg) must be less than current weight (${currentWeightText} kg)."
            } else null
            Goal.GainWeight -> if (targetWeight <= currentWeight && currentWeight > 0f) {
                "For weight gain, target weight (${targetWeightText} kg) must be greater than current weight (${currentWeightText} kg)."
            } else null
            Goal.MaintainWeight -> null
        }
}

@HiltViewModel
class GoalSettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val weightLogRepository: WeightLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalSettingsUiState())
    val uiState: StateFlow<GoalSettingsUiState> = _uiState.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    init {
        loadExistingProfile()
    }

    private fun loadExistingProfile() {
        viewModelScope.launch {
            try {
                val existing = profileRepository.getProfileDirect()
                val allLogs = weightLogRepository.getAllWeightLogsDirect()
                val earliestWeight = allLogs.minByOrNull { it.date }?.weightKg

                if (existing != null) {
                    _uiState.value = GoalSettingsUiState(
                        name = existing.name,
                        ageText = existing.age.toString(),
                        gender = existing.gender,
                        heightText = existing.height.toString(),
                        currentWeightText = existing.currentWeight.toString(),
                        targetWeightText = existing.targetWeight.toString(),
                        activityLevel = existing.activityLevel,
                        goal = existing.goal,
                        startWeight = earliestWeight ?: existing.currentWeight,
                        isLoading = false
                    )
                    recalculatePlan()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        startWeight = earliestWeight
                    )
                    recalculatePlan()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load profile data"
                )
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onAgeChanged(value: String) {
        _uiState.value = _uiState.value.copy(ageText = value.filter { it.isDigit() })
        recalculatePlan()
    }

    fun onGenderChanged(value: Gender) {
        _uiState.value = _uiState.value.copy(gender = value)
        recalculatePlan()
    }

    fun onHeightChanged(value: String) {
        _uiState.value = _uiState.value.copy(heightText = value.filter { it.isDigit() })
        recalculatePlan()
    }

    fun onCurrentWeightChanged(value: String) {
        _uiState.value = _uiState.value.copy(currentWeightText = value)
        recalculatePlan()
    }

    fun onTargetWeightChanged(value: String) {
        _uiState.value = _uiState.value.copy(targetWeightText = value)
        recalculatePlan()
    }

    fun onActivityLevelChanged(value: ActivityLevel) {
        _uiState.value = _uiState.value.copy(activityLevel = value)
        recalculatePlan()
    }

    fun onGoalChanged(value: Goal) {
        val curWeight = _uiState.value.currentWeightText.toFloatOrNull() ?: 70.0f
        val tgtWeight = _uiState.value.targetWeightText.toFloatOrNull() ?: 65.0f

        val updatedTarget = when (value) {
            Goal.LoseWeight -> if (tgtWeight >= curWeight) "${(curWeight - 2.0f).coerceAtLeast(30.0f)}" else _uiState.value.targetWeightText
            Goal.GainWeight -> if (tgtWeight <= curWeight) "${curWeight + 2.0f}" else _uiState.value.targetWeightText
            Goal.MaintainWeight -> "$curWeight"
        }

        _uiState.value = _uiState.value.copy(
            goal = value,
            targetWeightText = updatedTarget
        )
        recalculatePlan()
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun recalculatePlan() {
        val state = _uiState.value
        val age = state.ageText.toIntOrNull() ?: 25
        val height = state.heightText.toIntOrNull() ?: 170
        val currentWeight = state.currentWeightText.toFloatOrNull() ?: 70.0f
        val targetWeight = state.targetWeightText.toFloatOrNull() ?: 65.0f

        val plan = NutritionCalculator.calculatePlan(
            age = age,
            gender = state.gender,
            height = height,
            currentWeight = currentWeight,
            targetWeight = targetWeight,
            activityLevel = state.activityLevel,
            goal = state.goal
        )

        _uiState.value = _uiState.value.copy(calculatedPlan = plan)
    }

    fun saveGoalSettings() {
        val state = _uiState.value
        if (state.isSaving || !state.isInputValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val age = state.ageText.toIntOrNull() ?: 25
                val height = state.heightText.toIntOrNull() ?: 170
                val currentWeight = state.currentWeightText.toFloatOrNull() ?: 70.0f
                val targetWeight = state.targetWeightText.toFloatOrNull() ?: 65.0f

                val plan = state.calculatedPlan ?: NutritionCalculator.calculatePlan(
                    age = age,
                    gender = state.gender,
                    height = height,
                    currentWeight = currentWeight,
                    targetWeight = targetWeight,
                    activityLevel = state.activityLevel,
                    goal = state.goal
                )

                val updatedEntity = ProfileEntity(
                    name = state.name.ifBlank { "NutriMind User" },
                    age = age,
                    gender = state.gender,
                    height = height,
                    currentWeight = currentWeight,
                    targetWeight = targetWeight,
                    activityLevel = state.activityLevel,
                    goal = state.goal,
                    dailyCalories = plan.dailyCalories,
                    dailyProtein = plan.dailyProtein,
                    dailyCarbs = plan.dailyCarbs,
                    dailyFat = plan.dailyFat,
                    dailyExerciseMinutes = plan.dailyExerciseMinutes,
                    updatedAt = System.currentTimeMillis()
                )

                profileRepository.saveProfile(updatedEntity)

                // Record weight log entry for today
                val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                weightLogRepository.logWeight(
                    date = todayDateStr,
                    weightKg = currentWeight,
                    note = "Updated from Goal Settings"
                )

                _uiState.value = _uiState.value.copy(isSaving = false)
                _saveSuccessEvent.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save goal settings"
                )
            }
        }
    }
}
