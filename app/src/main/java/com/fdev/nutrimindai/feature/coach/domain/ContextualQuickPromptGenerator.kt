package com.fdev.nutrimindai.feature.coach.domain

import com.fdev.nutrimindai.feature.coach.domain.model.ContextualPrompt
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates dynamic, context-aware quick suggestion prompts for NutriCoach.
 * Prompts adapt automatically to the user's current time of day, remaining calorie/macro budget,
 * hydration level, and workout completion.
 */
@Singleton
class ContextualQuickPromptGenerator @Inject constructor(
    private val profileDao: ProfileDao,
    private val dailyLogDao: DailyLogDao,
    private val intakeDao: IntakeDao,
    private val waterDao: WaterDao,
    private val exerciseDao: ExerciseDao
) {

    /**
     * Retrieves a curated list of up to 5 dynamic prompts based on real-time diary status and time of day.
     *
     * @param overrideHour Optional hour of day (0-23) for unit testing or deterministic previews.
     */
    suspend fun getContextualPrompts(overrideHour: Int? = null): List<ContextualPrompt> {
        val hour = overrideHour ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val profile = profileDao.getProfileDirect()
        val dailyLog = dailyLogDao.getDailyLogByDateDirect(currentDate)

        val intakes = dailyLog?.let { intakeDao.getIntakesForDailyLogDirect(it.id) } ?: emptyList()
        val waters = dailyLog?.let { waterDao.getWaterRecordsForDailyLogDirect(it.id) } ?: emptyList()
        val exercises = dailyLog?.let { exerciseDao.getExercisesForDailyLogDirect(it.id) } ?: emptyList()

        val targetCal = profile?.dailyCalories ?: 2000
        val targetProtein = profile?.dailyProtein ?: 120
        val consumedCal = intakes.sumOf { it.calories }
        val consumedProtein = intakes.sumOf { it.protein }
        val remainingCal = targetCal - consumedCal
        val totalWater = waters.sumOf { it.amountMl }
        val totalExerciseMin = exercises.sumOf { it.durationMinute }

        val diaryPrompts = mutableListOf<ContextualPrompt>()

        // 1. Contextual evaluation based on today's logged data
        if (consumedCal > 0 && remainingCal in 1..400) {
            diaryPrompts.add(
                ContextualPrompt(
                    id = "rem_cal",
                    title = "Snack with $remainingCal kcal left",
                    promptText = "I only have $remainingCal calories remaining today. What is a light, satisfying snack option?",
                    emoji = "🍎"
                )
            )
        } else if (consumedCal >= targetCal && consumedCal > 0) {
            diaryPrompts.add(
                ContextualPrompt(
                    id = "met_cal",
                    title = "Met calorie budget",
                    promptText = "I have reached my daily calorie target. How should I pace my hydration and activity for the rest of today?",
                    emoji = "⚖️"
                )
            )
        }

        if (totalWater < 1200 && hour >= 13) {
            diaryPrompts.add(
                ContextualPrompt(
                    id = "hyd_boost",
                    title = "Hydration boost",
                    promptText = "I have only logged ${totalWater}ml of water so far today. What is an easy hydration strategy for this afternoon?",
                    emoji = "💧"
                )
            )
        }

        if (consumedProtein < (targetProtein * 0.45) && hour >= 12 && consumedCal > 0) {
            diaryPrompts.add(
                ContextualPrompt(
                    id = "prot_boost",
                    title = "Boost daily protein",
                    promptText = "My protein intake is low today (${consumedProtein.toInt()}/${targetProtein}g). What high-protein food should I add to my next meal?",
                    emoji = "🥩"
                )
            )
        }

        if (totalExerciseMin == 0 && hour in 10..20) {
            diaryPrompts.add(
                ContextualPrompt(
                    id = "quick_ex",
                    title = "15-min home workout",
                    promptText = "I have not logged any workouts today. Suggest a quick 15-minute home workout tailored to my fitness goal.",
                    emoji = "🏃"
                )
            )
        }

        // 2. Time-of-day contextual prompts
        val timePrompts = when (hour) {
            in 5..10 -> listOf(
                ContextualPrompt(
                    id = "m_brk",
                    title = "High-protein breakfast",
                    promptText = "What is a healthy, high-protein breakfast idea under 400 kcal to keep me full and energized?",
                    emoji = "🌅"
                ),
                ContextualPrompt(
                    id = "m_hyd",
                    title = "Morning hydration goal",
                    promptText = "How much water should I drink this morning to optimize energy and metabolism?",
                    emoji = "💧"
                ),
                ContextualPrompt(
                    id = "m_ene",
                    title = "Energizing morning meal",
                    promptText = "Suggest an energizing breakfast option that fits my target calories today.",
                    emoji = "⚡"
                )
            )
            in 11..15 -> listOf(
                ContextualPrompt(
                    id = "a_lun",
                    title = "Balanced lunch idea",
                    promptText = "What is a healthy lunch suggestion that fits within my remaining calorie budget today?",
                    emoji = "🥗"
                ),
                ContextualPrompt(
                    id = "a_slump",
                    title = "Beat afternoon slump",
                    promptText = "What healthy snack or habit can help me avoid the afternoon energy slump?",
                    emoji = "⚡"
                ),
                ContextualPrompt(
                    id = "a_walk",
                    title = "Post-lunch walk benefits",
                    promptText = "How does a quick 10-15 minute walk after lunch improve blood sugar and digestion?",
                    emoji = "🚶"
                )
            )
            in 16..20 -> listOf(
                ContextualPrompt(
                    id = "e_din",
                    title = "Low-carb dinner option",
                    promptText = "Suggest a nutritious, low-carb dinner that fits within my remaining calories tonight.",
                    emoji = "🍲"
                ),
                ContextualPrompt(
                    id = "e_rec",
                    title = "Post-workout recovery",
                    promptText = "What should I eat tonight for optimal muscle recovery after my workout?",
                    emoji = "🏋️"
                ),
                ContextualPrompt(
                    id = "e_mac",
                    title = "Daily macro check",
                    promptText = "Based on the meals I logged today, how are my calories and protein tracking against my goals?",
                    emoji = "📊"
                )
            )
            else -> listOf(
                ContextualPrompt(
                    id = "n_snk",
                    title = "Late-night snack < 150 kcal",
                    promptText = "Suggest a healthy, guilt-free late-night snack under 150 calories that won't disrupt digestion.",
                    emoji = "🌙"
                ),
                ContextualPrompt(
                    id = "n_slp",
                    title = "Deep sleep nutrition",
                    promptText = "What should I eat or drink tonight to promote restful deep sleep and recovery?",
                    emoji = "💤"
                ),
                ContextualPrompt(
                    id = "n_rev",
                    title = "Review today's progress",
                    promptText = "How did I do on my daily nutrition goals today and what can I improve tomorrow?",
                    emoji = "📈"
                )
            )
        }

        // Combine diary-specific prompts first (up to 2), then fill with time-of-day prompts
        val combined = mutableListOf<ContextualPrompt>()
        combined.addAll(diaryPrompts.take(2))
        for (tp in timePrompts) {
            if (combined.none { it.id == tp.id } && combined.size < 5) {
                combined.add(tp)
            }
        }

        return combined
    }
}
