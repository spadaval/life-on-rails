package com.spadaval.lor.collect

import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/** The observation envelope LoR stores for every sensor (src/shared/observation.ts). */
object Obs {
    /**
     * A stable id for something with an identity of its own (a call-log row,
     * an app session that began at a given moment), so uploading it twice is one row.
     */
    fun id(vararg parts: Any): String =
        UUID.nameUUIDFromBytes(("lor:" + parts.joinToString("|")).toByteArray()).toString()

    fun iso(epochMs: Long): String = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMs))

    fun envelope(
        id: String,
        device: String,
        start: Long,
        end: Long?,
        app: String?,
        title: String?,
        payload: JSONObject,
    ): JSONObject = JSONObject()
        .put("id", id)
        .put("t_start", iso(start))
        .put("t_end", if (end == null) JSONObject.NULL else iso(end))
        .put("device", device)
        .put("source", "android")
        .put("app", app ?: JSONObject.NULL)
        .put("calendar_id", JSONObject.NULL)
        .put("title", title ?: JSONObject.NULL)
        .put("url", JSONObject.NULL)
        .put("payload", payload)
        .put("intent_hint", JSONObject.NULL)
}

/** What one collector found, and where to resume next time once it is safely queued. */
data class Collected(val items: List<JSONObject>, val commit: () -> Unit = {})
