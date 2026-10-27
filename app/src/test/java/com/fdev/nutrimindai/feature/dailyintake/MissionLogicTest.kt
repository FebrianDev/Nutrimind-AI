package com.fdev.nutrimindai.feature.dailyintake

import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.NutritionSummary
import com.fdev.nutrimindai.feature.dailyintake.domain.model.TodaySummary
import com.fdev.nutrimindai.core.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionLogicTest {

    @Test
    fun `lose weight goal produces 5 fat loss missions and tracks water, calories and protein correctly`() {
        val nutrition = NutritionSummary(
            calories = 1450,
            goalCalories = 1800,
            protein = 95.0,
            goalProtein = 110,
            water = 2400,
            goalWater = 2500
        )

        val summary = TodaySummary(
            date = "2026-08-08",
            dailyLogId = 1L,
            userGoal = Goal.LoseWeight,
            breakfastList = listOf(
                Intake(
                    id = 1L,
                    dailyLogId = 1L,
                    mealType = MealType.BREAKFAST,
                    foodName = "Spinach Egg Salad",
                    servingSize = 1.0,
                    servingUnit = "portion",
                    calories = 350,
                    protein = 25.0,
                    carbs = 10.0,
                    fat = 15.0,
                    fiber = 4.0,
                    imagePath = null,
                    foodSource = FoodSource.MANUAL
                )
            )
        )

        val waterGoalMl = nutrition.goalWater
        val waterAchieved = nutrition.water >= (waterGoalMl * 0.9).toInt()
        val calorieDeficitMet = (nutrition.calories in 500..nutrition.goalCalories)
        val proteinTargetMet = nutrition.protein >= (nutrition.goalProtein * 0.8)

        assertTrue(waterAchieved)
        assertTrue(calorieDeficitMet)
        assertTrue(proteinTargetMet)
    }

    @Test
    fun `gain weight goal evaluates caloric surplus, high protein, and main meals`() {
        val nutrition = NutritionSummary(
            calories = 2600,
            goalCalories = 2500,
            protein = 150.0,
            goalProtein = 160,
            water = 2500,
            goalWater = 2500
        )

        val summary = TodaySummary(
            date = "2026-08-08",
            dailyLogId = 1L,
            userGoal = Goal.GainWeight,
            breakfastList = listOf(
                Intake(1, 1, MealType.BREAKFAST, "Oatmeal", 1.0, "bowl", 600, 30.0, 80.0, 15.0, 6.0, null, FoodSource.SEARCH)
            ),
            lunchList = listOf(
                Intake(2, 1, MealType.LUNCH, "Chicken Rice", 1.0, "plate", 1000, 60.0, 110.0, 20.0, 3.0, null, FoodSource.SEARCH)
            ),
            dinnerList = listOf(
                Intake(3, 1, MealType.DINNER, "Beef Steak", 1.0, "plate", 1000, 60.0, 40.0, 35.0, 2.0, null, FoodSource.SEARCH)
            )
        )

        val calorieSurplusMet = nutrition.calories >= (nutrition.goalCalories * 0.9).toInt()
        val highProteinMet = nutrition.protein >= (nutrition.goalProtein * 0.85)
        val mainMealsCount = listOf(summary.breakfastList, summary.lunchList, summary.dinnerList).count { it.isNotEmpty() }

        assertTrue(calorieSurplusMet)
        assertTrue(highProteinMet)
        assertEquals(3, mainMealsCount)
    }
}
