package com.fdev.nutrimindai.feature.progress.domain.repository

import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

interface WeightLogRepository {
    fun getAllWeightLogs(): Flow<List<WeightLogEntity>>
    suspend fun getAllWeightLogsDirect(): List<WeightLogEntity>
    fun getWeightLogByDate(date: String): Flow<WeightLogEntity?>
    suspend fun getWeightLogByDateDirect(date: String): WeightLogEntity?
    fun getLatestWeightLog(): Flow<WeightLogEntity?>
    suspend fun getLatestWeightLogDirect(): WeightLogEntity?
    suspend fun logWeight(date: String, weightKg: Float, note: String? = null)
    suspend fun deleteWeightLog(id: String)
}
