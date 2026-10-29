package com.fdev.nutrimindai.feature.dailyintake.data.seeder

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.FoodDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeder class providing pre-populated universal global & western food catalog in English.
 */
@Singleton
class DemoDataSeeder @Inject constructor(
    private val foodDao: FoodDao
) {
    suspend fun seedIfNeeded() {
        try {
            if (foodDao.getFoodCount() == 0) {
                seedFoodCatalog()
                Timber.d("DemoDataSeeder: Successfully seeded universal food catalog.")
            }
        } catch (e: Exception) {
            Timber.e(e, "DemoDataSeeder: Failed to seed food catalog")
        }
    }

    private suspend fun seedFoodCatalog() {
        foodDao.insertFoods(DefaultFoodCatalog.foods)
    }
}

