package com.fdev.nutrimindai.feature.dailyintake.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import java.util.UUID

@Entity(
    tableName = "intake",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dailyLogId"])
    ]
)
data class IntakeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val dailyLogId: String,
    val mealType: MealType,
    val foodName: String,
    val servingSize: Double,
    val servingUnit: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val imagePath: String? = null,
    val foodSource: FoodSource,
    val createdAt: Long = System.currentTimeMillis(),
    val isSync: Boolean = false
)
