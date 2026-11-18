package com.spadaval.lor.concepts.context

import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.ui.draw.*
import com.spadaval.lor.concepts.kit.MenuItem
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lab.LocalWallpaperKind
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.T3Card
import com.spadaval.lor.concepts.kit.BlurredWallpaper
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.KeyboardHeight
import com.spadaval.lor.concepts.kit.Sheet as KitSheet
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.lor.LorTasks
import com.spadaval.lor.t3.ThreadState
import com.spadaval.lor.ui.LocalClock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/*
 * Now, left of home: the agent page and the LoR panel in one. It leads with what waits on him from
 * T3 (the spec puts those first), each card with its action on it; then the current context as one
 * raised surface with everything about it; then the next fixed event and what fits before it; then,
 * flat and quiet, what waits elsewhere, each row opening in its own context. A bar of contexts at the
 * top switches the surface. Unfolded, all of that is the left column, and the open thread, page or
 * person (or, with nothing open, the context's page) is the right.
 */
@Composable
fun NowPage(store: ConceptStore, ui: CtxUi, wide: Boolean) {
    Box(Modifier.fillMaxSize().labAnchor("now")) {
        NowGround()
        if (wide) {
            Row(Modifier.fillMaxSize().padding(top = 32.dp)) {
                Column(Modifier.width(440.dp).fillMaxHeight()) {
                    ContextBar(store, ui)
                    NowList(store, ui, wide, Modifier.weight(1f))
                    Dock(store, ui, CtxUi.NOW, wide = false)
                }
                Box(Modifier.weight(1f).fillMaxHeight().padding(end = 12.dp, top = 8.dp, bottom = 16.dp).padding(bottom = if (ui.keyboard) KeyboardHeight - 16.dp else 0.dp)) { RightPane(store, ui) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
                ContextBar(store, ui)
                NowList(store, ui, wide, Modifier.weight(1f))
                Dock(store, ui, CtxUi.NOW, wide)
            }
        }
    }
}

/** The ground behind Now: the wallpaper blurred under most of the ground, with a faint warm light where the context sits. */
@Composable
fun NowGround() {
    BlurredWallpaper({ 1f })
    Box(
        Modifier.fillMaxSize().background(C.veil).drawBehind {
            drawRect(Brush.radialGradient(listOf(C.amber.copy(alpha = 0.07f), Color.Transparent), center = Offset(size.width * 0.15f, size.height * 0.3f), radius = size.width * 1.1f))
            drawRect(Brush.radialGradient(listOf(C.cyan.copy(alpha = 0.05f), Color.Transparent), center = Offset(size.width, size.height), radius = size.width))
        },
    )
}

/**
 * The bar of contexts, like tabs: the current one (amber dot, "Now"), the few most recent, Loose
 * ends, and All, which lists every project, goal, person and slot.
 */
@Composable
fun ContextBar(store: ConceptStore, ui: CtxUi) {
    val (shown, _) = ui.shown()
    val (current, _) = ui.model.current()
    val list = ui.model.recent(current)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).labAnchor("bar"), verticalAlignment = Alignment.CenterVertically) {
    Row(
        Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        list.forEach { c ->
            val on = c.key == shown.key
            val isNow = c.key == current.key
            val bg by animateColorAsState(if (on) C.region3 else Color.Transparent, tween(180))
            Column(
                Modifier.clip(C.rMd).background(bg).border(1.dp, if (on) C.line2 else Color.Transparent, C.rMd)
                    .clickable { if (isNow) ui.followNow() else ui.switchTo(c) }
                    .heightIn(min = 48.dp).widthIn(max = 200.dp).padding(horizontal = 12.dp, vertical = 6.dp).labAnchor(if (isNow) "bar:now" else "bar:${c.title}"),
                verticalArrangement = Arrangement.Center,
            ) {
                if (isNow) Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(C.amber))
                    Spacer(Modifier.width(5.dp))
                    BasicText("NOW", style = C.label.copy(color = C.amber, fontSize = C.meta.fontSize * 0.85f))
                }
                BasicText(c.title, style = C.meta.copy(color = if (on) C.text1 else C.text2, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
        Box(Modifier.size(1.dp, 28.dp).background(C.line2))
        Row(
            Modifier.padding(start = 4.dp).clip(C.rMd).clickable { ui.sheet = Sheet.Contexts }.heightIn(min = 48.dp).padding(horizontal = 10.dp).labAnchor("bar:all"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Grid, C.text2, size = 16.dp)
            Spacer(Modifier.width(6.dp))
            BasicText("All", style = C.meta.copy(color = C.text2, fontWeight = FontWeight.Medium))
        }
        MoreButton(ui, { Menu.Page(it) }, "now:more")
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun NowList(store: ConceptStore, ui: CtxUi, wide: Boolean, modifier: Modifier) {
    val (ctx, why) = ui.shown()
    val cards = store.cards
    val shownCards = if (cards.size > 4 && !ui.allCards) cards.take(3) else cards
    val elsewhere = ui.model.elsewhere(ctx)
    LazyColumn(modifier.fillMaxWidth().labAnchor("now:rows"), state = store.list("now"), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
        item(key = "notes") { Notes(store, ui) }
        if (cards.isNotEmpty()) {
            item(key = "waiting") { Label("Waiting on you", cards.size, C.cyan, Modifier.padding(top = 4.dp, bottom = 8.dp)) }
            items(shownCards, key = { it.key }) { c -> Box(Modifier.padding(bottom = 8.dp)) { CardRow(store, ui, c) } }
            if (shownCards.size < cards.size) item(key = "cards:more") {
                CButton("Show ${cards.size - shownCards.size} more waiting", { ui.allCards = true }, Modifier.fillMaxWidth().padding(bottom = 8.dp), kind = Tone.QUIET, glyph = Glyph.ChevronDown, anchor = "cards:more")
            }
        } else if (store.home?.needsYou.isNullOrEmpty()) item(key = "calm") { Box(Modifier.padding(bottom = 12.dp)) { Calm(store) } }
        item(key = "surface") {
            Box(Modifier.padding(top = 8.dp)) { ContextSurface(store, ui, ctx, why, wide) }
        }
        item(key = "next") { Box(Modifier.padding(top = C.regionGap)) { NextContext(store, ui, ctx) } }
        if (elsewhere.isNotEmpty()) {
            item(key = "elsewhere") {
                Column(Modifier.padding(top = C.regionGap, bottom = 4.dp, start = 4.dp)) {
                    Label("Waiting elsewhere", elsewhere.size, C.text3)
                    val flags = elsewhere.count { it.flag }
                    if (flags > 0) BasicText("$flags flagged by the agent, ${elsewhere.size - flags} due", style = C.meta.copy(color = C.text3))
                }
            }
            items(elsewhere, key = { "else:${it.key}" }) { e -> ElsewhereRow(store, ui, e) }
        }
    }
}

// ---- The context surface ----

/**
 * The current context as one raised surface: what it is and why it is shown, its title and its
 * timer, the next task, then its regions on their own tones (tasks, the agent's work, people, its
 * page, its time), and its uncommon things folded at the foot. Switching context turns the whole
 * surface: the new title arrives, and each region after it, a beat apart.
 */
@Composable
fun ContextSurface(store: ConceptStore, ui: CtxUi, ctx: Ctx, why: Why, wide: Boolean) {
    AnimatedContent(
        ctx, Modifier.fillMaxWidth(),
        transitionSpec = { (fadeIn(tween(220, 40)) + slideInHorizontally(tween(240)) { it / 14 }) togetherWith fadeOut(tween(120)) },
        contentKey = { it.key },
        label = "context",
    ) { c ->
        Column(
            Modifier.fillMaxWidth()
                .shadow(22.dp, C.rSurface, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(C.rSurface).background(C.surface).border(1.dp, C.surfaceEdge, C.rSurface)
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp)
                .labAnchor("ctx").labAnchor("ctx:${c.title}"),
        ) {
            Head(store, ui, c, why)
            var i = 1
            Arrive(c.key, i++) { TimerHead(store, ui, c) }
            Arrive(c.key, i++) { Lead(store, ui, c, why) }
            val m = ui.model
            val tasks = m.mineOf(c)
            val rest = tasks.drop(1)
            if (rest.isNotEmpty() || c.kind == Kind.LOOSE) Arrive(c.key, i++) { TasksRegion(store, ui, c, rest) }
            val agent = m.agentOf(c)
            if (agent.isNotEmpty()) Arrive(c.key, i++) { AgentRegion(store, ui, agent) }
            val people = m.peopleOf(c)
            if (people.isNotEmpty()) Arrive(c.key, i++) { PeopleRegion(store, ui, people) }
            m.pageOf(c)?.let { p -> if (!wide) Arrive(c.key, i++) { PageRegion(ui, c, p) } }
            if (c.kind != Kind.SLOT) Arrive(c.key, i++) { TimeRegion(store, ui, c) }
            Arrive(c.key, i) { About(store, ui, c) }
        }
    }
}

/** A region of the surface arriving after the title, [index] beats later, when the context changes. */
@Composable
private fun Arrive(key: String, index: Int, content: @Composable () -> Unit) {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { kotlinx.coroutines.delay(40L * index); a.animateTo(1f, tween(220)) }
    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 14.dp.toPx() }) { content() }
}

@Composable
private fun Head(store: ConceptStore, ui: CtxUi, c: Ctx, why: Why) {
    val goal = ui.model.goalOf(c)?.takeIf { it != c.title }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(glyphOf(c.kind), C.text3, size = 14.dp)
        Spacer(Modifier.width(6.dp))
        BasicText(c.kind.word.uppercase(), style = C.label)
        if (goal != null) {
            BasicText("  ·  ", style = C.meta.copy(color = C.text4))
            BasicText(goal, Modifier.weight(1f, fill = false).clip(C.rXs).clickable { ui.switchTo(Ctx(Kind.GOAL, goal)) }.labAnchor("ctx:goal"), style = C.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(4.dp))
    BasicText(why.words, Modifier.labAnchor("ctx:why:${why.name.lowercase()}"), style = C.meta.copy(color = if (why == Why.TIMER) C.amber else C.text3))
    Spacer(Modifier.height(6.dp))
    BasicText(c.title, Modifier.labAnchor("ctx:title"), style = C.ctx)
    val now = store.now
    when {
        c.kind == Kind.SLOT && c.event != null -> BasicText(
            "${LocalClock.time(c.event.start, store.zone)}–${LocalClock.time(c.event.end, store.zone)} · " + if (c.event.start > now) "in ${spoken(c.event.start - now)}" else "now",
            style = C.body.copy(color = C.cyan),
        )
        c.kind == Kind.PERSON -> store.people?.let { it.overdue + it.birthdays }?.firstOrNull { it.name == c.title }?.let { p ->
            BasicText(listOfNotNull(p.overdueBy?.let { "$it days past keeping in touch" }, p.birthdayIn?.let { "birthday in $it days" }).joinToString(" · "), style = C.body.copy(color = C.text2))
        }
        else -> {}
    }
}

fun glyphOf(k: Kind): Glyph = when (k) {
    Kind.PROJECT -> Glyph.Grid; Kind.GOAL -> Glyph.Flag; Kind.PERSON -> Glyph.Person; Kind.SLOT -> Glyph.Calendar
    Kind.LOOSE -> Glyph.Task; Kind.LABEL, Kind.UNNAMED -> Glyph.Clock
}

/**
 * The context's own timer: running here, an amber panel with what it is on, the clock, Stop and
 * Switch; running elsewhere, where; idle, Start here. Starting a timer on something makes it the
 * current context.
 */
@Composable
private fun TimerHead(store: ConceptStore, ui: CtxUi, c: Ctx) {
    val running = store.timer
    val here = running != null && ui.model.ctxOfLabel(running.label).key == c.key
    Spacer(Modifier.height(C.s4))
    if (here && running != null) {
        val now = liveNow(store)
        Column(Modifier.fillMaxWidth().clip(C.rLg).background(C.amberFaint).border(1.dp, C.amberEdge, C.rLg).padding(14.dp).labAnchor("ctx:timer")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(C.amber))
                Spacer(Modifier.width(8.dp))
                BasicText(running.label?.let { "On $it since ${LocalClock.time(running.start, store.zone)}" } ?: "Running since ${LocalClock.time(running.start, store.zone)}, no label", style = C.meta.copy(color = C.amber), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(readout(now - running.start), Modifier.weight(1f).labAnchor("ctx:readout"), style = C.num)
                CButton("Stop", { if (ui.saves()) store.stop() }, kind = Tone.PRIMARY, glyph = Glyph.Stop, compact = true, anchor = "ctx:stop", enabled = store.timerOk())
                Spacer(Modifier.width(6.dp))
                CButton("Switch", { ui.switching = true; ui.sheet = Sheet.Timer }, kind = Tone.QUIET, glyph = Glyph.Swap, compact = true, anchor = "ctx:switch", enabled = store.timerOk())
            }
        }
        return
    }
    if (c.kind == Kind.SLOT) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (running != null) {
            val elsewhere = ui.model.ctxOfLabel(running.label)
            BasicText(
                "Timer on ${elsewhere.title}", Modifier.weight(1f).clip(C.rXs).clickable { ui.followNow() }.labAnchor("ctx:timer-elsewhere"),
                style = C.meta.copy(color = C.amber), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            CButton("Switch here", { if (ui.saves()) { store.switch(c.title); ui.followNow() } }, glyph = Glyph.Swap, compact = true, anchor = "ctx:switch-here", enabled = store.timerOk() && c.kind != Kind.UNNAMED)
        } else {
            BasicText("No timer running", Modifier.weight(1f), style = C.meta.copy(color = C.text3))
            CButton("Start here", { if (ui.saves()) { store.start(c.title.takeIf { c.kind != Kind.UNNAMED && c.kind != Kind.LOOSE }); ui.followNow() } }, glyph = Glyph.Play, compact = true, anchor = "ctx:start", enabled = store.timerOk())
        }
    }
}

/** The next task, what leads: the one thing's own task when the agent chose it, with its reason. */
@Composable
private fun Lead(store: ConceptStore, ui: CtxUi, c: Ctx, why: Why) {
    val m = ui.model
    Spacer(Modifier.height(C.s5))
    when (c.kind) {
        Kind.UNNAMED, Kind.LABEL -> return Unlinked(store, ui, c)
        else -> {}
    }
    val tasks = m.mineOf(c)
    val lead = tasks.firstOrNull()
    val one = m.oneThing()
    Label(if (lead != null && lead.id == one?.id && why == Why.ONE) "The one thing" else if (lead?.flagged == true) "Next · flagged" else "Next")
    Spacer(Modifier.height(C.s2))
    if (lead == null) {
        BasicText(
            when (c.kind) {
                Kind.PERSON -> "No open task names ${c.title.substringBefore(' ')}."
                Kind.SLOT -> "Nothing open is about this event."
                else -> "Nothing open here."
            },
            Modifier.padding(vertical = 6.dp), style = C.body.copy(color = C.text2),
        )
        return
    }
    TaskRow(store, ui, lead, lead = true, showHome = c.kind == Kind.PERSON || c.kind == Kind.SLOT)
    if (c.kind == Kind.LOOSE && ui.expanded != lead.id) SUGGEST[lead.title]?.let { Box(Modifier.padding(start = 48.dp, top = 6.dp)) { SuggestLine(ui, lead, it) } }
    if (lead.id == one?.id && why == Why.ONE) store.home?.one?.why?.let { BasicText(it, Modifier.padding(start = 4.dp, top = 6.dp), style = C.meta) }
}

/**
 * A timer label that belongs to no context, or a timer with no label: say so, and offer where it
 * could go. Linking keeps the time already counted (nothing is stopped or restarted), with Undo.
 */
@Composable
private fun Unlinked(store: ConceptStore, ui: CtxUi, c: Ctx) {
    val key = if (c.kind == Kind.UNNAMED) "" else c.title
    val m = ui.model
    val suggestion = if (c.kind == Kind.LABEL) SUGGEST_LABEL[c.title] else m.oneThing()?.let { m.homeOf(it).title }
    val all = ui.more["link:$key"] == true
    val choices = (listOfNotNull(suggestion) + m.recent(LOOSE).filter { it.kind == Kind.PROJECT || it.kind == Kind.GOAL }.map { it.title } + if (all) m.allProjects().map { it.title } else emptyList()).distinct().let { if (all) it else it.take(4) }
    Column(Modifier.fillMaxWidth().clip(C.rRegion).background(C.region).border(1.dp, C.line, C.rRegion).padding(14.dp).labAnchor("ctx:unlinked")) {
        BasicText(if (c.kind == Kind.UNNAMED) "This time has no name yet" else "“${c.title}” counts toward nothing yet", style = C.title)
        Spacer(Modifier.height(4.dp))
        BasicText("It is kept either way. Pick where it counts: the time so far goes there too, and nothing stops.", style = C.meta)
        if (suggestion != null) {
            Spacer(Modifier.height(C.s3))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Glyph.Agent, C.cyan, size = 14.dp)
                Spacer(Modifier.width(8.dp))
                BasicText("The agent suggests $suggestion", style = C.meta.copy(color = C.cyan))
            }
        }
        Spacer(Modifier.height(C.s3))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(C.s2), verticalArrangement = Arrangement.spacedBy(C.s2)) {
            choices.forEach { t -> Pill(t, selected = t == suggestion, onClick = { ui.linkLabel(key, t); ui.followNow() }, anchor = "link-to:$t", enabled = store.lorLive, glyph = if (t == suggestion) Glyph.Agent else null) }
            if (!all) Pill("Another…", selected = false, onClick = { ui.more["link:$key"] = true }, anchor = "ctx:pick", enabled = store.lorLive)
        }
    }
}

/**
 * A task: a check, its title, and when it is due; a tap opens it in place, in its row, with its due
 * date, a timer on it, and where it lives.
 */
@Composable
fun TaskRow(store: ConceptStore, ui: CtxUi, t: LorTasks.Task, lead: Boolean = false, showHome: Boolean = false) {
    val done = store.done[t.id] == true
    val open = ui.expanded == t.id
    val bg by animateColorAsState(when { open -> C.region2; lead -> C.region; else -> Color.Transparent }, tween(160))
    Column(
        Modifier.fillMaxWidth().clip(C.rMd).background(bg).border(1.dp, if (lead || open) C.line2 else Color.Transparent, C.rMd).animateContentSize(tween(200)).labAnchor("task:${t.title}"),
    ) {
        Row(Modifier.fillMaxWidth().clickable { ui.expanded = if (open) null else t.id }.heightIn(min = if (lead) 60.dp else C.row).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Check(done, { if (ui.saves()) { if (done) store.untick(t.id) else store.tick(t.id) } }, anchor = "tick:${t.title}", enabled = store.lorLive)
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                TaskTitle(t.title, done, style = if (lead) C.title.copy(color = C.amberHi) else C.body)
                val sub = listOfNotNull(
                    t.waitingOn?.let { "waiting on $it" },
                    t.due?.let { dueWords(t, store) },
                    if (showHome) ui.model.homeOf(t).title else null,
                    if (store.timer?.label == t.title) "timing" else null,
                )
                if (sub.isNotEmpty()) BasicText(sub.joinToString(" · "), style = C.meta.copy(color = if (t.overdue && !done) C.red else C.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(if (open) Glyph.ChevronUp else Glyph.ChevronDown, C.text3, size = 16.dp)
        }
        if (open) TaskOpen(store, ui, t)
    }
}

/** A task opened in place: its due date, a timer on it, where it lives (and, under nothing, where the agent would put it). */
@Composable
private fun TaskOpen(store: ConceptStore, ui: CtxUi, t: LorTasks.Task) {
    Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp).labAnchor("task:open")) {
        val today = store.today
        fun next(day: DayOfWeek): LocalDate { var d = today.plus(DatePeriod(days = 1)); while (d.dayOfWeek != day) d = d.plus(DatePeriod(days = 1)); return d }
        val choices = listOf("Today" to today, "Tomorrow" to today.plus(DatePeriod(days = 1)), "Fri" to next(DayOfWeek.FRIDAY), "None" to null)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("Due", Modifier.weight(1f), style = C.label)
            BasicText(t.due?.let { "${it.dayOfWeek.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.dayOfMonth}" } ?: "No date", Modifier.labAnchor("due:is:${t.due ?: "none"}"), style = C.meta)
        }
        Spacer(Modifier.height(C.s2))
        Segmented(choices, choices.firstOrNull { it.second == t.due }?.second, { d -> if (ui.saves()) store.setDue(t.id, d) }, Modifier.fillMaxWidth(), anchor = "due", enabled = store.lorLive)
        Spacer(Modifier.height(C.s3))
        Row(horizontalArrangement = Arrangement.spacedBy(C.s2), verticalAlignment = Alignment.CenterVertically) {
            val timing = store.timer?.label == t.title
            CButton(if (timing) "Timing" else "Start timer on it", {
                if (!timing && ui.saves()) { if (store.timer != null) store.switch(t.title) else store.start(t.title); ui.followNow() }
            }, glyph = Glyph.Play, compact = true, anchor = "task:timer", enabled = store.timerOk() && !timing)
            MoreButton(ui, { Menu.Row("task:${t.id}", it) }, "more:task:${t.title}")
        }
        val home = ui.model.homeOf(t)
        val suggest = SUGGEST[t.title]
        if (home.kind == Kind.LOOSE && suggest != null) {
            Spacer(Modifier.height(C.s3))
            SuggestLine(ui, t, suggest)
        } else if (home.kind != Kind.LOOSE && home.key != ui.shown().first.key) {
            Spacer(Modifier.height(C.s2))
            BasicText("In ${home.title}", Modifier.clip(C.rXs).clickable { ui.switchTo(home) { ui.expanded = t.id } }.padding(vertical = 6.dp).labAnchor("task:home"), style = C.meta.copy(color = C.text2))
        }
    }
}

@Composable
private fun SuggestLine(ui: CtxUi, t: LorTasks.Task, to: String) {
    Row(Modifier.fillMaxWidth().clip(C.rSm).background(C.cyanFaint).border(1.dp, C.cyanEdge.copy(alpha = 0.25f), C.rSm).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp).labAnchor("suggest:${t.title}"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Glyph.Agent, C.cyan, size = 14.dp)
        Spacer(Modifier.width(8.dp))
        BasicText("Agent: put it under $to", Modifier.weight(1f), style = C.meta.copy(color = C.cyan), maxLines = 2)
        CButton("Link", { ui.link(t, to) }, kind = Tone.AGENT, compact = true, anchor = "link:${t.title}")
    }
}

@Composable
private fun TasksRegion(store: ConceptStore, ui: CtxUi, c: Ctx, rest: List<LorTasks.Task>) {
    Region("Tasks", rest.size, anchor = "region:tasks") {
        if (c.kind == Kind.LOOSE) {
            BasicText("These belong to no project or goal. The agent suggests where each goes; nothing moves until you link it.", Modifier.padding(start = 4.dp, bottom = 6.dp), style = C.meta)
        }
        if (rest.isEmpty()) BasicText("Only the next one.", Modifier.padding(4.dp), style = C.meta.copy(color = C.text3))
        rest.forEach { t ->
            TaskRow(store, ui, t, showHome = c.kind == Kind.PERSON || c.kind == Kind.SLOT)
            val s = SUGGEST[t.title]
            if (c.kind == Kind.LOOSE && s != null && ui.expanded != t.id) Box(Modifier.padding(start = 48.dp, bottom = 6.dp)) { SuggestLine(ui, t, s) }
        }
    }
}

/** What the context gave the agent: its threads (running ones lit) and its tasks queued for it. */
@Composable
private fun AgentRegion(store: ConceptStore, ui: CtxUi, work: List<AgentWork>) {
    Region("Given to the agent", work.size, tone = C.cyan, anchor = "region:agent") {
        work.forEach { w ->
            val t = w.thread
            val running = t?.state == ThreadState.RUNNING
            val waits = t != null && store.cards.any { it.thread.id == t.id }
            Row(
                Modifier.fillMaxWidth().clip(C.rMd).clickable(enabled = t != null) { ui.open = store.cards.firstOrNull { it.thread.id == t?.id }?.key ?: "thread:${t?.id}" }
                    .heightIn(min = C.row).padding(horizontal = 6.dp, vertical = 6.dp).labAnchor("work:${w.title}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (running || waits) C.cyan else Color.Transparent).border(1.5.dp, C.cyan.copy(alpha = if (t == null) 0.4f else 0.9f), CircleShape))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(w.title, style = C.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    BasicText(
                        when {
                            t == null -> "Queued, no thread yet" + (w.task?.due?.let { " · due ${dueWords(w.task, store)}" } ?: "")
                            waits -> "Waiting on you, above"
                            running -> "Working now" + (t.runStartedAt?.let(::parseOrNull)?.takeIf { it < store.now }?.let { " · ${spoken(store.now - it)}" } ?: "")
                            else -> "Thread in T3 · ${ago(parseOrNull(t.updatedAt), store.now)}"
                        },
                        style = C.meta.copy(color = if (running || waits) C.cyan else C.text3),
                    )
                }
                if (t != null) Icon(Glyph.Chevron, C.text3, size = 16.dp)
            }
        }
    }
}

@Composable
private fun PeopleRegion(store: ConceptStore, ui: CtxUi, people: List<PersonLink>) {
    Region("People", people.size, anchor = "region:people") {
        people.forEach { p ->
            Row(
                Modifier.fillMaxWidth().clip(C.rMd).clickable { ui.open = "person:${p.name}" }.heightIn(min = C.row).padding(horizontal = 4.dp, vertical = 6.dp).labAnchor("person:${p.name}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(p.name, 36)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(p.name, style = C.rowTitle, maxLines = 1)
                    BasicText(p.why, style = C.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (p.waiting) Chip("Waiting", C.amber)
            }
        }
    }
}

@Composable
private fun PageRegion(ui: CtxUi, c: Ctx, p: Page) {
    Region("Page", null, anchor = "region:page", action = { CButton("Open", { ui.open = "page:${c.title}" }, kind = Tone.QUIET, glyph = Glyph.Chevron, compact = true, anchor = "page:open") }) {
        Column(Modifier.fillMaxWidth().clip(C.rMd).clickable { ui.open = "page:${c.title}" }.padding(horizontal = 6.dp, vertical = 4.dp)) {
            BasicText(p.intro, style = C.body.copy(color = C.text2), maxLines = 2, overflow = TextOverflow.Ellipsis)
            p.sections.firstOrNull()?.let { (h, lines) ->
                Spacer(Modifier.height(8.dp))
                BasicText(h.uppercase(), style = C.label)
                lines.take(2).forEach { BasicText("•  $it", Modifier.padding(top = 2.dp), style = C.meta.copy(color = C.text2), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** Time this week: the goal's hours against its target, and today's running block here. "No data yet" when nothing was measured. */
@Composable
private fun TimeRegion(store: ConceptStore, ui: CtxUi, c: Ctx) {
    val goal = ui.model.goalOf(c)
    val week = goal?.let { WEEK[it] }
    val running = store.timer?.takeIf { ui.model.ctxOfLabel(it.label).key == c.key }
    Region("Time", null, anchor = "region:time") {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            if (running != null) {
                val now = liveNow(store)
                BasicText(spoken(now - running.start), style = C.title)
                BasicText("today, timed here", style = C.meta)
                Spacer(Modifier.height(C.s3))
            }
            if (week == null || store.homeLoad == Load.FAILED) {
                BasicText("No data yet", style = C.body.copy(color = C.text2), modifier = Modifier.labAnchor("time:none"))
                BasicText(if (goal == null) "Nothing here counts toward a goal's week." else "No time is committed to $goal this week.", style = C.meta.copy(color = C.text3))
            } else {
                val (h, target) = week
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicText("${h} h", style = C.title)
                    BasicText(" of ${target.toInt()} h this week", Modifier.weight(1f), style = C.meta)
                }
                BasicText(goal, style = C.meta.copy(color = C.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(C.rPill).background(C.line2)) {
                    Box(Modifier.fillMaxWidth((h / target).toFloat().coerceIn(0f, 1f)).height(6.dp).clip(C.rPill).background(C.amber))
                }
            }
        }
    }
}

/** The uncommon things of a context, folded at its foot: its goal and page, where it opens in LoR, cadence for a person. */
@Composable
private fun About(store: ConceptStore, ui: CtxUi, c: Ctx) {
    val open = ui.more[c.key] == true
    Spacer(Modifier.height(C.s4))
    Row(Modifier.fillMaxWidth().clip(C.rSm).clickable { ui.more[c.key] = !open }.heightIn(min = 44.dp).padding(horizontal = 6.dp).labAnchor("ctx:about"), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (open) Glyph.ChevronUp else Glyph.ChevronDown, C.text3, size = 14.dp)
        Spacer(Modifier.width(8.dp))
        BasicText("About this ${c.kind.word.lowercase().substringBefore(' ').let { if (c.kind == Kind.SLOT) "event" else if (c.kind == Kind.LOOSE) "list" else it }}", style = C.meta.copy(color = C.text2))
    }
    AnimatedVisibility(open, enter = expandVertically(tween(200)) + fadeIn(tween(200)), exit = shrinkVertically(tween(160)) + fadeOut(tween(160))) {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp).clip(C.rMd).background(C.region).border(1.dp, C.line, C.rMd)) {
            val entries = buildList {
                ui.model.goalOf(c)?.takeIf { it != c.title }?.let { add("Counts toward $it" to { ui.switchTo(Ctx(Kind.GOAL, it)) }) }
                ui.model.pageOf(c)?.let { p -> add("Notes · ${p.slug} · edited ${p.edited}" to { ui.open = "page:${c.title}" }) }
                if (c.kind == Kind.PERSON) add("Keep in touch: every ${store.people?.let { it.overdue + it.birthdays }?.firstOrNull { it.name == c.title }?.keepInTouchDays ?: 30} days" to { store.toast("Opens ${c.title}'s cadence in LoR") })
                add("Open in LoR" to { store.launch("LoR: ${c.title}") })
                add("History" to { store.toast("Opens ${c.title}'s history in LoR") })
            }
            entries.forEachIndexed { i, (label, act) ->
                if (i > 0) Hairline()
                Row(Modifier.fillMaxWidth().clickable { act() }.heightIn(min = 48.dp).padding(horizontal = 14.dp).labAnchor("about:$label"), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(label, Modifier.weight(1f), style = C.meta.copy(color = C.text1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Glyph.Chevron, C.text3, size = 14.dp)
                }
            }
        }
    }
}

// ---- Beside the context ----

/** The next fixed event and the gap before it, and what fits there. Flat on the ground; a tap turns Now to it. */
@Composable
private fun NextContext(store: ConceptStore, ui: CtxUi, here: Ctx) {
    val now = liveNow(store)
    val next = ui.model.next(now)
    Column(Modifier.fillMaxWidth().clip(C.rLg).background(C.ground2).border(1.dp, C.line, C.rLg).padding(16.dp).labAnchor("next")) {
        Label("Next context", null, C.cyan)
        Spacer(Modifier.height(C.s3))
        if (next == null) {
            BasicText(if (store.home == null) "The calendar shows once LoR answers." else "Nothing else fixed today.", style = C.body.copy(color = C.text2))
            return
        }
        val (e, gap) = next
        val slot = Ctx(Kind.SLOT, e.title, e)
        Row(Modifier.fillMaxWidth().clip(C.rMd).clickable { ui.switchTo(slot) }.heightIn(min = 52.dp).labAnchor("next:open"), verticalAlignment = Alignment.CenterVertically) {
            BasicText(LocalClock.time(e.start, store.zone), style = C.num.copy(fontSize = C.head.fontSize, color = C.cyan))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                BasicText(e.title, style = C.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("in ${spoken(gap)} · until ${LocalClock.time(e.end, store.zone)}", style = C.meta)
            }
            Icon(Glyph.Chevron, C.text3, size = 16.dp)
        }
        if (here.kind == Kind.SLOT && here.title == e.title) return
        val fits = ui.model.fits(here, gap)
        Hairline(Modifier.padding(vertical = 10.dp))
        BasicText("${spoken(gap)} free before it. Nothing is put there unasked.", style = C.meta)
        if (fits.isNotEmpty()) {
            Spacer(Modifier.height(C.s3))
            Label("Fits before it")
            fits.forEach { (t, min) ->
                Row(
                    Modifier.fillMaxWidth().clip(C.rSm).clickable { ui.switchTo(ui.model.homeOf(t)) { ui.expanded = t.id } }.heightIn(min = 44.dp).padding(horizontal = 2.dp).labAnchor("fits:${t.title}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(t.title, Modifier.weight(1f), style = C.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Chip("$min min", C.text2)
                }
            }
        }
    }
}

/** Something waiting outside this context: a flag with Done and Seen, or a task due with a check; its context opens it. */
@Composable
private fun ElsewhereRow(store: ConceptStore, ui: CtxUi, e: Elsewhere) {
    val done = e.task?.let { store.done[it.id] == true } == true
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(C.rMd).labAnchor("else:${e.title}").padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        if (e.flag) Box(Modifier.size(48.dp, 30.dp), contentAlignment = Alignment.Center) { Icon(Glyph.Flag, C.amber.copy(alpha = 0.75f), size = 16.dp) }
        else Check(done, { if (ui.saves()) e.task?.let { if (done) store.untick(it.id) else store.tick(it.id) } }, anchor = "tick:${e.title}", enabled = store.lorLive)
        Column(Modifier.weight(1f).padding(top = if (e.flag) 4.dp else 12.dp, end = 4.dp)) {
            TaskTitle(e.title, done, style = C.body.copy(color = C.text2))
            e.note?.let { BasicText(it, style = C.meta, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Row(Modifier.clip(C.rXs).clickable { ui.switchTo(e.home) { e.task?.let { ui.expanded = it.id } } }.padding(vertical = 6.dp).labAnchor("else:home:${e.title}"), verticalAlignment = Alignment.CenterVertically) {
                Icon(glyphOf(e.home.kind), C.text3, size = 12.dp)
                Spacer(Modifier.width(6.dp))
                BasicText(listOfNotNull(e.home.title, e.task?.takeIf { !e.flag }?.let { dueWords(it, store) }).joinToString(" · "), style = C.meta.copy(color = if (e.task?.overdue == true && !e.flag) C.red else C.text3), maxLines = 1)
            }
            if (e.flag) {
                val id = e.key.removePrefix("flag:")
                Row(horizontalArrangement = Arrangement.spacedBy(C.s2), modifier = Modifier.padding(top = 4.dp)) {
                    CButton("Done", { if (ui.saves()) store.flagDone(id) }, kind = Tone.QUIET, glyph = Glyph.Check, compact = true, anchor = "done:${e.key}", enabled = store.lorLive)
                    CButton("Seen", { if (ui.saves()) store.seen(id) }, kind = Tone.QUIET, compact = true, anchor = "seen:${e.key}", enabled = store.lorLive)
                }
            }
        }
    }
}

// ---- Pieces ----

/** A region of the surface: its tone, its tracked label and count, its rows. */
@Composable
private fun Region(title: String, count: Int?, tone: Color = C.text3, anchor: String, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(C.regionGap - 8.dp))
    Column(Modifier.fillMaxWidth().clip(C.rRegion).background(if (tone == C.cyan) C.cyanFaint else C.region).border(1.dp, if (tone == C.cyan) C.cyanEdge.copy(alpha = 0.18f) else C.line, C.rRegion).padding(10.dp).labAnchor(anchor)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 32.dp).padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(title, count, tone, Modifier.weight(1f))
            action?.invoke()
        }
        content()
    }
}

/** A tracked label with a count. */
@Composable
fun Label(text: String, count: Int? = null, tone: Color = C.text3, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BasicText(text.uppercase(), style = C.label.copy(color = tone))
        if (count != null && count > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(C.rPill).background(if (tone == C.text3) C.region3 else tone.copy(alpha = 0.16f)).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                BasicText("$count", style = C.meta.copy(color = if (tone == C.text3) C.text2 else tone, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
fun Chip(text: String, tone: Color) {
    BasicText(text, Modifier.clip(C.rChip).background(tone.copy(alpha = 0.13f)).padding(horizontal = 8.dp, vertical = 2.dp), style = C.meta.copy(color = tone, fontWeight = FontWeight.Medium), maxLines = 1)
}

@Composable
fun Avatar(name: String, size: Int) {
    val initials = name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(size.dp).clip(CircleShape).background(C.region3).border(1.dp, C.line2, CircleShape), contentAlignment = Alignment.Center) {
        BasicText(initials, style = C.meta.copy(color = C.text1, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
    }
}

fun dueWords(t: LorTasks.Task, store: ConceptStore): String {
    val due = t.due ?: return "no date"
    val days = (store.today.toEpochDays() - due.toEpochDays()).toInt()
    return when {
        days == 0 -> "due today"
        days == 1 -> "due yesterday"
        days > 1 -> "$days days late"
        days == -1 -> "due tomorrow"
        days > -7 -> "due ${due.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
        else -> "due ${due.dayOfMonth} ${due.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
    }
}

// ---- The right half, unfolded; and the same on the cover in a sheet ----

/** Unfolded, beside the context: what is open, or with nothing open, the context's page (or what it gave the agent). */
@Composable
private fun RightPane(store: ConceptStore, ui: CtxUi) {
    val (ctx, _) = ui.shown()
    val key = ui.open
    AnimatedContent(
        key ?: "rest:${ctx.key}", Modifier.fillMaxSize(),
        transitionSpec = { (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 }) togetherWith fadeOut(tween(140)) },
        label = "right",
    ) { k ->
        Box(
            Modifier.fillMaxSize().shadow(18.dp, C.rSurface, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(C.rSurface).background(if (k.startsWith("rest:")) C.ground2 else C.surface).border(1.dp, if (k.startsWith("rest:")) C.line else C.surfaceEdge, C.rSurface)
                .labAnchor(if (k.startsWith("rest:")) "pane:rest" else "pane:detail"),
        ) {
            if (k.startsWith("rest:")) {
                val page = ui.model.pageOf(ctx)
                if (page != null) PageView(ui, ctx.title, page, inSheet = false, resting = true)
                else Column(Modifier.fillMaxSize().padding(28.dp)) {
                    Label(ctx.title)
                    Spacer(Modifier.height(C.s3))
                    BasicText("No page in Notes for this ${ctx.kind.word.lowercase()}.", style = C.body.copy(color = C.text2))
                    BasicText("Open a thread, a person or a page on the left to read it here.", style = C.meta.copy(color = C.text3))
                }
            } else Detail(store, ui, k, inSheet = false)
        }
    }
}

/** What an open key shows: a page, a person, or a card's or thread's transcript. */
@Composable
fun Detail(store: ConceptStore, ui: CtxUi, key: String, inSheet: Boolean) {
    when {
        key.startsWith("page:") -> { val t = key.removePrefix("page:"); PAGES[t]?.let { PageView(ui, t, it, inSheet) } }
        key.startsWith("person:") -> PersonView(store, ui, key.removePrefix("person:"), inSheet)
        else -> ThreadDetail(store, ui, key, inSheet)
    }
}

/** On the cover, what Now opens (a thread, a page, a person) rises as a sheet over it. */
@Composable
fun OpenSheet(store: ConceptStore, ui: CtxUi, wide: Boolean) {
    val key = ui.open
    var shown by remember { androidx.compose.runtime.mutableStateOf(key) }
    if (key != null) shown = key
    Box(Modifier.fillMaxSize().padding(bottom = if (ui.keyboard) KeyboardHeight else 0.dp)) {
        KitSheet(key != null && !wide && ui.shell.pager.currentPage == CtxUi.NOW, { ui.open = null }, "sheet:open", detents = listOf(0.93f)) {
            shown?.let { Detail(store, ui, it, inSheet = true) }
        }
    }
}

@Composable
private fun PageView(ui: CtxUi, title: String, p: Page, inSheet: Boolean, resting: Boolean = false) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = if (inSheet) 0.dp else 24.dp, bottom = 24.dp).labAnchor("page").labAnchor("page:$title")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(if (resting) "Its page" else "Page", tone = C.text3, modifier = Modifier.weight(1f))
            if (!inSheet && !resting) IconButton(Glyph.Close, "Close", { ui.open = null }, anchor = "pane:close", size = 40.dp)
        }
        Spacer(Modifier.height(6.dp))
        BasicText(title, style = if (inSheet) C.head else C.ctx)
        BasicText("Notes · ${p.slug} · edited ${p.edited}", style = C.meta.copy(color = C.text3))
        Spacer(Modifier.height(C.s5))
        BasicText(p.intro, style = C.body)
        p.sections.forEach { (h, lines) ->
            Spacer(Modifier.height(C.s5))
            BasicText(h.uppercase(), style = C.label)
            Spacer(Modifier.height(6.dp))
            lines.forEach { l ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    BasicText("•", Modifier.width(18.dp), style = C.body.copy(color = C.text3))
                    BasicText(l, style = C.body.copy(color = C.text2))
                }
            }
        }
        Spacer(Modifier.height(C.s5))
        CButton("Edit in LoR", { ui.store.launch("LoR: ${p.slug}") }, glyph = Glyph.Chevron, compact = true)
    }
}

@Composable
private fun PersonView(store: ConceptStore, ui: CtxUi, name: String, inSheet: Boolean) {
    val p = store.people?.let { it.overdue + it.birthdays }?.firstOrNull { it.name == name }
    val tasks = ui.model.tasksOf(Ctx(Kind.PERSON, name))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = if (inSheet) 0.dp else 24.dp, bottom = 24.dp).labAnchor("person-pane")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, 48)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Label("Person")
                BasicText(name, style = C.head)
            }
            if (!inSheet) IconButton(Glyph.Close, "Close", { ui.open = null }, anchor = "pane:close", size = 40.dp)
        }
        Spacer(Modifier.height(C.s4))
        BasicText(
            listOfNotNull(
                p?.lastAt?.let { "Last in touch ${ago(it, store.now)}" } ?: p?.let { "No contact recorded" },
                p?.overdueBy?.let { "$it days past the cadence" },
                p?.birthdayIn?.let { "birthday in $it days" },
            ).joinToString(" · ").ifEmpty { "Named in this context." },
            style = C.meta,
        )
        Spacer(Modifier.height(C.s4))
        Row(horizontalArrangement = Arrangement.spacedBy(C.s2)) {
            CButton("Message", { store.launch("Messages: $name") }, kind = Tone.PRIMARY, compact = true, anchor = "person:message")
            CButton("Open as context", { ui.open = null; ui.switchTo(Ctx(Kind.PERSON, name)) }, glyph = Glyph.Person, compact = true, anchor = "person:context")
        }
        if (tasks.isNotEmpty()) {
            Spacer(Modifier.height(C.s5))
            Label("Tasks that name them", tasks.size)
            tasks.forEach { t -> BasicText(t.title, Modifier.padding(vertical = 6.dp), style = C.body) }
        }
    }
}

/** What is wrong, where he is already looking: a failed sync, LoR not answering, T3 away. */
@Composable
fun Notes(store: ConceptStore, ui: CtxUi) {
    val notes = buildList<@Composable () -> Unit> {
        if (store.syncFailed) add { Note("Phone sync failed", "${store.sync.pending} readings waiting · ${store.sync.errors.firstOrNull().orEmpty()}", C.red, Glyph.Warning, action = "Fix", onAction = { ui.sheet = Sheet.Settings("sync") }, anchor = "note:sync") }
        when (store.homeLoad) {
            Load.STALE -> add { Note("LoR isn't answering", "This is as of ${ago(store.homeReadAt, store.now)}. Changes wait until it's back.", C.red, Glyph.Sync, anchor = "note:lor") }
            Load.FAILED -> add { Note("Can't reach LoR", store.homeError ?: "No answer", C.red, Glyph.Warning, action = "Settings", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:lor") }
            else -> {}
        }
        when (store.t3Status) {
            com.spadaval.lor.lab.T3Status.RETRYING -> add { Note("T3 is reconnecting", "Its cards as of ${ago(store.t3SeenAt, store.now)}. Answers wait until it's back.", C.red, Glyph.Sync, anchor = "note:t3") }
            com.spadaval.lor.lab.T3Status.CONNECTING, com.spadaval.lor.lab.T3Status.STOPPED -> add { Note("Connecting to T3…", null, C.text2, Glyph.Sync, anchor = "note:t3") }
            com.spadaval.lor.lab.T3Status.REJECTED -> add { Note("T3 refused this phone", store.t3Error, C.red, Glyph.Warning, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3") }
            com.spadaval.lor.lab.T3Status.NOT_PAIRED -> add { Note("Not paired with T3", "Pair to see approvals and questions here", C.text2, Glyph.Agent, action = "Pair", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:t3") }
            com.spadaval.lor.lab.T3Status.LIVE -> {}
        }
    }
    if (notes.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(C.s2)) { notes.forEach { it() } }
}

/** Nothing waits: said calmly, and only when both sources were read. */
@Composable
fun Calm(store: ConceptStore) {
    val knows = store.knowsLor() && store.knowsT3()
    Row(Modifier.fillMaxWidth().clip(C.rLg).background(C.ground2).border(1.dp, C.line, C.rLg).padding(14.dp).labAnchor(if (knows) "calm" else "unknown"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(if (knows) C.greenSoft else C.region3), contentAlignment = Alignment.Center) {
            Icon(if (knows) Glyph.Check else Glyph.Sync, if (knows) C.green else C.text2, size = 16.dp, weight = 2f)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(if (knows) "Nothing waits on you" else "Can't tell yet", style = C.rowTitle)
            BasicText(if (knows) "No approvals, questions or flags" else if (!store.knowsLor()) "What waits shows once LoR answers" else "T3's cards show once it's connected", style = C.meta)
        }
    }
}
