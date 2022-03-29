package com.udacity.asteroidradar

import android.app.Application
import android.os.Build
import androidx.work.*
import com.udacity.asteroidradar.worker.RefreshCacheWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AsteroidRadarApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            setupRefreshCacheWork()
        }
    }

    /**
     * Download (and save in cache database) today's asteroids in background once a day
     */
    private fun setupRefreshCacheWork() {
        val constraints = Constraints.Builder()
            // Only when using wifi
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            // Only when device is charging
            .setRequiresCharging(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setRequiresDeviceIdle(true)
                }
            }.build()

        // Repeat once a day
        val repeatingRequest = PeriodicWorkRequestBuilder<RefreshCacheWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance()
            .enqueueUniquePeriodicWork(
                // Give a name to make sure it's unique
                RefreshCacheWorker.WORK_NAME,
                // If 2 requests for the same unique work are enqueued, keep the previous one
                // (discard the new work request)
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
            )
    }
}