package com.spadaval.lor.concepts.panes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorPeople
import com.spadaval.lor.lor.LorTasks
import com.spadaval.lor.ui.LocalClock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.coroutines.launch

/*
 * Today, right of home: the LoR panel. Its list leads with the timer as a row (its readout, Switch
 * and Stop), then what LoR flagged with Done and Seen on the row, the tasks due with a check to
 * tick, and the people past their cadence. A row opens beside the list unfolded, or in a sheet at
 * half height on the cover, to change what the row can't: a due date, a timer on it, a person's
 * cadence. Uncommon things are under the common ones, collapsed.
 */
@Composable
fun TodayPage(store: ConceptStore, ui: PanesUi, wide: Boolean) {
    val flags = store.home?.needsYou.orEmpty()
    val due = store.due()
    val people = store.peopleDue()
    PanelPage(
        store, ui, PanesUi.TODAY, wide, "today",
        head = {
            val now = liveNow(store)
            PaneHead("Today", store.todayCount(), "${LocalClock.day(now, store.zone)} · ${LocalClock.time(now, store.zone)}", countTone = P.amber) {
                IconButton(Glyph.Plus, "Add a task", { ui.addFromSearch() }, anchor = "today:add")
                MoreButton(ui, { Menu.Page(PanesUi.TODAY, it) }, "today:more")
            }
        },
        overview = { TodayOverview(store, wide) },
        detail = { key, inSheet -> TodayDetail(store, ui, key, inSheet) },
    ) {
        item(key = "notes") { Column(Modifier.padding(horizontal = 6.dp)) { TodayNotes(store, ui) } }
        item(key = "timer") { TimerRow(store, ui) }
        item(key = "needs-label") { GroupLabel("Needs you", flags.size) }
        if (flags.isEmpty() && due.isEmpty()) item(key = "calm") { Calm(store, t3 = false) }
        items(flags, key = { "flag:${it.id}" }) { FlagRow(store, ui, it) }
        if (due.isNotEmpty()) {
            item(key = "due-label") { GroupLabel("Due", due.count { store.done[it.id] != true }) }
            items(due, key = { "task:${it.id}" }) { TaskRow(store, ui, it) }
        }
        if (people.isNotEmpty()) {
            item(key = "people-label") { GroupLabel("People", people.size) }
            items(people, key = { "person:${it.id}" }) { PersonRow(store, ui, it) }
        }
    }
}

/** Opens search ready to add: the field reads a date or #goal as a task to add. */
fun PanesUi.addFromSearch() {
    focusSearch = true
    search.edit { replace(0, length, "") }
    sheet = null
    scope.launch { shell.goTo(PanesUi.HOME); shell.openDrawer() }
}

@Composable
private fun TodayNotes(store: ConceptStore, ui: PanesUi) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(P.s2)) {
        if (store.syncFailed) SyncNote(store, ui)
        when (store.homeLoad) {
            Load.STALE -> Note("LoR isn't answering", "Showing what it said ${ago(store.homeReadAt, store.now)}. Changes wait until it's back.", P.red, Glyph.Sync, anchor = "note:lor")
            Load.FAILED -> Note("Can't reach LoR", store.homeError ?: "No answer", P.red, Glyph.Warning, action = "Settings", onAction = { ui.sheet = Sheet.Settings("connections") }, anchor = "note:lor")
            Load.LOADING -> Note("Reading LoR…", null, P.text2, Glyph.Sync, anchor = "note:lor")
            Load.OK -> {}
        }
    }
}

/** The timer as Today's first row: running, an amber card with its readout, Switch and Stop; idle, Start. */
@Composable
private fun TimerRow(store: ConceptStore, ui: PanesUi) {
    val running = store.timer
    val now = liveNow(store)
    Box(Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp)) {
        if (running != null) Column(
            Modifier.fillMaxWidth().clip(P.rMd).background(Color(0x332A1A0B)).border(1.dp, P.amberEdge, P.rMd)
                .clickable { ui.sheet = Sheet.Timer }.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 10.dp).labAnchor("today:timer"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(P.amber))
                Spacer(Modifier.width(10.dp))
                BasicText(running.label ?: "No label", Modifier.weight(1f), style = P.rowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(readout(now - running.start), Modifier.labAnchor("today:readout"), style = P.head.copy(color = P.amber))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("since ${LocalClock.time(running.start, store.zone)}", Modifier.weight(1f).padding(start = 18.dp), style = P.meta)
                PButton("Switch", { ui.switching = true; ui.sheet = Sheet.Timer }, glyph = Glyph.Swap, compact = true, anchor = "today:switch", enabled = store.timerOk())
                Spacer(Modifier.width(P.s2))
                PButton("Stop", { if (ui.saves()) store.stop() }, glyph = Glyph.Stop, compact = true, anchor = "today:stop", enabled = store.timerOk())
            }
        } else Row(
            Modifier.fillMaxWidth().clip(P.rMd).background(P.raise).border(1.dp, P.line, P.rMd).heightIn(min = P.row).padding(start = 16.dp, end = 8.dp).labAnchor("today:timer"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).border(1.5.dp, P.text3, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                BasicText(if (store.timerOk()) "No timer running" else "Timer unavailable", style = P.body.copy(color = P.text2))
                if (!store.timerOk()) BasicText("LoR isn't answering", style = P.meta)
            }
            PButton("Start", { ui.sheet = Sheet.Timer }, glyph = Glyph.Play, compact = true, anchor = "today:start", enabled = store.timerOk())
        }
    }
}

/** A flag: the agent's mark, its title and note, and Done and Seen on the row. */
@Composable
private fun FlagRow(store: ConceptStore, ui: PanesUi, need: LorHome.Need) {
    val key = "flag:${need.id}"
    Row(
        Modifier.paneRow(ui.openToday == key, key) { ui.openToday = key }.padding(start = 6.dp, end = 4.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(36.dp, 26.dp), contentAlignment = Alignment.Center) { Icon(Glyph.Flag, P.cyan, size = 18.dp) }
        Column(Modifier.weight(1f).padding(end = 6.dp)) {
            BasicText(need.title, style = P.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            need.note?.let { BasicText(it, style = P.small, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(P.s2)) {
                PButton("Done", { if (ui.saves()) store.flagDone(need.id) }, glyph = Glyph.Check, compact = true, anchor = "done:$key", enabled = store.lorLive)
                PButton("Seen", { if (ui.saves()) store.seen(need.id) }, kind = Kind.QUIET, compact = true, anchor = "seen:$key", enabled = store.lorLive)
            }
        }
        MoreButton(ui, { Menu.Row(key, it) }, "more:$key")
    }
}

/** A task due: a check to tick it, its title, and when it was due (late in red). */
@Composable
private fun TaskRow(store: ConceptStore, ui: PanesUi, t: LorTasks.Task) {
    val key = "task:${t.id}"
    val done = store.done[t.id] == true
    Row(
        Modifier.paneRow(ui.openToday == key, key) { ui.openToday = key }.heightIn(min = P.row).padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Check(done, { if (ui.saves()) { if (done) store.untick(t.id) else store.tick(t.id) } }, anchor = "tick:$key", enabled = store.lorLive)
        Column(Modifier.weight(1f)) {
            TaskTitle(t.title, done)
            BasicText(dueWords(t, store), style = P.meta.copy(color = if (t.overdue && !done) P.red else P.text3))
        }
        Icon(Glyph.Chevron, P.text3, size = 16.dp)
    }
}

/** A person to get back to: their initial, their name, and how long it has been. */
@Composable
private fun PersonRow(store: ConceptStore, ui: PanesUi, p: LorPeople.Person) {
    val key = "person:${p.id}"
    Row(
        Modifier.paneRow(ui.openToday == key, key) { ui.openToday = key }.heightIn(min = P.row).padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(p.name, 36)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(p.name, style = P.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(personLine(p, store), style = P.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Glyph.Chevron, P.text3, size = 16.dp)
    }
}

@Composable
fun Avatar(name: String, size: Int) {
    val initials = name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(size.dp).clip(CircleShape).background(P.raise2).border(1.dp, P.line2, CircleShape), contentAlignment = Alignment.Center) {
        BasicText(initials, style = P.meta.copy(color = P.text1, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
    }
}

private fun personLine(p: LorPeople.Person, store: ConceptStore): String = listOfNotNull(
    p.birthdayIn?.let { if (it == 0) "Birthday today" else "Birthday in $it days" + (p.turning?.let { t -> ", turning $t" } ?: "") },
    p.overdueBy?.takeIf { it > 0 }?.let { "$it days past" },
    p.lastAt?.let { "last ${ago(it, store.now)}${p.lastVia?.let { v -> " by $v" } ?: ""}" },
).joinToString(" · ").ifEmpty { p.kind }

fun dueWords(t: LorTasks.Task, store: ConceptStore): String {
    val due = t.due ?: return "No date"
    val days = (store.today.toEpochDays() - due.toEpochDays()).toInt()
    return when {
        days == 0 -> "today"
        days == 1 -> "yesterday"
        days > 1 -> "$days days late"
        days == -1 -> "tomorrow"
        else -> "in ${-days} days"
    }
}

/** The detail side with nothing open: the day so far, quietly. On the cover it ends the list. */
@Composable
private fun TodayOverview(store: ConceptStore, wide: Boolean) {
    val now = liveNow(store)
    val day = store.home?.day
    Column(Modifier.fillMaxWidth().padding(if (wide) 24.dp else 12.dp).labAnchor("today:overview")) {
        if (wide) { BasicText("THE DAY", style = P.paneTitle.copy(color = P.text2, fontSize = P.meta.fontSize)); Spacer(Modifier.height(P.s3)) }
        else GroupLabel("The day", null, Modifier.padding(start = 0.dp))
        if (day == null) { BasicText("The day shows once LoR answers.", style = P.small); return@Column }
        Row(horizontalArrangement = Arrangement.spacedBy(P.s2)) {
            val left = (day.wakeEnd - now).takeIf { it.isPositive() }
            Stat(left?.let { spoken(it) } ?: "—", "awake left", Modifier.weight(1f))
            Stat("${day.events.count { it.end > now }}", "events to come", Modifier.weight(1f))
            Stat("${store.done.size}", "done here", Modifier.weight(1f))
        }
        Spacer(Modifier.height(P.s4))
        BasicText("Calendar", style = P.group)
        Spacer(Modifier.height(P.s1))
        if (day.events.isEmpty()) BasicText("Nothing on the calendar", style = P.small)
        day.events.forEach { e ->
            val past = e.end <= now
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(LocalClock.time(e.start, store.zone), Modifier.width(52.dp), style = P.meta.copy(color = if (past) P.text3 else P.cyan))
                BasicText(e.title, Modifier.weight(1f), style = P.small.copy(color = if (past) P.text3 else P.text1), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (wide) { Spacer(Modifier.height(P.s5)); BasicText("Open a task, a flag or a person on the left to see it here.", style = P.meta.copy(color = P.text3)) }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(P.rMd).background(P.raise).border(1.dp, P.line, P.rMd).padding(horizontal = 12.dp, vertical = 10.dp)) {
        BasicText(value, style = P.head.copy(fontWeight = FontWeight.Medium), maxLines = 1)
        BasicText(label, style = P.meta, maxLines = 1)
    }
}

/**
 * An open flag, task or person: what it is, its title, its main action, then the fields the row
 * can't change, and under them, collapsed, the uncommon things.
 */
@Composable
fun TodayDetail(store: ConceptStore, ui: PanesUi, key: String, inSheet: Boolean) {
    val row = store.todayRow(key)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = if (inSheet) 22.dp else 10.dp, top = if (inSheet) 0.dp else 18.dp, bottom = 24.dp)
            .labAnchor("detail").labAnchor("detail:$key"),
    ) {
        @Composable
        fun header(kicker: String, tone: Color, title: String, done: Boolean = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Kicker(kicker, tone, Modifier.weight(1f))
                if (!inSheet) IconButton(Glyph.Close, "Close", { ui.openToday = null }, anchor = "pane:close", size = 40.dp)
            }
            Spacer(Modifier.height(6.dp))
            TaskTitle(title, done, maxLines = 4, style = if (inSheet) P.head else P.title)
            Spacer(Modifier.height(P.s4))
        }
        when (row) {
            null -> BasicText("This one has been cleared.", style = P.body.copy(color = P.text2))
            is TodayRow.Flag -> {
                header("Flagged by the agent", P.cyan, row.need.title)
                Row(horizontalArrangement = Arrangement.spacedBy(P.s2)) {
                    PButton("Done", { if (ui.saves()) store.flagDone(row.need.id) }, Modifier.weight(1f), kind = Kind.PRIMARY, glyph = Glyph.Check, anchor = "detail:done", enabled = store.lorLive)
                    PButton("Seen", { if (ui.saves()) store.seen(row.need.id) }, Modifier.weight(1f), anchor = "detail:seen", enabled = store.lorLive)
                }
                row.need.note?.let { note ->
                    Spacer(Modifier.height(P.s4))
                    Row(Modifier.fillMaxWidth().clip(P.rMd).background(P.cyanSoft).border(1.dp, P.cyanEdge, P.rMd).padding(14.dp)) {
                        Icon(Glyph.Flag, P.cyan, size = 16.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            BasicText(note, style = P.body)
                            BasicText("From the agent", style = P.meta.copy(color = P.text3))
                        }
                    }
                }
                val task = store.tasks.firstOrNull { it.id == row.need.id }
                DueField(store, ui, row.need.id, task?.due)
                TimerOn(store, ui, row.need.title)
                More(ui, key, listOf("Open in LoR" to { store.toast("Opens the task in LoR") }, "Copy a link" to { store.toast("Link copied") }))
            }
            is TodayRow.Task -> {
                val t = row.task
                val done = store.done[t.id] == true
                header(if (t.overdue && !done) "Task · ${dueWords(t, store)}" else "Task", if (t.overdue && !done) P.red else P.text3, t.title, done)
                PButton(if (done) "Not done" else "Done", { if (ui.saves()) { if (done) store.untick(t.id) else store.tick(t.id) } }, Modifier.fillMaxWidth(), kind = if (done) Kind.PLAIN else Kind.PRIMARY, glyph = Glyph.Check, anchor = "detail:tick", enabled = store.lorLive)
                DueField(store, ui, t.id, t.due)
                TimerOn(store, ui, t.title)
                More(ui, key, listOfNotNull(
                    "Open in LoR" to { store.toast("Opens the task in LoR") },
                    "Waiting on someone…" to { store.toast("Opens the task in LoR to say who") },
                    "Delete…" to { store.toast("Deleting asks in LoR first") },
                ))
            }
            is TodayRow.Person -> {
                val p = row.person
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p.name, if (inSheet) 44 else 56)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Kicker("Person · ${p.kind}", P.text3)
                        BasicText(p.name, style = if (inSheet) P.head else P.title, maxLines = 2)
                    }
                    if (!inSheet) IconButton(Glyph.Close, "Close", { ui.openToday = null }, anchor = "pane:close", size = 40.dp)
                }
                Spacer(Modifier.height(P.s4))
                Row(horizontalArrangement = Arrangement.spacedBy(P.s2)) {
                    PButton("Call", { store.launch("Phone: ${p.name}") }, Modifier.weight(1f), kind = Kind.PRIMARY, glyph = Glyph.Phone, anchor = "detail:call")
                    PButton("Message", { store.launch("WhatsApp: ${p.name}") }, Modifier.weight(1f), glyph = Glyph.Send, anchor = "detail:message")
                }
                Spacer(Modifier.height(P.s4))
                Facts(listOfNotNull(
                    "Last talked" to (p.lastAt?.let { "${ago(it, store.now)}${p.lastVia?.let { v -> " · $v" } ?: ""}" } ?: "Not recorded"),
                    p.keepInTouchDays?.let { "Keep in touch" to "every $it days" },
                    p.overdueBy?.takeIf { it > 0 }?.let { "Past that by" to "$it days" },
                    p.birthdayIn?.let { "Birthday" to (if (it == 0) "today" else "in $it days") + (p.turning?.let { t -> ", turning $t" } ?: "") },
                ))
                More(ui, key, listOf("Change how often" to { store.toast("Opens ${p.name} in LoR") }, "Open in LoR" to { store.toast("Opens ${p.name} in LoR") }))
            }
        }
    }
}

/** The due date as the dashboard sets it: Today, Tomorrow, Friday, Next week, or none, one tap each, with Undo. */
@Composable
private fun DueField(store: ConceptStore, ui: PanesUi, id: String, due: LocalDate?) {
    val today = store.today
    fun next(day: DayOfWeek): LocalDate { var d = today.plus(DatePeriod(days = 1)); while (d.dayOfWeek != day) d = d.plus(DatePeriod(days = 1)); return d }
    val choices = listOf("Today" to today, "Tomorrow" to today.plus(DatePeriod(days = 1)), "Fri" to next(DayOfWeek.FRIDAY), "Mon" to next(DayOfWeek.MONDAY), "None" to null)
    Spacer(Modifier.height(P.s5))
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText("Due", Modifier.weight(1f), style = P.group)
        BasicText(due?.let { "${it.dayOfWeek.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.dayOfMonth} ${it.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }}" } ?: "No date", Modifier.labAnchor("due:is:${due ?: "none"}"), style = P.meta)
    }
    Spacer(Modifier.height(P.s2))
    val picked = choices.firstOrNull { it.second == due }?.second
    Segmented(choices, picked, { d -> if (ui.saves()) store.setDue(id, d) }, Modifier.fillMaxWidth(), anchor = "due", enabled = store.lorLive)
}

/** A timer on this item: the one timer, started here or switched to it. */
@Composable
private fun TimerOn(store: ConceptStore, ui: PanesUi, title: String) {
    Spacer(Modifier.height(P.s4))
    val on = store.timer?.label == title
    PButton(
        if (on) "Timer running on this" else if (store.timer != null) "Switch the timer to this" else "Start a timer on this",
        { if (!on && ui.saves()) { if (store.timer != null) store.switch(title) else store.start(title) } },
        Modifier.fillMaxWidth(), glyph = if (on) Glyph.Clock else Glyph.Play, enabled = store.timerOk() && !on, anchor = "detail:timer",
    )
}

@Composable
private fun Facts(rows: List<Pair<String, String>>) {
    Column(Modifier.fillMaxWidth().clip(P.rMd).background(P.raise).border(1.dp, P.line, P.rMd).padding(horizontal = 14.dp)) {
        rows.forEachIndexed { i, (k, v) ->
            if (i > 0) Hairline()
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(k, Modifier.weight(1f), style = P.small)
                BasicText(v, style = P.small.copy(color = P.text1))
            }
        }
    }
}

/** The uncommon things, under the common ones, collapsed until asked for. */
@Composable
fun More(ui: PanesUi, key: String, entries: List<Pair<String, () -> Unit>>) {
    val open = ui.more[key] == true
    Spacer(Modifier.height(P.s4))
    Row(
        Modifier.fillMaxWidth().clip(P.rSm).clickable { ui.more[key] = !open }.heightIn(min = 44.dp).padding(horizontal = 4.dp).labAnchor("detail:more"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText("More", Modifier.weight(1f), style = P.small.copy(fontWeight = FontWeight.Medium))
        Icon(if (open) Glyph.ChevronUp else Glyph.ChevronDown, P.text2, size = 16.dp)
    }
    AnimatedVisibility(open, enter = expandVertically(tween(200)) + fadeIn(tween(200)), exit = shrinkVertically(tween(160)) + fadeOut(tween(160))) {
        Column(Modifier.fillMaxWidth().clip(P.rMd).background(P.raise).border(1.dp, P.line, P.rMd)) {
            entries.forEachIndexed { i, (label, act) ->
                if (i > 0) Hairline()
                Row(Modifier.fillMaxWidth().clickable { act() }.heightIn(min = 48.dp).padding(horizontal = 14.dp).labAnchor("more:$label"), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(label, Modifier.weight(1f), style = P.small.copy(color = if (label.startsWith("Delete")) P.red else P.text1))
                    Icon(Glyph.Chevron, P.text3, size = 14.dp)
                }
            }
        }
    }
}
