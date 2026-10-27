package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    fun getAllFoods(): Flow<List<Food>>
    suspend fun getAllFoodsDirect(): List<Food>
    fun searchFoods(query: String): Flow<List<Food>>
    suspend fun searchFoodsDirect(query: String): List<Food>
    suspend fun findFoodByName(name: String): Food?
    suspend fun insertFoods(foods: List<Food>)
    suspend fun insertFood(food: Food): Long
    suspend fun getFoodCount(): Int
}
