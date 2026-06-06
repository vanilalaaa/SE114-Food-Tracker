package com.SE114.food_tracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.SE114.food_tracker.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FoodTrackerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var supabaseClient: SupabaseClient

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        appScope.launch {
            runCatching {
                // NOTE: this hardcoded credential block is for testing only.
                // TODO: replace with your real auth flow (login screen) before release.
                supabaseClient.auth.signInWith(Email) {
                    email    = "test@gmail.com"
                    password = "123456"
                }
                Timber.d("FoodTrackerApp: sign-in successful, scheduling sync.")
            }.onSuccess {
                // Schedule periodic background sync (every 15 min, network required).
                SyncScheduler.schedulePeriodicSync(this@FoodTrackerApp)

                // BUG FIX: trigger an immediate one-shot sync right after login so any
                // items that were saved offline (PENDING in Room) are pushed to Supabase
                // without waiting for the 15-minute periodic window.
                SyncScheduler.triggerImmediateSync(this@FoodTrackerApp)
            }.onFailure { e ->
                Timber.e(e, "FoodTrackerApp: sign-in failed — sync NOT scheduled.")
                SyncScheduler.schedulePeriodicSync(this@FoodTrackerApp)
            }
        }
    }
}