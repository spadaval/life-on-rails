package com.spadaval.lor.collect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import com.spadaval.lor.data.Prefs
import org.json.JSONObject

/** Calls from the call log: number, direction, length. Never audio. */
class CallCollector(private val context: Context, private val prefs: Prefs) {

    fun permitted(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    fun collect(now: Long): Collected {
        val cursor = prefs.cursor(CURSOR)
        val from = if (cursor > 0) cursor - OVERLAP_MS else now - 30 * DAY_MS
        val device = prefs.deviceName
        val items = mutableListOf<JSONObject>()
        var newest = cursor
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.CACHED_NAME),
            "${CallLog.Calls.DATE} > ?",
            arrayOf(from.toString()),
            "${CallLog.Calls.DATE} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val rowId = c.getLong(0)
                val number = c.getString(1) ?: ""
                val type = c.getInt(2)
                val date = c.getLong(3)
                val seconds = c.getLong(4)
                val name = c.getString(5)
                newest = maxOf(newest, date)
                if (number.isBlank()) continue // a withheld number
                val payload = JSONObject()
                    .put("kind", "call")
                    .put("direction", direction(type))
                    .put("phone", number)
                    .put("duration_s", seconds)
                if (!name.isNullOrBlank()) payload.put("name", name)
                items += Obs.envelope(Obs.id("call", device, rowId, date), device, date, date + seconds * 1000, "phone", name ?: number, payload)
            }
        }
        return Collected(items) { if (newest > cursor) prefs.setCursor(CURSOR, newest) }
    }

    private fun direction(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "in"
        CallLog.Calls.OUTGOING_TYPE -> "out"
        CallLog.Calls.MISSED_TYPE -> "missed"
        CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> "rejected"
        CallLog.Calls.VOICEMAIL_TYPE -> "voicemail"
        else -> "other"
    }

    companion object {
        private const val CURSOR = "calls"
        private const val DAY_MS = 24 * 3_600_000L
        private const val OVERLAP_MS = 60 * 60_000L
    }
}
