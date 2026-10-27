package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalFoodDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toDomain
import com.fdev.nutrimindai.feature.dailyintake.data.mapper.toEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FoodRepositoryImpl @Inject constructor(
    private val localDataSource: LocalFoodDataSource
) : FoodRepository {

    override fun getAllFoods(): Flow<List<Food>> {
        return localDataSource.getAllFoods().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllFoodsDirect(): List<Food> {
        return localDataSource.getAllFoodsDirect().map { it.toDomain() }
    }

    override fun searchFoods(query: String): Flow<List<Food>> {
        return localDataSource.searchFoods(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun searchFoodsDirect(query: String): List<Food> {
        return localDataSource.searchFoodsDirect(query).map { it.toDomain() }
    }

    override suspend fun insertFoods(foods: List<Food>) {
        localDataSource.insertFoods(foods.map { it.toEntity() })
    }

    override suspend fun insertFood(food: Food): Long {
        return localDataSource.insertFood(food.toEntity())
    }

    override suspend fun getFoodCount(): Int {
        return localDataSource.getFoodCount()
    }
}
