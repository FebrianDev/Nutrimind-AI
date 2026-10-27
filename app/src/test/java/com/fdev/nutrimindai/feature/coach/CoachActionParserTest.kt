package com.fdev.nutrimindai.feature.coach

import com.fdev.nutrimindai.feature.coach.domain.CoachActionParser
import com.fdev.nutrimindai.feature.coach.domain.model.CoachAction
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoachActionParserTest {

    private lateinit var parser: CoachActionParser

    @Before
    fun setUp() {
        parser = CoachActionParser()
    }

    @Test
    fun `parse returns clean text and empty actions when no action block present`() {
        val input = "Drinking enough water and eating balanced macros is key to energy levels!"
        val (cleanText, actions) = parser.parse(input)

        assertEquals(input, cleanText)
        assertTrue(actions.isEmpty())
        assertNull(parser.formatActionSummary(actions))
    }

    @Test
    fun `parse extracts food and water actions from fenced action block`() {
        val input = """
            Great choice having boiled eggs for breakfast! They give you high-quality protein to stay energized.
            
            ```json:action
            {
              "actions": [
                {
                  "type": "LOG_FOOD",
                  "foodName": "2 Boiled Eggs",
                  "mealType": "BREAKFAST",
                  "calories": 156,
                  "protein": 12.6,
                  "carbs": 1.1,
                  "fat": 10.6,
                  "fiber": 0.0,
                  "servingSize": 2.0,
                  "servingUnit": "large eggs"
                },
                {
                  "type": "LOG_WATER",
                  "amountMl": 500
                }
              ]
            }
            ```
        """.trimIndent()

        val (cleanText, actions) = parser.parse(input)

        assertEquals(
            "Great choice having boiled eggs for breakfast! They give you high-quality protein to stay energized.",
            cleanText
        )
        assertEquals(2, actions.size)

        val foodAction = actions[0] as CoachAction.LogFood
        assertEquals("2 Boiled Eggs", foodAction.foodName)
        assertEquals(MealType.BREAKFAST, foodAction.mealType)
        assertEquals(156, foodAction.calories)
        assertEquals(12.6, foodAction.protein, 0.01)
        assertEquals(1.1, foodAction.carbs, 0.01)
        assertEquals(10.6, foodAction.fat, 0.01)
        assertEquals(2.0, foodAction.servingSize, 0.01)
        assertEquals("large eggs", foodAction.servingUnit)

        val waterAction = actions[1] as CoachAction.LogWater
        assertEquals(500, waterAction.amountMl)

        val summary = parser.formatActionSummary(actions)
        assertNotNull(summary)
        assertTrue(summary!!.contains("2 Boiled Eggs (156 kcal)"))
        assertTrue(summary.contains("+500 ml Water"))
    }

    @Test
    fun `parse handles unfenced action block at end of response gracefully`() {
        val input = """
            I've logged your lunch! Keep up the momentum today.
            {"actions":[{"type":"LOG_FOOD","foodName":"Chicken Salad","mealType":"LUNCH","calories":320,"protein":28.0}]}
        """.trimIndent()

        val (cleanText, actions) = parser.parse(input)

        assertEquals("I've logged your lunch! Keep up the momentum today.", cleanText)
        assertEquals(1, actions.size)
        val food = actions[0] as CoachAction.LogFood
        assertEquals("Chicken Salad", food.foodName)
        assertEquals(MealType.LUNCH, food.mealType)
        assertEquals(320, food.calories)
        assertEquals(28.0, food.protein, 0.01)
    }

    @Test
    fun `parse safely ignores malformed or invalid action json without crashing`() {
        val input = """
            Nice job on your workout!
            ```json:action
            { this is broken json }
            ```
        """.trimIndent()

        val (cleanText, actions) = parser.parse(input)

        assertEquals("Nice job on your workout!", cleanText)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `parse defaults unknown mealType to OTHERS and filters empty food names`() {
        val input = """
            Logged your snack!
            ```action
            {
              "actions": [
                {
                  "type": "LOG_FOOD",
                  "foodName": "",
                  "calories": 100
                },
                {
                  "type": "LOG_FOOD",
                  "foodName": "Dark Chocolate",
                  "mealType": "MID_AFTERNOON_SNACK",
                  "calories": 120
                }
              ]
            }
            ```
        """.trimIndent()

        val (cleanText, actions) = parser.parse(input)
        assertEquals("Logged your snack!", cleanText)
        assertEquals(1, actions.size)
        val food = actions[0] as CoachAction.LogFood
        assertEquals("Dark Chocolate", food.foodName)
        assertEquals(MealType.OTHERS, food.mealType)
        assertEquals(120, food.calories)
    }

    @Test
    fun `parse extracts exercise and weight actions correctly`() {
        val input = """
            Awesome workout and great job tracking your body weight today!
            
            ```json:action
            {
              "actions": [
                {
                  "type": "LOG_EXERCISE",
                  "exerciseType": "Running",
                  "durationMinute": 35,
                  "caloriesBurned": 280,
                  "note": "Morning interval run"
                },
                {
                  "type": "LOG_WEIGHT",
                  "weightKg": 68.4,
                  "note": "Fasted morning weight"
                }
              ]
            }
            ```
        """.trimIndent()

        val (cleanText, actions) = parser.parse(input)
        assertEquals("Awesome workout and great job tracking your body weight today!", cleanText)
        assertEquals(2, actions.size)

        val exercise = actions[0] as CoachAction.LogExercise
        assertEquals("Running", exercise.exerciseType)
        assertEquals(35, exercise.durationMinute)
        assertEquals(280, exercise.caloriesBurned)
        assertEquals("Morning interval run", exercise.note)

        val weight = actions[1] as CoachAction.LogWeight
        assertEquals(68.4f, weight.weightKg, 0.01f)
        assertEquals("Fasted morning weight", weight.note)

        val summary = parser.formatActionSummary(actions)
        assertNotNull(summary)
        assertTrue(summary!!.contains("Running 35m (280 kcal)"))
        assertTrue(summary.contains("Weight 68.4 kg"))
    }
}
