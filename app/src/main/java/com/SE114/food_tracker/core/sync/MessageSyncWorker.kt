package com.SE114.food_tracker.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.SE114.food_tracker.data.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Drains the local PENDING chat-message queue once connectivity is available. Enqueued on an
 * offline / connectivity-failed send and on app start; the NetworkType.CONNECTED constraint
 * (from [BaseSyncWorker.SYNC_CONSTRAINTS], applied by the scheduler) makes WorkManager run it
 * when the network returns. Idempotent — [ChatRepository.flushPendingMessages] upserts by local id.
 */
@HiltWorker
class MessageSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val chatRepository: ChatRepository
) : BaseSyncWorker(appContext, params) {

    override suspend fun doSync(): Result =
        if (chatRepository.flushPendingMessages()) Result.success() else Result.retry()
}
