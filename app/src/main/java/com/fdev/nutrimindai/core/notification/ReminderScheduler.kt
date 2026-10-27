package com.fdev.nutrimindai.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.datastore.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val notificationHelper: NotificationHelper
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val ACTION_MEAL_REMINDER = "com.fdev.nutrimindai.ACTION_MEAL_REMINDER"
        const val ACTION_WATER_REMINDER = "com.fdev.nutrimindai.ACTION_WATER_REMINDER"
        const val ACTION_DAILY_REVIEW_REMINDER = "com.fdev.nutrimindai.ACTION_DAILY_REVIEW_REMINDER"

        const val EXTRA_MEAL_TYPE = "extra_meal_type"

        const val REQ_CODE_BREAKFAST = 101
        const val REQ_CODE_LUNCH = 102
        const val REQ_CODE_DINNER = 103
        const val REQ_CODE_WATER = 201
        const val REQ_CODE_DAILY_REVIEW = 301
    }

    fun rescheduleAllActiveReminders() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = preferenceManager.reminderSettings.first()
                applyReminderSettings(settings)
                Timber.d("All active reminders successfully rescheduled.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reschedule all reminders")
            }
        }
    }

    fun applyReminderSettings(settings: ReminderSettings) {
        // 1. Breakfast
        if (settings.breakfastEnabled) {
            scheduleDailyAlarm(
                timeString = settings.breakfastTime,
                requestCode = REQ_CODE_BREAKFAST,
                action = ACTION_MEAL_REMINDER,
                extraPair = Pair(EXTRA_MEAL_TYPE, "breakfast")
            )
        } else {
            cancelAlarm(REQ_CODE_BREAKFAST, ACTION_MEAL_REMINDER)
        }

        // 2. Lunch
        if (settings.lunchEnabled) {
            scheduleDailyAlarm(
                timeString = settings.lunchTime,
                requestCode = REQ_CODE_LUNCH,
                action = ACTION_MEAL_REMINDER,
                extraPair = Pair(EXTRA_MEAL_TYPE, "lunch")
            )
        } else {
            cancelAlarm(REQ_CODE_LUNCH, ACTION_MEAL_REMINDER)
        }

        // 3. Dinner
        if (settings.dinnerEnabled) {
            scheduleDailyAlarm(
                timeString = settings.dinnerTime,
                requestCode = REQ_CODE_DINNER,
                action = ACTION_MEAL_REMINDER,
                extraPair = Pair(EXTRA_MEAL_TYPE, "dinner")
            )
        } else {
            cancelAlarm(REQ_CODE_DINNER, ACTION_MEAL_REMINDER)
        }

        // 4. Water Hydration
        if (settings.waterEnabled) {
            val intervalMinutes = (settings.waterIntervalHours * 60).toInt().coerceAtLeast(30)
            scheduleNextWaterAlarm(
                intervalMinutes = intervalMinutes,
                startTimeStr = settings.waterStartTime,
                endTimeStr = settings.waterEndTime
            )
        } else {
            cancelAlarm(REQ_CODE_WATER, ACTION_WATER_REMINDER)
        }

        // 5. Daily Review
        if (settings.dailyReviewEnabled) {
            scheduleDailyAlarm(
                timeString = settings.dailyReviewTime,
                requestCode = REQ_CODE_DAILY_REVIEW,
                action = ACTION_DAILY_REVIEW_REMINDER
            )
        } else {
            cancelAlarm(REQ_CODE_DAILY_REVIEW, ACTION_DAILY_REVIEW_REMINDER)
        }
    }

    fun scheduleNextWaterAlarm(intervalMinutes: Int, startTimeStr: String, endTimeStr: String) {
        if (alarmManager == null) return

        val now = Calendar.getInstance()
        val (startHour, startMin) = parseTime(startTimeStr, 8, 0)
        val (endHour, endMin) = parseTime(endTimeStr, 21, 0)

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val triggerCal = Calendar.getInstance()

        when {
            now.before(todayStart) -> {
                // Before active window today -> trigger at start window
                triggerCal.timeInMillis = todayStart.timeInMillis
            }
            now.after(todayEnd) -> {
                // After active window today -> trigger tomorrow at start window
                triggerCal.timeInMillis = todayStart.timeInMillis
                triggerCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            else -> {
                // Inside active window -> trigger after interval minutes
                triggerCal.add(Calendar.MINUTE, intervalMinutes)
                // If calculated time goes past end window, schedule for next day's start window
                if (triggerCal.after(todayEnd)) {
                    triggerCal.timeInMillis = todayStart.timeInMillis
                    triggerCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }

        val pendingIntent = createAlarmPendingIntent(REQ_CODE_WATER, intent)

        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerCal.timeInMillis,
                pendingIntent
            )
            Timber.d("Water reminder scheduled for: ${triggerCal.time}")
        } catch (e: SecurityException) {
            Timber.w("SecurityException scheduling exact water alarm: ${e.message}")
        }
    }

    fun scheduleDailyAlarm(
        timeString: String,
        requestCode: Int,
        action: String,
        extraPair: Pair<String, String>? = null
    ) {
        if (alarmManager == null) return

        val (hour, minute) = parseTime(timeString, 8, 0)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            extraPair?.let { putExtra(it.first, it.second) }
        }

        val pendingIntent = createAlarmPendingIntent(requestCode, intent)

        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Timber.d("Scheduled alarm [$action, req: $requestCode] for: ${calendar.time}")
        } catch (e: SecurityException) {
            Timber.w("SecurityException scheduling exact alarm: ${e.message}")
        }
    }

    fun cancelAlarm(requestCode: Int, action: String) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = createAlarmPendingIntent(requestCode, intent)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Timber.d("Cancelled alarm [$action, req: $requestCode]")
    }

    fun cancelAllReminders() {
        cancelAlarm(REQ_CODE_BREAKFAST, ACTION_MEAL_REMINDER)
        cancelAlarm(REQ_CODE_LUNCH, ACTION_MEAL_REMINDER)
        cancelAlarm(REQ_CODE_DINNER, ACTION_MEAL_REMINDER)
        cancelAlarm(REQ_CODE_WATER, ACTION_WATER_REMINDER)
        cancelAlarm(REQ_CODE_DAILY_REVIEW, ACTION_DAILY_REVIEW_REMINDER)
        Timber.d("All scheduled reminders cancelled.")
    }

    fun sendTestNotification() {
        notificationHelper.showTestNotification()
    }

    private fun parseTime(timeStr: String, fallbackHour: Int, fallbackMin: Int): Pair<Int, Int> {
        return try {
            val parts = timeStr.split(":")
            val h = parts[0].trim().toInt()
            val m = parts[1].trim().toInt()
            Pair(h, m)
        } catch (e: Exception) {
            Pair(fallbackHour, fallbackMin)
        }
    }

    private fun createAlarmPendingIntent(requestCode: Int, intent: Intent): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
