package com.reelgrab

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Why `Configuration.Provider` + `HiltWorkerFactory`? `@HiltWorker` workers
 * (e.g. `DownloadWorker`) request injected dependencies via Hilt's
 * `WorkerAssistedFactory`. Returning the [HiltWorkerFactory] from
 * [workManagerConfiguration] is the contract WorkManager uses to construct
 * those workers without us touching reflection ourselves.
 */
@HiltAndroidApp
class ReelGrabApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
}
