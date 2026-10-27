package com.fdev.nutrimindai.core.model

/**
 * User gender representation for nutritional and BMR calculations.
 */
enum class Gender {
    Male,
    Female,
    PreferNotToSay
}

/**
 * User physical activity level for Total Daily Energy Expenditure (TDEE) calculation.
 */
enum class ActivityLevel {
    Sedentary,
    LightlyActive,
    ModeratelyActive,
    VeryActive
}

/**
 * Primary weight management goal of the user.
 */
enum class Goal {
    LoseWeight,
    MaintainWeight,
    GainWeight
}
