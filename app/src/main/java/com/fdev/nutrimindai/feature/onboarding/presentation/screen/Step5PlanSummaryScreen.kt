package com.fdev.nutrimindai.feature.onboarding.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.feature.onboarding.presentation.component.HeroEnergyCard
import com.fdev.nutrimindai.feature.onboarding.presentation.component.MacroNutrientCard
import com.fdev.nutrimindai.feature.onboarding.presentation.component.RecommendationCard
import com.fdev.nutrimindai.feature.onboarding.presentation.state.OnboardingUiState

/**
 * Step 5 Onboarding Screen: Displays 2-second loading animation sequence and personalized daily nutrition plan summary.
 *
 * @param state Current UI state.
 * @param modifier Custom modifier.
 */
@Composable
fun Step5PlanSummaryScreen(
    state: OnboardingUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Loading State Animation (~2 seconds)
        AnimatedVisibility(
            visible = state.isLoadingStep5,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = state.loadingStep5Text.ifEmpty { stringResource(id = R.string.step5_loading_1) },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Plan Summary Content once loaded
        AnimatedVisibility(
            visible = !state.isLoadingStep5,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.step5_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.step5_subtitle),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hero Calories Card
                HeroEnergyCard(calories = state.calculatedCalories)

                Spacer(modifier = Modifier.height(16.dp))

                // 2x2 Grid for Macros & Activity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MacroNutrientCard(
                        title = stringResource(id = R.string.label_protein),
                        value = state.calculatedProtein,
                        unit = stringResource(id = R.string.unit_grams),
                        icon = Icons.Default.WaterDrop,
                        modifier = Modifier.weight(1f)
                    )
                    MacroNutrientCard(
                        title = stringResource(id = R.string.label_carbs),
                        value = state.calculatedCarbs,
                        unit = stringResource(id = R.string.unit_grams),
                        icon = Icons.Default.Grain,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MacroNutrientCard(
                        title = stringResource(id = R.string.label_fats),
                        value = state.calculatedFat,
                        unit = stringResource(id = R.string.unit_grams),
                        icon = Icons.Default.WaterDrop,
                        modifier = Modifier.weight(1f)
                    )
                    MacroNutrientCard(
                        title = stringResource(id = R.string.label_activity),
                        value = state.calculatedExerciseMinutes,
                        unit = stringResource(id = R.string.unit_minutes),
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // AI Recommendation Box
                RecommendationCard()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
