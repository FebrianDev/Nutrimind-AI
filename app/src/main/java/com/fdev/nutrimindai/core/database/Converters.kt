package com.fdev.nutrimindai.core.database

import androidx.room.TypeConverter
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import java.time.LocalDate

/**
 * Room database TypeConverters for enum and date serialization and deserialization.
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
    fun fromDailyLogStatus(status: DailyLogStatus?): String? = status?.name

    @TypeConverter
    fun toDailyLogStatus(value: String?): DailyLogStatus? = value?.let {
        runCatching { DailyLogStatus.valueOf(it) }.getOrDefault(DailyLogStatus.DRAFT)
    }

    @TypeConverter
    fun fromMealType(mealType: MealType?): String? = mealType?.name

    @TypeConverter
    fun toMealType(value: String?): MealType? = value?.let {
        runCatching { MealType.valueOf(it) }.getOrNull()
    }

    @TypeConverter
    fun fromFoodSource(foodSource: FoodSource?): String? = foodSource?.name

    @TypeConverter
    fun toFoodSource(value: String?): FoodSource? = value?.let {
        runCatching { FoodSource.valueOf(it) }.getOrNull()
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }
}
