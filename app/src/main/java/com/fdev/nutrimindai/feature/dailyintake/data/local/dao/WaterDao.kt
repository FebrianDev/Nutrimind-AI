package com.fdev.nutrimindai.feature.dailyintake.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water ORDER BY createdAt DESC")
    fun getAllWaterRecords(): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water WHERE dailyLogId IN (:dailyLogIds) ORDER BY createdAt DESC")
    fun getWaterRecordsForDailyLogs(dailyLogIds: List<Long>): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water WHERE dailyLogId IN (:dailyLogIds) ORDER BY createdAt DESC")
    suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<Long>): List<WaterEntity>

    @Query("SELECT * FROM water WHERE dailyLogId = :dailyLogId ORDER BY createdAt DESC")
    fun getWaterRecordsForDailyLog(dailyLogId: Long): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water WHERE dailyLogId = :dailyLogId ORDER BY createdAt DESC")
    suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: Long): List<WaterEntity>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water WHERE dailyLogId = :dailyLogId")
    fun getTotalWater(dailyLogId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(water: WaterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterRecords(waters: List<WaterEntity>)

    @Delete
    suspend fun deleteWater(water: WaterEntity)

    @Query("SELECT COUNT(*) FROM water WHERE dailyLogId = :dailyLogId")
    suspend fun getWaterCount(dailyLogId: Long): Int

    @Query("SELECT * FROM water WHERE isSync = 0")
    suspend fun getUnsyncedWater(): List<WaterEntity>

    @Query("UPDATE water SET isSync = 1 WHERE id IN (:ids)")
    suspend fun markWaterSynced(ids: List<Long>)
}
