package com.fdev.nutrimindai.feature.coach.domain

import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.onboarding.data.dao.ProfileDao
import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType

/**
 * Builds a dynamic, context-rich system prompt for Meta Llama 3.3 70B,
 * injecting the user's live profile, daily meals, hydration, workouts, and weight progress.
 */
@Singleton
class NutriCoachContextBuilder @Inject constructor(
    private val profileDao: ProfileDao,
    private val dailyLogDao: DailyLogDao,
    private val intakeDao: IntakeDao,
    private val waterDao: WaterDao,
    private val exerciseDao: ExerciseDao,
    private val weightLogDao: WeightLogDao
) {

    suspend fun buildSystemPrompt(aiProviderName: String = "AI"): String {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val profile = profileDao.getProfileDirect()
        val dailyLog = dailyLogDao.getDailyLogByDateDirect(currentDate)

        val intakes = dailyLog?.let { intakeDao.getIntakesForDailyLogDirect(it.id) } ?: emptyList()
        val waters = dailyLog?.let { waterDao.getWaterRecordsForDailyLogDirect(it.id) } ?: emptyList()
        val exercises = dailyLog?.let { exerciseDao.getExercisesForDailyLogDirect(it.id) } ?: emptyList()
        val latestWeight = weightLogDao.getLatestWeightLogDirect()

        val name = profile?.name ?: "Pengguna"
        val goal = profile?.goal?.name ?: "MaintainWeight"
        val currentWeight = latestWeight?.weightKg ?: profile?.currentWeight ?: 70.0f
        val targetWeight = profile?.targetWeight ?: 65.0f

        val targetCal = profile?.dailyCalories ?: 2000
        val targetProtein = profile?.dailyProtein ?: 120
        val targetCarbs = profile?.dailyCarbs ?: 220
        val targetFat = profile?.dailyFat ?: 60
        val targetWater = 2500
        val targetExercise = profile?.dailyExerciseMinutes ?: 30

        val consumedCal = intakes.sumOf { it.calories }
        val consumedProtein = intakes.sumOf { it.protein }
        val consumedCarbs = intakes.sumOf { it.carbs }
        val consumedFat = intakes.sumOf { it.fat }
        val totalWater = waters.sumOf { it.amountMl }
        val totalExerciseMin = exercises.sumOf { it.durationMinute }
        val totalBurnedCal = exercises.sumOf { it.caloriesBurned }

        val remainingCal = targetCal - consumedCal
        val remainingProtein = targetProtein - consumedProtein
        val remainingCarbs = targetCarbs - consumedCarbs
        val remainingFat = targetFat - consumedFat

        val breakfastItems = intakes.filter { it.mealType == MealType.BREAKFAST }
        val lunchItems = intakes.filter { it.mealType == MealType.LUNCH }
        val dinnerItems = intakes.filter { it.mealType == MealType.DINNER }
        val snackItems = intakes.filter { it.mealType == MealType.OTHERS }

        fun formatMealList(items: List<com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity>): String {
            if (items.isEmpty()) return "Not logged yet"
            return items.joinToString(", ") { "${it.foodName} (${it.calories} kcal, P:${it.protein}g C:${it.carbs}g F:${it.fat}g)" }
        }

        return """
You are NutriCoach AI, a compassionate, supportive, and scientifically rigorous personal nutrition, wellness, and fitness coach powered by $aiProviderName.
You have direct, real-time access to the user's health diary and Room database for today ($currentDate).

[USER PROFILE & GOALS]
- Name: $name
- Primary Goal: $goal
- Current Weight: ${String.format(Locale.US, "%.1f", currentWeight)} kg | Target Weight: ${String.format(Locale.US, "%.1f", targetWeight)} kg
- Daily Target: $targetCal kcal (Protein: ${targetProtein}g, Carbs: ${targetCarbs}g, Fat: ${targetFat}g)
- Daily Water Target: $targetWater ml | Daily Exercise Target: $targetExercise minutes

[TODAY'S LOG PROGRESS ($currentDate)]
- Consumed Calories: $consumedCal / $targetCal kcal (${if (remainingCal >= 0) "$remainingCal kcal remaining" else "${-remainingCal} kcal over target"})
- Consumed Protein: ${String.format(Locale.US, "%.1f", consumedProtein)} / ${targetProtein}g
- Consumed Carbs: ${String.format(Locale.US, "%.1f", consumedCarbs)} / ${targetCarbs}g
- Consumed Fat: ${String.format(Locale.US, "%.1f", consumedFat)} / ${targetFat}g
- Hydration: $totalWater / $targetWater ml (${(totalWater * 100) / targetWater}%)
- Workouts: $totalExerciseMin mins, $totalBurnedCal kcal burned (${exercises.size} session(s))

[MEALS RECORDED TODAY]
- Breakfast: ${formatMealList(breakfastItems)}
- Lunch: ${formatMealList(lunchItems)}
- Dinner: ${formatMealList(dinnerItems)}
- Snacks/Others: ${formatMealList(snackItems)}

[COACHING PRINCIPLES]
1. Respond in English warmly, respectfully, and empathetically, matching the user's language style.
2. Provide personalized, context-aware suggestions directly addressing what the user has eaten, their remaining calories/macros, and their workout/hydration status today.
3. Keep responses structured, concise, and actionable using bullet points and emojis where helpful. Avoid overwhelmingly long essays unless the user explicitly asks for detailed explanations.
4. If recommending food or snacks, suggest realistic, accessible, and balanced healthy options while accurately noting estimated calories/macros.
5. Always encourage healthy habits, mindful eating, proper hydration, and consistency over perfection.
""".trimIndent()
    }
}
