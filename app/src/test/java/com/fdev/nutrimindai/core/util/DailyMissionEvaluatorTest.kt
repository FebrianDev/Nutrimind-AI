package com.fdev.nutrimindai.core.util

import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyMissionEvaluatorTest {

    @Test
    fun `lose weight goal evaluates all 5 missions and awards 150 points for perfect day`() {
        val intakes = listOf(
            Intake(
                id = "1",
                dailyLogId = "log-1",
                mealType = MealType.BREAKFAST,
                foodName = "Spinach and boiled eggs",
                servingSize = 1.0,
                servingUnit = "portion",
                calories = 350,
                protein = 30.0,
                carbs = 10.0,
                fat = 10.0,
                fiber = 4.0,
                imagePath = null,
                foodSource = FoodSource.MANUAL
            ),
            Intake(
                id = "2",
                dailyLogId = "log-1",
                mealType = MealType.LUNCH,
                foodName = "Grilled Chicken Breast",
                servingSize = 1.0,
                servingUnit = "portion",
                calories = 800,
                protein = 70.0,
                carbs = 50.0,
                fat = 20.0,
                fiber = 2.0,
                imagePath = null,
                foodSource = FoodSource.MANUAL
            )
        )

        val day = DailyMissionEvaluator.evaluateDay(
            goal = Goal.LoseWeight,
            calories = 1150,
            goalCalories = 1800,
            protein = 100.0,
            goalProtein = 110,
            waterMl = 2400,
            goalWaterMl = 2500,
            exerciseMinutes = 35,
            intakes = intakes,
            dateStr = "2026-10-31",
            dayLabel = "Today"
        )

        assertEquals(5, day.totalMissions)
        assertEquals(5, day.completedCount)
        assertTrue(day.isPerfectDay)
        // 5 * 20 + 50 = 150
        assertEquals(150, day.pointsEarned)
    }

    @Test
    fun `gain weight goal evaluates caloric surplus, high protein, and main meals`() {
        val intakes = listOf(
            Intake("1", "log-1", MealType.BREAKFAST, "Oats", 1.0, "p", 600, 30.0, 80.0, 10.0, 5.0, null, FoodSource.MANUAL),
            Intake("2", "log-1", MealType.LUNCH, "Steak", 1.0, "p", 1000, 70.0, 50.0, 30.0, 1.0, null, FoodSource.MANUAL),
            Intake("3", "log-1", MealType.DINNER, "Salmon Rice", 1.0, "p", 900, 55.0, 60.0, 25.0, 2.0, null, FoodSource.MANUAL)
        )

        val day = DailyMissionEvaluator.evaluateDay(
            goal = Goal.GainWeight,
            calories = 2500,
            goalCalories = 2600,
            protein = 155.0,
            goalProtein = 160,
            waterMl = 2500,
            goalWaterMl = 2500,
            exerciseMinutes = 30,
            intakes = intakes,
            dateStr = "2026-10-31",
            dayLabel = "Today"
        )

        assertEquals(5, day.completedCount)
        assertTrue(day.isPerfectDay)
        assertEquals(150, day.pointsEarned)
    }

    @Test
    fun `maintain weight goal evaluates calorie balance within range`() {
        val intakes = listOf(
            Intake("1", "log-1", MealType.BREAKFAST, "Toast & Avocado", 1.0, "p", 500, 15.0, 40.0, 20.0, 6.0, null, FoodSource.MANUAL)
        )

        val day = DailyMissionEvaluator.evaluateDay(
            goal = Goal.MaintainWeight,
            calories = 2050,
            goalCalories = 2100, // in 1850..2350
            protein = 100.0,
            goalProtein = 120,
            waterMl = 2400,
            goalWaterMl = 2500,
            exerciseMinutes = 30,
            intakes = intakes,
            dateStr = "2026-10-31",
            dayLabel = "Today"
        )

        assertEquals(5, day.completedCount)
        assertTrue(day.isPerfectDay)
    }

    @Test
    fun `calculateMissionRewards calculates streaks, points, and badges correctly`() {
        // Create 7 days history
        val historyDays = (1..7).map { dayNum ->
            val isComplete = dayNum >= 4 // Days 4, 5, 6, 7 have >= 3 completed missions
            val completed = if (isComplete) 5 else 1
            EvaluatedDayMissions(
                dateStr = "2026-10-0$dayNum",
                dayLabel = "Day $dayNum",
                totalMissions = 5,
                completedCount = completed,
                pointsEarned = if (completed == 5) 150 else 20,
                isPerfectDay = (completed == 5),
                missionItems = listOf(
                    DailyMissionItemSummary("water", "Water", "Water", isCompleted = isComplete, iconType = MissionIconType.WATER),
                    DailyMissionItemSummary("cals", "Cals", "Cals", isCompleted = isComplete, iconType = MissionIconType.CALORIES),
                    DailyMissionItemSummary("prot", "Prot", "Prot", isCompleted = isComplete, iconType = MissionIconType.PROTEIN),
                    DailyMissionItemSummary("ex", "Ex", "Ex", isCompleted = isComplete, iconType = MissionIconType.EXERCISE),
                    DailyMissionItemSummary("veg", "Veg", "Veg", isCompleted = isComplete, iconType = MissionIconType.VEGETABLES)
                )
            )
        }

        val rewardData = DailyMissionEvaluator.calculateMissionRewards(historyDays)

        // Days 4, 5, 6, 7 are active -> current streak = 4
        assertEquals(4, rewardData.currentStreak)
        assertEquals(4, rewardData.bestStreak)
        assertEquals(4, rewardData.perfectDaysCount)

        // 3 days with 20 pts + 4 days with 150 pts = 60 + 600 = 660 pts
        assertEquals(660, rewardData.totalPoints)

        // Badges: First step unlocked, Perfect day unlocked, 3-day streak unlocked
        val badgeFirstStep = rewardData.badges.first { it.id == "badge_first_step" }
        val badgePerfectDay = rewardData.badges.first { it.id == "badge_perfect_day" }
        val badgeStreak3 = rewardData.badges.first { it.id == "badge_streak_3" }
        val badgeStreak7 = rewardData.badges.first { it.id == "badge_streak_7" }

        assertTrue(badgeFirstStep.isUnlocked)
        assertTrue(badgePerfectDay.isUnlocked)
        assertTrue(badgeStreak3.isUnlocked)
        assertFalse(badgeStreak7.isUnlocked) // Only 4 days streak so far
    }

    @Test
    fun `evaluateDay includes progressText and progressFraction for missions`() {
        val intakes = listOf(
            Intake("1", "log-1", MealType.BREAKFAST, "Eggs", 1.0, "p", 300, 20.0, 5.0, 10.0, 0.0, null, FoodSource.MANUAL)
        )

        val day = DailyMissionEvaluator.evaluateDay(
            goal = Goal.LoseWeight,
            calories = 300,
            goalCalories = 1800,
            protein = 20.0,
            goalProtein = 100,
            waterMl = 500,
            goalWaterMl = 2000,
            exerciseMinutes = 15,
            intakes = intakes,
            dateStr = "2026-10-31",
            dayLabel = "Today"
        )

        val waterMission = day.missionItems.first { it.id == "water_hydration" }
        assertEquals("0.5 / 2.0 L", waterMission.progressText)
        assertEquals(0.25f, waterMission.progressFraction, 0.01f)
        assertFalse(waterMission.isCompleted)

        val exerciseMission = day.missionItems.first { it.id == "exercise_burn" }
        assertEquals("15 / 30 min", exerciseMission.progressText)
        assertEquals(0.5f, exerciseMission.progressFraction, 0.01f)
        assertFalse(exerciseMission.isCompleted)

        assertTrue(day.overallProgressFraction > 0f)
    }
}

