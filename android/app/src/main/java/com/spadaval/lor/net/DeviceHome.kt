package com.spadaval.lor.net

import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/** `GET /device/home`: what the agent page shows from LoR (`deviceHome` in `src/server/device.ts`). */
data class DeviceHome(
    val now: Instant,
    val zone: ZoneId,
    /** The one thing (`oneFrom`): a pin, the running timer, an event on or soon, the brief, or the oldest flag. */
    val one: One?,
    val needsYou: List<Need>,
    val day: Day,
    /** Current conditions where the phone last was (decision lor-g4ta); null without a key, a place, or after a failed fetch, and from an older server. */
    val weather: Weather?,
) {
    data class One(val kicker: String, val text: String, val why: String?) {
        /** `pickOne` names the running timer this way; the page draws the timer from [Day.running] instead. */
        val isTimer: Boolean get() = kicker.startsWith("Running since")
    }

    data class Need(val id: String, val title: String, val note: String?)

    data class Day(
        val wakeStart: Instant,
        val wakeEnd: Instant,
        val remainingFraction: Double,
        val running: Running?,
        /** Timed events not yet over, the one under way included; at most 5. */
        val events: List<Event>,
    )

    data class Running(val title: String, val start: Instant, val paused: Boolean)
    data class Event(val title: String, val start: Instant, val end: Instant)
    /** [tempC] in Celsius; [condition] is Google's words ("Partly cloudy"); [observedAt] is when Google measured it. */
    data class Weather(val tempC: Double, val condition: String, val isDay: Boolean, val observedAt: Instant)

    companion object {
        fun parse(o: JSONObject): DeviceHome {
            val day = o.getJSONObject("day")
            val needs = o.getJSONArray("needs_you")
            val events = day.getJSONArray("events")
            return DeviceHome(
                now = time(o.getString("now")),
                zone = ZoneId.of(o.getString("timezone")),
                one = o.optJSONObject("one")?.let { One(it.getString("kicker"), it.getString("text"), str(it, "why")) },
                needsYou = List(needs.length()) { i -> needs.getJSONObject(i).let { Need(it.getString("id"), it.getString("title"), str(it, "note")) } },
                day = Day(
                    wakeStart = time(day.getString("wake_start")),
                    wakeEnd = time(day.getString("wake_end")),
                    remainingFraction = day.getDouble("remaining_fraction"),
                    running = day.optJSONObject("running")?.let { Running(it.getString("title"), time(it.getString("start")), it.getBoolean("paused")) },
                    events = List(events.length()) { i ->
                        events.getJSONObject(i).let { Event(it.getString("title"), time(it.getString("start")), time(it.getString("end"))) }
                    },
                ),
                weather = o.optJSONObject("weather")?.let { Weather(it.getDouble("temp_c"), it.getString("condition"), it.getBoolean("is_day"), time(it.getString("observed_at"))) },
            )
        }

        /** An ISO time with `Z` or an offset. */
        private fun time(s: String): Instant = OffsetDateTime.parse(s).toInstant()

        private fun str(o: JSONObject, key: String): String? = if (o.isNull(key)) null else o.optString(key)
    }
}
