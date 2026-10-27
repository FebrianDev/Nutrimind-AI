package com.fdev.nutrimindai.feature.progress.data.repository

import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightLogRepositoryImpl @Inject constructor(
    private val weightLogDao: WeightLogDao
) : WeightLogRepository {

    override fun getAllWeightLogs(): Flow<List<WeightLogEntity>> {
        return weightLogDao.getAllWeightLogs()
    }

    override suspend fun getAllWeightLogsDirect(): List<WeightLogEntity> {
        return weightLogDao.getAllWeightLogsDirect()
    }

    override fun getWeightLogByDate(date: String): Flow<WeightLogEntity?> {
        return weightLogDao.getWeightLogByDate(date)
    }

    override suspend fun getWeightLogByDateDirect(date: String): WeightLogEntity? {
        return weightLogDao.getWeightLogByDateDirect(date)
    }

    override fun getLatestWeightLog(): Flow<WeightLogEntity?> {
        return weightLogDao.getLatestWeightLog()
    }

    override suspend fun getLatestWeightLogDirect(): WeightLogEntity? {
        return weightLogDao.getLatestWeightLogDirect()
    }

    override suspend fun logWeight(date: String, weightKg: Float, note: String?) {
        val entity = WeightLogEntity(
            date = date,
            weightKg = weightKg,
            note = note?.trim()?.ifBlank { null }
        )
        weightLogDao.insertWeightLog(entity)
    }

    override suspend fun deleteWeightLog(id: Long) {
        weightLogDao.deleteWeightLog(id)
    }
}
