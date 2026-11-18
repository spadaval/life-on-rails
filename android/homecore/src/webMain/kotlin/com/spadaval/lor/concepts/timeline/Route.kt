package com.spadaval.lor.concepts.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock
import kotlin.time.Instant

/*
 * The route: the day unrolled down the page, waking at the top and sleep at the foot. Hour ticks
 * in the gutter, the spine beside them, and each stretch beside the spine: what was recorded
 * (tracked in amber, the agent's guesses waiting on a yes, unlabelled time faint and asking to be
 * named), now (the timer itself, or a Start), and what is planned (calendar events and planned
 * tasks dashed in cyan, free time between). Long empty stretches are squeezed.
 */

private val GUTTER = 50.dp
private val SPINE = 22.dp

@Composable
fun Route(store: ConceptStore, ui: TimelineUi, modifier: Modifier = Modifier) {
    val now = liveNow(store)
    val spans = ui.route(now)
    val list = store.list("route")
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val view = with(density) { maxHeight.toPx() }
        // Now a third of the way down, once; after that the position is his, and a fold keeps it.
        LaunchedEffect(ui) {
            if (ui.placed) return@LaunchedEffect
            ui.placed = true
            val nowIndex = spans.indexOfFirst { it.kind == SpanKind.NOW }
            if (nowIndex < 0) return@LaunchedEffect
            var above = with(density) { 12.dp.toPx() } // the list's top padding
            spans.take(nowIndex).forEach { above += with(density) { it.height.toPx() } }
            var want = (above - view / 3f).coerceAtLeast(0f)
            var i = 0
            var y = with(density) { 12.dp.toPx() }
            while (i < nowIndex && y + with(density) { spans[i].height.toPx() } <= want) { y += with(density) { spans[i].height.toPx() }; i++ }
            want -= y
            list.scrollToItem(i, want.toInt().coerceAtLeast(0))
        }
        // Room under the day's end, so the route can always be scrolled as far on a short pane as on
        // a tall one: a fold then keeps its place instead of the taller screen pulling it back.
        LazyColumn(Modifier.fillMaxSize().labAnchor("route").labAnchor("route-at:${list.firstVisibleItemIndex}"), state = list, contentPadding = PaddingValues(top = 12.dp, bottom = maxOf(120.dp, maxHeight * 0.75f))) {
            items(spans, key = { it.key }) { s ->
                when (s.kind) {
                    SpanKind.NOW -> NowLine(store, ui, now)
                    else -> SpanRow(store, ui, s, now)
                }
            }
        }
    }
}

/** One stretch: its hour ticks in the gutter, its piece of the spine, and what it holds. */
@Composable
private fun SpanRow(store: ConceptStore, ui: TimelineUi, s: Span, now: Instant) {
    Row(Modifier.fillMaxWidth().height(s.height)) {
        Ticks(store, s, now)
        Spine(s)
        Box(Modifier.weight(1f).fillMaxHeight().padding(end = T.gutter, top = 2.dp, bottom = 2.dp)) {
            when (s.kind) {
                SpanKind.BLOCK -> BlockCard(store, ui, s)
                SpanKind.RUNNING -> RunningCard(store, ui, s, now)
                SpanKind.UNTRACKED -> Gap(store, ui, s)
                SpanKind.SILENT -> Silent(store, s)
                SpanKind.EVENT -> Planned(store, s, s.event!!.title, "Calendar")
                SpanKind.PLAN -> Planned(store, s, s.plan!!.title, "Planned", ui)
                SpanKind.FREE -> Free(store, ui, s)
                SpanKind.NOW -> {}
            }
        }
    }
}

/** The hours that fall in a stretch, at their place in it. */
@Composable
private fun Ticks(store: ConceptStore, s: Span, now: Instant) {
    val len = (s.end - s.start).inWholeSeconds.coerceAtLeast(1)
    Box(Modifier.width(GUTTER).fillMaxHeight()) {
        var h = (s.start.epochSeconds + 3599) / 3600 * 3600
        var lastY = (-100).dp
        while (h < s.end.epochSeconds) {
            // The now line says its own time; an hour just beside it would only crowd it.
            if (kotlin.math.abs(h - now.epochSeconds) < 600) { h += 3600; continue }
            val y = s.height * ((h - s.start.epochSeconds).toFloat() / len)
            // Where empty hours are squeezed, a tick too close to the last is left out.
            if (y - lastY < 20.dp) { h += 3600; continue }
            lastY = y
            BasicText(
                LocalClock.time(Instant.fromEpochSeconds(h), store.zone), Modifier.fillMaxWidth().offset(y = y - 8.dp).padding(end = 6.dp),
                style = T.time.copy(textAlign = TextAlign.End),
            )
            h += 3600
        }
    }
}

/** The spine: solid where time was recorded, faint where it wasn't, dashed where it is still to come. */
@Composable
private fun Spine(s: Span) {
    val (color, width, dash) = when (s.kind) {
        SpanKind.BLOCK -> when (s.block!!.tone) {
            DayBlock.Tone.TRACKED -> Triple(T.amber, 2.dp, false)
            DayBlock.Tone.PROPOSED -> Triple(T.amber.copy(alpha = 0.45f), 2.dp, false)
            DayBlock.Tone.BARE -> Triple(T.unl, 2.dp, false)
        }
        SpanKind.RUNNING -> Triple(T.amber, 3.dp, false)
        SpanKind.UNTRACKED -> Triple(T.unlLine, 1.5.dp, true)
        SpanKind.SILENT -> Triple(T.red.copy(alpha = 0.6f), 1.5.dp, true)
        SpanKind.EVENT, SpanKind.PLAN -> Triple(T.cyanLine, 2.dp, true)
        SpanKind.FREE -> Triple(T.unlLine, 1.5.dp, true)
        SpanKind.NOW -> Triple(Color.Transparent, 0.dp, false)
    }
    Canvas(Modifier.width(SPINE).fillMaxHeight()) {
        val x = size.width / 2
        drawLine(
            color, Offset(x, 0f), Offset(x, size.height), width.toPx(),
            cap = StrokeCap.Butt, pathEffect = if (dash) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())) else null,
        )
    }
}

/** "12:00–12:35". */
private fun ConceptStore.range(a: Instant, z: Instant) = "${LocalClock.time(a, zone)}–${LocalClock.time(z, zone)}"

/**
 * A recorded block. Tracked: amber, counted. Proposed: the agent's label with a one-tap yes.
 * Unlabelled: faint, and it asks to be named. A tap lifts it and opens its sheet to relabel.
 */
@Composable
private fun BlockCard(store: ConceptStore, ui: TimelineUi, s: Span) {
    val b = s.block!!
    val selected = ui.selected == b.id
    val lift by animateFloatAsState(if (selected) 1f else 0f, tween(180))
    val shape = T.rMd
    val (fill, edge) = when (b.tone) {
        DayBlock.Tone.TRACKED -> Color(0xFF2A1D10) to T.amber.copy(alpha = 0.32f)
        DayBlock.Tone.PROPOSED -> T.surface1 to T.line2
        DayBlock.Tone.BARE -> T.unlSoft to T.unlLine
    }
    val compact = s.height < 60.dp
    Row(
        Modifier.fillMaxSize()
            .then(if (lift > 0f) Modifier.shadow(14.dp * lift, shape, clip = false) else Modifier)
            .clip(shape).background(if (selected) T.surface2 else fill)
            .then(if (b.tone == DayBlock.Tone.BARE) Modifier.dashedBorder(edge, shape = 10.dp) else Modifier.border(1.dp, if (selected) T.amberEdge else edge, shape))
            .clickable { ui.selected = b.id; ui.sheet = Sheet.Block(b.id) }
            .labAnchor("block:${b.id}").labAnchor("block:${b.title}")
            // Its exact interval, in epoch seconds, for scripts that check a timer's endpoints.
            .labAnchor("span:${b.title}:${b.start.epochSeconds}-${b.end.epochSeconds}"),
        verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (b.tone == DayBlock.Tone.TRACKED) Box(Modifier.width(3.dp).fillMaxHeight().background(T.amber))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp, top = if (compact) 0.dp else 8.dp)) {
            val titleColor = if (b.tone == DayBlock.Tone.BARE) T.text2 else T.text1
            if (compact) Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(b.title, Modifier.weight(1f, fill = false), style = T.body.copy(fontWeight = FontWeight.Medium, color = titleColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                BasicText("${store.range(b.start, b.end)} · ${short(b.end - b.start)}", style = T.time, maxLines = 1)
            } else {
                BasicText(b.title, style = T.row.copy(color = titleColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("${store.range(b.start, b.end)} · ${short(b.end - b.start)} · ${b.from}", style = T.time, maxLines = 1)
            }
        }
        when (b.tone) {
            DayBlock.Tone.PROPOSED -> Row(Modifier.fillMaxHeight().padding(end = if (store.lorLive) 4.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText("${b.label}?", Modifier.widthIn(max = 150.dp), style = T.label.copy(color = T.amber, textAlign = TextAlign.End), maxLines = 1, overflow = TextOverflow.Ellipsis)
                // While LoR isn't answering a yes can't be saved, so there is no tick to press.
                if (store.lorLive) IconButton(Glyph.Check, "Yes, ${b.label}", { ui.confirm(b.id) }, tint = T.amber, anchor = "confirm:${b.id}", size = 40.dp)
            }
            DayBlock.Tone.BARE -> Box(Modifier.fillMaxHeight().padding(end = 12.dp), contentAlignment = Alignment.Center) {
                BasicText("Name it", style = T.label.copy(color = T.text3))
            }
            DayBlock.Tone.TRACKED -> {}
        }
    }
}

/** A dashed outline, for what is not settled: unlabelled time, and what is planned. */
fun Modifier.dashedBorder(color: Color, shape: Dp = 10.dp, width: Dp = 1.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(shape.toPx()), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
    )
}

/** The timer, running: the block growing from the now line, its readout, Switch and Stop. */
@Composable
private fun RunningCard(store: ConceptStore, ui: TimelineUi, s: Span, now: Instant) {
    val t = store.timer ?: return
    val shape = T.rLg
    Column(
        Modifier.fillMaxSize()
            .shadow(16.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(shape).background(Color(0xFF2B1D0F)).border(1.dp, T.amberEdge, shape)
            .clickable { ui.sheet = Sheet.Timer }.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
            .labAnchor("route:running"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(T.amber))
            Spacer(Modifier.width(8.dp))
            BasicText("RUNNING SINCE ${LocalClock.time(t.start, store.zone)}", Modifier.weight(1f), style = T.caps.copy(color = T.amber))
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BasicText(t.label ?: "No label yet", Modifier.weight(1f), style = T.row.copy(color = if (t.label == null) T.text2 else T.text1), maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(readout(now - t.start), Modifier.labAnchor("route:readout"), style = T.readout)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(T.s2)) {
            TButton("Switch", { ui.sheet = Sheet.Timer }, glyph = Glyph.Swap, height = 36.dp, anchor = "route:switch", enabled = store.timerReachable)
            TButton("Stop", { ui.stopTimer() }, glyph = Glyph.Stop, height = 36.dp, anchor = "route:stop", enabled = store.timerReachable)
        }
    }
}

/**
 * Now: the line across the route, its time in the gutter and the reticle on the spine. With nothing
 * running, a Start sits under it with his recent labels, one tap each; starting grows a block from here.
 */
@Composable
private fun NowLine(store: ConceptStore, ui: TimelineUi, now: Instant) {
    Column(Modifier.fillMaxWidth().labAnchor("now")) {
        Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.width(GUTTER).padding(end = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                BasicText(LocalClock.time(now, store.zone), style = T.time.copy(color = T.amber, fontWeight = FontWeight.SemiBold))
            }
            Canvas(Modifier.width(SPINE).fillMaxHeight()) {
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(T.amber.copy(alpha = 0.18f), 10.dp.toPx(), c)
                drawCircle(T.amber, 6.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                drawCircle(T.amber, 2.dp.toPx(), c)
            }
            Box(Modifier.weight(1f).height(2.dp).background(T.amber))
            BasicText("NOW", Modifier.padding(start = 6.dp, end = T.gutter), style = T.caps.copy(color = T.amber))
        }
        if (store.timer == null) {
            Row(
                Modifier.fillMaxWidth().padding(start = GUTTER + SPINE, end = T.gutter, top = 8.dp, bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(T.s2),
            ) {
                TButton("Start", { ui.sheet = Sheet.Timer }, kind = Kind.PRIMARY, glyph = Glyph.Play, height = 40.dp, anchor = "route:start", enabled = store.timerReachable)
                store.labels.take(3).forEach { l ->
                    TButton(l, { ui.startTimer(l) }, height = 40.dp, anchor = "route:start:$l", enabled = store.timerReachable)
                }
            }
        }
    }
}

/** A gap with nothing recorded. A long one before now offers to name it. */
@Composable
private fun Gap(store: ConceptStore, ui: TimelineUi, s: Span) {
    if (s.height < 20.dp) return
    val past = s.end <= store.clock()
    val len = s.end - s.start
    Row(
        Modifier.fillMaxSize().then(if (past && len.inWholeMinutes >= 10 && store.lorLive) Modifier.clip(T.rSm).clickable { ui.selected = s.key; ui.sheet = Sheet.Block(s.key) } else Modifier)
            .padding(start = 12.dp, end = 12.dp).labAnchor("gap:${LocalClock.time(s.start, store.zone)}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(short(len), style = T.time.copy(color = T.text3))
        Spacer(Modifier.width(8.dp))
        BasicText(if (past) "nothing recorded" else "", Modifier.weight(1f), style = T.label.copy(color = T.text3))
        if (past && len.inWholeMinutes >= 10 && store.lorLive) BasicText("Name it", style = T.label.copy(color = T.text2))
    }
}

/** LoR stopped answering here: the route doesn't pretend to know what happened since. */
@Composable
private fun Silent(store: ConceptStore, s: Span) {
    Column(
        Modifier.fillMaxSize().clip(T.rMd).background(T.redSoft).dashedBorder(T.redEdge).padding(horizontal = 12.dp, vertical = 10.dp).labAnchor("route:silent"),
        verticalArrangement = Arrangement.Center,
    ) {
        if (store.home == null) {
            BasicText("LoR can't be reached", style = T.body.copy(fontWeight = FontWeight.Medium))
            BasicText("The day, and what you did in it, shows once it answers", style = T.label)
        } else {
            BasicText("No word from LoR since ${LocalClock.time(s.start, store.zone)}", style = T.body.copy(fontWeight = FontWeight.Medium))
            BasicText("What you did since shows once it answers", style = T.label)
        }
    }
}

/** A calendar event, or a task planned into a gap: dashed in cyan until it happens. */
@Composable
private fun Planned(store: ConceptStore, s: Span, title: String, kind: String, ui: TimelineUi? = null) {
    val compact = s.height < 60.dp
    Row(
        Modifier.fillMaxSize().clip(T.rMd).background(T.cyanSoft).dashedBorder(T.cyanLine)
            .then(if (s.plan != null && ui != null) Modifier.clickable { ui.unplan(s.plan) } else Modifier)
            .padding(horizontal = 12.dp).labAnchor("${kind.lowercase()}:$title"),
        verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top,
    ) {
        Column(Modifier.weight(1f).padding(top = if (compact) 0.dp else 8.dp)) {
            if (compact) Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(title, Modifier.weight(1f, fill = false), style = T.body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                BasicText("${store.range(s.start, s.end)} · $kind", style = T.time.copy(color = T.cyan), maxLines = 1)
            } else {
                BasicText(title, style = T.row, maxLines = 2, overflow = TextOverflow.Ellipsis)
                BasicText("${store.range(s.start, s.end)} · $kind", style = T.time.copy(color = T.cyan))
            }
        }
    }
}

/** Free time ahead: how long, and a way to plan a task into it. Nothing is put here unasked. */
@Composable
private fun Free(store: ConceptStore, ui: TimelineUi, s: Span) {
    val can = store.lorLive && store.untimed(ui.plans).any { store.done[it.id] != true }
    Row(
        Modifier.fillMaxSize().padding(start = 12.dp).labAnchor("free:${LocalClock.time(s.start, store.zone)}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(short(s.end - s.start), style = T.time.copy(color = T.text2, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.width(6.dp))
        BasicText("free", Modifier.weight(1f), style = T.label.copy(color = T.text3))
        if (can) TButton("Plan a task", { ui.sheet = Sheet.Plan(s.start, s.end) }, kind = Kind.QUIET, glyph = Glyph.Plus, height = 36.dp, anchor = "plan:${LocalClock.time(s.start, store.zone)}")
    }
}
