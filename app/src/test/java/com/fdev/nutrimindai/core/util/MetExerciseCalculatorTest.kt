package com.fdev.nutrimindai.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetExerciseCalculatorTest {

    @Test
    fun `calculateCalories computes correct burn for Running 30 minutes at 70kg`() {
        // 8.3 MET * 70 kg * 0.5 hours = 290.5 kcal -> 291 kcal
        val calories = MetExerciseCalculator.calculateCalories(
            exerciseType = "running",
            durationMinutes = 30,
            weightKg = 70.0f
        )
        assertEquals(291, calories)
    }

    @Test
    fun `calculateCalories computes correct burn for Walking 60 minutes at 70kg`() {
        // 3.5 MET * 70 kg * 1.0 hours = 245.0 kcal -> 245 kcal
        val calories = MetExerciseCalculator.calculateCalories(
            exerciseType = "walking",
            durationMinutes = 60,
            weightKg = 70.0f
        )
        assertEquals(245, calories)
    }

    @Test
    fun `calculateCalories computes correct burn for Cycling 60 minutes at 80kg`() {
        // 6.8 MET * 80 kg * 1.0 hours = 544 kcal
        val calories = MetExerciseCalculator.calculateCalories(
            exerciseType = "cycling",
            durationMinutes = 60,
            weightKg = 80.0f
        )
        assertEquals(544, calories)
    }

    @Test
    fun `calculateCalories computes correct burn for Gym workout 45 minutes at 70kg`() {
        // 5.0 MET * 70 kg * 0.75 hours = 262.5 kcal -> 263 kcal
        val calories = MetExerciseCalculator.calculateCalories(
            exerciseType = "gym",
            durationMinutes = 45,
            weightKg = 70.0f
        )
        assertEquals(263, calories)
    }

    @Test
    fun `calculateCalories returns zero for non-positive duration`() {
        assertEquals(0, MetExerciseCalculator.calculateCalories("running", 0, 70f))
        assertEquals(0, MetExerciseCalculator.calculateCalories("running", -15, 70f))
    }

    @Test
    fun `calculateCalories falls back to default weight when weight is invalid`() {
        // Default weight: 70kg. Running 30 min -> 291 kcal
        val calories = MetExerciseCalculator.calculateCalories("running", 30, 0f)
        assertEquals(291, calories)
    }

    @Test
    fun `ExerciseCategory parses localized and colloquial keywords`() {
        assertEquals(ExerciseCategory.RUNNING, ExerciseCategory.fromType("jogging pagi"))
        assertEquals(ExerciseCategory.CYCLING, ExerciseCategory.fromType("gowes sepeda santai"))
        assertEquals(ExerciseCategory.SWIMMING, ExerciseCategory.fromType("berenang"))
        assertEquals(ExerciseCategory.YOGA, ExerciseCategory.fromType("pilates session"))
        assertEquals(ExerciseCategory.OTHER, ExerciseCategory.fromType("unknown activity"))
    }
}
