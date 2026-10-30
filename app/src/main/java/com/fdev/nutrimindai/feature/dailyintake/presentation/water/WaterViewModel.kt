package com.fdev.nutrimindai.feature.dailyintake.presentation.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.sync.SyncManager
import com.fdev.nutrimindai.core.util.NutritionCalculator
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

sealed interface WaterUiState {
    data object Loading : WaterUiState
    data class Success(
        val waterRecords: List<Water>,
        val totalMl: Int,
        val goalMl: Int = 2500,
        val exerciseMinutes: Int = 0,
        val status: DailyLogStatus
    ) : WaterUiState
    data class Error(val message: String) : WaterUiState
}

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<WaterUiState>(WaterUiState.Loading)
    val uiState: StateFlow<WaterUiState> = _uiState.asStateFlow()

    private var lastLoadedDate: String = ""
    private var loadJob: Job? = null

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val currentDate: String
        get() = LocalDate.now().toString()

    init {
        loadWater()
    }

    fun checkDateAndReloadIfNeeded() {
        if (currentDate != lastLoadedDate) {
            loadWater()
        }
    }

    private fun loadWater() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val dateToLoad = currentDate
                lastLoadedDate = dateToLoad
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(dateToLoad)

                combine(
                    dailyLogRepository.getDailyLogByDate(dateToLoad),
                    waterRepository.getWaterRecordsForDailyLog(dailyLogId),
                    exerciseRepository.getExercisesForDailyLog(dailyLogId),
                    profileRepository.getProfile()
                ) { dailyLog, records, exercises, profile ->
                    val status = dailyLog?.status ?: DailyLogStatus.DRAFT
                    val totalExerciseMin = exercises.sumOf { it.durationMinute }
                    val userWeight = profile?.currentWeight ?: 70f
                    val dynamicGoal = NutritionCalculator.calculateDynamicWaterTarget(userWeight, totalExerciseMin)

                    WaterUiState.Success(
                        waterRecords = records,
                        totalMl = records.sumOf { it.amountMl },
                        goalMl = dynamicGoal,
                        exerciseMinutes = totalExerciseMin,
                        status = status
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = WaterUiState.Error(e.message ?: "Failed to load water intake data.")
            }
        }
    }

    fun addWater(amountMl: Int) {
        if (amountMl <= 0) return
        checkDateAndReloadIfNeeded()
        val currentState = uiState.value
        if (currentState is WaterUiState.Success && currentState.status != DailyLogStatus.DRAFT && currentDate == lastLoadedDate) return

        viewModelScope.launch {
            try {
                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(currentDate)
                val timeString = LocalTime.now().format(timeFormatter)
                val water = Water(
                    id = 0,
                    dailyLogId = dailyLogId,
                    amountMl = amountMl,
                    time = timeString
                )
                waterRepository.addWater(water)
                syncManager.triggerManualSync()
            } catch (e: Exception) {
                _uiState.value = WaterUiState.Error(e.message ?: "Failed to record water intake.")
            }
        }
    }

    fun deleteWater(water: Water) {
        val currentState = uiState.value
        if (currentState is WaterUiState.Success && currentState.status == DailyLogStatus.DRAFT) {
            viewModelScope.launch {
                try {
                    waterRepository.deleteWater(water)
                    syncManager.triggerManualSync()
                } catch (e: Exception) {
                    _uiState.value = WaterUiState.Error(e.message ?: "Failed to delete water record.")
                }
            }
        }
    }
}
