package com.fdev.nutrimindai.feature.dailyintake.data.local.datasource

import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalWaterDataSource @Inject constructor(
    private val dao: WaterDao
) {
    fun getAllWaterRecords(): Flow<List<WaterEntity>> =
        dao.getAllWaterRecords()

    fun getWaterRecordsForDailyLogs(dailyLogIds: List<String>): Flow<List<WaterEntity>> =
        dao.getWaterRecordsForDailyLogs(dailyLogIds)

    suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<String>): List<WaterEntity> =
        if (dailyLogIds.isEmpty()) emptyList() else dao.getWaterRecordsForDailyLogsDirect(dailyLogIds)

    fun getWaterRecordsForDailyLog(dailyLogId: String): Flow<List<WaterEntity>> =
        dao.getWaterRecordsForDailyLog(dailyLogId)

    suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: String): List<WaterEntity> =
        dao.getWaterRecordsForDailyLogDirect(dailyLogId)

    fun getTotalWater(dailyLogId: String): Flow<Int> =
        dao.getTotalWater(dailyLogId)

    suspend fun insertWater(water: WaterEntity) =
        dao.insertWater(water)

    suspend fun deleteWater(water: WaterEntity) =
        dao.deleteWater(water)

    suspend fun getWaterCount(dailyLogId: String): Int =
        dao.getWaterCount(dailyLogId)
}
