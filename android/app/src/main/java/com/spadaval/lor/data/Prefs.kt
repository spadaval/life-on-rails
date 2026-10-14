package com.spadaval.lor.data

import android.content.Context
import android.os.Build
import androidx.core.content.edit

/** The sensors, each of which the owner can switch off. */
enum class Sensor(val key: String, val label: String, val unit: String, val description: String, val defaultOn: Boolean) {
    APPS("apps", "App usage", "app sessions", "Which app was on screen, and for how long.", true),
    CALLS("calls", "Calls", "calls", "Who you called or who called you, and for how long. Never audio.", true),
    TEXTS("texts", "Texts", "texts", "Who you texted and when. Never what was said. RCS chats are not visible to other apps.", true),
    HEALTH("health", "Sleep, exercise, steps", "health records", "Read from Health Connect.", true),
    LOCATION("location", "Location", "locations", "Where the phone was, every 15 minutes or so.", false),
}

/** Settings and sync state. Small, so SharedPreferences. */
class Prefs(context: Context) {
    private val p = context.getSharedPreferences("lor", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = p.getString("server_url", null) ?: DEFAULT_SERVER
        set(value) = p.edit { putString("server_url", value.trim().trimEnd('/')) }

    var deviceName: String
        get() = p.getString("device_name", null) ?: defaultDeviceName()
        set(value) = p.edit { putString("device_name", value.trim().lowercase().replace(Regex("[^a-z0-9-]+"), "-")) }

    fun enabled(sensor: Sensor): Boolean = p.getBoolean("on_${sensor.key}", sensor.defaultOn)
    fun setEnabled(sensor: Sensor, on: Boolean) = p.edit { putBoolean("on_${sensor.key}", on) }

    /** Where a collector got to, in epoch milliseconds. 0 = never ran. */
    fun cursor(name: String): Long = p.getLong("cursor_$name", 0L)
    fun setCursor(name: String, value: Long) = p.edit { putLong("cursor_$name", value) }

    var lastSyncAt: Long
        get() = p.getLong("last_sync_at", 0L)
        set(value) = p.edit { putLong("last_sync_at", value) }

    /** One line about the last sync, for the Sync screen. */
    var lastSyncSummary: String
        get() = p.getString("last_sync_summary", null) ?: "Never synced."
        set(value) = p.edit { putString("last_sync_summary", value) }

    var lastSyncErrors: String
        get() = p.getString("last_sync_errors", null) ?: ""
        set(value) = p.edit { putString("last_sync_errors", value) }

    var lastUploadAt: Long
        get() = p.getLong("last_upload_at", 0L)
        set(value) = p.edit { putLong("last_upload_at", value) }

    var uploadedTotal: Long
        get() = p.getLong("uploaded_total", 0L)
        set(value) = p.edit { putLong("uploaded_total", value) }

    companion object {
        const val DEFAULT_SERVER = "http://buildbot.tail47515a.ts.net:3847"

        /** "Pixel 9a" becomes "pixel-9a", the phone's name on the tailnet. */
        fun defaultDeviceName(): String = Build.MODEL.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "android" }
    }
}
