package com.fdev.nutrimindai.feature.dailyintake.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsResponse(
    val status: Int? = 0,
    @SerialName("status_verbose") val statusVerbose: String? = null,
    val code: String? = null,
    val product: OpenFoodFactsProduct? = null
)

@Serializable
data class OpenFoodFactsProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_en") val productNameEn: String? = null,
    @SerialName("generic_name") val genericName: String? = null,
    val brands: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_front_url") val imageFrontUrl: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null
)

@Serializable
data class OpenFoodFactsNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("energy-kcal_serving") val energyKcalServing: Double? = null,
    @SerialName("energy-kcal") val energyKcal: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("proteins_serving") val proteinsServing: Double? = null,
    @SerialName("proteins") val proteins: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("carbohydrates_serving") val carbohydratesServing: Double? = null,
    @SerialName("carbohydrates") val carbohydrates: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("fat_serving") val fatServing: Double? = null,
    @SerialName("fat") val fat: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("fiber_serving") val fiberServing: Double? = null,
    @SerialName("fiber") val fiber: Double? = null
)
