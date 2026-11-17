package com.spadaval.lor.concepts.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock

/*
 * The day's own sheets: a block to relabel, a gap to plan a task into, every waiting card, and the
 * tasks due with no time.
 */

/**
 * A block, lifted: when it was, where it came from, and what it counts toward. A tap on a label
 * counts it (with Undo); the agent's guess, if there is one, is first and marked. A gap with
 * nothing recorded opens here too, to be counted as something.
 */
@Composable
fun BlockSheet(store: ConceptStore, ui: TimelineUi, id: String) {
    val gap = parseGap(id)
    val b = ui.blocks.firstOrNull { it.id == id }
    val (start, end) = gap ?: (b?.start to b?.end)
    if (start == null || end == null) return
    val close = { ui.selected = null; ui.sheet = null }
    LazyColumn(Modifier.fillMaxSize().labAnchor("block-sheet"), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "title") {
            SheetTitle(
                b?.title ?: "Nothing recorded",
                "${LocalClock.time(start, store.zone)}–${LocalClock.time(end, store.zone)} · ${spoken(end - start)} · ${b?.from ?: "a gap"}",
            )
        }
        item(key = "state") {
            Column(Modifier.padding(horizontal = 20.dp)) {
                val (words, tone) = when (b?.tone) {
                    DayBlock.Tone.TRACKED -> "Counted as ${b.label}" to T.amber
                    DayBlock.Tone.PROPOSED -> "The agent thinks this was ${b.label}" to T.amber
                    else -> "Not counted toward anything yet" to T.text2
                }
                BasicText(words, style = T.body.copy(color = tone, fontWeight = FontWeight.Medium))
                if (b?.tone == DayBlock.Tone.PROPOSED) {
                    Spacer(Modifier.height(T.s3))
                    TButton("Yes, ${b.label}", { ui.confirm(b.id); close() }, kind = Kind.PRIMARY, glyph = Glyph.Check, anchor = "sheet:confirm")
                }
                Spacer(Modifier.height(T.s2))
            }
        }
        item(key = "labels") {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionLabel(if (b?.tone == DayBlock.Tone.TRACKED) "Count it as something else" else "Count it as")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(T.s2), verticalArrangement = Arrangement.spacedBy(T.s2)) {
                    ui.labelChoices().forEach { l ->
                        Chip(l, selected = b?.label == l && b.tone == DayBlock.Tone.TRACKED, onClick = { ui.relabel(id, l); close() }, anchor = "label:$l", enabled = store.lorLive)
                    }
                }
                if (!store.lorLive) {
                    Spacer(Modifier.height(T.s3))
                    BasicText("LoR isn't answering, so a label can't be saved yet.", style = T.label.copy(color = T.red))
                }
            }
        }
    }
}

/** A free gap, and the untimed tasks that could go in it: one tap plans one there (with Undo). */
@Composable
fun PlanSheet(store: ConceptStore, ui: TimelineUi, s: Sheet.Plan) {
    val tasks = store.untimed(ui.plans).filter { store.done[it.id] != true }
    LazyColumn(Modifier.fillMaxSize().labAnchor("plan-sheet"), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp)) {
        item(key = "title") {
            SheetTitle("Plan a task", "${LocalClock.time(s.start, store.zone)}–${LocalClock.time(s.end, store.zone)} · ${spoken(s.end - s.start)} free")
        }
        if (tasks.isEmpty()) item(key = "none") { BasicText("Nothing due without a time.", Modifier.padding(horizontal = 8.dp), style = T.label) }
        items(tasks, key = { it.id }) { t ->
            Row(
                Modifier.fillMaxWidth().clip(T.rMd).clickable { ui.plan(t.id, t.title, s.start, s.end); ui.sheet = null }
                    .heightIn(min = T.rowMin).padding(horizontal = 8.dp).labAnchor("plan-task:${t.title}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    BasicText(t.title, style = T.body.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    BasicText(dueWords(t, store) + (t.parent?.let { " · $it" } ?: ""), style = T.label.copy(color = if (t.overdue) T.red else T.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BasicText("at ${LocalClock.time(s.start, store.zone)}", style = T.time.copy(color = T.cyan))
            }
        }
    }
}

/** Every card that waits, in order: what the cover's pinned two lead into. */
@Composable
fun WaitingSheet(store: ConceptStore, ui: TimelineUi) {
    val items = store.waitingItems()
    LaunchedClose(items.isEmpty()) { ui.sheet = null }
    LazyColumn(Modifier.fillMaxSize().labAnchor("waiting-sheet"), state = store.list("waiting"), contentPadding = PaddingValues(start = T.gutter, end = T.gutter, bottom = 32.dp)) {
        item(key = "title") { Box(Modifier.padding(start = 4.dp)) { SheetTitle("Waiting on you", "${items.size} · T3's first, then LoR's flags") } }
        items(items, key = { it.key }) { item ->
            Box(Modifier.animateItem().padding(bottom = T.s2)) { ItemCard(store, ui, item, wide = false, selected = false) }
        }
    }
}

/** The tasks due with no time: tick one, or plan it into the next free gap. */
@Composable
fun UntimedSheet(store: ConceptStore, ui: TimelineUi) {
    val tasks = store.untimed(ui.plans)
    val gap = ui.route(store.clock()).firstOrNull { it.kind == SpanKind.FREE }
    LazyColumn(Modifier.fillMaxSize().labAnchor("untimed-sheet"), contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 32.dp)) {
        item(key = "title") {
            SheetTitle("Due, no time yet", gap?.let { "Next free: ${LocalClock.time(it.start, store.zone)}, ${spoken(it.end - it.start)}" } ?: "No free time left today")
        }
        if (tasks.isEmpty()) item(key = "none") { BasicText("Nothing due without a time.", Modifier.padding(horizontal = 20.dp), style = T.label) }
        items(tasks, key = { it.id }) { t ->
            val done = store.done[t.id] == true
            Row(Modifier.fillMaxWidth().heightIn(min = T.rowMin).padding(end = 8.dp).labAnchor("untimed:${t.title}"), verticalAlignment = Alignment.CenterVertically) {
                Check(done, { if (done) store.untick(t.id) else store.tick(t.id) }, anchor = "tick:task:${t.id}", enabled = store.lorLive)
                Column(Modifier.weight(1f).clickable { open(store, ui, "task:${t.id}", ui.wide) }) {
                    TaskTitle(t.title, done, maxLines = 2, style = T.body.copy(fontWeight = FontWeight.Medium))
                    BasicText(dueWords(t, store) + (t.parent?.let { " · $it" } ?: ""), style = T.label.copy(color = if (t.overdue && !done) T.red else T.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (gap != null && !done) TButton("Plan", { ui.plan(t.id, t.title, gap.start, gap.end) }, kind = Kind.QUIET, height = 36.dp, anchor = "plan-next:${t.title}", enabled = store.lorLive)
            }
        }
    }
}

@Composable
private fun LaunchedClose(empty: Boolean, close: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(empty) { if (empty) close() }
}
