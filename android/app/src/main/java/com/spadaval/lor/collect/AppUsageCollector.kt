package com.spadaval.lor.collect

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import com.spadaval.lor.data.Prefs
import org.json.JSONObject

/** App sessions from the system's usage events. Needs Usage access, granted in Settings. */
class AppUsageCollector(private val context: Context, private val prefs: Prefs) {

    fun permitted(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun collect(now: Long): Collected {
        val cursor = prefs.cursor(CURSOR)
        // First run looks back a day; the system keeps events for a few days at most.
        val from = (if (cursor > 0) cursor else now - DAY_MS).coerceAtLeast(now - 6 * DAY_MS) - OVERLAP_MS
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val events = mutableListOf<UsageEvent>()
        val it = usm.queryEvents(from, now)
        val e = UsageEvents.Event()
        while (it.hasNextEvent()) {
            it.getNextEvent(e)
            if (e.eventType in KEPT) events += UsageEvent(e.packageName ?: "", e.eventType, e.timeStamp)
        }
        val result = Sessions.build(events, ignore = ignored())
        val device = prefs.deviceName
        val items = result.closed.map { s ->
            val label = label(s.pkg)
            Obs.envelope(
                id = Obs.id("app", device, s.pkg, s.start),
                device = device,
                start = s.start,
                end = s.end,
                app = s.pkg,
                title = label,
                payload = JSONObject().put("kind", "app").put("package", s.pkg).put("label", label),
            )
        }
        return Collected(items) { prefs.setCursor(CURSOR, result.openSince ?: now) }
    }

    private val labels = mutableMapOf<String, String>()

    private fun label(pkg: String): String = labels.getOrPut(pkg) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    /** Time on the home screen and in the system UI is not time in an app. */
    private fun ignored(): Set<String> {
        val home = context.packageManager
            .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        return setOfNotNull(home, "com.android.systemui")
    }

    companion object {
        private const val CURSOR = "apps"
        private const val DAY_MS = 24 * 3_600_000L
        private const val OVERLAP_MS = 10 * 60_000L
        private val KEPT = setOf(
            Sessions.ACTIVITY_RESUMED, Sessions.ACTIVITY_PAUSED, Sessions.SCREEN_NON_INTERACTIVE,
            Sessions.KEYGUARD_SHOWN, Sessions.DEVICE_SHUTDOWN,
        )
    }
}
