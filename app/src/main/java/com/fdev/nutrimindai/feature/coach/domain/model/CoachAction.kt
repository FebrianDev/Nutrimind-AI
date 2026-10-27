package com.fdev.nutrimindai.feature.coach.domain.model

import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType

/**
 * Domain model representing an agentic action requested by NutriCoach AI.
 */
sealed interface CoachAction {
    /**
     * Action to automatically log food/meal into the user's daily diary.
     */
    data class LogFood(
        val foodName: String,
        val mealType: MealType = MealType.OTHERS,
        val calories: Int,
        val protein: Double = 0.0,
        val carbs: Double = 0.0,
        val fat: Double = 0.0,
        val fiber: Double = 0.0,
        val servingSize: Double = 1.0,
        val servingUnit: String = "portion"
    ) : CoachAction

    /**
     * Action to automatically log water intake into the user's daily diary.
     */
    data class LogWater(
        val amountMl: Int
    ) : CoachAction

    /**
     * Action to automatically log workout/physical exercise into the user's daily diary.
     */
    data class LogExercise(
        val exerciseType: String,
        val durationMinute: Int,
        val caloriesBurned: Int,
        val note: String? = null
    ) : CoachAction

    /**
     * Action to automatically log a new body weight measurement.
     */
    data class LogWeight(
        val weightKg: Float,
        val note: String? = null
    ) : CoachAction
}

/**
 * Result of parsing and executing an AI coach response containing potential agentic actions.
 */
data class CoachExecutionResult(
    val cleanResponseText: String,
    val executedActions: List<CoachAction> = emptyList(),
    val actionSummary: String? = null
)
