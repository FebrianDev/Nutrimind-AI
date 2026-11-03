package com.fdev.nutrimindai.feature.progress.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.core.util.DailyMissionEvaluator
import com.fdev.nutrimindai.core.util.EvaluatedDayMissions
import com.fdev.nutrimindai.core.util.MissionRewardData
import com.fdev.nutrimindai.core.util.NutritionCalculator
import com.fdev.nutrimindai.core.util.getAccountStartCalendar
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class DailyMissionHistoryUiState {
    data object Loading : DailyMissionHistoryUiState()
    data class Success(
        val rewardData: MissionRewardData,
        val selectedPeriodTab: Int = 0,
        val displayedHistory: List<EvaluatedDayMissions> = emptyList()
    ) : DailyMissionHistoryUiState()
    data class Error(val message: String) : DailyMissionHistoryUiState()
}

@HiltViewModel
class DailyMissionHistoryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyMissionHistoryUiState>(DailyMissionHistoryUiState.Loading)
    val uiState: StateFlow<DailyMissionHistoryUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)
    private val shortDatePattern = SimpleDateFormat("dd/MM", Locale.US)

    private var currentPeriodTab = 0
    private var cachedRewardData: MissionRewardData = MissionRewardData()

    init {
        loadHistory(0)
    }

    fun selectPeriodTab(tabIndex: Int) {
        currentPeriodTab = tabIndex
        loadHistory(tabIndex)
    }

    private fun loadHistory(periodTab: Int) {
        viewModelScope.launch {
            _uiState.value = DailyMissionHistoryUiState.Loading
            try {
                val dayCount = when (periodTab) {
                    0 -> 7
                    1 -> 30
                    2 -> 90
                    else -> 7
                }

                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val profile = profileRepository.getProfileDirect()
                val accountStartCal = getAccountStartCalendar(profile)

                val windowStartCal = (todayCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -(dayCount - 1))
                }

                // Constrain start date to user's account creation date to avoid empty pre-registration history
                val startCal = if (accountStartCal.after(windowStartCal)) {
                    if (accountStartCal.after(todayCal)) todayCal.clone() as Calendar else accountStartCal.clone() as Calendar
                } else {
                    windowStartCal
                }

                val startDate = dateFormat.format(startCal.time)
                val endDate = dateFormat.format(todayCal.time)
                val todayStr = dateFormat.format(todayCal.time)
                val yesterdayStr = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                    .let { dateFormat.format(it.time) }

                val targetCal = profile?.dailyCalories ?: 2100
                val targetProtein = profile?.dailyProtein ?: 120
                val targetExercise = profile?.dailyExerciseMinutes ?: 30
                val targetWater = NutritionCalculator.calculateDynamicWaterTarget(
                    weightKg = profile?.currentWeight ?: 70f,
                    exerciseMinutes = targetExercise
                )

                val logs = dailyLogRepository.getDailyLogsBetweenDatesDirect(startDate, endDate)
                val logByDate = logs.associateBy { it.date }
                val logIds = logs.map { it.id }

                val intakesByLogId = intakeRepository.getIntakesForDailyLogsDirect(logIds).groupBy { it.dailyLogId }
                val watersByLogId = waterRepository.getWaterRecordsForDailyLogsDirect(logIds).groupBy { it.dailyLogId }
                val exercisesByLogId = exerciseRepository.getExercisesForDailyLogsDirect(logIds).groupBy { it.dailyLogId }

                val dayCal = startCal.clone() as Calendar
                val evaluatedDays = mutableListOf<EvaluatedDayMissions>()

                while (!dayCal.after(todayCal)) {
                    val dateStr = dateFormat.format(dayCal.time)
                    val log = logByDate[dateStr]
                    val intakes = log?.let { intakesByLogId[it.id] }.orEmpty()
                    val waters = log?.let { watersByLogId[it.id] }.orEmpty()
                    val exercises = log?.let { exercisesByLogId[it.id] }.orEmpty()

                    val cals = intakes.sumOf { it.calories }
                    val protein = intakes.sumOf { it.protein }
                    val waterMl = waters.sumOf { it.amountMl }
                    val exerciseMinutes = exercises.sumOf { it.durationMinute }

                    val isToday = dateStr == todayStr
                    val isYesterday = dateStr == yesterdayStr
                    val dayLabel = when {
                        isToday -> "Today"
                        isYesterday -> "Yesterday"
                        dayCount <= 7 -> dayLabelFormat.format(dayCal.time)
                        else -> shortDatePattern.format(dayCal.time)
                    }

                    val evaluatedDay = DailyMissionEvaluator.evaluateDay(
                        goal = profile?.goal ?: Goal.MaintainWeight,
                        calories = cals,
                        goalCalories = targetCal,
                        protein = protein,
                        goalProtein = targetProtein,
                        waterMl = waterMl,
                        goalWaterMl = targetWater,
                        exerciseMinutes = exerciseMinutes,
                        intakes = intakes,
                        dateStr = dateStr,
                        dayLabel = dayLabel
                    )
                    evaluatedDays.add(evaluatedDay)
                    dayCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                cachedRewardData = DailyMissionEvaluator.calculateMissionRewards(evaluatedDays)

                _uiState.value = DailyMissionHistoryUiState.Success(
                    rewardData = cachedRewardData,
                    selectedPeriodTab = currentPeriodTab,
                    displayedHistory = cachedRewardData.history
                )
            } catch (e: Exception) {
                _uiState.value = DailyMissionHistoryUiState.Error(e.message ?: "Failed to load history")
            }
        }
    }
}
