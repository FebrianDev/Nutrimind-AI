package com.fdev.nutrimindai.feature.activity.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    date: String,
    viewModel: ActivityDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ActivityDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ActivityDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ActivityDetailUiState.Success -> {
                val data = state.data
                ActivityDetailContent(
                    data = data,
                    onBackClick = onBackClick,
                    onCompleteTodayClick = { viewModel.completeToday() },
                    onReopenTodayClick = { viewModel.reopenToday() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDetailContent(
    data: ActivityDetailData,
    onBackClick: () -> Unit,
    onCompleteTodayClick: () -> Unit,
    onReopenTodayClick: () -> Unit
) {
    val isDraft = data.status.equals("Draft", ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = data.displayDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (data.isToday) stringResource(R.string.activity_detail_today) else stringResource(R.string.activity_detail_journal_entry),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    Surface(
                        color = if (isDraft) NutritionCalories.copy(alpha = 0.1f) else LightSuccess.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        val statusLabel = when (data.status) {
                            "Draft" -> stringResource(R.string.status_draft)
                            "Completed" -> stringResource(R.string.status_completed)
                            else -> stringResource(R.string.status_missed)
                        }
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDraft) NutritionCalories else LightSuccess
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ActivityStatsSummary(data = data)

            if (data.isToday) {
                TodayMotivationBanner()
            } else {
                CompletedBanner()
            }

            TimelineSection(items = data.timelineItems)

            DailyScoreCard(score = data.healthScore)

            if (data.isToday && isDraft) {
                Button(
                    onClick = onCompleteTodayClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightSuccess)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.btn_complete_today), fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.complete_today_desc), fontSize = 10.sp)
                        }
                    }
                }
            } else if (data.isToday && !isDraft) {
                OutlinedButton(
                    onClick = onReopenTodayClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(width = 0.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.home_btn_reopen_journal), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActivityStatsSummary(data: ActivityDetailData) {
    val waterLiters = String.format(Locale.US, "%.1fL", data.totalWaterMl / 1000.0)
    val formattedProtein = "${data.totalProtein.toInt()}g"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DetailStatItem(icon = Icons.Default.LocalFireDepartment, color = NutritionCalories, value = "${data.totalCalories}", label = stringResource(R.string.home_label_calories))
        DetailStatItem(icon = Icons.Default.Egg, color = NutritionProtein, value = formattedProtein, label = stringResource(R.string.label_protein))
        DetailStatItem(icon = Icons.Default.WaterDrop, color = NutritionWater, value = waterLiters, label = stringResource(R.string.label_water))
        DetailStatItem(icon = Icons.AutoMirrored.Filled.DirectionsRun, color = NutritionExercise, value = "${data.exerciseCalories}", label = stringResource(R.string.label_kcal_burned))
    }
}

@Composable
fun DetailStatItem(icon: ImageVector, color: Color, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TodayMotivationBanner() {
    Surface(
        color = LightSuccess.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LightSuccess, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.ai_daily_score_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LightSuccess,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CompletedBanner() {
    Surface(
        color = LightSuccess.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = LightSuccess, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.activity_detail_completed_msg),
                style = MaterialTheme.typography.bodySmall,
                color = LightSuccess,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TimelineSection(items: List<ActivityTimelineItemData>) {
    if (items.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.activity_detail_empty_entries),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.forEach { item ->
                val (icon, color) = getTimelineIconAndColor(item.type)
                TimelineItem(
                    time = item.time,
                    icon = icon,
                    iconColor = color,
                    title = item.title,
                    subtitle = item.subtitle,
                    value = item.value,
                    isLocked = item.isLocked
                )
            }
        }
    }
}

private fun getTimelineIconAndColor(type: String): Pair<ImageVector, Color> {
    return when (type) {
        "BREAKFAST" -> Icons.Default.WbSunny to Color(0xFFFFB74D)
        "LUNCH" -> Icons.Default.Restaurant to Color(0xFFFF8A65)
        "DINNER" -> Icons.Default.Nightlight to Color(0xFF9575CD)
        "OTHERS" -> Icons.Default.Coffee to Color(0xFFE57373)
        "WATER" -> Icons.Default.WaterDrop to NutritionWater
        "EXERCISE" -> Icons.AutoMirrored.Filled.DirectionsRun to NutritionExercise
        else -> Icons.Default.Info to Color(0xFF2196F3)
    }
}

@Composable
fun TimelineItem(
    time: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    value: String,
    isLocked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(8.dp)
                .background(iconColor, CircleShape)
        )

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (subtitle.isNotEmpty()) {
                        Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyScoreCard(score: Int) {
    val statusText = when {
        score >= 90 -> stringResource(R.string.activity_detail_score_excellent)
        score >= 75 -> stringResource(R.string.activity_detail_score_great)
        score >= 60 -> stringResource(R.string.activity_detail_score_fair)
        else -> stringResource(R.string.activity_detail_score_keep_improving)
    }
    val subtitleText = when {
        score >= 90 -> stringResource(R.string.activity_detail_sub_excellent)
        score >= 75 -> stringResource(R.string.activity_detail_sub_great)
        score >= 60 -> stringResource(R.string.activity_detail_sub_fair)
        else -> stringResource(R.string.activity_detail_sub_keep_improving)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.label_daily_score), style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = LightSuccess)
                    Text(text = stringResource(R.string.score_out_of_100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Column(modifier = Modifier.weight(1f)) {
                Text(text = statusText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitleText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
