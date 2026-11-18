package com.spadaval.lor.concepts.timeline

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.lab.Fixtures
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lor.LorHome
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/*
 * The day as the route draws it. The phone's `/device/home` carries the day's events and the
 * running timer, not what was recorded before now, and Timeline's route needs that: the blocks
 * below are the demo set's afternoon (prototype/dashboard/src/data/snapshot.json, the same day the
 * dashboard's Timeline draws), up to the last one before the phone's 15:00. A real Timeline phone
 * needs `/device/home` to send them. A state at another moment of the day (the empty day at 09:00)
 * has recorded nothing, and one where LoR was never read has nothing to draw.
 */

enum class Origin(val words: String) { TIMER("Timer"), CALENDAR("Calendar"), PHONE("Phone"), PLACE("Place") }

/**
 * A block of recorded time. [label] is what it counts toward; [counted] once he (or a timer with
 * a label) said so; [guess] when the label is the agent's proposal waiting on a yes.
 */
data class DayBlock(
    val id: String,
    val start: Instant,
    val end: Instant,
    val origin: Origin,
    val source: String?,
    val label: String?,
    val counted: Boolean,
    val guess: Boolean = false,
) {
    enum class Tone { TRACKED, PROPOSED, BARE }

    val tone: Tone get() = when {
        counted && label != null -> Tone.TRACKED
        guess && label != null -> Tone.PROPOSED
        else -> Tone.BARE
    }

    /** What the block is called on the route: a calendar event keeps its own name; a counted block its label. */
    val title: String get() = when {
        origin == Origin.CALENDAR && source != null -> source
        tone == Tone.TRACKED -> label!!
        else -> source ?: "Unlabelled"
    }

    /** Where it came from, in a few words. */
    val from: String get() = when (origin) {
        Origin.TIMER -> "Timer"
        Origin.CALENDAR -> "Calendar"
        Origin.PHONE -> "Phone"
        Origin.PLACE -> "Place"
    }
}

private fun b(id: String, start: String, end: String, origin: Origin, source: String?, label: String?, counted: Boolean, guess: Boolean = false) =
    DayBlock(id, Instant.parse("2026-11-17T$start:00Z"), Instant.parse("2026-11-17T$end:00Z"), origin, source, label, counted, guess)

/** The demo set's afternoon, in UTC: 06:20 to 14:13 in New York. */
private val demoDay = listOf(
    b("b01", "11:20", "12:00", Origin.PLACE, "At Home", null, false),
    b("b02", "12:00", "12:35", Origin.TIMER, null, "Draft the offline maps spec", true),
    b("b03", "12:35", "12:55", Origin.TIMER, null, "Portuguese lesson", true),
    b("b04", "12:58", "13:12", Origin.PHONE, "Instagram", null, false),
    b("b05", "13:15", "13:25", Origin.PHONE, "Duolingo", "Hold a conversation in Portuguese", false, guess = true),
    b("b06", "13:25", "14:10", Origin.PLACE, "At Home", null, false),
    b("b07", "14:10", "14:24", Origin.PHONE, "Slack", null, false),
    b("b08", "14:24", "14:30", Origin.PLACE, "At Studio", "Ship the Field Guide app", false, guess = true),
    b("b09", "14:30", "14:45", Origin.CALENDAR, "Field Guide standup", "Meetings", false, guess = true),
    b("b10", "14:45", "15:15", Origin.PLACE, "At Studio", "Ship the Field Guide app", false, guess = true),
    b("b11", "15:15", "17:05", Origin.TIMER, null, "Offline maps", true),
    b("b12", "17:05", "17:20", Origin.PLACE, "At Studio", "Ship the Field Guide app", false, guess = true),
    b("b13", "17:30", "18:30", Origin.CALENDAR, "Lunch with Priya", "Meetings", false, guess = true),
    b("b14", "18:30", "18:40", Origin.PLACE, "At Studio", "Ship the Field Guide app", false, guess = true),
    b("b15", "18:40", "18:58", Origin.TIMER, null, "Public beta", true),
    b("b16", "19:00", "19:13", Origin.CALENDAR, "Beta design review", "Meetings", false, guess = true),
)

/** The blocks a state has recorded: the demo afternoon at the demo's 15:00, cut at what LoR last said. */
fun demoBlocks(store: ConceptStore): List<DayBlock> {
    if (store.home == null || store.fixture.clock.now != Fixtures.demo.clock.now) return emptyList()
    val seen = store.seenUntil()
    return demoDay.filter { it.start < seen }.map { if (it.end > seen) it.copy(end = seen) else it }
}

/** Until when LoR's view of the day holds: now, or when it last answered. */
fun ConceptStore.seenUntil(): Instant = if (homeLoad == Load.STALE) homeReadAt ?: now else now

/** Labels a block can be counted toward: the timer's, then those the day already uses. */
fun TimelineUi.labelChoices(): List<String> =
    (store.labels + blocks.mapNotNull { it.label }).distinct()

/** Whether a label can be saved: LoR answers. Every write to the day checks it, whichever control asked. */
private fun TimelineUi.canSave(): Boolean {
    if (store.lorLive) return true
    store.toast("LoR isn't answering, so that can't be saved yet")
    return false
}

/** Counts a block toward [label] (or a gap, as a new block), with Undo. */
fun TimelineUi.relabel(id: String, label: String) {
    if (!canSave()) return
    val before = blocks
    val gap = parseGap(id)
    val after = if (gap != null) (before + DayBlock("m${before.size}-${gap.first.epochSeconds}", gap.first, gap.second, Origin.TIMER, null, label, true)).sortedBy { it.start }
    else before.map { if (it.id == id) it.copy(label = label, counted = true, guess = false) else it }
    store.undoable("Counted as $label", { blocks = after }, { blocks = before })
}

/** Says yes to a block's proposed label, with Undo. */
fun TimelineUi.confirm(id: String) {
    val b = blocks.firstOrNull { it.id == id } ?: return
    relabel(id, b.label ?: return)
}

/** Says yes to every proposal at once, with one Undo. */
fun TimelineUi.confirmAll() {
    if (!canSave()) return
    val before = blocks
    val n = before.count { it.tone == DayBlock.Tone.PROPOSED }
    if (n == 0) return
    store.undoable("Counted $n blocks as proposed", { blocks = before.map { if (it.tone == DayBlock.Tone.PROPOSED) it.copy(counted = true, guess = false) else it } }, { blocks = before })
}

/** Plans [taskId] into a free gap from [start], for 30 minutes or the gap if shorter, with Undo. */
fun TimelineUi.plan(taskId: String, title: String, start: Instant, gapEnd: Instant) {
    if (!canSave()) return
    val p = Plan(taskId, title, start, minOf(start + 30.minutes, gapEnd))
    store.undoable("Planned at ${com.spadaval.lor.ui.LocalClock.time(start, store.zone)}: ${title.take(36)}", { plans.add(p) }, { plans.remove(p) })
}

/** Takes a planned task off the route, back to the untimed tasks, with Undo. */
fun TimelineUi.unplan(p: Plan) {
    if (!canSave()) return
    store.undoable("Unplanned: ${p.title.take(36)}", { plans.remove(p) }, { plans.add(p) })
}

// ---- The timer, as the route keeps it ----
// The store owns the one timer; the route owns the day's blocks. Stopping or switching ends the
// running block, so it stays on the route as recorded time, and one Undo takes back both.

/** The running timer's time so far, as a recorded block (counted when it has a label). */
private fun TimelineUi.ended(t: ConceptStore.Running, at: Instant) =
    DayBlock("t${t.start.epochSeconds}", t.start, maxOf(at, t.start.plus(1.minutes)), Origin.TIMER, null, t.label, counted = t.label != null)

/** Runs a store timer change and adds the block it ended, under one Undo with the store's words. */
private fun TimelineUi.endAnd(change: () -> Unit) {
    val t = store.timer
    val at = store.clock()
    change()
    if (t == null) return
    val inner = store.toast
    val before = blocks
    store.undoable(inner?.text ?: "Stopped", { blocks = (before + ended(t, at)).sortedBy { it.start } }, { inner?.undo?.invoke(); blocks = before })
}

fun TimelineUi.stopTimer() = endAnd { store.stop() }
fun TimelineUi.switchTimer(label: String?) = endAnd { store.switch(label) }
/** Starts a timer; with one running, that is a switch, so its time is kept. */
fun TimelineUi.startTimer(label: String?) = if (store.timer != null) switchTimer(label) else store.start(label)

/** A gap with nothing recorded, as a sheet's id: `gap:<start>:<end>` in epoch seconds. */
fun gapId(start: Instant, end: Instant) = "gap:${start.epochSeconds}:${end.epochSeconds}"
fun parseGap(id: String): Pair<Instant, Instant>? = id.takeIf { it.startsWith("gap:") }?.split(":")?.let {
    Instant.fromEpochSeconds(it[1].toLong()) to Instant.fromEpochSeconds(it[2].toLong())
}

// ---- The route ----

enum class SpanKind { BLOCK, RUNNING, UNTRACKED, SILENT, NOW, EVENT, PLAN, FREE }

/** A stretch of the route: what it is, its time, and its height, set by time with short and empty stretches evened out. */
data class Span(
    val kind: SpanKind,
    val key: String,
    val start: Instant,
    val end: Instant,
    val height: Dp,
    val block: DayBlock? = null,
    val event: LorHome.Event? = null,
    val plan: Plan? = null,
)

/** How tall a stretch draws: about a dp a minute where something happened, less where nothing did. */
private fun heightOf(kind: SpanKind, minutes: Long): Dp = when (kind) {
    SpanKind.BLOCK, SpanKind.EVENT, SpanKind.PLAN -> (minutes * 1.0).coerceIn(48.0, 132.0).dp
    SpanKind.RUNNING -> 132.dp
    SpanKind.UNTRACKED -> if (minutes < 4) 6.dp else (minutes * 0.5).coerceIn(28.0, 52.0).dp
    SpanKind.FREE -> (minutes * 0.6).coerceIn(52.0, 88.0).dp
    SpanKind.SILENT -> 76.dp
    SpanKind.NOW -> 0.dp
}

/** The route, the past (recorded, or gaps), now (the timer, or a Start), then the future (events, plans, free time). */
fun TimelineUi.route(now: Instant): List<Span> {
    val day = store.home?.day
    val wakeStart = day?.wakeStart ?: store.now.minus(7.hours)
    val wakeEnd = day?.wakeEnd ?: store.now.plus(7.hours)
    val events = day?.events.orEmpty()
    val stale = store.homeLoad == Load.STALE
    val seen = if (stale) store.seenUntil() else now
    fun floorHour(t: Instant) = Instant.fromEpochSeconds(t.epochSeconds - t.epochSeconds.mod(3600L))
    fun ceilHour(t: Instant) = floorHour(t).let { if (it < t) it.plus(1.hours) else it }
    val first = floorHour(listOfNotNull(wakeStart, blocks.firstOrNull()?.start).min())
    val last = ceilHour(listOf(wakeEnd, now.plus(1.hours)).plus(events.map { it.end }).max())
    val out = mutableListOf<Span>()
    fun add(kind: SpanKind, key: String, a: Instant, z: Instant, block: DayBlock? = null, event: LorHome.Event? = null, plan: Plan? = null) {
        out += Span(kind, key, a, z, heightOf(kind, (z - a).inWholeMinutes), block, event, plan)
    }
    // LoR never answered: the route says so rather than drawing a day of nothing recorded.
    if (store.home == null) {
        add(SpanKind.SILENT, "silent", now.minus(2.hours), now)
        add(SpanKind.NOW, "now", now, now)
        return out
    }
    val timer = store.timer
    val runFrom = timer?.start?.takeIf { !stale }
    var cursor = first
    for (b in blocks.sortedBy { it.start }) {
        if (b.start >= seen || (runFrom != null && b.start >= runFrom)) continue
        val z = minOf(b.end, seen, runFrom ?: b.end)
        if (b.start > cursor) add(SpanKind.UNTRACKED, gapId(cursor, b.start), cursor, b.start)
        add(SpanKind.BLOCK, b.id, b.start, maxOf(z, b.start.plus(1.minutes)), block = b)
        cursor = maxOf(cursor, z)
    }
    when {
        stale -> {
            if (cursor < seen) add(SpanKind.UNTRACKED, gapId(cursor, seen), cursor, seen)
            add(SpanKind.SILENT, "silent", seen, now)
        }
        runFrom != null -> {
            val a = maxOf(runFrom, cursor)
            if (a > cursor) add(SpanKind.UNTRACKED, gapId(cursor, a), cursor, a)
            add(SpanKind.RUNNING, "running", a, now)
        }
        cursor < now -> add(SpanKind.UNTRACKED, gapId(cursor, now), cursor, now)
    }
    add(SpanKind.NOW, "now", now, now)
    val ahead = (events.map { Triple(maxOf(it.start, now), it.end, it as Any) } + plans.map { Triple(maxOf(it.start, now), it.end, it as Any) })
        .filter { it.second > now }.sortedBy { it.first }
    cursor = now
    for ((a, z, x) in ahead) {
        if (a < cursor) continue
        if (a - cursor >= 10.minutes) add(SpanKind.FREE, "free:${cursor.epochSeconds}", cursor, a)
        else if (a > cursor) add(SpanKind.UNTRACKED, "soon:${cursor.epochSeconds}", cursor, a)
        when (x) {
            is LorHome.Event -> add(SpanKind.EVENT, "event:${x.title}:${x.start.epochSeconds}", a, z, event = x)
            is Plan -> add(SpanKind.PLAN, "plan:${x.taskId}", a, z, plan = x)
        }
        cursor = z
    }
    if (cursor < last) add(SpanKind.FREE, "free:${cursor.epochSeconds}", cursor, last)
    return out
}
