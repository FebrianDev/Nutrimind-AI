package com.fdev.nutrimindai.core.util

import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun `calculateDynamicWaterTarget computes correct base hydration for 70kg sedentary`() {
        // 70 kg * 35 ml = 2450 ml
        val water = NutritionCalculator.calculateDynamicWaterTarget(weightKg = 70.0f, exerciseMinutes = 0)
        assertEquals(2450, water)
    }

    @Test
    fun `calculateDynamicWaterTarget adds exercise compensation correctly`() {
        // 70 kg * 35 ml = 2450 ml
        // 60 minutes exercise -> (60 / 30) * 350 = +700 ml
        // Total = 3150 ml
        val water = NutritionCalculator.calculateDynamicWaterTarget(weightKg = 70.0f, exerciseMinutes = 60)
        assertEquals(3150, water)
    }

    @Test
    fun `calculateDynamicWaterTarget bounds water within safe range`() {
        // Very low weight
        val minWater = NutritionCalculator.calculateDynamicWaterTarget(weightKg = 10.0f, exerciseMinutes = 0)
        assertTrue(minWater >= 1500)

        // Extremely high weight + long exercise
        val maxWater = NutritionCalculator.calculateDynamicWaterTarget(weightKg = 180.0f, exerciseMinutes = 300)
        assertTrue(maxWater <= 5000)
    }

    @Test
    fun `calculatePlan includes calculated dynamic water target`() {
        val plan = NutritionCalculator.calculatePlan(
            age = 25,
            gender = Gender.Male,
            height = 175,
            currentWeight = 70.0f,
            targetWeight = 65.0f,
            activityLevel = ActivityLevel.ModeratelyActive,
            goal = Goal.LoseWeight
        )

        // 45 min exercise -> (45 / 30) * 350 = 525 ml. Base 2450 + 525 = 2975 ml
        assertEquals(2975, plan.dailyWaterMl)
    }
}
