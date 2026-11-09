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
 * usage events name the package), decayed to [at]. [cursor] is when the last read of usage
 * events ended, [cursorClock] the `elapsedRealtime` then and [cursorBoot] the boot it was in
 * (`Settings.Global.BOOT_COUNT`), and [foreground] the package last brought up, so the next read
 * counts only a change of app.
 */
class AppOpens(
    val counts: Map<String, DoubleArray> = emptyMap(),
    val at: Long = 0,
    /** When each app was last opened, for New's "not opened since installed". */
    val last: Map<String, Long> = emptyMap(),
    val cursor: Long = 0,
    val foreground: String? = null,
    val cursorClock: Long = 0,
    val cursorBoot: Int = 0,
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
        return AppOpens(decayed + (key to c), now, last + (key to maxOf(time, last[key] ?: 0)), cursor, foreground, cursorClock, cursorBoot)
    }

    /** The cursor and foreground app after a read of usage events, at [clock] (`elapsedRealtime`) in boot [boot]. */
    fun readTo(cursor: Long, foreground: String?, clock: Long = 0, boot: Int = 0) = AppOpens(counts, at, last, cursor, foreground, clock, boot)

    /**
     * These opens with every stored time moved by however far the clock was set since the
     * cursor, read at wall time [now], `elapsedRealtime` [clock] and boot [boot]: the cursor, the
     * decay's [at] and [last]. Android moves every stored usage event by a clock change
     * (`UsageStatsDatabase.onTimeChanged`), so a cursor left alone would count events again after
     * a change forward and skip new ones after a change back, and a decay basis left in the
     * future would stop the counts decaying. Only within one boot: `elapsedRealtime` starts again
     * at a restart, so across one (or with no boot count) nothing moves. Moved once, the cursor
     * agrees with [clock] again, so moving twice changes nothing.
     */
    fun corrected(now: Long, clock: Long, boot: Int): AppOpens {
        if (cursor <= 0 || cursorClock <= 0 || boot <= 0 || boot != cursorBoot || clock < cursorClock) return this
        val jump = now - (cursor + (clock - cursorClock))
        if (Math.abs(jump) <= CLOCK_SET_MS) return this
        return AppOpens(counts, if (at > 0) at + jump else at, last.mapValues { it.value + jump }, cursor + jump, foreground, cursorClock, cursorBoot)
    }

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
        .put("cursorClock", cursorClock)
        .put("cursorBoot", cursorBoot)
        .apply { foreground?.let { put("foreground", it) } }
        .put("apps", JSONObject().apply {
            counts.forEach { (k, c) -> put(k, JSONObject().put("slots", JSONArray(c.toList())).put("last", last[k] ?: 0L)) }
        })
        .toString()

    companion object {
        /** Each count halves in two weeks: a habit a month old still counts, one dropped fades. */
        const val HALF_LIFE_MS = 14L * 24 * 60 * 60 * 1000
        /** A difference between wall time and `elapsedRealtime` past this is the clock being set, not drift. */
        private const val CLOCK_SET_MS = 60_000L
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
            AppOpens(counts, root.getLong("at"), last, root.getLong("cursor"), root.optString("foreground").ifEmpty { null }, root.optLong("cursorClock"), root.optInt("cursorBoot"))
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
 * UI), which still count as being in front. [now], [clock] (`elapsedRealtime`) and [boot] become
 * the cursor; the events start at the cursor [AppOpens.corrected] for a clock set since.
 */
fun AppOpens.withUsage(events: List<Resumed>, ignored: Set<String>, now: Long, clock: Long, boot: Int, zone: ZoneId): AppOpens {
    var out = corrected(now, clock, boot)
    var front = foreground
    val from = out.cursor
    events.filter { it.time > from }.forEach { e ->
        if (e.pkg != front && e.pkg !in ignored) out = out.opened(e.pkg, e.time, zone)
        front = e.pkg
    }
    return out.readTo(maxOf(from, now), front, clock, boot)
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
 * `ShortcutInfo.lastChangedTimestamp`, [manifest] that the app declared it in its manifest, as
 * Messages does its "New conversation" with the conversation category: a chat with someone is
 * always dynamic or cached. [rank] is the app's own order (`ShortcutInfo.rank`) and [app] the
 * app that published it (package and profile), whose ranks alone compare.
 */
data class Chat<T>(val person: String, val changed: Long, val value: T, val manifest: Boolean = false, val rank: Int = 0, val app: String = "")

/** How far back a conversation counts as recent. */
const val PEOPLE_DAYS = 14L
const val PEOPLE = 4

/**
 * The people talked to most recently: [chats] changed within [PEOPLE_DAYS] of [now], not from a
 * manifest, newest first (chats one app published together in its rank order),
 * one per person (by name, ignoring case and spaces at the ends), at most [PEOPLE].
 */
fun <T> recentPeople(chats: List<Chat<T>>, now: Long, most: Int = PEOPLE): List<Chat<T>> {
    val since = now - PEOPLE_DAYS * 24 * 60 * 60 * 1000
    val kept = chats.filter { it.changed >= since && it.person.isNotBlank() && !it.manifest }
    // Messages republishes every chat at once when one changes, a few milliseconds apart (and
    // maybe across a second's edge): a chat changed within [BATCH_MS] of its app's next newer one
    // is in that publish, which is in the app's own order and as new as its newest chat. Publishes
    // of different apps are ordered by time alone.
    val ordered = kept.groupBy { it.app }.values.flatMap { mine ->
        val batches = mutableListOf<MutableList<Chat<T>>>()
        mine.sortedByDescending { it.changed }.forEach { c ->
            val last = batches.lastOrNull()
            if (last != null && last.last().changed - c.changed < BATCH_MS) last.add(c) else batches.add(mutableListOf(c))
        }
        batches.flatMap { b -> b.sortedBy { it.rank }.mapIndexed { i, c -> Triple(b.first().changed, i, c) } }
    }.sortedWith(compareByDescending<Triple<Long, Int, Chat<T>>> { it.first }.thenBy { it.second }).map { it.third }
    return ordered
        .distinctBy { it.person.trim().lowercase() }
        .take(most)
}

/** How close two chats' changes are to count as one publish of the app's chats. */
private const val BATCH_MS = 1000L

/** What quick dial's card shows: `/device/home`'s one thing, greyed with the time it was read when LoR can't be read now. */
data class QuickCard(val kicker: String, val text: String, val stale: String?)

/** The card for [one], read at [readAt]; [current] when the reply is current and the last read didn't fail ([com.spadaval.lor.net.LorReads]). Null without one. */
fun quickCard(one: DeviceHome.One?, current: Boolean, readAt: Instant?, zone: ZoneId): QuickCard? {
    one ?: return null
    val stale = if (current) null else "as of ${readAt?.let { DateTimeFormatter.ofPattern("H:mm").format(it.atZone(zone)) } ?: "earlier"}"
    return QuickCard(one.kicker, one.text, stale)
}

/** How long an app counts as new. */
const val NEW_FOR_MS = 3L * 24 * 60 * 60 * 1000

/** Installed at [installed] within [NEW_FOR_MS] of [now] and not opened since ([lastOpened], 0 for never). */
fun isNew(installed: Long, lastOpened: Long, now: Long): Boolean = now - installed < NEW_FOR_MS && lastOpened < installed
