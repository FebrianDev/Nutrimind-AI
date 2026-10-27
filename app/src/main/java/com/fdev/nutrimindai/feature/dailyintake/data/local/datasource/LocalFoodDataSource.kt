package com.fdev.nutrimindai.feature.dailyintake.data.local.datasource

import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

interface LocalFoodDataSource {
    fun getAllFoods(): Flow<List<FoodEntity>>
    suspend fun getAllFoodsDirect(): List<FoodEntity>
    fun searchFoods(query: String): Flow<List<FoodEntity>>
    suspend fun searchFoodsDirect(query: String): List<FoodEntity>
    suspend fun findFoodByName(name: String): FoodEntity?
    suspend fun insertFoods(foods: List<FoodEntity>)
    suspend fun insertFood(food: FoodEntity): Long
    suspend fun getFoodCount(): Int
}
