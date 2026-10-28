package com.fdev.nutrimindai.feature.dailyintake.presentation.nutrichef

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.SmartRecipe
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class NutriChefUiState(
    val remainingCalories: Int = 450,
    val remainingProtein: Double = 30.0,
    val remainingCarbs: Double = 40.0,
    val remainingFat: Double = 12.0,
    val selectedMealType: MealType = MealType.DINNER,
    val userGoal: String = "Maintenance",
    val isLoading: Boolean = false,
    val recipes: List<SmartRecipe> = emptyList(),
    val errorMessage: String? = null
)

sealed class NutriChefEvent {
    data class MealLoggedSuccess(val recipeTitle: String) : NutriChefEvent()
    data class ShowToast(val message: String) : NutriChefEvent()
}

@HiltViewModel
class NutriChefViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val intakeRepository: IntakeRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutriChefUiState())
    val uiState: StateFlow<NutriChefUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NutriChefEvent>()
    val events: SharedFlow<NutriChefEvent> = _events.asSharedFlow()

    private var currentDailyLogId: Long = 0L

    init {
        loadDailyNutritionBudget()
    }

    fun loadDailyNutritionBudget() {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val dailyLog = dailyLogRepository.getDailyLogByDate(todayStr).firstOrNull()
            if (dailyLog != null) {
                currentDailyLogId = dailyLog.id
                val intakes = intakeRepository.getIntakesForDailyLogDirect(dailyLog.id)
                val profile = profileRepository.getProfile().firstOrNull()

                val goalCalories = profile?.dailyCalories ?: 2100
                val goalProtein = (profile?.dailyProtein ?: 120).toDouble()
                val goalCarbs = (profile?.dailyCarbs ?: 245).toDouble()
                val goalFat = (profile?.dailyFat ?: 65).toDouble()
                val userGoal = profile?.goal?.name ?: "Maintenance"

                val consumedCalories = intakes.sumOf { it.calories }
                val consumedProtein = intakes.sumOf { it.protein }
                val consumedCarbs = intakes.sumOf { it.carbs }
                val consumedFat = intakes.sumOf { it.fat }

                val remainingCalories = (goalCalories - consumedCalories).coerceAtLeast(150)
                val remainingProtein = (goalProtein - consumedProtein).coerceAtLeast(10.0)
                val remainingCarbs = (goalCarbs - consumedCarbs).coerceAtLeast(10.0)
                val remainingFat = (goalFat - consumedFat).coerceAtLeast(5.0)

                // Pick smart default meal type by current time
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val defaultMealType = when {
                    currentHour < 11 -> MealType.BREAKFAST
                    currentHour < 16 -> MealType.LUNCH
                    currentHour < 21 -> MealType.DINNER
                    else -> MealType.OTHERS
                }

                _uiState.value = _uiState.value.copy(
                    remainingCalories = remainingCalories,
                    remainingProtein = remainingProtein,
                    remainingCarbs = remainingCarbs,
                    remainingFat = remainingFat,
                    selectedMealType = defaultMealType,
                    userGoal = userGoal
                )

                // Auto generate initial recipes
                generateRecipes()
            } else {
                generateRecipes()
            }
        }
    }

    fun onMealTypeSelected(mealType: MealType) {
        if (_uiState.value.selectedMealType != mealType) {
            _uiState.value = _uiState.value.copy(selectedMealType = mealType)
            generateRecipes()
        }
    }

    fun generateRecipes() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val result = aiRepository.generateSmartRecipes(
                remainingCalories = state.remainingCalories,
                remainingProtein = state.remainingProtein,
                remainingCarbs = state.remainingCarbs,
                remainingFat = state.remainingFat,
                mealType = state.selectedMealType,
                userGoal = state.userGoal
            )

            result.onSuccess { recipeList ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recipes = recipeList
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage
                )
            }
        }
    }

    fun logRecipeToIntake(recipe: SmartRecipe, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (currentDailyLogId == 0L) {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val dailyLog = dailyLogRepository.getDailyLogByDate(todayStr).firstOrNull()
                    if (dailyLog != null) {
                        currentDailyLogId = dailyLog.id
                    }
                }

                val intake = Intake(
                    id = 0L,
                    dailyLogId = currentDailyLogId,
                    mealType = recipe.mealType,
                    foodName = recipe.title,
                    servingSize = 1.0,
                    servingUnit = "porsi",
                    calories = recipe.calories,
                    protein = recipe.protein,
                    carbs = recipe.carbs,
                    fat = recipe.fat,
                    fiber = recipe.fiber,
                    imagePath = null,
                    foodSource = FoodSource.AI_CHEF,
                    createdAt = System.currentTimeMillis()
                )

                intakeRepository.addIntake(intake)
                _events.emit(NutriChefEvent.MealLoggedSuccess(recipe.title))
                onComplete()
            } catch (e: Exception) {
                _events.emit(NutriChefEvent.ShowToast("Failed to save recipe: ${e.localizedMessage}"))
            }
        }
    }
}
