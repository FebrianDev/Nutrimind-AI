package com.fdev.nutrimindai.feature.progress.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.feature.progress.presentation.components.ProgressTrendChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendDetailScreen(
    metric: String,
    viewModel: TrendDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf(
        stringResource(R.string.tab_7_days),
        stringResource(R.string.tab_30_days),
        stringResource(R.string.tab_90_days)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (metric) {
                        "Calories" -> stringResource(R.string.home_label_calories)
                        "Protein" -> stringResource(R.string.label_protein)
                        "Carbs" -> stringResource(R.string.label_carbs)
                        "Fat" -> stringResource(R.string.label_fat)
                        "Water" -> stringResource(R.string.label_water)
                        "Weight" -> stringResource(R.string.label_weight)
                        "Exercise" -> stringResource(R.string.label_activity)
                        else -> metric
                    }
                    Text(
                        titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is TrendDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is TrendDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is TrendDetailUiState.Success -> {
                val detail = state.data
                val lineColor = when (metric) {
                    "Calories" -> NutritionCalories
                    "Protein" -> NutritionProtein
                    "Carbs" -> NutritionCarbs
                    "Fat" -> NutritionFat
                    "Water" -> NutritionWater
                    "Weight" -> MaterialTheme.colorScheme.primary
                    "Exercise" -> NutritionExercise
                    else -> MaterialTheme.colorScheme.primary
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .screenPadding(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = detail.selectedPeriodTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = detail.selectedPeriodTab == index,
                                onClick = {
                                    viewModel.selectPeriodTab(index)
                                },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (detail.selectedPeriodTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }


                    TrendSummarySection(
                        metric = metric,
                        valueStr = detail.avgValueStr,
                        unit = detail.unit,
                        percentChangeStr = detail.percentChangeStr,
                        isPositiveTrend = detail.isPositiveTrend,
                        selectedPeriodTab = detail.selectedPeriodTab
                    )

                    // Canvas Trend Line Chart
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                    ) {
                        ProgressTrendChart(
                            dataPoints = detail.chartPoints,
                            labels = detail.chartLabels,
                            unit = detail.unit,
                            lineColor = lineColor,
                            pointTypes = detail.pointTypes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .cardPadding()
                        )
                    }

                    TrendStatsSection(
                        unit = detail.unit,
                        highestVal = detail.highestValStr,
                        highestSub = detail.highestSub,
                        lowestVal = detail.lowestValStr,
                        lowestSub = detail.lowestSub,
                        goalVal = detail.goalValStr
                    )

                    AboutMetricSection(metric)

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun TrendSummarySection(
    metric: String,
    valueStr: String,
    unit: String,
    percentChangeStr: String,
    isPositiveTrend: Boolean = true,
    selectedPeriodTab: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueStr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.trend_unit_per_day, unit),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val avgPeriodLabel = when (selectedPeriodTab) {
                1 -> stringResource(R.string.avg_this_month)
                2 -> stringResource(R.string.avg_last_3_months)
                else -> stringResource(R.string.avg_this_week)
            }
            Text(
                text = avgPeriodLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val trendColor = if (isPositiveTrend) LightSuccess else MaterialTheme.colorScheme.error
        Surface(
            color = trendColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.badgePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = percentChangeStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = trendColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                val vsPeriodLabel = when (selectedPeriodTab) {
                    1 -> stringResource(R.string.vs_last_30_days)
                    2 -> stringResource(R.string.vs_last_90_days)
                    else -> stringResource(R.string.vs_last_7_days)
                }
                Text(
                    text = vsPeriodLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun TrendStatsSection(
    unit: String,
    highestVal: String,
    highestSub: String,
    lowestVal: String,
    lowestSub: String,
    goalVal: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatItem(modifier = Modifier.weight(1f), label = stringResource(R.string.label_highest), value = highestVal, unit = unit, subValue = highestSub)
        StatItem(modifier = Modifier.weight(1f), label = stringResource(R.string.label_lowest), value = lowestVal, unit = unit, subValue = lowestSub)
        StatItem(modifier = Modifier.weight(1f), label = stringResource(R.string.label_goal), value = goalVal, unit = unit, subValue = "/day")
    }
}

@Composable
fun StatItem(modifier: Modifier, label: String, value: String, unit: String, subValue: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp
                )
            }
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun AboutMetricSection(metric: String) {
    val metricLabel = when (metric) {
        "Calories" -> stringResource(R.string.home_label_calories)
        "Protein" -> stringResource(R.string.label_protein)
        "Carbs" -> stringResource(R.string.label_carbs)
        "Fat" -> stringResource(R.string.label_fat)
        "Water" -> stringResource(R.string.label_water)
        "Weight" -> stringResource(R.string.label_weight)
        "Exercise" -> stringResource(R.string.label_activity)
        else -> metric
    }

    val description = when (metric) {
        "Calories" -> stringResource(R.string.trend_desc_calories)
        "Protein" -> stringResource(R.string.trend_desc_protein)
        "Carbs" -> stringResource(R.string.trend_desc_carbs)
        "Fat" -> stringResource(R.string.trend_desc_fat)
        "Water" -> stringResource(R.string.trend_desc_water)
        "Weight" -> stringResource(R.string.trend_desc_weight)
        "Exercise" -> stringResource(R.string.trend_desc_exercise)
        else -> ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.trend_about_metric, metricLabel),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
