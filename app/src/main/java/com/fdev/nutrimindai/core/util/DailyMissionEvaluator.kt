package com.fdev.nutrimindai.core.util

import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import java.util.Locale

enum class MissionIconType {
    CALORIES,
    WATER,
    PROTEIN,
    EXERCISE,
    VEGETABLES,
    MEALS
}

data class DailyMissionItemSummary(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val iconType: MissionIconType,
    val progressText: String = "",
    val progressFraction: Float = 0f
)

data class EvaluatedDayMissions(
    val dateStr: String,
    val dayLabel: String,
    val totalMissions: Int = 5,
    val completedCount: Int = 0,
    val pointsEarned: Int = 0,
    val isPerfectDay: Boolean = false,
    val missionItems: List<DailyMissionItemSummary> = emptyList(),
    val overallProgressFraction: Float = 0f
)

data class MissionRewardBadge(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val maxProgress: Int,
    val iconType: String
)

data class MissionRewardData(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalPoints: Int = 0,
    val perfectDaysCount: Int = 0,
    val totalCompletedMissions: Int = 0,
    val adherencePercent: Int = 0,
    val history: List<EvaluatedDayMissions> = emptyList(),
    val badges: List<MissionRewardBadge> = emptyList()
)

object DailyMissionEvaluator {

    const val POINTS_PER_MISSION = 20
    const val BONUS_PERFECT_DAY = 50
    const val STREAK_THRESHOLD = 3

    fun evaluateDay(
        goal: Goal,
        calories: Int,
        goalCalories: Int,
        protein: Double,
        goalProtein: Int,
        waterMl: Int,
        goalWaterMl: Int,
        exerciseMinutes: Int,
        intakes: List<Intake>,
        dateStr: String,
        dayLabel: String
    ): EvaluatedDayMissions {
        val waterAchieved = waterMl >= (goalWaterMl * 0.9).toInt()
        val exerciseAchieved = exerciseMinutes >= 30
        val hasVegetables = intakes.any {
            it.foodName.contains("Vegetable", ignoreCase = true) ||
                    it.foodName.contains("Salad", ignoreCase = true) ||
                    it.foodName.contains("Sayur", ignoreCase = true) ||
                    it.foodName.contains("Spinach", ignoreCase = true) ||
                    it.foodName.contains("Broccoli", ignoreCase = true) ||
                    it.fiber >= 3.0
        }

        val waterProgressText = "${String.format(Locale.US, "%.1f", waterMl / 1000.0)} / ${String.format(Locale.US, "%.1f", goalWaterMl / 1000.0)} L"
        val waterFraction = (waterMl.toFloat() / goalWaterMl.coerceAtLeast(1)).coerceIn(0f, 1f)
        val exerciseProgressText = "$exerciseMinutes / 30 min"
        val exerciseFraction = (exerciseMinutes.toFloat() / 30f).coerceIn(0f, 1f)
        val vegProgressText = if (hasVegetables) "Logged in meals" else "0 / 1 serving"
        val vegFraction = if (hasVegetables) 1f else 0f

        val missions: List<DailyMissionItemSummary> = when (goal) {
            Goal.LoseWeight -> {
                val hasMeals = intakes.any { it.mealType == MealType.BREAKFAST || it.mealType == MealType.LUNCH }
                val calorieDeficitMet = (calories in 500..goalCalories) ||
                        (calories in 1..goalCalories && hasMeals)
                val proteinTargetMet = protein >= (goalProtein * 0.8)

                val calProgressText = "$calories / $goalCalories kcal"
                val calFraction = (calories.toFloat() / goalCalories.coerceAtLeast(1)).coerceIn(0f, 1f)
                val proteinProgressText = "${protein.toInt()} / ${goalProtein} g"
                val proteinFraction = (protein.toFloat() / goalProtein.coerceAtLeast(1)).coerceIn(0f, 1f)

                listOf(
                    DailyMissionItemSummary(
                        id = "calorie_deficit",
                        title = "Stay in Calorie Deficit",
                        description = "Target: max $goalCalories kcal",
                        isCompleted = calorieDeficitMet,
                        iconType = MissionIconType.CALORIES,
                        progressText = calProgressText,
                        progressFraction = calFraction
                    ),
                    DailyMissionItemSummary(
                        id = "water_hydration",
                        title = "Optimal Hydration",
                        description = "Target: ${String.format(Locale.US, "%.1f", goalWaterMl / 1000.0)}L water",
                        isCompleted = waterAchieved,
                        iconType = MissionIconType.WATER,
                        progressText = waterProgressText,
                        progressFraction = waterFraction
                    ),
                    DailyMissionItemSummary(
                        id = "protein_preservation",
                        title = "Preserve Lean Muscle",
                        description = "Target: ${goalProtein}g protein",
                        isCompleted = proteinTargetMet,
                        iconType = MissionIconType.PROTEIN,
                        progressText = proteinProgressText,
                        progressFraction = proteinFraction
                    ),
                    DailyMissionItemSummary(
                        id = "exercise_burn",
                        title = "Active Fat Burn (30 min)",
                        description = "Cardio, walk, or workout",
                        isCompleted = exerciseAchieved,
                        iconType = MissionIconType.EXERCISE,
                        progressText = exerciseProgressText,
                        progressFraction = exerciseFraction
                    ),
                    DailyMissionItemSummary(
                        id = "vegetables_fiber",
                        title = "Eat Fiber & Vegetables",
                        description = "Satiety & gut health in main meals",
                        isCompleted = hasVegetables,
                        iconType = MissionIconType.VEGETABLES,
                        progressText = vegProgressText,
                        progressFraction = vegFraction
                    )
                )
            }

            Goal.GainWeight -> {
                val calorieSurplusMet = calories >= (goalCalories * 0.9).toInt()
                val highProteinMet = protein >= (goalProtein * 0.85)
                val mainMealsCount = listOf(
                    intakes.any { it.mealType == MealType.BREAKFAST },
                    intakes.any { it.mealType == MealType.LUNCH },
                    intakes.any { it.mealType == MealType.DINNER }
                ).count { it }

                val calProgressText = "$calories / $goalCalories kcal"
                val calFraction = (calories.toFloat() / goalCalories.coerceAtLeast(1)).coerceIn(0f, 1f)
                val proteinProgressText = "${protein.toInt()} / ${goalProtein} g"
                val proteinFraction = (protein.toFloat() / goalProtein.coerceAtLeast(1)).coerceIn(0f, 1f)
                val mealsProgressText = "$mainMealsCount / 3 main meals"
                val mealsFraction = (mainMealsCount.toFloat() / 3f).coerceIn(0f, 1f)

                listOf(
                    DailyMissionItemSummary(
                        id = "calorie_surplus",
                        title = "Reach Caloric Surplus",
                        description = "Target: $goalCalories kcal",
                        isCompleted = calorieSurplusMet,
                        iconType = MissionIconType.CALORIES,
                        progressText = calProgressText,
                        progressFraction = calFraction
                    ),
                    DailyMissionItemSummary(
                        id = "protein_hypertrophy",
                        title = "High Protein Hypertrophy",
                        description = "Target: ${goalProtein}g protein",
                        isCompleted = highProteinMet,
                        iconType = MissionIconType.PROTEIN,
                        progressText = proteinProgressText,
                        progressFraction = proteinFraction
                    ),
                    DailyMissionItemSummary(
                        id = "main_meals",
                        title = "Log 3 Main Meals",
                        description = "Breakfast, lunch, & dinner",
                        isCompleted = mainMealsCount >= 3,
                        iconType = MissionIconType.MEALS,
                        progressText = mealsProgressText,
                        progressFraction = mealsFraction
                    ),
                    DailyMissionItemSummary(
                        id = "exercise_strength",
                        title = "Strength & Muscle Training",
                        description = "Resistance training / gym 30 min",
                        isCompleted = exerciseAchieved,
                        iconType = MissionIconType.EXERCISE,
                        progressText = exerciseProgressText,
                        progressFraction = exerciseFraction
                    ),
                    DailyMissionItemSummary(
                        id = "water_hydration",
                        title = "Muscle Hydration & Recovery",
                        description = "Target: ${String.format(Locale.US, "%.1f", goalWaterMl / 1000.0)}L water",
                        isCompleted = waterAchieved,
                        iconType = MissionIconType.WATER,
                        progressText = waterProgressText,
                        progressFraction = waterFraction
                    )
                )
            }

            Goal.MaintainWeight -> {
                val hasMeals = intakes.any { it.mealType == MealType.BREAKFAST || it.mealType == MealType.LUNCH }
                val calorieBalanceMet = calories in (goalCalories - 250)..(goalCalories + 250) && hasMeals
                val proteinBalancedMet = protein >= (goalProtein * 0.8)

                val calProgressText = "$calories / $goalCalories kcal"
                val calFraction = (calories.toFloat() / goalCalories.coerceAtLeast(1)).coerceIn(0f, 1f)
                val proteinProgressText = "${protein.toInt()} / ${goalProtein} g"
                val proteinFraction = (protein.toFloat() / goalProtein.coerceAtLeast(1)).coerceIn(0f, 1f)

                listOf(
                    DailyMissionItemSummary(
                        id = "calorie_balance",
                        title = "Maintain Energy Balance",
                        description = "Target: ~$goalCalories kcal",
                        isCompleted = calorieBalanceMet,
                        iconType = MissionIconType.CALORIES,
                        progressText = calProgressText,
                        progressFraction = calFraction
                    ),
                    DailyMissionItemSummary(
                        id = "water_hydration",
                        title = "Daily Vitality Hydration",
                        description = "Target: ${String.format(Locale.US, "%.1f", goalWaterMl / 1000.0)}L water",
                        isCompleted = waterAchieved,
                        iconType = MissionIconType.WATER,
                        progressText = waterProgressText,
                        progressFraction = waterFraction
                    ),
                    DailyMissionItemSummary(
                        id = "protein_daily",
                        title = "Reach Protein Target",
                        description = "Target: ${goalProtein}g protein",
                        isCompleted = proteinBalancedMet,
                        iconType = MissionIconType.PROTEIN,
                        progressText = proteinProgressText,
                        progressFraction = proteinFraction
                    ),
                    DailyMissionItemSummary(
                        id = "exercise_daily",
                        title = "Daily Active Movement",
                        description = "Stay active for 30 minutes",
                        isCompleted = exerciseAchieved,
                        iconType = MissionIconType.EXERCISE,
                        progressText = exerciseProgressText,
                        progressFraction = exerciseFraction
                    ),
                    DailyMissionItemSummary(
                        id = "vegetables_fiber",
                        title = "Eat Fresh Greens & Fiber",
                        description = "Essential vitamins & antioxidants",
                        isCompleted = hasVegetables,
                        iconType = MissionIconType.VEGETABLES,
                        progressText = vegProgressText,
                        progressFraction = vegFraction
                    )
                )
            }
        }

        val completedCount = missions.count { it.isCompleted }
        val isPerfectDay = completedCount == missions.size && missions.isNotEmpty()
        val pointsEarned = (completedCount * POINTS_PER_MISSION) + (if (isPerfectDay) BONUS_PERFECT_DAY else 0)
        val overallProgressFraction = if (missions.isNotEmpty()) {
            missions.map { it.progressFraction }.average().toFloat()
        } else 0f

        return EvaluatedDayMissions(
            dateStr = dateStr,
            dayLabel = dayLabel,
            totalMissions = missions.size,
            completedCount = completedCount,
            pointsEarned = pointsEarned,
            isPerfectDay = isPerfectDay,
            missionItems = missions,
            overallProgressFraction = overallProgressFraction
        )
    }

    fun calculateMissionRewards(historyDays: List<EvaluatedDayMissions>): MissionRewardData {
        if (historyDays.isEmpty()) return MissionRewardData()

        val totalCompletedMissions = historyDays.sumOf { it.completedCount }
        val totalPossibleMissions = historyDays.size * 5
        val adherencePercent = if (totalPossibleMissions > 0) {
            ((totalCompletedMissions.toFloat() / totalPossibleMissions) * 100).toInt()
        } else 0
        val totalPoints = historyDays.sumOf { it.pointsEarned }
        val perfectDaysCount = historyDays.count { it.isPerfectDay }

        val (currentStreak, bestStreak) = calculateStreaks(historyDays)

        val hydrationDays = historyDays.count { day ->
            day.missionItems.any { m -> m.iconType == MissionIconType.WATER && m.isCompleted }
        }
        val greensDays = historyDays.count { day ->
            day.missionItems.any { m -> m.iconType == MissionIconType.VEGETABLES && m.isCompleted }
        }

        val badges = listOf(
            MissionRewardBadge(
                id = "badge_first_step",
                title = "First Step",
                description = "Complete your first daily mission",
                isUnlocked = totalCompletedMissions >= 1,
                currentProgress = totalCompletedMissions.coerceAtMost(1),
                maxProgress = 1,
                iconType = "FIRST_STEP"
            ),
            MissionRewardBadge(
                id = "badge_perfect_day",
                title = "Perfect Day",
                description = "Complete all 5 daily missions in a single day",
                isUnlocked = perfectDaysCount >= 1,
                currentProgress = perfectDaysCount.coerceAtMost(1),
                maxProgress = 1,
                iconType = "PERFECT_DAY"
            ),
            MissionRewardBadge(
                id = "badge_streak_3",
                title = "3-Day Momentum",
                description = "Maintain a 3-day daily mission streak",
                isUnlocked = maxOf(currentStreak, bestStreak) >= 3,
                currentProgress = maxOf(currentStreak, bestStreak).coerceAtMost(3),
                maxProgress = 3,
                iconType = "STREAK_3"
            ),
            MissionRewardBadge(
                id = "badge_streak_7",
                title = "Consistency Master",
                description = "Maintain a 7-day daily mission streak",
                isUnlocked = maxOf(currentStreak, bestStreak) >= 7,
                currentProgress = maxOf(currentStreak, bestStreak).coerceAtMost(7),
                maxProgress = 7,
                iconType = "STREAK_7"
            ),
            MissionRewardBadge(
                id = "badge_hydration",
                title = "Hydration Hero",
                description = "Achieve optimal hydration on 5 separate days",
                isUnlocked = hydrationDays >= 5,
                currentProgress = hydrationDays.coerceAtMost(5),
                maxProgress = 5,
                iconType = "HYDRATION"
            ),
            MissionRewardBadge(
                id = "badge_greens",
                title = "Fiber & Greens Master",
                description = "Log greens or high fiber on 5 separate days",
                isUnlocked = greensDays >= 5,
                currentProgress = greensDays.coerceAtMost(5),
                maxProgress = 5,
                iconType = "GREENS"
            )
        )

        return MissionRewardData(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalPoints = totalPoints,
            perfectDaysCount = perfectDaysCount,
            totalCompletedMissions = totalCompletedMissions,
            adherencePercent = adherencePercent,
            history = historyDays.asReversed(),
            badges = badges
        )
    }

    private fun calculateStreaks(history: List<EvaluatedDayMissions>): Pair<Int, Int> {
        if (history.isEmpty()) return Pair(0, 0)

        // Best streak across entire history
        var bestStreak = 0
        var tempStreak = 0
        for (day in history) {
            if (day.completedCount >= STREAK_THRESHOLD) {
                tempStreak++
                if (tempStreak > bestStreak) bestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        // Current streak from newest day backwards
        val reversed = history.asReversed()
        val today = reversed.firstOrNull()
        var currentStreak = 0

        if (today != null) {
            if (today.completedCount >= STREAK_THRESHOLD) {
                for (day in reversed) {
                    if (day.completedCount >= STREAK_THRESHOLD) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            } else {
                // Today may still be in progress, so check starting from yesterday
                val pastDays = reversed.drop(1)
                for (day in pastDays) {
                    if (day.completedCount >= STREAK_THRESHOLD) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }
        }

        bestStreak = maxOf(bestStreak, currentStreak)
        return Pair(currentStreak, bestStreak)
    }
}

