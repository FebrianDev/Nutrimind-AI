package com.fdev.nutrimindai.feature.dailyintake.data.mapper

import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food

fun FoodEntity.toDomain(): Food = Food(
    id = id,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    servingSize = servingSize,
    servingUnit = servingUnit
)

fun Food.toEntity(): FoodEntity = FoodEntity(
    id = id,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    servingSize = servingSize,
    servingUnit = servingUnit
)
