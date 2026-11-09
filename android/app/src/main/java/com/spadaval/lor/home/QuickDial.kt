package com.spadaval.lor.home

import com.spadaval.lor.net.DeviceHome
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Quick dial (lor-2imw), the drawer's first tab: what Sumeet likely wants now. Pure, so the JVM
 * tests reach all of it; HomeStore keeps the counts, DrawerScreen.kt draws the tab.
 *
 * An app's score is how often and how lately it was opened, weighted by how much of that falls in
 * the current slot: an hour band on a weekday or a weekend day. Each app keeps a count per slot,
 * every open adding 1 and every count halving each [AppOpens.HALF_LIFE_MS] (decision note on
 * lor-2imw: LaunchStats' per-launch decay fades a morning app by evening at usage-event volume).
 */

/** A slot's hour band, by the local hour it starts at; night wraps past midnight. */
enum class Band(val from: Int, val to: Int, val word: String) {
    MORNING(6, 11, "morning"), MIDDAY(11, 14, "midday"), AFTERNOON(14, 18, "afternoon"), EVENING(18, 22, "evening"), NIGHT(22, 6, "night");

    val hours: Int get() = if (to > from) to - from else to + 24 - from

    companion object {
        fun of(hour: Int): Band = entries.first { b -> if (b.to > b.from) hour in b.from until b.to else hour >= b.from || hour < b.to }
    }
}

/** When an open happened, as quick dial counts it: its band and whether the day was a weekend day. */
data class HourSlot(val band: Band, val weekend: Boolean) {
    val index: Int get() = band.ordinal * 2 + if (weekend) 1 else 0

    /** The slot's share of a week's hours: what an app opened at random times would give it. */
    val share: Double get() = band.hours / 24.0 * (if (weekend) 2 else 5) / 7.0

    /** "Sunday evening", for the tab's Now header. */
    fun words(at: Instant, zone: ZoneId): String = "${DateTimeFormatter.ofPattern("EEEE").format(at.atZone(zone))} ${band.word}"

    companion object {
        const val COUNT = 10

        fun of(at: Instant, zone: ZoneId): HourSlot {
            val t = at.atZone(zone)
            return HourSlot(Band.of(t.hour), t.dayOfWeek == DayOfWeek.SATURDAY || t.dayOfWeek == DayOfWeek.SUNDAY)
        }
    }
}

/**
 * Per-app open counts by slot, keyed by the app's ref (`pkg`, or `work:pkg`: a package, since
 * usage events name the package), decayed to [at]. [cursor] is the last usage event read and
 * [foreground] the package it brought up, so the next read counts only a change of app.
 */
class AppOpens(
    val counts: Map<String, DoubleArray> = emptyMap(),
    val at: Long = 0,
    /** When each app was last opened, for New's "not opened since installed". */
    val last: Map<String, Long> = emptyMap(),
    val cursor: Long = 0,
    val foreground: String? = null,
) {
    /** The counts decayed to [now] (never back in time). */
    private fun decayedTo(now: Long): Map<String, DoubleArray> {
        if (now <= at) return counts
        val f = Math.pow(0.5, (now - at).toDouble() / HALF_LIFE_MS)
        return counts.mapValues { (_, c) -> DoubleArray(c.size) { c[it] * f } }
    }

    /** Counts an open of [key] at [time] in [zone]; opens are counted in time order. */
    fun opened(key: String, time: Long, zone: ZoneId): AppOpens {
        val now = maxOf(time, at)
        val decayed = decayedTo(now).filterValues { it.sum() >= FLOOR }
        val c = (decayed[key] ?: DoubleArray(HourSlot.COUNT)).copyOf()
        c[HourSlot.of(Instant.ofEpochMilli(time), zone).index] += 1.0
        return AppOpens(decayed + (key to c), now, last + (key to maxOf(time, last[key] ?: 0)), cursor, foreground)
    }

    /** The cursor and foreground app after a read of usage events. */
    fun readTo(cursor: Long, foreground: String?) = AppOpens(counts, at, last, cursor, foreground)

    /**
     * How likely [key] is to be wanted at [now]: its decayed count in all ([total]) times its
     * smoothed share in now's slot, `(n + M·u) / (total + M)`, where u is the slot's share of the
     * week. M pseudo-opens spread evenly keep an app opened once from being ranked by that one.
     */
    fun score(key: String, now: Long, zone: ZoneId): Double {
        val c = counts[key] ?: return 0.0
        val f = if (now > at) Math.pow(0.5, (now - at).toDouble() / HALF_LIFE_MS) else 1.0
        val total = c.sum() * f
        val slot = HourSlot.of(Instant.ofEpochMilli(now), zone)
        return total * (c[slot.index] * f + SMOOTHING * slot.share) / (total + SMOOTHING)
    }

    fun toJson(): String = JSONObject()
        .put("version", 1)
        .put("at", at)
        .put("cursor", cursor)
        .apply { foreground?.let { put("foreground", it) } }
        .put("apps", JSONObject().apply {
            counts.forEach { (k, c) -> put(k, JSONObject().put("slots", JSONArray(c.toList())).put("last", last[k] ?: 0L)) }
        })
        .toString()

    companion object {
        /** Each count halves in two weeks: a habit a month old still counts, one dropped fades. */
        const val HALF_LIFE_MS = 14L * 24 * 60 * 60 * 1000
        /** Pseudo-opens spread over the week's slots (see [score]). */
        const val SMOOTHING = 4.0
        /** An app whose counts add to less than this is dropped from the file. */
        private const val FLOOR = 0.05

        /** Parses [text]; anything unreadable starts the counts again rather than failing the drawer. */
        fun parse(text: String): AppOpens = try {
            val root = JSONObject(text)
            val apps = root.getJSONObject("apps")
            val counts = HashMap<String, DoubleArray>()
            val last = HashMap<String, Long>()
            apps.keys().forEach { k ->
                val o = apps.getJSONObject(k)
                val s = o.getJSONArray("slots")
                if (s.length() == HourSlot.COUNT) {
                    counts[k] = DoubleArray(HourSlot.COUNT) { s.getDouble(it) }
                    last[k] = o.optLong("last")
                }
            }
            AppOpens(counts, root.getLong("at"), last, root.getLong("cursor"), root.optString("foreground").ifEmpty { null })
        } catch (e: Exception) {
            AppOpens()
        }
    }
}

/** One app brought to the foreground, as a usage event gives it: `ACTIVITY_RESUMED`'s package and time. */
data class Resumed(val pkg: String, val time: Long)

/**
 * Counts the opens in [events] (in time order, after [AppOpens.cursor]): a resume of a package
 * other than the one in front before it, except the [ignored] ones (LoR, the home app, the system
 * UI), which still count as being in front. [now] becomes the cursor.
 */
fun AppOpens.withUsage(events: List<Resumed>, ignored: Set<String>, now: Long, zone: ZoneId): AppOpens {
    var out = this
    var front = foreground
    events.filter { it.time > cursor }.forEach { e ->
        if (e.pkg != front && e.pkg !in ignored) out = out.opened(e.pkg, e.time, zone)
        front = e.pkg
    }
    return out.readTo(maxOf(cursor, now), front)
}

/** The ref quick dial counts [app] under: its package, `work:` before it for the work profile. */
fun opensKey(id: AppId): String = if (id.work) "work:${id.packageName}" else id.packageName

/**
 * The [most] [apps] with the highest [AppOpens.score] at [now], best first; apps never opened are
 * left out. Ties go to the order [apps] gives (label order).
 */
fun <T> rankForNow(apps: List<T>, id: (T) -> AppId, opens: AppOpens, now: Long, zone: ZoneId, most: Int = QUICK_APPS): List<T> =
    apps.map { it to opens.score(opensKey(id(it)), now, zone) }
        .filter { it.second > 0.0 }
        .sortedByDescending { it.second }
        .take(most)
        .map { it.first }

const val QUICK_APPS = 8

/**
 * A conversation shortcut as quick dial's people row reads it: [person] names who it is with
 * (the shortcut's label; Android keeps a shortcut's `Person` from launchers), [changed] is
 * `ShortcutInfo.lastChangedTimestamp`.
 */
data class Chat<T>(val person: String, val changed: Long, val value: T)

/** How far back a conversation counts as recent. */
const val PEOPLE_DAYS = 14L
const val PEOPLE = 4

/**
 * The people talked to most recently: [chats] changed within [PEOPLE_DAYS] of [now], newest first,
 * one per person (by name, ignoring case and spaces at the ends), at most [PEOPLE].
 */
fun <T> recentPeople(chats: List<Chat<T>>, now: Long, most: Int = PEOPLE): List<Chat<T>> {
    val since = now - PEOPLE_DAYS * 24 * 60 * 60 * 1000
    return chats.filter { it.changed >= since && it.person.isNotBlank() }
        .sortedByDescending { it.changed }
        .distinctBy { it.person.trim().lowercase() }
        .take(most)
}

/** What quick dial's card shows: `/device/home`'s one thing, greyed with the time it was read when LoR can't be read now. */
data class QuickCard(val kicker: String, val text: String, val stale: String?)

/** The card for [one], read at [readAt]; [current] as [com.spadaval.lor.net.LorReads.current]. Null without one. */
fun quickCard(one: DeviceHome.One?, current: Boolean, readAt: Instant?, zone: ZoneId): QuickCard? {
    one ?: return null
    val stale = if (current) null else "as of ${readAt?.let { DateTimeFormatter.ofPattern("H:mm").format(it.atZone(zone)) } ?: "earlier"}"
    return QuickCard(one.kicker, one.text, stale)
}

/** How long an app counts as new. */
const val NEW_FOR_MS = 3L * 24 * 60 * 60 * 1000

/** Installed at [installed] within [NEW_FOR_MS] of [now] and not opened since ([lastOpened], 0 for never). */
fun isNew(installed: Long, lastOpened: Long, now: Long): Boolean = now - installed < NEW_FOR_MS && lastOpened < installed
