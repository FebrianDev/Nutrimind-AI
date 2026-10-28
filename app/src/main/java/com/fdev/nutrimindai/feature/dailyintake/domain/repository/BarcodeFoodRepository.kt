package com.fdev.nutrimindai.feature.dailyintake.domain.repository

import com.fdev.nutrimindai.core.ai.FoodRecognitionResult

interface BarcodeFoodRepository {
    suspend fun getFoodByBarcode(barcode: String): Result<FoodRecognitionResult>
}
