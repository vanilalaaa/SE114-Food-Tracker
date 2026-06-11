package com.SE114.food_tracker.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for triggering background sync.
 *
 * Contract:
 * - Feature code never touches WorkManager directly; it calls these enqueue*()
 *   helpers. Each enqueues a unique-named one-time request with
 *   [ExistingWorkPolicy.KEEP], so repeated calls coalesce instead of stacking.
 * - Every worker runs under [BaseSyncWorker.SYNC_CONSTRAINTS] (network required)
 *   and retries with exponential backoff.
 * - [startInitialSync] runs once right after authentication: it schedules the
 *   recurring item pull and kicks off an immediate profile + item pass.
 * - Item sync stays owned by the diary feature (the existing [Sync] worker);
 *   [ProfileSyncWorker] is the reference implementation. New features add their
 *   own enqueue helper + [BaseSyncWorker] subclass following this same shape.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun enqueueItemSync() {
        workManager.enqueueUniqueWork(
            ITEM_SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            buildRequest<Sync>()
        )
    }

    fun enqueueProfileSync() {
        workManager.enqueueUniqueWork(
            PROFILE_SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            buildRequest<ProfileSyncWorker>()
        )
    }

    fun startInitialSync() {
        SyncScheduler.schedulePeriodicSync(context)
        enqueueProfileSync()
        enqueueItemSync()
    }

    private inline fun <reified W : ListenableWorker> buildRequest() =
        OneTimeWorkRequestBuilder<W>()
            .setConstraints(BaseSyncWorker.SYNC_CONSTRAINTS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BaseSyncWorker.BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

    companion object {
        private const val ITEM_SYNC_WORK = "sync_item"
        private const val PROFILE_SYNC_WORK = "sync_profile"
    }
}
