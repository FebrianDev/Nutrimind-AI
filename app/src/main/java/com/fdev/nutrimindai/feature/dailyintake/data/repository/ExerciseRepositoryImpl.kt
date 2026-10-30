package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalExerciseDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val localDataSource: LocalExerciseDataSource,
    private val preferenceManager: PreferenceManager
) : ExerciseRepository {

    override fun getExercisesForDailyLog(dailyLogId: Long): Flow<List<Exercise>> {
        return localDataSource.getExercisesForDailyLog(dailyLogId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExercisesForDailyLogDirect(dailyLogId: Long): List<Exercise> {
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
            if (exercise.id > 0L) {
                add(exercise.id.toString())
                if (createdAt > 0L) {
                    add("${createdAt}_${exercise.id}")
                }
            }
        }
        localDataSource.deleteExercise(existingEntity ?: exercise.toEntity())
        preferenceManager.queueCloudDeletion("exercises", docIds)
    }
}
