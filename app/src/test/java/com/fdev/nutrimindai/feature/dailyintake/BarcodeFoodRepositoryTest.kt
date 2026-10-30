package com.fdev.nutrimindai.feature.dailyintake

import com.fdev.nutrimindai.feature.dailyintake.data.remote.api.OpenFoodFactsApi
import com.fdev.nutrimindai.feature.dailyintake.data.remote.dto.OpenFoodFactsNutriments
import com.fdev.nutrimindai.feature.dailyintake.data.remote.dto.OpenFoodFactsProduct
import com.fdev.nutrimindai.feature.dailyintake.data.remote.dto.OpenFoodFactsResponse
import com.fdev.nutrimindai.feature.dailyintake.data.repository.BarcodeFoodRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.FoodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BarcodeFoodRepositoryTest {

    private lateinit var api: OpenFoodFactsApi
    private lateinit var foodRepository: FoodRepository
    private lateinit var repository: BarcodeFoodRepositoryImpl

    @Before
    fun setup() {
        api = mock()
        foodRepository = mock()
        repository = BarcodeFoodRepositoryImpl(api, foodRepository)
    }

    @Test
    fun `getFoodByBarcode returns parsed food when found in OpenFoodFacts with serving values`() = runTest {
        val fakeResponse = OpenFoodFactsResponse(
            status = 1,
            product = OpenFoodFactsProduct(
                code = "8992753210111",
                productName = "Susu UHT Cokelat",
                brands = "Ultra Milk",
                servingSize = "200 ml",
                servingQuantity = 1.0,
                nutriments = OpenFoodFactsNutriments(
                    energyKcalServing = 160.0,
                    proteinsServing = 6.0,
                    carbohydratesServing = 24.0,
                    fatServing = 4.5,
                    fiberServing = 1.0
                )
            )
        )
        whenever(api.getProductByBarcode("8992753210111")).thenReturn(fakeResponse)

        val result = repository.getFoodByBarcode("8992753210111")
        assertTrue(result.isSuccess)
        val food = result.getOrThrow()
        assertEquals("Susu UHT Cokelat (Ultra Milk)", food.name)
        assertEquals(160, food.calories)
        assertEquals(6.0, food.protein, 0.01)
        assertEquals(24.0, food.carbs, 0.01)
        assertEquals(4.5, food.fat, 0.01)
        assertEquals(1.0, food.fiber, 0.01)
    }

    @Test
    fun `getFoodByBarcode falls back to 100g values when serving values missing`() = runTest {
        val fakeResponse = OpenFoodFactsResponse(
            status = 1,
            product = OpenFoodFactsProduct(
                code = "737628064502",
                productName = "Rolled Oats",
                brands = "Quaker",
                nutriments = OpenFoodFactsNutriments(
                    energyKcal100g = 370.0,
                    proteins100g = 13.0,
                    carbohydrates100g = 68.0,
                    fat100g = 7.0,
                    fiber100g = 10.0
                )
            )
        )
        whenever(api.getProductByBarcode("737628064502")).thenReturn(fakeResponse)

        val result = repository.getFoodByBarcode("737628064502")
        assertTrue(result.isSuccess)
        val food = result.getOrThrow()
        assertEquals("Rolled Oats (Quaker)", food.name)
        assertEquals(370, food.calories)
        assertEquals(13.0, food.protein, 0.01)
        assertEquals("g", food.servingUnit)
        assertEquals(100.0, food.servingSize, 0.01)
    }

    @Test
    fun `getFoodByBarcode falls back to local database when OpenFoodFacts returns status 0`() = runTest {
        val notFoundResponse = OpenFoodFactsResponse(status = 0, product = null)
        whenever(api.getProductByBarcode("123456")).thenReturn(notFoundResponse)
        whenever(foodRepository.searchFoodsDirect("123456")).thenReturn(
            listOf(
                Food(
                    id = "food-1",
                    name = "Local Banana",
                    calories = 105,
                    protein = 1.3,
                    carbs = 27.0,
                    fat = 0.3
                )
            )
        )

        val result = repository.getFoodByBarcode("123456")
        assertTrue(result.isSuccess)
        val food = result.getOrThrow()
        assertEquals("Local Banana", food.name)
        assertEquals(105, food.calories)
    }
}
