package com.spadaval.lor.concepts.queue

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.T3Card
import com.spadaval.lor.concepts.kit.BlurredWallpaper
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock
import kotlin.time.Instant

/*
 * The queue: the page left of home, merging the agent page and the LoR panel. Notes on what is
 * wrong first (they make the rest untrue), the timer, then the list: the lead card in the reticle,
 * every other item below it, then what the agent is working on and the rest of the day. On the
 * inner screen the list is the left pane and the open item the right.
 */
@Composable
fun QueuePage(store: ConceptStore, ui: QueueUi, wide: Boolean) {
    Box(Modifier.fillMaxSize()) {
        BlurredWallpaper({ 1f })
        Box(Modifier.fillMaxSize().background(Q.veil))
        if (!wide) QueueList(store, ui, wide = false, Modifier.fillMaxSize())
        else Row(Modifier.fillMaxSize()) {
            QueueList(store, ui, wide = true, Modifier.width(400.dp).fillMaxHeight())
            Box(Modifier.width(1.dp).fillMaxHeight().background(Q.line))
            val items = store.items()
            val key = store.open?.takeIf { k -> items.any { it.key == k } || k.startsWith("thread:") } ?: items.firstOrNull()?.key
            // The pane makes room for the keyboard, as the phone resizes a window for it.
            Box(Modifier.weight(1f).fillMaxHeight().background(Q.ground.copy(alpha = 0.35f)).padding(bottom = if (ui.keyboard) com.spadaval.lor.concepts.kit.KeyboardHeight else 0.dp)) {
                if (key != null) Detail(store, ui, key, pane = true) else Calm(store, Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun QueueList(store: ConceptStore, ui: QueueUi, wide: Boolean, modifier: Modifier) {
    val items = store.items()
    val waiting = store.waiting()
    val list = store.list("queue")
    LazyColumn(modifier.labAnchor("queue"), state = list, contentPadding = PaddingValues(start = Q.gutter, end = Q.gutter, top = 40.dp, bottom = 120.dp)) {
        item(key = "head") {
            Row(Modifier.fillMaxWidth().padding(bottom = Q.s3), verticalAlignment = Alignment.CenterVertically) {
                BasicText("Queue", style = Q.title.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.width(10.dp))
                BasicText(
                    when { waiting > 0 -> "$waiting for you"; store.knows() -> "clear"; else -> "can't tell yet" },
                    Modifier.labAnchor("queue:count"), style = Q.label,
                )
                Spacer(Modifier.weight(1f))
                IconButton(Glyph.Gear, "Settings", { ui.sheet = Sheet.Settings() }, anchor = "queue:settings")
            }
        }
        item(key = "notes") { Notes(store, ui) }
        item(key = "timer") { TimerBar(store, ui, Modifier.padding(bottom = Q.s4)) }
        if (items.isEmpty()) item(key = "calm") { Calm(store, Modifier.fillMaxWidth().padding(vertical = 40.dp)) }
        items(items, key = { it.key }) { item ->
            val lead = item == items.first()
            val selected = wide && (store.open ?: items.first().key) == item.key
            Box(Modifier.animateItem(fadeInSpec = tween(200), placementSpec = tween(240), fadeOutSpec = tween(160)).padding(bottom = if (lead) Q.s5 else Q.s3)) {
                if (item is Item.Due && !lead) DueRow(store, ui, item, wide, selected)
                else ItemCard(store, ui, item, lead, wide, selected)
            }
        }
        item(key = "agent") { AgentWork(store) }
        item(key = "later") { LaterToday(store) }
    }
}

/**
 * What is wrong, where he is already looking: a failed sync, LoR not answering (the list is what it
 * last said), T3 disconnected (its cards are as last seen).
 */
@Composable
private fun Notes(store: ConceptStore, ui: QueueUi) {
    val now = store.now
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Q.s2)) {
        if (store.syncFailed) Note(
            "Phone sync failed", "${store.sync.pending} readings waiting · ${store.sync.errors.firstOrNull().orEmpty()}",
            Q.red, Glyph.Warning, action = "Fix", onAction = { ui.sheet = Sheet.Settings("sync") }, anchor = "note:sync",
        )
        when (store.homeLoad) {
            Load.STALE -> Note("LoR isn't answering", "Showing what it said ${ago(store.homeReadAt, now)} (${store.homeError ?: "no answer"})", Q.red, Glyph.Sync, anchor = "note:lor")
            Load.FAILED -> Note("Can't reach LoR", store.homeError ?: "No answer", Q.red, Glyph.Warning, action = "Settings", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:lor")
            Load.LOADING -> Note("Reading LoR…", null, Q.text2, Glyph.Sync, anchor = "note:lor")
            Load.OK -> {}
        }
        when (store.t3Status) {
            T3Status.RETRYING -> Note("T3 is reconnecting", "Cards as T3 had them ${ago(store.t3SeenAt, now)}; answers wait until it's back", Q.text2, Glyph.Sync, anchor = "note:t3")
            T3Status.CONNECTING, T3Status.STOPPED -> Note("Connecting to T3…", null, Q.text2, Glyph.Sync, anchor = "note:t3")
            T3Status.REJECTED -> Note("T3 refused this phone", store.t3Error, Q.red, Glyph.Warning, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3")
            T3Status.NOT_PAIRED -> Note("T3 isn't paired", "Pair to see approvals and questions here", Q.text2, Glyph.Agent, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3")
            T3Status.LIVE -> {}
        }
        Spacer(Modifier.height(if (store.syncFailed || store.homeLoad != Load.OK || store.t3Status != T3Status.LIVE) Q.s2 else 0.dp))
    }
}

/** The timer as a bar: running with its readout, Switch and Stop; idle with Start. A tap opens its sheet. */
@Composable
fun TimerBar(store: ConceptStore, ui: QueueUi, modifier: Modifier = Modifier) {
    val now = liveNow(store)
    val running = store.timer
    Row(
        modifier.fillMaxWidth().clip(Q.rMd).background(if (running != null) Q.amberGlow else Q.wash).border(1.dp, if (running != null) Q.amber.copy(alpha = 0.25f) else Q.line, Q.rMd)
            .clickable { ui.sheet = Sheet.Timer }.heightIn(min = 56.dp).padding(start = 14.dp, end = 6.dp).labAnchor("queue:timer"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (running != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Q.amber))
            Spacer(Modifier.width(10.dp))
            BasicText(running.label ?: "No label", Modifier.weight(1f), style = Q.body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(readout(now - running.start), style = Q.readout, modifier = Modifier.labAnchor("queue:readout"))
            Spacer(Modifier.width(4.dp))
            IconButton(Glyph.Swap, "Switch", { ui.sheet = Sheet.Timer }, anchor = "queue:switch", tint = Q.text1)
            IconButton(Glyph.Stop, "Stop", { store.stop() }, anchor = "queue:stop", tint = Q.text1)
        } else {
            Icon(Glyph.Clock, Q.text3, size = 18.dp)
            Spacer(Modifier.width(10.dp))
            BasicText(if (store.timerReachable) "No timer running" else "Timer unavailable: LoR isn't answering", Modifier.weight(1f), style = Q.body.copy(color = Q.text2))
            QButton("Start", { ui.sheet = Sheet.Timer }, kind = Kind.QUIET, glyph = Glyph.Play, height = 40.dp, anchor = "queue:start", enabled = store.timerReachable)
        }
    }
}

/**
 * A card in the queue. The lead wears the reticle and is raised, with the one primary action; the
 * rest lie flat with plain actions. A tap opens it (a sheet on the cover, the right pane unfolded);
 * its menu is the visible ⋯.
 */
@Composable
fun ItemCard(store: ConceptStore, ui: QueueUi, item: Item, lead: Boolean, wide: Boolean, selected: Boolean) {
    Box(Modifier.fillMaxWidth()) {
        if (lead) Reticle(item.key)
        Column(
            Modifier.fillMaxWidth().padding(if (lead) 6.dp else 0.dp).card(lead)
                .then(if (selected && !lead) Modifier.border(1.dp, Q.text3, Q.rLg) else Modifier)
                .clickable { open(store, ui, item.key, wide) }
                .padding(Q.cardPad)
                .labAnchor(if (lead) "lead" else "item:${item.key}"),
        ) {
            ItemBody(store, ui, item, lead, wide)
        }
    }
}

/**
 * Opens an item: in the right pane on the inner screen, in a sheet on the cover. Both are recorded,
 * so a fold either way shows it in the other.
 */
fun open(store: ConceptStore, ui: QueueUi, key: String, @Suppress("UNUSED_PARAMETER") wide: Boolean) {
    store.open = key
    ui.sheet = Sheet.Item(key)
}

@Composable
fun ItemBody(store: ConceptStore, ui: QueueUi, item: Item, lead: Boolean, wide: Boolean, inDetail: Boolean = false) {
    val titleStyle = if (lead || inDetail) Q.title else Q.row
    val pending = store.pending[item.key]
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            when (item) {
                is Item.Card -> {
                    val c = item.card
                    val tone = if (c.kind == T3Card.Kind.FAILED) Q.red else Q.cyan
                    Kicker("${c.kind.label} · ${c.thread.projectTitle}", tone)
                    Spacer(Modifier.height(6.dp))
                    val question = store.questions(c).firstOrNull()
                    BasicText(question?.question ?: c.thread.title, style = titleStyle, maxLines = if (inDetail) 6 else 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    val sub = when (c.kind) {
                        T3Card.Kind.APPROVAL -> "Asked ${ago(parseOrNull(c.thread.updatedAt), store.now)} · the turn waits"
                        T3Card.Kind.QUESTION -> "${c.thread.title} · ${ago(parseOrNull(c.thread.updatedAt), store.now)}"
                        T3Card.Kind.FAILED -> c.thread.lastError ?: "The turn failed"
                    }
                    BasicText(sub, style = Q.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                is Item.Flag -> {
                    Kicker("Flagged for you", Q.text2)
                    Spacer(Modifier.height(6.dp))
                    BasicText(item.need.title, style = titleStyle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    item.need.note?.let { Spacer(Modifier.height(4.dp)); BasicText(it, style = Q.label, maxLines = if (inDetail) 8 else 2, overflow = TextOverflow.Ellipsis) }
                }
                is Item.Due -> {
                    Kicker(dueWords(item.task, store), if (item.task.overdue) Q.red else Q.text2)
                    Spacer(Modifier.height(6.dp))
                    TaskTitle(item.task.title, store.done[item.task.id] == true, maxLines = 3, style = titleStyle)
                }
            }
        }
        if (!inDetail) MoreButton(store, ui, item.key)
    }
    Spacer(Modifier.height(Q.s3))
    if (pending != null) Sending(store, item.key, pending)
    else Actions(store, ui, item, lead, wide)
}

/** The visible ⋯ that opens an item's menu. */
@Composable
private fun MoreButton(store: ConceptStore, ui: QueueUi, key: String) {
    var at by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    IconButton(
        Glyph.More, "More", { ui.menu = Menu.Item(key, at) },
        modifier = Modifier.padding(start = 4.dp).onGloballyPositioned { at = it.boundsInRoot() }, anchor = "more:$key", size = 40.dp,
    )
}

/** An item's actions: each clears it explicitly; T3's go after a moment to cancel, LoR's offer Undo. */
@Composable
private fun Actions(store: ConceptStore, ui: QueueUi, item: Item, lead: Boolean, wide: Boolean) {
    val primary = if (lead) Kind.PRIMARY else Kind.PLAIN
    when (item) {
        is Item.Card -> {
            val c = item.card
            when (c.kind) {
                T3Card.Kind.APPROVAL -> Row(horizontalArrangement = Arrangement.spacedBy(Q.s2)) {
                    QButton("Approve", { store.approve(c) }, kind = primary, anchor = "approve:${c.key}", enabled = store.t3Live)
                    QButton("Deny", { store.deny(c) }, anchor = "deny:${c.key}", enabled = store.t3Live)
                    QButton("Open", { open(store, ui, c.key, wide) }, kind = Kind.QUIET)
                }
                T3Card.Kind.QUESTION -> Answers(store, ui, c, lead)
                T3Card.Kind.FAILED -> Row(horizontalArrangement = Arrangement.spacedBy(Q.s2)) {
                    QButton("Open thread", { open(store, ui, c.key, wide) }, kind = primary)
                    QButton("Dismiss", { store.dismiss(c) }, anchor = "dismiss:${c.key}")
                }
            }
        }
        is Item.Flag -> Row(horizontalArrangement = Arrangement.spacedBy(Q.s2)) {
            QButton("Done", { store.flagDone(item.need.id) }, kind = primary, glyph = Glyph.Check, anchor = "done:${item.key}", enabled = store.lorLive)
            QButton("Seen", { store.seen(item.need.id) }, anchor = "seen:${item.key}", enabled = store.lorLive)
        }
        is Item.Due -> {
            val done = store.done[item.task.id] == true
            Row(horizontalArrangement = Arrangement.spacedBy(Q.s2)) {
                QButton(if (done) "Not done" else "Done", { if (done) store.untick(item.task.id) else store.tick(item.task.id) }, kind = if (done) Kind.PLAIN else primary, glyph = Glyph.Check, anchor = "tick:${item.key}", enabled = store.lorLive)
                QButton("Change date", { open(store, ui, item.key, wide) }, kind = Kind.QUIET)
            }
        }
    }
}

/**
 * A question's answers: its options as choices with their descriptions. One answer sends on a tap
 * (after a moment to cancel); several are picked and then sent; Other takes free words.
 */
@Composable
fun Answers(store: ConceptStore, ui: QueueUi, c: T3Card, lead: Boolean) {
    val q = store.questions(c).firstOrNull()
    if (q == null) {
        BasicText(if (store.t3Live) "Open the thread to answer." else "T3 is away; the question shows once it's back.", style = Q.label)
        return
    }
    val picked = ui.picked[c.key].orEmpty()
    val field = ui.field("other:${c.key}")
    var other by remember(c.key) { mutableStateOf(field.text.isNotEmpty()) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Q.s2), verticalArrangement = Arrangement.spacedBy(Q.s2)) {
        q.options.forEach { o ->
            Chip(
                o.label, selected = o.label in picked, detail = o.description.ifEmpty { null }, anchor = "option:${o.label}", enabled = store.t3Live,
                onClick = {
                    if (q.multiSelect) ui.picked[c.key] = if (o.label in picked) picked - o.label else picked + o.label
                    else store.answer(c, o.label)
                },
            )
        }
        if (q.allowCustomAnswer) Chip("Other\u2026", selected = other, onClick = { other = !other }, anchor = "option:other", enabled = store.t3Live)
    }
    val typed = field.text.toString().trim()
    val words = (picked.toList() + listOfNotNull(typed.ifEmpty { null }.takeIf { other })).joinToString(", ")
    val send = { store.answer(c, words); ui.picked.remove(c.key); field.edit { replace(0, length, "") } }
    if (other) {
        Spacer(Modifier.height(Q.s2))
        Field(ui, field, "Your answer, in your words", anchor = "answer-field", onEnter = { if (words.isNotEmpty()) send() })
    }
    if (q.multiSelect || other) {
        Spacer(Modifier.height(Q.s2))
        val n = picked.size + if (other && typed.isNotEmpty()) 1 else 0
        QButton(if (n > 1) "Send $n answers" else "Send", send, kind = if (lead) Kind.PRIMARY else Kind.PLAIN, enabled = words.isNotEmpty() && store.t3Live, anchor = "answer:send")
    }
}

/** A call on its way to T3: what it does, a bar running down, and Cancel. No Undo comes after. */
@Composable
private fun Sending(store: ConceptStore, key: String, p: ConceptStore.Pending) {
    var left by remember(p) { mutableStateOf(1f) }
    LaunchedEffect(p) {
        while (true) {
            withFrameMillis { }
            left = (1f - p.since.elapsedNow().inWholeMilliseconds.toFloat() / p.forMs).coerceIn(0f, 1f)
            if (left == 0f) break
        }
    }
    Row(Modifier.fillMaxWidth().labAnchor("sending:$key"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText("${p.verb}…", style = Q.body.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(Q.rPill).background(Q.line2)) {
                Box(Modifier.fillMaxWidth(left).height(3.dp).background(Q.cyan))
            }
        }
        Spacer(Modifier.width(Q.s3))
        QButton("Cancel", { store.cancelSend(key) }, anchor = "cancel:$key")
    }
}

/** A task due today, as a row: tick, title, when. A tap opens it to change its date. */
@Composable
private fun DueRow(store: ConceptStore, ui: QueueUi, item: Item.Due, wide: Boolean, selected: Boolean) {
    val t = item.task
    val done = store.done[t.id] == true
    Row(
        Modifier.fillMaxWidth().clip(Q.rMd).background(if (selected) Q.wash2 else Color.Transparent)
            .clickable { open(store, ui, item.key, wide) }.heightIn(min = Q.rowMin).padding(end = 4.dp).labAnchor("item:${item.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Check(done, { if (done) store.untick(t.id) else store.tick(t.id) }, anchor = "tick:${item.key}")
        Column(Modifier.weight(1f)) {
            TaskTitle(t.title, done, maxLines = 2)
            BasicText(dueWords(t, store), style = Q.label.copy(color = if (t.overdue && !done) Q.red else Q.text3))
        }
        MoreButton(store, ui, item.key)
    }
}

fun dueWords(t: com.spadaval.lor.lor.LorTasks.Task, store: ConceptStore): String {
    val due = t.due ?: return "No date"
    val days = (store.today.toEpochDays() - due.toEpochDays()).toInt()
    return when {
        days == 0 -> "Due today"
        days == 1 -> "Due yesterday"
        days > 1 -> "$days days overdue"
        days == -1 -> "Due tomorrow"
        else -> "Due in ${-days} days"
    }
}

/**
 * The signature move: the lead card wears the "now" reticle, corner brackets in amber with a small
 * label. Keyed on the lead, so when it clears the brackets close in on the card that slid up.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Reticle(key: String) {
    val drawn = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { drawn.animateTo(1f, tween(240)) }
    Canvas(Modifier.matchParentSize().graphicsLayer { alpha = drawn.value }.labAnchor("reticle")) {
        val inset = (1f - drawn.value) * 10.dp.toPx()
        val arm = 18.dp.toPx()
        val w = 2.dp.toPx()
        val l = inset; val t = inset; val r = size.width - inset; val b = size.height - inset
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(Q.amber, Offset(x, y), Offset(x + dx * arm, y), w, StrokeCap.Round)
            drawLine(Q.amber, Offset(x, y), Offset(x, y + dy * arm), w, StrokeCap.Round)
        }
        corner(l, t, 1f, 1f); corner(r, t, -1f, 1f); corner(l, b, 1f, -1f); corner(r, b, -1f, -1f)
    }
    BasicText(
        "NOW", Modifier.padding(start = 26.dp).graphicsLayer { alpha = drawn.value; translationY = -8.dp.toPx() }.padding(horizontal = 4.dp),
        style = Q.readout.copy(fontSize = Q.caps.fontSize, lineHeight = Q.caps.lineHeight, letterSpacing = Q.caps.letterSpacing),
    )
}

/** What the agent is working on: quiet rows in the agent's colour. */
@Composable
private fun AgentWork(store: ConceptStore) {
    val running = store.running
    val queued = store.home?.agentQueue.orEmpty().filter { q -> q.thread == null && q.queuedAt != null && running.none { it.title == q.title } }
    if (running.isEmpty() && queued.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("The agent is on", trailing = if (queued.isNotEmpty()) "${queued.size} queued" else null)
        running.forEach { t ->
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Q.cyan))
                Spacer(Modifier.width(12.dp))
                BasicText(t.title, Modifier.weight(1f), style = Q.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(t.runStartedAt?.let { Instant.parse(it) }?.takeIf { it < store.now }?.let { "for ${spoken(store.now - it)}" }.orEmpty(), style = Q.label)
            }
        }
        queued.take(3).forEach { q ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).border(1.5.dp, Q.cyan.copy(alpha = 0.6f), CircleShape))
                Spacer(Modifier.width(12.dp))
                BasicText(q.title, Modifier.weight(1f), style = Q.body.copy(color = Q.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("next run", style = Q.label.copy(color = Q.text3))
            }
        }
    }
}

/** The rest of the day: the next fixed events, quietly. */
@Composable
private fun LaterToday(store: ConceptStore) {
    val events = store.home?.day?.events.orEmpty().filter { it.end > store.now }
    if (events.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("Later today")
        events.take(3).forEach { e ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(LocalClock.time(e.start, store.zone), Modifier.width(56.dp), style = Q.body.copy(color = Q.text2))
                BasicText(e.title, style = Q.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Whether an empty queue means nothing waits: LoR's list was read, and T3's cards are live. */
fun ConceptStore.knows(): Boolean = home != null && t3Status == com.spadaval.lor.lab.T3Status.LIVE

/** Nothing wants him: said calmly, with what comes next. When LoR or T3 can't be read, it says it can't tell. */
@Composable
fun Calm(store: ConceptStore, modifier: Modifier = Modifier) {
    if (!store.knows()) {
        Column(modifier.labAnchor("unknown"), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Q.wash2), contentAlignment = Alignment.Center) { Icon(Glyph.Sync, Q.text2, size = 24.dp) }
            Spacer(Modifier.height(Q.s4))
            BasicText("Nothing to show yet", style = Q.title)
            Spacer(Modifier.height(4.dp))
            BasicText(if (store.home == null) "What waits on you shows once LoR answers." else "T3's cards show once it's connected.", style = Q.body.copy(color = Q.text2))
        }
        return
    }
    Column(modifier.labAnchor("calm"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(Q.wash2), contentAlignment = Alignment.Center) { Icon(Glyph.Check, Q.text1, size = 26.dp, weight = 2f) }
        Spacer(Modifier.height(Q.s4))
        BasicText("Nothing wants you", style = Q.title)
        Spacer(Modifier.height(4.dp))
        val next = store.home?.day?.events?.firstOrNull { it.end > store.now }
        BasicText(next?.let { "Next: ${it.title} at ${LocalClock.time(it.start, store.zone)}" } ?: "No approvals, flags or tasks due.", style = Q.body.copy(color = Q.text2))
    }
}
