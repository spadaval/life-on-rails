package com.spadaval.lor

import android.app.Application
import android.os.SystemClock
import android.util.Log
import com.spadaval.lor.agent.CardCalls
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.home.HomeStore
import com.spadaval.lor.live.LiveUpdates
import com.spadaval.lor.net.LorClient
import com.spadaval.lor.net.LorReads
import com.spadaval.lor.sync.SyncWorker
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

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

    /** The agent page's card calls, held for the process so a page built again sees one under way (lor-0fbs). */
    val cardCalls: CardCalls by lazy { CardCalls(scope) { T3Connection.describe(it) } }

    /**
     * LoR's `/device/home` for every screen that shows it (the agent page, the clock block, LoR's
     * blocks), read once a minute while one holds it (lor-leh4), and `/device/tasks` and
     * `/device/people` while a `lor` panel holds it (lor-lz97). The server and device name are read
     * at each read, so a change in settings takes at the next.
     */
    val reads: LorReads by lazy {
        val prefs = Prefs(this)
        LorReads(
            scope,
            fetchHome = { withContext(Dispatchers.IO) { LorClient(prefs.serverUrl).home(prefs.deviceName) } },
            fetchTasks = { withContext(Dispatchers.IO) { LorClient(prefs.serverUrl).tasks() } },
            fetchPeople = { withContext(Dispatchers.IO) { LorClient(prefs.serverUrl).people() } },
            describe = { LorClient.describe(it) },
            clock = SystemClock::elapsedRealtime,
            log = { Log.i("LorReads", it) },
        )
    }

    override fun onCreate() {
        super.onCreate()
        SyncWorker.schedule(this)
        LiveUpdates.start(this)
    }
}
