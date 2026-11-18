package com.spadaval.lor.concepts.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
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
import com.spadaval.lor.lor.LorTasks
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/*
 * The drawer, a swipe up from home, and search. Every app is a grid until something is typed; the
 * one field sits at the foot, at the thumb, above the keyboard. Typed words turn the drawer into
 * search: results rise from the field, nearest first (apps, then what the field understood, then
 * people, tasks and settings). Unfolded, search is two panes beside home's: the results on the
 * left, and on the right the chosen app (the first, until another is picked) with its shortcuts
 * and recent conversations, or the task the field would add. On the cover an app's › opens that
 * pane as a sheet.
 */
@Composable
fun Drawer(store: ConceptStore, ui: PanesUi, wide: Boolean, close: () -> Unit) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(ui.focusSearch, ui.shell.drawer.targetValue) {
        if (ui.focusSearch && ui.shell.drawer.targetValue == 1f) { runCatching { focus.requestFocus() }; ui.focusSearch = false }
        if (ui.shell.drawer.targetValue == 0f) { focusManager.clearFocus(); ui.search.edit { replace(0, length, "") }; ui.searchApp = null }
    }
    val query = ui.search.text.toString().trim()
    Box(Modifier.fillMaxSize().labAnchor("drawer:open")) {
        BlurredWallpaper({ 1f })
        Box(Modifier.fillMaxSize().background(P.ground.copy(alpha = 0.78f)))
        Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
            // The header: a handle and the drawer's name; a drag down on it closes the drawer.
            Column(Modifier.fillMaxWidth().dragsDrawer(ui.shell).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 10.dp).labAnchor("drawer")) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(P.rPill).background(P.line3))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(if (query.isEmpty()) "APPS" else "SEARCH", style = P.paneTitle)
                    Spacer(Modifier.width(10.dp))
                    BasicText(if (query.isEmpty()) "${store.apps.count { it.ref !in store.hidden }}" else "“$query”", style = P.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val field: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        Field(ui, ui.search, "Search, add or ask", Modifier.focusRequester(focus), anchor = "search-field", leading = Glyph.Search, pill = true, onEnter = {
                            firstResult(store, ui, query, close)?.invoke()
                        })
                    }
                    if (query.isNotEmpty()) IconButton(Glyph.Close, "Clear", { ui.search.edit { replace(0, length, "") }; ui.searchApp = null }, anchor = "search:clear")
                }
            }
            // The field stays in one place in the tree whatever is shown above it, so typing the
            // first letter (which turns the grid into search, and unfolded into two panes) keeps its focus.
            val panes = wide && query.isNotEmpty()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    panes -> Row(Modifier.fillMaxSize().padding(horizontal = P.gap), horizontalArrangement = Arrangement.spacedBy(P.gap)) {
                        ListPane(Modifier.width(400.dp).fillMaxHeight(), head = {}) { Results(store, ui, query, close, wide = true) }
                        DetailPane(Modifier.weight(1f).fillMaxHeight()) { SearchPane(store, ui, query, close) }
                    }
                    query.isEmpty() -> AllApps(store, ui, wide, close)
                    else -> Results(store, ui, query, close, wide = false)
                }
            }
            Box(if (panes) Modifier.width(424.dp) else Modifier.fillMaxWidth().padding(horizontal = if (wide) 120.dp else 0.dp)) { field() }
            Spacer(Modifier.height(if (ui.keyboard) KeyboardHeight else 14.dp))
        }
    }
}

@Composable
private fun AllApps(store: ConceptStore, ui: PanesUi, wide: Boolean, close: () -> Unit) {
    val apps = store.apps.filter { it.ref !in store.hidden }.sortedBy { it.label.lowercase() }
    val columns = if (wide) 7 else 4
    if (store.appsLoading) { BasicText("Reading the app list…", Modifier.padding(20.dp), style = P.small); return }
    LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize().labAnchor("drawer:grid"), state = rememberGrid(store), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
        if (store.quickDial.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { GroupLabel("Used most now") }
            items(store.quickDial.take(columns), key = { "q:${it.ref}" }) { DrawerApp(store, ui, it, "drawer:${it.label}", close) }
            item(span = { GridItemSpan(maxLineSpan) }) { GroupLabel("All apps", apps.size) }
        }
        items(apps, key = { it.ref }) { DrawerApp(store, ui, it, if (it in store.quickDial.take(columns)) "drawer-all:${it.label}" else "drawer:${it.label}", close) }
    }
}

@Composable
private fun rememberGrid(store: ConceptStore) = remember(store) { androidx.compose.foundation.lazy.grid.LazyGridState() }

@Composable
private fun DrawerApp(store: ConceptStore, ui: PanesUi, app: App, anchor: String, close: () -> Unit) {
    var at by remember { mutableStateOf(Rect.Zero) }
    Column(
        Modifier.fillMaxWidth().height(96.dp).clip(P.rMd)
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
        BasicText(app.label, Modifier.padding(horizontal = 4.dp), style = P.meta.copy(color = P.text1, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

fun dayWords(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Today"
    today.plus(DatePeriod(days = 1)) -> "Tomorrow"
    else -> d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3) + " ${d.dayOfMonth}"
}

private class Hits(val apps: List<App>, val people: List<String>, val tasks: List<LorTasks.Task>, val settings: List<Pair<String, String>>)

private fun hits(store: ConceptStore, q: String): Hits {
    val l = q.lowercase()
    fun m(s: String) = s.lowercase().split(' ').any { it.startsWith(l) } || (l.length >= 3 && l in s.lowercase())
    val apps = store.apps.filter { m(it.label) }.sortedByDescending { store.launches[it.ref] ?: 0 }.take(6)
    val people = (store.fixture.search.contacts.map { it.name } + store.fixture.search.shortcuts.filter { it.conversation }.map { it.label }).distinct().filter(::m).take(4)
    val tasks = store.tasks.filter { m(it.title) }.take(4)
    val lorSettings = listOf("Sync" to "Phone sync and sensors", "Server address" to "Where LoR is", "T3 pairing" to "Approvals and questions", "Home" to "Icons, labels, hidden apps", "Timer" to "Labels and the running timer")
    val settings = (lorSettings + store.fixture.search.settingsPages.map { it.title to "Android · ${it.summary}" }).filter { m(it.first) }.take(4)
    return Hits(apps, people, tasks, settings)
}

/** What Enter does: the first app, or else what the field understood. */
private fun firstResult(store: ConceptStore, ui: PanesUi, q: String, close: () -> Unit): (() -> Unit)? {
    if (q.isEmpty()) return null
    hits(store, q).apps.firstOrNull()?.let { return { store.launch(it.label); close() } }
    return { act(store, ui, understand(q, store.today)) }
}

private fun act(store: ConceptStore, ui: PanesUi, u: Understood) {
    when (u) {
        is Understood.Add -> if (ui.saves()) { store.addTask(u.title, u.due); ui.search.edit { replace(0, length, "") } }
        is Understood.Ask -> { ui.quick.edit { replace(0, length, u.text) }; ui.sent = null; ui.sheet = Sheet.QuickSend }
    }
}

/**
 * The results, rising from the field: the list is laid out from its foot, so the first app is
 * nearest the thumb, and each group's label sits above its rows.
 */
@Composable
private fun Results(store: ConceptStore, ui: PanesUi, q: String, close: () -> Unit, wide: Boolean) {
    val h = hits(store, q)
    val u = understand(q, store.today)
    val chosen = ui.searchApp?.let { r -> h.apps.firstOrNull { it.ref == r } } ?: h.apps.firstOrNull()
    LazyColumn(
        Modifier.fillMaxSize().labAnchor("results"), reverseLayout = true, verticalArrangement = Arrangement.Bottom,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        // Laid out from the foot: each group's rows come first (bottom up), then its label.
        group("Apps", h.apps) { a ->
            AppResult(store, ui, a, selected = wide && a == chosen, wide, close)
        }
        item(key = "understood") { UnderstoodRow(store, ui, u, wide) }
        group("People", h.people) { name -> ResultRow(Glyph.Person, name, "Call, message or open in LoR", P.text2, anchor = "person:$name") { store.launch("Contacts: $name") } }
        group("Tasks", h.tasks) { t ->
            ResultRow(Glyph.Task, t.title, t.due?.let { "Due ${dayWords(it, store.today)}" } ?: "No date", P.text2, anchor = "task:${t.title}") {
                ui.openToday = "task:${t.id}"
                close()
                ui.later(ui.scope) { shell.goTo(PanesUi.TODAY) }
            }
        }
        group("Settings", h.settings) { (title, detail) ->
            ResultRow(Glyph.Settings, title, detail, P.text2, anchor = "setting:$title") {
                when {
                    detail.startsWith("Android") -> store.toast("Opens Android's $title settings")
                    title == "Timer" -> ui.sheet = Sheet.Timer
                    title == "Home" -> ui.sheet = Sheet.Settings("home")
                    title == "Sync" -> ui.sheet = Sheet.Settings("sync")
                    else -> ui.sheet = Sheet.Settings("connections")
                }
            }
        }
    }
}

private fun <T> LazyListScope.group(label: String, rows: List<T>, row: @Composable (T) -> Unit) {
    if (rows.isEmpty()) return
    items(rows, key = { "$label:$it" }) { row(it) }
    item(key = "label:$label") { GroupLabel(label, rows.size) }
}

/** An app among the results: a tap opens it; its › (or, unfolded, the row being chosen) shows its pane. */
@Composable
private fun AppResult(store: ConceptStore, ui: PanesUi, a: App, selected: Boolean, wide: Boolean, close: () -> Unit) {
    Row(
        Modifier.paneRow(selected, "app:${a.ref}") { store.launch(a.label); close() }.heightIn(min = 60.dp).padding(start = 8.dp, end = 4.dp).labAnchor("search:${a.label}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(a, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(a.label, style = P.rowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val n = store.launches[a.ref] ?: 0
            val shortcuts = store.fixture.search.shortcuts.count { it.pkg == a.pkg }
            BasicText(listOfNotNull("App", if (n > 0) "opened $n times" else null, if (shortcuts > 0) "$shortcuts shortcuts" else null).joinToString(" · "), style = P.meta, maxLines = 1)
        }
        IconButton(Glyph.Chevron, "Shortcuts", { ui.searchApp = a.ref; if (!wide) ui.appSheet = a.ref }, anchor = "app-pane:${a.label}", tint = P.text2)
    }
}

/**
 * What the field understood, before it acts: the task it would add, with its day and goal as chips,
 * or the question it would ask the agent. The other reading is one tap below.
 */
@Composable
private fun UnderstoodRow(store: ConceptStore, ui: PanesUi, u: Understood, wide: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(P.rMd).background(P.raise).border(1.dp, P.line2, P.rMd).padding(4.dp)) {
        when (u) {
            is Understood.Add -> {
                ResultRow(Glyph.Plus, "Add task", null, P.amber, anchor = "understood:add", chips = listOfNotNull(u.title, u.due?.let { dayWords(it, store.today) }, u.goal?.let { "#$it" })) { act(store, ui, u) }
                ResultRow(Glyph.Agent, "Ask the agent instead", null, P.cyan, anchor = "understood:ask-instead") { act(store, ui, Understood.Ask(ui.search.text.toString())) }
            }
            is Understood.Ask -> ResultRow(Glyph.Agent, "Ask the agent", "“${u.text}”", P.cyan, anchor = "understood:ask") { act(store, ui, u) }
        }
    }
}

@Composable
private fun ResultRow(glyph: Glyph, title: String, detail: String?, tint: Color, anchor: String, chips: List<String> = emptyList(), onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(P.rMd).clickable { onClick() }.heightIn(min = 56.dp).padding(horizontal = 10.dp, vertical = 8.dp).labAnchor(anchor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(glyph, tint, size = 18.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = P.rowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) BasicText(detail, style = P.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (chips.isNotEmpty()) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chips.forEachIndexed { i, c ->
                    BasicText(
                        c, Modifier.widthIn(max = 220.dp).clip(P.rXs).background(if (i == 0) P.raise2 else P.amberSoft).padding(horizontal = 8.dp, vertical = 3.dp),
                        style = P.meta.copy(color = if (i == 0) P.text1 else P.amber), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Icon(Glyph.Chevron, P.text3, size = 16.dp)
    }
}

/** Search's right pane, unfolded: the chosen app with its shortcuts and conversations, or else what the field understood. */
@Composable
private fun SearchPane(store: ConceptStore, ui: PanesUi, q: String, close: () -> Unit) {
    val h = hits(store, q)
    val app = ui.searchApp?.let(store::app) ?: h.apps.firstOrNull()
    if (app != null) AppPane(store, ui, app, close, inSheet = false)
    else Column(Modifier.fillMaxSize().padding(24.dp).labAnchor("search:pane")) {
        when (val u = understand(q, store.today)) {
            is Understood.Add -> {
                Kicker("Add a task", P.amber)
                Spacer(Modifier.height(6.dp))
                BasicText(u.title, style = P.title)
                Spacer(Modifier.height(P.s3))
                BasicText(listOfNotNull(u.due?.let { "Due ${dayWords(it, store.today)}" }, u.goal?.let { "Counts toward #$it" }).joinToString(" · "), style = P.small)
                Spacer(Modifier.height(P.s4))
                PButton("Add task", { act(store, ui, u) }, kind = Kind.PRIMARY, glyph = Glyph.Plus, anchor = "pane:add")
            }
            is Understood.Ask -> {
                Kicker("Ask the agent", P.cyan)
                Spacer(Modifier.height(6.dp))
                BasicText("“${u.text}”", style = P.head)
                Spacer(Modifier.height(P.s3))
                BasicText("Nothing on the phone matches. Send it to the agent as a new thread.", style = P.small)
                Spacer(Modifier.height(P.s4))
                PButton("Ask the agent", { act(store, ui, u) }, kind = Kind.AGENT, glyph = Glyph.Agent, anchor = "pane:ask")
            }
        }
    }
}

/** An app's pane: open it, its shortcuts, its recent conversations, and its uncommon settings under More. */
@Composable
fun AppPane(store: ConceptStore, ui: PanesUi, app: App, close: () -> Unit, inSheet: Boolean) {
    val shortcuts = store.fixture.search.shortcuts.filter { it.pkg == app.pkg }
    Column(Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = if (inSheet) 0.dp else 22.dp).labAnchor("app-pane").labAnchor("app-pane:${app.label}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Kicker("App", P.text3)
                BasicText(app.label, style = if (inSheet) P.head else P.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(P.s4))
        PButton("Open ${app.label}", { store.launch(app.label); ui.appSheet = null; close() }, Modifier.fillMaxWidth(), kind = Kind.PRIMARY, anchor = "app-pane:open")
        val conversations = shortcuts.filter { it.conversation }
        val actions = shortcuts.filter { !it.conversation }
        if (conversations.isNotEmpty()) {
            GroupLabel("Recent conversations", conversations.size, Modifier.padding(start = 0.dp))
            conversations.forEach { s -> PaneLink(Glyph.Person, s.label, "app-pane:${s.label}") { store.launch("${app.label}: ${s.label}"); ui.appSheet = null; close() } }
        }
        if (actions.isNotEmpty()) {
            GroupLabel("Shortcuts", actions.size, Modifier.padding(start = 0.dp))
            actions.forEach { s -> PaneLink(Glyph.Chevron, s.label, "app-pane:${s.label}") { store.launch("${app.label}: ${s.label}"); ui.appSheet = null; close() } }
        }
        if (shortcuts.isEmpty()) { Spacer(Modifier.height(P.s4)); BasicText("${app.label} offers no shortcuts.", style = P.small) }
        More(ui, "app:${app.ref}", listOf(
            "App info" to { store.toast("Opens Android's page for ${app.label}") },
            "Hide from the drawer" to { store.toast("${app.label} is hidden. Settings › Home lists it.") },
        ))
    }
}

@Composable
private fun PaneLink(glyph: Glyph, text: String, anchor: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(P.rSm).clickable { onClick() }.heightIn(min = 48.dp).padding(horizontal = 4.dp).labAnchor(anchor), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(P.raise2), contentAlignment = Alignment.Center) { Icon(glyph, P.text2, size = 16.dp) }
        Spacer(Modifier.width(12.dp))
        BasicText(text, Modifier.weight(1f), style = P.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
