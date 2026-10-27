package com.fdev.nutrimindai.feature.activity.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
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
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val parseDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadActivity()
    }

    private fun loadActivity() {
        viewModelScope.launch {
            try {
                combine(
                    dailyLogRepository.getAllDailyLogs(),
                    _searchQuery,
                    _selectedDate
                ) { dailyLogs, query, filterDate ->
                    if (dailyLogs.isEmpty()) {
                        return@combine ActivityUiState.Empty
                    }

                    // 1. Filter by Date if selected
                    val dateFilteredLogs = if (!filterDate.isNullOrBlank()) {
                        dailyLogs.filter { it.date == filterDate }
                    } else {
                        dailyLogs
                    }

                    // 2. Build summaries
                    val summaries = dateFilteredLogs.map { log -> buildDaySummary(log) }

                    // 3. Filter by Search Query if entered
                    val finalSummaries = if (query.isNotBlank()) {
                        summaries.filter { summary ->
                            summary.displayDate.contains(query, ignoreCase = true) ||
                            summary.fullDate.contains(query, ignoreCase = true) ||
                            summary.status.contains(query, ignoreCase = true) ||
                            summary.dailyLog.date.contains(query, ignoreCase = true) ||
                            summary.keywords.any { it.contains(query, ignoreCase = true) }
                        }
                    } else {
                        summaries
                    }

                    if (finalSummaries.isEmpty()) {
                        ActivityUiState.Empty
                    } else {
                        ActivityUiState.Success(
                            days = finalSummaries,
                            isFiltered = !filterDate.isNullOrBlank() || query.isNotBlank()
                        )
                    }
                }.collect { state ->
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

    private suspend fun buildDaySummary(dailyLog: DailyLog): ActivityDaySummary {
        val intakes = intakeRepository.getIntakesForDailyLogDirect(dailyLog.id)
        val waters = waterRepository.getWaterRecordsForDailyLogDirect(dailyLog.id)
        val exercises = exerciseRepository.getExercisesForDailyLogDirect(dailyLog.id)

        val isToday = dailyLog.date == currentDate
        val displayDate = if (isToday) "Today" else formatDisplayDate(dailyLog.date)

        val parsedDate = runCatching { parseDateFormat.parse(dailyLog.date) }.getOrNull()
        val fullDate = if (parsedDate != null) displayDateFormat.format(parsedDate) else dailyLog.date

        val statusText = when (dailyLog.status) {
            DailyLogStatus.DRAFT -> "Draft"
            DailyLogStatus.COMPLETED -> "Completed"
            DailyLogStatus.MISSED -> "Missed"
        }

        val keywords = mutableListOf<String>()
        keywords.addAll(intakes.map { it.foodName })
        keywords.addAll(exercises.map { it.exerciseType })

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

    private fun formatDisplayDate(dateString: String): String {
        val parsedDate = runCatching { parseDateFormat.parse(dateString) }.getOrNull()
        return if (parsedDate != null) {
            val dayFormat = SimpleDateFormat("EEE, MMMM dd", Locale.getDefault())
            dayFormat.format(parsedDate)
        } else {
            dateString
        }
    }
}

