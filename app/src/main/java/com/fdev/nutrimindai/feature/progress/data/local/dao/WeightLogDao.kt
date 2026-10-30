package com.fdev.nutrimindai.feature.progress.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {

    @Query("SELECT * FROM weight_log_table ORDER BY date ASC, timestamp ASC")
    fun getAllWeightLogs(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log_table ORDER BY date ASC, timestamp ASC")
    suspend fun getAllWeightLogsDirect(): List<WeightLogEntity>

    @Query("SELECT * FROM weight_log_table WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
    fun getWeightLogByDate(date: String): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_log_table WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getWeightLogByDateDirect(date: String): WeightLogEntity?

    @Query("SELECT * FROM weight_log_table ORDER BY date DESC, timestamp DESC LIMIT 1")
    fun getLatestWeightLog(): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_log_table ORDER BY date DESC, timestamp DESC LIMIT 1")
    suspend fun getLatestWeightLogDirect(): WeightLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLogs(logs: List<WeightLogEntity>)

    @Query("DELETE FROM weight_log_table WHERE id = :id")
    suspend fun deleteWeightLog(id: Long)

    @Query("SELECT * FROM weight_log_table WHERE isSync = 0")
    suspend fun getUnsyncedWeightLogs(): List<WeightLogEntity>

    @Query("UPDATE weight_log_table SET isSync = 1 WHERE id IN (:ids)")
    suspend fun markWeightLogsSynced(ids: List<Long>)
}
