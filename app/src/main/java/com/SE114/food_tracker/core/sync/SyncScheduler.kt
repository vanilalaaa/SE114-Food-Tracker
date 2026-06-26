package com.SE114.food_tracker.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<Sync>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "item_sync_periodic",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            syncRequest
        )
    }

    fun triggerImmediateSync(context: Context) {
        val immediateRequest = OneTimeWorkRequestBuilder<Sync>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "item_sync_immediate",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    /**
     * Flush queued (PENDING) chat messages. KEEP so an offline backlog coalesces onto one
     * worker; the CONNECTED constraint runs it as soon as the network returns.
     */
    fun enqueueMessageSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<MessageSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                BaseSyncWorker.BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "message_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}