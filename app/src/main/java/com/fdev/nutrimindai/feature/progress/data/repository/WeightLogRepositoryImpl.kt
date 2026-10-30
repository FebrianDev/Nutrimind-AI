package com.fdev.nutrimindai.feature.progress.data.repository

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightLogRepositoryImpl @Inject constructor(
    private val weightLogDao: WeightLogDao,
    private val preferenceManager: PreferenceManager
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
        val existing = weightLogDao.getWeightLogByDateDirect(date)
        val entity = WeightLogEntity(
            id = existing?.id ?: 0,
            date = date,
            weightKg = weightKg,
            note = note?.trim()?.ifBlank { null } ?: existing?.note,
            timestamp = System.currentTimeMillis(),
            isSync = false
        )
        weightLogDao.insertWeightLog(entity)
    }

    override suspend fun deleteWeightLog(id: Long) {
        val existing = weightLogDao.getAllWeightLogsDirect().find { it.id == id }
        val docIds = buildList {
            if (id > 0L) add(id.toString())
            if (existing != null && existing.date.isNotBlank()) {
                add(existing.date)
                add("${existing.date}_${existing.timestamp}_$id")
            }
        }
        weightLogDao.deleteWeightLog(id)
        preferenceManager.queueCloudDeletion("weight_logs", docIds)
    }
}
