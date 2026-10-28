package com.fdev.nutrimindai.feature.dailyintake.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SmartRecipe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val prepTimeMinutes: Int = 15,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 3.0,
    val mealType: MealType = MealType.DINNER,
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val chefTip: String = ""
)
