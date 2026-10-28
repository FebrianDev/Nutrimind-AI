package com.fdev.nutrimindai.feature.progress.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.designsystem.LightSuccess
import com.fdev.nutrimindai.core.designsystem.NutritionFat
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.core.util.AdaptiveCalibrationResult
import com.fdev.nutrimindai.core.util.NutritionCalculator
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

sealed class DateRangeFilter {
    data object Last1Day : DateRangeFilter()
    data object Last3Days : DateRangeFilter()
    data object Last7Days : DateRangeFilter()
    data object Last30Days : DateRangeFilter()
    data class Custom(val startDateMillis: Long, val endDateMillis: Long) : DateRangeFilter()

    fun getLabel(): String = when (this) {
        is Last1Day -> "1 Day"
        is Last3Days -> "3 Days"
        is Last7Days -> "7 Days"
        is Last30Days -> "30 Days"
        is Custom -> "Custom"
    }
}

data class DailyTrendPoint(
    val dayLabel: String,
    val dateStr: String,
    val calories: Float,
    val protein: Float,
    val water: Float,
    val weight: Float,
    val exercise: Float,
    val isActualWeight: Boolean = true
)

data class ProgressGoalData(
    val goalTitle: String = "Weight Loss Goal",
    val progressPercent: Float = 0.6f,
    val currentWeightKg: Float = 72.0f,
    val targetWeightKg: Float = 68.0f,
    val remainingDays: Int = 30,
    val isUsingProjection: Boolean = false,
    val lastActualWeightKg: Float = 72.0f,
    val lastActualWeightDate: String? = null,
    val projectedDeltaKg: Float = 0.0f,
    val metabolicNotice: String? = null,
    val needsCalibrationPrompt: Boolean = false
)

data class ProgressData(
    val goalData: ProgressGoalData = ProgressGoalData(),
    val todayNutrition: NutritionSummary = NutritionSummary(),
    val completedDays: Int = 0,
    val totalDays: Int = 7,
    val avgCalories: Int = 0,
    val waterGoalPercent: Int = 0,
    val missionPercent: Int = 0,
    val weekRange: String = "",
    val weeklyTrendPoints: List<DailyTrendPoint> = emptyList(),
    val dailyReviews: List<DailyReviewData> = emptyList(),
    val aiInsight: String = "",
    val selectedFilter: DateRangeFilter = DateRangeFilter.Last7Days,
    val userName: String = "Champion"
)

sealed class ProgressUiState {
    data object Loading : ProgressUiState()
    data class Success(val data: ProgressData) : ProgressUiState()
    data object Empty : ProgressUiState()
    data class Error(val message: String) : ProgressUiState()
}

private data class DayRecord(
    val dateStr: String,
    val calendar: Calendar,
    val dailyLog: DailyLog?,
    val intakes: List<Intake>,
    val waters: List<Water>,
    val exercises: List<Exercise>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val profileRepository: ProfileRepository,
    private val weightLogRepository: WeightLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProgressUiState>(ProgressUiState.Loading)
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow<DateRangeFilter>(DateRangeFilter.Last7Days)
    val selectedFilter: StateFlow<DateRangeFilter> = _selectedFilter.asStateFlow()

    private val _calibrationFeedback = MutableStateFlow<AdaptiveCalibrationResult?>(null)
    val calibrationFeedback: StateFlow<AdaptiveCalibrationResult?> = _calibrationFeedback.asStateFlow()

    fun dismissCalibrationFeedback() {
        _calibrationFeedback.value = null
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val dateDisplayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val currentDate: String
        get() = dateFormat.format(Date())

    init {
        loadProgress()
    }

    fun selectDateRangeFilter(filter: DateRangeFilter) {
        _selectedFilter.value = filter
    }

    fun logWeight(weightKg: Float, note: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                // If user was using AI projection, trigger Adaptive Metabolic Calibration evaluation
                val currentGoalData = (_uiState.value as? ProgressUiState.Success)?.data?.goalData
                if (currentGoalData != null && currentGoalData.isUsingProjection) {
                    val feedback = NutritionCalculator.evaluateMetabolicCalibration(
                        actualWeight = weightKg,
                        projectedWeight = currentGoalData.currentWeightKg
                    )
                    _calibrationFeedback.value = feedback
                }

                weightLogRepository.logWeight(
                    date = currentDate,
                    weightKg = weightKg,
                    note = note
                )

                // Seamlessly synchronize ProfileEntity.currentWeight and recalculate daily targets
                val currentProfile = profileRepository.getProfileDirect()
                if (currentProfile != null) {
                    val plan = NutritionCalculator.calculatePlan(
                        age = currentProfile.age,
                        gender = currentProfile.gender,
                        height = currentProfile.height,
                        currentWeight = weightKg,
                        targetWeight = currentProfile.targetWeight,
                        activityLevel = currentProfile.activityLevel,
                        goal = currentProfile.goal
                    )
                    val updatedProfile = currentProfile.copy(
                        currentWeight = weightKg,
                        dailyCalories = plan.dailyCalories,
                        dailyProtein = plan.dailyProtein,
                        dailyCarbs = plan.dailyCarbs,
                        dailyFat = plan.dailyFat,
                        dailyExerciseMinutes = plan.dailyExerciseMinutes,
                        updatedAt = System.currentTimeMillis()
                    )
                    profileRepository.updateProfile(updatedProfile)
                }

                onSuccess()
            } catch (e: Exception) {
                _uiState.value = ProgressUiState.Error(e.message ?: "Failed to log weight")
            }
        }
    }

    private fun calculateCalendarDays(filter: DateRangeFilter): List<Calendar> {
        return when (filter) {
            is DateRangeFilter.Last1Day -> {
                listOf(Calendar.getInstance())
            }
            is DateRangeFilter.Last3Days -> {
                (2 downTo 0).map { offset ->
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                }
            }
            is DateRangeFilter.Last7Days -> {
                (6 downTo 0).map { offset ->
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                }
            }
            is DateRangeFilter.Last30Days -> {
                (29 downTo 0).map { offset ->
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                }
            }
            is DateRangeFilter.Custom -> {
                val startCal = Calendar.getInstance().apply { timeInMillis = filter.startDateMillis }
                val endCal = Calendar.getInstance().apply { timeInMillis = filter.endDateMillis }

                if (startCal.after(endCal)) {
                    listOf(Calendar.getInstance())
                } else {
                    val list = mutableListOf<Calendar>()
                    val cur = startCal.clone() as Calendar
                    while (!cur.after(endCal)) {
                        list.add(cur.clone() as Calendar)
                        cur.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    list.ifEmpty { listOf(Calendar.getInstance()) }
                }
            }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            try {
                // Ensure today's log exists
                dailyLogRepository.getOrCreateDailyLog(currentDate)

                _selectedFilter.flatMapLatest { filter ->
                    val dayCals = calculateCalendarDays(filter)
                    val startDate = dateFormat.format(dayCals.first().time)
                    val endDate = dateFormat.format(dayCals.last().time)

                    val startDisplay = dateDisplayFormat.format(dayCals.first().time)
                    val endDisplay = dateDisplayFormat.format(dayCals.last().time)
                    val periodRangeLabel = if (dayCals.size == 1) {
                        startDisplay
                    } else {
                        "$startDisplay - $endDisplay"
                    }

                    combine(
                        profileRepository.getProfile(),
                        dailyLogRepository.getDailyLogsBetweenDates(startDate, endDate),
                        weightLogRepository.getAllWeightLogs()
                    ) { profile, rangeLogs, weightLogs ->
                        profile to Triple(rangeLogs, weightLogs, dayCals to periodRangeLabel)
                    }.flatMapLatest { (profile, info) ->
                        val (rangeLogs, weightLogs, calendarInfo) = info
                        val (calendars, rangeLabel) = calendarInfo
                        val dayFlows = calendars.map { cal ->
                            val dateStr = dateFormat.format(cal.time)
                            val log = rangeLogs.find { it.date == dateStr }
                            if (log != null) {
                                combine(
                                    intakeRepository.getIntakesForDailyLog(log.id),
                                    waterRepository.getWaterRecordsForDailyLog(log.id),
                                    exerciseRepository.getExercisesForDailyLog(log.id)
                                ) { intakes, waters, exercises ->
                                    DayRecord(dateStr, cal, log, intakes, waters, exercises)
                                }
                            } else {
                                flowOf(DayRecord(dateStr, cal, null, emptyList(), emptyList(), emptyList()))
                            }
                        }

                        combine(dayFlows) { dayRecords ->
                            buildProgressData(profile, dayRecords.toList(), weightLogs, rangeLabel, filter)
                        }
                    }
                }.collect { progressData ->
                    _uiState.value = ProgressUiState.Success(progressData)
                }
            } catch (e: Exception) {
                _uiState.value = ProgressUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun buildProgressData(
        profile: ProfileEntity?,
        dayRecords: List<DayRecord>,
        weightLogs: List<WeightLogEntity>,
        periodRangeLabel: String,
        filter: DateRangeFilter
    ): ProgressData {
        val totalDays = dayRecords.size
        val goalData = buildGoalData(profile, weightLogs, dayRecords)
        val trendPoints = mutableListOf<DailyTrendPoint>()
        val reviews = mutableListOf<DailyReviewData>()

        var totalCalSum = 0
        var activeCalDays = 0
        var totalWaterSum = 0
        var missionCompletedCount = 0

        val targetCal = profile?.dailyCalories ?: NutritionSummary.DEFAULT_GOAL_CALORIES
        val targetProtein = profile?.dailyProtein?.toDouble() ?: NutritionSummary.DEFAULT_GOAL_PROTEIN.toDouble()
        val targetExercise = profile?.dailyExerciseMinutes ?: 30
        val targetWater = com.fdev.nutrimindai.core.util.NutritionCalculator.calculateDynamicWaterTarget(
            weightKg = profile?.currentWeight ?: 70f,
            exerciseMinutes = targetExercise
        )

        val latestActualLog = weightLogs.maxByOrNull { it.date }
        val anchorDate = latestActualLog?.date ?: ""
        val anchorWeight = latestActualLog?.weightKg ?: profile?.currentWeight ?: 70.0f
        val userTdee = if (profile != null) {
            NutritionCalculator.calculateTdee(
                profile.age,
                profile.gender,
                profile.height,
                goalData.lastActualWeightKg,
                profile.activityLevel
            )
        } else 2200.0

        val shortDatePattern = SimpleDateFormat("dd/MM", Locale.getDefault())

        for (i in dayRecords.indices) {
            val record = dayRecords[i]
            val dayLabel = if (totalDays <= 7) {
                dayLabelFormat.format(record.calendar.time)
            } else {
                shortDatePattern.format(record.calendar.time)
            }
            val dateDisplay = dateDisplayFormat.format(record.calendar.time)

            val cals = record.intakes.sumOf { it.calories }
            val protein = record.intakes.sumOf { it.protein }
            val water = record.waters.sumOf { it.amountMl }
            val exerciseMinutes = record.exercises.sumOf { it.durationMinute }
            val exerciseCalories = record.exercises.sumOf { it.caloriesBurned }

            if (cals > 0) {
                totalCalSum += cals
                activeCalDays++
            }
            totalWaterSum += water

            val loggedWeightForDay = weightLogs.find { it.date == record.dateStr }?.weightKg
            val isActual = loggedWeightForDay != null
            val actualWeight = if (isActual) {
                loggedWeightForDay!!
            } else {
                if (anchorDate.isNotBlank() && record.dateStr > anchorDate) {
                    var netUpToDay = 0.0
                    for (prev in dayRecords) {
                        if (prev.dateStr > anchorDate && prev.dateStr <= record.dateStr) {
                            val c = prev.intakes.sumOf { it.calories }
                            val ex = prev.exercises.sumOf { it.caloriesBurned }
                            if (c > 0) {
                                netUpToDay += (c - (userTdee + ex))
                            }
                        }
                    }
                    NutritionCalculator.calculateProjectedWeight(anchorWeight, netUpToDay)
                } else {
                    weightLogs.filter { it.date <= record.dateStr }.lastOrNull()?.weightKg
                        ?: anchorWeight
                }
            }

            trendPoints.add(
                DailyTrendPoint(
                    dayLabel = dayLabel,
                    dateStr = record.dateStr,
                    calories = cals.toFloat(),
                    protein = protein.toFloat(),
                    water = water.toFloat(),
                    weight = actualWeight,
                    exercise = exerciseCalories.toFloat(),
                    isActualWeight = isActual
                )
            )

            val score = NutritionCalculator.calculateHealthScore(
                cals = cals,
                targetCal = targetCal,
                protein = protein,
                targetProtein = targetProtein,
                water = water,
                targetWater = targetWater,
                exerciseMin = exerciseMinutes,
                targetExerciseMin = targetExercise
            )

            val status = when {
                score >= 90 -> "Excellent"
                score >= 75 -> "Good"
                score >= 60 -> "Fair"
                else -> "Needs Attention"
            }
            val statusColor = if (score >= 80) LightSuccess else NutritionFat

            val isToday = record.dateStr == currentDate
            val dayTitle = when {
                isToday -> "Today"
                i == dayRecords.size - 2 && dayRecords.size > 1 -> "Yesterday"
                else -> "$dayLabel, $dateDisplay"
            }

            if (cals > 0 || water > 0 || exerciseMinutes > 0) {
                missionCompletedCount++
            }

            reviews.add(
                0,
                DailyReviewData(
                    day = dayTitle,
                    date = record.dateStr,
                    score = score,
                    status = status,
                    color = statusColor
                )
            )
        }

        val completedDays = dayRecords.count { it.dailyLog?.status == DailyLogStatus.COMPLETED }
        val avgCalories = if (activeCalDays > 0) totalCalSum / activeCalDays else 0
        val waterGoalPercent = ((totalWaterSum.toFloat() / (targetWater * totalDays)) * 100).toInt().coerceAtMost(100)
        val missionPercent = ((missionCompletedCount.toFloat() / totalDays) * 100).toInt()

        val aiInsight = generateAiInsight(avgCalories, targetCal, waterGoalPercent, completedDays, totalDays)

        val todayPoint = trendPoints.lastOrNull()
        val todayNutrition = NutritionSummary(
            calories = todayPoint?.calories?.toInt() ?: 0,
            protein = todayPoint?.protein?.toDouble() ?: 0.0,
            water = todayPoint?.water?.toInt() ?: 0,
            exerciseCalories = (todayPoint?.exercise ?: 0f).toInt(),
            goalCalories = targetCal,
            goalProtein = targetProtein.toInt(),
            goalWater = targetWater
        )

        val userName = profile?.name?.ifBlank { "Champion" } ?: "Champion"

        return ProgressData(
            goalData = goalData,
            todayNutrition = todayNutrition,
            completedDays = completedDays,
            totalDays = totalDays,
            avgCalories = avgCalories,
            waterGoalPercent = waterGoalPercent,
            missionPercent = missionPercent,
            weekRange = periodRangeLabel,
            weeklyTrendPoints = trendPoints,
            dailyReviews = reviews,
            aiInsight = aiInsight,
            selectedFilter = filter,
            userName = userName
        )
    }

    private fun buildGoalData(
        profile: ProfileEntity?,
        weightLogs: List<WeightLogEntity>,
        dayRecords: List<DayRecord>
    ): ProgressGoalData {
        if (profile == null) return ProgressGoalData()

        val goalTitle = when (profile.goal) {
            Goal.LoseWeight -> "Weight Loss Goal"
            Goal.MaintainWeight -> "Weight Maintenance"
            Goal.GainWeight -> "Weight Gain Goal"
        }

        val startWeight = weightLogs.minByOrNull { it.date }?.weightKg ?: profile.currentWeight
        val targetWeight = profile.targetWeight

        val latestActualLog = weightLogs.maxByOrNull { it.date }
        val anchorWeight = latestActualLog?.weightKg ?: profile.currentWeight
        val anchorDate = latestActualLog?.date ?: ""
        val todayStr = currentDate

        val todayActualLog = weightLogs.find { it.date == todayStr }

        val userTdee = NutritionCalculator.calculateTdee(
            profile.age,
            profile.gender,
            profile.height,
            anchorWeight,
            profile.activityLevel
        )

        var cumulativeNetBalance = 0.0
        for (record in dayRecords) {
            if (record.dateStr > anchorDate && record.dateStr <= todayStr) {
                val cals = record.intakes.sumOf { it.calories }
                val exBurn = record.exercises.sumOf { it.caloriesBurned }
                if (cals > 0) {
                    cumulativeNetBalance += (cals - (userTdee + exBurn))
                }
            }
        }

        val isUsingProjection = todayActualLog == null && anchorDate.isNotBlank() && anchorDate < todayStr
        val currentWeight = if (isUsingProjection) {
            NutritionCalculator.calculateProjectedWeight(anchorWeight, cumulativeNetBalance)
        } else {
            todayActualLog?.weightKg ?: anchorWeight
        }

        val projectedDelta = if (isUsingProjection) {
            val rawDelta = currentWeight - anchorWeight
            (rawDelta * 10f).roundToInt() / 10f
        } else 0.0f

        var diffDays = 0
        val lastActualWeightDate = if (anchorDate.isBlank()) {
            null
        } else {
            try {
                val anchorTime = dateFormat.parse(anchorDate)?.time ?: 0L
                val todayTime = dateFormat.parse(todayStr)?.time ?: 0L
                diffDays = ((todayTime - anchorTime) / (1000 * 60 * 60 * 24)).toInt()
                when (diffDays) {
                    0 -> "Today"
                    1 -> "Yesterday"
                    in 2..30 -> "$diffDays days ago"
                    else -> dateDisplayFormat.format(dateFormat.parse(anchorDate) ?: Date())
                }
            } catch (e: Exception) {
                anchorDate
            }
        }

        val needsCalibrationPrompt = isUsingProjection && diffDays >= 7

        val metabolicNotice = if (isUsingProjection) {
            if (projectedDelta < 0) {
                "Deficit projection: ${projectedDelta} kg"
            } else if (projectedDelta > 0) {
                "Surplus projection: +${projectedDelta} kg"
            } else {
                "On track with baseline"
            }
        } else null

        val progressPercent = when (profile.goal) {
            Goal.LoseWeight -> {
                val totalLossNeeded = startWeight - targetWeight
                if (totalLossNeeded > 0f) {
                    val actualLoss = startWeight - currentWeight
                    (actualLoss / totalLossNeeded).coerceIn(0.0f, 1.0f)
                } else {
                    1.0f
                }
            }
            Goal.GainWeight -> {
                val totalGainNeeded = targetWeight - startWeight
                if (totalGainNeeded > 0f) {
                    val actualGain = currentWeight - startWeight
                    (actualGain / totalGainNeeded).coerceIn(0.0f, 1.0f)
                } else {
                    1.0f
                }
            }
            Goal.MaintainWeight -> {
                val diff = abs(currentWeight - targetWeight)
                if (diff <= 0.5f) 1.0f else (1.0f - (diff / 5.0f)).coerceIn(0.0f, 1.0f)
            }
        }

        val remainingKg = abs(currentWeight - targetWeight)
        val remainingDays = if (profile.goal == Goal.MaintainWeight || remainingKg <= 0.2f) {
            0
        } else {
            ((remainingKg / 0.5f) * 7).toInt().coerceIn(1, 180)
        }

        return ProgressGoalData(
            goalTitle = goalTitle,
            progressPercent = progressPercent,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            remainingDays = remainingDays,
            isUsingProjection = isUsingProjection,
            lastActualWeightKg = anchorWeight,
            lastActualWeightDate = lastActualWeightDate,
            projectedDeltaKg = projectedDelta,
            metabolicNotice = metabolicNotice,
            needsCalibrationPrompt = needsCalibrationPrompt
        )
    }

    private fun generateAiInsight(
        avgCalories: Int,
        targetCalories: Int,
        waterPercent: Int,
        completedDays: Int,
        totalDays: Int
    ): String {
        val completionRatio = completedDays.toFloat() / totalDays
        return when {
            completionRatio >= 0.7f && waterPercent >= 80 ->
                "Outstanding consistency! You've logged $completedDays of $totalDays days in this period and met 80%+ of your hydration goal. Keep up this momentum!"
            avgCalories in (targetCalories - 200)..(targetCalories + 200) ->
                "Great calorie adherence! Your average daily intake ($avgCalories kcal) is right on target with your goal of $targetCalories kcal."
            waterPercent < 60 ->
                "You're doing great on tracking meals! Try to increase your daily water intake to boost metabolism and reach your hydration target."
            else ->
                "Consistent tracking is key! You are making steady progress towards your health targets."
        }
    }
}

