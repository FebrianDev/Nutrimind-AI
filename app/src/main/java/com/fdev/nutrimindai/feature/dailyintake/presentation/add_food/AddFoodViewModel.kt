package com.fdev.nutrimindai.feature.dailyintake.presentation.add_food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.FoodRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed interface AddFoodUiState {
    data object Idle : AddFoodUiState
    data object Loading : AddFoodUiState
    data class ValidationError(val message: String) : AddFoodUiState
    data object Success : AddFoodUiState
    data class Error(val message: String) : AddFoodUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val intakeRepository: IntakeRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddFoodUiState>(AddFoodUiState.Idle)
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val foods: StateFlow<List<Food>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                foodRepository.getAllFoods()
            } else {
                foodRepository.searchFoods(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun resetState() {
        _uiState.value = AddFoodUiState.Idle
    }

    fun saveFood(
        mealTypeString: String,
        foodName: String,
        servingSizeText: String,
        servingUnit: String,
        caloriesText: String,
        proteinText: String,
        carbsText: String,
        fatText: String,
        fiberText: String = "0",
        foodSource: FoodSource,
        onSuccess: () -> Unit
    ) {
        val nameTrimmed = foodName.trim()
        if (nameTrimmed.isBlank()) {
            _uiState.value = AddFoodUiState.ValidationError("Food name is required.")
            return
        }

        val calories = caloriesText.toIntOrNull()
        if (calories == null || calories < 0) {
            _uiState.value = AddFoodUiState.ValidationError("Valid calories amount is required.")
            return
        }

        val servingSize = servingSizeText.toDoubleOrNull() ?: 1.0
        val protein = proteinText.toDoubleOrNull() ?: 0.0
        val carbs = carbsText.toDoubleOrNull() ?: 0.0
        val fat = fatText.toDoubleOrNull() ?: 0.0
        val fiber = fiberText.toDoubleOrNull() ?: 0.0

        val mealType = try {
            MealType.valueOf(mealTypeString.uppercase())
        } catch (e: Exception) {
            MealType.BREAKFAST
        }

        viewModelScope.launch {
            _uiState.value = AddFoodUiState.Loading
            try {
                val dailyLog = dailyLogRepository.getDailyLogByDateDirect(currentDate)
                if (dailyLog != null && dailyLog.status != DailyLogStatus.DRAFT) {
                    _uiState.value = AddFoodUiState.Error("Today's journal is completed and read-only.")
                    return@launch
                }

                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(currentDate)
                val intake = Intake(
                    id = 0,
                    dailyLogId = dailyLogId,
                    mealType = mealType,
                    foodName = nameTrimmed,
                    servingSize = servingSize,
                    servingUnit = servingUnit.ifBlank { "serving" },
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    imagePath = null,
                    foodSource = foodSource
                )

                intakeRepository.addIntake(intake)

                // Also save custom manual entry into food catalog so it can be searched later
                if (foodSource == FoodSource.MANUAL) {
                    foodRepository.insertFood(
                        Food(
                            id = 0,
                            name = nameTrimmed,
                            calories = calories,
                            protein = protein,
                            carbs = carbs,
                            fat = fat,
                            servingSize = servingSize,
                            servingUnit = servingUnit.ifBlank { "serving" }
                        )
                    )
                }

                _uiState.value = AddFoodUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = AddFoodUiState.Error(e.message ?: "Failed to save food.")
            }
        }
    }
}
