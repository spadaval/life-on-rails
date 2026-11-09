package com.spadaval.lor.collect

import android.content.Context
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.data.Queue
import com.spadaval.lor.data.Sensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Where on the launcher an app was opened from: the `from` of a `kind: "launch"` observation. */
enum class LaunchFrom(val wire: String) {
    HOME("home"),
    FOLDER("folder"),
    /** The drawer with nothing typed. */
    DRAWER("drawer"),
    /** The drawer with something typed, including the keyboard's Go opening the first result. */
    SEARCH("search"),
    /** A card's own action on the agent page: Open on a failed or LoR card, a tap on a running turn. */
    AGENT_CARD("agent_card"),
    /** A card's icon on the agent page ("Open in T3", "Open in LoR"). */
    AGENT_ICON("agent_icon"),
}

/** What was opened, and the details of where from. */
data class Launch(
    val from: LaunchFrom,
    /** The package that was opened; null when nothing resolved it (a link with no app chosen). */
    val pkg: String?,
    val activity: String? = null,
    /** The opened app's name, the observation's title. */
    val label: String?,
    /** The folder's name, for [LaunchFrom.FOLDER]. */
    val folder: String? = null,
    /** What was typed, as typed, for [LaunchFrom.SEARCH]. */
    val query: String? = null,
    /** The T3 thread an agent card opened. */
    val thread: String? = null,
    /** The link an agent card opened (a T3 thread link, a LoR dashboard page). */
    val url: String? = null,
    /** The app shortcut's id, when one was opened from the app's long-press menu. */
    val shortcut: String? = null,
)

/**
 * Each launch from the launcher, as an observation (`source=android`, `payload.kind = "launch"`),
 * put in the sensor queue the moment it happens and uploaded with the next sync. It is an
 * instant, so it ends where it starts. The id comes from the device, the moment and what was
 * opened, and is made once: the queued copy is what every retry sends.
 */
object Launches {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observation(device: String, at: Long, launch: Launch): JSONObject {
        val payload = JSONObject().put("kind", "launch").put("from", launch.from.wire)
        launch.pkg?.let { payload.put("package", it) }
        launch.activity?.let { payload.put("activity", it) }
        launch.label?.let { payload.put("label", it) }
        launch.folder?.let { payload.put("folder", it) }
        launch.query?.let { payload.put("query", it) }
        launch.thread?.let { payload.put("thread", it) }
        launch.shortcut?.let { payload.put("shortcut", it) }
        return Obs.envelope(
            id = Obs.id(*listOfNotNull<Any>("launch", device, at, launch.pkg ?: "", launch.activity ?: launch.url ?: "", launch.shortcut).toTypedArray()),
            device = device,
            start = at,
            end = at,
            app = launch.pkg,
            title = launch.label,
            payload = payload,
        ).put("url", launch.url ?: JSONObject.NULL)
    }

    /** Queues [launch] off the main thread, unless the owner switched launches off. Never throws. */
    fun record(context: Context, launch: Launch) {
        val app = context.applicationContext
        val at = System.currentTimeMillis()
        scope.launch {
            try {
                val prefs = Prefs(app)
                if (!prefs.enabled(Sensor.LAUNCHES)) return@launch
                Queue(app).put(listOf(observation(prefs.deviceName, at, launch)))
            } catch (_: Exception) {
                // A lost launch record must never take home down with it.
            }
        }
    }

    /** The name of the app [pkg], or null when it isn't installed. */
    fun label(context: Context, pkg: String): String? = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        null
    }
}
