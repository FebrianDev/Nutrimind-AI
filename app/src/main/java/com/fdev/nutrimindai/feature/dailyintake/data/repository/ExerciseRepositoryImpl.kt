package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalExerciseDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val localDataSource: LocalExerciseDataSource,
    private val preferenceManager: PreferenceManager
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return localDataSource.getAllExercises().map { list -> list.map { it.toDomain() } }
    }

    override fun getExercisesForDailyLogs(dailyLogIds: List<String>): Flow<List<Exercise>> {
        if (dailyLogIds.isEmpty()) return flowOf(emptyList())
        return localDataSource.getExercisesForDailyLogs(dailyLogIds).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExercisesForDailyLogsDirect(dailyLogIds: List<String>): List<Exercise> {
        return localDataSource.getExercisesForDailyLogsDirect(dailyLogIds).map { it.toDomain() }
    }

    override fun getExercisesForDailyLog(dailyLogId: String): Flow<List<Exercise>> {
        return localDataSource.getExercisesForDailyLog(dailyLogId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExercisesForDailyLogDirect(dailyLogId: String): List<Exercise> {
        return localDataSource.getExercisesForDailyLogDirect(dailyLogId).map { it.toDomain() }
    }

    override suspend fun addExercise(exercise: Exercise) {
        localDataSource.insertExercise(exercise.toEntity())
    }

    override suspend fun deleteExercise(exercise: Exercise) {
        val existingEntity = localDataSource.getExercisesForDailyLogDirect(exercise.dailyLogId)
            .find { it.id == exercise.id }
        val createdAt = existingEntity?.createdAt?.takeIf { it > 0L } ?: exercise.createdAt
        val docIds = buildList {
            if (exercise.id.isNotBlank()) {
                add(exercise.id)
                if (createdAt > 0L) {
                    add("${createdAt}_${exercise.id}")
                }
            }
        }
        localDataSource.deleteExercise(existingEntity ?: exercise.toEntity())
        preferenceManager.queueCloudDeletion("exercises", docIds)
    }
}
