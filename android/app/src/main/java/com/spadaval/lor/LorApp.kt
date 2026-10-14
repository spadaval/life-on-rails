package com.spadaval.lor

import android.app.Application
import com.spadaval.lor.sync.SyncWorker

class LorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedule(this)
    }
}
