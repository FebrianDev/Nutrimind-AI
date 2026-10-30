package com.fdev.nutrimindai.feature.dailyintake.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.util.MetExerciseCalculator
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.core.health.HealthConnectManager
import com.fdev.nutrimindai.core.health.HealthMetrics
import com.fdev.nutrimindai.core.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    private val dailyLogRepository: DailyLogRepository,
    private val profileRepository: ProfileRepository,
    private val healthConnectManager: HealthConnectManager,
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
                checkDateAndReloadIfNeeded()
                val metrics = healthConnectManager.readTodayMetrics()
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(currentDate)
                val existing = exerciseRepository.getExercisesForDailyLogDirect(dailyLogId)
                    .find { it.exerciseType == HealthConnectManager.HEALTH_CONNECT_EXERCISE_TYPE }

                if (metrics.activeCalories > 0 || metrics.steps > 0) {
                    exerciseRepository.addExercise(
                        Exercise(
                            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            exerciseType = HealthConnectManager.HEALTH_CONNECT_EXERCISE_TYPE,
                            durationMinute = (metrics.steps / 100).toInt().coerceAtLeast(1),
                            caloriesBurned = metrics.activeCalories,
                            note = "Synced from Google Health Connect (${metrics.steps} steps)"
                        )
                    )
                    syncManager.triggerManualSync()
                    onResult("Successfully synced ${metrics.steps} steps & ${metrics.activeCalories} kcal!")
                } else if (existing != null) {
                    exerciseRepository.deleteExercise(existing)
                    syncManager.triggerManualSync()
                    onResult("Health Connect: No steps or active calorie data logged today.")
                } else {
                    onResult("Health Connect: No steps or active calorie data logged today.")
                }
            } catch (e: Exception) {
                onResult("Failed to sync Health Connect: ${e.localizedMessage}")
            }
        }
    }

    private val _uiState = MutableStateFlow<ExerciseUiState>(ExerciseUiState.Loading)
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    private val _addExerciseUiState = MutableStateFlow<AddExerciseUiState>(AddExerciseUiState.Idle)
    val addExerciseUiState: StateFlow<AddExerciseUiState> = _addExerciseUiState.asStateFlow()

    private var lastLoadedDate: String = ""
    private var loadJob: Job? = null

    private val currentDate: String
        get() = LocalDate.now().toString()

    var userWeight: Float = MetExerciseCalculator.DEFAULT_WEIGHT_KG
        private set

    init {
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                if (profile != null && profile.currentWeight > 20f) {
                    userWeight = profile.currentWeight
                }
            }
        }
        loadExercises()
    }

    fun checkDateAndReloadIfNeeded() {
        if (currentDate != lastLoadedDate) {
            loadExercises()
        }
    }

    fun calculateMetCalories(type: String, durationMinutes: Int): Int {
        return MetExerciseCalculator.calculateCalories(
            exerciseType = type,
            durationMinutes = durationMinutes,
            weightKg = userWeight
        )
    }

    private fun loadExercises() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val dateToLoad = currentDate
                lastLoadedDate = dateToLoad
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(dateToLoad)

                combine(
                    dailyLogRepository.getDailyLogByDate(dateToLoad),
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
            } catch (e: CancellationException) {
                throw e
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
                    dailyLogId = dailyLogId,
                    exerciseType = exerciseType,
                    durationMinute = duration,
                    caloriesBurned = caloriesBurned,
                    note = note.takeIf { it.isNotBlank() }
                )

                exerciseRepository.addExercise(exercise)
                syncManager.triggerManualSync()
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
                    syncManager.triggerManualSync()
                } catch (e: Exception) {
                    _uiState.value = ExerciseUiState.Error(e.message ?: "Failed to delete exercise.")
                }
            }
        }
    }
}
