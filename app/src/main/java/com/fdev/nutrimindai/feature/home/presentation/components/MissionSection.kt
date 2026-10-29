package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.LightSuccess
import com.fdev.nutrimindai.core.designsystem.NutritionCalories
import com.fdev.nutrimindai.core.designsystem.NutritionExercise
import com.fdev.nutrimindai.core.designsystem.NutritionProtein
import com.fdev.nutrimindai.core.designsystem.NutritionWater
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyMissionItem
import com.fdev.nutrimindai.core.model.Goal

@Composable
fun MissionSection(
    userGoal: Goal,
    missions: List<DailyMissionItem>,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val completedCount = missions.count { it.isCompleted }
    val totalCount = missions.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val isAllCompleted = totalCount > 0 && completedCount == totalCount

    // Sleek, compact card on Home Screen
    Card(
        onClick = { showDialog = true },
        modifier = modifier.fillMaxWidth(),
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isAllCompleted) {
                    Surface(
                        color = LightSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_mission_status_done),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LightSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                // Title
                Text(
                    text = stringResource(R.string.home_mission_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Progress count
                Text(
                    text = stringResource(R.string.home_mission_progress_format, completedCount, totalCount, (progress * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Interactive CTA row (on its own dedicated line to prevent wrapping)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_missions_tap_to_view),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Circular Progress Indicator
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 5.dp,
                    color = if (isAllCompleted) LightSuccess else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )
                if (isAllCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = LightSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Modal Alert Dialog displaying the full detailed list of missions
    if (showDialog) {
        DailyMissionsDialog(
            userGoal = userGoal,
            missions = missions,
            completedCount = completedCount,
            totalCount = totalCount,
            progress = progress,
            isAllCompleted = isAllCompleted,
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
private fun DailyMissionsDialog(
    userGoal: Goal,
    missions: List<DailyMissionItem>,
    completedCount: Int,
    totalCount: Int,
    progress: Float,
    isAllCompleted: Boolean,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.home_mission_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_missions_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Celebratory Banner when 100% completed
                AnimatedVisibility(
                    visible = isAllCompleted,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = LightSuccess.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LightSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.home_mission_all_accomplished_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = LightSuccess
                                )
                                Text(
                                    text = stringResource(R.string.home_mission_all_accomplished_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightSuccess,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_mission_progress_format, completedCount, totalCount, (progress * 100).toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isAllCompleted) LightSuccess else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Missions list
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    missions.forEach { mission ->
                        MissionItemRow(mission = mission)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(R.string.btn_close),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun MissionItemRow(
    mission: DailyMissionItem
) {
    val (icon, iconColor) = getMissionIconAndColor(mission.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (mission.isCompleted) LightSuccess.copy(alpha = 0.06f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category Icon Box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (mission.isCompleted) LightSuccess.copy(alpha = 0.15f)
                    else iconColor.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (mission.isCompleted) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (mission.isCompleted) LightSuccess else iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Mission details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mission.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (mission.isCompleted) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = mission.currentProgressText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (mission.isCompleted) LightSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = mission.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }

        // Status indicator
        if (mission.isCompleted) {
            Surface(
                color = LightSuccess.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.home_mission_status_done),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = LightSuccess,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.RadioButtonUnchecked,
                contentDescription = stringResource(R.string.home_mission_status_in_progress),
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getMissionIconAndColor(missionId: String): Pair<ImageVector, Color> {
    return when {
        missionId.contains("water", ignoreCase = true) -> Icons.Default.WaterDrop to NutritionWater
        missionId.contains("calorie", ignoreCase = true) -> Icons.Default.LocalFireDepartment to NutritionCalories
        missionId.contains("protein", ignoreCase = true) -> Icons.Default.Egg to NutritionProtein
        missionId.contains("exercise", ignoreCase = true) -> Icons.Default.FitnessCenter to NutritionExercise
        missionId.contains("vegetable", ignoreCase = true) || missionId.contains("fiber", ignoreCase = true) -> Icons.Default.Eco to LightSuccess
        missionId.contains("meal", ignoreCase = true) -> Icons.Default.Restaurant to NutritionCalories
        else -> Icons.Default.LocalFireDepartment to NutritionCalories
    }
}
