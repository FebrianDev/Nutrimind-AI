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

    fun getWaterRecordsForDailyLogs(dailyLogIds: List<Long>): Flow<List<WaterEntity>> =
        dao.getWaterRecordsForDailyLogs(dailyLogIds)

    suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<Long>): List<WaterEntity> =
        if (dailyLogIds.isEmpty()) emptyList() else dao.getWaterRecordsForDailyLogsDirect(dailyLogIds)

    fun getWaterRecordsForDailyLog(dailyLogId: Long): Flow<List<WaterEntity>> =
        dao.getWaterRecordsForDailyLog(dailyLogId)

    suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: Long): List<WaterEntity> =
        dao.getWaterRecordsForDailyLogDirect(dailyLogId)

    fun getTotalWater(dailyLogId: Long): Flow<Int> =
        dao.getTotalWater(dailyLogId)

    suspend fun insertWater(water: WaterEntity) =
        dao.insertWater(water)

    suspend fun deleteWater(water: WaterEntity) =
        dao.deleteWater(water)

    suspend fun getWaterCount(dailyLogId: Long): Int =
        dao.getWaterCount(dailyLogId)
}
