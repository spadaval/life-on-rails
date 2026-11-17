package com.spadaval.lor.lor

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

/*
 * LoR's three replies as the agent page, the LoR panel and its blocks draw them (lor-rgx4): the
 * app's `DeviceHome`, `DeviceTasks` and `DevicePeople`, with common types. The app makes them from
 * its own (`agent/PanelState.kt`); the lab reads them from a fixture's replies ([LorReplies]), and
 * the app's FixtureWireTest checks that both read a fixture the same. When lor-xdoi's common reader
 * replaces the app's org.json parsers, these and the app's become one.
 */

/** `GET /device/home`, as the panels read it. [zone] is an IANA zone name. */
data class LorHome(
    val now: Instant,
    val zone: String,
    val one: One?,
    val needsYou: List<Need>,
    val day: Day,
    val agentQueue: List<AgentTask> = emptyList(),
) {
    data class One(val kicker: String, val text: String, val why: String?) {
        /** `pickOne` names the running timer this way; the page draws the timer from [Day.running] instead. */
        val isTimer: Boolean get() = kicker.startsWith("Running since")
    }

    /** A flagged task; [google] when it lives in Google Tasks, where it is done. */
    data class Need(val id: String, val title: String, val note: String?, val google: Boolean = false)

    /** A task given to the agent: [queuedAt] while it waits for the next run, [thread] once a run works it in T3. */
    data class AgentTask(val id: String, val title: String, val queuedAt: Instant?, val thread: String?)

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
}

/** `GET /device/tasks`: the open tasks and those done today. */
data class LorTasks(
    val now: Instant,
    val zone: String,
    /** The local day the server read them on. */
    val today: LocalDate,
    /** Every open task, including those past the cap of [tasks]. */
    val open: Int,
    val tasks: List<Task>,
    val doneToday: List<Task>,
) {
    data class Task(
        val id: String,
        val title: String,
        val due: LocalDate?,
        val overdue: Boolean,
        /** Who a waiting task waits on; null when not waiting. */
        val waitingOn: String?,
        val parent: String?,
        val flagged: Boolean,
        val agent: Boolean,
        val google: Boolean,
    )
}

/** `GET /device/people`: who is past their keep-in-touch cadence and whose birthday is near. */
data class LorPeople(val today: LocalDate, val overdue: List<Person>, val birthdays: List<Person>) {
    data class Person(
        val id: String,
        val name: String,
        val kind: String,
        val lastAt: Instant?,
        val lastVia: String?,
        val overdueBy: Int?,
        val keepInTouchDays: Int?,
        val birthdayIn: Int?,
        val turning: Int?,
    )
}

/** Reads LoR's replies as the server sends them (the fixtures hold them so). Throws on a reply that doesn't read. */
object LorReplies {
    fun home(o: JsonObject): LorHome {
        val day = o.obj("day")
        return LorHome(
            now = o.time("now"),
            zone = o.string("timezone"),
            one = o.objOrNull("one")?.let { LorHome.One(it.string("kicker"), it.string("text"), it.str("why")) },
            needsYou = o.array("needs_you").map { it.jsonObject.let { n -> LorHome.Need(n.string("id"), n.string("title"), n.str("note"), n["google"]?.jsonPrimitive?.booleanOrNull ?: false) } },
            day = LorHome.Day(
                wakeStart = day.time("wake_start"),
                wakeEnd = day.time("wake_end"),
                remainingFraction = day.getValue("remaining_fraction").jsonPrimitive.double,
                running = day.objOrNull("running")?.let { LorHome.Running(it.string("title"), it.time("start"), it.getValue("paused").jsonPrimitive.boolean) },
                events = day.array("events").map { it.jsonObject.let { e -> LorHome.Event(e.string("title"), e.time("start"), e.time("end")) } },
            ),
            agentQueue = (o["agent_queue"] as? JsonArray).orEmpty().map {
                it.jsonObject.let { t -> LorHome.AgentTask(t.string("id"), t.string("title"), t.str("queued_at")?.let(Instant::parse), t.str("thread")) }
            },
        )
    }

    fun tasks(o: JsonObject): LorTasks = LorTasks(
        now = o.time("now"),
        zone = o.string("timezone"),
        today = LocalDate.parse(o.string("today")),
        open = o.getValue("open").jsonPrimitive.int,
        tasks = o.array("tasks").map { task(it.jsonObject) },
        doneToday = o.array("done_today").map { task(it.jsonObject) },
    )

    fun people(o: JsonObject): LorPeople = LorPeople(
        today = LocalDate.parse(o.string("today")),
        overdue = o.array("overdue").map { person(it.jsonObject) },
        birthdays = o.array("birthdays").map { person(it.jsonObject) },
    )

    private fun task(o: JsonObject) = LorTasks.Task(
        id = o.string("id"),
        title = o.string("title"),
        due = o.str("due")?.let(LocalDate::parse),
        overdue = o.bool("overdue"),
        waitingOn = o.str("waiting_on"),
        parent = o.str("parent"),
        flagged = o.bool("flagged"),
        agent = o.bool("agent"),
        google = o.bool("google"),
    )

    private fun person(o: JsonObject) = LorPeople.Person(
        id = o.string("id"),
        name = o.string("name"),
        kind = o.string("kind"),
        lastAt = o.str("last_at")?.let(Instant::parse),
        lastVia = o.str("last_via"),
        overdueBy = o.int("overdue_by"),
        keepInTouchDays = o.int("keep_in_touch_days"),
        birthdayIn = o.int("birthday_in"),
        turning = o.int("turning"),
    )

    private fun JsonObject.obj(key: String) = getValue(key).jsonObject
    private fun JsonObject.objOrNull(key: String): JsonObject? = this[key]?.takeUnless { it is JsonNull }?.jsonObject
    private fun JsonObject.array(key: String) = getValue(key).jsonArray
    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
    private fun JsonObject.str(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
    private fun JsonObject.bool(key: String): Boolean = getValue(key).jsonPrimitive.boolean
    private fun JsonObject.int(key: String): Int? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.int
    private fun JsonObject.time(key: String): Instant = Instant.parse(string(key))
}
