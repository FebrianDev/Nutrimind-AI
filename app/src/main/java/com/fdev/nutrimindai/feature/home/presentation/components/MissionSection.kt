package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.fdev.nutrimindai.feature.onboarding.util.Goal

@Composable
fun MissionSection(
    userGoal: Goal,
    missions: List<DailyMissionItem>,
    modifier: Modifier = Modifier
) {
    val completedCount = missions.count { it.isCompleted }
    val totalCount = missions.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val isAllCompleted = totalCount > 0 && completedCount == totalCount

    val goalBadgeTitle = when (userGoal) {
        Goal.LoseWeight -> stringResource(R.string.home_mission_fat_loss_protocol)
        Goal.GainWeight -> stringResource(R.string.home_mission_hypertrophy_protocol)
        Goal.MaintainWeight -> stringResource(R.string.home_mission_balance_protocol)
    }

    val goalBadgeColor = when (userGoal) {
        Goal.LoseWeight -> NutritionCalories
        Goal.GainWeight -> NutritionProtein
        Goal.MaintainWeight -> LightSuccess
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Goal Protocol Badge and Circular Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = goalBadgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = goalBadgeTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = goalBadgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.home_mission_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.home_mission_progress_format, completedCount, totalCount, (progress * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Progress Indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 5.dp,
                        color = if (isAllCompleted) LightSuccess else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllCompleted) LightSuccess else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Missions list
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                missions.forEach { mission ->
                    MissionItemRow(mission = mission)
                }
            }
        }
    }
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
