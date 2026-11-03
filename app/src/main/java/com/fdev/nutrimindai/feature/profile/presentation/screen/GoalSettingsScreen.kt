package com.fdev.nutrimindai.feature.profile.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.fdev.nutrimindai.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@Composable
fun GoalSettingsScreen(
    viewModel: GoalSettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditGoalAndTargetsModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collectLatest {
            onBackClick()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- Top Bar ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = stringResource(R.string.goal_settings_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    // --- Content Body ---
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .screenPadding()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- Goal Inconsistency Warning (if any) ---
                        AnimatedVisibility(
                            visible = uiState.goalWarningText != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            uiState.goalWarningText?.let { warningText ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.cardPadding(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = warningText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // --- YOUR GOAL Section ---
                        Text(
                            text = stringResource(R.string.goal_settings_section_your_goal),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEditGoalAndTargetsModal = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.cardPadding()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                             .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getGoalIcon(uiState.goal),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = getGoalTitle(uiState.goal),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = getGoalDescription(uiState.goal),
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = stringResource(R.string.goal_settings_edit_goal),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(14.dp))

                                // 3 Sub-Metrics (Real & Computed)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    RowMetric(
                                        icon = Icons.Default.FitnessCenter,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        label = stringResource(R.string.goal_settings_current),
                                        value = "${uiState.currentWeightText} kg"
                                    )
                                    VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    RowMetric(
                                        icon = Icons.Default.TrackChanges,
                                        iconTint = MaterialTheme.colorScheme.primary,
                                        label = stringResource(R.string.goal_settings_target),
                                        value = "${uiState.targetWeightText} " + stringResource(R.string.unit_kg)
                                    )
                                    VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    RowMetric(
                                        icon = Icons.Default.CalendarToday,
                                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = stringResource(R.string.goal_settings_est_time),
                                        value = uiState.calculatedPlan?.estimatedTime?.replace("Around ", "") ?: "-"
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Progress to Goal Block (Calculated dynamically)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.goal_settings_progress_to_goal),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "${uiState.progressPercent}%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { uiState.progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = getMotivationalMessage(uiState.progressRatio, uiState.goal),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        // --- NUTRITION TARGET (DAILY) Section ---
                        Text(
                            text = stringResource(R.string.goal_settings_nutrition_target),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )

                        val plan = uiState.calculatedPlan
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                // Macro breakdown visual bar
                                val protein = plan?.dailyProtein ?: 120
                                val carbs = plan?.dailyCarbs ?: 250
                                val fat = plan?.dailyFat ?: 70
                                val totalMacroGrams = (protein + carbs + fat).coerceAtLeast(1).toFloat()

                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text(
                                        text = stringResource(R.string.goal_settings_macro_distribution),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight((protein / totalMacroGrams).coerceAtLeast(0.05f))
                                                .fillMaxHeight()
                                                .background(NutritionProtein)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight((carbs / totalMacroGrams).coerceAtLeast(0.05f))
                                                .fillMaxHeight()
                                                .background(NutritionCarbs)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight((fat / totalMacroGrams).coerceAtLeast(0.05f))
                                                .fillMaxHeight()
                                                .background(NutritionFat)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                NutritionRow(
                                    icon = Icons.Default.LocalFireDepartment,
                                    iconBg = NutritionCalories.copy(alpha = 0.15f),
                                    iconTint = NutritionCalories,
                                    label = stringResource(R.string.home_label_calories),
                                    value = "${plan?.dailyCalories ?: 2100} kcal",
                                    badge = stringResource(R.string.goal_settings_badge_energy),
                                    badgeColor = NutritionCalories
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                NutritionRow(
                                    icon = Icons.Default.Shield,
                                    iconBg = NutritionProtein.copy(alpha = 0.15f),
                                    iconTint = NutritionProtein,
                                    label = stringResource(R.string.label_protein),
                                    value = "${plan?.dailyProtein ?: 120} g",
                                    badge = "30%",
                                    badgeColor = NutritionProtein
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                NutritionRow(
                                    icon = Icons.Default.Grain,
                                    iconBg = NutritionCarbs.copy(alpha = 0.15f),
                                    iconTint = NutritionCarbs,
                                    label = stringResource(R.string.label_carbs),
                                    value = "${plan?.dailyCarbs ?: 250} g",
                                    badge = "45%",
                                    badgeColor = NutritionCarbs
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                NutritionRow(
                                    icon = Icons.Default.Opacity,
                                    iconBg = NutritionFat.copy(alpha = 0.15f),
                                    iconTint = NutritionFat,
                                    label = stringResource(R.string.label_fat),
                                    value = "${plan?.dailyFat ?: 70} g",
                                    badge = "25%",
                                    badgeColor = NutritionFat
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                NutritionRow(
                                    icon = Icons.Default.WaterDrop,
                                    iconBg = NutritionWater.copy(alpha = 0.15f),
                                    iconTint = NutritionWater,
                                    label = stringResource(R.string.goal_settings_water_target),
                                    value = "${String.format(Locale.US, "%.1f", uiState.dailyWaterLiters)} L",
                                    badge = stringResource(R.string.goal_settings_badge_hydration),
                                    badgeColor = NutritionWater
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                NutritionRow(
                                    icon = Icons.Default.Eco,
                                    iconBg = NutritionFiber.copy(alpha = 0.15f),
                                    iconTint = NutritionFiber,
                                    label = stringResource(R.string.goal_settings_dietary_fiber),
                                    value = "${uiState.dailyFiberGrams} g",
                                    badge = stringResource(R.string.goal_settings_badge_digestion),
                                    badgeColor = NutritionFiber
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                NutritionRow(
                                    icon = Icons.Default.FitnessCenter,
                                    iconBg = NutritionExercise.copy(alpha = 0.15f),
                                    iconTint = NutritionExercise,
                                    label = stringResource(R.string.goal_settings_daily_exercise),
                                    value = "${plan?.dailyExerciseMinutes ?: 30} mins",
                                    badge = stringResource(R.string.goal_settings_badge_activity),
                                    badgeColor = NutritionExercise
                                )
                            }
                        }

                        // --- ABOUT YOUR GOAL Section ---
                        Text(
                            text = stringResource(R.string.goal_settings_about_goal),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                AboutRow(
                                    icon = Icons.Default.AccessTime,
                                    title = stringResource(R.string.goal_settings_why_goal),
                                    subtitle = getGoalWhyText(uiState.goal)
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                AboutRow(
                                    icon = Icons.Default.CalendarToday,
                                    title = stringResource(R.string.goal_settings_est_target_date),
                                    subtitle = uiState.targetDateFormatted
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                AboutRow(
                                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                    title = stringResource(R.string.goal_settings_activity_level),
                                    subtitle = "${getActivityLevelTitle(uiState.activityLevel)} • ${getActivityLevelDescription(uiState.activityLevel)}"
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.screenPadding())

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showEditGoalAndTargetsModal = true }
                                        .cardPadding(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.goal_settings_adjust_targets),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = stringResource(R.string.goal_settings_adjust_desc),
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // --- AI Personalized Coach Banner Card ---
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.cardPadding(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.goal_settings_ai_coach),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = getGoalAiTip(uiState.goal),
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // --- Save Changes Button ---
                        Button(
                            onClick = { viewModel.saveGoalSettings() },
                            enabled = !uiState.isSaving && uiState.isInputValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.goal_settings_saving),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.goal_settings_save_btn),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        if (!uiState.isWeightGoalValid) {
                            uiState.goalWarningText?.let { warning ->
                                Text(
                                    text = warning,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // --- Unified Modal: Edit Goal & Adjust Body Stats & Targets ---
    if (showEditGoalAndTargetsModal) {
        AlertDialog(
            onDismissRequest = { showEditGoalAndTargetsModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.goal_settings_configure_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: Primary Goal Selection
                    Text(
                        text = stringResource(R.string.goal_settings_primary_goal),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Goal.entries.forEach { goal ->
                            val isSelected = uiState.goal == goal
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onGoalChanged(goal) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getGoalIcon(goal),
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = getGoalTitle(goal),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = getGoalDescription(goal),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = stringResource(R.string.cd_selected),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Section 2: Basic Body Stats
                    Text(
                        text = stringResource(R.string.goal_settings_body_stats),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.ageText,
                            onValueChange = { viewModel.onAgeChanged(it) },
                            label = { Text(stringResource(R.string.label_age)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = uiState.heightText,
                            onValueChange = { viewModel.onHeightChanged(it) },
                            label = { Text(stringResource(R.string.label_height)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    // Gender Selector
                    Text(
                        text = stringResource(R.string.label_gender),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.entries.forEach { gender ->
                            FilterChip(
                                selected = uiState.gender == gender,
                                onClick = { viewModel.onGenderChanged(gender) },
                                label = {
                                    Text(
                                        text = when (gender) {
                                            Gender.Male -> stringResource(R.string.gender_male)
                                            Gender.Female -> stringResource(R.string.gender_female)
                                            Gender.PreferNotToSay -> stringResource(R.string.gender_other)
                                        },
                                        fontWeight = if (uiState.gender == gender) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Section 3: Weight Targets
                    Text(
                        text = stringResource(R.string.goal_settings_weight_targets),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.currentWeightText,
                            onValueChange = { viewModel.onCurrentWeightChanged(it) },
                            label = { Text(stringResource(R.string.label_weight)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = uiState.targetWeightText,
                            onValueChange = { viewModel.onTargetWeightChanged(it) },
                            label = { Text(stringResource(R.string.label_target_weight)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    uiState.goalWarningText?.let { warningText ->
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Section 4: Activity Level
                    Text(
                        text = stringResource(R.string.goal_settings_activity_level_header),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityLevel.entries.forEach { level ->
                            val isSelected = uiState.activityLevel == level
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onActivityLevelChanged(level) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = getActivityLevelTitle(level),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = getActivityLevelDescription(level),
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 5: Live Calculated Preview
                    val previewPlan = uiState.calculatedPlan
                    if (previewPlan != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.goal_settings_daily_calorie_target),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    )
                                    Text(
                                        text = stringResource(R.string.home_intake_kcal_val, previewPlan.dailyCalories),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.goal_settings_est_time),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    )
                                    Text(
                                        text = previewPlan.estimatedTime,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEditGoalAndTargetsModal = false },
                    enabled = uiState.isInputValid,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.btn_apply_changes), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalAndTargetsModal = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@Composable
private fun RowMetric(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun NutritionRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.badgePadding()
                )
            }
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

private fun getGoalIcon(goal: Goal): ImageVector {
    return when (goal) {
        Goal.LoseWeight -> Icons.AutoMirrored.Filled.TrendingDown
        Goal.MaintainWeight -> Icons.AutoMirrored.Filled.TrendingFlat
        Goal.GainWeight -> Icons.AutoMirrored.Filled.TrendingUp
    }
}

@Composable
private fun getGoalTitle(goal: Goal): String {
    return stringResource(
        when (goal) {
            Goal.LoseWeight -> R.string.goal_lose_title
            Goal.MaintainWeight -> R.string.goal_maintain_title
            Goal.GainWeight -> R.string.goal_gain_title
        }
    )
}

@Composable
private fun getGoalDescription(goal: Goal): String {
    return stringResource(
        when (goal) {
            Goal.LoseWeight -> R.string.goal_settings_desc_lose
            Goal.MaintainWeight -> R.string.goal_settings_desc_maintain
            Goal.GainWeight -> R.string.goal_settings_desc_gain
        }
    )
}

@Composable
private fun getGoalWhyText(goal: Goal): String {
    return stringResource(
        when (goal) {
            Goal.LoseWeight -> R.string.goal_settings_why_lose
            Goal.MaintainWeight -> R.string.goal_settings_why_maintain
            Goal.GainWeight -> R.string.goal_settings_why_gain
        }
    )
}

@Composable
private fun getGoalAiTip(goal: Goal): String {
    return stringResource(
        when (goal) {
            Goal.LoseWeight -> R.string.goal_settings_tip_lose
            Goal.MaintainWeight -> R.string.goal_settings_tip_maintain
            Goal.GainWeight -> R.string.goal_settings_tip_gain
        }
    )
}

@Composable
private fun getActivityLevelTitle(level: ActivityLevel): String {
    return stringResource(
        when (level) {
            ActivityLevel.Sedentary -> R.string.activity_sedentary_title
            ActivityLevel.LightlyActive -> R.string.activity_lightly_title
            ActivityLevel.ModeratelyActive -> R.string.activity_moderately_title
            ActivityLevel.VeryActive -> R.string.activity_very_title
        }
    )
}

@Composable
private fun getActivityLevelDescription(level: ActivityLevel): String {
    return stringResource(
        when (level) {
            ActivityLevel.Sedentary -> R.string.goal_settings_activity_sedentary_desc
            ActivityLevel.LightlyActive -> R.string.goal_settings_activity_lightly_desc
            ActivityLevel.ModeratelyActive -> R.string.goal_settings_activity_moderately_desc
            ActivityLevel.VeryActive -> R.string.goal_settings_activity_very_desc
        }
    )
}

@Composable
private fun getMotivationalMessage(progressRatio: Float, goal: Goal): String {
    if (goal == Goal.MaintainWeight) {
        return stringResource(R.string.goal_settings_motivational_maintain)
    }
    return stringResource(
        when {
            progressRatio >= 1.0f -> R.string.goal_settings_motivational_reached
            progressRatio >= 0.75f -> R.string.goal_settings_motivational_almost
            progressRatio >= 0.40f -> R.string.goal_settings_motivational_momentum
            else -> R.string.goal_settings_motivational_start
        }
    )
}


