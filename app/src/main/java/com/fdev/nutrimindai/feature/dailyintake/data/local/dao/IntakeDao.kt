package com.fdev.nutrimindai.feature.dailyintake.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeDao {
    @Query("SELECT * FROM intake ORDER BY createdAt DESC")
    fun getAllIntakes(): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM intake WHERE dailyLogId IN (:dailyLogIds) ORDER BY createdAt DESC")
    fun getIntakesForDailyLogs(dailyLogIds: List<Long>): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM intake WHERE dailyLogId IN (:dailyLogIds) ORDER BY createdAt DESC")
    suspend fun getIntakesForDailyLogsDirect(dailyLogIds: List<Long>): List<IntakeEntity>

    @Query("SELECT * FROM intake WHERE dailyLogId = :dailyLogId ORDER BY createdAt DESC")
    fun getIntakesForDailyLog(dailyLogId: Long): Flow<List<IntakeEntity>>

    @Query("SELECT * FROM intake WHERE dailyLogId = :dailyLogId ORDER BY createdAt DESC")
    suspend fun getIntakesForDailyLogDirect(dailyLogId: Long): List<IntakeEntity>

    @Query("SELECT * FROM intake WHERE dailyLogId = :dailyLogId AND mealType = :mealType ORDER BY createdAt DESC")
    fun getIntakesByMealType(dailyLogId: Long, mealType: String): Flow<List<IntakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntake(intake: IntakeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakes(intakes: List<IntakeEntity>)

    @Update
    suspend fun updateIntake(intake: IntakeEntity)

    @Delete
    suspend fun deleteIntake(intake: IntakeEntity)

    @Query("SELECT COUNT(*) FROM intake WHERE dailyLogId = :dailyLogId")
    suspend fun getIntakeCount(dailyLogId: Long): Int

    @Query("SELECT * FROM intake WHERE isSync = 0")
    suspend fun getUnsyncedIntakes(): List<IntakeEntity>

    @Query("UPDATE intake SET isSync = 1 WHERE id IN (:ids)")
    suspend fun markIntakesSynced(ids: List<Long>)
}
