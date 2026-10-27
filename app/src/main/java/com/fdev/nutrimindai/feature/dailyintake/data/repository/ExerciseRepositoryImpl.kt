package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalExerciseDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val localDataSource: LocalExerciseDataSource
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
        localDataSource.deleteExercise(exercise.toEntity())
    }
}
