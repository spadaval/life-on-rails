package com.spadaval.lor.concepts.timeline

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.BlurredWallpaper
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.KeyboardHeight
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock
import kotlinx.coroutines.launch

/*
 * The day page, left of home: it merges the agent page and the LoR panel. What waits on him is
 * pinned at the top, whatever time it arrived, so an approval is never mid-scroll: T3's cards
 * first, then LoR's flags, two at a time on the cover and the rest a tap away. Under them a line
 * for the tasks due with no time and the blocks to name, then the route, which scrolls on its own
 * with now a third of the way down.
 * Unfolded, the route is the left pane, and the right holds the waiting cards and the untimed
 * tasks, or the card or thread he opened.
 */
@Composable
fun DayPage(store: ConceptStore, ui: TimelineUi, wide: Boolean) {
    Box(Modifier.fillMaxSize().labAnchor("day")) {
        BlurredWallpaper({ 1f })
        Box(Modifier.fillMaxSize().background(T.veil))
        if (!wide) Column(Modifier.fillMaxSize()) {
            Pinned(store, ui, wide = false)
            Route(store, ui, Modifier.weight(1f).fillMaxWidth())
        } else Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                DayHead(store, ui, Modifier.padding(top = 36.dp))
                Column(Modifier.padding(horizontal = T.gutter)) { Notes(store, ui) }
                Route(store, ui, Modifier.weight(1f).fillMaxWidth())
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(T.line))
            // The pane makes room for the keyboard, as the phone resizes a window for it.
            Box(Modifier.width(400.dp).fillMaxHeight().background(T.surface1.copy(alpha = 0.55f)).padding(bottom = if (ui.keyboard) KeyboardHeight else 0.dp)) {
                val key = store.open?.takeIf { k -> store.item(k) != null || k.startsWith("thread:") }
                if (key != null) Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(top = 36.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TButton("Waiting", { store.open = null; ui.sheet = null }, kind = Kind.QUIET, glyph = Glyph.Back, height = 40.dp, anchor = "pane:back")
                    }
                    Box(Modifier.weight(1f)) { Detail(store, ui, key, pane = true, top = 0.dp) }
                } else RightPane(store, ui)
            }
        }
    }
}

/** The date, what is left of the day and what is next, and the ⋯ for the uncommon things. */
@Composable
fun DayHead(store: ConceptStore, ui: TimelineUi, modifier: Modifier = Modifier) {
    val now = liveNow(store)
    val day = store.home?.day
    var at by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()
    Row(modifier.fillMaxWidth().padding(start = T.gutter, end = 6.dp, bottom = T.s2), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(LocalClock.day(now, store.zone), style = T.title)
            val next = day?.events?.firstOrNull { it.start > now }
            val left = day?.wakeEnd?.let { (it - now).takeIf { d -> d.isPositive() } }
            val words = listOfNotNull(
                left?.let { "${it.inWholeHours} h ${it.inWholeMinutes % 60} min left" },
                next?.let { "next ${it.title} at ${LocalClock.time(it.start, store.zone)}" },
            ).joinToString(" · ").ifEmpty { if (store.home == null) "LoR hasn't sent the day" else "" }
            BasicText(words, style = T.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(Glyph.Search, "Search", { ui.focusSearch = true; ui.later(scope) { shell.goTo(TimelineUi.HOME); shell.openDrawer() } }, anchor = "day:search")
        IconButton(Glyph.More, "More", { ui.menu = Menu.Day(at) }, anchor = "day:more", modifier = Modifier.onGloballyPositioned { at = it.boundsInRoot() })
    }
}

/**
 * The top of the cover's day page, lifted over the route: the head, what is wrong, and what waits,
 * at most two cards; the rest are one tap away. Then one line for the untimed tasks and the blocks to name.
 */
@Composable
private fun Pinned(store: ConceptStore, ui: TimelineUi, wide: Boolean) {
    val items = store.waitingItems()
    val shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
    Column(
        Modifier.fillMaxWidth().shadow(18.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.7f), spotColor = Color.Black.copy(alpha = 0.7f))
            .clip(shape).background(T.ground).border(1.dp, T.line, shape).padding(top = 36.dp, bottom = T.s3)
            .animateContentSize(tween(200)).labAnchor("pinned"),
    ) {
        DayHead(store, ui)
        Column(Modifier.padding(horizontal = T.gutter)) {
            Notes(store, ui)
            WaitingHead(store, items.size)
            if (items.isEmpty()) CalmLine(store)
            else Column(verticalArrangement = Arrangement.spacedBy(T.s2)) {
                items.take(2).forEach { item -> Box(Modifier.labAnchor("pinned:${item.key}")) { ItemCard(store, ui, item, wide, selected = false, compact = true) } }
                if (items.size > 2) MoreWaiting(items.size - 2) { ui.sheet = Sheet.Waiting }
            }
            if (store.home != null) {
                Spacer(Modifier.height(T.s3))
                Shortcuts(store, ui)
            }
        }
    }
}

@Composable
private fun WaitingHead(store: ConceptStore, n: Int) {
    Row(Modifier.fillMaxWidth().padding(top = T.s2, bottom = T.s2), verticalAlignment = Alignment.CenterVertically) {
        BasicText("WAITING ON YOU", style = T.caps)
        if (n > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(CircleShape).background(T.wash2).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                BasicText("$n", Modifier.labAnchor("waiting:$n"), style = T.time.copy(color = T.text1))
            }
        }
        Spacer(Modifier.weight(1f))
        if (!store.t3Live && store.t3SeenAt != null) BasicText("T3 as of ${ago(store.t3SeenAt, store.now)}", style = T.label.copy(color = T.text3))
    }
}

/** Nothing waits: one calm line, so the route keeps the screen. When LoR or T3 can't be read, it says it can't tell. */
@Composable
private fun CalmLine(store: ConceptStore) {
    val knows = store.knows()
    Row(Modifier.fillMaxWidth().clip(T.rLg).background(T.wash).padding(horizontal = 14.dp, vertical = 12.dp).labAnchor(if (knows) "calm" else "unknown"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(T.wash2), contentAlignment = Alignment.Center) {
            Icon(if (knows) Glyph.Check else Glyph.Sync, if (knows) T.text1 else T.text2, size = 16.dp, weight = 2f)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(if (knows) "Nothing waits on you" else "Can't tell yet", style = T.body.copy(fontWeight = FontWeight.Medium))
            BasicText(
                if (knows) "No approvals, questions or flags" else if (store.home == null) "What waits shows once LoR answers" else "T3's cards show once it's connected",
                style = T.label,
            )
        }
    }
}

/** The cards past the first two: a line that opens them all. */
@Composable
private fun MoreWaiting(n: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(T.rMd).background(T.wash).clickable(onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 14.dp).labAnchor("waiting:more"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText("$n more waiting", Modifier.weight(1f), style = T.body.copy(fontWeight = FontWeight.Medium))
        Icon(Glyph.Chevron, T.text2, size = 18.dp)
    }
}

/**
 * Two ways into the rest of the day: the tasks due with no time (to tick, or plan into a gap), and
 * the blocks still to name, which scrolls the route to the first of them.
 */
@Composable
private fun Shortcuts(store: ConceptStore, ui: TimelineUi) {
    val scope = rememberCoroutineScope()
    val untimed = store.untimed(ui.plans).filter { store.done[it.id] != true }
    val toName = ui.blocks.filter { it.tone != DayBlock.Tone.TRACKED }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.s2)) {
        Pill("${untimed.size}", if (untimed.size == 1) "due, no time" else "due, no time", Glyph.Task, Modifier.weight(1f), "untimed", enabled = store.tasksLoad != com.spadaval.lor.lab.Load.FAILED) { ui.sheet = Sheet.Untimed }
        Pill("${toName.size}", "to name", Glyph.Clock, Modifier.weight(1f), "to-name", enabled = toName.isNotEmpty()) {
            val first = toName.minByOrNull { it.start } ?: return@Pill
            val index = ui.route(store.clock()).indexOfFirst { it.key == first.id }
            if (index >= 0) scope.launch { store.list("route").animateScrollToItem(index) }
        }
    }
}

@Composable
private fun Pill(count: String, words: String, glyph: Glyph, modifier: Modifier, anchor: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier.clip(T.rMd).background(T.wash).border(1.dp, T.line, T.rMd).clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 44.dp).padding(horizontal = 12.dp).labAnchor(anchor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, T.text3, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        BasicText(count, style = T.timeLg.copy(fontSize = T.row.fontSize, color = if (enabled) T.text1 else T.text3))
        Spacer(Modifier.width(6.dp))
        BasicText(words, Modifier.weight(1f), style = T.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Glyph.Chevron, T.text3, size = 16.dp)
    }
}

/** The inner screen's right pane at rest: every waiting card in full, then the untimed tasks, then what the agent is on. */
@Composable
private fun RightPane(store: ConceptStore, ui: TimelineUi) {
    val items = store.waitingItems()
    val untimed = store.untimed(ui.plans)
    LazyColumn(Modifier.fillMaxSize().labAnchor("pane:waiting"), state = store.list("waiting"), contentPadding = PaddingValues(start = T.gutter, end = T.gutter, top = 36.dp, bottom = 120.dp)) {
        item(key = "head") { WaitingHead(store, items.size) }
        if (items.isEmpty()) item(key = "calm") { CalmLine(store) }
        items(items, key = { it.key }) { item ->
            Box(Modifier.animateItem(fadeInSpec = tween(200), placementSpec = tween(220), fadeOutSpec = tween(160)).padding(bottom = T.s2)) {
                ItemCard(store, ui, item, wide = true, selected = store.open == item.key)
            }
        }
        item(key = "untimed-head") { SectionLabel("Due, no time yet", trailing = "${untimed.count { store.done[it.id] != true }}") }
        if (untimed.isEmpty()) item(key = "untimed-none") { BasicText("Nothing due without a time.", style = T.label) }
        items(untimed, key = { "task:${it.id}" }) { t -> DueRow(store, ui, Item.Due(t), wide = true, selected = false) }
        item(key = "agent") { AgentWork(store) }
    }
}
