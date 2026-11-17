package com.spadaval.lor.concepts.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.AppIcon
import com.spadaval.lor.concepts.kit.BlurredWallpaper
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.KeyboardHeight
import com.spadaval.lor.concepts.kit.dragsDrawer
import com.spadaval.lor.lab.App
import com.spadaval.lor.lab.labAnchor
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/*
 * The drawer, a swipe up from home: glass over the blurred wallpaper, search at its top, then the
 * apps used most and every app A to Z. Typing turns it into search: what it understood first (an
 * app, a task to add, a question for the agent), then apps, people, tasks and settings.
 */
@Composable
fun Drawer(store: ConceptStore, ui: QueueUi, wide: Boolean, close: () -> Unit) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(ui.focusSearch, ui.shell.drawer.targetValue) {
        if (ui.focusSearch && ui.shell.drawer.targetValue == 1f) { runCatching { focus.requestFocus() }; ui.focusSearch = false }
        if (ui.shell.drawer.targetValue == 0f) { focusManager.clearFocus(); ui.search.edit { replace(0, length, "") } }
    }
    val query = ui.search.text.toString().trim()
    Box(Modifier.fillMaxSize()) {
        BlurredWallpaper({ 1f })
        Box(Modifier.fillMaxSize().background(Q.ground.copy(alpha = 0.72f)))
        Column(Modifier.fillMaxSize().padding(top = 40.dp)) {
            Row(Modifier.fillMaxWidth().dragsDrawer(ui.shell).padding(horizontal = Q.gutter, vertical = 8.dp).labAnchor("drawer"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Field(ui, ui.search, "Search apps, people, or ask", Modifier.focusRequester(focus), anchor = "search-field", leading = Glyph.Search, onEnter = {
                        firstResult(store, ui, query)?.invoke()
                    })
                }
                if (query.isNotEmpty()) IconButton(Glyph.Close, "Clear", { ui.search.edit { replace(0, length, "") } }, anchor = "search:clear")
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (query.isEmpty()) AllApps(store, ui, wide, close) else Results(store, ui, query, close)
            }
            if (ui.keyboard) Spacer(Modifier.height(KeyboardHeight))
        }
    }
}

@Composable
private fun AllApps(store: ConceptStore, ui: QueueUi, wide: Boolean, close: () -> Unit) {
    val apps = store.apps.filter { it.ref !in store.hidden }.sortedBy { it.label.lowercase() }
    val columns = if (wide) 7 else 4
    val grid = rememberLazyGridState()
    if (store.appsLoading) { BasicText("Reading the app list…", Modifier.padding(Q.gutter), style = Q.label); return }
    LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize().labAnchor("drawer:grid"), state = grid, contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 40.dp)) {
        if (store.quickDial.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("Used most now", Modifier.padding(horizontal = 8.dp)) }
            items(store.quickDial.take(columns), key = { "q:${it.ref}" }) { DrawerApp(store, ui, it, "drawer:${it.label}", close) }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("All apps", Modifier.padding(horizontal = 8.dp), trailing = "${apps.size}") }
        }
        items(apps, key = { it.ref }) { DrawerApp(store, ui, it, if (it in store.quickDial.take(columns)) "drawer-all:${it.label}" else "drawer:${it.label}", close) }
    }
}

@Composable
private fun DrawerApp(store: ConceptStore, ui: QueueUi, app: App, anchor: String, close: () -> Unit) {
    var at by remember { mutableStateOf(Rect.Zero) }
    Column(
        Modifier.fillMaxWidth().height(96.dp).clip(Q.rMd)
            .onGloballyPositioned { at = it.boundsInRoot() }
            .pointerInput(app.ref) {
                detectTapGestures(
                    onTap = { store.launch(app.label); close() },
                    onLongPress = { ui.menu = Menu.App(app.ref, Rect(at.center.x - at.height * 0.33f, at.top, at.center.x + at.height * 0.33f, at.top + at.height * 0.74f)) },
                )
            }
            .labAnchor(anchor),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(10.dp))
        AppIcon(app, 52.dp)
        Spacer(Modifier.height(6.dp))
        BasicText(app.label, Modifier.padding(horizontal = 4.dp), style = Q.label.copy(color = Q.text1, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---- Search ----

/** What the field understood from typed words: a task to add (it saw a date or a #goal), or a question for the agent. */
sealed interface Understood {
    data class Add(val title: String, val due: LocalDate?, val goal: String?) : Understood
    data class Ask(val text: String) : Understood
}

private val DAYS = mapOf(
    "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
    "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY,
    "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
    "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY,
)

/** Reads typed words: a day word or `#goal` makes it a task to add; anything else is a question to ask. No grammar to learn. */
fun understand(text: String, today: LocalDate): Understood {
    val words = text.trim().split(Regex("\\s+"))
    var due: LocalDate? = null
    var goal: String? = null
    val rest = words.filter { w ->
        val l = w.lowercase().trimEnd(',', '.')
        when {
            l == "today" -> { due = today; false }
            l == "tomorrow" -> { due = today.plus(DatePeriod(days = 1)); false }
            l in DAYS -> { var d = today.plus(DatePeriod(days = 1)); while (d.dayOfWeek != DAYS.getValue(l)) d = d.plus(DatePeriod(days = 1)); due = d; false }
            l.startsWith("#") && l.length > 1 -> { goal = w.drop(1); false }
            else -> true
        }
    }
    val title = rest.joinToString(" ").replaceFirstChar { it.uppercase() }
    return if ((due != null || goal != null) && title.isNotEmpty()) Understood.Add(title, due, goal) else Understood.Ask(text.trim())
}

private fun dayWords(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Today"
    today.plus(DatePeriod(days = 1)) -> "Tomorrow"
    else -> d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3) + " ${d.dayOfMonth}"
}

private class Hits(val apps: List<App>, val people: List<String>, val tasks: List<com.spadaval.lor.lor.LorTasks.Task>, val settings: List<Pair<String, String>>)

private fun hits(store: ConceptStore, q: String): Hits {
    val l = q.lowercase()
    fun m(s: String) = s.lowercase().split(' ').any { it.startsWith(l) } || (l.length >= 3 && l in s.lowercase())
    val apps = store.apps.filter { m(it.label) }.sortedByDescending { store.launches[it.ref] ?: 0 }.take(8)
    val people = (store.fixture.search.contacts.map { it.name } + store.fixture.search.shortcuts.filter { it.conversation }.map { it.label }).distinct().filter(::m).take(4)
    val tasks = store.tasks.filter { m(it.title) }.take(4)
    val lorSettings = listOf("Sync" to "Phone sync and sensors", "Server address" to "Where LoR is", "T3 pairing" to "Approvals and questions", "Home" to "Icons, labels, hidden apps", "Timer" to "Labels and the running timer")
    val settings = (lorSettings + store.fixture.search.settingsPages.map { it.title to "Android · ${it.summary}" }).filter { m(it.first) }.take(4)
    return Hits(apps, people, tasks, settings)
}

/** What Enter does: the first app, or else what the field understood. */
private fun firstResult(store: ConceptStore, ui: QueueUi, q: String): (() -> Unit)? {
    if (q.isEmpty()) return null
    hits(store, q).apps.firstOrNull()?.let { return { store.launch(it.label) } }
    return { act(store, ui, understand(q, store.today)) }
}

private fun act(store: ConceptStore, ui: QueueUi, u: Understood) {
    when (u) {
        is Understood.Add -> { store.addTask(u.title, u.due); ui.search.edit { replace(0, length, "") } }
        is Understood.Ask -> { ui.quick.edit { replace(0, length, u.text) }; ui.sent = null; ui.sheet = Sheet.QuickSend }
    }
}

@Composable
private fun Results(store: ConceptStore, ui: QueueUi, q: String, close: () -> Unit) {
    val h = hits(store, q)
    val u = understand(q, store.today)
    LazyColumn(Modifier.fillMaxSize().labAnchor("results"), contentPadding = PaddingValues(start = Q.gutter, end = Q.gutter, bottom = 24.dp)) {
        if (h.apps.isNotEmpty()) item(key = "apps") {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                h.apps.take(4).forEach { a ->
                    Column(
                        Modifier.weight(1f).clip(Q.rMd).clickable { store.launch(a.label); close() }.padding(vertical = 8.dp).labAnchor("search:${a.label}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(a, 52.dp)
                        Spacer(Modifier.height(6.dp))
                        BasicText(a.label, style = Q.label.copy(color = Q.text1, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(4 - h.apps.take(4).size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item(key = "understood") { UnderstoodRow(store, ui, u) }
        if (h.people.isNotEmpty()) {
            item(key = "people-label") { SectionLabel("People") }
            items(h.people, key = { "p:$it" }) { name ->
                ResultRow(Glyph.Person, name, "Call, message or open in LoR", Q.text2, anchor = "person:$name") { store.launch("Contacts: $name") }
            }
        }
        if (h.tasks.isNotEmpty()) {
            item(key = "tasks-label") { SectionLabel("Tasks") }
            items(h.tasks, key = { "t:${it.id}" }) { t ->
                ResultRow(Glyph.Task, t.title, t.due?.let { "Due ${dayWords(it, store.today)}" } ?: "No date", Q.text2, anchor = "task:${t.title}") {
                    store.open = "task:${t.id}"; ui.sheet = Sheet.Item("task:${t.id}")
                }
            }
        }
        if (h.settings.isNotEmpty()) {
            item(key = "settings-label") { SectionLabel("Settings") }
            items(h.settings, key = { "s:${it.first}" }) { (title, detail) ->
                ResultRow(Glyph.Settings, title, detail, Q.text2, anchor = "setting:$title") {
                    if (detail.startsWith("Android")) store.toast("Opens Android's $title settings") else ui.sheet = Sheet.Settings(title.lowercase())
                }
            }
        }
    }
}

/**
 * What the field understood, before it acts: the task it would add, with its day and goal as
 * chips, or the question it would ask the agent. The other reading is one tap below.
 */
@Composable
private fun UnderstoodRow(store: ConceptStore, ui: QueueUi, u: Understood) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(Q.rLg).background(Q.surface1.copy(alpha = 0.9f)).border(1.dp, Q.line2, Q.rLg).padding(6.dp),
    ) {
        when (u) {
            is Understood.Add -> {
                ResultRow(Glyph.Plus, "Add task", null, Q.text1, anchor = "understood:add", chips = listOfNotNull(u.title, u.due?.let { dayWords(it, store.today) }, u.goal?.let { "#$it" })) { act(store, ui, u) }
                ResultRow(Glyph.Agent, "Ask the agent instead", null, Q.text2, anchor = "understood:ask-instead") { act(store, ui, Understood.Ask(ui.search.text.toString())) }
            }
            is Understood.Ask -> ResultRow(Glyph.Agent, "Ask the agent", "“${u.text}”", Q.cyan, anchor = "understood:ask") { act(store, ui, u) }
        }
    }
}

@Composable
private fun ResultRow(glyph: Glyph, title: String, detail: String?, tint: Color, anchor: String, chips: List<String> = emptyList(), onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Q.rMd).clickable { onClick() }.heightIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 8.dp).labAnchor(anchor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Q.wash2), contentAlignment = Alignment.Center) { Icon(glyph, tint, size = 18.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Q.body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) BasicText(detail, style = Q.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (chips.isNotEmpty()) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEachIndexed { i, c ->
                    BasicText(
                        c, Modifier.widthIn(max = 220.dp).clip(Q.rSm).background(if (i == 0) Q.wash2 else Q.amberGlow).padding(horizontal = 8.dp, vertical = 3.dp),
                        style = Q.label.copy(color = if (i == 0) Q.text1 else Q.amber), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Icon(Glyph.Chevron, Q.text3, size = 16.dp)
    }
}
