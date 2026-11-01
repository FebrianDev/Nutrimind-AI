package com.fdev.nutrimindai.core.navigation

import kotlinx.serialization.Serializable

sealed interface NavigationDestination {
    @Serializable
    data object Login : NavigationDestination

    @Serializable
    data object Register : NavigationDestination

    @Serializable
    data object Onboarding : NavigationDestination
    
    @Serializable
    data object Home : NavigationDestination
    
    @Serializable
    data object Profile : NavigationDestination
    
    @Serializable
    data object Tracking : NavigationDestination

    @Serializable
    data class DailyReview(val date: String) : NavigationDestination

    @Serializable
    data class TrendDetail(val metric: String) : NavigationDestination

    @Serializable
    data object DailyMissionHistory : NavigationDestination

    @Serializable
    data object GoalSettings : NavigationDestination

    @Serializable
    data object Insight : NavigationDestination

    @Serializable
    data class ActivityDetail(val date: String) : NavigationDestination

    @Serializable
    data class MealDetail(val mealType: String) : NavigationDestination

    @Serializable
    data object Water : NavigationDestination

    @Serializable
    data object Exercise : NavigationDestination

    @Serializable
    data object AddExercise : NavigationDestination

    @Serializable
    data class SearchFood(val mealType: String) : NavigationDestination

    @Serializable
    data class ManualEntry(val mealType: String) : NavigationDestination

    @Serializable
    data class CameraScan(
        val mealType: String,
        val launchGalleryDirectly: Boolean = false,
        val initialMode: String? = null
    ) : NavigationDestination

    @Serializable
    data object ReminderSettings : NavigationDestination

    @Serializable
    data class Coach(val initialPrompt: String? = null) : NavigationDestination
}

