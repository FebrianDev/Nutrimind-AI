package com.fdev.nutrimindai.feature.onboarding.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fdev.nutrimindai.core.navigation.NavigationDestination
import com.fdev.nutrimindai.feature.onboarding.presentation.screen.OnboardingScreenContainer

/**
 * Extension function registering the Onboarding feature navigation destination within Navigation Compose.
 *
 * @param onOnboardingComplete Callback invoked when onboarding flow finishes to navigate to Home destination.
 */
fun NavGraphBuilder.onboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    composable<NavigationDestination.Onboarding> {
        OnboardingScreenContainer(
            onOnboardingComplete = onOnboardingComplete
        )
    }
}
