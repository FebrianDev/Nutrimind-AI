package com.fdev.nutrimindai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fdev.nutrimindai.core.notification.NotificationHelper
import com.fdev.nutrimindai.core.notification.ReminderScheduler
import com.fdev.nutrimindai.core.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NutriMindApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        syncManager.schedulePeriodicSync()
        
        // Initialize Notification Channels and Active Reminders
        notificationHelper.createNotificationChannels()
        reminderScheduler.rescheduleAllActiveReminders()
    }
}
