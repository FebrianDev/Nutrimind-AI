package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import kotlinx.coroutines.flow.Flow

interface DailyLogRepository {
    fun getDailyLogByDate(date: String): Flow<DailyLog?>
    suspend fun getDailyLogByDateDirect(date: String): DailyLog?
    fun getAllDailyLogs(): Flow<List<DailyLog>>
    suspend fun getAllDailyLogsDirect(): List<DailyLog>
    fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLog>>
    suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLog>
    suspend fun getOrCreateDailyLog(date: String): String
    suspend fun completeDailyLog(date: String, aiSummary: String? = null)
    suspend fun completeToday(aiSummary: String? = null)
    suspend fun saveAiSummary(dailyLogId: String, aiSummary: String)
    suspend fun reopenToday()
    suspend fun reopenDailyLog(date: String)
    suspend fun getDailyLogCount(): Int
    suspend fun autoCompletePastLogs()
}
