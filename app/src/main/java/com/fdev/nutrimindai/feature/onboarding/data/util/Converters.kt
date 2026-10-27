package com.fdev.nutrimindai.feature.onboarding.data.util

import androidx.room.TypeConverter
import com.fdev.nutrimindai.feature.onboarding.util.ActivityLevel
import com.fdev.nutrimindai.feature.onboarding.util.Gender
import com.fdev.nutrimindai.feature.onboarding.util.Goal

/**
 * Room database TypeConverters for enum serialization and deserialization.
 */
class Converters {

    @TypeConverter
    fun fromGender(gender: Gender?): String? = gender?.name

    @TypeConverter
    fun toGender(value: String?): Gender? = value?.let {
        runCatching { Gender.valueOf(it) }.getOrDefault(Gender.PreferNotToSay)
    }

    @TypeConverter
    fun fromActivityLevel(activityLevel: ActivityLevel?): String? = activityLevel?.name

    @TypeConverter
    fun toActivityLevel(value: String?): ActivityLevel? = value?.let {
        runCatching { ActivityLevel.valueOf(it) }.getOrDefault(ActivityLevel.Sedentary)
    }

    @TypeConverter
    fun fromGoal(goal: Goal?): String? = goal?.name

    @TypeConverter
    fun toGoal(value: String?): Goal? = value?.let {
        runCatching { Goal.valueOf(it) }.getOrDefault(Goal.MaintainWeight)
    }

    @TypeConverter
    fun fromDailyLogStatus(status: com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus?): String? = status?.name

    @TypeConverter
    fun toDailyLogStatus(value: String?): com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus? = value?.let {
        runCatching { com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus.valueOf(it) }.getOrDefault(com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus.DRAFT)
    }

    @TypeConverter
    fun fromMealType(mealType: com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType?): String? = mealType?.name

    @TypeConverter
    fun toMealType(value: String?): com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType? = value?.let {
        runCatching { com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType.valueOf(it) }.getOrNull()
    }

    @TypeConverter
    fun fromFoodSource(foodSource: com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource?): String? = foodSource?.name

    @TypeConverter
    fun toFoodSource(value: String?): com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource? = value?.let {
        runCatching { com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource.valueOf(it) }.getOrNull()
    }

    @TypeConverter
    fun fromLocalDate(date: java.time.LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): java.time.LocalDate? = value?.let {
        runCatching { java.time.LocalDate.parse(it) }.getOrNull()
    }
}
