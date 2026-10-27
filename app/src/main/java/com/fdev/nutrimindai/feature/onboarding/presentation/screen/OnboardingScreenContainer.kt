package com.fdev.nutrimindai.feature.onboarding.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.feature.onboarding.presentation.component.BottomCTA
import com.fdev.nutrimindai.feature.onboarding.presentation.component.OnboardingTopBar
import com.fdev.nutrimindai.feature.onboarding.presentation.component.PrimaryButton
import com.fdev.nutrimindai.feature.onboarding.presentation.viewmodel.OnboardingViewModel

/**
 * Host container composable managing onboarding step navigation, animated transitions, top bar header,
 * bottom call-to-action button, and completion callback.
 *
 * @param onOnboardingComplete Callback invoked when onboarding completes to navigate to Home.
 * @param modifier Custom modifier.
 * @param viewModel Injected [OnboardingViewModel].
 */
@Composable
fun OnboardingScreenContainer(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.currentStep > 1 && !uiState.isCompleted) {
        viewModel.onBackClicked()
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingTopBar(
                currentStep = uiState.currentStep,
                showBackButton = uiState.currentStep > 1 && !uiState.isLoadingStep5,
                onBackClick = { viewModel.onBackClicked() }
            )
        },
        bottomBar = {
            val isCurrentStepValid = when (uiState.currentStep) {
                1 -> uiState.isStep1Valid
                2 -> uiState.isStep2Valid
                3 -> uiState.isStep3Valid
                4 -> uiState.isStep4Valid
                5 -> !uiState.isLoadingStep5
                else -> false
            }

            val buttonText = if (uiState.currentStep == 5) {
                stringResource(id = R.string.btn_start_journey)
            } else {
                stringResource(id = R.string.btn_continue)
            }

            if (!(uiState.currentStep == 5 && uiState.isLoadingStep5)) {
                BottomCTA {
                    PrimaryButton(
                        text = buttonText,
                        onClick = {
                            if (uiState.currentStep == 5) {
                                viewModel.onStartJourneyClicked()
                            } else {
                                viewModel.onNextClicked()
                            }
                        },
                        enabled = isCurrentStepValid
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "onboardingStepTransition"
            ) { targetStep ->
                when (targetStep) {
                    1 -> Step1PersonalInformationScreen(
                        state = uiState,
                        onNameChange = viewModel::onNameChanged,
                        onAgeChange = viewModel::onAgeChanged,
                        onGenderChange = viewModel::onGenderChanged,
                        onHeightChange = viewModel::onHeightChanged,
                        onWeightChange = viewModel::onWeightChanged
                    )
                    2 -> Step2ActivityLevelScreen(
                        state = uiState,
                        onActivitySelected = viewModel::onActivityLevelSelected
                    )
                    3 -> Step3GoalScreen(
                        state = uiState,
                        onGoalSelected = viewModel::onGoalSelected
                    )
                    4 -> Step4TargetWeightScreen(
                        state = uiState,
                        onTargetWeightChange = viewModel::onTargetWeightChanged
                    )
                    5 -> Step5PlanSummaryScreen(
                        state = uiState
                    )
                }
            }
        }
    }
}
