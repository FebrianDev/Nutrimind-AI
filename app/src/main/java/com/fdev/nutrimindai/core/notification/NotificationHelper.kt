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
    @ApplicationContext private val context: Context
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

    fun showMealReminder(mealType: String, title: String, message: String, notificationId: Int) {
        val navExtra = when (mealType.lowercase()) {
            "breakfast" -> NAV_DEST_BREAKFAST
            "lunch" -> NAV_DEST_LUNCH
            "dinner" -> NAV_DEST_DINNER
            else -> NAV_DEST_BREAKFAST
        }

        val pendingIntent = createContentPendingIntent(navExtra, notificationId)

        val notification = NotificationCompat.Builder(context, CHANNEL_MEAL_ID)
            .setSmallIcon(R.drawable.ic_stat_restaurant)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showWaterReminder(title: String, message: String) {
        val pendingIntent = createContentPendingIntent(NAV_DEST_WATER, NOTIFICATION_ID_WATER)

        val notification = NotificationCompat.Builder(context, CHANNEL_WATER_ID)
            .setSmallIcon(R.drawable.ic_stat_water)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_WATER, notification)
    }

    fun showDailyReviewReminder(title: String, message: String) {
        val pendingIntent = createContentPendingIntent(NAV_DEST_DAILY_REVIEW, NOTIFICATION_ID_DAILY_REVIEW)

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REVIEW_ID)
            .setSmallIcon(R.drawable.ic_stat_review)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY_REVIEW, notification)
    }

    fun showTestNotification() {
        val pendingIntent = createContentPendingIntent(null, NOTIFICATION_ID_TEST)

        val notification = NotificationCompat.Builder(context, CHANNEL_MEAL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            .setContentTitle(context.getString(R.string.notif_test_title))
            .setContentText(context.getString(R.string.notif_test_short_message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_test_long_message)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_TEST, notification)
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
