package com.fdev.nutrimindai.feature.dailyintake.data.local.datasource

import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.FoodDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalFoodDataSourceImpl @Inject constructor(
    private val foodDao: FoodDao
) : LocalFoodDataSource {

    override fun getAllFoods(): Flow<List<FoodEntity>> {
        return foodDao.getAllFoods()
    }

    override suspend fun getAllFoodsDirect(): List<FoodEntity> {
        return foodDao.getAllFoodsDirect()
    }

    override fun searchFoods(query: String): Flow<List<FoodEntity>> {
        return foodDao.searchFoods(query)
    }

    override suspend fun searchFoodsDirect(query: String): List<FoodEntity> {
        return foodDao.searchFoodsDirect(query)
    }

    override suspend fun findFoodByName(name: String): FoodEntity? {
        return foodDao.findFoodByName(name)
    }

    override suspend fun insertFoods(foods: List<FoodEntity>) {
        foodDao.insertFoods(foods)
    }

    override suspend fun insertFood(food: FoodEntity): Long {
        return foodDao.insertFood(food)
    }

    override suspend fun getFoodCount(): Int {
        return foodDao.getFoodCount()
    }
}
