package com.fdev.nutrimindai.feature.dailyintake.presentation.meal_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.sync.SyncManager
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import timber.log.Timber

sealed interface MealDetailUiState {
    data object Loading : MealDetailUiState
    data class Success(
        val mealType: String,
        val status: DailyLogStatus,
        val intakes: List<Intake>,
        val totalCalories: Int,
        val totalProtein: Double,
        val totalCarbs: Double,
        val totalFat: Double,
        val totalFiber: Double
    ) : MealDetailUiState
    data class Error(val message: String) : MealDetailUiState
}

@HiltViewModel
class MealDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val intakeRepository: IntakeRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    val mealType: String = checkNotNull(savedStateHandle["mealType"])

    private val _uiState = MutableStateFlow<MealDetailUiState>(MealDetailUiState.Loading)
    val uiState: StateFlow<MealDetailUiState> = _uiState.asStateFlow()

    private var lastLoadedDate: String = ""
    private var loadJob: Job? = null

    private val currentDate: String
        get() = LocalDate.now().toString()

    init {
        loadMealDetails()
    }

    fun checkDateAndReloadIfNeeded() {
        if (currentDate != lastLoadedDate) {
            loadMealDetails()
        }
    }

    private fun loadMealDetails() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val dateToLoad = currentDate
                lastLoadedDate = dateToLoad
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(dateToLoad)

                combine(
                    dailyLogRepository.getDailyLogByDate(dateToLoad),
                    intakeRepository.getIntakesForDailyLog(dailyLogId)
                ) { dailyLog, allIntakes ->
                    val status = dailyLog?.status ?: DailyLogStatus.DRAFT
                    val mealIntakes = allIntakes.filter { it.mealType.name.equals(mealType, ignoreCase = true) }

                    MealDetailUiState.Success(
                        mealType = mealType.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
                        status = status,
                        intakes = mealIntakes,
                        totalCalories = mealIntakes.sumOf { it.calories },
                        totalProtein = mealIntakes.sumOf { it.protein },
                        totalCarbs = mealIntakes.sumOf { it.carbs },
                        totalFat = mealIntakes.sumOf { it.fat },
                        totalFiber = mealIntakes.sumOf { it.fiber }
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = MealDetailUiState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun deleteIntake(intake: Intake) {
        val currentState = uiState.value
        if (currentState is MealDetailUiState.Success && currentState.status == DailyLogStatus.DRAFT) {
            viewModelScope.launch {
                try {
                    intakeRepository.deleteIntake(intake)
                    syncManager.triggerManualSync()
                    intake.imagePath?.let { path ->
                        try {
                            val file = File(path)
                            if (file.exists()) {
                                val deleted = file.delete()
                                Timber.d("Deleted orphan food image file ($path): $deleted")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to delete orphan food image file")
                        }
                    }
                } catch (e: Exception) {
                    _uiState.value = MealDetailUiState.Error(e.message ?: "Failed to delete food.")
                }
            }
        }
    }
}
