package com.fdev.nutrimindai.feature.progress.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.LightSuccess
import com.fdev.nutrimindai.core.designsystem.NutritionCalories
import com.fdev.nutrimindai.core.designsystem.NutritionExercise
import com.fdev.nutrimindai.core.designsystem.NutritionProtein
import com.fdev.nutrimindai.core.designsystem.NutritionWater
import com.fdev.nutrimindai.core.util.AdaptiveCalibrationResult
import com.fdev.nutrimindai.core.util.NutritionPdfReportGenerator
import com.fdev.nutrimindai.feature.progress.presentation.components.ProgressTrendChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
    onNavigateToDailyReview: (String) -> Unit = {},
    onNavigateToTrendDetail: (String) -> Unit,
    onNavigateToGoalSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExportingPdf by remember { mutableStateOf(false) }
    var showDateFilterSheet by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showLogWeightDialog by remember { mutableStateOf(false) }

    val currentFilter =
        (uiState as? ProgressUiState.Success)?.data?.selectedFilter ?: DateRangeFilter.Last7Days
    val filterLabel = currentFilter.getLabel()

    Column(modifier = Modifier.fillMaxSize()) {
        ProgressHeader(
            selectedFilterLabel = filterLabel,
            onCalendarClick = { showDateFilterSheet = true },
            isExportingPdf = isExportingPdf,
            onExportPdfClick = {
                val successData = (uiState as? ProgressUiState.Success)?.data
                if (successData != null) {
                    coroutineScope.launch {
                        isExportingPdf = true
                        val file = withContext(Dispatchers.IO) {
                            NutritionPdfReportGenerator.generateReport(
                                context = context,
                                data = successData,
                                userName = successData.userName
                            )
                        }
                        isExportingPdf = false
                        if (file != null) {
                            NutritionPdfReportGenerator.sharePdf(context, file)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.export_pdf_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )

        when (val state = uiState) {
            is ProgressUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is ProgressUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is ProgressUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.progress_empty_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is ProgressUiState.Success -> {
                ProgressContent(
                    data = state.data,
                    onNavigateToDailyReview = onNavigateToDailyReview,
                    onNavigateToTrendDetail = onNavigateToTrendDetail,
                    onNavigateToGoalSettings = onNavigateToGoalSettings,
                    onLogWeightClick = { showLogWeightDialog = true }
                )
            }
        }
    }

    if (showLogWeightDialog && uiState is ProgressUiState.Success) {
        val successData = (uiState as ProgressUiState.Success).data
        LogWeightDialog(
            initialWeight = successData.goalData.currentWeightKg,
            onDismiss = { showLogWeightDialog = false },
            onConfirm = { weight, note ->
                viewModel.logWeight(weight, note)
                showLogWeightDialog = false
            }
        )
    }

    if (showDateFilterSheet) {
        DateFilterBottomSheet(
            currentFilter = currentFilter,
            onDismissRequest = { showDateFilterSheet = false },
            onFilterSelected = { filter -> viewModel.selectDateRangeFilter(filter) },
            onCustomClick = { showCustomDatePicker = true }
        )
    }

    if (showCustomDatePicker) {
        CustomDateRangePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            onDateRangeSelected = { startMillis, endMillis ->
                viewModel.selectDateRangeFilter(DateRangeFilter.Custom(startMillis, endMillis))
                showCustomDatePicker = false
            }
        )
    }

    val calibrationFeedback by viewModel.calibrationFeedback.collectAsStateWithLifecycle()
    calibrationFeedback?.let { feedback ->
        CalibrationResultDialog(
            result = feedback,
            onDismiss = { viewModel.dismissCalibrationFeedback() }
        )
    }
}

@Composable
private fun ProgressContent(
    data: ProgressData,
    onNavigateToDailyReview: (String) -> Unit = {},
    onNavigateToTrendDetail: (String) -> Unit,
    onNavigateToGoalSettings: () -> Unit,
    onLogWeightClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        GoalProgressCard(
            goalData = data.goalData,
            onEditGoalClick = onNavigateToGoalSettings,
            onLogWeightClick = onLogWeightClick
        )
        if (data.goalData.needsCalibrationPrompt) {
            WeeklyCalibrationBannerCard(
                onCalibrateClick = onLogWeightClick
            )
        }
        WeeklySummarySection(data = data)
        TrendSection(
            trendPoints = data.weeklyTrendPoints,
            onDetailClick = onNavigateToTrendDetail
        )

        AIInsightCard(insightText = data.aiInsight)
    }
}

@Composable
fun ProgressHeader(
    selectedFilterLabel: String,
    onCalendarClick: () -> Unit,
    isExportingPdf: Boolean = false,
    onExportPdfClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.progress_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.progress_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = onExportPdfClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                enabled = !isExportingPdf
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isExportingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = stringResource(R.string.cd_export_pdf),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.btn_export_pdf),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onCalendarClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.progress_cd_calendar_filter),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = selectedFilterLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun GoalProgressCard(
    goalData: ProgressGoalData,
    onEditGoalClick: () -> Unit,
    onLogWeightClick: () -> Unit
) {
    val progressPercentInt = (goalData.progressPercent * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_goal_progress),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onEditGoalClick) {
                    Text(text = stringResource(R.string.btn_edit_goal))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goalData.goalTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$progressPercentInt%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.progress_on_track_msg),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            LinearProgressIndicator(
                progress = { goalData.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f kg", goalData.currentWeightKg),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (goalData.isUsingProjection) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_ai_projected),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = LightSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LightSuccess.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LightSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_scale_verified),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LightSuccess,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    if (goalData.isUsingProjection && goalData.lastActualWeightDate != null) {
                        Text(
                            text = stringResource(
                                R.string.label_last_actual,
                                goalData.lastActualWeightKg,
                                goalData.lastActualWeightDate
                                    ?: stringResource(R.string.label_earlier)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (goalData.projectedDeltaKg != 0f) {
                            val prefix = if (goalData.projectedDeltaKg > 0) "+" else ""
                            val explanation = if (goalData.projectedDeltaKg < 0) {
                                stringResource(R.string.label_from_caloric_balance)
                            } else {
                                stringResource(R.string.label_from_caloric_surplus)
                            }
                            Text(
                                text = "$prefix${
                                    String.format(
                                        Locale.US,
                                        "%.1f",
                                        goalData.projectedDeltaKg
                                    )
                                } kg $explanation",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (goalData.projectedDeltaKg < 0) LightSuccess else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (!goalData.isUsingProjection) {
                        Text(
                            text = stringResource(R.string.label_scale_verified_today),
                            style = MaterialTheme.typography.bodySmall,
                            color = LightSuccess
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.label_current),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.1f kg", goalData.targetWeightKg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.label_goal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onLogWeightClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (goalData.isUsingProjection) {
                        stringResource(R.string.label_calibrate_scale)
                    } else {
                        stringResource(R.string.label_update_scale)
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.est_remaining_days, goalData.remainingDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklySummarySection(data: ProgressData) {
    val sectionTitle = when (data.selectedFilter) {
        is DateRangeFilter.Last1Day -> stringResource(R.string.progress_filter_today)
        is DateRangeFilter.Last3Days -> stringResource(R.string.progress_filter_last_3_days)
        is DateRangeFilter.Last7Days -> stringResource(R.string.label_this_week)
        is DateRangeFilter.Last30Days -> stringResource(R.string.progress_filter_last_30_days)
        is DateRangeFilter.Custom -> stringResource(R.string.progress_filter_period_summary)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = data.weekRange,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconColor = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.label_completed),
                    value = "${data.completedDays} / ${data.totalDays}",
                    description = stringResource(R.string.progress_unit_days)
                )
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = NutritionCalories,
                    label = stringResource(R.string.label_avg_calories),
                    value = String.format(Locale.US, "%,d", data.avgCalories),
                    description = stringResource(R.string.progress_unit_kcal_per_day)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WaterDrop,
                    iconColor = NutritionWater,
                    label = stringResource(R.string.label_water_goal),
                    value = "${data.waterGoalPercent}%",
                    description = ""
                )
                StatisticCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AdsClick,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.label_mission),
                    value = "${data.missionPercent}%",
                    description = ""
                )
            }
        }
    }
}

@Composable
fun StatisticCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    description: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        iconColor.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TrendSection(
    trendPoints: List<DailyTrendPoint>,
    onDetailClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf("Calories") }
    val metrics = listOf("Calories", "Protein", "Water", "Weight", "Exercise")

    val labels = trendPoints.map { it.dayLabel }
    val dataPoints = trendPoints.map { point ->
        when (selectedMetric) {
            "Calories" -> point.calories
            "Protein" -> point.protein
            "Water" -> point.water / 1000f // in liters
            "Weight" -> point.weight
            "Exercise" -> point.exercise
            else -> point.calories
        }
    }

    val selectedMetricLabel = when (selectedMetric) {
        "Calories" -> stringResource(R.string.home_label_calories)
        "Protein" -> stringResource(R.string.label_protein)
        "Water" -> stringResource(R.string.label_water)
        "Weight" -> stringResource(R.string.label_weight)
        "Exercise" -> stringResource(R.string.label_activity)
        else -> selectedMetric
    }

    val unit = when (selectedMetric) {
        "Calories" -> stringResource(R.string.unit_kcal)
        "Protein" -> stringResource(R.string.unit_grams)
        "Water" -> stringResource(R.string.unit_liters)
        "Weight" -> stringResource(R.string.unit_kg)
        "Exercise" -> stringResource(R.string.unit_minutes)
        else -> ""
    }

    val avgValue = if (dataPoints.isNotEmpty()) dataPoints.average().toFloat() else 0f
    val displayAvg = if (selectedMetric == "Water" || selectedMetric == "Weight") {
        String.format(Locale.US, "%.1f", avgValue)
    } else {
        String.format(Locale.US, "%,d", avgValue.toInt())
    }

    val lineColor = when (selectedMetric) {
        "Calories" -> NutritionCalories
        "Protein" -> NutritionProtein
        "Water" -> NutritionWater
        "Weight" -> MaterialTheme.colorScheme.primary
        "Exercise" -> NutritionExercise
        else -> MaterialTheme.colorScheme.primary
    }

    val activePoints = dataPoints.filter { it > 0f }
    val percentChangeStr = if (activePoints.size >= 2) {
        val half = (activePoints.size / 2).coerceAtLeast(1)
        val firstHalfAvg = activePoints.take(half).average()
        val secondHalfAvg = activePoints.takeLast(half).average()
        if (firstHalfAvg > 0) {
            val diff = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0
            val prefix = if (diff >= 0) "+" else ""
            "$prefix${String.format(Locale.US, "%.1f", diff)}%"
        } else "+0%"
    } else "+0%"

    Card(
        onClick = { onDetailClick(selectedMetric) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.label_trend),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayAvg,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.trend_unit_per_day, unit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.avg_this_week),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box {
                        OutlinedCard(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(8.dp),
                            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedMetricLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            metrics.forEach { metric ->
                                val label = when (metric) {
                                    "Calories" -> stringResource(R.string.home_label_calories)
                                    "Protein" -> stringResource(R.string.label_protein)
                                    "Water" -> stringResource(R.string.label_water)
                                    "Weight" -> stringResource(R.string.label_weight)
                                    "Exercise" -> stringResource(R.string.label_activity)
                                    else -> metric
                                }
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedMetric = metric
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = LightSuccess.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = percentChangeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = LightSuccess,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = stringResource(R.string.vs_last_week),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            // Real Canvas Trend Line Chart
            ProgressTrendChart(
                dataPoints = dataPoints,
                labels = labels,
                unit = unit,
                lineColor = lineColor,
                pointTypes = if (selectedMetric == "Weight") trendPoints.map { it.isActualWeight } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }
}

@Composable
fun AIInsightCard(insightText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_ai_insight),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = insightText.ifEmpty { stringResource(R.string.progress_ai_insight_empty) },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun DailyReviewSection(
    reviews: List<DailyReviewData>,
    onSeeAllClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_daily_reviews),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (reviews.isEmpty()) {
                Text(
                    text = stringResource(R.string.progress_no_reviews),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                reviews.forEach { review ->
                    DailyReviewItem(data = review, onClick = { onItemClick(review.date) })
                }
            }
        }
    }
}

data class DailyReviewData(
    val day: String,
    val date: String,
    val score: Int,
    val status: String,
    val color: Color
)

@Composable
fun DailyReviewItem(data: DailyReviewData, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(data.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = data.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                val displayDay = when (data.day) {
                    "Today" -> stringResource(R.string.activity_detail_today)
                    "Yesterday" -> stringResource(R.string.label_yesterday)
                    else -> data.day
                }
                Text(
                    text = displayDay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = data.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = data.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = data.color
                )
                val statusLabel = when (data.status) {
                    "Excellent" -> stringResource(R.string.status_excellent)
                    "Good" -> stringResource(R.string.status_good)
                    "Fair" -> stringResource(R.string.status_fair)
                    "Needs Attention" -> stringResource(R.string.status_needs_attention)
                    else -> data.status
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterBottomSheet(
    currentFilter: DateRangeFilter,
    onDismissRequest: () -> Unit,
    onFilterSelected: (DateRangeFilter) -> Unit,
    onCustomClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.progress_select_date_range),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val options = listOf(
                DateRangeFilter.Last1Day to stringResource(R.string.progress_range_1day),
                DateRangeFilter.Last3Days to stringResource(R.string.progress_range_3days),
                DateRangeFilter.Last7Days to stringResource(R.string.progress_range_7days),
                DateRangeFilter.Last30Days to stringResource(R.string.progress_range_30days)
            )

            options.forEach { (filter, label) ->
                val isSelected = currentFilter::class == filter::class
                OutlinedCard(
                    onClick = {
                        onFilterSelected(filter)
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.4f
                        ) else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        brush = if (isSelected) androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.primary
                        ) else CardDefaults.outlinedCardBorder().brush
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Custom Range button
            val isCustom = currentFilter is DateRangeFilter.Custom
            OutlinedCard(
                onClick = {
                    onDismissRequest()
                    onCustomClick()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isCustom) MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.4f
                    ) else MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = if (isCustom) 1.5.dp else 0.5.dp,
                    brush = if (isCustom) androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary) else CardDefaults.outlinedCardBorder().brush
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.progress_custom_date_range),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isCustom) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onDateRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                        dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text(stringResource(R.string.dialog_select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        )
    }
}

@Composable
fun LogWeightDialog(
    initialWeight: Float,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Float, note: String?) -> Unit
) {
    var weightText by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialWeight)) }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val invalidWeightError = stringResource(R.string.progress_dialog_weight_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    stringResource(R.string.dialog_log_weight_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.desc_calibrate_dialog),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.label_weight_kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        Text(
                            stringResource(R.string.unit_kg),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.label_optional_note)) },
                    placeholder = { Text(stringResource(R.string.placeholder_weight_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.toFloatOrNull()
                    if (weight == null || weight <= 20f || weight >= 400f) {
                        errorMessage = invalidWeightError
                    } else {
                        onConfirm(weight, noteText)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.btn_save_weight))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun WeeklyCalibrationBannerCard(
    onCalibrateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.card_weekly_calibration_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(R.string.card_weekly_calibration_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Button(
                onClick = onCalibrateClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_calibrate_now),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CalibrationResultDialog(
    result: AdaptiveCalibrationResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_understand),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = result.statusEmoji,
                    fontSize = 40.sp
                )
                Text(
                    text = stringResource(R.string.dialog_calibration_result_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (result.isAccurate) LightSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.6f
                    )
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isAccurate) LightSuccess else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Metric comparison row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.label_scale_reading),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f kg", result.actualWeight),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LightSuccess
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.label_ai_prior_projection),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f kg", result.projectedWeight),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Deviation indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_deviation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val devSign = if (result.deviationKg > 0) "+" else ""
                    Text(
                        text = "$devSign${String.format(Locale.US, "%.1f kg", result.deviationKg)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isAccurate) LightSuccess else MaterialTheme.colorScheme.primary
                    )
                }

                // Feedback card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result.feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}



