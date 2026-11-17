package com.spadaval.lor.concepts.kit

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spadaval.lor.agent.AgentQueue
import com.spadaval.lor.agent.T3Card
import com.spadaval.lor.lab.App
import com.spadaval.lor.lab.Fixture
import com.spadaval.lor.lab.LabPanels
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.Message
import com.spadaval.lor.lab.Question
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.TimerState
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorPeople
import com.spadaval.lor.lor.LorReplies
import com.spadaval.lor.lor.LorTasks
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import com.spadaval.lor.ui.LocalClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/*
 * A concept's model of the phone: everything a fixture holds, as state a concept draws and changes,
 * and every tap's effect. Nothing is sent anywhere. A tap changes what is drawn as the real call
 * would once it succeeded, except where the brief asks to see a failure (quick send without T3).
 *
 * What a concept may need beyond the fixture lives here too, so a fold or an unfold keeps it: the
 * open item, reply drafts by thread, list positions by name. A concept keeps its own screen state
 * (which sheet is open) wherever it likes, as long as it is above anything that branches on the
 * screen; the README says why.
 *
 * Undo follows the brief: what LoR can reverse (a tick, Done, Seen, a timer change, an icon moved)
 * goes through [undoable] and offers Undo; what goes to T3 (approve, deny, an answer) goes through
 * [sendSoon], which gives a moment to cancel before it is sent and no Undo after.
 */
class ConceptStore(val fixture: Fixture, private val scope: CoroutineScope) {
    val zone: String = fixture.clock.zone
    /** The fixture's moment: what the state is drawn at. */
    val now: Instant = Instant.parse(fixture.clock.now)
    private val opened = kotlin.time.TimeSource.Monotonic.markNow()

    /**
     * The state's clock moving on with the wall clock since it opened: what a timer starts at and
     * every readout shows, so all of them agree.
     */
    fun clock(): Instant = now + opened.elapsedNow()

    // ---- LoR ----

    private val panels = LabPanels.ui(fixture)

    /** `/device/home`'s reply, with taps applied; null when none was ever read. */
    var home: LorHome? by mutableStateOf(panels.lor.home)
        private set

    /** How `/device/home`'s read stands: OK, loading, stale (a kept reply) or failed (none). */
    val homeLoad: Load = fixture.lor.home.load
    val homeError: String? = fixture.lor.home.error
    val homeReadAt: Instant? = fixture.lor.home.readAt?.let(Instant::parse)

    /** The open tasks, with ticks applied. */
    var tasks: List<LorTasks.Task> by mutableStateOf(panels.lor.tasks.value?.tasks.orEmpty())
        private set
    val tasksLoad: Load = fixture.lor.tasks.load
    val people: LorPeople? = panels.lor.people.value

    /** Task ids ticked here; a ticked task stays listed, struck, until the list is drawn again. */
    val done = mutableStateMapOf<String, Boolean>()

    /** The local day it is. */
    val today: LocalDate = panels.lor.tasks.value?.today ?: LocalClock.date(now, zone)

    /** The weather line from `/device/home`, when it has one: "12° Partly cloudy". */
    val weather: String? = fixture.lor.home.reply?.get("weather")?.let { w ->
        val o = w as? JsonObject ?: return@let null
        val t = o["temp_c"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@let null
        "${kotlin.math.round(t).toInt()}° ${o["condition"]?.jsonPrimitive?.content.orEmpty()}".trim()
    }

    /** Whether LoR answers: false when its last read failed or is kept from before. */
    val lorLive: Boolean get() = homeLoad == Load.OK

    // ---- T3 ----

    val t3Status: T3Status = fixture.t3.status
    val t3Error: String? = fixture.t3.error
    val t3SeenAt: Instant? = fixture.t3.seenAt?.let(Instant::parse)
    val t3Live: Boolean get() = t3Status == T3Status.LIVE

    var threads: List<T3Thread> by mutableStateOf(LabPanels.t3(fixture.t3).threads)
        private set

    /** Card keys answered, approved, denied or dismissed here. */
    val answered = mutableStateMapOf<String, Boolean>()

    /** What waits on Sumeet from T3, as the agent page ranks it (approvals, questions, failures). */
    val cards: List<T3Card> get() = AgentQueue.cards(threads, answered.keys, now).filter { it.key !in answered }

    /** Threads with a turn under way. */
    val running: List<T3Thread> get() = threads.filter { it.state == ThreadState.RUNNING }

    fun questions(card: T3Card): List<Question> = card.request?.let { fixture.t3.questions[it.requestId] }.orEmpty()

    /** A thread's transcript: the fixture's, then what was sent from here. */
    fun messages(threadId: String): List<Message> = fixture.t3.messages[threadId].orEmpty() + sent[threadId].orEmpty()

    private val sent = mutableStateMapOf<String, List<Message>>()

    /** Reply drafts by thread id; they survive a fold. */
    val drafts = mutableStateMapOf<String, String>()

    fun reply(threadId: String) {
        val text = drafts[threadId]?.trim().orEmpty()
        if (text.isEmpty()) return
        sent[threadId] = sent[threadId].orEmpty() + Message(true, text, now.toString())
        drafts.remove(threadId)
    }

    // ---- Sending to T3, with a moment to cancel ----

    /** Calls waiting to go to T3, by card key, with when they go. */
    val pending = mutableStateMapOf<String, Pending>()
    private val pendingJobs = mutableMapOf<String, Job>()

    /** A call on its way: what it does, when it was asked for, and how long Cancel stays. */
    class Pending(val verb: String, val since: kotlin.time.TimeMark, val forMs: Long)

    /**
     * Sends [verb] for [key] after [wait]: until then the card shows it is going and Cancel takes it
     * back; after, [send] runs and nothing undoes it.
     */
    fun sendSoon(key: String, verb: String, wait: kotlin.time.Duration = 3.seconds, send: () -> Unit) {
        pendingJobs.remove(key)?.cancel()
        pending[key] = Pending(verb, kotlin.time.TimeSource.Monotonic.markNow(), wait.inWholeMilliseconds)
        pendingJobs[key] = scope.launch {
            delay(wait)
            pending.remove(key)
            pendingJobs.remove(key)
            send()
        }
    }

    fun cancelSend(key: String) {
        pendingJobs.remove(key)?.cancel()
        pending.remove(key)
    }

    fun approve(card: T3Card) = sendSoon(card.key, "Approving") { answered[card.key] = true; toast("Approved. T3 runs the command.") }
    fun deny(card: T3Card) = sendSoon(card.key, "Denying") { answered[card.key] = true; toast("Denied. T3 was told not to run it.") }
    fun answer(card: T3Card, answer: String) = sendSoon(card.key, "Sending “${answer.take(40)}”") { answered[card.key] = true; toast("Answer sent.") }

    /** A failed turn's card hidden here (T3 keeps the thread): reversible, so it has Undo. */
    fun dismiss(card: T3Card) = undoable("Dismissed “${card.thread.title.take(40)}”", { answered[card.key] = true }, { answered.remove(card.key) })

    // ---- LoR's writes, with Undo ----

    /** A task marked done, from any surface. A flagged one also leaves Needs you, as [flagDone] does. */
    fun tick(id: String) {
        if (home?.needsYou?.any { it.id == id } == true) return flagDone(id)
        val title = tasks.firstOrNull { it.id == id }?.title ?: home?.needsYou?.firstOrNull { it.id == id }?.title ?: "Task"
        undoable("Done: ${title.take(48)}", { done[id] = true }, { done.remove(id) })
    }

    fun untick(id: String) = undoable("Not done", { done.remove(id) }, { done[id] = true })

    /** A flag marked Done: the task is done and leaves Needs you. */
    fun flagDone(id: String) {
        val before = home
        val title = before?.needsYou?.firstOrNull { it.id == id }?.title ?: "Task"
        undoable("Done: ${title.take(48)}", { done[id] = true; home = before?.copy(needsYou = before.needsYou.filterNot { it.id == id }) }, { done.remove(id); home = before })
    }

    /** A flag marked Seen: it leaves Needs you and the task stays open. */
    fun seen(id: String) {
        val before = home
        undoable("Seen", { home = before?.copy(needsYou = before.needsYou.filterNot { it.id == id }) }, { home = before })
    }

    /** A task's due date changed. */
    fun setDue(id: String, due: LocalDate?) {
        val before = tasks
        undoable(if (due == null) "No due date" else "Due ${due.dayOfMonth} ${due.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}", {
            tasks = before.map { if (it.id == id) it.copy(due = due, overdue = due != null && due < today) else it }
        }, { tasks = before })
    }

    /** A task added from search: it goes to the top of the open tasks. */
    fun addTask(title: String, due: LocalDate?) {
        val before = tasks
        val task = LorTasks.Task("new-${before.size}", title, due, false, null, null, false, false, false)
        undoable("Added “${title.take(40)}”", { tasks = listOf(task) + before }, { tasks = before })
    }

    // ---- The timer: one, with one vocabulary ----

    /** The running timer, or null. [label] is null for one with no label. */
    data class Running(val label: String?, val start: Instant)

    var timer: Running? by mutableStateOf(
        when (fixture.timer.state) {
            TimerState.RUNNING, TimerState.PAUSED -> Running(fixture.timer.intent ?: fixture.timer.title, fixture.timer.start?.let(Instant::parse) ?: now)
            else -> panels.lor.home?.day?.running?.let { Running(it.title, it.start) }
        },
    )
        private set

    val timerReachable: Boolean = fixture.timer.state != TimerState.UNREACHABLE && homeLoad != Load.FAILED

    /** Labels to start or switch to: the recent ones first, then the rest. */
    val labels: List<String> = (fixture.timer.recent + fixture.timer.intents).map { it.title }.distinct()

    fun start(label: String?) {
        val before = timer
        val at = clock()
        undoable(if (label == null) "Timer started" else "Started $label", { timer = Running(label, at) }, { timer = before })
    }

    fun stop() {
        val before = timer ?: return
        undoable("Stopped ${before.label ?: "the timer"}", { timer = null }, { timer = before })
    }

    /** Ends the running block and starts one under [label] from now. */
    fun switch(label: String?) {
        val before = timer
        val at = clock()
        undoable("Switched to ${label ?: "no label"}", { timer = Running(label, at) }, { timer = before })
    }

    // ---- The phone: apps, home's icons, sync ----

    val apps: List<App> = fixture.apps.list
    val appsLoading: Boolean = fixture.apps.load == Load.LOADING

    /** Apps hidden from the drawer by the layout. */
    val hidden: Set<String> = fixture.layout?.get("hidden")?.let { it as? JsonArray }?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty().toSet()

    fun app(ref: String): App? = apps.firstOrNull { it.ref == ref }

    /** Home's icons: the start cell's apps blocks, in order; a folder is one item. Moving one changes this. */
    var icons: List<Icon> by mutableStateOf(startIcons())
        private set

    sealed interface Icon {
        val key: String
        data class AppIcon(val app: App) : Icon { override val key get() = app.ref }
        data class Folder(val name: String, val apps: List<App>) : Icon { override val key get() = "folder:$name" }
    }

    fun move(key: String, to: Int) {
        val before = icons
        val from = before.indexOfFirst { it.key == key }
        if (from < 0 || from == to) return
        val after = before.toMutableList().apply { add(to.coerceIn(0, size - 1), removeAt(from)) }
        undoable("Moved ${label(before[from])}", { icons = after }, { icons = before })
    }

    private fun label(i: Icon) = when (i) { is Icon.AppIcon -> i.app.label; is Icon.Folder -> i.name }

    private fun startIcons(): List<Icon> {
        val layout = fixture.layout ?: return apps.take(12).map { Icon.AppIcon(it) }
        val chosen = layout["layouts"]?.jsonArray?.map { it.jsonObject }?.let { all ->
            all.firstOrNull { it["name"]?.jsonPrimitive?.content == layout["active"]?.jsonPrimitive?.content } ?: all.firstOrNull()
        } ?: return emptyList()
        val start = chosen["start"]?.jsonObject?.get("narrow")?.jsonArray?.map { it.jsonPrimitive.content.toInt() } ?: listOf(0, 0)
        val panel = chosen["panels"]?.jsonArray?.map { it.jsonObject }?.firstOrNull { p -> p["at"]?.jsonArray?.map { it.jsonPrimitive.content.toInt() } == start }
        val items = panel?.get("blocks")?.jsonArray.orEmpty().map { it.jsonObject }.filter { it["type"]?.jsonPrimitive?.content == "apps" }
            .flatMap { it["items"]?.jsonArray.orEmpty() }
        return items.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> app(item.content)?.let { Icon.AppIcon(it) }
                is JsonObject -> Icon.Folder(item["folder"]?.jsonPrimitive?.content ?: "Folder", item["apps"]?.jsonArray.orEmpty().mapNotNull { app(it.jsonPrimitive.content) })
                else -> null
            }
        }
    }

    /** Launch counts, for search's ranking and the drawer's suggestions. */
    val launches: Map<String, Int> = fixture.search.launches
    val quickDial: List<App> = fixture.search.quickDial.mapNotNull(::app)

    /** The last sync and what failed in it; [fixSync] is the lab's stand-in for granting and retrying. */
    var sync by mutableStateOf(fixture.phone.sync)
        private set
    var permissions by mutableStateOf(fixture.phone.permissions)
        private set
    var sensors by mutableStateOf(fixture.phone.sensors)
        private set

    val syncFailed: Boolean get() = sync.errors.isNotEmpty()

    /** Sensors whose permission is missing. */
    val missing get() = sensors.filter { it.on && !it.allowed }

    fun allow(key: String) {
        sensors = sensors.map { if (it.key == key) it.copy(allowed = true, status = null, action = null) else it }
    }

    fun allowAll() { sensors = sensors.map { if (it.on) it.copy(allowed = true, status = null, action = null) else it } }

    fun toggleSensor(key: String) {
        sensors = sensors.map { if (it.key == key) it.copy(on = !it.on) else it }
    }

    /** Sync now: it succeeds once nothing is missing, and fails again while something is. */
    fun syncNow() {
        sync = if (missing.isEmpty()) sync.copy(lastAt = now.toString(), uploaded = sync.uploaded + sync.pending.toInt(), pending = 0, errors = emptyList())
        else sync.copy(errors = listOf("Still missing: ${missing.joinToString { it.key }}"))
        toast(if (syncFailed) "Sync failed again" else "Synced")
    }

    // ---- Quick send: succeeds when T3 is live, fails when it isn't ----

    sealed interface SendResult {
        data class Sent(val project: String) : SendResult
        data class Failed(val why: String) : SendResult
    }

    fun quickSend(text: String, project: String): SendResult =
        if (t3Live) SendResult.Sent(project).also { toast("Sent to $project") }
        else SendResult.Failed(
            when (t3Status) {
                T3Status.NOT_PAIRED -> "This phone isn't paired with T3."
                T3Status.REJECTED -> "T3 refused this phone: ${t3Error ?: "the pairing was revoked"}."
                else -> "T3 isn't connected${t3Error?.let { " ($it)" } ?: ""}."
            },
        )

    /** T3's projects, by title. */
    val projects: List<String> = fixture.t3.projects.values.toList().ifEmpty { threads.map { it.projectTitle }.distinct() }

    // ---- What a concept keeps across a fold ----

    /** The open item (a card key, a task id, a thread id): the concept says which. */
    var open: String? by mutableStateOf(null)

    /** List positions by name, so a fold keeps the scroll. */
    private val lists = mutableMapOf<String, LazyListState>()
    fun list(name: String): LazyListState = lists.getOrPut(name) { LazyListState() }

    // ---- Undo and notices ----

    /** The notice at the foot of the screen: words, and an Undo when the change can be taken back. */
    class Toast(val text: String, val undo: (() -> Unit)?, val id: Int)

    var toast: Toast? by mutableStateOf(null)
        private set
    private var toasts = 0

    fun toast(text: String) { toast = Toast(text, null, ++toasts) }

    /** Applies [apply], and offers Undo, which runs [revert]. */
    fun undoable(text: String, apply: () -> Unit, revert: () -> Unit) {
        apply()
        val id = ++toasts
        toast = Toast(text, { revert(); if (toast?.id == id) toast = null }, id)
    }

    fun dismissToast(id: Int) { if (toast?.id == id) toast = null }
}
