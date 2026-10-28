package com.fdev.nutrimindai.feature.dailyintake.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.fdev.nutrimindai.core.navigation.NavigationDestination
import com.fdev.nutrimindai.feature.dailyintake.presentation.add_food.AddFoodBottomSheet
import com.fdev.nutrimindai.feature.dailyintake.presentation.add_food.ManualEntryScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.add_food.SearchFoodScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.camera.CameraScanScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.exercise.AddExerciseScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.exercise.ExerciseScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.meal_detail.MealDetailScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.water.WaterScreen

fun NavGraphBuilder.dailyIntakeGraph(navController: NavController) {

    composable<NavigationDestination.MealDetail> { backStackEntry ->
        val mealDetail = backStackEntry.toRoute<NavigationDestination.MealDetail>()
        var showAddFoodBottomSheet by remember { mutableStateOf(false) }
        var showNutriChefBottomSheet by remember { mutableStateOf(false) }

        if (showAddFoodBottomSheet) {
            AddFoodBottomSheet(
                onDismiss = { showAddFoodBottomSheet = false },
                onNavigateToCamera = {
                    navController.navigate(NavigationDestination.CameraScan(mealDetail.mealType, launchGalleryDirectly = false))
                },
                onNavigateToBarcode = {
                    navController.navigate(NavigationDestination.CameraScan(mealDetail.mealType, launchGalleryDirectly = false, initialMode = "BARCODE"))
                },
                onNavigateToNutriChef = {
                    showNutriChefBottomSheet = true
                },
                onNavigateToGallery = {
                    navController.navigate(NavigationDestination.CameraScan(mealDetail.mealType, launchGalleryDirectly = true))
                },
                onNavigateToSearch = {
                    navController.navigate(NavigationDestination.SearchFood(mealDetail.mealType))
                },
                onNavigateToManual = {
                    navController.navigate(NavigationDestination.ManualEntry(mealDetail.mealType))
                }
            )
        }

        if (showNutriChefBottomSheet) {
            com.fdev.nutrimindai.feature.dailyintake.presentation.nutrichef.NutriChefBottomSheet(
                onDismiss = { showNutriChefBottomSheet = false }
            )
        }

        MealDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddFood = { showAddFoodBottomSheet = true },
            onNavigateToCoachIdeas = { prompt ->
                navController.navigate(NavigationDestination.Coach(prompt))
            }
        )
    }

    composable<NavigationDestination.CameraScan> { backStackEntry ->
        val args = backStackEntry.toRoute<NavigationDestination.CameraScan>()
        CameraScanScreen(
            mealType = args.mealType,
            launchGalleryDirectly = args.launchGalleryDirectly,
            initialMode = args.initialMode,
            onNavigateBack = { navController.popBackStack() }
        )
    }


    composable<NavigationDestination.SearchFood> { backStackEntry ->
        val args = backStackEntry.toRoute<NavigationDestination.SearchFood>()
        SearchFoodScreen(
            mealType = args.mealType,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToBarcode = {
                navController.navigate(NavigationDestination.CameraScan(args.mealType, initialMode = "BARCODE"))
            }
        )
    }

    composable<NavigationDestination.ManualEntry> { backStackEntry ->
        val args = backStackEntry.toRoute<NavigationDestination.ManualEntry>()
        ManualEntryScreen(
            mealType = args.mealType,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<NavigationDestination.Water> {
        WaterScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<NavigationDestination.Exercise> {
        ExerciseScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddExercise = { navController.navigate(NavigationDestination.AddExercise) }
        )
    }

    composable<NavigationDestination.AddExercise> {
        AddExerciseScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
