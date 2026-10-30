package com.fdev.nutrimindai.feature.dailyintake.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import java.util.UUID

@Entity(
    tableName = "daily_log",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class DailyLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val date: String, // ISO-8601 format (YYYY-MM-DD)
    val status: DailyLogStatus = DailyLogStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val aiSummary: String? = null,
    val aiScore: Int? = null,
    val isSync: Boolean = false
)

