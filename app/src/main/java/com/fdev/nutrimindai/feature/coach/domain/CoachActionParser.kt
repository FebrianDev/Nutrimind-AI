package com.fdev.nutrimindai.feature.coach.domain

import com.fdev.nutrimindai.feature.coach.domain.model.CoachAction
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent parser that detects, extracts, and validates agentic action blocks
 * produced by Meta AI in NutriCoach conversational responses.
 */
@Singleton
class CoachActionParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val explicitActionRegex = Regex(
        """```(?:json:action|action)\s*([\s\S]*?)\s*```""",
        RegexOption.IGNORE_CASE
    )

    private val genericActionRegex = Regex(
        """```(?:json)?\s*(\{[\s\S]*?"actions"[\s\S]*?\})\s*```""",
        RegexOption.IGNORE_CASE
    )

    @Serializable
    private data class ActionEnvelope(
        val actions: List<RawActionPayload> = emptyList()
    )

    @Serializable
    private data class RawActionPayload(
        val type: String,
        val foodName: String? = null,
        val mealType: String? = null,
        val calories: Int? = null,
        val protein: Double? = null,
        val carbs: Double? = null,
        val fat: Double? = null,
        val fiber: Double? = null,
        val servingSize: Double? = null,
        val servingUnit: String? = null,
        val amountMl: Int? = null
    )

    /**
     * Parses the raw AI response text, extracting any action directives while
     * cleaning the conversational text for display.
     *
     * @param rawResponse The raw string returned by the LLM
     * @return Pair of (cleanTextForUi, listOfExecutedActions)
     */
    fun parse(rawResponse: String): Pair<String, List<CoachAction>> {
        val trimmed = rawResponse.trim()
        if (trimmed.isBlank()) {
            return Pair("", emptyList())
        }

        val explicitMatch = explicitActionRegex.find(trimmed)
        val genericMatch = if (explicitMatch == null) genericActionRegex.find(trimmed) else null
        val match = explicitMatch ?: genericMatch

        val jsonPayload: String?
        val cleanText: String

        if (match != null) {
            jsonPayload = match.groupValues[1]
            cleanText = trimmed.removeRange(match.range).trim()
        } else {
            // Check if there is an un-fenced JSON action block at the end of the text
            val actionStart = trimmed.lastIndexOf("{\"actions\"")
            if (actionStart != -1 && trimmed.endsWith("}")) {
                jsonPayload = trimmed.substring(actionStart)
                cleanText = trimmed.substring(0, actionStart).trim()
            } else {
                jsonPayload = null
                cleanText = trimmed
            }
        }

        if (jsonPayload.isNullOrBlank()) {
            return Pair(cleanText, emptyList())
        }

        val parsedActions = try {
            val envelope = json.decodeFromString<ActionEnvelope>(jsonPayload)
            envelope.actions.mapNotNull { raw ->
                mapRawAction(raw)
            }
        } catch (e: Exception) {
            Timber.w(e, "CoachActionParser: Failed to parse action JSON: $jsonPayload")
            emptyList()
        }

        return Pair(cleanText, parsedActions)
    }

    private fun mapRawAction(raw: RawActionPayload): CoachAction? {
        return when (raw.type.uppercase().trim()) {
            "LOG_FOOD" -> {
                val name = raw.foodName?.trim()
                if (name.isNullOrBlank()) return null

                val mealType = MealType.entries.firstOrNull {
                    it.name.equals(raw.mealType, ignoreCase = true)
                } ?: MealType.OTHERS

                CoachAction.LogFood(
                    foodName = name,
                    mealType = mealType,
                    calories = (raw.calories ?: 0).coerceAtLeast(0),
                    protein = (raw.protein ?: 0.0).coerceAtLeast(0.0),
                    carbs = (raw.carbs ?: 0.0).coerceAtLeast(0.0),
                    fat = (raw.fat ?: 0.0).coerceAtLeast(0.0),
                    fiber = (raw.fiber ?: 0.0).coerceAtLeast(0.0),
                    servingSize = (raw.servingSize ?: 1.0).coerceAtLeast(0.1),
                    servingUnit = raw.servingUnit?.trim()?.ifBlank { "portion" } ?: "portion"
                )
            }
            "LOG_WATER" -> {
                val amount = raw.amountMl ?: 0
                if (amount <= 0) return null
                CoachAction.LogWater(amountMl = amount)
            }
            else -> {
                Timber.w("CoachActionParser: Unknown action type '${raw.type}'")
                null
            }
        }
    }

    /**
     * Formats a concise, elegant badge summary of the executed actions to display in the UI.
     */
    fun formatActionSummary(actions: List<CoachAction>): String? {
        if (actions.isEmpty()) return null
        val items = mutableListOf<String>()

        val foods = actions.filterIsInstance<CoachAction.LogFood>()
        val waters = actions.filterIsInstance<CoachAction.LogWater>()

        foods.forEach { food ->
            items.add("${food.foodName} (${food.calories} kcal)")
        }

        val totalWater = waters.sumOf { it.amountMl }
        if (totalWater > 0) {
            items.add("+$totalWater ml Water")
        }

        return "Logged to Diary: " + items.joinToString(", ")
    }
}
