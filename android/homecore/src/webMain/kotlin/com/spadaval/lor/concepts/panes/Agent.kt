package com.spadaval.lor.concepts.panes

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.T3Card
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import com.spadaval.lor.ui.LocalClock

/*
 * The agent page, left of home. Its list leads with what waits on him from T3, as the spec has it
 * (approvals, questions, failed turns), each with its common action on the row; then every thread,
 * the running ones first. A tap on a row opens it: unfolded, in the pane on the right, with its
 * transcript and a reply field; on the cover, in a sheet at half height over the list.
 */
@Composable
fun AgentPage(store: ConceptStore, ui: PanesUi, wide: Boolean) {
    val cards = store.cards
    val threads = store.otherThreads()
    PanelPage(
        store, ui, PanesUi.AGENT, wide, "agent",
        head = {
            PaneHead("Agent", cards.size, t3Line(store), countTone = P.cyan) {
                MoreButton(ui, { Menu.Page(PanesUi.AGENT, it) }, "agent:more")
            }
        },
        overview = { AgentOverview(store, wide) },
        detail = { key, inSheet -> AgentDetail(store, ui, key, inSheet) },
    ) {
        item(key = "notes") { Column(Modifier.padding(horizontal = 6.dp)) { AgentNotes(store, ui) } }
        item(key = "waiting-label") { GroupLabel("Waiting on you", cards.size) }
        if (cards.isEmpty()) item(key = "calm") { Calm(store, t3 = true) }
        items(cards, key = { it.key }) { c -> CardRow(store, ui, c) }
        if (threads.isNotEmpty()) {
            item(key = "threads-label") { GroupLabel("Threads", threads.size) }
            items(threads, key = { "thread:${it.id}" }) { t -> ThreadRow(store, ui, t) }
        }
    }
}

private fun t3Line(store: ConceptStore): String = when (store.t3Status) {
    T3Status.LIVE -> "T3 connected · ${store.threads.size} threads"
    T3Status.RETRYING -> "T3 reconnecting · as of ${ago(store.t3SeenAt, store.now)}"
    T3Status.CONNECTING, T3Status.STOPPED -> "Connecting to T3…"
    T3Status.NOT_PAIRED -> "Not paired with T3"
    T3Status.REJECTED -> "T3 refused this phone"
}

/** What is wrong, where he is already looking: a failed sync, T3 away (its cards as last seen), LoR not answering. */
@Composable
fun AgentNotes(store: ConceptStore, ui: PanesUi) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(P.s2)) {
        if (store.syncFailed) SyncNote(store, ui)
        when (store.t3Status) {
            T3Status.RETRYING -> Note("T3 is reconnecting", "These are its cards as of ${ago(store.t3SeenAt, store.now)}. Answers wait until it's back.", P.red, Glyph.Sync, anchor = "note:t3")
            T3Status.CONNECTING, T3Status.STOPPED -> Note("Connecting to T3…", null, P.text2, Glyph.Sync, anchor = "note:t3")
            T3Status.REJECTED -> Note("T3 refused this phone", store.t3Error, P.red, Glyph.Warning, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3")
            T3Status.NOT_PAIRED -> Note("Not paired with T3", "Pair to see approvals and questions here", P.text2, Glyph.Agent, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3")
            T3Status.LIVE -> {}
        }
    }
}

@Composable
fun SyncNote(store: ConceptStore, ui: PanesUi) =
    Note("Phone sync failed", "${store.sync.pending} readings waiting · ${store.sync.errors.firstOrNull().orEmpty()}", P.red, Glyph.Warning, action = "Fix", onAction = { ui.sheet = Sheet.Settings("sync") }, anchor = "note:sync")

/** Nothing waits: said calmly. When T3 (or LoR) can't be read, it says it can't tell instead. */
@Composable
fun Calm(store: ConceptStore, t3: Boolean) {
    val knows = if (t3) store.knowsT3() else store.knowsLor()
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp).clip(P.rMd).background(P.raise).padding(horizontal = 14.dp, vertical = 14.dp).labAnchor(if (knows) "calm" else "unknown"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(P.raise2), contentAlignment = Alignment.Center) {
            Icon(if (knows) Glyph.Check else Glyph.Sync, if (knows) P.text1 else P.text2, size = 16.dp, weight = 2f)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(if (knows) "Nothing waits on you" else "Can't tell yet", style = P.rowTitle)
            BasicText(
                when {
                    knows && t3 -> "No approvals, questions or failed turns"
                    knows -> "No flags, and nothing due today"
                    t3 -> "T3's cards show once it's connected"
                    else -> "What waits shows once LoR answers"
                },
                style = P.meta,
            )
        }
    }
}

/**
 * A card that waits on him: what it is, its title, and its action on the row (Approve and Deny,
 * Answer, Dismiss), so the common job opens nothing. While an answer is on its way the row says so,
 * with Cancel; there is no Undo after, as T3 can't take it back.
 */
@Composable
private fun CardRow(store: ConceptStore, ui: PanesUi, c: T3Card) {
    val open = ui.openAgent == c.key
    val failed = c.kind == T3Card.Kind.FAILED
    val tone = if (failed) P.red else P.cyan
    val q = store.questions(c).firstOrNull()
    Column(
        Modifier.paneRow(open, c.key) { ui.openAgent = c.key }.edge(tone).padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp).animateContentSize(tween(200)),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 4.dp)) {
                Kicker("${c.kind.label} · ${c.thread.projectTitle}", tone)
                Spacer(Modifier.height(4.dp))
                BasicText(q?.question ?: c.thread.title, style = P.rowTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val sub = when (c.kind) {
                    T3Card.Kind.APPROVAL -> listOfNotNull(c.thread.title.takeIf { it != (q?.question ?: c.thread.title) }, "asked ${ago(parseOrNull(c.thread.updatedAt), store.now)}", "the turn waits").joinToString(" · ")
                    T3Card.Kind.QUESTION -> listOfNotNull(q?.let { "${it.options.size} options" + if (it.multiSelect) ", pick several" else "" }, ago(parseOrNull(c.thread.updatedAt), store.now)).joinToString(" · ")
                    T3Card.Kind.FAILED -> c.thread.lastError ?: "The turn failed"
                }
                BasicText(sub, style = P.meta, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            MoreButton(ui, { Menu.Row(c.key, it) }, "more:${c.key}")
        }
        Spacer(Modifier.height(10.dp))
        val pending = store.pending[c.key]
        if (pending != null) Sending(store, c.key, pending, "")
        else Row(horizontalArrangement = Arrangement.spacedBy(P.s2), verticalAlignment = Alignment.CenterVertically) {
            when (c.kind) {
                T3Card.Kind.APPROVAL -> {
                    PButton("Approve", { store.approve(c) }, kind = Kind.PRIMARY, glyph = Glyph.Check, compact = true, anchor = "approve:${c.key}", enabled = store.t3Live)
                    PButton("Deny", { store.deny(c) }, compact = true, anchor = "deny:${c.key}", enabled = store.t3Live)
                }
                T3Card.Kind.QUESTION -> PButton(if (q?.multiSelect == true) "Answer…" else "Answer", { ui.openAgent = c.key; ui.full[PanesUi.AGENT] = true }, kind = Kind.AGENT, glyph = Glyph.Chevron, compact = true, anchor = "answer:${c.key}", enabled = store.t3Live)
                T3Card.Kind.FAILED -> PButton("Dismiss", { store.dismiss(c) }, compact = true, anchor = "dismiss:${c.key}")
            }
        }
    }
}

/** A thread: whether it runs, its title and project, and when it last moved. */
@Composable
private fun ThreadRow(store: ConceptStore, ui: PanesUi, t: T3Thread) {
    val key = "thread:${t.id}"
    val running = t.state == ThreadState.RUNNING
    Row(
        Modifier.paneRow(ui.openAgent == key, key) { ui.openAgent = key }.heightIn(min = P.row).padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (running) P.cyan else Color.Transparent).border(1.5.dp, if (running) P.cyan else P.line3, CircleShape))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(t.title, style = P.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val last = store.messages(t.id).lastOrNull()
            BasicText(
                listOfNotNull(t.projectTitle, if (running) t.runStartedAt?.let(::parseOrNull)?.takeIf { it < store.now }?.let { "working for ${spoken(store.now - it)}" } ?: "working" else ago(parseOrNull(t.updatedAt), store.now), last?.text?.take(60)?.takeIf { !running }).joinToString(" · "),
                style = P.meta.copy(color = if (running) P.cyan else P.text3), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Glyph.Chevron, P.text3, size = 16.dp)
    }
}

/** A call on its way to T3: what it does, a bar running down, and Cancel. No Undo comes after. */
@Composable
fun Sending(store: ConceptStore, key: String, p: ConceptStore.Pending, prefix: String) {
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
            BasicText("${p.verb}…", style = P.small.copy(color = P.text1, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(P.rPill).background(P.line2)) {
                Box(Modifier.fillMaxWidth(left).height(3.dp).background(P.cyan))
            }
        }
        Spacer(Modifier.width(P.s3))
        PButton("Cancel", { store.cancelSend(key) }, compact = true, anchor = "${prefix}cancel:$key")
    }
}

/** The detail side with nothing open: what the agent is on, quietly. */
@Composable
private fun AgentOverview(store: ConceptStore, wide: Boolean) {
    val running = store.running
    val queued = store.home?.agentQueue.orEmpty().filter { q -> q.thread == null && q.queuedAt != null && running.none { it.title == q.title } }
    Column(Modifier.fillMaxWidth().padding(if (wide) 24.dp else 12.dp).labAnchor("agent:overview")) {
        if (wide) { BasicText("THE AGENT IS ON", style = P.paneTitle.copy(color = P.text2, fontSize = P.meta.fontSize)); Spacer(Modifier.height(P.s3)) }
        else GroupLabel("The agent is on", running.size, Modifier.padding(start = 0.dp))
        if (running.isEmpty() && queued.isEmpty()) BasicText("Nothing running. A quick send starts a thread.", style = P.small)
        running.forEach { t ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(P.cyan))
                Spacer(Modifier.width(12.dp))
                BasicText(t.title, Modifier.weight(1f), style = P.small.copy(color = P.text1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(t.projectTitle, style = P.meta)
            }
        }
        queued.take(4).forEach { q ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).border(1.5.dp, P.cyan.copy(alpha = 0.6f), CircleShape))
                Spacer(Modifier.width(12.dp))
                BasicText(q.title, Modifier.weight(1f), style = P.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("next run", style = P.meta.copy(color = P.text3))
            }
        }
        if (wide) { Spacer(Modifier.height(P.s5)); BasicText("Open a card or a thread on the left to read it here.", style = P.meta.copy(color = P.text3)) }
    }
}

/**
 * An open card or thread: what it is, its title and its main action under it (Approve and Deny, or
 * the question's answers), then the transcript, newest at the foot by the reply field. The header
 * scrolls on its own when a question's answers are long, so the reply field never leaves the screen.
 */
@Composable
fun AgentDetail(store: ConceptStore, ui: PanesUi, key: String, inSheet: Boolean) {
    val row = store.agentRow(key)
    val thread = when (row) { is AgentRow.Card -> row.card.thread; is AgentRow.Thread -> row.thread; null -> null }
    val card = (row as? AgentRow.Card)?.card
    if (thread == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { BasicText("This one has gone from T3.", style = P.body.copy(color = P.text2)) }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().labAnchor("detail").labAnchor("detail:$key")) {
        // A question's answers are the point of opening it, so they get most of the pane.
        val maxHead = maxHeight * if (card?.kind == T3Card.Kind.QUESTION) 0.78f else 0.58f
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxHead).verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = if (inSheet) 22.dp else 10.dp, top = if (inSheet) 0.dp else 18.dp, bottom = P.s3),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tone = if (card?.kind == T3Card.Kind.FAILED) P.red else P.cyan
                    Kicker(listOfNotNull(card?.kind?.label ?: "Thread", thread.projectTitle).joinToString(" · "), tone, Modifier.weight(1f))
                    if (!inSheet) IconButton(Glyph.Close, "Close", { ui.openAgent = null }, anchor = "pane:close", size = 40.dp)
                }
                Spacer(Modifier.height(6.dp))
                val q = card?.let { store.questions(it).firstOrNull() }
                BasicText(q?.question ?: thread.title, style = if (inSheet) P.head else P.title)
                Spacer(Modifier.height(4.dp))
                BasicText(
                    listOfNotNull(if (q != null) thread.title else null, if (thread.state == ThreadState.RUNNING) "working now" else "updated ${ago(parseOrNull(thread.updatedAt), store.now)}").joinToString(" · "),
                    style = P.meta,
                )
                if (card != null) {
                    Spacer(Modifier.height(P.s4))
                    val pending = store.pending[card.key]
                    if (pending != null) Sending(store, card.key, pending, "detail:")
                    else when (card.kind) {
                        T3Card.Kind.APPROVAL -> Row(horizontalArrangement = Arrangement.spacedBy(P.s2)) {
                            PButton("Approve", { store.approve(card) }, Modifier.weight(1f), kind = Kind.PRIMARY, glyph = Glyph.Check, anchor = "detail:approve", enabled = store.t3Live)
                            PButton("Deny", { store.deny(card) }, Modifier.weight(1f), anchor = "detail:deny", enabled = store.t3Live)
                        }
                        T3Card.Kind.QUESTION -> Answers(store, ui, card)
                        T3Card.Kind.FAILED -> Note("The turn failed", thread.lastError, P.red, Glyph.Warning, action = "Dismiss", onAction = { store.dismiss(card); ui.openAgent = null })
                    }
                }
            }
            Hairline()
            Transcript(store, thread, Modifier.weight(1f))
            Reply(store, ui, thread)
        }
    }
}

/**
 * A question's answers: its options with their descriptions. One answer goes on a tap (after a
 * moment to cancel); with several, each is ticked and then sent; Other takes his own words.
 */
@Composable
private fun Answers(store: ConceptStore, ui: PanesUi, c: T3Card) {
    val q = store.questions(c).firstOrNull()
    if (q == null) { BasicText(if (store.t3Live) "Reply below to answer." else "T3 is away; the question shows once it's back.", style = P.small); return }
    val picked = ui.picked[c.key].orEmpty()
    val field = ui.field("other:${c.key}")
    var other by remember(c.key) { mutableStateOf(field.text.isNotEmpty()) }
    if (q.multiSelect) { BasicText("Pick any that apply", style = P.meta); Spacer(Modifier.height(P.s2)) }
    Column(verticalArrangement = Arrangement.spacedBy(P.s2)) {
        q.options.forEach { o ->
            Choice(
                o.label, selected = o.label in picked, detail = o.description.ifEmpty { null }, anchor = "option:${o.label}", enabled = store.t3Live, multi = q.multiSelect,
                onClick = {
                    if (q.multiSelect) ui.picked[c.key] = if (o.label in picked) picked - o.label else picked + o.label
                    else store.answer(c, o.label)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (q.allowCustomAnswer) Choice("Something else…", selected = other, onClick = { other = !other }, anchor = "option:other", enabled = store.t3Live, modifier = Modifier.fillMaxWidth(), multi = q.multiSelect)
    }
    val typed = field.text.toString().trim()
    val words = (picked.toList() + listOfNotNull(typed.ifEmpty { null }.takeIf { other })).joinToString(", ")
    val send = { store.answer(c, words) }
    if (other) { Spacer(Modifier.height(P.s2)); Field(ui, field, "Your answer, in your words", anchor = "answer-field", onEnter = { if (words.isNotEmpty()) send() }) }
    if (q.multiSelect || other) {
        Spacer(Modifier.height(P.s3))
        val n = picked.size + if (other && typed.isNotEmpty()) 1 else 0
        PButton(if (n > 1) "Send $n answers" else "Send answer", send, Modifier.fillMaxWidth(), kind = Kind.PRIMARY, glyph = Glyph.Send, enabled = words.isNotEmpty() && store.t3Live, anchor = "answer:send")
    }
}

/** The transcript, newest at the foot; its scroll is the store's, so a fold keeps it. */
@Composable
private fun Transcript(store: ConceptStore, thread: T3Thread, modifier: Modifier) {
    val messages = store.messages(thread.id)
    val state = store.list("thread:${thread.id}")
    LazyColumn(
        modifier.fillMaxWidth().labAnchor("thread").labAnchor("messages:${messages.size}").labAnchor("thread-at:${state.firstVisibleItemIndex}"),
        state = state, reverseLayout = true, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (messages.isEmpty()) item(key = "none") { BasicText(if (store.t3Live) "No messages yet." else "T3 is away; the thread shows once it's back.", style = P.meta) }
        items(messages.withIndex().reversed(), key = { "m${it.index}" }) { (_, m) ->
            Bubble(m.fromUser, m.text, LocalClock.parse(m.at)?.let { LocalClock.time(it, store.zone) }.orEmpty(), m.streaming)
        }
    }
}

/** A message: his on the right, raised; the agent's on the left, flat, with a cyan mark. */
@Composable
private fun Bubble(mine: Boolean, text: String, at: String, streaming: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth(if (mine) 0.84f else 0.94f)
                .clip(P.rMd).background(if (mine) P.raise2 else P.well.copy(alpha = 0.14f)).border(1.dp, if (mine) P.line2 else P.line, P.rMd)
                .then(if (!mine) Modifier.edge(P.cyan.copy(alpha = 0.7f), 2.dp) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            BasicText(text, style = P.body)
            Spacer(Modifier.height(4.dp))
            BasicText(if (streaming) "writing…" else "${if (mine) "You" else "Agent"} · $at", style = P.meta.copy(color = if (streaming) P.cyan else P.text3))
        }
    }
}

/** The reply field, at the foot of the open thread. What is typed is kept by thread, so a fold keeps it. */
@Composable
private fun Reply(store: ConceptStore, ui: PanesUi, thread: T3Thread) {
    val field = ui.field("reply:${thread.id}")
    val can = field.text.isNotBlank() && store.t3Live
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Box(Modifier.weight(1f).labAnchor("draft:${field.text.length}")) {
            Field(ui, field, if (store.t3Live) "Reply to the agent" else "T3 is away; replies wait", anchor = "reply-field", lines = 4, pill = false)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(Glyph.Send, "Send", {
            if (can) { store.drafts[thread.id] = field.text.toString(); store.reply(thread.id); field.clearText() }
        }, anchor = "reply:send", tint = if (can) P.amberInk else P.text3, size = 48.dp, fill = if (can) P.amber else P.raise, enabled = store.t3Live)
    }
}
