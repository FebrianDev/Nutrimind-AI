package com.fdev.nutrimindai.feature.activity.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class ActivityDaySummary(
    val dailyLog: DailyLog,
    val displayDate: String,
    val fullDate: String,
    val status: String,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalWaterMl: Int,
    val exerciseCalories: Int,
    val entriesCount: Int,
    val isEditable: Boolean,
    val keywords: List<String> = emptyList()
)

sealed class ActivityUiState {
    data object Loading : ActivityUiState()
    data class Success(
        val days: List<ActivityDaySummary>,
        val isFiltered: Boolean = false
    ) : ActivityUiState()
    data object Empty : ActivityUiState()
    data class Error(val message: String) : ActivityUiState()
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityUiState>(ActivityUiState.Loading)
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val currentDate: String
        get() = LocalDate.now().toString()
    private val displayDateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault())
    private val dayHeaderFormatter = DateTimeFormatter.ofPattern("EEE, MMMM dd", Locale.getDefault())

    init {
        loadActivity()
    }

    @OptIn(FlowPreview::class)
    private fun loadActivity() {
        viewModelScope.launch {
            try {
                // 1. Precompute all day summaries reactively in O(N) using single batch table flows (no N+1 queries)
                val allSummariesFlow = combine(
                    dailyLogRepository.getAllDailyLogs(),
                    intakeRepository.getAllIntakes(),
                    waterRepository.getAllWaterRecords(),
                    exerciseRepository.getAllExercises()
                ) { dailyLogs, allIntakes, allWaters, allExercises ->
                    if (dailyLogs.isEmpty()) {
                        emptyList()
                    } else {
                        val intakesByLogId = allIntakes.groupBy { it.dailyLogId }
                        val watersByLogId = allWaters.groupBy { it.dailyLogId }
                        val exercisesByLogId = allExercises.groupBy { it.dailyLogId }
                        val todayStr = currentDate

                        dailyLogs.map { log ->
                            buildDaySummary(
                                dailyLog = log,
                                intakes = intakesByLogId[log.id].orEmpty(),
                                waters = watersByLogId[log.id].orEmpty(),
                                exercises = exercisesByLogId[log.id].orEmpty(),
                                todayStr = todayStr
                            )
                        }
                    }
                }.flowOn(Dispatchers.Default)

                // 2. Apply date & debounced search filters in memory without triggering any SQLite queries
                combine(
                    allSummariesFlow,
                    _searchQuery.debounce(250L),
                    _selectedDate
                ) { summaries, query, filterDate ->
                    if (summaries.isEmpty()) {
                        return@combine ActivityUiState.Empty
                    }

                    val dateFiltered = if (!filterDate.isNullOrBlank()) {
                        summaries.filter { it.dailyLog.date == filterDate }
                    } else {
                        summaries
                    }

                    val trimmedQuery = query.trim()
                    val finalSummaries = if (trimmedQuery.isNotBlank()) {
                        dateFiltered.filter { summary ->
                            summary.displayDate.contains(trimmedQuery, ignoreCase = true) ||
                                summary.fullDate.contains(trimmedQuery, ignoreCase = true) ||
                                summary.status.contains(trimmedQuery, ignoreCase = true) ||
                                summary.dailyLog.date.contains(trimmedQuery, ignoreCase = true) ||
                                summary.keywords.any { it.contains(trimmedQuery, ignoreCase = true) }
                        }
                    } else {
                        dateFiltered
                    }

                    if (finalSummaries.isEmpty()) {
                        ActivityUiState.Empty
                    } else {
                        ActivityUiState.Success(
                            days = finalSummaries,
                            isFiltered = !filterDate.isNullOrBlank() || trimmedQuery.isNotBlank()
                        )
                    }
                }.flowOn(Dispatchers.Default).collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = ActivityUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDateSelected(date: String?) {
        // Toggle selection if same date clicked again
        if (_selectedDate.value == date) {
            _selectedDate.value = null
        } else {
            _selectedDate.value = date
        }
    }

    fun clearDateFilter() {
        _selectedDate.value = null
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun clearAllFilters() {
        _selectedDate.value = null
        _searchQuery.value = ""
    }

    private fun buildDaySummary(
        dailyLog: DailyLog,
        intakes: List<Intake>,
        waters: List<Water>,
        exercises: List<Exercise>,
        todayStr: String
    ): ActivityDaySummary {
        val isToday = dailyLog.date == todayStr
        val parsedDate = runCatching { LocalDate.parse(dailyLog.date) }.getOrNull()

        val displayDate = when {
            isToday -> "Today"
            parsedDate != null -> parsedDate.format(dayHeaderFormatter)
            else -> dailyLog.date
        }
        val fullDate = parsedDate?.format(displayDateFormatter) ?: dailyLog.date

        val statusText = when (dailyLog.status) {
            DailyLogStatus.DRAFT -> "Draft"
            DailyLogStatus.COMPLETED -> "Completed"
            DailyLogStatus.MISSED -> "Missed"
        }

        val keywords = ArrayList<String>(intakes.size + exercises.size)
        intakes.forEach { keywords.add(it.foodName) }
        exercises.forEach { keywords.add(it.exerciseType) }

        return ActivityDaySummary(
            dailyLog = dailyLog,
            displayDate = displayDate,
            fullDate = fullDate,
            status = statusText,
            totalCalories = intakes.sumOf { it.calories },
            totalProtein = intakes.sumOf { it.protein },
            totalWaterMl = waters.sumOf { it.amountMl },
            exerciseCalories = exercises.sumOf { it.caloriesBurned },
            entriesCount = intakes.size + waters.size + exercises.size,
            isEditable = isToday && dailyLog.status == DailyLogStatus.DRAFT,
            keywords = keywords
        )
    }
}

