package com.fdev.nutrimindai.feature.coach.domain

import com.fdev.nutrimindai.core.sync.SyncManager
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.coach.domain.model.CoachAction
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Exercise
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes agentic actions extracted from NutriCoach by directly logging
 * food, water, workouts, and weight updates into Room database via their respective repositories.
 */
@Singleton
class CoachActionExecutor @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val intakeRepository: IntakeRepository,
    private val waterRepository: WaterRepository,
    private val exerciseRepository: ExerciseRepository,
    private val weightLogRepository: WeightLogRepository,
    private val profileRepository: ProfileRepository,
    private val actionParser: CoachActionParser,
    private val syncManager: SyncManager
) {

    /**
     * Executes the list of actions against today's daily log and user profile.
     *
     * @param actions The list of actions to execute
     * @return Formatted summary badge (e.g. "Logged to Diary: 2 Boiled Eggs (156 kcal) & +500 ml Water") or null if empty
     */
    suspend fun executeActions(actions: List<CoachAction>): String? {
        if (actions.isEmpty()) return null

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayLog = dailyLogRepository.getDailyLogByDateDirect(currentDate)
        val isTodayCompleted = todayLog?.status == DailyLogStatus.COMPLETED

        val dailyLogId = try {
            dailyLogRepository.getOrCreateDailyLog(currentDate)
        } catch (e: Exception) {
            Timber.e(e, "CoachActionExecutor: Failed to get/create daily log for $currentDate")
            return null
        }

        val executedActions = mutableListOf<CoachAction>()
        var skippedDueToCompleted = false

        for (action in actions) {
            when (action) {
                is CoachAction.LogFood -> {
                    if (isTodayCompleted) {
                        Timber.w("CoachActionExecutor: Cannot log food '${action.foodName}' because today's journal is COMPLETED.")
                        skippedDueToCompleted = true
                        continue
                    }
                    try {
                        val intake = Intake(
                            dailyLogId = dailyLogId,
                            mealType = action.mealType,
                            foodName = action.foodName,
                            servingSize = action.servingSize,
                            servingUnit = action.servingUnit,
                            calories = action.calories,
                            protein = action.protein,
                            carbs = action.carbs,
                            fat = action.fat,
                            fiber = action.fiber,
                            imagePath = null,
                            foodSource = FoodSource.MANUAL,
                            createdAt = System.currentTimeMillis()
                        )
                        intakeRepository.addIntake(intake)
                        executedActions.add(action)
                        Timber.d("CoachActionExecutor: Successfully logged food '${action.foodName}' (${action.calories} kcal) to daily log #$dailyLogId")
                    } catch (e: Exception) {
                        Timber.e(e, "CoachActionExecutor: Failed to log food '${action.foodName}'")
                    }
                }
                is CoachAction.LogWater -> {
                    if (isTodayCompleted) {
                        Timber.w("CoachActionExecutor: Cannot log water +${action.amountMl}ml because today's journal is COMPLETED.")
                        skippedDueToCompleted = true
                        continue
                    }
                    try {
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val water = Water(
                            dailyLogId = dailyLogId,
                            amountMl = action.amountMl,
                            time = currentTime
                        )
                        waterRepository.addWater(water)
                        executedActions.add(action)
                        Timber.d("CoachActionExecutor: Successfully logged water +${action.amountMl}ml to daily log #$dailyLogId")
                    } catch (e: Exception) {
                        Timber.e(e, "CoachActionExecutor: Failed to log water ${action.amountMl}ml")
                    }
                }
                is CoachAction.LogExercise -> {
                    if (isTodayCompleted) {
                        Timber.w("CoachActionExecutor: Cannot log exercise '${action.exerciseType}' because today's journal is COMPLETED.")
                        skippedDueToCompleted = true
                        continue
                    }
                    try {
                        val exercise = Exercise(
                            dailyLogId = dailyLogId,
                            exerciseType = action.exerciseType,
                            durationMinute = action.durationMinute,
                            caloriesBurned = action.caloriesBurned,
                            note = action.note,
                            createdAt = System.currentTimeMillis()
                        )
                        exerciseRepository.addExercise(exercise)
                        executedActions.add(action)
                        Timber.d("CoachActionExecutor: Successfully logged exercise '${action.exerciseType}' (${action.durationMinute}m, ${action.caloriesBurned} kcal) to daily log #$dailyLogId")
                    } catch (e: Exception) {
                        Timber.e(e, "CoachActionExecutor: Failed to log exercise '${action.exerciseType}'")
                    }
                }
                is CoachAction.LogWeight -> {
                    try {
                        weightLogRepository.logWeight(
                            date = currentDate,
                            weightKg = action.weightKg,
                            note = action.note
                        )
                        val profile = profileRepository.getProfileDirect()
                        if (profile != null) {
                            val plan = com.fdev.nutrimindai.core.util.NutritionCalculator.calculatePlan(
                                age = profile.age,
                                gender = profile.gender,
                                height = profile.height,
                                currentWeight = action.weightKg,
                                targetWeight = profile.targetWeight,
                                activityLevel = profile.activityLevel,
                                goal = profile.goal
                            )
                            profileRepository.updateProfile(
                                profile.copy(
                                    currentWeight = action.weightKg,
                                    dailyCalories = plan.dailyCalories,
                                    dailyProtein = plan.dailyProtein,
                                    dailyCarbs = plan.dailyCarbs,
                                    dailyFat = plan.dailyFat,
                                    dailyExerciseMinutes = plan.dailyExerciseMinutes,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                        executedActions.add(action)
                        Timber.d("CoachActionExecutor: Successfully logged weight ${action.weightKg} kg and updated profile")
                    } catch (e: Exception) {
                        Timber.e(e, "CoachActionExecutor: Failed to log weight ${action.weightKg} kg")
                    }
                }
            }
        }

        if (executedActions.isNotEmpty()) {
            syncManager.triggerManualSync()
        }

        return when {
            executedActions.isNotEmpty() -> actionParser.formatActionSummary(executedActions)
            skippedDueToCompleted -> "Today's journal is already completed (read-only). Reopen today in Activity to record meals."
            else -> null
        }
    }
}
