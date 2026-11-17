package com.spadaval.lor.agent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorRows
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.UserInput
import com.spadaval.lor.ui.BodyText
import com.spadaval.lor.ui.Chamfer
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Failed
import com.spadaval.lor.ui.Field
import com.spadaval.lor.ui.Frame
import com.spadaval.lor.ui.Line
import com.spadaval.lor.ui.LocalClock
import com.spadaval.lor.ui.MenuItem
import com.spadaval.lor.ui.More
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.OnOr
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.Pill
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.SectionHeader
import com.spadaval.lor.ui.SmallText
import com.spadaval.lor.ui.StateFill
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Unreachable
import com.spadaval.lor.ui.Unseen
import com.spadaval.lor.ui.Veil
import com.spadaval.lor.ui.framedPanel
import com.spadaval.lor.ui.selection
import kotlin.time.Duration

/*
 * The agent page (T3 Launcher, Increment 3.3), the left-most page of home: Today from LoR, the
 * running T3 turns, and the queue (T3 approvals, questions and failures, then LoR's Needs you),
 * with one action per card and a quick-send button that opens the sheet (Increment 4). After the
 * mockup's "Agent page" (design/t3-launcher/t3-launcher-screens.html). Every card's icon opens its
 * place: the T3 app at the thread, or LoR's dashboard. Nothing moves on its own: the page redraws
 * when data changes. Drawn from [PanelUi]; the app's `AgentPage()` builds it (lor-rgx4).
 */

/** The wide form's left column is at most this wide; the chosen thread takes the rest. */
private val LEFT_MAX = 420.dp

/**
 * The agent page. One column for both forms: the narrow page, and the wide form's left column (600 dp
 * and up), which adds the Threads section under it, with the chosen thread on the right.
 */
@Composable
fun AgentPage(ui: PanelUi, on: PanelActions, grid: InGrid = InGrid()) {
    // Held here, so a fold (which changes the form in place) keeps its place.
    val scroll = rememberScrollState()
    // A dark veil over the wallpaper, as the mockup's page has, so the text reads on any wallpaper.
    BoxWithConstraints(Modifier.fillMaxSize().background(Veil).then(grid.insets)) {
        val wide = maxWidth >= 600.dp
        val leftWidth = minOf(LEFT_MAX, maxWidth / 2)
        val t3 = ui.t3
        // The thread the wide form's right column shows (lor-iabt): the one chosen, else the first
        // T3 card's, else the most recently updated.
        val cards = AgentQueue.cards(t3.threads, t3.answered, ui.lor.home?.now ?: ui.lor.now)
        val shown = if (wide) ThreadChoice.shown(t3.threads, cards, ui.chosen) else null
        val choose: ((T3Thread) -> Unit)? = if (wide) ({ on.choose(it.id) }) else null
        // The column ends at the keyboard (lor-sqfe: home's window doesn't pan), so a focused field,
        // an answer on a queue card, the pairing link or the reply box, sits above it.
        Row(Modifier.fillMaxSize().imePadding()) {
            Box(if (wide) Modifier.width(leftWidth).fillMaxHeight() else Modifier.fillMaxSize()) {
                ScrollColumn(scroll, Modifier.fillMaxWidth(), bottom = 120.dp, grid) {
                    Today(ui.lor, on)
                    ui.notice?.let { Text(it, style = SmallText.copy(color = com.spadaval.lor.ui.Bad), modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
                    if (!t3.paired) PairCard(t3, on.pair, Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    else T3State(t3)
                    Running(ui, on, choose = choose, chosen = shown?.id)
                    NeedsYou(ui, on, choose = choose, chosen = shown?.id)
                    if (wide) Threads(ThreadChoice.others(t3.threads, cards), shown?.id, ui.lor, choose!!)
                }
                // At the column's bottom-end corner (6.8: it floated mid-screen on the Fold), and
                // under the keyboard rather than over the field being typed in.
                SendButton(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp), on.quickSend)
            }
            if (wide) ThreadColumn(ui, on, shown, cards.firstOrNull { it.thread.id == shown?.id }, Modifier.weight(1f).fillMaxHeight(), grid)
        }
    }
}

/** A column of a panel that scrolls on its own; its scroll takes a vertical swipe until its edge, then home's grid moves (lor-qdmy). */
@Composable
fun ScrollColumn(scroll: ScrollState, modifier: Modifier, bottom: Dp, grid: InGrid, content: @Composable () -> Unit) {
    Column(modifier.fillMaxHeight().then(grid.scrolls(scroll, false)).verticalScroll(scroll).padding(top = 12.dp, bottom = bottom)) { content() }
}

/**
 * T3's turns running now, at most [max] then "N more" ([More]): the agent page's Running section,
 * where it shows only with something running, and the `running` block ([block]), which always
 * shows and says why it is empty. In the wide form a row's tap [choose]s its thread for the right
 * column, and the [chosen] one carries the selection.
 */
@Composable
fun Running(
    ui: PanelUi,
    on: PanelActions,
    max: Int? = null,
    more: (() -> Unit)? = null,
    block: Boolean = false,
    choose: ((T3Thread) -> Unit)? = null,
    chosen: String? = null,
) {
    val t3 = ui.t3
    val running = AgentQueue.running(t3.threads)
    if (running.isEmpty() && !block) return
    val seen = t3.live && t3.loaded
    SectionHeader("Running", if (seen || running.isNotEmpty()) running.size.toString() else null)
    val (shown, hidden) = LorRows.cap(running, max)
    shown.forEach { t ->
        key(t.id) {
            if (choose != null) ThreadRow(t, t.id == chosen, ui.lor) { choose(t) }
            else RunningRow(t) { on.openThread(t.id, Tap.CARD) }
        }
    }
    if (hidden > 0) More(hidden, more)
    if (running.isEmpty()) {
        Unseen(
            when {
                seen -> "Nothing is running on T3."
                !t3.paired -> "T3 isn't paired. Pair it on the agent page."
                t3.link == T3Link.CONNECTING -> "Connecting to T3…"
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
fun NeedsYou(
    ui: PanelUi,
    on: PanelActions,
    max: Int? = null,
    more: (() -> Unit)? = null,
    block: Boolean = false,
    choose: ((T3Thread) -> Unit)? = null,
    chosen: String? = null,
) {
    val t3 = ui.t3
    val lor = ui.lor
    val home = lor.home
    val cards = AgentQueue.cards(t3.threads, t3.answered, home?.now ?: lor.now)
    val needs = home?.needsYou.orEmpty()
    val lorSource = when {
        lor.error != null -> AgentStates.Source.UNSEEN
        home != null && lor.current -> AgentStates.Source.SEEN
        else -> AgentStates.Source.READING
    }
    val t3Source = when {
        t3.live && t3.loaded -> AgentStates.Source.SEEN
        t3.link == T3Link.CONNECTING -> AgentStates.Source.READING
        else -> AgentStates.Source.UNSEEN
    }
    // A number once both sources are read, or whenever something is listed; with nothing
    // listed and a source not yet seen, none: "0" would claim what the page can't know.
    val count = cards.size + needs.size
    val whole = lorSource == AgentStates.Source.SEEN && t3Source == AgentStates.Source.SEEN
    SectionHeader("Needs you", if (whole || count > 0) count.toString() else null)
    val items = cards.map { c ->
        c.key to @Composable {
            T3QueueCard(c, ui, on, selected = c.thread.id == chosen, onText = choose?.let { f -> { f(c.thread) } })
        }
    } +
        // LoR's flags greyed while they aren't current (launcher polish 3.3's rule); T3's cards are live.
        needs.map { "lor:${it.id}" to @Composable { Box(Modifier.alpha(if (lorSource == AgentStates.Source.SEEN) 1f else 0.6f)) { LorFlagCard(lor, on, it) } } }
    val (shown, hidden) = LorRows.cap(items, max)
    shown.forEach { (k, card) -> key(k) { card() } }
    if (hidden > 0) More(hidden, more)
    AgentStates.notes(lorSource, t3Source, cards.size, needs.size)
        // On the page, T3State's line above already says T3 is disconnected and its approvals with it.
        .filterNot { it == AgentStates.Note.T3_UNSEEN && t3.link == T3Link.RETRYING && !block }
        .forEach { note ->
            when (note) {
                AgentStates.Note.ALL_CLEAR -> {
                    val h = home!!
                    Column(
                        Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .border(1.dp, Line, RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text("All clear.", style = BodyText.copy(fontSize = 13.sp))
                        Text(AgentStates.next(h.day.events, h.now, h.zone), style = SmallText.copy(fontSize = 12.5.sp), modifier = Modifier.padding(top = 2.dp))
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
fun LorFlagCard(lor: LorUi, on: PanelActions, need: LorHome.Need) {
    val path = LorRows.taskPath(need.id)
    val busy = need.id in lor.busy
    val failure = lor.failures[need.id]
    QueueCard(
        tile = "LoR", tileColor = Raised, tileLabel = "Open in LoR", onTile = { on.openLor(path, Tap.ICON) },
        kind = "LoR", kindColor = Mute, where = "flagged", title = need.title, detail = failure ?: need.note,
        detailColor = if (failure != null) com.spadaval.lor.ui.Bad else Mute,
        onText = { on.openLor(path, Tap.CARD) },
        anchor = "flag:${need.id}",
    ) {
        Pill("Seen", enabled = !busy) { on.task(need.id, need.title, TaskWrite.SEEN, "Seen: ${need.title}", null, null) }
        if (need.google) Text("Google Tasks", style = SmallText, modifier = Modifier.padding(horizontal = 4.dp))
        else Pill("Done", primary = true, enabled = !busy) {
            on.task(need.id, need.title, TaskWrite.DONE, "Done: ${need.title}. Undo reopens it, without the flag.", TaskWrite.REOPEN, null)
        }
    }
}

// ---- Today ----

/**
 * Today from LoR. When LoR can't be reached (launcher polish 3.3), the reason in plain words and a
 * Retry; what LoR said last stays, greyed, with the time it said it.
 */
@Composable
fun Today(lor: LorUi, on: PanelActions) {
    val home = lor.home
    val error = lor.error
    val zone = home?.zone ?: lor.zone
    SectionHeader("Today", LocalClock.day(home?.now ?: lor.now, zone))
    if (home == null) {
        if (error == null) Text("Reading LoR…", style = SmallText, modifier = Modifier.padding(horizontal = 18.dp))
        else Unreachable(error, lor.retrying, on.retry)
        return
    }
    // Greyed while it isn't fresh: LoR can't be reached, or no read has ended since the page opened.
    Column(Modifier.alpha(if (error != null || !lor.current) 0.6f else 1f)) { TodayBody(home) }
    TimerControls(lor, on, home.day.running)
    if (error != null) Unreachable("$error Showing what it said at ${LocalClock.time(home.now, home.zone)}.", lor.retrying, on.retry)
}

/**
 * The timer's controls under Today (lor-rz50), as the web's timer card: with one running, Pause
 * (Resume while paused) and Stop; with none, Start, whose menu offers "Untitled" (no intent) and
 * the intents timed most recently, read each time it opens. Each sends `POST /device/timer` and
 * shows what its reply says is running; no Undo, as on the web. Disabled while a call is under
 * way; a failure says why under them.
 */
@Composable
private fun TimerControls(lor: LorUi, on: PanelActions, running: LorHome.Running?) {
    val busy = LorUi.TIMER in lor.busy
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (running == null) Text("No timer running.", style = SmallText, modifier = Modifier.weight(1f))
        else Spacer(Modifier.weight(1f))
        if (running != null) {
            Box(Modifier.labAnchor("timer-pause")) { Pill(if (running.paused) "Resume" else "Pause", enabled = !busy) { on.timer(if (running.paused) "resume" else "pause", null) } }
            Spacer(Modifier.width(5.dp))
            Box(Modifier.labAnchor("timer-stop")) { Pill("Stop", enabled = !busy) { on.timer("stop", null) } }
        } else {
            Box(Modifier.labAnchor("timer-start")) {
                Pill("Start", enabled = !busy) { menu = true }
                if (menu) StartMenu(on) { intentId ->
                    menu = false
                    if (intentId != null) on.timer("start", intentId.ifEmpty { null })
                }
            }
        }
    }
    lor.failures[LorUi.TIMER]?.let { Failed(it, Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp)) }
}

/**
 * The Start menu: "Untitled" at once, then "Reading…" until the recent intents come, or why they
 * couldn't be read. [chosen] gets the intent's id, "" for Untitled, or null when the menu closes
 * with nothing chosen.
 */
@Composable
private fun StartMenu(on: PanelActions, chosen: (String?) -> Unit) {
    val recent = on.recentIntents()
    DropdownMenu(true, { chosen(null) }, containerColor = Raised, shape = RectangleShape) {
        Box(Modifier.labAnchor("timer-untitled")) { MenuItem("Untitled") { chosen("") } }
        val intents = recent?.value
        when {
            recent == null -> Text("Reading…", style = SmallText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
            intents == null -> Text(recent.error.orEmpty(), style = SmallText, modifier = Modifier.widthIn(max = 260.dp).padding(horizontal = 12.dp, vertical = 10.dp))
            else -> intents.forEach { i -> MenuItem(i.title) { chosen(i.id) } }
        }
    }
}

@Composable
private fun TodayBody(home: LorHome) {
    val zone = home.zone
    DayArc(home)
    val running = home.day.running
    val one = home.one?.takeUnless { it.isTimer }
    if (running != null) {
        val since = LocalClock.time(running.start, zone)
        Text(
            if (running.paused) "Paused: ${running.title}" else "Timer: ${running.title} · since $since",
            style = SmallText.copy(color = if (running.paused) Mute else Or, fontSize = 13.sp),
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp),
        )
    }
    if (one != null) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp)) {
            Text("NEXT · ${one.kicker.uppercase()}", style = Type.title)
            Text(one.text, style = BodyText.copy(fontSize = 17.sp, lineHeight = 22.sp), modifier = Modifier.padding(top = 3.dp))
            one.why?.let { Text(it, style = SmallText, modifier = Modifier.padding(top = 2.dp)) }
        }
    }
    val events = home.day.events.take(3)
    if (events.isNotEmpty()) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            events.forEach { e ->
                Row {
                    Text(LocalClock.time(e.start, zone), style = SmallText, modifier = Modifier.widthIn(min = 44.dp))
                    Text(e.title, style = SmallText.copy(color = Txt), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The waking day as an arc, filled up to now. Drawn once per read of LoR; nothing animates. */
@Composable
private fun DayArc(home: LorHome) {
    val zone = home.zone
    val done = (1 - home.day.remainingFraction).toFloat().coerceIn(0f, 1f)
    val left = (home.day.wakeEnd - home.now).coerceAtLeast(Duration.ZERO)
    val minutes = left.inWholeMinutes
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
        Text(LocalClock.time(home.day.wakeStart, zone), style = SmallText.copy(color = Dim))
        Text("now · $leftText", style = SmallText, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.Center)
        Text(LocalClock.time(home.day.wakeEnd, zone), style = SmallText.copy(color = Dim))
    }
}

// ---- T3 ----

/** One line when T3 can't show its threads; nothing while it is live. Unpaired and refused get [PairCard]. */
@Composable
private fun T3State(t3: T3Ui) {
    val text = when (t3.link) {
        T3Link.NOT_PAIRED, T3Link.REJECTED -> null
        T3Link.RETRYING -> "T3 is disconnected; trying again. Its approvals aren't here and actions are off until it's back."
        T3Link.LIVE -> null
        T3Link.CONNECTING -> if (t3.loaded) null else "Connecting to T3…"
    } ?: return
    if (t3.link == T3Link.RETRYING) Failed(text, Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp))
    else Text(text, style = SmallText, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp))
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
        Text(thread.title, style = BodyText.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text(thread.projectTitle, style = SmallText, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * A T3 card. [onText] is what a tap on its text does (the wide form chooses the thread; elsewhere
 * nothing); [selected] draws the selection; [expanded] opens a question's choices from the start
 * (the thread column, which shows what the thread waits on in full).
 */
@Composable
fun T3QueueCard(card: T3Card, ui: PanelUi, on: PanelActions, selected: Boolean = false, onText: (() -> Unit)? = null, expanded: Boolean = false) {
    val t3 = ui.t3
    val live = t3.live
    val busy = card.key in t3.busy
    val ready = live && !busy
    var open by remember(card.key) { mutableStateOf(expanded) }
    val color = if (card.kind == T3Card.Kind.FAILED) com.spadaval.lor.ui.Bad else Or
    val detail = t3.failures[card.key]
        ?: when (card.kind) {
            T3Card.Kind.APPROVAL -> "Asks to run a ${card.request!!.kind.replace('_', ' ')}"
            T3Card.Kind.FAILED -> card.thread.lastError
            T3Card.Kind.QUESTION -> null
        }
    // Anchors for the lab's gesture scripts; the thread column's copy of a card has its own.
    val anchor = (if (expanded) "waits:" else "card:") + card.thread.id
    QueueCard(
        tile = "T3", tileColor = Raised, tileLabel = "Open in T3", onTile = { on.openThread(card.thread.id, Tap.ICON) },
        kind = card.kind.label, kindColor = color, where = card.thread.projectTitle, title = card.thread.title,
        detail = detail, detailColor = if (card.key in t3.failures) com.spadaval.lor.ui.Bad else Mute,
        below = if (card.kind == T3Card.Kind.QUESTION && open) ({ Choices(card, on, live, busy, anchor) }) else null,
        onText = onText, textLabel = "Show the thread", selected = selected, anchor = anchor,
    ) {
        when (card.kind) {
            T3Card.Kind.APPROVAL -> {
                Box(Modifier.labAnchor("$anchor/deny")) { Pill("Deny", enabled = ready) { on.approve(card, false) } }
                Box(Modifier.labAnchor("$anchor/approve")) { Pill(if (busy) "…" else "Approve", primary = true, enabled = ready) { on.approve(card, true) } }
            }
            T3Card.Kind.QUESTION -> Box(Modifier.labAnchor("$anchor/answer")) { Pill(if (open) "Close" else "Answer", primary = !open) { open = !open } }
            T3Card.Kind.FAILED -> Pill("Open", primary = true) { on.openThread(card.thread.id, Tap.CARD) }
        }
    }
}

/**
 * A question's choices, read when they open and again after each reconnect ([PanelActions.questions]);
 * what was chosen survives the reread. Answers follow T3's rules ([UserInput]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(card: T3Card, on: PanelActions, live: Boolean, busy: Boolean, anchor: String) {
    val request = card.request!!
    val read = on.questions(request, live)
    var drafts by remember(request.requestId) { mutableStateOf(emptyMap<String, UserInput.Draft>()) }
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val list = read?.value
        when {
            read == null -> Text(if (live) "Reading the question…" else "Waiting for T3…", style = SmallText)
            list == null -> Text("Can't read the question: ${read.error}", style = SmallText)
            list.isEmpty() -> Text("The question isn't in the thread. Open it in T3.", style = SmallText)
            else -> {
                // A question with a starting answer has it until something is chosen or typed.
                val seeded = UserInput.seed(list, drafts)
                list.forEachIndexed { qi, q ->
                    Text(q.question, style = BodyText.copy(fontSize = 13.sp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        q.options.forEachIndexed { oi, o ->
                            Box(Modifier.labAnchor("$anchor/q$qi/o$oi")) {
                                Pill(o.label, primary = UserInput.isSelected(q, seeded[q.id], o.answer)) {
                                    drafts = seeded + (q.id to UserInput.toggle(q, seeded[q.id], o.answer))
                                }
                            }
                        }
                    }
                    if (q.allowCustomAnswer) {
                        Field(seeded[q.id]?.custom.orEmpty(), "Or type an answer") { drafts = seeded + (q.id to UserInput.setCustom(q, seeded[q.id], it)) }
                    }
                }
                val answers = UserInput.build(list, seeded)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.labAnchor("$anchor/send")) {
                        Pill(if (busy) "…" else "Send answer", primary = true, enabled = live && !busy && answers != null) { on.answer(card, answers!!) }
                    }
                }
            }
        }
    }
}

// ---- Shared parts ----

@Composable
internal fun QueueCard(
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
    anchor: String? = null,
    actions: @Composable () -> Unit,
) {
    Column(
        Modifier
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier)
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
                        style = SmallText.copy(color = kindColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(title, style = BodyText.copy(fontSize = 13.5.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    detail?.let { Text(it, style = SmallText.copy(color = detailColor), maxLines = 2, overflow = TextOverflow.Ellipsis) }
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

/** The quick-send button: a paper plane on amber, as in the mockup. */
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
