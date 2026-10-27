package com.fdev.nutrimindai.feature.dailyintake.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "water",
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
data class WaterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dailyLogId: Long,
    val amountMl: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isSync: Boolean = false
)
