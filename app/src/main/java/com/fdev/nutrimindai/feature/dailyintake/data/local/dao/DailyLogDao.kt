package com.fdev.nutrimindai.feature.dailyintake.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_log WHERE date = :date LIMIT 1")
    fun getDailyLogByDate(date: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_log WHERE date = :date LIMIT 1")
    suspend fun getDailyLogByDateDirect(date: String): DailyLogEntity?

    @Query("SELECT * FROM daily_log ORDER BY date DESC")
    fun getAllDailyLogs(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_log ORDER BY date DESC")
    suspend fun getAllDailyLogsDirect(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyLog(dailyLog: DailyLogEntity): Long

    @Update
    suspend fun updateDailyLog(dailyLog: DailyLogEntity)

    @Query("UPDATE daily_log SET aiSummary = :aiSummary, isSync = 0 WHERE id = :dailyLogId")
    suspend fun updateAiSummary(dailyLogId: Long, aiSummary: String)

    @Query("SELECT COUNT(*) FROM daily_log")
    suspend fun getDailyLogCount(): Int

    @Query("SELECT * FROM daily_log WHERE date < :currentDate AND status = 'DRAFT'")
    suspend fun getPastDraftLogs(currentDate: String): List<DailyLogEntity>

    @Query("SELECT * FROM daily_log WHERE isSync = 0")
    suspend fun getUnsyncedDailyLogs(): List<DailyLogEntity>

    @Query("UPDATE daily_log SET isSync = 1 WHERE id IN (:ids)")
    suspend fun markDailyLogsSynced(ids: List<Long>)
}
