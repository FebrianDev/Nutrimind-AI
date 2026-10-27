package com.fdev.nutrimindai.feature.dailyintake.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE dailyLogId = :dailyLogId ORDER BY createdAt DESC")
    fun getExercisesForDailyLog(dailyLogId: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE dailyLogId = :dailyLogId ORDER BY createdAt DESC")
    suspend fun getExercisesForDailyLogDirect(dailyLogId: Long): List<ExerciseEntity>

    @Query("SELECT COALESCE(SUM(caloriesBurned), 0) FROM exercise WHERE dailyLogId = :dailyLogId")
    fun getTotalCaloriesBurned(dailyLogId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("SELECT COUNT(*) FROM exercise WHERE dailyLogId = :dailyLogId")
    suspend fun getExerciseCount(dailyLogId: Long): Int

    @Query("SELECT * FROM exercise WHERE isSync = 0")
    suspend fun getUnsyncedExercises(): List<ExerciseEntity>

    @Query("UPDATE exercise SET isSync = 1 WHERE id IN (:ids)")
    suspend fun markExercisesSynced(ids: List<Long>)
}
