package com.fdev.nutrimindai.feature.progress.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fdev.nutrimindai.core.util.EvaluatedDayMissions
import com.fdev.nutrimindai.feature.progress.presentation.components.DayMissionDetailDialog
import com.fdev.nutrimindai.feature.progress.presentation.components.DayMissionHistoryRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMissionHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: DailyMissionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDayForDetail by remember { mutableStateOf<EvaluatedDayMissions?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.mission_history_screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DailyMissionHistoryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is DailyMissionHistoryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(AppSpacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.selectPeriodTab(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                is DailyMissionHistoryUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.m),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Period Selection Tabs
                        item {
                            val tabs = listOf(
                                stringResource(R.string.mission_history_tab_7d),
                                stringResource(R.string.mission_history_tab_30d),
                                stringResource(R.string.mission_history_tab_90d)
                            )
                            PrimaryTabRow(
                                selectedTabIndex = state.selectedPeriodTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                divider = {}
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = state.selectedPeriodTab == index,
                                        onClick = { viewModel.selectPeriodTab(index) },
                                        selectedContentColor = MaterialTheme.colorScheme.primary,
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        text = {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (state.selectedPeriodTab == index) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .cardPadding(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HistoryStatTile(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.LocalFireDepartment,
                                        iconTint = NutritionCalories,
                                        value = stringResource(R.string.progress_mission_streak_unit, state.rewardData.currentStreak),
                                        label = stringResource(R.string.progress_mission_streak_label),
                                        containerColor = NutritionCalories.copy(alpha = 0.1f)
                                    )

                                    HistoryStatTile(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Star,
                                        iconTint = NutritionFat,
                                        value = stringResource(R.string.progress_mission_points_val, state.rewardData.totalPoints),
                                        label = stringResource(R.string.progress_mission_points_label),
                                        containerColor = NutritionFat.copy(alpha = 0.12f)
                                    )

                                    HistoryStatTile(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.TrackChanges,
                                        iconTint = LightSuccess,
                                        value = stringResource(R.string.progress_mission_perfect_unit, state.rewardData.perfectDaysCount),
                                        label = stringResource(R.string.progress_mission_perfect_label),
                                        containerColor = LightSuccess.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }

                        // 3. Day List
                        if (state.displayedHistory.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(AppSpacing.xxl),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.progress_mission_no_history),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = state.displayedHistory,
                                key = { it.dateStr }
                            ) { dayRecord ->
                                DayMissionHistoryRow(
                                    dayRecord = dayRecord,
                                    onClick = { selectedDayForDetail = dayRecord }
                                )
                            }
                        }
                    }

                    // Detail Dialog
                    selectedDayForDetail?.let { day ->
                        DayMissionDetailDialog(
                            dayMissions = day,
                            onDismiss = { selectedDayForDetail = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    containerColor: Color,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
