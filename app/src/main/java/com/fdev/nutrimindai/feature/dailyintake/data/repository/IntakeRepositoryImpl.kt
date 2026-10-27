package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalIntakeDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IntakeRepositoryImpl @Inject constructor(
    private val localDataSource: LocalIntakeDataSource
) : IntakeRepository {

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
        localDataSource.deleteIntake(intake.toEntity())
    }
}
