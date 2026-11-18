package com.spadaval.lor.concepts.timeline

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
import androidx.compose.ui.draw.drawBehind
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

/**
 * What is wrong, where he is already looking: a failed sync, LoR not answering (the list is what it
 * last said), T3 disconnected (its cards are as last seen).
 */
@Composable
fun Notes(store: ConceptStore, ui: TimelineUi) {
    val now = store.now
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(T.s2)) {
        if (store.syncFailed) Note(
            "Phone sync failed", "${store.sync.pending} readings waiting · ${store.sync.errors.firstOrNull().orEmpty()}",
            T.red, Glyph.Warning, action = "Fix", onAction = { ui.sheet = Sheet.Settings("sync") }, anchor = "note:sync",
        )
        when (store.homeLoad) {
            Load.STALE -> Note("LoR isn't answering", "Showing what it said ${ago(store.homeReadAt, now)} (${store.homeError ?: "no answer"})", T.red, Glyph.Sync, anchor = "note:lor")
            Load.FAILED -> Note("Can't reach LoR", store.homeError ?: "No answer", T.red, Glyph.Warning, action = "Settings", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:lor")
            Load.LOADING -> Note("Reading LoR…", null, T.text2, Glyph.Sync, anchor = "note:lor")
            Load.OK -> {}
        }
        when (store.t3Status) {
            T3Status.RETRYING -> Note("T3 is reconnecting", "Cards as T3 had them ${ago(store.t3SeenAt, now)}; answers wait until it's back", T.text2, Glyph.Sync, anchor = "note:t3")
            T3Status.CONNECTING, T3Status.STOPPED -> Note("Connecting to T3…", null, T.text2, Glyph.Sync, anchor = "note:t3")
            T3Status.REJECTED -> Note("T3 refused this phone", store.t3Error, T.red, Glyph.Warning, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3")
            T3Status.NOT_PAIRED -> Note("T3 isn't paired", "Pair to see approvals and questions here", T.text2, Glyph.Agent, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3")
            T3Status.LIVE -> {}
        }
        Spacer(Modifier.height(if (store.syncFailed || store.homeLoad != Load.OK || store.t3Status != T3Status.LIVE) T.s2 else 0.dp))
    }
}

/**
 * A card that waits on him, lifted off the route: what it is, its title, and its actions in reach
 * of the thumb. [compact] (pinned on the cover) keeps a question's options for its sheet. A tap
 * opens it (a sheet on the cover, the right pane unfolded); its menu is the visible ⋯.
 */
@Composable
fun ItemCard(store: ConceptStore, ui: TimelineUi, item: Item, wide: Boolean, selected: Boolean, compact: Boolean = false) {
    val edge = when (item) {
        is Item.Card -> if (item.card.kind == T3Card.Kind.FAILED) T.red else T.cyan
        else -> T.amber
    }
    Column(
        Modifier.fillMaxWidth().card()
            .then(if (selected) Modifier.border(1.dp, T.text3, T.rLg) else Modifier)
            .drawBehind { drawRect(edge, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }
            .clickable { open(store, ui, item.key, wide) }
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .labAnchor("item:${item.key}"),
    ) {
        ItemBody(store, ui, item, lead = false, wide = wide, compact = compact)
    }
}

/**
 * Opens an item: in the right pane on the inner screen, in a sheet on the cover. Both are recorded,
 * so a fold either way shows it in the other.
 */
fun open(store: ConceptStore, ui: TimelineUi, key: String, @Suppress("UNUSED_PARAMETER") wide: Boolean) {
    store.open = key
    ui.sheet = Sheet.Item(key)
}

@Composable
fun ItemBody(store: ConceptStore, ui: TimelineUi, item: Item, lead: Boolean, wide: Boolean, inDetail: Boolean = false, compact: Boolean = false) {
    val titleStyle = if (lead || inDetail) T.title else T.row
    val lines = if (compact) 2 else 3
    val pending = store.pending[item.key]
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            when (item) {
                is Item.Card -> {
                    val c = item.card
                    val tone = if (c.kind == T3Card.Kind.FAILED) T.red else T.cyan
                    Kicker("${c.kind.label} · ${c.thread.projectTitle}", tone)
                    Spacer(Modifier.height(6.dp))
                    val question = store.questions(c).firstOrNull()
                    BasicText(question?.question ?: c.thread.title, style = titleStyle, maxLines = if (inDetail) 6 else lines, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    val sub = when (c.kind) {
                        T3Card.Kind.APPROVAL -> "Asked ${ago(parseOrNull(c.thread.updatedAt), store.now)} · the turn waits"
                        T3Card.Kind.QUESTION -> "${c.thread.title} · ${ago(parseOrNull(c.thread.updatedAt), store.now)}"
                        T3Card.Kind.FAILED -> c.thread.lastError ?: "The turn failed"
                    }
                    BasicText(sub, style = T.label, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
                }
                is Item.Flag -> {
                    Kicker("Flagged for you", T.amber)
                    Spacer(Modifier.height(6.dp))
                    BasicText(item.need.title, style = titleStyle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    item.need.note?.let { Spacer(Modifier.height(4.dp)); BasicText(it, style = T.label, maxLines = if (inDetail) 8 else if (compact) 1 else 2, overflow = TextOverflow.Ellipsis) }
                }
                is Item.Due -> {
                    Kicker(dueWords(item.task, store), if (item.task.overdue) T.red else T.text2)
                    Spacer(Modifier.height(6.dp))
                    TaskTitle(item.task.title, store.done[item.task.id] == true, maxLines = 3, style = titleStyle)
                }
            }
        }
        if (!inDetail) MoreButton(store, ui, item.key)
    }
    Spacer(Modifier.height(T.s3))
    if (pending != null) Sending(store, item.key, pending, if (inDetail) "detail:" else "")
    else Actions(store, ui, item, lead, wide, compact)
}

/** The visible ⋯ that opens an item's menu. */
@Composable
private fun MoreButton(store: ConceptStore, ui: TimelineUi, key: String) {
    var at by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    IconButton(
        Glyph.More, "More", { ui.menu = Menu.Item(key, at) },
        modifier = Modifier.padding(start = 4.dp).onGloballyPositioned { at = it.boundsInRoot() }, anchor = "more:$key", size = 40.dp,
    )
}

/** An item's actions: each clears it explicitly; T3's go after a moment to cancel, LoR's offer Undo. */
@Composable
private fun Actions(store: ConceptStore, ui: TimelineUi, item: Item, lead: Boolean, wide: Boolean, compact: Boolean) {
    val primary = Kind.PRIMARY
    when (item) {
        is Item.Card -> {
            val c = item.card
            when (c.kind) {
                T3Card.Kind.APPROVAL -> Row(horizontalArrangement = Arrangement.spacedBy(T.s2)) {
                    TButton("Approve", { store.approve(c) }, kind = primary, anchor = "approve:${c.key}", enabled = store.t3Live)
                    TButton("Deny", { store.deny(c) }, anchor = "deny:${c.key}", enabled = store.t3Live)
                    TButton("Open", { open(store, ui, c.key, wide) }, kind = Kind.QUIET)
                }
                T3Card.Kind.QUESTION -> if (compact) Row(horizontalArrangement = Arrangement.spacedBy(T.s2)) {
                    val q = store.questions(c).firstOrNull()
                    TButton(if (q?.multiSelect == true) "Answer (pick several)" else "Answer", { open(store, ui, c.key, wide) }, kind = primary, anchor = "answer:${c.key}", enabled = store.t3Live)
                } else Answers(store, ui, c, lead = true)
                T3Card.Kind.FAILED -> Row(horizontalArrangement = Arrangement.spacedBy(T.s2)) {
                    TButton("Open thread", { open(store, ui, c.key, wide) }, kind = primary)
                    TButton("Dismiss", { store.dismiss(c) }, anchor = "dismiss:${c.key}")
                }
            }
        }
        is Item.Flag -> Row(horizontalArrangement = Arrangement.spacedBy(T.s2)) {
            TButton("Done", { store.flagDone(item.need.id) }, kind = primary, glyph = Glyph.Check, anchor = "done:${item.key}", enabled = store.lorLive)
            TButton("Seen", { store.seen(item.need.id) }, anchor = "seen:${item.key}", enabled = store.lorLive)
        }
        is Item.Due -> {
            val done = store.done[item.task.id] == true
            Row(horizontalArrangement = Arrangement.spacedBy(T.s2)) {
                TButton(if (done) "Not done" else "Done", { if (done) store.untick(item.task.id) else store.tick(item.task.id) }, kind = if (done) Kind.PLAIN else primary, glyph = Glyph.Check, anchor = "tick:${item.key}", enabled = store.lorLive)
                TButton("Change date", { open(store, ui, item.key, wide) }, kind = Kind.QUIET)
            }
        }
    }
}

/**
 * A question's answers: its options as choices with their descriptions. One answer sends on a tap
 * (after a moment to cancel); several are picked and then sent; Other takes free words.
 */
@Composable
fun Answers(store: ConceptStore, ui: TimelineUi, c: T3Card, lead: Boolean) {
    val q = store.questions(c).firstOrNull()
    if (q == null) {
        BasicText(if (store.t3Live) "Open the thread to answer." else "T3 is away; the question shows once it's back.", style = T.label)
        return
    }
    val picked = ui.picked[c.key].orEmpty()
    val field = ui.field("other:${c.key}")
    var other by remember(c.key) { mutableStateOf(field.text.isNotEmpty()) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(T.s2), verticalArrangement = Arrangement.spacedBy(T.s2)) {
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
    // The choices and words stay until the answer has gone: Cancel in its window leaves them to revise.
    // Once it is sent the card leaves, and them with it.
    val send = { store.answer(c, words) }
    if (other) {
        Spacer(Modifier.height(T.s2))
        Field(ui, field, "Your answer, in your words", anchor = "answer-field", onEnter = { if (words.isNotEmpty()) send() })
    }
    if (q.multiSelect || other) {
        Spacer(Modifier.height(T.s2))
        val n = picked.size + if (other && typed.isNotEmpty()) 1 else 0
        TButton(if (n > 1) "Send $n answers" else "Send", send, kind = if (lead) Kind.PRIMARY else Kind.PLAIN, enabled = words.isNotEmpty() && store.t3Live, anchor = "answer:send")
    }
}

/** A call on its way to T3: what it does, a bar running down, and Cancel. No Undo comes after. */
@Composable
/** [prefix] tells an open card's controls from the same card's pinned ones, which a sheet may cover. */
private fun Sending(store: ConceptStore, key: String, p: ConceptStore.Pending, prefix: String = "") {
    var left by remember(p) { mutableStateOf(1f) }
    LaunchedEffect(p) {
        while (true) {
            withFrameMillis { }
            left = (1f - p.since.elapsedNow().inWholeMilliseconds.toFloat() / p.forMs).coerceIn(0f, 1f)
            if (left == 0f) break
        }
    }
    Row(Modifier.fillMaxWidth().labAnchor("${prefix}sending:$key"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText("${p.verb}…", style = T.body.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(T.rPill).background(T.line2)) {
                Box(Modifier.fillMaxWidth(left).height(3.dp).background(T.cyan))
            }
        }
        Spacer(Modifier.width(T.s3))
        TButton("Cancel", { store.cancelSend(key) }, anchor = "${prefix}cancel:$key")
    }
}

/** A task due today, as a row: tick, title, when. A tap opens it to change its date. */
@Composable
fun DueRow(store: ConceptStore, ui: TimelineUi, item: Item.Due, wide: Boolean, selected: Boolean) {
    val t = item.task
    val done = store.done[t.id] == true
    Row(
        Modifier.fillMaxWidth().clip(T.rMd).background(if (selected) T.wash2 else Color.Transparent)
            .clickable { open(store, ui, item.key, wide) }.heightIn(min = T.rowMin).padding(end = 4.dp).labAnchor("item:${item.key}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Check(done, { if (done) store.untick(t.id) else store.tick(t.id) }, anchor = "tick:${item.key}", enabled = store.lorLive)
        Column(Modifier.weight(1f)) {
            TaskTitle(t.title, done, maxLines = 2)
            BasicText(dueWords(t, store), style = T.label.copy(color = if (t.overdue && !done) T.red else T.text3))
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

/** What the agent is working on: quiet rows in the agent's colour. */
@Composable
fun AgentWork(store: ConceptStore) {
    val running = store.running
    val queued = store.home?.agentQueue.orEmpty().filter { q -> q.thread == null && q.queuedAt != null && running.none { it.title == q.title } }
    if (running.isEmpty() && queued.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("The agent is on", trailing = if (queued.isNotEmpty()) "${queued.size} queued" else null)
        running.forEach { t ->
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(T.cyan))
                Spacer(Modifier.width(12.dp))
                BasicText(t.title, Modifier.weight(1f), style = T.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(t.runStartedAt?.let { Instant.parse(it) }?.takeIf { it < store.now }?.let { "for ${spoken(store.now - it)}" }.orEmpty(), style = T.label)
            }
        }
        queued.take(3).forEach { q ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).border(1.5.dp, T.cyan.copy(alpha = 0.6f), CircleShape))
                Spacer(Modifier.width(12.dp))
                BasicText(q.title, Modifier.weight(1f), style = T.body.copy(color = T.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("next run", style = T.label.copy(color = T.text3))
            }
        }
    }
}

/** Nothing wants him: said calmly, with what comes next. When LoR or T3 can't be read, it says it can't tell. */
@Composable
fun Calm(store: ConceptStore, modifier: Modifier = Modifier) {
    if (!store.knows()) {
        Column(modifier.labAnchor("unknown"), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(T.wash2), contentAlignment = Alignment.Center) { Icon(Glyph.Sync, T.text2, size = 24.dp) }
            Spacer(Modifier.height(T.s4))
            BasicText("Nothing to show yet", style = T.title)
            Spacer(Modifier.height(4.dp))
            BasicText(if (store.home == null) "What waits on you shows once LoR answers." else "T3's cards show once it's connected.", style = T.body.copy(color = T.text2))
        }
        return
    }
    Column(modifier.labAnchor("calm"), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(T.wash2), contentAlignment = Alignment.Center) { Icon(Glyph.Check, T.text1, size = 26.dp, weight = 2f) }
        Spacer(Modifier.height(T.s4))
        BasicText("Nothing wants you", style = T.title)
        Spacer(Modifier.height(4.dp))
        val next = store.home?.day?.events?.firstOrNull { it.end > store.now }
        BasicText(next?.let { "Next: ${it.title} at ${LocalClock.time(it.start, store.zone)}" } ?: "No approvals, flags or tasks due.", style = T.body.copy(color = T.text2))
    }
}
