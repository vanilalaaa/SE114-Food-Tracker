package com.SE114.food_tracker.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.SE114.food_tracker.core.network.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class ProfileSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val networkMonitor: NetworkMonitor
) : BaseSyncWorker(appContext, params) {

    // Reference worker: proves @HiltWorker + injected deps resolve through HiltWorkerFactory.
    // TODO(tv1): push/pull the profile row once ProfileRepository lands.
    override suspend fun doSync(): Result {
        val online = networkMonitor.isOnline.first()
        Timber.tag("Sync").d("ProfileSyncWorker ran (online=%s) — stub, nothing to sync yet", online)
        return Result.success()
    }
}
