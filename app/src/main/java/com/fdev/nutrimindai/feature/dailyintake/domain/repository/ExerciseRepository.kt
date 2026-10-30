package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExercisesForDailyLogs(dailyLogIds: List<String>): Flow<List<Exercise>>
    suspend fun getExercisesForDailyLogsDirect(dailyLogIds: List<String>): List<Exercise>
    fun getExercisesForDailyLog(dailyLogId: String): Flow<List<Exercise>>
    suspend fun getExercisesForDailyLogDirect(dailyLogId: String): List<Exercise>
    suspend fun addExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
}
