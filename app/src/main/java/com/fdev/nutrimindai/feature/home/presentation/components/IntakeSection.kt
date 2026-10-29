package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R

@Composable
fun IntakeSection(
    breakfastCalories: Int,
    lunchCalories: Int,
    dinnerCalories: Int,
    othersCalories: Int,
    waterSummary: String,
    exerciseSummary: String,
    onMealClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isWaterLogged: Boolean = false,
    isExerciseLogged: Boolean = false,
    onSyncHealthConnect: (() -> Unit)? = null
) {
    val notLoggedText = stringResource(R.string.home_intake_not_logged)

    val items = listOf(
        IntakeItemData(
            id = "Breakfast",
            title = stringResource(R.string.home_intake_breakfast),
            summary = if (breakfastCalories > 0) stringResource(R.string.home_intake_kcal_val, breakfastCalories) else notLoggedText,
            isLogged = breakfastCalories > 0,
            icon = Icons.Rounded.FreeBreakfast
        ),
        IntakeItemData(
            id = "Lunch",
            title = stringResource(R.string.home_intake_lunch),
            summary = if (lunchCalories > 0) stringResource(R.string.home_intake_kcal_val, lunchCalories) else notLoggedText,
            isLogged = lunchCalories > 0,
            icon = Icons.Rounded.LunchDining
        ),
        IntakeItemData(
            id = "Dinner",
            title = stringResource(R.string.home_intake_dinner),
            summary = if (dinnerCalories > 0) stringResource(R.string.home_intake_kcal_val, dinnerCalories) else notLoggedText,
            isLogged = dinnerCalories > 0,
            icon = Icons.Rounded.DinnerDining
        ),
        IntakeItemData(
            id = "Others",
            title = stringResource(R.string.home_intake_others),
            summary = if (othersCalories > 0) stringResource(R.string.home_intake_kcal_val, othersCalories) else notLoggedText,
            isLogged = othersCalories > 0,
            icon = Icons.Rounded.Restaurant
        ),
        IntakeItemData(
            id = "Water",
            title = stringResource(R.string.home_intake_water),
            summary = waterSummary,
            isLogged = isWaterLogged,
            icon = Icons.Rounded.WaterDrop
        ),
        IntakeItemData(
            id = "Exercise",
            title = stringResource(R.string.home_intake_exercise),
            summary = exerciseSummary,
            isLogged = isExerciseLogged,
            icon = Icons.AutoMirrored.Rounded.DirectionsRun
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with Icon Circle, Title & Badge, and Sync Watch Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestaurantMenu,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.home_todays_intake),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_intake_badge),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (onSyncHealthConnect != null) {
                    Surface(
                        onClick = onSyncHealthConnect,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Watch,
                                contentDescription = stringResource(R.string.health_connect_sync),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Sync Watch",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid 3 Columns, 2 Rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in items.indices step 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (j in 0 until 3) {
                            val index = i + j
                            if (index < items.size) {
                                val item = items[index]
                                MealCard(
                                    title = item.title,
                                    summary = item.summary,
                                    icon = item.icon,
                                    isLogged = item.isLogged,
                                    onClick = { onMealClick(item.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class IntakeItemData(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val isLogged: Boolean = false
)
