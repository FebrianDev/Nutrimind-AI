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

    @Test
    fun `calculateProjectedWeight correctly calculates weight change based on calorie deficit and surplus`() {
        // Anchor weight 70.0 kg, net calorie deficit of 3850 kcal -> -0.5 kg -> 69.5 kg
        val deficitWeight = NutritionCalculator.calculateProjectedWeight(
            anchorWeightKg = 70.0f,
            netCalorieBalance = -3850.0
        )
        assertEquals(69.5f, deficitWeight, 0.05f)

        // Net calorie surplus of 3850 kcal -> +0.5 kg -> 70.5 kg
        val surplusWeight = NutritionCalculator.calculateProjectedWeight(
            anchorWeightKg = 70.0f,
            netCalorieBalance = 3850.0
        )
        assertEquals(70.5f, surplusWeight, 0.05f)
    }

    @Test
    fun `evaluateMetabolicCalibration produces high accuracy result when deviation is within 0_3 kg`() {
        val result = NutritionCalculator.evaluateMetabolicCalibration(
            actualWeight = 69.8f,
            projectedWeight = 70.0f
        )
        assertTrue(result.isAccurate)
        assertEquals(-0.2f, result.deviationKg, 0.01f)
        assertEquals("🎯", result.statusEmoji)
    }

    @Test
    fun `evaluateMetabolicCalibration detects faster metabolic burn when weight loss exceeds projection`() {
        val result = NutritionCalculator.evaluateMetabolicCalibration(
            actualWeight = 69.2f,
            projectedWeight = 70.0f
        )
        assertEquals(false, result.isAccurate)
        assertEquals(-0.8f, result.deviationKg, 0.01f)
        assertEquals("🔥", result.statusEmoji)
    }

    @Test
    fun `evaluateMetabolicCalibration handles fluid retention appropriately when weight is higher than projection`() {
        val result = NutritionCalculator.evaluateMetabolicCalibration(
            actualWeight = 70.6f,
            projectedWeight = 70.0f
        )
        assertEquals(false, result.isAccurate)
        assertEquals(0.6f, result.deviationKg, 0.01f)
        assertEquals("💧", result.statusEmoji)
    }
}

