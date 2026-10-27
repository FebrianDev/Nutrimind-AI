package com.fdev.nutrimindai.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestoreSyncRepository: FirestoreSyncRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("DataSyncWorker: Starting background sync task...")
        val result = firestoreSyncRepository.syncUnsyncedData()
        return if (result.success) {
            Timber.d("DataSyncWorker: Background sync completed successfully.")
            Result.success()
        } else {
            Timber.w("DataSyncWorker: Background sync failed: ${result.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "nutrimind_data_sync_work"
    }
}
