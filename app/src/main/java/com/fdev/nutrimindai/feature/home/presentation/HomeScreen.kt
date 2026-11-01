package com.fdev.nutrimindai.feature.home.presentation

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.core.designsystem.NutriMindAITheme
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyMissionItem
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.model.TodaySummary
import com.fdev.nutrimindai.feature.dailyintake.presentation.nutrichef.NutriChefBottomSheet
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
import kotlinx.coroutines.launch
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
    var showNutriChefBottomSheet by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkDateAndReloadIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showCompleteDialog) {
        CompleteConfirmationDialog(
            onDismissRequest = { showCompleteDialog = false },
            onConfirm = {
                showCompleteDialog = false
                viewModel.completeToday()
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

    if (showNutriChefBottomSheet) {
        NutriChefBottomSheet(
            onDismiss = { showNutriChefBottomSheet = false },
            onMealLogged = {
                viewModel.checkDateAndReloadIfNeeded()
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
                onOpenNutriChef = { showNutriChefBottomSheet = true },
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
                onOpenNutriChef = { showNutriChefBottomSheet = true },
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
    onOpenNutriChef: () -> Unit = {},
    onCompleteTodayClick: () -> Unit,
    onReopenTodayClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val nutrition = summary.nutritionSummary

    // Daily missions tailored specifically to user's primary goal & today's intake (cached)
    val missions = remember(
        summary.userGoal,
        nutrition.calories,
        nutrition.goalCalories,
        nutrition.protein,
        nutrition.goalProtein,
        nutrition.water,
        nutrition.goalWater,
        summary.breakfastList.size,
        summary.lunchList.size,
        summary.dinnerList.size,
        summary.othersList.size,
        summary.exerciseList.size
    ) {
        buildGoalDrivenMissions(context, nutrition, summary)
    }

    val calories = remember(nutrition.calories, nutrition.goalCalories) {
        NutritionData(
            context.getString(R.string.home_label_calories),
            nutrition.calories,
            nutrition.goalCalories,
            context.getString(R.string.unit_kcal)
        )
    }
    val protein = remember(nutrition.protein, nutrition.goalProtein) {
        NutritionData(
            context.getString(R.string.label_protein),
            nutrition.protein.toInt(),
            nutrition.goalProtein,
            context.getString(R.string.unit_grams)
        )
    }
    val carbs = remember(nutrition.carbs, nutrition.goalCarbs) {
        NutritionData(
            context.getString(R.string.label_carbs),
            nutrition.carbs.toInt(),
            nutrition.goalCarbs,
            context.getString(R.string.unit_grams)
        )
    }
    val fat = remember(nutrition.fat, nutrition.goalFat) {
        NutritionData(
            context.getString(R.string.label_fat),
            nutrition.fat.toInt(),
            nutrition.goalFat,
            context.getString(R.string.unit_grams)
        )
    }
    val waterSummary = remember(nutrition.water, nutrition.goalWater) {
        formatWaterSummary(context, nutrition.water, nutrition.goalWater)
    }
    val exerciseSummary = remember(summary.exerciseList) {
        formatExerciseSummary(context, summary.exerciseList.sumOf { it.durationMinute })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            GreetingSection(
                userName = summary.userName,
                userPhotoUrl = summary.userPhotoUrl,
                date = summary.date,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .verticalScroll(scrollState)
                    .padding(bottom = 100.dp)
            ) {
                MissionSection(
                    userGoal = summary.userGoal,
                    missions = missions
                )

                Spacer(modifier = Modifier.height(16.dp))

                NutritionSection(
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat
                )

                Spacer(modifier = Modifier.height(16.dp))

                IntakeSection(
                    breakfastCalories = summary.breakfastList.sumOf { it.calories },
                    lunchCalories = summary.lunchList.sumOf { it.calories },
                    dinnerCalories = summary.dinnerList.sumOf { it.calories },
                    othersCalories = summary.othersList.sumOf { it.calories },
                    waterSummary = waterSummary,
                    exerciseSummary = exerciseSummary,
                    isWaterLogged = summary.waterTotalMl > 0,
                    isExerciseLogged = summary.exerciseList.isNotEmpty(),
                    onOpenNutriChef = null,
                    onMealClick = { mealType ->
                        when (mealType) {
                            "Water" -> onNavigateToWater()
                            "Exercise" -> onNavigateToExercise()
                            else -> onNavigateToMealDetail(mealType)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                CoachInsightCard(
                    tipText = coachingTip,
                    isGenerating = isGeneratingTip,
                    provider = activeProvider,
                    onRefreshClick = onRefreshTip
                )
            }
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

private fun buildGoalDrivenMissions(
    context: Context,
    nutrition: NutritionSummary,
    summary: TodaySummary
): List<DailyMissionItem> {
    val userGoal = summary.userGoal
    val waterGoalMl = nutrition.goalWater
    val waterAchieved = nutrition.water >= (waterGoalMl * 0.9).toInt()
    val totalExerciseMin = summary.exerciseList.sumOf { it.durationMinute }
    val exerciseAchieved = totalExerciseMin >= 30
    val hasVegetables =
        (summary.breakfastList + summary.lunchList + summary.dinnerList + summary.othersList).any {
            it.foodName.contains("Vegetable", ignoreCase = true) ||
                    it.foodName.contains("Salad", ignoreCase = true) ||
                    it.foodName.contains("Sayur", ignoreCase = true) ||
                    it.foodName.contains("Spinach", ignoreCase = true) ||
                    it.foodName.contains("Broccoli", ignoreCase = true) ||
                    it.fiber >= 3.0
        }

    val waterProgressText = context.getString(
        R.string.mission_water_hydration_progress,
        String.format(Locale.US, "%.1f", nutrition.water / 1000.0),
        String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)
    )
    val exerciseProgressText = if (totalExerciseMin > 0) {
        context.getString(R.string.mission_exercise_burn_progress, totalExerciseMin)
    } else {
        context.getString(R.string.mission_exercise_burn_progress_zero)
    }

    return when (userGoal) {
        Goal.LoseWeight -> {
            val calorieDeficitMet = (nutrition.calories in 500..nutrition.goalCalories) ||
                    (nutrition.calories <= nutrition.goalCalories && (summary.breakfastList.isNotEmpty() || summary.lunchList.isNotEmpty()))
            val proteinTargetMet = nutrition.protein >= (nutrition.goalProtein * 0.8)

            listOf(
                DailyMissionItem(
                    id = "calorie_deficit",
                    title = context.getString(R.string.mission_calorie_deficit_title),
                    description = context.getString(
                        R.string.mission_calorie_deficit_desc,
                        nutrition.goalCalories
                    ),
                    currentProgressText = context.getString(
                        R.string.mission_calorie_deficit_progress,
                        nutrition.calories,
                        nutrition.goalCalories
                    ),
                    isCompleted = calorieDeficitMet,
                    progressFraction = (nutrition.calories.toFloat() / nutrition.goalCalories.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "water_hydration",
                    title = context.getString(R.string.mission_water_hydration_title),
                    description = context.getString(
                        R.string.mission_water_hydration_desc,
                        String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)
                    ),
                    currentProgressText = waterProgressText,
                    isCompleted = waterAchieved,
                    progressFraction = (nutrition.water.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "protein_preservation",
                    title = context.getString(R.string.mission_protein_preservation_title),
                    description = context.getString(
                        R.string.mission_protein_preservation_desc,
                        nutrition.goalProtein
                    ),
                    currentProgressText = context.getString(
                        R.string.mission_protein_preservation_progress,
                        nutrition.protein.toInt(),
                        nutrition.goalProtein
                    ),
                    isCompleted = proteinTargetMet,
                    progressFraction = (nutrition.protein.toFloat() / nutrition.goalProtein.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "exercise_burn",
                    title = context.getString(R.string.mission_exercise_burn_title),
                    description = context.getString(R.string.mission_exercise_burn_desc),
                    currentProgressText = exerciseProgressText,
                    isCompleted = exerciseAchieved,
                    progressFraction = (totalExerciseMin.toFloat() / 30f).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "vegetables_fiber",
                    title = context.getString(R.string.mission_vegetables_fiber_title),
                    description = context.getString(R.string.mission_vegetables_fiber_desc),
                    currentProgressText = if (hasVegetables) context.getString(R.string.mission_vegetables_fiber_logged) else context.getString(
                        R.string.mission_vegetables_fiber_add
                    ),
                    isCompleted = hasVegetables,
                    progressFraction = if (hasVegetables) 1f else 0f
                )
            )
        }

        Goal.GainWeight -> {
            val calorieSurplusMet = nutrition.calories >= (nutrition.goalCalories * 0.9).toInt()
            val highProteinMet = nutrition.protein >= (nutrition.goalProtein * 0.85)
            val mainMealsCount = listOf(
                summary.breakfastList,
                summary.lunchList,
                summary.dinnerList
            ).count { it.isNotEmpty() }

            listOf(
                DailyMissionItem(
                    id = "calorie_surplus",
                    title = context.getString(R.string.mission_calorie_surplus_title),
                    description = context.getString(
                        R.string.mission_calorie_surplus_desc,
                        nutrition.goalCalories
                    ),
                    currentProgressText = context.getString(
                        R.string.mission_calorie_deficit_progress,
                        nutrition.calories,
                        nutrition.goalCalories
                    ),
                    isCompleted = calorieSurplusMet,
                    progressFraction = (nutrition.calories.toFloat() / nutrition.goalCalories.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "protein_hypertrophy",
                    title = context.getString(R.string.mission_protein_hypertrophy_title),
                    description = context.getString(
                        R.string.mission_protein_preservation_desc,
                        nutrition.goalProtein
                    ),
                    currentProgressText = context.getString(
                        R.string.mission_protein_preservation_progress,
                        nutrition.protein.toInt(),
                        nutrition.goalProtein
                    ),
                    isCompleted = highProteinMet,
                    progressFraction = (nutrition.protein.toFloat() / nutrition.goalProtein.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "main_meals",
                    title = context.getString(R.string.mission_main_meals_title),
                    description = context.getString(R.string.mission_main_meals_desc),
                    currentProgressText = context.getString(
                        R.string.mission_main_meals_progress,
                        mainMealsCount
                    ),
                    isCompleted = mainMealsCount >= 3,
                    progressFraction = (mainMealsCount.toFloat() / 3f).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "exercise_strength",
                    title = context.getString(R.string.mission_exercise_strength_title),
                    description = context.getString(R.string.mission_exercise_strength_desc),
                    currentProgressText = exerciseProgressText,
                    isCompleted = exerciseAchieved,
                    progressFraction = (totalExerciseMin.toFloat() / 30f).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "water_hydration",
                    title = context.getString(R.string.mission_water_recovery_title),
                    description = context.getString(
                        R.string.mission_water_hydration_desc,
                        String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)
                    ),
                    currentProgressText = waterProgressText,
                    isCompleted = waterAchieved,
                    progressFraction = (nutrition.water.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
                )
            )
        }

        Goal.MaintainWeight -> {
            val calorieBalanceMet =
                nutrition.calories in (nutrition.goalCalories - 250)..(nutrition.goalCalories + 250) &&
                        (summary.breakfastList.isNotEmpty() || summary.lunchList.isNotEmpty())
            val proteinBalancedMet = nutrition.protein >= (nutrition.goalProtein * 0.8)

            listOf(
                DailyMissionItem(
                    id = "calorie_balance",
                    title = context.getString(R.string.mission_calorie_balance_title),
                    description = context.getString(
                        R.string.mission_calorie_balance_desc,
                        nutrition.goalCalories
                    ),
                    currentProgressText = context.getString(
                        R.string.mission_calorie_deficit_progress,
                        nutrition.calories,
                        nutrition.goalCalories
                    ),
                    isCompleted = calorieBalanceMet,
                    progressFraction = (nutrition.calories.toFloat() / nutrition.goalCalories.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "water_hydration",
                    title = context.getString(R.string.mission_water_vitality_title),
                    description = context.getString(
                        R.string.mission_water_hydration_desc,
                        String.format(Locale.US, "%.1f", waterGoalMl / 1000.0)
                    ),
                    currentProgressText = waterProgressText,
                    isCompleted = waterAchieved,
                    progressFraction = (nutrition.water.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "protein_daily",
                    title = context.getString(R.string.mission_protein_daily_title),
                    description = context.getString(
                        R.string.mission_protein_preservation_desc,
                        nutrition.goalProtein
                    ),
                    currentProgressText = context.getString(
                        R.string.mission_protein_preservation_progress,
                        nutrition.protein.toInt(),
                        nutrition.goalProtein
                    ),
                    isCompleted = proteinBalancedMet,
                    progressFraction = (nutrition.protein.toFloat() / nutrition.goalProtein.coerceAtLeast(1)).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "exercise_daily",
                    title = context.getString(R.string.mission_exercise_daily_title),
                    description = context.getString(R.string.mission_exercise_daily_desc),
                    currentProgressText = exerciseProgressText,
                    isCompleted = exerciseAchieved,
                    progressFraction = (totalExerciseMin.toFloat() / 30f).coerceIn(0f, 1f)
                ),
                DailyMissionItem(
                    id = "vegetables_fiber",
                    title = context.getString(R.string.mission_vegetables_greens_title),
                    description = context.getString(R.string.mission_vegetables_greens_desc),
                    currentProgressText = if (hasVegetables) context.getString(R.string.mission_vegetables_fiber_logged) else context.getString(
                        R.string.mission_vegetables_fiber_add
                    ),
                    isCompleted = hasVegetables,
                    progressFraction = if (hasVegetables) 1f else 0f
                )
            )
        }
    }
}

private fun formatWaterSummary(context: Context, currentMl: Int, goalMl: Int): String {
    val currentL = currentMl / 1000.0
    val goalL = goalMl / 1000.0
    return context.getString(
        R.string.home_intake_water_format,
        String.format(Locale.US, "%.1f", currentL),
        String.format(Locale.US, "%.1f", goalL)
    )
}

private fun formatExerciseSummary(context: Context, totalMinutes: Int): String {
    return if (totalMinutes > 0) {
        context.getString(R.string.home_intake_exercise_format, totalMinutes)
    } else {
        context.getString(R.string.home_intake_not_logged)
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
