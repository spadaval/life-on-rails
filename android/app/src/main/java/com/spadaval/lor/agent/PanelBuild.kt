package com.spadaval.lor.agent

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.spadaval.lor.LorApp
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.home.cellInsetsPadding
import com.spadaval.lor.home.listTakes
import com.spadaval.lor.home.scrollsInGrid
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorPanelState
import com.spadaval.lor.lor.LorPeople
import com.spadaval.lor.lor.LorTasks
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.net.DevicePeople
import com.spadaval.lor.net.DeviceTasks
import com.spadaval.lor.net.LorClient
import com.spadaval.lor.net.LorReads
import com.spadaval.lor.net.TaskAction
import com.spadaval.lor.quicksend.QuickSend
import com.spadaval.lor.t3.PairScan
import com.spadaval.lor.t3.Pairing
import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.Question
import com.spadaval.lor.t3.T3Connection
import com.spadaval.lor.t3.T3Thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

/*
 * The agent page's, the LoR panel's and the blocks' state and actions, from what the app reads
 * (lor-rgx4): LoR's reader with the writes held over it, the T3 connection, and the cards' calls.
 * Their drawing is homecore's (`AgentPageView.kt`, `LorPanelView.kt`, `LorBlockView.kt`).
 */

/** The panels' state now. Read while composing, so a change to any of it recomposes the panel. */
@Composable
internal fun panelUi(model: AgentModel): PanelUi {
    val status by model.connection.status.collectAsState()
    val shell by model.connection.shell.collectAsState()
    val home = model.lor
    val writes = model.writes
    return PanelUi(
        lor = LorUi(
            home = remember(home) { home?.common() },
            error = model.lorError,
            current = model.lorRead,
            retrying = model.retrying,
            tasks = model.tasks.common { writes.tasks(it).common() },
            people = model.people.common { it.common() },
            busy = writes.busy,
            failures = writes.failures,
            done = writes.doneHeld(),
            now = java.time.Instant.now().common(),
            zone = ZoneId.systemDefault().id,
        ),
        t3 = T3Ui(
            link = status.link(),
            error = (status as? T3Connection.Status.Rejected)?.error,
            loaded = shell.loaded,
            threads = shell.threads,
            answered = model.answered,
            busy = model.busy,
            failures = model.failures,
        ),
        notice = model.notice,
        chosen = ChosenThread.id,
        replies = RepliesUi(Replies.drafts, Replies.sending, Replies.failures),
        tab = LorPanelState.tab,
    )
}

/** What the panels' taps do: [model]'s calls and writes, opening T3, LoR and the quick-send sheet. */
@Composable
internal fun rememberPanelActions(model: AgentModel): PanelActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(model) {
        val work = (context.applicationContext as LorApp).scope
        PanelActions(
            retry = { model.retry() },
            timer = { action, intentId -> model.writes.timer(action, intentId) },
            recentIntents = { recentIntents(model) },
            task = { id, title, action, say, undo, thread -> model.writes.task(id, title, action.wire(), say, undo?.wire(), thread) },
            openLor = { path, from -> model.openLor(path, from.launch()) },
            openThread = { id, from -> model.openThread(id, from.launch()) },
            approve = { card, approve -> model.approve(card, approve) },
            answer = { card, answers -> model.answer(card, answers) },
            questions = { request, live -> questions(model, request, live) },
            choose = { ChosenThread.id = it },
            readThread = { thread, live -> threadRead(model.connection, thread, live) },
            editReply = { id, text -> Replies.edit(id, text) },
            sendReply = { id -> Replies.send(work, model.connection, id) },
            quickSend = { QuickSend.open(context, QuickSend.From.AGENT) },
            pair = pairCalls(context, model.connection, scope),
            tab = { LorPanelState.tab = it },
        )
    }
}

/** Home's grid around a panel: the cell's insets and the grid's scroll arbitration (lor-qdmy). */
internal val HomeGrid = InGrid(
    insets = Modifier.cellInsetsPadding(),
    scrolls = { scroll: ScrollState, reversed: Boolean ->
        if (reversed) Modifier.scrollsInGrid { way -> listTakes(way, scroll.canScrollForward, scroll.canScrollBackward, reversed = true) }
        else Modifier.scrollsInGrid(scroll)
    },
)

/** Pairing with T3 from a card: scanning its code, or a pasted link, in [scope]. */
internal fun pairCalls(context: Context, connection: T3Connection, scope: CoroutineScope) = PairCalls(
    scan = { found, failed -> PairScan.scan(context, { link, _ -> found(link) }, failed) },
    pair = { link, done ->
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { connection.pair(Pairing.parse("", link)) } }
            done(result.exceptionOrNull()?.let { "Can't pair: ${T3Connection.describe(it)}" })
        }
    },
)

/** The Start menu's intents, read from `/device/state` each time it opens. */
@Composable
private fun recentIntents(model: AgentModel): Loaded<List<StartIntent>>? {
    var recent by remember { mutableStateOf<Loaded<List<StartIntent>>?>(null) }
    LaunchedEffect(Unit) {
        recent = try {
            Loaded(model.recentIntents().map { StartIntent(it.id, it.title) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Loaded(error = LorClient.describe(e))
        }
    }
    return recent
}

/** A question's questions, read from its thread when the connection is live and again after each reconnect. */
@Composable
private fun questions(model: AgentModel, request: PendingRequest, live: Boolean): Loaded<List<Question>>? {
    var questions by remember(request.requestId) { mutableStateOf<Loaded<List<Question>>?>(null) }
    LaunchedEffect(request.requestId, live) {
        if (!live) return@LaunchedEffect
        questions = try {
            Loaded(model.connection.questions(request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Loaded(error = T3Connection.describe(e))
        }
    }
    return questions
}

/**
 * The thread column's read, while T3 is live, and again whenever the thread, its updatedAt or the
 * connection changes ([threadReads]).
 */
@Composable
private fun threadRead(connection: T3Connection, thread: T3Thread, live: Boolean): ThreadRead? {
    var read by remember { mutableStateOf<ThreadRead?>(null) }
    val shown by rememberUpdatedState(Shown(ReadKey(thread.id, thread.updatedAt), live))
    LaunchedEffect(Unit) {
        threadReads(snapshotFlow { shown }).collect { k -> read = readThread(connection, k.threadId, k.updatedAt, read) }
    }
    return read
}

private fun Tap.launch(): LaunchFrom = when (this) {
    Tap.ICON -> LaunchFrom.AGENT_ICON
    Tap.CARD -> LaunchFrom.AGENT_CARD
}

private fun TaskWrite.wire(): TaskAction = TaskAction.valueOf(name)

internal fun T3Connection.Status.link(): T3Link = when (this) {
    T3Connection.Status.NotPaired -> T3Link.NOT_PAIRED
    T3Connection.Status.Stopped, T3Connection.Status.Connecting -> T3Link.CONNECTING
    T3Connection.Status.Live -> T3Link.LIVE
    is T3Connection.Status.Retrying -> T3Link.RETRYING
    is T3Connection.Status.Rejected -> T3Link.REJECTED
}

// ---- LoR's replies in common types ----

internal fun java.time.Instant.common(): Instant = Instant.fromEpochSeconds(epochSecond, nano)

private fun java.time.LocalDate.common(): LocalDate = LocalDate(year, monthValue, dayOfMonth)

private fun <T, C> LorReads.Kept<T>.common(convert: (T) -> C): Kept<C> =
    Kept(value?.let(convert), error, current, at?.common())

/** DeviceHome is in common types already (lor-xdoi); its zone goes by name. */
internal fun DeviceHome.common(): LorHome = LorHome(
    now = now,
    zone = zone.id,
    one = one?.let { LorHome.One(it.kicker, it.text, it.why) },
    needsYou = needsYou.map { LorHome.Need(it.id, it.title, it.note, it.google) },
    day = LorHome.Day(
        wakeStart = day.wakeStart,
        wakeEnd = day.wakeEnd,
        remainingFraction = day.remainingFraction,
        running = day.running?.let { LorHome.Running(it.title, it.start, it.paused) },
        events = day.events.map { LorHome.Event(it.title, it.start, it.end) },
    ),
    agentQueue = agentQueue.map { LorHome.AgentTask(it.id, it.title, it.queuedAt, it.thread) },
)

internal fun DeviceTasks.common(): LorTasks = LorTasks(
    now = now.common(),
    zone = zone.id,
    today = today.common(),
    open = open,
    tasks = tasks.map { it.common() },
    doneToday = doneToday.map { it.common() },
)

private fun DeviceTasks.Task.common() = LorTasks.Task(id, title, due?.common(), overdue, waitingOn, parent, flagged, agent, google)

internal fun DevicePeople.common(): LorPeople = LorPeople(
    today = today.common(),
    overdue = overdue.map { it.common() },
    birthdays = birthdays.map { it.common() },
)

private fun DevicePeople.Person.common() =
    LorPeople.Person(id, name, kind, lastAt?.common(), lastVia, overdueBy, keepInTouchDays, birthdayIn, turning)
