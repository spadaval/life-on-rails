package com.spadaval.lor.concepts.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.T3Card
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * A text field in Timeline's style: a rounded wash, a placeholder, and the stand-in keyboard while it
 * has focus. Its [state] lives in [TimelineUi], so what is typed survives a fold.
 */
@Composable
fun Field(
    ui: TimelineUi,
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    anchor: String? = null,
    lines: Int = 1,
    onEnter: (() -> Unit)? = null,
    leading: Glyph? = null,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        state,
        modifier.fillMaxWidth().onFocusChanged { f ->
            focused = f.isFocused
            if (f.isFocused) { ui.focused = state; ui.onEnter = onEnter } else if (ui.focused === state) ui.focused = null
        }.then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        textStyle = T.body,
        cursorBrush = SolidColor(T.amber),
        lineLimits = if (lines == 1) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(1, lines),
        onKeyboardAction = { onEnter?.invoke() },
        decorator = { inner ->
            Row(
                Modifier.fillMaxWidth().clip(T.rMd).background(if (focused) T.surface2 else T.wash)
                    .border(1.dp, if (focused) T.text3 else T.line2, T.rMd).heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = if (lines == 1) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (leading != null) { Icon(leading, T.text2, size = 18.dp); Spacer(Modifier.padding(start = 10.dp)) }
                Box(Modifier.weight(1f)) {
                    if (state.text.isEmpty()) BasicText(placeholder, style = T.body.copy(color = T.text3))
                    inner()
                }
            }
        },
    )
}

/**
 * An open item, in a sheet (cover) or the right pane (inner): its card in full, then what belongs to
 * it. A T3 card or thread shows its transcript and a reply field; a flag or a task its note and its
 * due date, which can be changed.
 */
@Composable
fun Detail(store: ConceptStore, ui: TimelineUi, key: String, pane: Boolean, top: androidx.compose.ui.unit.Dp = 44.dp) {
    val item = store.item(key)
    val thread = when {
        item is Item.Card -> item.card.thread
        key.startsWith("thread:") -> store.threads.firstOrNull { it.id == key.removePrefix("thread:") }
        else -> null
    }
    Column(Modifier.fillMaxSize().labAnchor("detail")) {
        // The item itself stays put; what belongs to it scrolls below.
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = if (pane) top else 4.dp, bottom = T.s3)) {
            if (item != null) ItemBody(store, ui, item, lead = false, wide = pane, inDetail = true)
            else if (thread != null) {
                Kicker("Thread \u00B7 ${thread.projectTitle}", T.cyan)
                Spacer(Modifier.height(6.dp))
                BasicText(thread.title, style = T.title)
            } else BasicText("This one has been cleared.", style = T.body.copy(color = T.text2))
        }
        if (thread != null) {
            val messages = store.messages(thread.id)
            Hairline()
            // Newest at the foot, by the reply field, as a chat reads; the position is the store's, so a fold keeps it.
            LazyColumn(
                // Anchors for scripts: the thread, and where it is scrolled to (a fold keeps both).
                Modifier.weight(1f).fillMaxWidth().labAnchor("thread").labAnchor("messages:${messages.size}").labAnchor("thread-at:${store.list("thread:${thread.id}").firstVisibleItemIndex}"),
                state = store.list("thread:${thread.id}"), reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (messages.isEmpty()) item(key = "none") { BasicText(if (store.t3Live) "No messages yet." else "T3 is away; the thread shows once it's back.", style = T.label) }
                items(messages.withIndex().reversed(), key = { "m${it.index}" }) { (_, m) -> Bubble(m.fromUser, m.text, LocalClock.parse(m.at)?.let { LocalClock.time(it, store.zone) }.orEmpty(), m.streaming) }
                item(key = "label") { SectionLabel("Thread", Modifier.padding(top = 0.dp), trailing = "${messages.size} messages") }
            }
            val field = ui.field("reply:${thread.id}")
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1f).labAnchor("draft:${field.text.length}")) { Field(ui, field, if (store.t3Live) "Reply to the agent" else "T3 is away; replies wait", anchor = "reply-field", lines = 4) }
                Spacer(Modifier.padding(start = 6.dp))
                IconButton(Glyph.Send, "Send", {
                    if (field.text.isNotBlank() && store.t3Live) {
                        store.drafts[thread.id] = field.text.toString()
                        store.reply(thread.id)
                        field.clearText()
                    }
                }, anchor = "reply:send", tint = if (field.text.isNotBlank() && store.t3Live) T.amber else T.text3, size = 48.dp)
            }
        } else if (item is Item.Flag || item is Item.Due) {
            val taskId = when (item) { is Item.Flag -> item.need.id; is Item.Due -> item.task.id; else -> "" }
            val task = store.tasks.firstOrNull { it.id == taskId }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                SectionLabel("Due", trailing = task?.due?.let { "now ${it.dayOfMonth} ${it.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }}" } ?: "no date")
                DueChoices(store, ui, taskId, task?.due)
                Spacer(Modifier.height(T.s4))
                TButton("Open in LoR", { store.toast("Opens the task in LoR") }, kind = Kind.QUIET, glyph = Glyph.Chevron)
            }
        }
    }
}

/** Today, Tomorrow, the coming Friday, next Monday, or no date: one tap each, with Undo. */
@Composable
private fun DueChoices(store: ConceptStore, ui: TimelineUi, id: String, due: LocalDate?) {
    val today = store.today
    fun next(day: DayOfWeek): LocalDate { var d = today.plus(DatePeriod(days = 1)); while (d.dayOfWeek != day) d = d.plus(DatePeriod(days = 1)); return d }
    val choices = listOf("Today" to today, "Tomorrow" to today.plus(DatePeriod(days = 1)), "Friday" to next(DayOfWeek.FRIDAY), "Next week" to next(DayOfWeek.MONDAY), "No date" to null)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(T.s2), verticalArrangement = Arrangement.spacedBy(T.s2)) {
        choices.forEach { (label, d) ->
            Chip(label, selected = d == due, detail = d?.let { "${it.dayOfMonth} ${it.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }}" }, onClick = { ui.setDue(id, d) }, anchor = "due:$label", enabled = store.lorLive)
        }
    }
}

/** A message of a thread: his on the right in a wash, the agent's on the left on the page. */
@Composable
private fun Bubble(mine: Boolean, text: String, at: String, streaming: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth(if (mine) 0.86f else 0.94f)
                .clip(T.rLg).background(if (mine) T.wash2 else T.surface1.copy(alpha = 0.7f)).border(1.dp, T.line, T.rLg).padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            BasicText(text, style = T.body)
            Spacer(Modifier.height(4.dp))
            BasicText(if (streaming) "writing…" else "${if (mine) "You" else "Agent"} · $at", style = T.label.copy(color = if (streaming) T.cyan else T.text3))
        }
    }
}

/** The menu a state's ⋯ or long press opened. */
@Composable
fun MenuFor(store: ConceptStore, ui: TimelineUi, menu: Menu) {
    val close = { ui.menu = null }
    when (menu) {
        is Menu.App -> {
            val app = store.app(menu.ref)
            val folder = store.icons.firstOrNull { it.key == menu.ref } as? ConceptStore.Icon.Folder
            val label = app?.label ?: folder?.name ?: "App"
            com.spadaval.lor.concepts.kit.Menu(
                menu.at,
                header = { MenuHeader(label, if (app != null) "Hold and drag to move" else "Folder · ${folder?.apps?.size ?: 0} apps") },
                items = listOfNotNull(
                    if (app != null) com.spadaval.lor.concepts.kit.MenuItem("Open", Glyph.Chevron) { close(); store.launch(label) } else null,
                    if (app?.label == "WhatsApp") com.spadaval.lor.concepts.kit.MenuItem("Message Maya Okafor", Glyph.Person) { close(); store.launch("WhatsApp: Maya Okafor") } else null,
                    com.spadaval.lor.concepts.kit.MenuItem("App info", Glyph.Settings) { close(); store.toast("Opens Android's page for $label") },
                    com.spadaval.lor.concepts.kit.MenuItem("Hide from the drawer", Glyph.Close) { close(); store.toast("$label is hidden. Settings › Home lists it.") },
                    com.spadaval.lor.concepts.kit.MenuItem("Remove from home", Glyph.Close, danger = true) { close(); store.toast("Removed $label from home") },
                ),
                onDismiss = close,
            )
        }
        is Menu.Item -> {
            val item = store.item(menu.key)
            val entries = buildList {
                add(com.spadaval.lor.concepts.kit.MenuItem("Open", Glyph.Chevron) { close(); open(store, ui, menu.key, ui.wide) })
                when (item) {
                    is Item.Card -> {
                        add(com.spadaval.lor.concepts.kit.MenuItem("Open in T3", Glyph.Agent) { close(); store.launch("T3 Code: ${item.card.thread.title}") })
                        if (item.card.kind == T3Card.Kind.FAILED) add(com.spadaval.lor.concepts.kit.MenuItem("Dismiss", Glyph.Close) { close(); store.dismiss(item.card) })
                        else add(com.spadaval.lor.concepts.kit.MenuItem("Start a timer on it", Glyph.Play) { close(); ui.startTimer(item.card.thread.title) })
                    }
                    is Item.Flag -> {
                        add(com.spadaval.lor.concepts.kit.MenuItem("Start a timer on it", Glyph.Play) { close(); ui.startTimer(item.need.title) })
                        if (store.lorLive) add(com.spadaval.lor.concepts.kit.MenuItem("Due tomorrow", Glyph.Calendar) { close(); ui.setDue(item.need.id, store.today.plus(DatePeriod(days = 1))) })
                    }
                    is Item.Due -> {
                        add(com.spadaval.lor.concepts.kit.MenuItem("Start a timer on it", Glyph.Play) { close(); ui.startTimer(item.task.title) })
                        if (store.lorLive) ui.route(store.clock()).firstOrNull { it.kind == SpanKind.FREE }?.let { gap ->
                            add(com.spadaval.lor.concepts.kit.MenuItem("Plan at ${LocalClock.time(gap.start, store.zone)}", Glyph.Clock) { close(); ui.plan(item.task.id, item.task.title, gap.start, gap.end) })
                        }
                        // While LoR isn't answering a date can't be saved, so the menu doesn't offer one.
                        if (store.lorLive) {
                            add(com.spadaval.lor.concepts.kit.MenuItem("Due tomorrow", Glyph.Calendar) { close(); ui.setDue(item.task.id, store.today.plus(DatePeriod(days = 1))) })
                            add(com.spadaval.lor.concepts.kit.MenuItem("No date", Glyph.Close) { close(); ui.setDue(item.task.id, null) })
                        }
                    }
                    null -> {}
                }
            }
            com.spadaval.lor.concepts.kit.Menu(menu.at, entries, onDismiss = close)
        }
        is Menu.Day -> com.spadaval.lor.concepts.kit.Menu(
            menu.at,
            header = { MenuHeader("The day", "What isn't needed every day") },
            items = listOfNotNull(
                if (store.lorLive && ui.blocks.any { it.tone == DayBlock.Tone.PROPOSED }) com.spadaval.lor.concepts.kit.MenuItem("Say yes to every guess (${ui.blocks.count { it.tone == DayBlock.Tone.PROPOSED }})", Glyph.Check) { close(); ui.confirmAll() } else null,
                com.spadaval.lor.concepts.kit.MenuItem("Start a thread in a project", Glyph.Agent) { close(); ui.sheet = Sheet.QuickSend },
                com.spadaval.lor.concepts.kit.MenuItem("People to get back to (${store.people?.overdue?.size ?: 0})", Glyph.Person) { close(); store.toast("Opens People in LoR") },
                com.spadaval.lor.concepts.kit.MenuItem("This phone's sync", Glyph.Sync) { close(); ui.sheet = Sheet.Settings("sync") },
                com.spadaval.lor.concepts.kit.MenuItem("Pairing and the server", Glyph.Phone) { close(); ui.sheet = Sheet.Settings("connections") },
                com.spadaval.lor.concepts.kit.MenuItem("Settings", Glyph.Settings) { close(); ui.sheet = Sheet.Settings() },
            ),
            onDismiss = close,
        )
    }
}

@Composable
private fun MenuHeader(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)) {
        BasicText(title, style = T.body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(detail, style = T.label.copy(color = T.text3))
    }
    Hairline(Modifier.padding(bottom = 4.dp))
}
