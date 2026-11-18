package com.spadaval.lor.concepts.context

import androidx.compose.foundation.*
import androidx.compose.ui.draw.*
import com.spadaval.lor.concepts.kit.MenuItem
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lab.LocalWallpaperKind
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

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
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import com.spadaval.lor.ui.LocalClock


/*
 * T3 on Now: a card that waits on him with its action on it, and an open card or thread with its
 * transcript and a reply field (in the right half unfolded, in a sheet on the cover).
 */
/**
 * A card that waits on him: what it is, its title, and its action on the row (Approve and Deny,
 * Answer, Dismiss), so the common job opens nothing. While an answer is on its way the row says so,
 * with Cancel; there is no Undo after, as T3 can't take it back.
 */
@Composable
fun CardRow(store: ConceptStore, ui: CtxUi, c: T3Card) {
    val open = ui.open == c.key
    val failed = c.kind == T3Card.Kind.FAILED
    val tone = if (failed) C.red else C.cyan
    val q = store.questions(c).firstOrNull()
    // The card names the context its thread belongs to, when it has one; else T3's project.
    val home = ui.model.ctxOfThread(c.thread)?.title ?: c.thread.projectTitle
    Column(
        Modifier.fillMaxWidth().clip(C.rLg).background(if (open) C.region3 else C.ground2).border(1.dp, if (open) tone.copy(alpha = 0.5f) else C.line2, C.rLg)
            .clickable { ui.open = c.key }.labAnchor("row:${c.key}")
            .edge(tone).padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp).animateContentSize(tween(200)),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 4.dp)) {
                Kicker("${c.kind.label} · $home", tone)
                Spacer(Modifier.height(4.dp))
                BasicText(q?.question ?: c.thread.title, style = C.rowTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val sub = when (c.kind) {
                    T3Card.Kind.APPROVAL -> listOfNotNull(c.thread.title.takeIf { it != (q?.question ?: c.thread.title) }, "asked ${ago(parseOrNull(c.thread.updatedAt), store.now)}", "the turn waits").joinToString(" · ")
                    T3Card.Kind.QUESTION -> listOfNotNull(q?.let { "${it.options.size} options" + if (it.multiSelect) ", pick several" else "" }, ago(parseOrNull(c.thread.updatedAt), store.now)).joinToString(" · ")
                    T3Card.Kind.FAILED -> c.thread.lastError ?: "The turn failed"
                }
                BasicText(sub, style = C.meta, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            MoreButton(ui, { Menu.Row(c.key, it) }, "more:${c.key}")
        }
        Spacer(Modifier.height(10.dp))
        val pending = store.pending[c.key]
        if (pending != null) Sending(store, c.key, pending, "")
        else Row(horizontalArrangement = Arrangement.spacedBy(C.s2), verticalAlignment = Alignment.CenterVertically) {
            when (c.kind) {
                T3Card.Kind.APPROVAL -> {
                    CButton("Approve", { store.approve(c) }, kind = Tone.PRIMARY, glyph = Glyph.Check, compact = true, anchor = "approve:${c.key}", enabled = store.t3Live)
                    CButton("Deny", { store.deny(c) }, compact = true, anchor = "deny:${c.key}", enabled = store.t3Live)
                }
                T3Card.Kind.QUESTION -> CButton(if (q?.multiSelect == true) "Answer…" else "Answer", { ui.open = c.key }, kind = Tone.AGENT, glyph = Glyph.Chevron, compact = true, anchor = "answer:${c.key}", enabled = store.t3Live)
                T3Card.Kind.FAILED -> CButton("Dismiss", { store.dismiss(c) }, compact = true, anchor = "dismiss:${c.key}")
            }
        }
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
            BasicText("${p.verb}…", style = C.small.copy(color = C.text1, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(C.rPill).background(C.line2)) {
                Box(Modifier.fillMaxWidth(left).height(3.dp).background(C.cyan))
            }
        }
        Spacer(Modifier.width(C.s3))
        CButton("Cancel", { store.cancelSend(key) }, compact = true, anchor = "${prefix}cancel:$key")
    }
}

/**
 * An open card or thread: what it is, its title and its main action under it (Approve and Deny, or
 * the question's answers), then the transcript, newest at the foot by the reply field. The header
 * scrolls on its own when a question's answers are long, so the reply field never leaves the screen.
 */
@Composable
fun ThreadDetail(store: ConceptStore, ui: CtxUi, key: String, inSheet: Boolean) {
    val (card, thread) = store.cardOrThread(key)
    if (thread == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { BasicText("This one has gone from T3.", style = C.body.copy(color = C.text2)) }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().labAnchor("detail").labAnchor("detail:$key")) {
        // A question's answers are the point of opening it, so they get most of the pane.
        val maxHead = maxHeight * if (card?.kind == T3Card.Kind.QUESTION) 0.78f else 0.58f
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = maxHead).verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = if (inSheet) 22.dp else 10.dp, top = if (inSheet) 0.dp else 18.dp, bottom = C.s3),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tone = if (card?.kind == T3Card.Kind.FAILED) C.red else C.cyan
                    Kicker(listOfNotNull(card?.kind?.label ?: "Thread", ui.model.ctxOfThread(thread)?.title ?: thread.projectTitle).joinToString(" · "), tone, Modifier.weight(1f))
                    if (!inSheet) IconButton(Glyph.Close, "Close", { ui.open = null }, anchor = "pane:close", size = 40.dp)
                }
                Spacer(Modifier.height(6.dp))
                val q = card?.let { store.questions(it).firstOrNull() }
                BasicText(q?.question ?: thread.title, style = C.head)
                Spacer(Modifier.height(4.dp))
                BasicText(
                    listOfNotNull(if (q != null) thread.title else null, if (thread.state == ThreadState.RUNNING) "working now" else "updated ${ago(parseOrNull(thread.updatedAt), store.now)}").joinToString(" · "),
                    style = C.meta,
                )
                if (card != null) {
                    Spacer(Modifier.height(C.s4))
                    val pending = store.pending[card.key]
                    if (pending != null) Sending(store, card.key, pending, "detail:")
                    else when (card.kind) {
                        T3Card.Kind.APPROVAL -> Row(horizontalArrangement = Arrangement.spacedBy(C.s2)) {
                            CButton("Approve", { store.approve(card) }, Modifier.weight(1f), kind = Tone.PRIMARY, glyph = Glyph.Check, anchor = "detail:approve", enabled = store.t3Live)
                            CButton("Deny", { store.deny(card) }, Modifier.weight(1f), anchor = "detail:deny", enabled = store.t3Live)
                        }
                        T3Card.Kind.QUESTION -> Answers(store, ui, card)
                        T3Card.Kind.FAILED -> Note("The turn failed", thread.lastError, C.red, Glyph.Warning, action = "Dismiss", onAction = { store.dismiss(card); ui.open = null })
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
private fun Answers(store: ConceptStore, ui: CtxUi, c: T3Card) {
    val q = store.questions(c).firstOrNull()
    if (q == null) { BasicText(if (store.t3Live) "Reply below to answer." else "T3 is away; the question shows once it's back.", style = C.small); return }
    val picked = ui.picked[c.key].orEmpty()
    val field = ui.field("other:${c.key}")
    var other by remember(c.key) { mutableStateOf(field.text.isNotEmpty()) }
    if (q.multiSelect) { BasicText("Pick any that apply", style = C.meta); Spacer(Modifier.height(C.s2)) }
    Column(verticalArrangement = Arrangement.spacedBy(C.s2)) {
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
    if (other) { Spacer(Modifier.height(C.s2)); Field(ui, field, "Your answer, in your words", anchor = "answer-field", onEnter = { if (words.isNotEmpty()) send() }) }
    if (q.multiSelect || other) {
        Spacer(Modifier.height(C.s3))
        val n = picked.size + if (other && typed.isNotEmpty()) 1 else 0
        CButton(if (n > 1) "Send $n answers" else "Send answer", send, Modifier.fillMaxWidth(), kind = Tone.PRIMARY, glyph = Glyph.Send, enabled = words.isNotEmpty() && store.t3Live, anchor = "answer:send")
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
        if (messages.isEmpty()) item(key = "none") { BasicText(if (store.t3Live) "No messages yet." else "T3 is away; the thread shows once it's back.", style = C.meta) }
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
                .clip(C.rMd).background(if (mine) C.raise2 else C.well.copy(alpha = 0.14f)).border(1.dp, if (mine) C.line2 else C.line, C.rMd)
                .then(if (!mine) Modifier.edge(C.cyan.copy(alpha = 0.7f), 2.dp) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            BasicText(text, style = C.body)
            Spacer(Modifier.height(4.dp))
            BasicText(if (streaming) "writing…" else "${if (mine) "You" else "Agent"} · $at", style = C.meta.copy(color = if (streaming) C.cyan else C.text3))
        }
    }
}

/** The reply field, at the foot of the open thread. What is typed is kept by thread, so a fold keeps it. */
@Composable
private fun Reply(store: ConceptStore, ui: CtxUi, thread: T3Thread) {
    val field = ui.field("reply:${thread.id}")
    val can = field.text.isNotBlank() && store.t3Live
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Box(Modifier.weight(1f).labAnchor("draft:${field.text.length}")) {
            Field(ui, field, if (store.t3Live) "Reply to the agent" else "T3 is away; replies wait", anchor = "reply-field", lines = 4, pill = false)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(Glyph.Send, "Send", {
            if (can) { store.drafts[thread.id] = field.text.toString(); store.reply(thread.id); field.clearText() }
        }, anchor = "reply:send", tint = if (can) C.amberInk else C.text3, size = 48.dp, fill = if (can) C.amber else C.raise, enabled = store.t3Live)
    }
}

