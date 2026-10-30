package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalIntakeDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IntakeRepositoryImpl @Inject constructor(
    private val localDataSource: LocalIntakeDataSource,
    private val preferenceManager: PreferenceManager
) : IntakeRepository {

    override fun getAllIntakes(): Flow<List<Intake>> {
        return localDataSource.getAllIntakes().map { list -> list.map { it.toDomain() } }
    }

    override fun getIntakesForDailyLogs(dailyLogIds: List<Long>): Flow<List<Intake>> {
        if (dailyLogIds.isEmpty()) return flowOf(emptyList())
        return localDataSource.getIntakesForDailyLogs(dailyLogIds).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getIntakesForDailyLogsDirect(dailyLogIds: List<Long>): List<Intake> {
        return localDataSource.getIntakesForDailyLogsDirect(dailyLogIds).map { it.toDomain() }
    }

    override fun getIntakesForDailyLog(dailyLogId: Long): Flow<List<Intake>> {
        return localDataSource.getIntakesForDailyLog(dailyLogId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getIntakesForDailyLogDirect(dailyLogId: Long): List<Intake> {
        return localDataSource.getIntakesForDailyLogDirect(dailyLogId).map { it.toDomain() }
    }

    override suspend fun addIntake(intake: Intake) {
        localDataSource.insertIntake(intake.toEntity())
    }

    override suspend fun updateIntake(intake: Intake) {
        localDataSource.updateIntake(intake.toEntity())
    }

    override suspend fun deleteIntake(intake: Intake) {
        val existingEntity = localDataSource.getIntakesForDailyLogDirect(intake.dailyLogId)
            .find { it.id == intake.id }
        val createdAt = existingEntity?.createdAt?.takeIf { it > 0L } ?: intake.createdAt
        val docIds = buildList {
            if (intake.id > 0L) {
                add(intake.id.toString())
                if (createdAt > 0L) {
                    add("${createdAt}_${intake.id}")
                }
            }
        }
        localDataSource.deleteIntake(existingEntity ?: intake.toEntity())
        preferenceManager.queueCloudDeletion("intakes", docIds)
    }
}
