package com.fdev.nutrimindai.feature.dailyintake.data.mapper

import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun DailyLogEntity.toDomain() = DailyLog(
    id = id,
    date = date,
    status = status,
    createdAt = createdAt,
    completedAt = completedAt,
    aiSummary = aiSummary
)

fun IntakeEntity.toDomain() = Intake(
    id = id,
    dailyLogId = dailyLogId,
    mealType = mealType,
    foodName = foodName,
    servingSize = servingSize,
    servingUnit = servingUnit,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    imagePath = imagePath,
    foodSource = foodSource,
    createdAt = createdAt
)

fun Intake.toEntity() = IntakeEntity(
    id = id,
    dailyLogId = dailyLogId,
    mealType = mealType,
    foodName = foodName,
    servingSize = servingSize,
    servingUnit = servingUnit,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    imagePath = imagePath,
    foodSource = foodSource,
    createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis()
)

fun WaterEntity.toDomain(): Water {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = formatter.format(Date(createdAt))
    return Water(
        id = id,
        dailyLogId = dailyLogId,
        amountMl = amountMl,
        time = timeString
    )
}

fun Water.toEntity(createdAt: Long = System.currentTimeMillis()) = WaterEntity(
    id = id,
    dailyLogId = dailyLogId,
    amountMl = amountMl,
    createdAt = createdAt
)

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    dailyLogId = dailyLogId,
    exerciseType = exerciseType,
    durationMinute = durationMinute,
    caloriesBurned = caloriesBurned,
    note = note,
    createdAt = createdAt
)

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    dailyLogId = dailyLogId,
    exerciseType = exerciseType,
    durationMinute = durationMinute,
    caloriesBurned = caloriesBurned,
    note = note,
    createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis()
)
