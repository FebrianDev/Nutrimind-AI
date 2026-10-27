package com.fdev.nutrimindai.core.notification

import com.fdev.nutrimindai.core.datastore.ReminderSettings
import com.fdev.nutrimindai.core.util.NutritionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartNotificationLogicTest {

    @Test
    fun `default ReminderSettings has smartSkip enabled and 250ml quick log`() {
        val settings = ReminderSettings()
        assertTrue(settings.breakfastEnabled)
        assertTrue(settings.lunchEnabled)
        assertTrue(settings.dinnerEnabled)
        assertTrue(settings.waterEnabled)
        assertTrue(settings.dailyReviewEnabled)
        assertTrue(settings.smartSkipIfLogged)
        assertEquals(250, settings.quickLogWaterAmountMl)
        assertEquals(2.0f, settings.waterIntervalHours, 0.001f)
    }

    @Test
    fun `water interval in minutes is correctly converted from hours`() {
        val settings2h = ReminderSettings(waterIntervalHours = 2.0f)
        val minutes2h = (settings2h.waterIntervalHours * 60).toInt().coerceAtLeast(30)
        assertEquals(120, minutes2h)

        val settings1_5h = ReminderSettings(waterIntervalHours = 1.5f)
        val minutes1_5h = (settings1_5h.waterIntervalHours * 60).toInt().coerceAtLeast(30)
        assertEquals(90, minutes1_5h)

        val settingsMin = ReminderSettings(waterIntervalHours = 0.2f)
        val minutesMin = (settingsMin.waterIntervalHours * 60).toInt().coerceAtLeast(30)
        assertEquals(30, minutesMin) // Enforces 30 minutes minimum
    }

    @Test
    fun `smart hydration logic detects goal reached accurately`() {
        val targetWater = NutritionCalculator.calculateDynamicWaterTarget(weightKg = 70f, exerciseMinutes = 30) // 2450 + 350 = 2800 ml
        assertEquals(2800, targetWater)

        val currentWater1 = 1500
        val isGoalReached1 = currentWater1 >= targetWater
        assertFalse(isGoalReached1)

        val currentWater2 = 2800
        val isGoalReached2 = currentWater2 >= targetWater
        assertTrue(isGoalReached2)

        val currentWater3 = 3000
        val isGoalReached3 = currentWater3 >= targetWater
        assertTrue(isGoalReached3)
    }

    @Test
    fun `smart meal remaining calorie calculation handles under and over budget`() {
        val targetCalories = 2200

        // Eaten 800 kcal (e.g. Breakfast 500 + Snack 300)
        val consumed1 = 800
        val remaining1 = (targetCalories - consumed1).coerceAtLeast(0)
        assertEquals(1400, remaining1)

        // Eaten 2400 kcal (over budget)
        val consumed2 = 2400
        val remaining2 = (targetCalories - consumed2).coerceAtLeast(0)
        assertEquals(0, remaining2) // Doesn't go negative
    }

    @Test
    fun `quick log water addition calculates new total and remaining accurately`() {
        val initialWater = 1200
        val quickLogAmount = 250
        val newTotal = initialWater + quickLogAmount
        assertEquals(1450, newTotal)

        val targetWater = 2500
        val percentage = (newTotal * 100) / targetWater
        assertEquals(58, percentage)
    }
}
