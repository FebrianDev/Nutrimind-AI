package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.badgePadding
import com.fdev.nutrimindai.core.designsystem.cardPadding

@Composable
fun NutritionSection(
    calories: NutritionData,
    protein: NutritionData,
    carbs: NutritionData,
    fat: NutritionData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .cardPadding()
        ) {
            // Header with Icon Circle & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.home_todays_nutrition),
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
                                text = stringResource(R.string.home_nutrition_badge),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.badgePadding()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Circular Calorie Progress on the Left
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(138.dp)
                ) {
                    val progress = if (calories.target > 0) {
                        (calories.current.toFloat() / calories.target).coerceIn(0f, 1f)
                    } else 0f
                    val remainingCalories = calories.target - calories.current
                    val isOverBudget = remainingCalories < 0
                    val progressColor = if (isOverBudget) {
                        Color(0xFFE57373)
                    } else {
                        Color(0xFF4CAF50)
                    }

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 11.dp,
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        strokeCap = StrokeCap.Round
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%,d", calories.current),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/ ${String.format(java.util.Locale.US, "%,d", calories.target)} kcal",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            color = if (isOverBudget) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            } else {
                                Color(0xFF4CAF50).copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isOverBudget) {
                                    stringResource(R.string.home_calories_over, kotlin.math.abs(remainingCalories))
                                } else {
                                    stringResource(R.string.home_calories_remaining, remainingCalories)
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                modifier = Modifier.badgePadding()
                            )
                        }
                    }
                }

                // Macronutrients List on the Right (Protein, Carbs, Fat)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NutritionItem(data = protein, icon = Icons.Rounded.Egg, color = Color(0xFF5C6BC0))
                    NutritionItem(data = carbs, icon = Icons.Rounded.BakeryDining, color = Color(0xFFFFA000))
                    NutritionItem(data = fat, icon = Icons.Rounded.LocalFireDepartment, color = Color(0xFFF44336))
                }
            }
        }
    }
}

@Composable
private fun NutritionItem(
    data: NutritionData,
    icon: ImageVector,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = data.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = stringResource(R.string.home_nutrition_item_format, data.current, data.target, data.unit),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val percent = if (data.target > 0) ((data.current.toFloat() / data.target) * 100).toInt() else 0
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { if (data.target > 0) (data.current.toFloat() / data.target).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = color,
            trackColor = color.copy(alpha = 0.12f),
            strokeCap = StrokeCap.Round
        )
    }
}

data class NutritionData(
    val label: String,
    val current: Int,
    val target: Int,
    val unit: String
)
