package com.fdev.nutrimindai.feature.progress.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.feature.progress.presentation.components.ShareSummaryBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReviewScreen(
    date: String,
    viewModel: DailyReviewViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showShareSheet by remember { mutableStateOf(false) }

    val currentDetail = (uiState as? DailyReviewUiState.Success)?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.daily_review_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentDetail != null) {
                                showShareSheet = true
                            }
                        },
                        enabled = currentDetail != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.btn_share))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is DailyReviewUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DailyReviewUiState.Error -> {
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

            is DailyReviewUiState.Success -> {
                val detail = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    DateAndScoreHeader(date = detail.displayDate, score = detail.score, status = detail.status)
                    NutritionBreakdownSection(detail = detail)
                    AISummarySection(
                        summary = detail.aiSummary
                    )
                    NotesSection(note = detail.note)

                    // Prominent Share Summary Button
                    Button(
                        onClick = { showShareSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.share_summary_btn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.medical_disclaimer_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showShareSheet && currentDetail != null) {
            ShareSummaryBottomSheet(
                detail = currentDetail,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}


@Composable
fun DateAndScoreHeader(date: String, score: Int, status: String) {
    val statusColor = if (score >= 80) LightSuccess else NutritionFat
    val statusLabel = when (status) {
        "Excellent" -> stringResource(R.string.status_excellent)
        "Good" -> stringResource(R.string.status_good)
        "Fair" -> stringResource(R.string.status_fair)
        "Needs Attention" -> stringResource(R.string.status_needs_attention)
        else -> status
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(NutritionCalories.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = NutritionCalories
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.daily_review_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
                Text(
                    text = stringResource(R.string.score_out_of_100),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NutritionBreakdownSection(detail: DailyReviewDetailData) {
    val calProg = (detail.calories.toFloat() / detail.targetCalories).coerceIn(0f, 1f)
    val calPercent = "${(calProg * 100).toInt()}%"

    val proteinProg = (detail.protein.toFloat() / detail.targetProtein.toFloat()).coerceIn(0f, 1f)
    val proteinPercent = "${(proteinProg * 100).toInt()}%"

    val carbsProg = (detail.carbs.toFloat() / detail.targetCarbs.toFloat()).coerceIn(0f, 1f)
    val carbsPercent = "${(carbsProg * 100).toInt()}%"

    val fatProg = (detail.fat.toFloat() / detail.targetFat.toFloat()).coerceIn(0f, 1f)
    val fatPercent = "${(fatProg * 100).toInt()}%"

    val waterL = detail.water / 1000f
    val targetWaterL = detail.targetWater / 1000f
    val waterProg = (detail.water.toFloat() / detail.targetWater).coerceIn(0f, 1f)
    val waterPercent = "${(waterProg * 100).toInt()}%"

    val exProg = (detail.exerciseMinutes.toFloat() / detail.targetExerciseMinutes).coerceIn(0f, 1f)
    val exPercent = if (exProg >= 1f) stringResource(R.string.status_completed) else "${(exProg * 100).toInt()}%"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_nutrition_score),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NutritionProgressRow(
                icon = Icons.Default.LocalFireDepartment,
                iconColor = NutritionCalories,
                label = stringResource(R.string.home_label_calories),
                current = String.format("%,d", detail.calories),
                target = "${detail.targetCalories} kcal",
                progress = calProg,
                percent = calPercent
            )
            NutritionProgressRow(
                icon = Icons.Default.Egg,
                iconColor = NutritionProtein,
                label = stringResource(R.string.label_protein),
                current = "${detail.protein.toInt()}",
                target = "${detail.targetProtein.toInt()} g",
                progress = proteinProg,
                percent = proteinPercent
            )
            NutritionProgressRow(
                icon = Icons.Default.Grain,
                iconColor = NutritionCarbs,
                label = stringResource(R.string.label_carbs),
                current = "${detail.carbs.toInt()}",
                target = "${detail.targetCarbs.toInt()} g",
                progress = carbsProg,
                percent = carbsPercent
            )
            NutritionProgressRow(
                icon = Icons.Default.Opacity,
                iconColor = NutritionFat,
                label = stringResource(R.string.label_fat),
                current = "${detail.fat.toInt()}",
                target = "${detail.targetFat.toInt()} g",
                progress = fatProg,
                percent = fatPercent
            )
            NutritionProgressRow(
                icon = Icons.Default.WaterDrop,
                iconColor = NutritionWater,
                label = stringResource(R.string.label_water),
                current = String.format("%.1f", waterL),
                target = String.format("%.1f L", targetWaterL),
                progress = waterProg,
                percent = waterPercent
            )
            NutritionProgressRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                iconColor = NutritionExercise,
                label = stringResource(R.string.label_activity),
                current = "${detail.exerciseMinutes}",
                target = "${detail.targetExerciseMinutes} min",
                progress = exProg,
                percent = exPercent
            )
        }
    }
}

@Composable
fun NutritionProgressRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    current: String,
    target: String,
    progress: Float,
    percent: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$current / $target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = percent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (percent == stringResource(R.string.status_completed)) LightSuccess else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (progress >= 1f) LightSuccess else iconColor,
                trackColor = iconColor.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AISummarySection(
    summary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_ai_summary),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.ifBlank { stringResource(R.string.daily_review_default_ai_summary) },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}


@Composable
fun NotesSection(note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.label_notes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = note.ifEmpty { stringResource(R.string.progress_no_notes) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
