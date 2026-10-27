package com.fdev.nutrimindai.feature.home.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.NutriMindAITheme
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyMissionItem
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.model.TodaySummary
import com.fdev.nutrimindai.feature.dailyintake.presentation.home.HomeUiState
import com.fdev.nutrimindai.feature.dailyintake.presentation.home.HomeViewModel
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.feature.home.presentation.components.CoachInsightCard
import com.fdev.nutrimindai.feature.home.presentation.components.CompleteConfirmationDialog
import com.fdev.nutrimindai.feature.home.presentation.components.CompleteTodayButton
import com.fdev.nutrimindai.feature.home.presentation.components.CompletedCard
import com.fdev.nutrimindai.feature.home.presentation.components.GreetingSection
import com.fdev.nutrimindai.feature.home.presentation.components.IntakeSection
import com.fdev.nutrimindai.feature.home.presentation.components.MissionSection
import com.fdev.nutrimindai.feature.home.presentation.components.NutritionData
import com.fdev.nutrimindai.feature.home.presentation.components.NutritionSection
import com.fdev.nutrimindai.feature.home.presentation.components.ReopenConfirmationDialog
import com.fdev.nutrimindai.feature.onboarding.util.Goal
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToMealDetail: (String) -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToExercise: () -> Unit,
    onNavigateToDailyReview: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coachingTip by viewModel.coachingTip.collectAsStateWithLifecycle()
    val isGeneratingTip by viewModel.isGeneratingTip.collectAsStateWithLifecycle()
    val activeProvider by viewModel.activeProvider.collectAsStateWithLifecycle()

    var showCompleteDialog by remember { mutableStateOf(false) }
    var showReopenDialog by remember { mutableStateOf(false) }

    if (showCompleteDialog) {
        CompleteConfirmationDialog(
            onDismissRequest = { showCompleteDialog = false },
            onConfirm = {
                showCompleteDialog = false
                val targetDate = (uiState as? HomeUiState.Success)?.summary?.date
                viewModel.completeToday {
                    if (!targetDate.isNullOrBlank()) {
                        onNavigateToDailyReview(targetDate)
                    }
                }
            }
        )
    }

    if (showReopenDialog) {
        ReopenConfirmationDialog(
            onDismissRequest = { showReopenDialog = false },
            onConfirm = {
                showReopenDialog = false
                viewModel.reopenToday()
            }
        )
    }

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is HomeUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        is HomeUiState.Empty -> {
            HomeContent(
                summary = TodaySummary(date = "", dailyLogId = null),
                coachingTip = coachingTip,
                isGeneratingTip = isGeneratingTip,
                activeProvider = activeProvider,
                onRefreshTip = { viewModel.refreshCoachingTip(force = true) },
                onNavigateToMealDetail = onNavigateToMealDetail,
                onNavigateToWater = onNavigateToWater,
                onNavigateToExercise = onNavigateToExercise,
                onNavigateToDailyReview = onNavigateToDailyReview,
                onCompleteTodayClick = { showCompleteDialog = true },
                onReopenTodayClick = { showReopenDialog = true }
            )
        }

        is HomeUiState.Success -> {
            HomeContent(
                summary = state.summary,
                coachingTip = coachingTip,
                isGeneratingTip = isGeneratingTip,
                activeProvider = activeProvider,
                onRefreshTip = { viewModel.refreshCoachingTip(force = true) },
                onNavigateToMealDetail = onNavigateToMealDetail,
                onNavigateToWater = onNavigateToWater,
                onNavigateToExercise = onNavigateToExercise,
                onNavigateToDailyReview = onNavigateToDailyReview,
                onCompleteTodayClick = { showCompleteDialog = true },
                onReopenTodayClick = { showReopenDialog = true }
            )
        }
    }
}

@Composable
fun HomeContent(
    summary: TodaySummary,
    coachingTip: String = "",
    isGeneratingTip: Boolean = false,
    activeProvider: AiProvider = AiProvider.META_LLAMA,
    onRefreshTip: () -> Unit = {},
    onNavigateToMealDetail: (String) -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToExercise: () -> Unit,
    onNavigateToDailyReview: (String) -> Unit = {},
    onCompleteTodayClick: () -> Unit,
    onReopenTodayClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val nutrition = summary.nutritionSummary

    // Daily missions tailored specifically to user's primary goal & today's intake
    val missions = buildGoalDrivenMissions(nutrition, summary)

    val calories = NutritionData(
        stringResource(R.string.home_label_calories),
        nutrition.calories,
        nutrition.goalCalories,
        stringResource(R.string.unit_kcal)
    )
    val protein = NutritionData(
        stringResource(R.string.label_protein),
        nutrition.protein.toInt(),
        nutrition.goalProtein,
        stringResource(R.string.unit_grams)
    )
    val carbs = NutritionData(
        stringResource(R.string.label_carbs),
        nutrition.carbs.toInt(),
        nutrition.goalCarbs,
        stringResource(R.string.unit_grams)
    )
    val fat = NutritionData(
        stringResource(R.string.label_fat),
        nutrition.fat.toInt(),
        nutrition.goalFat,
        stringResource(R.string.unit_grams)
    )
    val water = NutritionData(
        stringResource(R.string.label_water),
        nutrition.water,
        nutrition.goalWater,
        stringResource(R.string.unit_ml)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp)
        ) {
            GreetingSection(
                userName = summary.userName,
                userPhotoUrl = summary.userPhotoUrl
            )

            Spacer(modifier = Modifier.height(16.dp))

            CoachInsightCard(
                tipText = coachingTip,
                isGenerating = isGeneratingTip,
                provider = activeProvider,
                onRefreshClick = onRefreshTip
            )

            Spacer(modifier = Modifier.height(20.dp))

            MissionSection(
                userGoal = summary.userGoal,
                missions = missions
            )

            Spacer(modifier = Modifier.height(20.dp))

            NutritionSection(
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                water = water
            )

            Spacer(modifier = Modifier.height(20.dp))

            IntakeSection(
                breakfastCalories = summary.breakfastList.sumOf { it.calories },
                lunchCalories = summary.lunchList.sumOf { it.calories },
                dinnerCalories = summary.dinnerList.sumOf { it.calories },
                othersCalories = summary.othersList.sumOf { it.calories },
                waterSummary = formatWaterSummary(nutrition.water, nutrition.goalWater),
                exerciseSummary = formatExerciseSummary(summary.exerciseList.sumOf { it.durationMinute }),
                onMealClick = { mealType ->
                    when (mealType) {
                        "Water" -> onNavigateToWater()
                        "Exercise" -> onNavigateToExercise()
                        else -> onNavigateToMealDetail(mealType)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            if (summary.status == DailyLogStatus.COMPLETED) {
                CompletedCard(
                    onReopenClick = onReopenTodayClick,
                    onViewReviewClick = {
                        if (summary.date.isNotBlank()) {
                            onNavigateToDailyReview(summary.date)
                        }
                    }
                )
            } else {
                CompleteTodayButton(
                    onClick = onCompleteTodayClick,
                    enabled = summary.status == DailyLogStatus.DRAFT
                )
            }
        }
    }
}

@Composable
private fun buildGoalDrivenMissions(
    nutrition: NutritionSummary,
    summary: TodaySummary
): List<DailyMissionItem> {
    val userGoal = summary.userGoal
    val waterGoalMl = nutrition.goalWater
    val waterAchieved = nutrition.water >= (waterGoalMl * 0.9).toInt()
    val totalExerciseMin = summary.exerciseList.sumOf { it.durationMinute }
    val exerciseAchieved = totalExerciseMin >= 30
    val hasVegetables = (summary.breakfastList + summary.lunchList + summary.dinnerList + summary.othersList).any {
        it.foodName.contains("Vegetable", ignoreCase = true) ||
            it.foodName.contains("Salad", ignoreCase = true) ||
            it.foodName.contains("Sayur", ignoreCase = true) ||
            it.foodName.contains("Spinach", ignoreCase = true) ||
            it.foodName.contains("Broccoli", ignoreCase = true) ||
            it.fiber >= 3.0
    }

    val waterProgressText = stringResource(
        R.string.mission_water_hydration_progress,
        String.format(Locale.US, "%.1f", nutrition.water / 1000.0),
        String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)
    )
    val exerciseProgressText = if (totalExerciseMin > 0) {
        stringResource(R.string.mission_exercise_burn_progress, totalExerciseMin)
    } else {
        stringResource(R.string.mission_exercise_burn_progress_zero)
    }

    return when (userGoal) {
        Goal.LoseWeight -> {
            val calorieDeficitMet = (nutrition.calories in 500..nutrition.goalCalories) ||
                (nutrition.calories <= nutrition.goalCalories && (summary.breakfastList.isNotEmpty() || summary.lunchList.isNotEmpty()))
            val proteinTargetMet = nutrition.protein >= (nutrition.goalProtein * 0.8)

            listOf(
                DailyMissionItem(
                    id = "calorie_deficit",
                    title = stringResource(R.string.mission_calorie_deficit_title),
                    description = stringResource(R.string.mission_calorie_deficit_desc, nutrition.goalCalories),
                    currentProgressText = stringResource(R.string.mission_calorie_deficit_progress, nutrition.calories, nutrition.goalCalories),
                    isCompleted = calorieDeficitMet
                ),
                DailyMissionItem(
                    id = "water_hydration",
                    title = stringResource(R.string.mission_water_hydration_title),
                    description = stringResource(R.string.mission_water_hydration_desc, String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)),
                    currentProgressText = waterProgressText,
                    isCompleted = waterAchieved
                ),
                DailyMissionItem(
                    id = "protein_preservation",
                    title = stringResource(R.string.mission_protein_preservation_title),
                    description = stringResource(R.string.mission_protein_preservation_desc, nutrition.goalProtein),
                    currentProgressText = stringResource(R.string.mission_protein_preservation_progress, nutrition.protein.toInt(), nutrition.goalProtein),
                    isCompleted = proteinTargetMet
                ),
                DailyMissionItem(
                    id = "exercise_burn",
                    title = stringResource(R.string.mission_exercise_burn_title),
                    description = stringResource(R.string.mission_exercise_burn_desc),
                    currentProgressText = exerciseProgressText,
                    isCompleted = exerciseAchieved
                ),
                DailyMissionItem(
                    id = "vegetables_fiber",
                    title = stringResource(R.string.mission_vegetables_fiber_title),
                    description = stringResource(R.string.mission_vegetables_fiber_desc),
                    currentProgressText = if (hasVegetables) stringResource(R.string.mission_vegetables_fiber_logged) else stringResource(R.string.mission_vegetables_fiber_add),
                    isCompleted = hasVegetables
                )
            )
        }
        Goal.GainWeight -> {
            val calorieSurplusMet = nutrition.calories >= (nutrition.goalCalories * 0.9).toInt()
            val highProteinMet = nutrition.protein >= (nutrition.goalProtein * 0.85)
            val mainMealsCount = listOf(summary.breakfastList, summary.lunchList, summary.dinnerList).count { it.isNotEmpty() }

            listOf(
                DailyMissionItem(
                    id = "calorie_surplus",
                    title = stringResource(R.string.mission_calorie_surplus_title),
                    description = stringResource(R.string.mission_calorie_surplus_desc, nutrition.goalCalories),
                    currentProgressText = stringResource(R.string.mission_calorie_deficit_progress, nutrition.calories, nutrition.goalCalories),
                    isCompleted = calorieSurplusMet
                ),
                DailyMissionItem(
                    id = "protein_hypertrophy",
                    title = stringResource(R.string.mission_protein_hypertrophy_title),
                    description = stringResource(R.string.mission_protein_preservation_desc, nutrition.goalProtein),
                    currentProgressText = stringResource(R.string.mission_protein_preservation_progress, nutrition.protein.toInt(), nutrition.goalProtein),
                    isCompleted = highProteinMet
                ),
                DailyMissionItem(
                    id = "main_meals",
                    title = stringResource(R.string.mission_main_meals_title),
                    description = stringResource(R.string.mission_main_meals_desc),
                    currentProgressText = stringResource(R.string.mission_main_meals_progress, mainMealsCount),
                    isCompleted = mainMealsCount >= 3
                ),
                DailyMissionItem(
                    id = "exercise_strength",
                    title = stringResource(R.string.mission_exercise_strength_title),
                    description = stringResource(R.string.mission_exercise_strength_desc),
                    currentProgressText = exerciseProgressText,
                    isCompleted = exerciseAchieved
                ),
                DailyMissionItem(
                    id = "water_hydration",
                    title = stringResource(R.string.mission_water_recovery_title),
                    description = stringResource(R.string.mission_water_hydration_desc, String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)),
                    currentProgressText = waterProgressText,
                    isCompleted = waterAchieved
                )
            )
        }
        Goal.MaintainWeight -> {
            val calorieBalanceMet = nutrition.calories in (nutrition.goalCalories - 250)..(nutrition.goalCalories + 250) &&
                (summary.breakfastList.isNotEmpty() || summary.lunchList.isNotEmpty())
            val proteinBalancedMet = nutrition.protein >= (nutrition.goalProtein * 0.8)

            listOf(
                DailyMissionItem(
                    id = "calorie_balance",
                    title = stringResource(R.string.mission_calorie_balance_title),
                    description = stringResource(R.string.mission_calorie_balance_desc, nutrition.goalCalories),
                    currentProgressText = stringResource(R.string.mission_calorie_deficit_progress, nutrition.calories, nutrition.goalCalories),
                    isCompleted = calorieBalanceMet
                ),
                DailyMissionItem(
                    id = "water_hydration",
                    title = stringResource(R.string.mission_water_vitality_title),
                    description = stringResource(R.string.mission_water_hydration_desc, String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)),
                    currentProgressText = waterProgressText,
                    isCompleted = waterAchieved
                ),
                DailyMissionItem(
                    id = "protein_daily",
                    title = stringResource(R.string.mission_protein_daily_title),
                    description = stringResource(R.string.mission_protein_preservation_desc, nutrition.goalProtein),
                    currentProgressText = stringResource(R.string.mission_protein_preservation_progress, nutrition.protein.toInt(), nutrition.goalProtein),
                    isCompleted = proteinBalancedMet
                ),
                DailyMissionItem(
                    id = "exercise_daily",
                    title = stringResource(R.string.mission_exercise_daily_title),
                    description = stringResource(R.string.mission_exercise_daily_desc),
                    currentProgressText = exerciseProgressText,
                    isCompleted = exerciseAchieved
                ),
                DailyMissionItem(
                    id = "vegetables_fiber",
                    title = stringResource(R.string.mission_vegetables_greens_title),
                    description = stringResource(R.string.mission_vegetables_greens_desc),
                    currentProgressText = if (hasVegetables) stringResource(R.string.mission_vegetables_fiber_logged) else stringResource(R.string.mission_vegetables_fiber_add),
                    isCompleted = hasVegetables
                )
            )
        }
    }
}

@Composable
private fun formatWaterSummary(currentMl: Int, goalMl: Int): String {
    val currentL = currentMl / 1000.0
    val goalL = goalMl / 1000.0
    return stringResource(
        R.string.home_intake_water_format,
        String.format(Locale.US, "%.1f", currentL),
        String.format(Locale.US, "%.1f", goalL)
    )
}

@Composable
private fun formatExerciseSummary(totalMinutes: Int): String {
    return if (totalMinutes > 0) {
        stringResource(R.string.home_intake_exercise_format, totalMinutes)
    } else {
        stringResource(R.string.home_intake_not_logged)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NutriMindAITheme {
        HomeScreen(
            onNavigateToMealDetail = {},
            onNavigateToWater = {},
            onNavigateToExercise = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenDarkPreview() {
    NutriMindAITheme {
        HomeScreen(
            onNavigateToMealDetail = {},
            onNavigateToWater = {},
            onNavigateToExercise = {}
        )
    }
}
