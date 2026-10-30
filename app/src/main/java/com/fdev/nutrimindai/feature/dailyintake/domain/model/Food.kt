package com.fdev.nutrimindai.feature.dailyintake.domain.model

import java.util.UUID

data class Food(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servingSize: Double = 1.0,
    val servingUnit: String = "serving"
)
