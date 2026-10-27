package com.fdev.nutrimindai.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fdev.nutrimindai.MainActivity
import com.fdev.nutrimindai.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_MEAL_ID = "channel_nutrimind_meals"
        const val CHANNEL_MEAL_NAME = "Meal Reminders"
        const val CHANNEL_MEAL_DESC = "Reminders for breakfast, lunch, and dinner"

        const val CHANNEL_WATER_ID = "channel_nutrimind_water"
        const val CHANNEL_WATER_NAME = "Hydration Reminders"
        const val CHANNEL_WATER_DESC = "Periodic reminders to drink water and stay hydrated"

        const val CHANNEL_DAILY_REVIEW_ID = "channel_nutrimind_daily_review"
        const val CHANNEL_DAILY_REVIEW_NAME = "Daily Nutrition Review"
        const val CHANNEL_DAILY_REVIEW_DESC = "Evening reminder to complete and review today's nutrition score"

        const val NOTIFICATION_ID_BREAKFAST = 1001
        const val NOTIFICATION_ID_LUNCH = 1002
        const val NOTIFICATION_ID_DINNER = 1003
        const val NOTIFICATION_ID_WATER = 2001
        const val NOTIFICATION_ID_DAILY_REVIEW = 3001
        const val NOTIFICATION_ID_TEST = 9999

        const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
        const val NAV_DEST_BREAKFAST = "nav_breakfast"
        const val NAV_DEST_LUNCH = "nav_lunch"
        const val NAV_DEST_DINNER = "nav_dinner"
        const val NAV_DEST_WATER = "nav_water"
        const val NAV_DEST_DAILY_REVIEW = "nav_daily_review"
        const val NAV_DEST_CAMERA_BREAKFAST = "nav_camera_breakfast"
        const val NAV_DEST_CAMERA_LUNCH = "nav_camera_lunch"
        const val NAV_DEST_CAMERA_DINNER = "nav_camera_dinner"
        const val NAV_DEST_COACH = "nav_coach"
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mealChannel = NotificationChannel(
                CHANNEL_MEAL_ID,
                context.getString(R.string.notif_channel_meals_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_meals_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            val waterChannel = NotificationChannel(
                CHANNEL_WATER_ID,
                context.getString(R.string.notif_channel_water_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_water_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            val reviewChannel = NotificationChannel(
                CHANNEL_DAILY_REVIEW_ID,
                context.getString(R.string.notif_channel_daily_review_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_daily_review_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(mealChannel, waterChannel, reviewChannel))
            Timber.d("Notification channels registered successfully.")
        }
    }

    fun showMealReminder(
        mealType: String,
        title: String,
        message: String,
        notificationId: Int,
        isAlreadyLogged: Boolean = false
    ) {
        val navExtra = when (mealType.lowercase()) {
            "breakfast" -> NAV_DEST_BREAKFAST
            "lunch" -> NAV_DEST_LUNCH
            "dinner" -> NAV_DEST_DINNER
            else -> NAV_DEST_BREAKFAST
        }

        val cameraNavExtra = when (mealType.lowercase()) {
            "breakfast" -> NAV_DEST_CAMERA_BREAKFAST
            "lunch" -> NAV_DEST_CAMERA_LUNCH
            "dinner" -> NAV_DEST_CAMERA_DINNER
            else -> NAV_DEST_CAMERA_BREAKFAST
        }

        val contentPendingIntent = createContentPendingIntent(navExtra, notificationId)
        val cameraPendingIntent = createContentPendingIntent(cameraNavExtra, notificationId + 100)
        val coachPendingIntent = createContentPendingIntent(NAV_DEST_COACH, notificationId + 200)

        val builder = NotificationCompat.Builder(context, CHANNEL_MEAL_ID)
            .setSmallIcon(R.drawable.ic_stat_restaurant)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        if (!isAlreadyLogged) {
            val snapAction = NotificationCompat.Action.Builder(
                R.drawable.ic_stat_camera,
                context.getString(R.string.notif_action_snap_meal),
                cameraPendingIntent
            ).build()

            val logAction = NotificationCompat.Action.Builder(
                R.drawable.ic_stat_restaurant,
                context.getString(R.string.notif_action_log_meal),
                contentPendingIntent
            ).build()

            builder.addAction(snapAction).addAction(logAction)
        } else {
            val viewAction = NotificationCompat.Action.Builder(
                R.drawable.ic_stat_restaurant,
                context.getString(R.string.notif_action_view_meal),
                contentPendingIntent
            ).build()

            val coachAction = NotificationCompat.Action.Builder(
                R.drawable.ic_stat_review,
                context.getString(R.string.notif_action_ask_coach),
                coachPendingIntent
            ).build()

            builder.addAction(viewAction).addAction(coachAction)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun showWaterReminder(
        title: String,
        message: String,
        currentMl: Int = 0,
        targetMl: Int = 2500,
        quickLogAmount: Int = 250
    ) {
        val contentPendingIntent = createContentPendingIntent(NAV_DEST_WATER, NOTIFICATION_ID_WATER)

        val quickLogIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_QUICK_LOG_WATER
            putExtra(ReminderScheduler.EXTRA_AMOUNT_ML, quickLogAmount)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val quickLogPendingIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.REQ_CODE_QUICK_LOG_WATER,
            quickLogIntent,
            flags
        )

        val quickAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_water,
            "+$quickLogAmount ml",
            quickLogPendingIntent
        ).build()

        val openAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_water,
            context.getString(R.string.notif_action_open_tracker),
            contentPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_WATER_ID)
            .setSmallIcon(R.drawable.ic_stat_water)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setProgress(targetMl.coerceAtLeast(1), currentMl.coerceAtMost(targetMl), false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .addAction(quickAction)
            .addAction(openAction)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_WATER, notification)
    }

    fun showWaterLoggedConfirmation(
        addedMl: Int,
        totalMl: Int,
        targetMl: Int,
        quickLogAmount: Int = 250
    ) {
        val contentPendingIntent = createContentPendingIntent(NAV_DEST_WATER, NOTIFICATION_ID_WATER)

        val quickLogIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_QUICK_LOG_WATER
            putExtra(ReminderScheduler.EXTRA_AMOUNT_ML, quickLogAmount)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val quickLogPendingIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.REQ_CODE_QUICK_LOG_WATER,
            quickLogIntent,
            flags
        )

        val isGoalReached = totalMl >= targetMl
        val title = if (isGoalReached) {
            context.getString(R.string.notif_water_goal_reached_title)
        } else {
            context.getString(R.string.notif_water_quick_logged_title, addedMl)
        }

        val message = if (isGoalReached) {
            context.getString(R.string.notif_water_goal_reached_message, totalMl, targetMl)
        } else {
            val percent = if (targetMl > 0) (totalMl * 100) / targetMl else 0
            context.getString(R.string.notif_water_quick_logged_message, totalMl, targetMl, percent)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_WATER_ID)
            .setSmallIcon(R.drawable.ic_stat_water)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setProgress(targetMl.coerceAtLeast(1), totalMl.coerceAtMost(targetMl), false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        if (!isGoalReached) {
            val quickAction = NotificationCompat.Action.Builder(
                R.drawable.ic_stat_water,
                "+$quickLogAmount ml",
                quickLogPendingIntent
            ).build()
            builder.addAction(quickAction)
        }

        notificationManager.notify(NOTIFICATION_ID_WATER, builder.build())
    }

    fun showDailyReviewReminder(title: String, message: String) {
        val contentPendingIntent = createContentPendingIntent(NAV_DEST_DAILY_REVIEW, NOTIFICATION_ID_DAILY_REVIEW)
        val coachPendingIntent = createContentPendingIntent(NAV_DEST_COACH, NOTIFICATION_ID_DAILY_REVIEW + 100)

        val reviewAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_review,
            context.getString(R.string.notif_action_view_score),
            contentPendingIntent
        ).build()

        val coachAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_notification,
            context.getString(R.string.notif_action_ask_coach),
            coachPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REVIEW_ID)
            .setSmallIcon(R.drawable.ic_stat_review)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .addAction(reviewAction)
            .addAction(coachAction)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY_REVIEW, notification)
    }

    fun showTestNotification() {
        showWaterReminder(
            title = context.getString(R.string.notif_test_title),
            message = context.getString(R.string.notif_test_long_message),
            currentMl = 1250,
            targetMl = 2500,
            quickLogAmount = 250
        )
    }

    private fun createContentPendingIntent(destinationExtra: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            destinationExtra?.let { putExtra(EXTRA_NAVIGATE_TO, it) }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
