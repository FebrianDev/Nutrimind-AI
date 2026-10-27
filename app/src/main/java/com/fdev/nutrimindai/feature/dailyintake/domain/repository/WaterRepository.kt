package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun getWaterRecordsForDailyLog(dailyLogId: Long): Flow<List<Water>>
    suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: Long): List<Water>
    suspend fun addWater(water: Water)
    suspend fun deleteWater(water: Water)
}
