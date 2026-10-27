package com.fdev.nutrimindai.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firestoreSyncRepository: FirestoreSyncRepository
) {

    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context)
    }

    fun schedulePeriodicSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                DataSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Timber.d("SyncManager: Periodic sync scheduled every 6 hours.")
        } catch (e: Exception) {
            Timber.e(e, "SyncManager: Failed to schedule periodic sync")
        }
    }

    fun triggerManualSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                "manual_${DataSyncWorker.WORK_NAME}",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Timber.d("SyncManager: Manual sync triggered via WorkManager.")
        } catch (e: Exception) {
            Timber.e(e, "SyncManager: Failed to trigger manual sync")
        }
    }

    suspend fun runSyncNowDirectly(): SyncResult {
        triggerManualSync()
        return firestoreSyncRepository.syncUnsyncedData()
    }
}
