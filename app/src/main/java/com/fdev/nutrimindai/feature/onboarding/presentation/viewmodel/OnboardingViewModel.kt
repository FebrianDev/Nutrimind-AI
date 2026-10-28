package com.fdev.nutrimindai.feature.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.feature.onboarding.presentation.state.OnboardingUiState
import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.core.util.NutritionCalculator
import com.fdev.nutrimindai.core.sync.SyncManager
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel managing the onboarding wizard flow, input validation, daily target calculations,
 * and profile database persistence.
 *
 * @property profileRepository Single source of truth repository for saving profile entity.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
    private val weightLogRepository: WeightLogRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        val firebaseName = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
        } catch (e: Exception) {
            null
        }
        if (!firebaseName.isNullOrBlank()) {
            _uiState.update { it.copy(name = firebaseName) }
        }
    }

    /**
     * Updates user's input name and re-evaluates validation.
     */
    fun onNameChanged(name: String) {
        _uiState.update { currentState ->
            val newState = currentState.copy(name = name)
            validateStep1(newState)
        }
    }

    /**
     * Updates user's input age and re-evaluates validation.
     */
    fun onAgeChanged(age: String) {
        val filteredAge = age.filter { it.isDigit() }
        _uiState.update { currentState ->
            val newState = currentState.copy(age = filteredAge)
            validateStep1(newState)
        }
    }

    /**
     * Updates user's selected gender.
     */
    fun onGenderChanged(gender: Gender) {
        _uiState.update { currentState ->
            val newState = currentState.copy(gender = gender)
            validateStep1(newState)
        }
    }

    /**
     * Updates user's input height in cm and re-evaluates validation.
     */
    fun onHeightChanged(height: String) {
        val filteredHeight = height.filter { it.isDigit() }
        _uiState.update { currentState ->
            val newState = currentState.copy(height = filteredHeight)
            validateStep1(newState)
        }
    }

    /**
     * Updates user's input current weight in kg and re-evaluates validation.
     */
    fun onWeightChanged(weight: String) {
        val filteredWeight = filterWeightInput(weight)
        _uiState.update { currentState ->
            val newState = currentState.copy(
                currentWeight = filteredWeight,
                // Synchronize target weight if goal is MaintainWeight
                targetWeight = if (currentState.goal == Goal.MaintainWeight) filteredWeight else currentState.targetWeight
            )
            val validatedStep1 = validateStep1(newState)
            validateStep4(validatedStep1)
        }
    }

    /**
     * Handles selection of user activity level.
     */
    fun onActivityLevelSelected(activityLevel: ActivityLevel) {
        _uiState.update { currentState ->
            currentState.copy(
                activityLevel = activityLevel,
                isStep2Valid = true
            )
        }
    }

    /**
     * Handles selection of weight goal.
     */
    fun onGoalSelected(goal: Goal) {
        _uiState.update { currentState ->
            val updatedTargetWeight = if (goal == Goal.MaintainWeight) {
                currentState.currentWeight
            } else if (currentState.targetWeight.isEmpty()) {
                currentState.currentWeight
            } else {
                currentState.targetWeight
            }

            val newState = currentState.copy(
                goal = goal,
                targetWeight = updatedTargetWeight,
                isStep3Valid = true
            )
            validateStep4(newState)
        }
    }

    /**
     * Updates user's target weight in kg and re-evaluates Step 4 validation.
     */
    fun onTargetWeightChanged(targetWeight: String) {
        if (_uiState.value.goal == Goal.MaintainWeight) return
        val filteredTarget = filterWeightInput(targetWeight)
        _uiState.update { currentState ->
            val newState = currentState.copy(targetWeight = filteredTarget)
            validateStep4(newState)
        }
    }

    /**
     * Navigates to the next onboarding step or triggers Step 5 loading sequence.
     */
    fun onNextClicked() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < 5) {
            val nextStep = currentStep + 1
            _uiState.update { it.copy(currentStep = nextStep) }
            if (nextStep == 5) {
                startStep5LoadingSequence()
            }
        }
    }

    /**
     * Navigates to the previous onboarding step.
     */
    fun onBackClicked() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 1) {
            _uiState.update { it.copy(currentStep = currentStep - 1) }
        }
    }

    /**
     * Finalizes onboarding by saving the user profile into Room database and signalling completion.
     */
    fun onStartJourneyClicked() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentWeightVal = state.currentWeight.toFloatOrNull() ?: 70.0f
            val photoUrl = try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
            } catch (e: Exception) {
                null
            }

            val profile = ProfileEntity(
                name = state.name.trim(),
                age = state.age.toIntOrNull() ?: 25,
                gender = state.gender,
                height = state.height.toIntOrNull() ?: 170,
                currentWeight = currentWeightVal,
                activityLevel = state.activityLevel ?: ActivityLevel.Sedentary,
                goal = state.goal ?: Goal.MaintainWeight,
                targetWeight = state.targetWeight.toFloatOrNull() ?: currentWeightVal,
                dailyCalories = state.calculatedCalories,
                dailyProtein = state.calculatedProtein,
                dailyCarbs = state.calculatedCarbs,
                dailyFat = state.calculatedFat,
                dailyExerciseMinutes = state.calculatedExerciseMinutes,
                photoUrl = photoUrl
            )

            profileRepository.saveProfile(profile)

            // Record baseline weight log for today
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            weightLogRepository.logWeight(
                date = todayDateStr,
                weightKg = currentWeightVal,
                note = "Initial weight from onboarding"
            )

            preferenceManager.setOnboardingCompleted(true)
            syncManager.triggerManualSync()
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    /**
     * Simulated 2-second loading animation sequence for Step 5 plan computation.
     */
    private fun startStep5LoadingSequence() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStep5 = true) }

            val messages = listOf(
                "Analyzing profile...",
                "Calculating nutrition target...",
                "Preparing your personalized plan...",
                "Building Nutrition Memory..."
            )

            for (message in messages) {
                _uiState.update { it.copy(loadingStep5Text = message) }
                delay(500)
            }

            computePlanResults()
            _uiState.update { it.copy(isLoadingStep5 = false) }
        }
    }

    /**
     * Executes nutrition target calculation via [NutritionCalculator].
     */
    private fun computePlanResults() {
        val state = _uiState.value
        val age = state.age.toIntOrNull() ?: 25
        val height = state.height.toIntOrNull() ?: 170
        val weight = state.currentWeight.toFloatOrNull() ?: 70.0f
        val targetWeight = state.targetWeight.toFloatOrNull() ?: weight
        val activity = state.activityLevel ?: ActivityLevel.Sedentary
        val goal = state.goal ?: Goal.MaintainWeight

        val result = NutritionCalculator.calculatePlan(
            age = age,
            gender = state.gender,
            height = height,
            currentWeight = weight,
            targetWeight = targetWeight,
            activityLevel = activity,
            goal = goal
        )

        _uiState.update { currentState ->
            currentState.copy(
                calculatedCalories = result.dailyCalories,
                calculatedProtein = result.dailyProtein,
                calculatedCarbs = result.dailyCarbs,
                calculatedFat = result.dailyFat,
                calculatedExerciseMinutes = result.dailyExerciseMinutes,
                estimatedCompletionTime = result.estimatedTime
            )
        }
    }

    /**
     * Validates Step 1 inputs (Name, Age, Height, Weight).
     */
    private fun validateStep1(state: OnboardingUiState): OnboardingUiState {
        val isNameValid = state.name.trim().isNotEmpty()
        val ageVal = state.age.toIntOrNull()
        val isAgeValid = ageVal != null && ageVal in 1..120

        val heightVal = state.height.toIntOrNull()
        val isHeightValid = heightVal != null && heightVal in 50..250

        val weightVal = state.currentWeight.toFloatOrNull()
        val isWeightValid = weightVal != null && weightVal in 20.0f..300.0f

        val isValid = isNameValid && isAgeValid && isHeightValid && isWeightValid

        return state.copy(
            isStep1Valid = isValid
        )
    }

    /**
     * Validates Step 4 Target Weight input.
     */
    private fun validateStep4(state: OnboardingUiState): OnboardingUiState {
        val currentWeightVal = state.currentWeight.toFloatOrNull() ?: 0f
        val targetWeightVal = state.targetWeight.toFloatOrNull()

        val isTargetValid = when {
            targetWeightVal == null || targetWeightVal !in 20.0f..300.0f -> false
            state.goal == Goal.LoseWeight && targetWeightVal >= currentWeightVal -> false
            state.goal == Goal.GainWeight && targetWeightVal <= currentWeightVal -> false
            else -> true
        }

        val estimatedTime = if (isTargetValid && state.goal != null) {
            NutritionCalculator.calculateEstimatedTimeframe(
                currentWeight = currentWeightVal,
                targetWeight = targetWeightVal ?: currentWeightVal,
                goal = state.goal
            )
        } else {
            ""
        }

        return state.copy(
            isStep4Valid = isTargetValid,
            estimatedCompletionTime = estimatedTime
        )
    }

    /**
     * Helper to restrict weight input to valid numeric float pattern.
     */
    private fun filterWeightInput(input: String): String {
        var hasDot = false
        val builder = StringBuilder()
        for (ch in input) {
            if (ch.isDigit()) {
                builder.append(ch)
            } else if (ch == '.' && !hasDot) {
                hasDot = true
                builder.append(ch)
            }
        }
        return builder.toString()
    }
}
