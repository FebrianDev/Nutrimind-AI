package com.fdev.nutrimindai.feature.progress.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_log_table",
    indices = [
        Index(value = ["date"])
    ]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val weightKg: Float,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSync: Boolean = false
)
