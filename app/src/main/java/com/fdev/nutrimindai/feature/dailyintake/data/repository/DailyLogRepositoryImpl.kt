package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalDailyLogDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalExerciseDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalIntakeDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalWaterDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class DailyLogRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDailyLogDataSource,
    private val intakeDataSource: LocalIntakeDataSource,
    private val waterDataSource: LocalWaterDataSource,
    private val exerciseDataSource: LocalExerciseDataSource
) : DailyLogRepository {

    override fun getDailyLogByDate(date: String): Flow<DailyLog?> {
        return localDataSource.getDailyLogByDate(date).map { it?.toDomain() }
    }

    override suspend fun getDailyLogByDateDirect(date: String): DailyLog? {
        return localDataSource.getDailyLogByDateDirect(date)?.toDomain()
    }

    override fun getAllDailyLogs(): Flow<List<DailyLog>> {
        return localDataSource.getAllDailyLogs().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllDailyLogsDirect(): List<DailyLog> {
        return localDataSource.getAllDailyLogsDirect().map { it.toDomain() }
    }

    override fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLog>> {
        return localDataSource.getDailyLogsBetweenDates(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLog> {
        return localDataSource.getDailyLogsBetweenDatesDirect(startDate, endDate).map { it.toDomain() }
    }

    override suspend fun autoCompletePastLogs() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val pastDrafts = localDataSource.getPastDraftLogs(currentDate)

        for (log in pastDrafts) {
            val intakeCount = intakeDataSource.getIntakeCount(log.id)
            val waterCount = waterDataSource.getWaterCount(log.id)
            val exerciseCount = exerciseDataSource.getExerciseCount(log.id)

            val newStatus = if (intakeCount > 0 || waterCount > 0 || exerciseCount > 0) {
                DailyLogStatus.COMPLETED
            } else {
                DailyLogStatus.MISSED
            }

            localDataSource.updateDailyLog(
                log.copy(
                    status = newStatus,
                    completedAt = if (newStatus == DailyLogStatus.COMPLETED) System.currentTimeMillis() else null,
                    isSync = false
                )
            )
        }
    }

    override suspend fun getOrCreateDailyLog(date: String): Long {
        autoCompletePastLogs()
        val existing = localDataSource.getDailyLogByDateDirect(date)
        if (existing != null) return existing.id

        val newLog = DailyLogEntity(date = date)
        val insertedId = localDataSource.insertDailyLog(newLog)
        return if (insertedId == -1L) {
            localDataSource.getDailyLogByDateDirect(date)?.id ?: 0L
        } else {
            insertedId
        }
    }

    override suspend fun completeDailyLog(date: String, aiSummary: String?) {
        val existing = localDataSource.getDailyLogByDateDirect(date)
        if (existing != null) {
            localDataSource.updateDailyLog(
                existing.copy(
                    status = DailyLogStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    aiSummary = aiSummary ?: existing.aiSummary,
                    isSync = false
                )
            )
        }
    }

    override suspend fun completeToday(aiSummary: String?) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        getOrCreateDailyLog(currentDate)
        completeDailyLog(currentDate, aiSummary)
    }

    override suspend fun saveAiSummary(dailyLogId: Long, aiSummary: String) {
        localDataSource.updateAiSummary(dailyLogId, aiSummary)
    }

    override suspend fun reopenToday() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        getOrCreateDailyLog(currentDate)
        reopenDailyLog(currentDate)
    }

    override suspend fun reopenDailyLog(date: String) {
        val existing = localDataSource.getDailyLogByDateDirect(date)
        if (existing != null) {
            localDataSource.updateDailyLog(
                existing.copy(
                    status = DailyLogStatus.DRAFT,
                    completedAt = null,
                    isSync = false
                )
            )
        }
    }

    override suspend fun getDailyLogCount(): Int {
        return localDataSource.getDailyLogCount()
    }
}

