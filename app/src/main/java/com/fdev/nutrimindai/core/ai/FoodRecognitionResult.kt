package com.fdev.nutrimindai.core.ai

import kotlinx.serialization.Serializable

/**
 * Data class holding the result of AI food image recognition analysis.
 */
@Serializable
data class FoodRecognitionResult(
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val servingSize: Double = 1.0,
    val servingUnit: String = "portion",
    val confidence: Double = 0.9,
    val description: String = ""
)
