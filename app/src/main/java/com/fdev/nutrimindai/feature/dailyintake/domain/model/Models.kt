package com.fdev.nutrimindai.feature.dailyintake.domain.model
import com.fdev.nutrimindai.core.model.Goal
import java.util.UUID

data class DailyLog(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val status: DailyLogStatus,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    val aiSummary: String? = null
)

data class Intake(
    val id: String = UUID.randomUUID().toString(),
    val dailyLogId: String,
    val mealType: MealType,
    val foodName: String,
    val servingSize: Double,
    val servingUnit: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val imagePath: String?,
    val foodSource: FoodSource,
    val createdAt: Long = 0L
)

data class Water(
    val id: String = UUID.randomUUID().toString(),
    val dailyLogId: String,
    val amountMl: Int,
    val time: String // Formatted time
)

data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val dailyLogId: String,
    val exerciseType: String,
    val durationMinute: Int,
    val caloriesBurned: Int,
    val note: String?,
    val createdAt: Long = 0L
)

data class NutritionSummary(
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val water: Int = 0,
    val exerciseCalories: Int = 0,
    val goalCalories: Int = DEFAULT_GOAL_CALORIES,
    val goalProtein: Int = DEFAULT_GOAL_PROTEIN,
    val goalCarbs: Int = DEFAULT_GOAL_CARBS,
    val goalFat: Int = DEFAULT_GOAL_FAT,
    val goalWater: Int = DEFAULT_GOAL_WATER
) {
    companion object {
        const val DEFAULT_GOAL_CALORIES = 2100
        const val DEFAULT_GOAL_PROTEIN = 120
        const val DEFAULT_GOAL_CARBS = 245
        const val DEFAULT_GOAL_FAT = 65
        const val DEFAULT_GOAL_WATER = 2500
    }
}



data class DailyMissionItem(
    val id: String,
    val title: String,
    val description: String,
    val currentProgressText: String,
    val isCompleted: Boolean,
    val progressFraction: Float = 0f
)

data class TodaySummary(
    val date: String,
    val dailyLogId: String?,
    val userName: String = "User",
    val userPhotoUrl: String? = null,
    val userGoal: Goal = Goal.MaintainWeight,
    val status: DailyLogStatus = DailyLogStatus.DRAFT,
    val breakfastList: List<Intake> = emptyList(),
    val lunchList: List<Intake> = emptyList(),
    val dinnerList: List<Intake> = emptyList(),
    val othersList: List<Intake> = emptyList(),
    val waterList: List<Water> = emptyList(),
    val waterTotalMl: Int = 0,
    val exerciseList: List<Exercise> = emptyList(),
    val nutritionSummary: NutritionSummary = NutritionSummary(),
    val currentWeightKg: Float? = null,
    val isWeightLoggedToday: Boolean = false
)
