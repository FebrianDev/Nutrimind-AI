package com.fdev.nutrimindai.feature.activity.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var weekOffset by remember { mutableIntStateOf(0) }

    // Reset weekOffset to 0 when date is cleared or changed externally
    LaunchedEffect(selectedDate) {
        weekOffset = 0
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calendar.timeInMillis = millis
                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH) + 1
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            val formattedDate = String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                year,
                                month,
                                day
                            )
                            viewModel.onDateSelected(formattedDate)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.food_dialog_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    headlineContentColor = MaterialTheme.colorScheme.primary,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    navigationContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityHeader(
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearchToggle = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) {
                    viewModel.clearSearchQuery()
                }
            },
            onCalendarClick = { showDatePicker = true },
            selectedDate = selectedDate
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Interactive Calendar Strip with auto-sync
            CalendarStrip(
                selectedDate = selectedDate,
                weekOffset = weekOffset,
                onDateSelected = viewModel::onDateSelected,
                onWeekOffsetChange = { weekOffset += it },
                onMonthHeaderClick = { showDatePicker = true }
            )

            // Active Filter Chips Bar
            AnimatedVisibility(
                visible = selectedDate != null || searchQuery.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectedDate?.let { date ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.clearDateFilter() },
                            label = {
                                Text(
                                    stringResource(R.string.activity_date_chip, date),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.activity_clear_date_filter_desc),
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = LightSuccess.copy(alpha = 0.15f),
                                selectedLabelColor = LightSuccess
                            )
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        InputChip(
                            selected = true,
                            onClick = { viewModel.clearSearchQuery() },
                            label = {
                                Text(
                                    stringResource(R.string.activity_search_chip, searchQuery),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.activity_clear_search_desc),
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = NutritionCalories.copy(alpha = 0.15f),
                                selectedLabelColor = NutritionCalories
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = { viewModel.clearAllFilters() }) {
                        Text(
                            stringResource(R.string.activity_clear_all),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            when (val state = uiState) {
                is ActivityUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ActivityUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is ActivityUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(AppSpacing.xl)
                        ) {
                            Icon(
                                imageVector = if (selectedDate != null || searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.AutoMirrored.Filled.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedDate != null || searchQuery.isNotBlank()) {
                                    stringResource(R.string.activity_empty_filtered_title)
                                } else {
                                    stringResource(R.string.activity_empty_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedDate != null || searchQuery.isNotBlank()) {
                                    stringResource(R.string.activity_empty_filtered_desc)
                                } else {
                                    stringResource(R.string.activity_empty_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedDate != null || searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.clearAllFilters() },
                                    colors = ButtonDefaults.buttonColors(containerColor = LightSuccess)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.activity_reset_filters))
                                }
                            }
                        }
                    }
                }

                is ActivityUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.days) { daySummary ->
                            ActivitySummaryCard(
                                date = daySummary.displayDate,
                                fullDate = daySummary.fullDate,
                                status = daySummary.status,
                                calories = String.format(
                                    Locale.US,
                                    "%,d",
                                    daySummary.totalCalories
                                ),
                                protein = stringResource(R.string.macro_val_g, daySummary.totalProtein.toInt()),
                                water = "${
                                    String.format(
                                        Locale.US,
                                        "%.1f",
                                        daySummary.totalWaterMl / 1000.0
                                    )
                                }" + stringResource(R.string.unit_liters),
                                exercise = daySummary.exerciseCalories.toString(),
                                entriesCount = daySummary.entriesCount,
                                isEditable = daySummary.isEditable,
                                onClick = { onNavigateToDetail(daySummary.dailyLog.date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityHeader(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onCalendarClick: () -> Unit,
    selectedDate: String?
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "HeaderTransition"
        ) { searchActive ->
            if (searchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSearchToggle) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.activity_close_search_desc)
                        )
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.activity_search_placeholder), fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = LightSuccess
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.activity_clear_text_desc))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightSuccess,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .screenPadding()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.activity_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.activity_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSearchToggle) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.activity_toggle_search_desc),
                                tint = if (searchQuery.isNotBlank()) NutritionCalories else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = onCalendarClick) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.activity_calendar_picker_desc),
                                tint = if (selectedDate != null) LightSuccess else LocalContentColor.current
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarStrip(
    selectedDate: String?,
    weekOffset: Int,
    onDateSelected: (String) -> Unit,
    onWeekOffsetChange: (Int) -> Unit,
    onMonthHeaderClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val startMonthFormat = remember { SimpleDateFormat("MMM", Locale.US) }
    val fullMonthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }
    val shortYearFormat = remember { SimpleDateFormat("MMM yyyy", Locale.US) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.US) }
    val todayDateStr = remember { dateFormat.format(Date()) }

    // Anchor calendar around selectedDate if set, or today's date
    val baseCalendar = remember(selectedDate, weekOffset) {
        Calendar.getInstance().apply {
            if (selectedDate != null) {
                runCatching {
                    val parsed = dateFormat.parse(selectedDate)
                    if (parsed != null) time = parsed
                }
            }
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
    }

    // Format start and end of week for accurate month/year header span
    val monthYearText = remember(baseCalendar) {
        val startCal = baseCalendar.clone() as Calendar
        val endCal = (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_WEEK, 6) }

        if (startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
            startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
        ) {
            fullMonthYearFormat.format(startCal.time)
        } else if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)) {
            "${startMonthFormat.format(startCal.time)} - ${fullMonthYearFormat.format(endCal.time)}"
        } else {
            "${shortYearFormat.format(startCal.time)} - ${shortYearFormat.format(endCal.time)}"
        }
    }

    val days = remember(baseCalendar, selectedDate) {
        val calIter = baseCalendar.clone() as Calendar
        (0 until 7).map {
            val dateStr = dateFormat.format(calIter.time)
            val dayName = dayNameFormat.format(calIter.time).uppercase()
            val dayNum = calIter.get(Calendar.DAY_OF_MONTH).toString()
            val isToday = (dateStr == todayDateStr)
            val isSelected = (selectedDate == dateStr)
            calIter.add(Calendar.DAY_OF_WEEK, 1)
            CalendarDayItem(dayName, dayNum, dateStr, isToday, isSelected)
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .screenPadding(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onWeekOffsetChange(-1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.activity_prev_week_desc),
                    modifier = Modifier.size(20.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onMonthHeaderClick() }
                    .badgePadding()
            ) {
                Text(
                    text = monthYearText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.activity_open_date_picker_desc))
            }
            IconButton(onClick = { onWeekOffsetChange(1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.activity_next_week_desc),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items(days, key = { it.dateStr }) { dayItem ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onDateSelected(dayItem.dateStr) }
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = dayItem.dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dayItem.isSelected) LightSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (dayItem.isSelected || dayItem.isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (dayItem.isSelected) LightSuccess else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayItem.dayNum,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (dayItem.isSelected || dayItem.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (dayItem.isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Small dot indicator for Today when not selected
                    if (dayItem.isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    if (dayItem.isSelected) LightSuccess else LightSuccess,
                                    CircleShape
                                )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private data class CalendarDayItem(
    val dayName: String,
    val dayNum: String,
    val dateStr: String,
    val isToday: Boolean,
    val isSelected: Boolean
)

@Composable
fun ActivitySummaryCard(
    date: String,
    fullDate: String,
    status: String,
    calories: String,
    protein: String,
    water: String,
    exercise: String,
    entriesCount: Int,
    isEditable: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Column(modifier = Modifier.cardPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = fullDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when (status) {
                        "Draft" -> NutritionCalories.copy(alpha = 0.1f)
                        "Completed" -> LightSuccess.copy(alpha = 0.1f)
                        else -> LightError.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.badgePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusLabel = when (status) {
                            "Draft" -> stringResource(R.string.status_draft)
                            "Completed" -> stringResource(R.string.status_completed)
                            else -> stringResource(R.string.status_missed)
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                "Draft" -> NutritionCalories
                                "Completed" -> LightSuccess
                                else -> LightError
                            }
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when (status) {
                                "Draft" -> NutritionCalories
                                "Completed" -> LightSuccess
                                else -> LightError
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActivityStatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    color = NutritionCalories,
                    value = calories,
                    label = stringResource(R.string.unit_kcal)
                )
                ActivityStatItem(
                    icon = Icons.Default.Egg,
                    color = NutritionProtein,
                    value = protein,
                    label = stringResource(R.string.label_protein)
                )
                ActivityStatItem(
                    icon = Icons.Default.WaterDrop,
                    color = NutritionWater,
                    value = water,
                    label = stringResource(R.string.label_water)
                )
                ActivityStatItem(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    color = NutritionExercise,
                    value = exercise,
                    label = stringResource(R.string.label_kcal_burned)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_entries_logged, entriesCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditable) Icons.Default.Edit else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isEditable) LightSuccess else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(if (isEditable) R.string.label_can_still_edit else R.string.label_read_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEditable) LightSuccess else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityStatItem(icon: ImageVector, color: Color, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 8.sp
        )
    }
}

