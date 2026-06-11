package com.SE114.food_tracker.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters

/**
 * Template for sync workers: subclasses implement [doSync]; this base owns the
 * shared [androidx.work.ListenableWorker] plumbing. Pair with the request-level
 * [SYNC_CONSTRAINTS] and exponential backoff applied by [SyncManager].
 */
abstract class BaseSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    protected abstract suspend fun doSync(): Result

    final override suspend fun doWork(): Result = doSync()

    companion object {
        val SYNC_CONSTRAINTS: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        const val BACKOFF_DELAY_SECONDS = 10L
    }
}
