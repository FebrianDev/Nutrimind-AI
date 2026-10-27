package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalWaterDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val localDataSource: LocalWaterDataSource
) : WaterRepository {

    override fun getWaterRecordsForDailyLog(dailyLogId: Long): Flow<List<Water>> {
        return localDataSource.getWaterRecordsForDailyLog(dailyLogId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: Long): List<Water> {
        return localDataSource.getWaterRecordsForDailyLogDirect(dailyLogId).map { it.toDomain() }
    }

    override suspend fun addWater(water: Water) {
        localDataSource.insertWater(water.toEntity())
    }

    override suspend fun deleteWater(water: Water) {
        localDataSource.deleteWater(water.toEntity(createdAt = 0L))
    }
}
