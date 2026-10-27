package com.fdev.nutrimindai.core.util

import kotlin.math.roundToInt

/**
 * Standard Metabolic Equivalent of Task (MET) categories based on the
 * Compendium of Physical Activities (Ainsworth et al., Stanford University / ACSM).
 *
 * 1 MET represents the energy expenditure of sitting quietly (~1 kcal/kg/hour).
 */
enum class ExerciseCategory(
    val id: String,
    val defaultName: String,
    val metValue: Float,
    val intensity: String
) {
    RUNNING("running", "Running", 8.3f, "Vigorous (~8.5 km/h)"),
    WALKING("walking", "Walking", 3.5f, "Moderate (~4.8 km/h)"),
    CYCLING("cycling", "Cycling", 6.8f, "Moderate (~16-19 km/h)"),
    GYM("gym", "Gym / Resistance Training", 5.0f, "Moderate to High Effort"),
    SWIMMING("swimming", "Swimming", 7.0f, "Moderate Freestyle Laps"),
    YOGA("yoga", "Yoga & Stretching", 2.5f, "Light to Moderate Effort"),
    OTHER("other", "General Workout", 4.5f, "Moderate Aerobic Exercise");

    companion object {
        fun fromType(type: String): ExerciseCategory {
            val query = type.trim().lowercase()
            return when {
                query.contains("run") || query.contains("jog") || query.contains("sprint") -> RUNNING
                query.contains("walk") || query.contains("hike") || query.contains("stroll") -> WALKING
                query.contains("cycl") || query.contains("bike") || query.contains("sepeda") -> CYCLING
                query.contains("gym") || query.contains("weight") || query.contains("fitness") || query.contains("angkat") -> GYM
                query.contains("swim") || query.contains("renang") -> SWIMMING
                query.contains("yoga") || query.contains("stretch") || query.contains("pilates") -> YOGA
                else -> OTHER
            }
        }
    }
}

/**
 * Utility for scientifically calculating calories burned based on body weight,
 * exercise duration, and Metabolic Equivalent of Task (MET).
 */
object MetExerciseCalculator {

    const val DEFAULT_WEIGHT_KG = 70.0f

    /**
     * Calculates calories burned using the standard sports science formula:
     * Calories = MET * Weight(kg) * Duration(hours)
     *
     * @param exerciseType String identifier (e.g. "running", "walking", "cycling", "gym").
     * @param durationMinutes Duration of the exercise in minutes.
     * @param weightKg User body weight in kilograms. Defaults to 70kg if unspecified or <= 0.
     * @return Calories burned in kcal (rounded to nearest integer).
     */
    fun calculateCalories(
        exerciseType: String,
        durationMinutes: Int,
        weightKg: Float = DEFAULT_WEIGHT_KG
    ): Int {
        if (durationMinutes <= 0) return 0
        val safeWeight = if (weightKg > 20f) weightKg else DEFAULT_WEIGHT_KG
        val category = ExerciseCategory.fromType(exerciseType)
        val hours = durationMinutes / 60.0
        val calories = category.metValue * safeWeight * hours
        return calories.roundToInt().coerceAtLeast(1)
    }

    /**
     * Retrieves the MET value for a given exercise type string.
     */
    fun getMetValue(exerciseType: String): Float {
        return ExerciseCategory.fromType(exerciseType).metValue
    }
}
