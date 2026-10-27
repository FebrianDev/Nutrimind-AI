package com.fdev.nutrimindai.feature.dailyintake.data.local.datasource

import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalDailyLogDataSource @Inject constructor(
    private val dao: DailyLogDao
) {
    fun getDailyLogByDate(date: String): Flow<DailyLogEntity?> =
        dao.getDailyLogByDate(date)

    suspend fun getDailyLogByDateDirect(date: String): DailyLogEntity? =
        dao.getDailyLogByDateDirect(date)

    fun getAllDailyLogs(): Flow<List<DailyLogEntity>> =
        dao.getAllDailyLogs()

    suspend fun getAllDailyLogsDirect(): List<DailyLogEntity> =
        dao.getAllDailyLogsDirect()

    fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLogEntity>> =
        dao.getDailyLogsBetweenDates(startDate, endDate)

    suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLogEntity> =
        dao.getDailyLogsBetweenDatesDirect(startDate, endDate)

    suspend fun insertDailyLog(dailyLog: DailyLogEntity): Long =
        dao.insertDailyLog(dailyLog)

    suspend fun updateDailyLog(dailyLog: DailyLogEntity) =
        dao.updateDailyLog(dailyLog)

    suspend fun updateAiSummary(dailyLogId: Long, aiSummary: String) =
        dao.updateAiSummary(dailyLogId, aiSummary)

    suspend fun getDailyLogCount(): Int =
        dao.getDailyLogCount()

    suspend fun getPastDraftLogs(currentDate: String): List<DailyLogEntity> =
        dao.getPastDraftLogs(currentDate)
}
