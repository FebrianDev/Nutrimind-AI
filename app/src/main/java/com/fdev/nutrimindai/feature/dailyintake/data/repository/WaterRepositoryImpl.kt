package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalWaterDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WaterRepositoryImpl @Inject constructor(
    private val localDataSource: LocalWaterDataSource,
    private val preferenceManager: PreferenceManager
) : WaterRepository {

    override fun getAllWaterRecords(): Flow<List<Water>> {
        return localDataSource.getAllWaterRecords().map { list -> list.map { it.toDomain() } }
    }

    override fun getWaterRecordsForDailyLogs(dailyLogIds: List<Long>): Flow<List<Water>> {
        if (dailyLogIds.isEmpty()) return flowOf(emptyList())
        return localDataSource.getWaterRecordsForDailyLogs(dailyLogIds).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<Long>): List<Water> {
        return localDataSource.getWaterRecordsForDailyLogsDirect(dailyLogIds).map { it.toDomain() }
    }

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
        val existingEntity = localDataSource.getWaterRecordsForDailyLogDirect(water.dailyLogId)
            .find { it.id == water.id }
        val docIds = buildList {
            if (water.id > 0L) {
                add(water.id.toString())
                if (existingEntity != null && existingEntity.createdAt > 0L) {
                    add("${existingEntity.createdAt}_${water.id}")
                }
            }
        }
        localDataSource.deleteWater(existingEntity ?: water.toEntity(createdAt = 0L))
        preferenceManager.queueCloudDeletion("water_records", docIds)
    }
}
