package com.fdev.nutrimindai.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Timber.d("AlarmReceiver: Received action $action")

        when (action) {
            ReminderScheduler.ACTION_MEAL_REMINDER -> {
                val mealType = intent.getStringExtra(ReminderScheduler.EXTRA_MEAL_TYPE) ?: "breakfast"
                handleMealReminder(context, mealType)
            }

            ReminderScheduler.ACTION_WATER_REMINDER -> {
                handleWaterReminder(context)
            }

            ReminderScheduler.ACTION_DAILY_REVIEW_REMINDER -> {
                handleDailyReviewReminder(context)
            }
        }
    }

    private fun handleMealReminder(context: Context, mealType: String) {
        val (title, message, notifId, reqCode) = when (mealType.lowercase()) {
            "breakfast" -> Quadruple(
                context.getString(R.string.notif_breakfast_title),
                context.getString(R.string.notif_breakfast_message),
                NotificationHelper.NOTIFICATION_ID_BREAKFAST,
                ReminderScheduler.REQ_CODE_BREAKFAST
            )
            "lunch" -> Quadruple(
                context.getString(R.string.notif_lunch_title),
                context.getString(R.string.notif_lunch_message),
                NotificationHelper.NOTIFICATION_ID_LUNCH,
                ReminderScheduler.REQ_CODE_LUNCH
            )
            "dinner" -> Quadruple(
                context.getString(R.string.notif_dinner_title),
                context.getString(R.string.notif_dinner_message),
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

        notificationHelper.showMealReminder(
            mealType = mealType,
            title = title,
            message = message,
            notificationId = notifId
        )

        // Reschedule for next day
        CoroutineScope(Dispatchers.IO).launch {
            val settings = preferenceManager.reminderSettings.first()
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
    }

    private fun handleWaterReminder(context: Context) {
        val messages = listOf(
            context.getString(R.string.notif_water_msg_1),
            context.getString(R.string.notif_water_msg_2),
            context.getString(R.string.notif_water_msg_3)
        )
        val selectedMessage = messages.random()

        notificationHelper.showWaterReminder(
            title = context.getString(R.string.notif_water_title),
            message = selectedMessage
        )

        // Schedule next water reminder
        CoroutineScope(Dispatchers.IO).launch {
            val settings = preferenceManager.reminderSettings.first()
            if (settings.waterEnabled) {
                val intervalMinutes = (settings.waterIntervalHours * 60).toInt().coerceAtLeast(30)
                reminderScheduler.scheduleNextWaterAlarm(
                    intervalMinutes = intervalMinutes,
                    startTimeStr = settings.waterStartTime,
                    endTimeStr = settings.waterEndTime
                )
            }
        }
    }

    private fun handleDailyReviewReminder(context: Context) {
        notificationHelper.showDailyReviewReminder(
            title = context.getString(R.string.notif_daily_review_title),
            message = context.getString(R.string.notif_daily_review_message)
        )

        // Reschedule for next day
        CoroutineScope(Dispatchers.IO).launch {
            val settings = preferenceManager.reminderSettings.first()
            if (settings.dailyReviewEnabled) {
                reminderScheduler.scheduleDailyAlarm(
                    timeString = settings.dailyReviewTime,
                    requestCode = ReminderScheduler.REQ_CODE_DAILY_REVIEW,
                    action = ReminderScheduler.ACTION_DAILY_REVIEW_REMINDER
                )
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
