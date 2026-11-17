package com.spadaval.lor.net

import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime

/*
 * Reading LoR's replies with Android's org.json: what DeviceHome's parser did before it moved to
 * homecore with a reader of its own (lor-xdoi). The lists and the task writes still read here.
 */

/** An ISO time with `Z` or an offset. */
internal fun time(s: String): Instant = OffsetDateTime.parse(s).toInstant()

/** [key]'s string, null when it is missing or JSON's null. */
internal fun str(o: JSONObject, key: String): String? = if (o.isNull(key)) null else o.optString(key)

/** `/device/home` from Android's org.json, as LorClient and the tests read it. */
fun DeviceHome.Companion.parse(o: JSONObject): DeviceHome = parse(o.toString())
