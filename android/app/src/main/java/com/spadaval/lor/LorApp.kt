package com.spadaval.lor

import android.app.Application
import com.spadaval.lor.home.HomeStore
import com.spadaval.lor.live.LiveUpdates
import com.spadaval.lor.sync.SyncWorker

class LorApp : Application() {
    /**
     * Home's app list and layout file, made when home or Home settings first asks. Ask on the main
     * thread: the store registers a `LauncherApps.Callback`, which binds to the calling thread's looper.
     */
    val home: HomeStore by lazy { HomeStore(this) }

    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedule(this)
        LiveUpdates.start(this)
    }
}
