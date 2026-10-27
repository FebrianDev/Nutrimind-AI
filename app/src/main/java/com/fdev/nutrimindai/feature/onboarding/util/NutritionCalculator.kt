package com.fdev.nutrimindai.feature.onboarding.util

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Result data holder representing calculated daily nutrition targets and physical activity goals.
 *
 * @property dailyCalories Total daily calorie target in kcal.
 * @property dailyProtein Total daily protein target in grams.
 * @property dailyCarbs Total daily carbohydrate target in grams.
 * @property dailyFat Total daily fat target in grams.
 * @property dailyExerciseMinutes Daily physical activity target in minutes.
 * @property estimatedTime Human-readable completion estimate for achieving target weight.
 */
data class NutritionPlanResult(
    val dailyCalories: Int,
    val dailyProtein: Int,
    val dailyCarbs: Int,
    val dailyFat: Int,
    val dailyExerciseMinutes: Int,
    val estimatedTime: String
)

/**
 * Domain utility for calculating basal metabolic rate (BMR), total daily energy expenditure (TDEE),
 * macronutrient split, and physical activity targets using scientific Mifflin-St Jeor formulas.
 */
object NutritionCalculator {

    private const val MINIMUM_CALORIES = 1200

    /**
     * Calculates personalized daily nutrition targets based on user metrics and goal selections.
     *
     * @param age User age in years.
     * @param gender User gender representation.
     * @param height User height in centimeters.
     * @param currentWeight User current weight in kilograms.
     * @param targetWeight User target weight in kilograms.
     * @param activityLevel User activity level.
     * @param goal User primary weight management goal.
     * @return [NutritionPlanResult] containing calculated calories, macros, exercise minutes, and estimated timeframe.
     */
    fun calculatePlan(
        age: Int,
        gender: Gender,
        height: Int,
        currentWeight: Float,
        targetWeight: Float,
        activityLevel: ActivityLevel,
        goal: Goal
    ): NutritionPlanResult {
        val bmr = calculateBmr(age, gender, height, currentWeight)
        val tdee = bmr * getActivityMultiplier(activityLevel)

        val targetCalories = when (goal) {
            Goal.LoseWeight -> (tdee - 500.0).coerceAtLeast(MINIMUM_CALORIES.toDouble())
            Goal.MaintainWeight -> tdee
            Goal.GainWeight -> tdee + 400.0
        }.roundToInt()

        val proteinGrams = ((targetCalories * 0.30) / 4.0).roundToInt()
        val carbsGrams = ((targetCalories * 0.45) / 4.0).roundToInt()
        val fatGrams = ((targetCalories * 0.25) / 9.0).roundToInt()

        val exerciseMinutes = when (activityLevel) {
            ActivityLevel.Sedentary -> 30
            ActivityLevel.LightlyActive -> 40
            ActivityLevel.ModeratelyActive -> 45
            ActivityLevel.VeryActive -> 60
        }

        val estimatedTime = calculateEstimatedTimeframe(currentWeight, targetWeight, goal)

        return NutritionPlanResult(
            dailyCalories = targetCalories,
            dailyProtein = proteinGrams,
            dailyCarbs = carbsGrams,
            dailyFat = fatGrams,
            dailyExerciseMinutes = exerciseMinutes,
            estimatedTime = estimatedTime
        )
    }

    /**
     * Mifflin-St Jeor BMR calculation formula.
     */
    private fun calculateBmr(
        age: Int,
        gender: Gender,
        height: Int,
        weight: Float
    ): Double {
        val baseBmr = 10.0 * weight + 6.25 * height - 5.0 * age
        return when (gender) {
            Gender.Male -> baseBmr + 5.0
            Gender.Female -> baseBmr - 161.0
            Gender.PreferNotToSay -> baseBmr - 78.0
        }
    }

    /**
     * Retrieves PAL (Physical Activity Level) multiplier.
     */
    private fun getActivityMultiplier(activityLevel: ActivityLevel): Double {
        return when (activityLevel) {
            ActivityLevel.Sedentary -> 1.2
            ActivityLevel.LightlyActive -> 1.375
            ActivityLevel.ModeratelyActive -> 1.55
            ActivityLevel.VeryActive -> 1.725
        }
    }

    /**
     * Computes estimated timeframe for achieving target weight based on healthy rate of change (~0.5kg per week).
     */
    fun calculateEstimatedTimeframe(
        currentWeight: Float,
        targetWeight: Float,
        goal: Goal
    ): String {
        if (goal == Goal.MaintainWeight) {
            return "Maintain current weight"
        }

        val weightDifference = abs(targetWeight - currentWeight)
        if (weightDifference < 0.1f) {
            return "Maintain current weight"
        }

        val weeks = (weightDifference / 0.5f).roundToInt().coerceAtLeast(1)
        return if (weeks < 4) {
            "Around $weeks ${if (weeks == 1) "week" else "weeks"}"
        } else {
            val months = (weeks / 4.0).roundToInt().coerceAtLeast(1)
            "Around $months ${if (months == 1) "month" else "months"}"
        }
    }

    /**
     * Calculates unified daily health score (0 - 100) based on calories, protein, water, and exercise targets.
     * Weights: Calories (40%), Protein (20%), Water (20%), Exercise (20%).
     * Returns 0 if no data has been logged yet for the day.
     */
    fun calculateHealthScore(
        cals: Int,
        targetCal: Int,
        protein: Double,
        targetProtein: Double,
        water: Int,
        targetWater: Int,
        exerciseMin: Int,
        targetExerciseMin: Int
    ): Int {
        if (cals == 0 && water == 0 && exerciseMin == 0 && protein == 0.0) {
            return 0
        }

        val safeTargetCal = if (targetCal > 0) targetCal else 2200
        val safeTargetProtein = if (targetProtein > 0.0) targetProtein else 140.0
        val safeTargetWater = if (targetWater > 0) targetWater else 2500
        val safeTargetExercise = if (targetExerciseMin > 0) targetExerciseMin else 30

        val calScore = (1.0f - (abs(cals - safeTargetCal).toFloat() / safeTargetCal)).coerceIn(0f, 1f) * 40f
        val proteinScore = (protein.toFloat() / safeTargetProtein.toFloat()).coerceIn(0f, 1f) * 20f
        val waterScore = (water.toFloat() / safeTargetWater.toFloat()).coerceIn(0f, 1f) * 20f
        val exerciseScore = (exerciseMin.toFloat() / safeTargetExercise.toFloat()).coerceIn(0f, 1f) * 20f

        return (calScore + proteinScore + waterScore + exerciseScore).roundToInt().coerceIn(0, 100)
    }
}
