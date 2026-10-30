package com.fdev.nutrimindai.feature.dailyintake.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_table ORDER BY name ASC")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food_table ORDER BY name ASC")
    suspend fun getAllFoodsDirect(): List<FoodEntity>

    @Query("SELECT * FROM food_table WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchFoods(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food_table WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchFoodsDirect(query: String): List<FoodEntity>

    @Query("SELECT * FROM food_table WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findFoodByName(name: String): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Query("SELECT COUNT(*) FROM food_table")
    suspend fun getFoodCount(): Int

    @Query("SELECT * FROM food_table WHERE isSync = 0")
    suspend fun getUnsyncedFoods(): List<FoodEntity>

    @Query("UPDATE food_table SET isSync = 1 WHERE id IN (:ids)")
    suspend fun markFoodsSynced(ids: List<String>)
}
