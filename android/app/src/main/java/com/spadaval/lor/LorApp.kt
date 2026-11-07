package com.spadaval.lor

import android.app.Application
import com.spadaval.lor.home.HomeStore
import com.spadaval.lor.live.LiveUpdates
import com.spadaval.lor.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LorApp : Application() {
    /**
     * Home's app list and layout file, made when home or Home settings first asks. Ask on the main
     * thread: the store registers a `LauncherApps.Callback`, which binds to the calling thread's looper.
     */
    val home: HomeStore by lazy { HomeStore(this) }

    /**
     * Work that must outlive the screen that started it, on the main thread: a card's call from the
     * agent page (F4), which leaving the page or a recreated home must not cancel.
     */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedule(this)
        LiveUpdates.start(this)
    }
}
