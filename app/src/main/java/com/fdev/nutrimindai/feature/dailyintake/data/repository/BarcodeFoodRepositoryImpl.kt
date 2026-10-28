package com.fdev.nutrimindai.feature.dailyintake.data.repository

import com.fdev.nutrimindai.core.ai.FoodRecognitionResult
import com.fdev.nutrimindai.feature.dailyintake.data.remote.api.OpenFoodFactsApi
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.BarcodeFoodRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.FoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeFoodRepositoryImpl @Inject constructor(
    private val openFoodFactsApi: OpenFoodFactsApi,
    private val foodRepository: FoodRepository
) : BarcodeFoodRepository {

    override suspend fun getFoodByBarcode(barcode: String): Result<FoodRecognitionResult> = withContext(Dispatchers.IO) {
        try {
            val response = openFoodFactsApi.getProductByBarcode(barcode.trim())
            val product = response.product

            if (response.status == 1 && product != null) {
                val rawName = product.productName?.takeIf { it.isNotBlank() }
                    ?: product.productNameEn?.takeIf { it.isNotBlank() }
                    ?: product.genericName?.takeIf { it.isNotBlank() }
                    ?: "Product $barcode"

                val brand = product.brands?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                val displayName = "$rawName$brand"

                val nutriments = product.nutriments

                val calories: Int
                val protein: Double
                val carbs: Double
                val fat: Double
                val fiber: Double
                val servingSize: Double
                val servingUnit: String

                val servingCal = nutriments?.energyKcalServing
                if (servingCal != null && servingCal > 0.0) {
                    calories = servingCal.toInt()
                    protein = nutriments.proteinsServing ?: 0.0
                    carbs = nutriments.carbohydratesServing ?: 0.0
                    fat = nutriments.fatServing ?: 0.0
                    fiber = nutriments.fiberServing ?: 0.0
                    servingSize = product.servingQuantity ?: 1.0
                    servingUnit = product.servingSize?.takeIf { it.isNotBlank() } ?: "serving"
                } else {
                    val kcal100 = nutriments?.energyKcal100g ?: nutriments?.energyKcal ?: 0.0
                    calories = kcal100.toInt()
                    protein = nutriments?.proteins100g ?: nutriments?.proteins ?: 0.0
                    carbs = nutriments?.carbohydrates100g ?: nutriments?.carbohydrates ?: 0.0
                    fat = nutriments?.fat100g ?: nutriments?.fat ?: 0.0
                    fiber = nutriments?.fiber100g ?: nutriments?.fiber ?: 0.0
                    servingSize = 100.0
                    servingUnit = "g"
                }

                val result = FoodRecognitionResult(
                    name = displayName,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    servingSize = servingSize,
                    servingUnit = servingUnit,
                    confidence = 0.98,
                    description = "Open Food Facts: $barcode"
                )

                Result.success(result)
            } else {
                // If not found in Open Food Facts, attempt to match local database or return not found
                val localMatches = foodRepository.searchFoodsDirect(barcode)
                if (localMatches.isNotEmpty()) {
                    val first = localMatches.first()
                    Result.success(
                        FoodRecognitionResult(
                            name = first.name,
                            calories = first.calories,
                            protein = first.protein,
                            carbs = first.carbs,
                            fat = first.fat,
                            fiber = 0.0,
                            servingSize = first.servingSize,
                            servingUnit = first.servingUnit,
                            confidence = 0.95,
                            description = "Local Database: $barcode"
                        )
                    )
                } else {
                    Result.failure(Exception("Product with barcode $barcode not found in food database."))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching product by barcode: $barcode")
            Result.failure(e)
        }
    }
}
