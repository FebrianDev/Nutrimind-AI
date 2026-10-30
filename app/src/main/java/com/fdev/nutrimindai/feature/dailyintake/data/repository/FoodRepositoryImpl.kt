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

    override suspend fun findFoodByName(name: String): Food? {
        return localDataSource.findFoodByName(name)?.toDomain()
    }

    override suspend fun insertFoods(foods: List<Food>) {
        val nonDuplicateEntities = foods.map { food ->
            val existing = localDataSource.findFoodByName(food.name)
            if (existing != null) {
                food.toEntity().copy(id = existing.id)
            } else {
                food.toEntity()
            }
        }
        localDataSource.insertFoods(nonDuplicateEntities)
    }

    override suspend fun insertFood(food: Food): String {
        val existing = localDataSource.findFoodByName(food.name)
        return if (existing != null) {
            val entity = food.toEntity().copy(id = existing.id)
            localDataSource.insertFood(entity)
            existing.id
        } else {
            val entity = food.toEntity()
            localDataSource.insertFood(entity)
            entity.id
        }
    }

    override suspend fun getFoodCount(): Int {
        return localDataSource.getFoodCount()
    }
}
