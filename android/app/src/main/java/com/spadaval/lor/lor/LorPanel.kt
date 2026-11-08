package com.spadaval.lor.lor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.agent.AgentModel
import com.spadaval.lor.agent.Body
import com.spadaval.lor.agent.Clock
import com.spadaval.lor.agent.LorFlagCard
import com.spadaval.lor.agent.More
import com.spadaval.lor.agent.ScrollColumn
import com.spadaval.lor.agent.SectionHeader
import com.spadaval.lor.agent.Small
import com.spadaval.lor.agent.Today
import com.spadaval.lor.agent.Unreachable
import com.spadaval.lor.agent.Unseen
import com.spadaval.lor.agent.rememberAgentModel
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.home.cellInsetsPadding
import com.spadaval.lor.net.DeviceTasks
import com.spadaval.lor.net.LorReads
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Veil
import com.spadaval.lor.ui.selection
import java.time.Instant
import java.time.ZoneId

/*
 * The `lor` panel (lor-lz97, decision lor-p86v): LoR's dashboard shaped for the phone, in native
 * Compose. Narrow, one column: Today, LoR's Needs you with the agent's queue under it, and Due
 * soon. Wide (600 dp and up, the Fold's inner screen), that column without Due soon on the left,
 * and on the right the Tasks and People tabs. Rows only read: a tap on a row's text opens its
 * place in the dashboard or T3 (the writes are lor-rz50's).
 */

/** The wide form's tabs. */
enum class LorTab(val label: String) { TASKS("Tasks"), PEOPLE("People") }

/**
 * The tab the wide form shows: screen state, kept in memory for the process and never written to
 * `layout.json` (decision lor-p86v), so a fold and unfold, or a move away and back, keep it.
 */
object LorPanelState {
    var tab by mutableStateOf(LorTab.TASKS)
}

/** The `lor` panel. While composed and home is started, it holds LoR's reader with the lists. */
@Composable
fun LorPanel() {
    val model = rememberAgentModel(t3 = false, lists = true)
    val narrow = rememberScrollState()
    val left = rememberScrollState()
    val tasks = rememberScrollState()
    val people = rememberScrollState()
    // The agent page's veil, so the text reads on any wallpaper.
    BoxWithConstraints(Modifier.fillMaxSize().background(Veil).cellInsetsPadding()) {
        if (maxWidth < 600.dp) {
            ScrollColumn(narrow, Modifier.fillMaxWidth(), bottom = 56.dp) {
                Notice(model)
                Today(model)
                LorNeedsYou(model)
                AgentTasks(model)
                DueSoon(model)
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                ScrollColumn(left, Modifier.weight(1f), bottom = 56.dp) {
                    Notice(model)
                    Today(model)
                    LorNeedsYou(model)
                    AgentTasks(model)
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Tabs(Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp))
                    when (LorPanelState.tab) {
                        LorTab.TASKS -> ScrollColumn(tasks, Modifier.fillMaxWidth(), bottom = 56.dp) { TasksList(model) }
                        LorTab.PEOPLE -> ScrollColumn(people, Modifier.fillMaxWidth(), bottom = 56.dp) { PeopleList(model) }
                    }
                }
            }
        }
    }
}

/** Something the panel couldn't do outside a row, such as open the dashboard. */
@Composable
private fun Notice(model: AgentModel) {
    model.notice?.let { Text(it, style = Small.copy(color = Bad), modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
}

/** DESIGN.md's segmented control: text buttons divided by 1 dp Edge rules, the chosen one in the selection's amber. */
@Composable
private fun Tabs(modifier: Modifier) {
    Row(modifier.height(IntrinsicSize.Min).border(1.dp, Edge, RectangleShape)) {
        LorTab.entries.forEachIndexed { i, tab ->
            val chosen = LorPanelState.tab == tab
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(Edge))
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .then(if (chosen) Modifier.selection() else Modifier)
                    .semantics { selected = chosen }
                    .clickable(role = Role.Tab) { LorPanelState.tab = tab },
                contentAlignment = Alignment.Center,
            ) {
                Text(tab.label, style = Type.button.copy(color = if (chosen) Txt else Mute), modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

/** LoR's Needs you: the flagged tasks, oldest flag first, as the web's Today lists them. */
@Composable
internal fun LorNeedsYou(model: AgentModel) {
    val lor = model.lor
    val seen = lor != null && model.lorRead && model.lorError == null
    SectionHeader("Needs you", lor?.needsYou?.size?.takeIf { seen || it > 0 }?.toString())
    if (lor == null) {
        Unseen(if (model.lorError == null) "Reading LoR…" else "LoR can't be read, so its flags aren't here.")
        return
    }
    Column(Modifier.alpha(if (seen) 1f else 0.6f)) {
        lor.needsYou.forEach { key(it.id) { LorFlagCard(model, it) } }
        if (lor.needsYou.isEmpty()) Unseen("Nothing flagged. The agent will put things here when it needs you.")
    }
}

/**
 * LoR's work queue for the agent (`agent_queue`): tasks queued for the next run, oldest first,
 * then those a run is working in a T3 thread. A row's text opens its thread in T3, or the task in
 * the dashboard when no run has it. At most [max], then "N more" ([more]): the `t3-queue` block.
 */
@Composable
internal fun AgentTasks(model: AgentModel, max: Int? = null, more: (() -> Unit)? = null) {
    val lor = model.lor
    val seen = lor != null && model.lorRead && model.lorError == null
    SectionHeader("With the agent", lor?.agentQueue?.size?.takeIf { seen || it > 0 }?.toString())
    if (lor == null) {
        Unseen(if (model.lorError == null) "Reading LoR…" else "LoR can't be read, so the agent's queue isn't here.")
        return
    }
    val (shown, hidden) = LorRows.cap(lor.agentQueue, max)
    Column(Modifier.alpha(if (seen) 1f else 0.6f)) {
        shown.forEach { t ->
            key(t.id) {
                val thread = t.thread
                LorRow(t.title, LorRows.agentLine(t), mark = Cy, label = if (thread != null) "Open in T3" else "Open in LoR") {
                    if (thread != null) model.openThread(thread, LaunchFrom.AGENT_CARD) else model.openLor(LorRows.taskPath(t.id), LaunchFrom.AGENT_CARD)
                }
            }
        }
        if (hidden > 0) More(hidden, more)
        if (lor.agentQueue.isEmpty()) Unseen("Nothing is given to the agent.")
    }
}

/** Due soon, as the web's Today: the first six open tasks, then "All N tasks", which opens the dashboard's Tasks. */
@Composable
private fun DueSoon(model: AgentModel) {
    SectionHeader("Due soon")
    Lists(model, model.tasks) { t ->
        val rows = LorRows.dueSoon(t)
        if (rows.isEmpty()) Unseen("No open tasks.")
        rows.forEach { (task, done) -> key(task.id) { TaskRow(model, task, LorRows.dueSoonLine(task, done), done) } }
        Link("All ${t.open} tasks") { model.openLor("/tasks", LaunchFrom.AGENT_CARD) }
    }
}

/** The Tasks tab: the open tasks grouped by due date, as the web's Tasks groups them. */
@Composable
private fun TasksList(model: AgentModel) {
    Lists(model, model.tasks) { t ->
        val groups = LorRows.groups(t.tasks, t.today)
        if (groups.isEmpty()) Unseen("No open tasks.")
        groups.forEach { g ->
            GroupHead(g.title, g.tasks.size)
            g.tasks.forEach { key(it.id) { TaskRow(model, it, LorRows.taskLine(it, t.today)) } }
        }
        if (t.open > t.tasks.size) Unseen("The first ${t.tasks.size} of ${t.open} open tasks.")
        Link("Open Tasks in LoR") { model.openLor("/tasks", LaunchFrom.AGENT_CARD) }
    }
}

/** The People tab: who is past their keep-in-touch cadence, most overdue first, and birthdays within 30 days. */
@Composable
private fun PeopleList(model: AgentModel) {
    Lists(model, model.people) { p ->
        val now = Instant.now()
        GroupHead("Past their cadence", p.overdue.size)
        if (p.overdue.isEmpty()) Unseen("Nobody is past their cadence.")
        p.overdue.forEach {
            key("o" + it.id) { PersonRow(model, it.name, it.kind, listOfNotNull(LorRows.lastTouch(it, now), LorRows.overdueLine(it)).joinToString(" · "), it.id) }
        }
        GroupHead("Birthdays", p.birthdays.size)
        if (p.birthdays.isEmpty()) Unseen("No birthdays in the next 30 days.")
        p.birthdays.forEach { key("b" + it.id) { PersonRow(model, it.name, it.kind, LorRows.birthdayLine(it), it.id) } }
        Link("Open People in LoR") { model.openLor("/people", LaunchFrom.AGENT_CARD) }
    }
}

/**
 * One of the lists' replies: "Reading LoR…" until the first, then [content], greyed while it isn't
 * current, with why LoR can't be read and the time of what is shown (launcher polish 3.3's rule).
 */
@Composable
private fun <T> Lists(model: AgentModel, kept: LorReads.Kept<T>, content: @Composable (T) -> Unit) {
    val value = kept.value
    val error = kept.error
    if (value == null) {
        if (error == null) Unseen("Reading LoR…") else Unreachable(error, model)
        return
    }
    Column(Modifier.alpha(if (error != null || !kept.current) 0.6f else 1f)) { content(value) }
    if (error != null) {
        val at = kept.at?.atZone(ZoneId.systemDefault())?.format(Clock)
        Unreachable(if (at != null) "$error Showing what it said at $at." else error, model)
    }
}

@Composable
private fun TaskRow(model: AgentModel, t: DeviceTasks.Task, line: String, done: Boolean = false) {
    LorRow(t.title, line.ifEmpty { null }, mark = if (t.flagged) Or else null, label = "Open in LoR", done = done) {
        model.openLor(LorRows.taskPath(t.id), LaunchFrom.AGENT_CARD)
    }
}

@Composable
private fun PersonRow(model: AgentModel, name: String, kind: String, line: String?, id: String) {
    LorRow(if (kind == "person") name else "$name · $kind", line, label = "Open in LoR") { model.openLor(LorRows.personPath(id), LaunchFrom.AGENT_CARD) }
}

/**
 * A row of a list: its title and the line under it, both opening its place. [mark] is a small dot
 * before it (amber for a flag, cyan for the agent); a [done] task's title is dim and struck through, as the web's.
 */
@Composable
internal fun LorRow(title: String, line: String?, mark: Color? = null, label: String, done: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(mark ?: Color.Transparent, CircleShape))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                title,
                style = Body.copy(fontSize = 13.5.sp, color = if (done) Dim else Txt, textDecoration = if (done) TextDecoration.LineThrough else null),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            line?.let { Text(it, style = Small, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** A group's head inside a list: DESIGN.md's label, with its count. */
@Composable
private fun GroupHead(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 4.dp)) {
        Text(title.uppercase(), style = Type.label, modifier = Modifier.weight(1f))
        Text(count.toString(), style = Type.label)
    }
}

/** A line that opens the dashboard, under a list. */
@Composable
private fun Link(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = TextStyle(fontFamily = Type.button.fontFamily, fontSize = 13.sp, color = Or))
    }
}
