package com.fdev.nutrimindai.feature.dailyintake.domain.model

enum class DailyLogStatus {
    DRAFT,
    COMPLETED,
    MISSED
}

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    OTHERS
}

enum class FoodSource {
    CAMERA,
    GALLERY,
    SEARCH,
    MANUAL
}
