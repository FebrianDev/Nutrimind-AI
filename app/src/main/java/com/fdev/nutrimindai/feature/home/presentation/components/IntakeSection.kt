package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    onSyncHealthConnect: (() -> Unit)? = null
) {
    val notLoggedText = stringResource(R.string.home_intake_not_logged)

    val items = listOf(
        IntakeItemData(
            id = "Breakfast",
            title = stringResource(R.string.home_intake_breakfast),
            summary = if (breakfastCalories > 0) stringResource(R.string.home_intake_kcal_val, breakfastCalories) else notLoggedText,
            icon = Icons.Rounded.FreeBreakfast
        ),
        IntakeItemData(
            id = "Lunch",
            title = stringResource(R.string.home_intake_lunch),
            summary = if (lunchCalories > 0) stringResource(R.string.home_intake_kcal_val, lunchCalories) else notLoggedText,
            icon = Icons.Rounded.LunchDining
        ),
        IntakeItemData(
            id = "Dinner",
            title = stringResource(R.string.home_intake_dinner),
            summary = if (dinnerCalories > 0) stringResource(R.string.home_intake_kcal_val, dinnerCalories) else notLoggedText,
            icon = Icons.Rounded.DinnerDining
        ),
        IntakeItemData(
            id = "Others",
            title = stringResource(R.string.home_intake_others),
            summary = if (othersCalories > 0) stringResource(R.string.home_intake_kcal_val, othersCalories) else notLoggedText,
            icon = Icons.Rounded.Restaurant
        ),
        IntakeItemData(
            id = "Water",
            title = stringResource(R.string.home_intake_water),
            summary = waterSummary,
            icon = Icons.Rounded.WaterDrop
        ),
        IntakeItemData(
            id = "Exercise",
            title = stringResource(R.string.home_intake_exercise),
            summary = exerciseSummary,
            icon = Icons.AutoMirrored.Rounded.DirectionsRun
        )
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_todays_intake),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (onSyncHealthConnect != null) {
                androidx.compose.material3.Surface(
                    onClick = onSyncHealthConnect,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Grid 3 Columns, 2 Rows
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in items.indices step 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (j in 0 until 3) {
                        val index = i + j
                        if (index < items.size) {
                            val item = items[index]
                            MealCard(
                                title = item.title,
                                summary = item.summary,
                                icon = item.icon,
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

private data class IntakeItemData(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector
)
