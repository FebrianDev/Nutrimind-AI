package com.fdev.nutrimindai.feature.dailyintake.data.local.datasource

import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalExerciseDataSource @Inject constructor(
    private val dao: ExerciseDao
) {
    fun getAllExercises(): Flow<List<ExerciseEntity>> =
        dao.getAllExercises()

    fun getExercisesForDailyLogs(dailyLogIds: List<String>): Flow<List<ExerciseEntity>> =
        dao.getExercisesForDailyLogs(dailyLogIds)

    suspend fun getExercisesForDailyLogsDirect(dailyLogIds: List<String>): List<ExerciseEntity> =
        if (dailyLogIds.isEmpty()) emptyList() else dao.getExercisesForDailyLogsDirect(dailyLogIds)

    fun getExercisesForDailyLog(dailyLogId: String): Flow<List<ExerciseEntity>> =
        dao.getExercisesForDailyLog(dailyLogId)

    suspend fun getExercisesForDailyLogDirect(dailyLogId: String): List<ExerciseEntity> =
        dao.getExercisesForDailyLogDirect(dailyLogId)

    fun getTotalCaloriesBurned(dailyLogId: String): Flow<Int> =
        dao.getTotalCaloriesBurned(dailyLogId)

    suspend fun insertExercise(exercise: ExerciseEntity) =
        dao.insertExercise(exercise)

    suspend fun deleteExercise(exercise: ExerciseEntity) =
        dao.deleteExercise(exercise)

    suspend fun getExerciseCount(dailyLogId: String): Int =
        dao.getExerciseCount(dailyLogId)
}
