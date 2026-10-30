package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import kotlinx.coroutines.flow.Flow

interface IntakeRepository {
    fun getAllIntakes(): Flow<List<Intake>>
    fun getIntakesForDailyLogs(dailyLogIds: List<Long>): Flow<List<Intake>>
    suspend fun getIntakesForDailyLogsDirect(dailyLogIds: List<Long>): List<Intake>
    fun getIntakesForDailyLog(dailyLogId: Long): Flow<List<Intake>>
    suspend fun getIntakesForDailyLogDirect(dailyLogId: Long): List<Intake>
    suspend fun addIntake(intake: Intake)
    suspend fun updateIntake(intake: Intake)
    suspend fun deleteIntake(intake: Intake)
}
