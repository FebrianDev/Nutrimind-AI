package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExercisesForDailyLogs(dailyLogIds: List<Long>): Flow<List<Exercise>>
    suspend fun getExercisesForDailyLogsDirect(dailyLogIds: List<Long>): List<Exercise>
    fun getExercisesForDailyLog(dailyLogId: Long): Flow<List<Exercise>>
    suspend fun getExercisesForDailyLogDirect(dailyLogId: Long): List<Exercise>
    suspend fun addExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
}
