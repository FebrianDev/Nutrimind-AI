package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getExercisesForDailyLog(dailyLogId: Long): Flow<List<Exercise>>
    suspend fun getExercisesForDailyLogDirect(dailyLogId: Long): List<Exercise>
    suspend fun addExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
}
