package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun getAllWaterRecords(): Flow<List<Water>>
    fun getWaterRecordsForDailyLogs(dailyLogIds: List<Long>): Flow<List<Water>>
    suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<Long>): List<Water>
    fun getWaterRecordsForDailyLog(dailyLogId: Long): Flow<List<Water>>
    suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: Long): List<Water>
    suspend fun addWater(water: Water)
    suspend fun deleteWater(water: Water)
}
