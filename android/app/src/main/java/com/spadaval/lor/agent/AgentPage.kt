package com.spadaval.lor.agent

import androidx.compose.ui.graphics.RectangleShape
import com.spadaval.lor.ui.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.spadaval.lor.home.cellInsetsPadding
import com.spadaval.lor.home.scrollsInGrid
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.lor.LorRows
import com.spadaval.lor.lor.LorWrites
import com.spadaval.lor.net.DeviceState
import com.spadaval.lor.net.LorClient
import com.spadaval.lor.net.TaskAction
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.key
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.quicksend.QuickSend
import com.spadaval.lor.t3.Question
import com.spadaval.lor.t3.T3Connection
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.UserInput
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * The agent page (T3 Launcher, Increment 3.3), the left-most page of home: Today from LoR, the
 * running T3 turns, and the queue (T3 approvals, questions and failures, then LoR's Needs you),
 * with one action per card and a quick-send button that opens the sheet (Increment 4). After the mockup's "Agent page"
 * (design/t3-launcher/t3-launcher-screens.html). Every card's icon opens its place: the T3 app at
 * the thread, or LoR's dashboard. Nothing moves on its own: the page redraws when data changes.
 */


internal val Body = Type.body.copy(fontSize = 14.sp)
internal val Small = Type.bodySmall.copy(fontSize = 12.sp)
/** Section heads: DESIGN.md's title, condensed caps in Dust (launcher polish 6.2). */
private val Heading = Type.title

@Composable
fun AgentPage() {
    val context = LocalContext.current
    val model = rememberAgentModel(t3 = true)
    val status by model.connection.status.collectAsState()
    val shell by model.connection.shell.collectAsState()

    // One column for both forms: the narrow page, and the wide form's left column, which adds the
    // Threads section under it. Held here, so a fold (which changes the form in place) keeps its place.
    val scroll = rememberScrollState()
    // A dark veil over the wallpaper, as the mockup's page has, so the text reads on any wallpaper.
    BoxWithConstraints(Modifier.fillMaxSize().background(Veil).cellInsetsPadding()) {
        val wide = maxWidth >= 600.dp
        val leftWidth = minOf(LEFT_MAX, maxWidth / 2)
        // The thread the wide form's right column shows (lor-iabt): the one chosen, else the first
        // T3 card's, else the most recently updated.
        val cards = AgentQueue.cards(shell.threads, model.answered, model.lor?.now ?: Instant.now())
        val shown = if (wide) ThreadChoice.shown(shell.threads, cards) else null
        val choose: ((T3Thread) -> Unit)? = if (wide) ({ ThreadChoice.chosen = it.id }) else null
        // The column ends at the keyboard (lor-sqfe: home's window doesn't pan), so a focused field,
        // an answer on a queue card, the pairing link or the reply box, sits above it.
        Row(Modifier.fillMaxSize().imePadding()) {
            Box(if (wide) Modifier.width(leftWidth).fillMaxHeight() else Modifier.fillMaxSize()) {
                ScrollColumn(scroll, Modifier.fillMaxWidth(), bottom = 120.dp) {
                    Today(model)
                    model.notice?.let { Text(it, style = Small.copy(color = Bad), modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
                    if (status == T3Connection.Status.NotPaired || status is T3Connection.Status.Rejected) PairCard(model.connection, status, Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    else T3State(status, shell.loaded)
                    Running(model, choose = choose, chosen = shown?.id)
                    NeedsYou(model, choose = choose, chosen = shown?.id)
                    if (wide) Threads(ThreadChoice.others(shell.threads, cards), shown?.id, choose!!)
                }
                // At the column's bottom-end corner (6.8: it floated mid-screen on the Fold), and
                // under the keyboard rather than over the field being typed in.
                SendButton(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp)) { QuickSend.open(context, QuickSend.From.AGENT) }
            }
            if (wide) ThreadColumn(model, shown, cards.firstOrNull { it.thread.id == shown?.id }, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** The wide form's left column is at most this wide; the chosen thread takes the rest. */
private val LEFT_MAX = 420.dp

/** A column of the page that scrolls on its own; its scroll takes a vertical swipe until its edge, then home's grid moves (lor-qdmy). */
@Composable
internal fun ScrollColumn(scroll: ScrollState, modifier: Modifier, bottom: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Column(modifier.fillMaxHeight().scrollsInGrid(scroll).verticalScroll(scroll).padding(top = 12.dp, bottom = bottom)) { content() }
}

/**
 * The model of a screen showing the agent page's sections: the page, LoR's blocks and the `lor`
 * panel (lor-lz97). While the screen is composed and home is started it holds LoR's reader (with
 * the lists when [lists]) and, when [t3], the T3 connection.
 */
@Composable
internal fun rememberAgentModel(t3: Boolean, lists: Boolean = false): AgentModel {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val model = remember { AgentModel(context, scope) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, t3, lists) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { model.whileVisible(t3, lists) } }
    DisposableEffect(model) { onDispose { model.dispose() } }
    return model
}

/**
 * T3's turns running now, at most [max] then "N more" ([More]): the agent page's Running section,
 * where it shows only with something running, and the `running` block ([block]), which always
 * shows and says why it is empty. In the wide form a row's tap [choose]s its thread for the right
 * column, and the [chosen] one carries the selection.
 */
@Composable
internal fun Running(
    model: AgentModel,
    max: Int? = null,
    more: (() -> Unit)? = null,
    block: Boolean = false,
    choose: ((T3Thread) -> Unit)? = null,
    chosen: String? = null,
) {
    val status by model.connection.status.collectAsState()
    val shell by model.connection.shell.collectAsState()
    val running = AgentQueue.running(shell.threads)
    if (running.isEmpty() && !block) return
    val seen = status == T3Connection.Status.Live && shell.loaded
    SectionHeader("Running", if (seen || running.isNotEmpty()) running.size.toString() else null)
    val (shown, hidden) = LorRows.cap(running, max)
    shown.forEach { t ->
        key(t.id) {
            if (choose != null) ThreadRow(t, t.id == chosen) { choose(t) }
            else RunningRow(t) { model.openThread(t.id, LaunchFrom.AGENT_CARD) }
        }
    }
    if (hidden > 0) More(hidden, more)
    if (running.isEmpty()) {
        Unseen(
            when {
                seen -> "Nothing is running on T3."
                status == T3Connection.Status.NotPaired || status is T3Connection.Status.Rejected -> "T3 isn't paired. Pair it on the agent page."
                status == T3Connection.Status.Connecting || status == T3Connection.Status.Stopped -> "Connecting to T3…"
                else -> "T3 isn't connected, so its turns aren't here."
            },
        )
    }
}

/**
 * The agent page's Needs you queue: T3's approvals, questions and failures, then LoR's flags, with
 * its count, all-clear and unseen notes ([AgentStates]); at most [max] cards, then "N more"
 * ([More]). The agent page shows it whole; the `needs-you` block ([block]) shows three. In the wide
 * form a T3 card's text [choose]s its thread, and the [chosen] one's card carries the selection; a
 * LoR flag has no thread, so its text still opens the dashboard.
 */
@Composable
internal fun NeedsYou(
    model: AgentModel,
    max: Int? = null,
    more: (() -> Unit)? = null,
    block: Boolean = false,
    choose: ((T3Thread) -> Unit)? = null,
    chosen: String? = null,
) {
    val status by model.connection.status.collectAsState()
    val shell by model.connection.shell.collectAsState()
    val live = status == T3Connection.Status.Live
    val now = model.lor?.now ?: Instant.now()
    val cards = AgentQueue.cards(shell.threads, model.answered, now)
    val needs = model.lor?.needsYou.orEmpty()
    val lorSource = when {
        model.lorError != null -> AgentStates.Source.UNSEEN
        model.lor != null && model.lorRead -> AgentStates.Source.SEEN
        else -> AgentStates.Source.READING
    }
    val t3Source = when {
        live && shell.loaded -> AgentStates.Source.SEEN
        status == T3Connection.Status.Connecting || status == T3Connection.Status.Stopped -> AgentStates.Source.READING
        else -> AgentStates.Source.UNSEEN
    }
    // A number once both sources are read, or whenever something is listed; with nothing
    // listed and a source not yet seen, none: "0" would claim what the page can't know.
    val count = cards.size + needs.size
    val whole = lorSource == AgentStates.Source.SEEN && t3Source == AgentStates.Source.SEEN
    SectionHeader("Needs you", if (whole || count > 0) count.toString() else null)
    val items = cards.map { c ->
        c.key to @Composable {
            T3QueueCard(c, model, live, selected = c.thread.id == chosen, onText = choose?.let { f -> { f(c.thread) } })
        }
    } +
        // LoR's flags greyed while they aren't current (launcher polish 3.3's rule); T3's cards are live.
        needs.map { "lor:${it.id}" to @Composable { Box(Modifier.alpha(if (lorSource == AgentStates.Source.SEEN) 1f else 0.6f)) { LorFlagCard(model, it) } } }
    val (shown, hidden) = LorRows.cap(items, max)
    shown.forEach { (k, card) -> key(k) { card() } }
    if (hidden > 0) More(hidden, more)
    AgentStates.notes(lorSource, t3Source, cards.size, needs.size)
        // On the page, T3State's line above already says T3 is disconnected and its approvals with it.
        .filterNot { it == AgentStates.Note.T3_UNSEEN && status is T3Connection.Status.Retrying && !block }
        .forEach { note ->
            when (note) {
                AgentStates.Note.ALL_CLEAR -> {
                    val lor = model.lor!!
                    Column(
                        Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .border(1.dp, Line, RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text("All clear.", style = Body.copy(fontSize = 13.sp))
                        Text(AgentStates.next(lor.day.events, lor.now, lor.zone), style = Small.copy(fontSize = 12.5.sp), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                AgentStates.Note.LOR_UNSEEN -> Unseen("LoR can't be read, so its flags aren't here.")
                AgentStates.Note.T3_UNSEEN -> Unseen("T3 isn't connected, so its approvals aren't here.")
            }
        }
}

/**
 * A LoR flag in a Needs you list, the same card in the `lor` panel, the `needs-you` block and the
 * agent page: its tile and its text open the task in the dashboard; Done and Seen write to LoR
 * (lor-rz50) as the web's Needs you does. Done also clears the flag, which its Undo doesn't put
 * back; Seen has no Undo (decision lor-dofd). A Google task has no Done: it is done in Google Tasks.
 * While a write is under way both are disabled; when one fails, the card is back with the reason.
 */
@Composable
internal fun LorFlagCard(model: AgentModel, need: DeviceHome.Need) {
    val path = LorRows.taskPath(need.id)
    val writes = model.writes
    val busy = need.id in writes.busy
    val failure = writes.failures[need.id]
    QueueCard(
        tile = "LoR", tileColor = Raised, tileLabel = "Open in LoR", onTile = { model.openLor(path, LaunchFrom.AGENT_ICON) },
        kind = "LoR", kindColor = Mute, where = "flagged", title = need.title, detail = failure ?: need.note,
        detailColor = if (failure != null) Bad else Mute,
        onText = { model.openLor(path, LaunchFrom.AGENT_CARD) },
    ) {
        Pill("Seen", enabled = !busy) { writes.task(need.id, need.title, TaskAction.SEEN, say = "Seen: ${need.title}") }
        if (need.google) Text("Google Tasks", style = Small, modifier = Modifier.padding(horizontal = 4.dp))
        else Pill("Done", primary = true, enabled = !busy) {
            writes.task(need.id, need.title, TaskAction.DONE, say = "Done: ${need.title}. Undo reopens it, without the flag.", undo = TaskAction.REOPEN)
        }
    }
}

/** "N more" under a block's rows; a tap moves the grid to the panel that shows them all, when the layout has one. */
@Composable
internal fun More(count: Int, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$count more", style = Small.copy(color = if (onClick != null) Txt else Mute, fontSize = 13.sp))
    }
}

/** A section's head: DESIGN.md's title, condensed caps, with a count or date at its end. */
@Composable
internal fun SectionHeader(title: String, right: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
        Text(title.uppercase(), style = Heading.copy(color = Txt, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        right?.let { Text(it, style = Heading) }
    }
}

// ---- Today ----

internal val Clock = DateTimeFormatter.ofPattern("H:mm")
private val Day = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * Today from LoR. When LoR can't be reached (launcher polish 3.3), the reason in plain words and a
 * Retry; what LoR said last stays, greyed, with the time it said it.
 */
@Composable
internal fun Today(model: AgentModel) {
    val lor = model.lor
    val error = model.lorError
    val zone = lor?.zone ?: ZoneId.systemDefault()
    SectionHeader("Today", (lor?.now ?: Instant.now()).atZone(zone).format(Day))
    if (lor == null) {
        if (error == null) Text("Reading LoR…", style = Small, modifier = Modifier.padding(horizontal = 18.dp))
        else Unreachable(error, model)
        return
    }
    // Greyed while it isn't fresh: LoR can't be reached, or no read has ended since the page opened.
    Column(Modifier.alpha(if (error != null || !model.lorRead) 0.6f else 1f)) { TodayBody(lor, zone) }
    TimerControls(model, lor.day.running)
    if (error != null) Unreachable("$error Showing what it said at ${lor.now.atZone(zone).format(Clock)}.", model)
}

/**
 * The timer's controls under Today (lor-rz50), as the web's timer card: with one running, Pause
 * (Resume while paused) and Stop; with none, Start, whose menu offers "Untitled" (no intent) and
 * the intents timed most recently, read from `/device/state` each time it opens. Each sends
 * `POST /device/timer` and shows what its reply says is running ([LorWrites.timer]); no Undo, as
 * on the web. Disabled while a call is under way; a failure says why under them.
 */
@Composable
private fun TimerControls(model: AgentModel, running: DeviceHome.Running?) {
    val writes = model.writes
    val busy = LorWrites.TIMER in writes.busy
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (running == null) Text("No timer running.", style = Small, modifier = Modifier.weight(1f))
        else Spacer(Modifier.weight(1f))
        if (running != null) {
            Pill(if (running.paused) "Resume" else "Pause", enabled = !busy) { writes.timer(if (running.paused) "resume" else "pause") }
            Spacer(Modifier.width(5.dp))
            Pill("Stop", enabled = !busy) { writes.timer("stop") }
        } else {
            Box {
                Pill("Start", enabled = !busy) { menu = true }
                if (menu) StartMenu(model) { intentId ->
                    menu = false
                    if (intentId != null) writes.timer("start", intentId.ifEmpty { null })
                }
            }
        }
    }
    writes.failures[LorWrites.TIMER]?.let { Failed(it, Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp)) }
}

/**
 * The Start menu: "Untitled" at once, then "Reading…" until `/device/state`'s recent intents come,
 * or why they couldn't be read. [chosen] gets the intent's id, "" for Untitled, or null when the
 * menu closes with nothing chosen.
 */
@Composable
private fun StartMenu(model: AgentModel, chosen: (String?) -> Unit) {
    var recent by remember { mutableStateOf<Result<List<DeviceState.Intent>>?>(null) }
    LaunchedEffect(Unit) {
        recent = try {
            Result.success(model.recentIntents())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    DropdownMenu(true, { chosen(null) }, containerColor = Raised, shape = RectangleShape) {
        MenuItem("Untitled") { chosen("") }
        val r = recent
        when {
            r == null -> Text("Reading…", style = Small, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
            r.isFailure -> Text(
                LorClient.describe(r.exceptionOrNull() as? Exception ?: Exception(r.exceptionOrNull())),
                style = Small,
                modifier = Modifier.widthIn(max = 260.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            )
            else -> r.getOrThrow().forEach { i -> MenuItem(i.title) { chosen(i.id) } }
        }
    }
}

@Composable
internal fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, style = Type.body.copy(color = Txt, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = onClick)
}

/** Why LoR can't be read, with a Retry that reads at once. */
@Composable
internal fun Unreachable(text: String, model: AgentModel) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(Bad, CircleShape))
        Text(text, style = Small, modifier = Modifier.weight(1f).padding(start = 8.dp, end = 10.dp))
        Pill(if (model.retrying) "Trying…" else "Retry", enabled = !model.retrying) { model.retry() }
    }
}

/** A source the queue can't see, in place of an all-clear. */
@Composable
internal fun Unseen(text: String) {
    Text(text, style = Small, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
}

@Composable
private fun TodayBody(lor: DeviceHome, zone: ZoneId) {
    DayArc(lor)
    val running = lor.day.running
    val one = lor.one?.takeUnless { it.isTimer }
    if (running != null) {
        val since = running.start.atZone(zone).format(Clock)
        Text(
            if (running.paused) "Paused: ${running.title}" else "Timer: ${running.title} · since $since",
            style = Small.copy(color = if (running.paused) Mute else Or, fontSize = 13.sp),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp),
        )
    }
    if (one != null) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp)) {
            Text("NEXT · ${one.kicker.uppercase()}", style = Heading)
            Text(one.text, style = Body.copy(fontSize = 17.sp, lineHeight = 22.sp), modifier = Modifier.padding(top = 3.dp))
            one.why?.let { Text(it, style = Small, modifier = Modifier.padding(top = 2.dp)) }
        }
    }
    val events = lor.day.events.take(3)
    if (events.isNotEmpty()) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            events.forEach { e ->
                Row {
                    Text(e.start.atZone(zone).format(Clock), style = Small, modifier = Modifier.widthIn(min = 44.dp))
                    Text(e.title, style = Small.copy(color = Txt), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The waking day as an arc, filled up to now. Drawn once per read of LoR; nothing animates. */
@Composable
private fun DayArc(lor: DeviceHome) {
    val zone = lor.zone
    val done = (1 - lor.day.remainingFraction).toFloat().coerceIn(0f, 1f)
    val left = Duration.between(lor.now, lor.day.wakeEnd).coerceAtLeast(Duration.ZERO)
    val minutes = left.toMinutes()
    val leftText = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} m left" else "$minutes m left"
    Canvas(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(30.dp).semantics { contentDescription = "Day $leftText" }) {
        val a = Offset(4f, size.height - 3f)
        val c = Offset(size.width / 2, -size.height * 0.55f)
        val b = Offset(size.width - 4f, size.height - 3f)
        fun at(t: Float) = Offset(
            (1 - t) * (1 - t) * a.x + 2 * (1 - t) * t * c.x + t * t * b.x,
            (1 - t) * (1 - t) * a.y + 2 * (1 - t) * t * c.y + t * t * b.y,
        )
        val whole = Path().apply { moveTo(a.x, a.y); quadraticTo(c.x, c.y, b.x, b.y) }
        drawPath(whole, Frame, style = Stroke(2.dp.toPx()))
        val part = Path().apply {
            moveTo(a.x, a.y)
            for (i in 1..40) at(done * i / 40).let { lineTo(it.x, it.y) }
        }
        drawPath(part, Or, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Or, 4.dp.toPx(), at(done))
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp)) {
        Text(lor.day.wakeStart.atZone(zone).format(Clock), style = Small.copy(color = Dim))
        Text("now · $leftText", style = Small, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.Center)
        Text(lor.day.wakeEnd.atZone(zone).format(Clock), style = Small.copy(color = Dim))
    }
}

// ---- T3 ----

/** One line when T3 can't show its threads; nothing while it is live. Unpaired and refused get [PairCard]. */
@Composable
private fun T3State(status: T3Connection.Status, loaded: Boolean) {
    val text = when (status) {
        T3Connection.Status.NotPaired, is T3Connection.Status.Rejected -> null
        is T3Connection.Status.Retrying -> "T3 is disconnected; trying again. Its approvals aren't here and actions are off until it's back."
        T3Connection.Status.Live -> null
        T3Connection.Status.Connecting, T3Connection.Status.Stopped -> if (loaded) null else "Connecting to T3…"
    } ?: return
    if (status is T3Connection.Status.Retrying) Failed(text, Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp))
    else Text(text, style = Small, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp))
}

/** Machinery that failed, the same everywhere (launcher polish 3.6): a coral dot and the reason in the muted colour. */
@Composable
internal fun Failed(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(Bad, CircleShape))
        Text(text, style = Small, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningRow(thread: T3Thread, open: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = "Open in T3", onClick = open)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(Cy, CircleShape))
        Text(thread.title, style = Body.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text(thread.projectTitle, style = Small, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * A T3 card. [onText] is what a tap on its text does (the wide form chooses the thread; elsewhere
 * nothing); [selected] draws the selection; [expanded] opens a question's choices from the start
 * (the thread column, which shows what the thread waits on in full).
 */
@Composable
internal fun T3QueueCard(card: T3Card, model: AgentModel, live: Boolean, selected: Boolean = false, onText: (() -> Unit)? = null, expanded: Boolean = false) {
    val busy = card.key in model.busy
    val ready = live && !busy
    var open by remember(card.key) { mutableStateOf(expanded) }
    val color = if (card.kind == T3Card.Kind.FAILED) Bad else Or
    val detail = model.failures[card.key]
        ?: when (card.kind) {
            T3Card.Kind.APPROVAL -> "Asks to run a ${card.request!!.kind.replace('_', ' ')}"
            T3Card.Kind.FAILED -> card.thread.lastError
            T3Card.Kind.QUESTION -> null
        }
    QueueCard(
        tile = "T3", tileColor = Raised, tileLabel = "Open in T3", onTile = { model.openThread(card.thread.id, LaunchFrom.AGENT_ICON) },
        kind = card.kind.label, kindColor = color, where = card.thread.projectTitle, title = card.thread.title,
        detail = detail, detailColor = if (card.key in model.failures) Bad else Mute,
        below = if (card.kind == T3Card.Kind.QUESTION && open) ({ Choices(card, model, live, busy) }) else null,
        onText = onText, textLabel = "Show the thread", selected = selected,
    ) {
        when (card.kind) {
            T3Card.Kind.APPROVAL -> {
                Pill("Deny", enabled = ready) { model.approve(card, approve = false) }
                Pill(if (busy) "…" else "Approve", primary = true, enabled = ready) { model.approve(card, approve = true) }
            }
            T3Card.Kind.QUESTION -> Pill(if (open) "Close" else "Answer", primary = !open) { open = !open }
            T3Card.Kind.FAILED -> Pill("Open", primary = true) { model.openThread(card.thread.id, LaunchFrom.AGENT_CARD) }
        }
    }
}

/**
 * A question's choices, read from its thread when the connection is live and again after each
 * reconnect; what was chosen survives the reread. Answers follow T3's rules ([UserInput]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(card: T3Card, model: AgentModel, live: Boolean, busy: Boolean) {
    val request = card.request!!
    var questions by remember(request.requestId) { mutableStateOf<Result<List<Question>>?>(null) }
    var drafts by remember(request.requestId) { mutableStateOf(emptyMap<String, UserInput.Draft>()) }
    LaunchedEffect(request.requestId, live) {
        if (!live) return@LaunchedEffect
        questions = try {
            Result.success(model.connection.questions(request)).also { r -> drafts = UserInput.seed(r.getOrThrow(), drafts) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    val loaded = questions
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            loaded == null -> Text(if (live) "Reading the question…" else "Waiting for T3…", style = Small)
            loaded.isFailure -> Text("Can't read the question: ${T3Connection.describe(loaded.exceptionOrNull()!!)}", style = Small)
            loaded.getOrThrow().isEmpty() -> Text("The question isn't in the thread. Open it in T3.", style = Small)
            else -> {
                val list = loaded.getOrThrow()
                list.forEach { q ->
                    Text(q.question, style = Body.copy(fontSize = 13.sp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        q.options.forEach { o ->
                            Pill(o.label, primary = UserInput.isSelected(q, drafts[q.id], o.answer)) {
                                drafts = drafts + (q.id to UserInput.toggle(q, drafts[q.id], o.answer))
                            }
                        }
                    }
                    if (q.allowCustomAnswer) {
                        Field(drafts[q.id]?.custom.orEmpty(), "Or type an answer") { drafts = drafts + (q.id to UserInput.setCustom(q, drafts[q.id], it)) }
                    }
                }
                val answers = UserInput.build(list, drafts)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Pill(if (busy) "…" else "Send answer", primary = true, enabled = live && !busy && answers != null) { model.answer(card, answers!!) }
                }
            }
        }
    }
}

// ---- Shared parts ----

@Composable
private fun QueueCard(
    tile: String,
    tileColor: Color,
    tileLabel: String,
    onTile: () -> Unit,
    kind: String,
    kindColor: Color,
    where: String,
    title: String,
    detail: String?,
    detailColor: Color = Mute,
    below: (@Composable () -> Unit)? = null,
    onText: (() -> Unit)? = null,
    textLabel: String = tileLabel,
    selected: Boolean = false,
    actions: @Composable () -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth()
            .framedPanel(StateFill)
            .then(if (selected) Modifier.selection(Chamfer) else Modifier)
            .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    // A 48 dp touch target around the 32 dp tile (3.6's critique).
                    .minimumInteractiveComponentSize()
                    // 32 dp, wider and taller when a large font needs it, so "LoR" is never clipped.
                    .widthIn(min = 32.dp)
                    .heightIn(min = 32.dp)
                    .background(tileColor, RectangleShape)
                    .clickable(role = Role.Button, onClickLabel = tileLabel, onClick = onTile)
                    .semantics { contentDescription = tileLabel }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) { Text(tile, style = Type.label.copy(color = Txt), maxLines = 1, softWrap = false) }
            BesideOrUnder(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .then(if (onText != null) Modifier.clickable(onClickLabel = textLabel, onClick = onText) else Modifier)
                        .padding(horizontal = 10.dp),
                ) {
                    Text(
                        buildString { append(kind); append(" · "); append(where) },
                        style = Small.copy(color = kindColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(title, style = Body.copy(fontSize = 13.5.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    detail?.let { Text(it, style = Small.copy(color = detailColor), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
            }
        }
        below?.invoke()
    }
}

/**
 * A card's text and its actions: side by side while that leaves the text at least 100 dp times the
 * font scale, else the actions under the text at its end, so a large font or a narrow cell
 * (a block in a stack) never squeezes the text into a column a word wide.
 */
@Composable
private fun BesideOrUnder(modifier: Modifier, content: @Composable () -> Unit) {
    val minText = 100.dp * LocalDensity.current.fontScale
    Layout(content, modifier) { measurables, constraints ->
        val (text, actions) = measurables
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val a = actions.measure(loose)
        val width = constraints.maxWidth
        if (width - a.width >= minText.roundToPx()) {
            val t = text.measure(loose.copy(minWidth = width - a.width, maxWidth = width - a.width))
            val h = maxOf(t.height, a.height)
            layout(width, h) {
                t.placeRelative(0, (h - t.height) / 2)
                a.placeRelative(width - a.width, (h - a.height) / 2)
            }
        } else {
            val t = text.measure(loose.copy(minWidth = width, maxWidth = width))
            layout(width, t.height + a.height) {
                t.placeRelative(0, 0)
                a.placeRelative(width - a.width, t.height)
            }
        }
    }
}

@Composable
internal fun Pill(text: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RectangleShape
    val base = if (primary) Modifier.background(Or, shape) else Modifier.border(1.dp, if (enabled) Edge else Line, shape)
    Text(
        text,
        style = Type.button.copy(
            color = when {
                primary -> OnOr
                enabled -> Txt
                else -> Dim
            },
            fontSize = 12.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
        ),
        // The pill stays small; the space around it makes the touch target 48 dp, as Material's buttons do.
        modifier = Modifier.minimumInteractiveComponentSize().then(if (enabled) Modifier else Modifier.alpha(0.45f)).then(base).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
internal fun Field(value: String, placeholder: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value,
        onChange,
        modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Dim) },
        textStyle = Body,
        shape = RectangleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Txt, unfocusedTextColor = Txt, cursorColor = Or,
            focusedBorderColor = Or.copy(alpha = 0.7f), unfocusedBorderColor = Edge,
        ),
    )
}

/** The quick-send button: a paper plane in a light circle, as in the mockup. */
@Composable
private fun SendButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(52.dp)
            .background(Or)
            .clickable(role = Role.Button, onClickLabel = "Quick send", onClick = onClick)
            .semantics { contentDescription = "Quick send" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val plane = Path().apply {
                moveTo(0.1f * w, 0.1f * h); lineTo(0.95f * w, 0.5f * h); lineTo(0.1f * w, 0.9f * h); lineTo(0.22f * w, 0.5f * h); close()
            }
            drawPath(plane, OnOr)
        }
    }
}
