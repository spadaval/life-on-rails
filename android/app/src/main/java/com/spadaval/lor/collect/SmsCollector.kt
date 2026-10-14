package com.spadaval.lor.collect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import com.spadaval.lor.data.Prefs
import org.json.JSONObject

/**
 * Texts from the SMS store: who, which way, when, and what was said. The first
 * run reads the whole store. RCS chats live inside Google Messages and are not
 * visible here.
 */
class SmsCollector(private val context: Context, private val prefs: Prefs) {

    fun permitted(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun collect(now: Long): Collected {
        val cursor = prefs.cursor(CURSOR)
        val from = if (cursor > 0) cursor - OVERLAP_MS else 0L
        val device = prefs.deviceName
        val items = mutableListOf<JSONObject>()
        var newest = cursor
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.BODY),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(from.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val rowId = c.getLong(0)
                val address = c.getString(1) ?: ""
                val date = c.getLong(2)
                val direction = when (c.getInt(3)) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "in"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "out"
                    else -> null // drafts, outbox, failed
                }
                newest = maxOf(newest, date)
                if (direction == null || address.isBlank()) continue
                val payload = JSONObject().put("kind", "sms").put("direction", direction).put("phone", address)
                    .put("text", c.getString(4) ?: JSONObject.NULL)
                items += Obs.envelope(Obs.id("sms", device, rowId, date), device, date, date, "sms", address, payload)
            }
        }
        return Collected(items) { if (newest > cursor) prefs.setCursor(CURSOR, newest) }
    }

    companion object {
        // "texts" was the cursor when only who and when were kept; a new name re-reads everything with the text.
        private const val CURSOR = "texts_with_body"
        private const val OVERLAP_MS = 60 * 60_000L
    }
}
