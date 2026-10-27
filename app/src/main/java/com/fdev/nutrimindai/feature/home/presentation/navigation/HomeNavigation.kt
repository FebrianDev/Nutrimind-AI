package com.fdev.nutrimindai.feature.home.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.fdev.nutrimindai.core.navigation.NavigationDestination
import com.fdev.nutrimindai.feature.home.presentation.HomeScreen

fun NavGraphBuilder.homeScreen(
    onNavigateToMealDetail: (String) -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToExercise: () -> Unit,
    onNavigateToDailyReview: (String) -> Unit = {}
) {
    composable<NavigationDestination.Home> {
        HomeScreen(
            onNavigateToMealDetail = onNavigateToMealDetail,
            onNavigateToWater = onNavigateToWater,
            onNavigateToExercise = onNavigateToExercise,
            onNavigateToDailyReview = onNavigateToDailyReview
        )
    }
}
