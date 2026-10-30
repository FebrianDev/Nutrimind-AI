package com.fdev.nutrimindai.feature.dailyintake.data.local.datasource

import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalIntakeDataSource @Inject constructor(
    private val dao: IntakeDao
) {
    fun getAllIntakes(): Flow<List<IntakeEntity>> =
        dao.getAllIntakes()

    fun getIntakesForDailyLogs(dailyLogIds: List<Long>): Flow<List<IntakeEntity>> =
        dao.getIntakesForDailyLogs(dailyLogIds)

    suspend fun getIntakesForDailyLogsDirect(dailyLogIds: List<Long>): List<IntakeEntity> =
        if (dailyLogIds.isEmpty()) emptyList() else dao.getIntakesForDailyLogsDirect(dailyLogIds)

    fun getIntakesForDailyLog(dailyLogId: Long): Flow<List<IntakeEntity>> =
        dao.getIntakesForDailyLog(dailyLogId)

    suspend fun getIntakesForDailyLogDirect(dailyLogId: Long): List<IntakeEntity> =
        dao.getIntakesForDailyLogDirect(dailyLogId)

    fun getIntakesByMealType(dailyLogId: Long, mealType: String): Flow<List<IntakeEntity>> =
        dao.getIntakesByMealType(dailyLogId, mealType)

    suspend fun insertIntake(intake: IntakeEntity) =
        dao.insertIntake(intake)

    suspend fun updateIntake(intake: IntakeEntity) =
        dao.updateIntake(intake)

    suspend fun deleteIntake(intake: IntakeEntity) =
        dao.deleteIntake(intake)

    suspend fun getIntakeCount(dailyLogId: Long): Int =
        dao.getIntakeCount(dailyLogId)
}
