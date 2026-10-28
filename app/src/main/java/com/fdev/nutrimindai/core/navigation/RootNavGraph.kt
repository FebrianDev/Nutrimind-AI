package com.fdev.nutrimindai.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.fdev.nutrimindai.feature.auth.presentation.login.LoginScreen
import com.fdev.nutrimindai.feature.auth.presentation.register.RegisterScreen
import com.fdev.nutrimindai.feature.onboarding.presentation.navigation.onboardingScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.navigation.dailyIntakeGraph
import com.fdev.nutrimindai.feature.home.presentation.navigation.homeScreen
import com.fdev.nutrimindai.feature.progress.presentation.screen.ProgressScreen
import com.fdev.nutrimindai.feature.progress.presentation.screen.TrendDetailScreen
import com.fdev.nutrimindai.feature.profile.presentation.screen.GoalSettingsScreen
import com.fdev.nutrimindai.feature.profile.presentation.screen.ProfileScreen
import com.fdev.nutrimindai.feature.activity.presentation.screen.ActivityScreen
import com.fdev.nutrimindai.feature.activity.presentation.screen.ActivityDetailScreen
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.fdev.nutrimindai.feature.coach.presentation.CoachScreen

/**
 * Root Navigation Graph of NutriMind AI app setting up navigation routes.
 *
 * @param navController Controller managing app navigation state.
 * @param startDestination The initial destination to show.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RootNavGraph(
    navController: NavHostController,
    startDestination: NavigationDestination = NavigationDestination.Login
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarDestinations = listOf(
        NavigationDestination.Home::class,
        NavigationDestination.Tracking::class,
        NavigationDestination.Coach::class,
        NavigationDestination.Insight::class,
        NavigationDestination.Profile::class
    )

    val isImeVisible = WindowInsets.isImeVisible
    val shouldShowBottomBar = bottomBarDestinations.any { currentDestination?.hierarchy?.any { d -> d.hasRoute(it) } == true } && !isImeVisible

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NutriMindBottomBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // --- Authentication Routes ---
            composable<NavigationDestination.Login> {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(NavigationDestination.Register)
                    },
                    onNavigateToHome = {
                        navController.navigate(NavigationDestination.Home) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(NavigationDestination.Onboarding) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable<NavigationDestination.Register> {
                RegisterScreen(
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(NavigationDestination.Onboarding) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // --- Onboarding Flow ---
            onboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(NavigationDestination.Home) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )

            // --- Main Home Flow ---
            homeScreen(
                onNavigateToMealDetail = { mealType ->
                    navController.navigate(NavigationDestination.MealDetail(mealType))
                },
                onNavigateToWater = {
                    navController.navigate(NavigationDestination.Water)
                },
                onNavigateToExercise = {
                    navController.navigate(NavigationDestination.Exercise)
                },
                onNavigateToDailyReview = { date ->
                    navController.navigate(NavigationDestination.ActivityDetail(date))
                }
            )

            // --- NutriCoach AI Tab ---
            composable<NavigationDestination.Coach> { backStackEntry ->
                val route = backStackEntry.toRoute<NavigationDestination.Coach>()
                CoachScreen(
                    initialPrompt = route.initialPrompt
                )
            }

            dailyIntakeGraph(navController)

            // --- Profile Tab ---
            composable<NavigationDestination.Profile> {
                ProfileScreen(
                    onNavigateToGoalSettings = {
                        navController.navigate(NavigationDestination.GoalSettings)
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(NavigationDestination.Login) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    },
                    onNavigateToReminderSettings = {
                        navController.navigate(NavigationDestination.ReminderSettings)
                    }
                )
            }

            composable<NavigationDestination.ReminderSettings> {
                com.fdev.nutrimindai.feature.profile.presentation.screen.ReminderSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // --- Progress & Analytics Tab ---
            composable<NavigationDestination.Tracking> {
                ProgressScreen(
                    onNavigateToTrendDetail = { metric ->
                        navController.navigate(NavigationDestination.TrendDetail(metric))
                    },
                    onNavigateToGoalSettings = {
                        navController.navigate(NavigationDestination.GoalSettings)
                    }
                )
            }
            composable<NavigationDestination.DailyReview> { backStackEntry ->
                val route = backStackEntry.toRoute<NavigationDestination.DailyReview>()
                ActivityDetailScreen(
                    date = route.date,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<NavigationDestination.TrendDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<NavigationDestination.TrendDetail>()
                TrendDetailScreen(
                    metric = route.metric,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<NavigationDestination.GoalSettings> {
                GoalSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // --- Activity / History Tab ---
            composable<NavigationDestination.ActivityDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<NavigationDestination.ActivityDetail>()
                ActivityDetailScreen(
                    date = route.date,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<NavigationDestination.Insight> {
                ActivityScreen(
                    onNavigateToDetail = { date ->
                        navController.navigate(NavigationDestination.ActivityDetail(date))
                    }
                )
            }
        }
    }
}

