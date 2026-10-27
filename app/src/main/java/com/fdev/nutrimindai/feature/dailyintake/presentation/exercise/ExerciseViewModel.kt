package com.fdev.nutrimindai.feature.dailyintake.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
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

sealed interface ExerciseUiState {
    data object Loading : ExerciseUiState
    data class Success(
        val exercises: List<Exercise>,
        val totalCaloriesBurned: Int,
        val totalDuration: Int,
        val status: DailyLogStatus
    ) : ExerciseUiState
    data class Error(val message: String) : ExerciseUiState
}

sealed interface AddExerciseUiState {
    data object Idle : AddExerciseUiState
    data object Loading : AddExerciseUiState
    data class ValidationError(val message: String) : AddExerciseUiState
    data object Success : AddExerciseUiState
    data class Error(val message: String) : AddExerciseUiState
}

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val dailyLogRepository: DailyLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExerciseUiState>(ExerciseUiState.Loading)
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    private val _addExerciseUiState = MutableStateFlow<AddExerciseUiState>(AddExerciseUiState.Idle)
    val addExerciseUiState: StateFlow<AddExerciseUiState> = _addExerciseUiState.asStateFlow()

    private val currentDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            try {
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(currentDate)

                combine(
                    dailyLogRepository.getDailyLogByDate(currentDate),
                    exerciseRepository.getExercisesForDailyLog(dailyLogId)
                ) { dailyLog, list ->
                    val status = dailyLog?.status ?: DailyLogStatus.DRAFT
                    ExerciseUiState.Success(
                        exercises = list,
                        totalCaloriesBurned = list.sumOf { it.caloriesBurned },
                        totalDuration = list.sumOf { it.durationMinute },
                        status = status
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = ExerciseUiState.Error(e.message ?: "Failed to load exercise records.")
            }
        }
    }

    fun resetAddExerciseState() {
        _addExerciseUiState.value = AddExerciseUiState.Idle
    }

    fun addExercise(
        type: String,
        durationText: String,
        caloriesBurnedText: String,
        note: String,
        onSuccess: () -> Unit
    ) {
        val exerciseType = type.trim()
        if (exerciseType.isBlank()) {
            _addExerciseUiState.value = AddExerciseUiState.ValidationError("Exercise type is required.")
            return
        }

        val duration = durationText.toIntOrNull()
        if (duration == null || duration <= 0) {
            _addExerciseUiState.value = AddExerciseUiState.ValidationError("Valid exercise duration in minutes is required.")
            return
        }

        val caloriesBurned = caloriesBurnedText.toIntOrNull() ?: 0

        viewModelScope.launch {
            _addExerciseUiState.value = AddExerciseUiState.Loading
            try {
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(currentDate)
                val exercise = Exercise(
                    id = 0,
                    dailyLogId = dailyLogId,
                    exerciseType = exerciseType,
                    durationMinute = duration,
                    caloriesBurned = caloriesBurned,
                    note = note.takeIf { it.isNotBlank() }
                )

                exerciseRepository.addExercise(exercise)
                _addExerciseUiState.value = AddExerciseUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _addExerciseUiState.value = AddExerciseUiState.Error(e.message ?: "Failed to save exercise.")
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        val currentState = uiState.value
        if (currentState is ExerciseUiState.Success && currentState.status == DailyLogStatus.DRAFT) {
            viewModelScope.launch {
                try {
                    exerciseRepository.deleteExercise(exercise)
                } catch (e: Exception) {
                    _uiState.value = ExerciseUiState.Error(e.message ?: "Failed to delete exercise.")
                }
            }
        }
    }
}
