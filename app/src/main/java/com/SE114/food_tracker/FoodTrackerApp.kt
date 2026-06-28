package com.SE114.food_tracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FoodTrackerApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Without a planted tree every Timber.* call is a no-op, so caught errors never reach logcat.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    /**
     * App-wide Coil loader. Default Coil only kept images in the in-memory cache here, so on a cold
     * app restart (memory gone) every image had to re-download — and any hiccup left the list with
     * no images at all. A persistent on-disk cache fixes that; respectCacheHeaders(false) stops a
     * restrictive Supabase cache-control header from preventing the disk write. DebugLogger surfaces
     * any remaining image-load failure in logcat for debug builds.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
}
