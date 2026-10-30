package com.fdev.nutrimindai.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.util.NutritionCalculator
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var dailyLogRepository: DailyLogRepository

    @Inject
    lateinit var waterRepository: WaterRepository

    @Inject
    lateinit var intakeRepository: IntakeRepository

    @Inject
    lateinit var exerciseRepository: ExerciseRepository

    @Inject
    lateinit var profileRepository: ProfileRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Timber.d("AlarmReceiver: Received action $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_MEAL_REMINDER -> {
                        val mealType = intent.getStringExtra(ReminderScheduler.EXTRA_MEAL_TYPE) ?: "breakfast"
                        handleMealReminder(context, mealType)
                    }

                    ReminderScheduler.ACTION_WATER_REMINDER -> {
                        handleWaterReminder(context)
                    }

                    ReminderScheduler.ACTION_QUICK_LOG_WATER -> {
                        val amountMl = intent.getIntExtra(ReminderScheduler.EXTRA_AMOUNT_ML, 250)
                        handleQuickLogWater(context, amountMl)
                    }

                    ReminderScheduler.ACTION_DAILY_REVIEW_REMINDER -> {
                        handleDailyReviewReminder(context)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "AlarmReceiver error handling action $action")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleQuickLogWater(context: Context, amountMl: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dailyLogId = dailyLogRepository.getOrCreateDailyLog(today)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val water = Water(
            dailyLogId = dailyLogId,
            amountMl = amountMl,
            time = timeStr
        )
        waterRepository.addWater(water)

        val profile = profileRepository.getProfileDirect()
        val exercises = exerciseRepository.getExercisesForDailyLogDirect(dailyLogId)
        val totalExerciseMin = exercises.sumOf { it.durationMinute }
        val targetWater = NutritionCalculator.calculateDynamicWaterTarget(
            weightKg = profile?.currentWeight ?: 70f,
            exerciseMinutes = totalExerciseMin
        )

        val waterRecords = waterRepository.getWaterRecordsForDailyLogDirect(dailyLogId)
        val totalWater = waterRecords.sumOf { it.amountMl }

        val settings = preferenceManager.reminderSettings.first()

        notificationHelper.showWaterLoggedConfirmation(
            addedMl = amountMl,
            totalMl = totalWater,
            targetMl = targetWater,
            quickLogAmount = settings.quickLogWaterAmountMl
        )
        Timber.d("Quick logged $amountMl ml water from notification! Total today: $totalWater ml / $targetWater ml")
    }

    private suspend fun handleMealReminder(context: Context, mealType: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dailyLog = dailyLogRepository.getDailyLogByDateDirect(today)
        val dailyLogId = dailyLog?.id

        val intakes = if (dailyLogId != null) {
            intakeRepository.getIntakesForDailyLogDirect(dailyLogId)
        } else emptyList()

        val mealIntakes = intakes.filter { it.mealType.name.equals(mealType, ignoreCase = true) }
        val isAlreadyLogged = mealIntakes.isNotEmpty()

        val settings = preferenceManager.reminderSettings.first()

        val (title, message, notifId, reqCode) = if (isAlreadyLogged) {
            val foodSummary = mealIntakes.joinToString(", ") { it.foodName }.take(40)
            val mealCalories = mealIntakes.sumOf { it.calories }
            when (mealType.lowercase()) {
                "breakfast" -> Quadruple(
                    context.getString(R.string.notif_breakfast_logged_title),
                    context.getString(R.string.notif_breakfast_logged_message, foodSummary, mealCalories),
                    NotificationHelper.NOTIFICATION_ID_BREAKFAST,
                    ReminderScheduler.REQ_CODE_BREAKFAST
                )
                "lunch" -> Quadruple(
                    context.getString(R.string.notif_lunch_logged_title),
                    context.getString(R.string.notif_lunch_logged_message, foodSummary, mealCalories),
                    NotificationHelper.NOTIFICATION_ID_LUNCH,
                    ReminderScheduler.REQ_CODE_LUNCH
                )
                "dinner" -> Quadruple(
                    context.getString(R.string.notif_dinner_logged_title),
                    context.getString(R.string.notif_dinner_logged_message, foodSummary, mealCalories),
                    NotificationHelper.NOTIFICATION_ID_DINNER,
                    ReminderScheduler.REQ_CODE_DINNER
                )
                else -> Quadruple(
                    context.getString(R.string.notif_meal_default_title),
                    context.getString(R.string.notif_meal_default_message),
                    NotificationHelper.NOTIFICATION_ID_BREAKFAST,
                    ReminderScheduler.REQ_CODE_BREAKFAST
                )
            }
        } else {
            val profile = profileRepository.getProfileDirect()
            val totalCaloriesConsumed = intakes.sumOf { it.calories }
            val targetCalories = profile?.dailyCalories ?: 2000
            val remainingCal = (targetCalories - totalCaloriesConsumed).coerceAtLeast(0)

            when (mealType.lowercase()) {
                "breakfast" -> Quadruple(
                    context.getString(R.string.notif_breakfast_title),
                    context.getString(R.string.notif_breakfast_message),
                    NotificationHelper.NOTIFICATION_ID_BREAKFAST,
                    ReminderScheduler.REQ_CODE_BREAKFAST
                )
                "lunch" -> Quadruple(
                    context.getString(R.string.notif_lunch_title),
                    context.getString(R.string.notif_meal_smart_prompt, "Lunch", remainingCal),
                    NotificationHelper.NOTIFICATION_ID_LUNCH,
                    ReminderScheduler.REQ_CODE_LUNCH
                )
                "dinner" -> Quadruple(
                    context.getString(R.string.notif_dinner_title),
                    context.getString(R.string.notif_meal_smart_prompt, "Dinner", remainingCal),
                    NotificationHelper.NOTIFICATION_ID_DINNER,
                    ReminderScheduler.REQ_CODE_DINNER
                )
                else -> Quadruple(
                    context.getString(R.string.notif_meal_default_title),
                    context.getString(R.string.notif_meal_default_message),
                    NotificationHelper.NOTIFICATION_ID_BREAKFAST,
                    ReminderScheduler.REQ_CODE_BREAKFAST
                )
            }
        }

        if (isAlreadyLogged && settings.smartSkipIfLogged) {
            Timber.d("AlarmReceiver: Meal $mealType already logged and smartSkipIfLogged is true. Skipping notification.")
        } else {
            notificationHelper.showMealReminder(
                mealType = mealType,
                title = title,
                message = message,
                notificationId = notifId,
                isAlreadyLogged = isAlreadyLogged
            )
        }

        // Reschedule for next day
        val timeStr = when (mealType.lowercase()) {
            "breakfast" -> settings.breakfastTime
            "lunch" -> settings.lunchTime
            "dinner" -> settings.dinnerTime
            else -> "08:00"
        }
        val enabled = when (mealType.lowercase()) {
            "breakfast" -> settings.breakfastEnabled
            "lunch" -> settings.lunchEnabled
            "dinner" -> settings.dinnerEnabled
            else -> false
        }

        if (enabled) {
            reminderScheduler.scheduleDailyAlarm(
                timeString = timeStr,
                requestCode = reqCode,
                action = ReminderScheduler.ACTION_MEAL_REMINDER,
                extraPair = Pair(ReminderScheduler.EXTRA_MEAL_TYPE, mealType)
            )
        }
    }

    private suspend fun handleWaterReminder(context: Context) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dailyLog = dailyLogRepository.getDailyLogByDateDirect(today)
        val dailyLogId = dailyLog?.id

        val profile = profileRepository.getProfileDirect()
        val exercises = if (dailyLogId != null) exerciseRepository.getExercisesForDailyLogDirect(dailyLogId) else emptyList()
        val totalExerciseMin = exercises.sumOf { it.durationMinute }
        val targetWater = NutritionCalculator.calculateDynamicWaterTarget(
            weightKg = profile?.currentWeight ?: 70f,
            exerciseMinutes = totalExerciseMin
        )

        val currentWater = if (dailyLogId != null) {
            waterRepository.getWaterRecordsForDailyLogDirect(dailyLogId).sumOf { it.amountMl }
        } else 0

        val settings = preferenceManager.reminderSettings.first()

        if (currentWater >= targetWater && settings.smartSkipIfLogged) {
            Timber.d("AlarmReceiver: Water target reached ($currentWater >= $targetWater ml) and smartSkipIfLogged is true. Skipping notification.")
        } else {
            val messages = listOf(
                context.getString(R.string.notif_water_msg_1),
                context.getString(R.string.notif_water_msg_2),
                context.getString(R.string.notif_water_msg_3)
            )
            val selectedMessage = messages.random()

            notificationHelper.showWaterReminder(
                title = context.getString(R.string.notif_water_title),
                message = selectedMessage,
                currentMl = currentWater,
                targetMl = targetWater,
                quickLogAmount = settings.quickLogWaterAmountMl
            )
        }

        // Schedule next water reminder
        if (settings.waterEnabled) {
            val intervalMinutes = (settings.waterIntervalHours * 60).toInt().coerceAtLeast(30)
            reminderScheduler.scheduleNextWaterAlarm(
                intervalMinutes = intervalMinutes,
                startTimeStr = settings.waterStartTime,
                endTimeStr = settings.waterEndTime
            )
        }
    }

    private suspend fun handleDailyReviewReminder(context: Context) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dailyLog = dailyLogRepository.getDailyLogByDateDirect(today)
        val dailyLogId = dailyLog?.id

        val intakes = if (dailyLogId != null) intakeRepository.getIntakesForDailyLogDirect(dailyLogId) else emptyList()
        val totalCalories = intakes.sumOf { it.calories }

        val totalWater = if (dailyLogId != null) {
            waterRepository.getWaterRecordsForDailyLogDirect(dailyLogId).sumOf { it.amountMl }
        } else 0

        val message = if (totalCalories > 0 || totalWater > 0) {
            context.getString(R.string.notif_daily_review_smart_message, totalCalories, totalWater)
        } else {
            context.getString(R.string.notif_daily_review_message)
        }

        notificationHelper.showDailyReviewReminder(
            title = context.getString(R.string.notif_daily_review_title),
            message = message
        )

        // Reschedule for next day
        val settings = preferenceManager.reminderSettings.first()
        if (settings.dailyReviewEnabled) {
            reminderScheduler.scheduleDailyAlarm(
                timeString = settings.dailyReviewTime,
                requestCode = ReminderScheduler.REQ_CODE_DAILY_REVIEW,
                action = ReminderScheduler.ACTION_DAILY_REVIEW_REMINDER
            )
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
