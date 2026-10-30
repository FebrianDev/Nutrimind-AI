package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import kotlinx.coroutines.flow.Flow

interface WaterRepository {
    fun getAllWaterRecords(): Flow<List<Water>>
    fun getWaterRecordsForDailyLogs(dailyLogIds: List<String>): Flow<List<Water>>
    suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<String>): List<Water>
    fun getWaterRecordsForDailyLog(dailyLogId: String): Flow<List<Water>>
    suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: String): List<Water>
    suspend fun addWater(water: Water)
    suspend fun deleteWater(water: Water)
}
