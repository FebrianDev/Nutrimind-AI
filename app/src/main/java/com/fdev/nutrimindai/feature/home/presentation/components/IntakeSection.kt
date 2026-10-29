package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    isWaterLogged: Boolean = false,
    isExerciseLogged: Boolean = false
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header with Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_todays_intake),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
