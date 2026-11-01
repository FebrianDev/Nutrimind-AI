package com.fdev.nutrimindai.feature.progress.presentation.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TrendDetailData(
    val metric: String = "Calories",
    val unit: String = "kcal",
    val selectedPeriodTab: Int = 0, // 0 = Week, 1 = Month, 2 = 3 Months
    val avgValueStr: String = "0",
    val percentChangeStr: String = "+0%",
    val isPositiveTrend: Boolean = true,
    val highestValStr: String = "0",
    val highestSub: String = "-",
    val lowestValStr: String = "0",
    val lowestSub: String = "-",
    val goalValStr: String = "0",
    val chartPoints: List<Float> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    val pointTypes: List<Boolean>? = null
)

sealed class TrendDetailUiState {
    data object Loading : TrendDetailUiState()
    data class Success(val data: TrendDetailData) : TrendDetailUiState()
    data class Error(val message: String) : TrendDetailUiState()
}

@HiltViewModel
class TrendDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val weightLogRepository: WeightLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrendDetailUiState>(TrendDetailUiState.Loading)
    val uiState: StateFlow<TrendDetailUiState> = _uiState.asStateFlow()


    private val metricParam: String = savedStateHandle["metric"] ?: "Calories"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        loadMetricTrend(metricParam, 0)
    }

    fun selectPeriodTab(tabIndex: Int) {
        loadMetricTrend(metricParam, tabIndex)
    }

    private fun loadMetricTrend(metric: String, periodTab: Int) {
        viewModelScope.launch {
            try {
                val dayCount = when (periodTab) {
                    0 -> 7
                    1 -> 30
                    2 -> 90
                    else -> 7
                }

                val endDate = dateFormat.format(Date())
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -(dayCount - 1))
                val startDate = dateFormat.format(cal.time)

                val profile = profileRepository.getProfileDirect()
                val logs = dailyLogRepository.getDailyLogsBetweenDatesDirect(startDate, endDate)
                val logByDate = logs.associateBy { it.date }
                val logIds = logs.map { it.id }

                val intakesByLogId = if (metric != "Water" && metric != "Exercise") {
                    intakeRepository.getIntakesForDailyLogsDirect(logIds).groupBy { it.dailyLogId }
                } else {
                    emptyMap()
                }
                val watersByLogId = if (metric == "Water") {
                    waterRepository.getWaterRecordsForDailyLogsDirect(logIds).groupBy { it.dailyLogId }
                } else {
                    emptyMap()
                }
                val exercisesByLogId = if (metric == "Exercise" || metric == "Weight") {
                    exerciseRepository.getExercisesForDailyLogsDirect(logIds).groupBy { it.dailyLogId }
                } else {
                    emptyMap()
                }
                val weightLogs = if (metric == "Weight") weightLogRepository.getAllWeightLogsDirect() else emptyList()
                val weightByDate = if (metric == "Weight") weightLogs.associateBy { it.date } else emptyMap()

                val dayLabelFormat = if (dayCount <= 7) SimpleDateFormat("EEE", Locale.US) else SimpleDateFormat("dd/MM", Locale.US)

                val points = mutableListOf<Float>()
                val labels = mutableListOf<String>()
                val pointTypes = mutableListOf<Boolean>()

                val dayCal = cal.clone() as Calendar
                var highestVal = Float.MIN_VALUE
                var highestLabel = "-"
                var lowestVal = Float.MAX_VALUE
                var lowestLabel = "-"

                // Pre-initialize anchor weight & running balance for O(N) weight projection
                val initialAnchorLog = if (metric == "Weight") {
                    weightLogs.filter { it.date < startDate }.maxByOrNull { it.date }
                } else null
                var currentAnchorWeight = initialAnchorLog?.weightKg ?: profile?.currentWeight ?: 70.0f
                var currentAnchorDate = initialAnchorLog?.date ?: ""
                var runningNetBalance = 0.0

                // If initialAnchorDate is before startDate, accumulate any logs between initialAnchorDate and startDate
                if (metric == "Weight" && currentAnchorDate.isNotBlank() && profile != null) {
                    val tdee = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateTdee(
                        profile.age,
                        profile.gender,
                        profile.height,
                        currentAnchorWeight,
                        profile.activityLevel
                    )
                    for (bLog in logs) {
                        if (bLog.date > currentAnchorDate && bLog.date < startDate) {
                            val c = intakesByLogId[bLog.id].orEmpty().sumOf { it.calories }
                            val ex = exercisesByLogId[bLog.id].orEmpty().sumOf { it.caloriesBurned }
                            if (c > 0) {
                                runningNetBalance += (c - (tdee + ex))
                            }
                        }
                    }
                }

                for (i in 0 until dayCount) {
                    val dateStr = dateFormat.format(dayCal.time)
                    val label = dayLabelFormat.format(dayCal.time)
                    labels.add(label)

                    val log = logByDate[dateStr]
                    var valForDay = 0f

                    when (metric) {
                        "Calories" -> {
                            if (log != null) {
                                val intakes = intakesByLogId[log.id].orEmpty()
                                valForDay = intakes.sumOf { it.calories }.toFloat()
                            }
                        }
                        "Protein" -> {
                            if (log != null) {
                                val intakes = intakesByLogId[log.id].orEmpty()
                                valForDay = intakes.sumOf { it.protein }.toFloat()
                            }
                        }
                        "Carbs" -> {
                            if (log != null) {
                                val intakes = intakesByLogId[log.id].orEmpty()
                                valForDay = intakes.sumOf { it.carbs }.toFloat()
                            }
                        }
                        "Fat" -> {
                            if (log != null) {
                                val intakes = intakesByLogId[log.id].orEmpty()
                                valForDay = intakes.sumOf { it.fat }.toFloat()
                            }
                        }
                        "Water" -> {
                            if (log != null) {
                                val waters = watersByLogId[log.id].orEmpty()
                                valForDay = waters.sumOf { it.amountMl } / 1000f
                            }
                        }
                        "Weight" -> {
                            val loggedForDay = weightByDate[dateStr]?.weightKg
                            pointTypes.add(loggedForDay != null)
                            if (loggedForDay != null) {
                                currentAnchorWeight = loggedForDay
                                currentAnchorDate = dateStr
                                runningNetBalance = 0.0
                                valForDay = loggedForDay
                            } else {
                                if (currentAnchorDate.isNotBlank() && dateStr > currentAnchorDate && profile != null) {
                                    if (log != null) {
                                        val c = intakesByLogId[log.id].orEmpty().sumOf { it.calories }
                                        val ex = exercisesByLogId[log.id].orEmpty().sumOf { it.caloriesBurned }
                                        if (c > 0) {
                                            val tdee = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateTdee(
                                                profile.age,
                                                profile.gender,
                                                profile.height,
                                                currentAnchorWeight,
                                                profile.activityLevel
                                            )
                                            runningNetBalance += (c - (tdee + ex))
                                        }
                                    }
                                    valForDay = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateProjectedWeight(
                                        currentAnchorWeight,
                                        runningNetBalance
                                    )
                                } else {
                                    valForDay = currentAnchorWeight
                                }
                            }
                        }
                        "Exercise" -> {
                            if (log != null) {
                                val exercises = exercisesByLogId[log.id].orEmpty()
                                valForDay = exercises.sumOf { it.durationMinute }.toFloat()
                            }
                        }
                        else -> {
                            if (log != null) {
                                val intakes = intakesByLogId[log.id].orEmpty()
                                valForDay = intakes.sumOf { it.calories }.toFloat()
                            }
                        }
                    }

                    points.add(valForDay)

                    if (valForDay > highestVal) {
                        highestVal = valForDay
                        highestLabel = label
                    }
                    if (valForDay < lowestVal && valForDay > 0f) {
                        lowestVal = valForDay
                        lowestLabel = label
                    }

                    dayCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val unit = when (metric) {
                    "Calories" -> "kcal"
                    "Protein", "Carbs", "Fat" -> "g"
                    "Water" -> "L"
                    "Weight" -> "kg"
                    "Exercise" -> "min"
                    else -> ""
                }

                val avg = if (points.isNotEmpty()) points.filter { it > 0 }.let { if (it.isNotEmpty()) it.average().toFloat() else 0f } else 0f
                val avgStr = if (metric == "Water" || metric == "Weight") String.format(Locale.US, "%.1f", avg) else String.format(Locale.US, "%,d", avg.toInt())

                val highStr = if (highestVal != Float.MIN_VALUE) {
                    if (metric == "Water" || metric == "Weight") String.format(Locale.US, "%.1f", highestVal) else String.format(Locale.US, "%,d", highestVal.toInt())
                } else "0"

                val lowStr = if (lowestVal != Float.MAX_VALUE) {
                    if (metric == "Water" || metric == "Weight") String.format(Locale.US, "%.1f", lowestVal) else String.format(Locale.US, "%,d", lowestVal.toInt())
                } else "0"

                val goalValStr = when (metric) {
                    "Calories" -> "${profile?.dailyCalories ?: 2200}"
                    "Protein" -> "${profile?.dailyProtein ?: 140}"
                    "Carbs" -> "${profile?.dailyCarbs ?: 245}"
                    "Fat" -> "${profile?.dailyFat ?: 65}"
                    "Water" -> {
                        val waterTargetMl = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateDynamicWaterTarget(
                            profile?.currentWeight ?: 70f,
                            profile?.dailyExerciseMinutes ?: 30
                        )
                        String.format(Locale.US, "%.1f", waterTargetMl / 1000f)
                    }
                    "Weight" -> String.format(Locale.US, "%.1f", profile?.targetWeight ?: 68.0f)
                    "Exercise" -> "${profile?.dailyExerciseMinutes ?: 30}"
                    else -> "0"
                }

                // Dynamically compute percent change comparing first half of window to second half
                val activePoints = points.filter { it > 0f }
                val (percentChangeStr, isPositiveTrend) = if (activePoints.size >= 2) {
                    val halfSize = (activePoints.size / 2).coerceAtLeast(1)
                    val firstHalfAvg = activePoints.take(halfSize).average()
                    val secondHalfAvg = activePoints.takeLast(halfSize).average()
                    if (firstHalfAvg > 0) {
                        val diffPercent = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0
                        val sign = if (diffPercent >= 0) "+" else ""
                        val formatted = "$sign${String.format(Locale.US, "%.1f", diffPercent)}%"
                        val isPositive = if (metric == "Weight" && profile?.goal == Goal.LoseWeight) {
                            diffPercent <= 0
                        } else {
                            diffPercent >= 0
                        }
                        formatted to isPositive
                    } else {
                        "+0%" to true
                    }
                } else {
                    "+0%" to true
                }

                _uiState.value = TrendDetailUiState.Success(
                    TrendDetailData(
                        metric = metric,
                        unit = unit,
                        selectedPeriodTab = periodTab,
                        avgValueStr = avgStr,
                        percentChangeStr = percentChangeStr,
                        isPositiveTrend = isPositiveTrend,
                        highestValStr = highStr,
                        highestSub = highestLabel,
                        lowestValStr = lowStr,
                        lowestSub = lowestLabel,
                        goalValStr = goalValStr,
                        chartPoints = points,
                        chartLabels = labels,
                        pointTypes = if (metric == "Weight") pointTypes else null
                    )
                )
            } catch (e: Exception) {
                _uiState.value = TrendDetailUiState.Error(e.message ?: "Failed to load trend detail")
            }
        }
    }
}
