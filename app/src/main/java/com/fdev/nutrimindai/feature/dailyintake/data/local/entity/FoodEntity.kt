package com.fdev.nutrimindai.feature.dailyintake.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "food_table")
data class FoodEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servingSize: Double = 1.0,
    val servingUnit: String = "serving",
    val isSync: Boolean = false
)
