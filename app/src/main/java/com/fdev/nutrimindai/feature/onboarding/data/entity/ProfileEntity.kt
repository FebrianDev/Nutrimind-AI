package com.fdev.nutrimindai.feature.onboarding.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fdev.nutrimindai.feature.onboarding.util.ActivityLevel
import com.fdev.nutrimindai.feature.onboarding.util.Gender
import com.fdev.nutrimindai.feature.onboarding.util.Goal

/**
 * Room Entity representing the user's profile and baseline daily nutritional targets.
 *
 * @property id Unique identifier for the profile. Defaults to "user_profile".
 * @property name User's display name.
 * @property age User's age in years.
 * @property gender User's gender enum representation.
 * @property height User's height in centimeters.
 * @property currentWeight User's current weight in kilograms.
 * @property activityLevel User's physical activity level.
 * @property goal User's primary weight management target goal.
 * @property targetWeight User's target body weight in kilograms.
 * @property dailyCalories Recommended daily calorie intake in kcal.
 * @property dailyProtein Recommended daily protein intake in grams.
 * @property dailyCarbs Recommended daily carbohydrate intake in grams.
 * @property dailyFat Recommended daily fat intake in grams.
 * @property dailyExerciseMinutes Recommended daily physical activity in minutes.
 * @property createdAt Epoch timestamp when the profile was generated.
 * @property updatedAt Epoch timestamp when the profile was last modified.
 */
@Entity(tableName = "profile_table")
data class ProfileEntity(
    @PrimaryKey
    val id: String = DEFAULT_PROFILE_ID,
    val name: String,
    val age: Int,
    val gender: Gender,
    val height: Int,
    val currentWeight: Float,
    val activityLevel: ActivityLevel,
    val goal: Goal,
    val targetWeight: Float,
    val dailyCalories: Int,
    val dailyProtein: Int,
    val dailyCarbs: Int,
    val dailyFat: Int,
    val dailyExerciseMinutes: Int,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSync: Boolean = false
) {
    companion object {
        const val DEFAULT_PROFILE_ID = "user_profile"
    }
}
