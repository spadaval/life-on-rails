package com.spadaval.lor.agent

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

internal val Ink = Color(0xFFECEBE6)
internal val Muted = Color(0xFF9097A1)
internal val Dim = Color(0xFF616871)
internal val Line = Color(0x14FFFFFF)
internal val CardFill = Color(0xDD15181D)
private val Veil = Color(0xD90B0D10)
internal val Amber = Color(0xFFE3A857)
private val Green = Color(0xFF84B98F)
internal val Red = Color(0xFFE2806F)
private val Blue = Color(0xFF82A7E2)
private val T3Tile = Color(0xFF3B3F8F)
private val LorTile = Color(0xFF2C4A3A)

internal val Body = TextStyle(color = Ink, fontSize = 14.sp)
internal val Small = TextStyle(color = Muted, fontSize = 12.sp)
private val Heading = TextStyle(color = Dim, fontSize = 11.sp, letterSpacing = 0.8.sp)

@Composable
fun AgentPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val model = remember { AgentModel(context, scope) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { model.whileVisible() } }
    DisposableEffect(model) { onDispose { model.dispose() } }

    val status by model.connection.status.collectAsState()
    val shell by model.connection.shell.collectAsState()
    val live = status == T3Connection.Status.Live
    val now = model.lor?.now ?: Instant.now()
    val cards = AgentQueue.cards(shell.threads, model.answered, now)
    val running = AgentQueue.running(shell.threads)

    // A dark veil over the wallpaper, as the mockup's page has, so the text reads on any wallpaper.
    BoxWithConstraints(Modifier.fillMaxSize().background(Veil).safeDrawingPadding()) {
        // On a large screen the page keeps to the left, leaving the right for the T3 app beside it.
        Box(Modifier.fillMaxHeight().then(if (maxWidth >= 600.dp) Modifier.width(520.dp) else Modifier.fillMaxWidth())) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 120.dp)) {
                Today(model)
                model.notice?.let { Text(it, style = Small.copy(color = Red), modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
                if (status == T3Connection.Status.NotPaired || status is T3Connection.Status.Rejected) PairCard(model.connection, status, Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                else T3State(status, shell.loaded)
                if (running.isNotEmpty()) {
                    Header("Running", running.size.toString())
                    running.forEach { RunningRow(it) { model.openThread(it.id, LaunchFrom.AGENT_CARD) } }
                }
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
                // A count only when it is the whole count: with a source unseen, "0" would claim what the page can't know.
                val count = cards.size + needs.size
                val whole = lorSource == AgentStates.Source.SEEN && t3Source == AgentStates.Source.SEEN
                Header("Needs you", if (whole || count > 0) count.toString() else null)
                cards.forEach { T3QueueCard(it, model, live) }
                needs.forEach { need ->
                    QueueCard(
                        tile = "LoR", tileColor = LorTile, tileLabel = "Open in LoR", onTile = { model.openLor(taskPath(need.id), LaunchFrom.AGENT_ICON) },
                        kind = "LoR", kindColor = Green, where = "flagged", title = need.title, detail = need.note,
                    ) { Pill("Open", primary = true) { model.openLor(taskPath(need.id), LaunchFrom.AGENT_CARD) } }
                }
                AgentStates.notes(lorSource, t3Source, cards.size, needs.size)
                    // T3State's line above already says T3 is disconnected and its approvals with it.
                    .filterNot { it == AgentStates.Note.T3_UNSEEN && status is T3Connection.Status.Retrying }
                    .forEach { note ->
                    when (note) {
                        AgentStates.Note.ALL_CLEAR -> {
                            val lor = model.lor!!
                            Column(
                                Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .fillMaxWidth()
                                    .border(1.dp, Line, RoundedCornerShape(12.dp))
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
            SendButton(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp)) { QuickSend.open(context, QuickSend.From.AGENT) }
        }
    }
}

private fun taskPath(id: String) = "/tasks?id=$id"

@Composable
private fun Header(title: String, right: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
        Text(title.uppercase(), style = Heading.copy(color = Ink, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        right?.let { Text(it, style = Heading) }
    }
}

// ---- Today ----

private val Clock = DateTimeFormatter.ofPattern("H:mm")
private val Day = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * Today from LoR. When LoR can't be reached (launcher polish 3.3), the reason in plain words and a
 * Retry; what LoR said last stays, greyed, with the time it said it.
 */
@Composable
private fun Today(model: AgentModel) {
    val lor = model.lor
    val error = model.lorError
    val zone = lor?.zone ?: ZoneId.systemDefault()
    Header("Today", (lor?.now ?: Instant.now()).atZone(zone).format(Day))
    if (lor == null) {
        if (error == null) Text("Reading LoR…", style = Small, modifier = Modifier.padding(horizontal = 18.dp))
        else Unreachable(error, model)
        return
    }
    // Greyed while it isn't fresh: LoR can't be reached, or no read has ended since the page opened.
    Column(Modifier.alpha(if (error != null || !model.lorRead) 0.6f else 1f)) { TodayBody(lor, zone) }
    if (error != null) Unreachable("$error Showing what it said at ${lor.now.atZone(zone).format(Clock)}.", model)
}

/** Why LoR can't be read, with a Retry that reads at once. */
@Composable
private fun Unreachable(text: String, model: AgentModel) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(Red, CircleShape))
        Text(text, style = Small, modifier = Modifier.weight(1f).padding(start = 8.dp, end = 10.dp))
        Pill(if (model.retrying) "Trying…" else "Retry", enabled = !model.retrying) { model.retry() }
    }
}

/** A source the queue can't see, in place of an all-clear. */
@Composable
private fun Unseen(text: String) {
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
            style = Small.copy(color = if (running.paused) Muted else Green, fontSize = 13.sp),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp),
        )
    }
    if (one != null) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp)) {
            Text("NEXT · ${one.kicker.uppercase()}", style = Heading.copy(color = Green))
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
                    Text(e.title, style = Small.copy(color = Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        drawPath(whole, Color(0x22FFFFFF), style = Stroke(2.dp.toPx()))
        val part = Path().apply {
            moveTo(a.x, a.y)
            for (i in 1..40) at(done * i / 40).let { lineTo(it.x, it.y) }
        }
        drawPath(part, Green, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Green, 4.dp.toPx(), at(done))
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp)) {
        Text(lor.day.wakeStart.atZone(zone).format(Clock), style = Small.copy(color = Dim, fontSize = 10.sp))
        Text("now · $leftText", style = Small.copy(fontSize = 10.sp), modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.Center)
        Text(lor.day.wakeEnd.atZone(zone).format(Clock), style = Small.copy(color = Dim, fontSize = 10.sp))
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
        Box(Modifier.size(6.dp).background(Red, CircleShape))
        Text(text, style = Small, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningRow(thread: T3Thread, open: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open in T3", onClick = open)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(Blue, CircleShape))
        Text(thread.title, style = Body.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text(thread.projectTitle, style = Small, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun T3QueueCard(card: T3Card, model: AgentModel, live: Boolean) {
    val busy = card.key in model.busy
    val ready = live && !busy
    var open by remember(card.key) { mutableStateOf(false) }
    val color = if (card.kind == T3Card.Kind.FAILED) Red else Amber
    val detail = model.failures[card.key]
        ?: when (card.kind) {
            T3Card.Kind.APPROVAL -> "Asks to run a ${card.request!!.kind.replace('_', ' ')}"
            T3Card.Kind.FAILED -> card.thread.lastError
            T3Card.Kind.QUESTION -> null
        }
    QueueCard(
        tile = "T3", tileColor = T3Tile, tileLabel = "Open in T3", onTile = { model.openThread(card.thread.id, LaunchFrom.AGENT_ICON) },
        kind = card.kind.label, kindColor = color, where = card.thread.projectTitle, title = card.thread.title,
        detail = detail, detailColor = if (card.key in model.failures) Red else Muted,
        below = if (card.kind == T3Card.Kind.QUESTION && open) ({ Choices(card, model, live, busy) }) else null,
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
    detailColor: Color = Muted,
    below: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth()
            .background(CardFill, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(tileColor, RoundedCornerShape(9.dp))
                    .clickable(role = Role.Button, onClickLabel = tileLabel, onClick = onTile)
                    .semantics { contentDescription = tileLabel },
                contentAlignment = Alignment.Center,
            ) { Text(tile, style = TextStyle(color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)) }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    buildString { append(kind); append(" · "); append(where) },
                    style = Small.copy(fontSize = 11.sp, color = kindColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(title, style = Body.copy(fontSize = 13.5.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                detail?.let { Text(it, style = Small.copy(fontSize = 11.5.sp, color = detailColor), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { actions() }
        }
        below?.invoke()
    }
}

@Composable
internal fun Pill(text: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    val base = if (primary) Modifier.background(if (enabled) Ink else Muted, shape) else Modifier.border(1.dp, if (enabled) Color(0x33FFFFFF) else Line, shape)
    Text(
        text,
        style = TextStyle(
            color = when {
                primary -> Color(0xFF0B0D10)
                enabled -> Ink
                else -> Dim
            },
            fontSize = 12.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
        ),
        // The pill stays small; the space around it makes the touch target 48 dp, as Material's buttons do.
        modifier = Modifier.minimumInteractiveComponentSize().then(base).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp),
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink, unfocusedTextColor = Ink, cursorColor = Amber,
            focusedBorderColor = Muted, unfocusedBorderColor = Color(0x2AFFFFFF),
        ),
    )
}

/** The quick-send button: a paper plane in a light circle, as in the mockup. */
@Composable
private fun SendButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(52.dp)
            .background(Ink, CircleShape)
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
            drawPath(plane, Color(0xFF0B0D10))
        }
    }
}
