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
    val exercise: Float
)

data class ProgressGoalData(
    val goalTitle: String = "Weight Loss Goal",
    val progressPercent: Float = 0.6f,
    val currentWeightKg: Float = 72.0f,
    val targetWeightKg: Float = 68.0f,
    val remainingDays: Int = 30
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
    val selectedFilter: DateRangeFilter = DateRangeFilter.Last7Days
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
        val latestWeightLog = weightLogs.lastOrNull()
        val goalData = buildGoalData(profile, latestWeightLog)
        val trendPoints = mutableListOf<DailyTrendPoint>()
        val reviews = mutableListOf<DailyReviewData>()

        var totalCalSum = 0
        var activeCalDays = 0
        var totalWaterSum = 0
        var missionCompletedCount = 0

        val targetCal = profile?.dailyCalories ?: NutritionSummary.DEFAULT_GOAL_CALORIES
        val targetProtein = profile?.dailyProtein?.toDouble() ?: NutritionSummary.DEFAULT_GOAL_PROTEIN.toDouble()
        val targetWater = NutritionSummary.DEFAULT_GOAL_WATER
        val targetExercise = profile?.dailyExerciseMinutes ?: 30

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
            val actualWeight = loggedWeightForDay
                ?: latestWeightLog?.weightKg
                ?: profile?.currentWeight
                ?: 72.0f

            trendPoints.add(
                DailyTrendPoint(
                    dayLabel = dayLabel,
                    dateStr = record.dateStr,
                    calories = cals.toFloat(),
                    protein = protein.toFloat(),
                    water = water.toFloat(),
                    weight = actualWeight,
                    exercise = exerciseCalories.toFloat()
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
            selectedFilter = filter
        )
    }

    private fun buildGoalData(profile: ProfileEntity?, latestWeightLog: WeightLogEntity?): ProgressGoalData {
        if (profile == null) return ProgressGoalData()

        val goalTitle = when (profile.goal) {
            Goal.LoseWeight -> "Weight Loss Goal"
            Goal.MaintainWeight -> "Weight Maintenance"
            Goal.GainWeight -> "Weight Gain Goal"
        }

        val startWeight = profile.currentWeight
        val currentWeight = latestWeightLog?.weightKg ?: profile.currentWeight
        val targetWeight = profile.targetWeight

        val totalDiff = abs(startWeight - targetWeight)
        val achievedDiff = abs(startWeight - currentWeight)
        val progressPercent = if (totalDiff > 0f) (achievedDiff / totalDiff).coerceIn(0.05f, 1.0f) else 1.0f

        return ProgressGoalData(
            goalTitle = goalTitle,
            progressPercent = progressPercent,
            currentWeightKg = currentWeight,
            targetWeightKg = targetWeight,
            remainingDays = 30
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

