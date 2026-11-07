package com.spadaval.lor.home

import androidx.compose.ui.graphics.RectangleShape
import com.spadaval.lor.ui.*
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.collect.LaunchFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * The drawer (launcher polish 2.2): search at the top, then one of three views. Sections opens on
 * Recent and Frequent rows, New, the sorted sections ([Drawer.sort]) and Work profile; A–Z has
 * letter headers and a rail that jumps to a letter, as Launcher3's all-apps fast scroller does;
 * Most used ranks by launch count. Typing shows search results whatever the view.
 */

/** How long an app counts as new. */
private const val NEW_FOR_MS = 3L * 24 * 60 * 60 * 1000

/** One cell or full-width line of the drawer's grid. Keys are unique: an app can show twice (a row and its section). */
private sealed interface Entry {
    val key: String

    class Header(val title: String, val reason: String?, override val key: String) : Entry
    class Tile(val app: HomeApp, val dot: Boolean, override val key: String, val go: Boolean = false) : Entry
    /** A search result drawn as a row: a shortcut, a settings page, a contact (launcher polish 5.2 on). */
    class Row(val value: Any, override val key: String, val go: Boolean) : Entry
    class HiddenApps(val count: Int) : Entry {
        override val key = "hidden-apps"
    }
}

/** The grid's entries for [view], and the A–Z view's letters with the index of each header. */
private class Built(val entries: List<Entry>, val letters: List<Pair<String, Int>>)

private fun build(view: DrawerView, apps: List<HomeApp>, file: LayoutFile?, stats: LaunchStats, columns: Int, now: Long): Built {
    val byKey = apps.associateBy { it.key }
    val launched = stats.entries
    // New, and not opened from the launcher since it was installed.
    fun dot(app: HomeApp) = app.mainProfile && now - app.firstInstalled < NEW_FOR_MS && (launched[app.key]?.last ?: 0) < app.firstInstalled
    val out = mutableListOf<Entry>()
    fun header(title: String, reason: String? = null) = out.add(Entry.Header(title, reason, "h${out.size}:$title"))
    fun tiles(prefix: String, list: List<HomeApp>) = list.forEach { out.add(Entry.Tile(it, dot(it), "$prefix:${it.key}")) }
    val letters = mutableListOf<Pair<String, Int>>()
    when (view) {
        DrawerView.SECTIONS -> {
            val recent = stats.recent().mapNotNull(byKey::get).take(columns)
            if (recent.isNotEmpty()) { header("Recent"); tiles("recent", recent) }
            val frequent = stats.frequent().mapNotNull(byKey::get).take(columns)
            if (frequent.isNotEmpty()) { header("Frequent"); tiles("frequent", frequent) }
            // Main-profile apps only: a work-profile app's install time can't be read (see HomeApp.firstInstalled).
            val new = apps.filter { it.mainProfile && now - it.firstInstalled < NEW_FOR_MS }.sortedByDescending { it.firstInstalled }
            if (new.isNotEmpty()) { header("New"); tiles("new", new) }
            val drawer = file?.drawer ?: Drawer()
            drawer.sort(apps.filter { it.mainProfile }, { it.id }, { it.category })
                .filter { !it.hidden && it.apps.isNotEmpty() }
                .forEach { s -> header(s.name, s.reason); tiles("s", s.apps) }
            val work = apps.filterNot { it.mainProfile }
            if (work.isNotEmpty()) { header("Work profile"); tiles("work", work) }
        }
        DrawerView.AZ -> apps.groupBy { letterOf(it.label) }.forEach { (letter, list) ->
            letters.add(letter to out.size)
            header(letter)
            tiles("az", list)
        }
        DrawerView.USED -> {
            val used = stats.mostUsed().mapNotNull(byKey::get)
            tiles("used", used + apps.filterNot { it.key in launched })
        }
    }
    val hidden = file?.hidden?.size ?: 0
    if (hidden > 0) out.add(Entry.HiddenApps(hidden))
    return Built(out, letters)
}

/** The grid's entries for search results: a header per block when more than one shows. */
private fun searchEntries(results: Results<Any>, goKey: String?): Built {
    val out = mutableListOf<Entry>()
    val headers = results.blocks.size > 1
    results.blocks.forEach { block ->
        if (headers) out.add(Entry.Header(block.kind.title, null, "qh:${block.kind}"))
        block.entries.forEach { e ->
            when (val v = e.value) {
                is HomeApp -> out.add(Entry.Tile(v, false, "q:${e.key}", go = e.key == goKey))
                else -> out.add(Entry.Row(v, "q:${e.key}", e.key == goKey))
            }
        }
    }
    return Built(out, emptyList())
}

/** The A–Z header an app sits under: its first letter without accents, or # for anything else. */
internal fun letterOf(label: String): String {
    val c = Match.normalize(label).firstOrNull()?.uppercaseChar()
    return if (c != null && c in 'A'..'Z') c.toString() else "#"
}

/**
 * Searches the drawer's [apps] and [sources] for [query] (launcher polish 5.1), logging how many
 * items it scored and how long it took (never the query). Off the main thread, except for Go's
 * fallback when the results on screen are for an older text.
 */
internal fun searchAll(query: String, apps: List<HomeApp>, sources: SearchSources, stats: LaunchStats, check: () -> Unit = {}): Results<Any> {
    val start = System.nanoTime()
    // A shortcut shows only when its app is in the drawer's list (launcher polish 5.2), so a hidden app's don't.
    val owners = apps.map { Owner(it.packageName, it.id.className, it.user.toString()) to it }
    val shortcuts = sources.shortcuts.mapNotNull { f ->
        val hit = f.value as ShortcutHit
        val i = hit.info
        ownerOf(owners, i.`package`, i.activity?.className, i.userHandle.toString())?.let { Found<Any>(f.kind, f.key, f.names, SearchShortcut(hit, it)) }
    }
    val entries: List<Found<Any>> = apps.map { Found(Kind.APPS, it.key, listOf(it.searchName), it) } +
        shortcuts + sources.settings + sources.contacts
    val results = Search.run(query, entries, { e -> if (e.kind == Kind.APPS) stats.entries[e.key]?.weight ?: 0.0 else 0.0 }, { Answers.of(it) }, check)
        .withShortcutsOfOneApp({ (it.value as HomeApp).packageName }) { one ->
            // The app's menu's shortcuts: its own activity's, manifest first, then by rank (1.2); not its cached chats.
            shortcuts.filter { (it.value as SearchShortcut).owner === one.value && !(it.value as SearchShortcut).hit.info.isCached }
                .sortedWith(compareBy({ !(it.value as SearchShortcut).hit.info.isDeclaredInManifest }, { (it.value as SearchShortcut).hit.info.rank }))
        }
    Log.d(HomeStore.SEARCH_TAG, "searched ${entries.size} items in ${"%.2f".format((System.nanoTime() - start) / 1e6)} ms")
    return results
}

@Composable
internal fun Drawer(apps: List<HomeApp>, stats: LaunchStats, sources: SearchSources, ui: HomeUi, home: HomeContext) {
    // Something typed makes it a search, recorded with the text as typed.
    val searching = ui.query.isNotBlank()
    val at = if (searching) At(LaunchFrom.SEARCH, query = ui.query) else At(LaunchFrom.DRAWER)
    // Each keystroke starts a search off the main thread and cancels the one before (launcher polish 5.1).
    val results by produceState(Results.empty<Any>(""), ui.query, apps, sources, stats) {
        val q = ui.query
        value = if (q.isBlank()) Results.empty(q) else withContext(Dispatchers.Default) {
            val job = coroutineContext.job
            searchAll(q, apps, sources, stats) { job.ensureActive() }
        }
    }
    // Results for the text in the field; "nothing" waits for them so it doesn't flicker while typing.
    val current = results.query == ui.query
    // While the app list loads (launcher polish 3.2), nothing matches, but there's nothing to say yet.
    val nothing = searching && current && results.nothing && apps.isNotEmpty()
    val ways = remember(nothing, ui.query, sources.targets) { if (nothing) elsewhere(sources.targets, ui.query) else emptyList() }
    var focused by remember { mutableStateOf(false) }
    // The field's text and cursor; a chip or a home press changes ui.query, which puts the cursor at the end.
    var field by remember { mutableStateOf(TextFieldValue(ui.query, TextRange(ui.query.length))) }
    if (field.text != ui.query) field = TextFieldValue(ui.query, TextRange(ui.query.length))
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Opened ready to type, when the file asks (launcher polish 5.5), once the drawer has settled
    // open (7.1), so a swipe that goes back down leaves no keyboard.
    val settledOpen by remember { derivedStateOf { ui.drawer && !ui.grid.follow.held && ui.drawerShown >= 1f } }
    LaunchedEffect(settledOpen) {
        if (settledOpen && home.file?.drawer?.keyboard == true) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    // A finger taking the drawer puts the keyboard away, as a drag on the list does.
    LaunchedEffect(ui.grid.follow.held) {
        if (ui.grid.follow.held && focused) {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }
    // Where each result's icon is, by its entry's key, so Go grows its app out of it (launcher polish 7.4).
    val found = remember(results) { HashMap<String, IconSpot>() }
    fun go() {
        val r = if (current) results else searchAll(ui.query, apps, sources, stats)
        val first = r.go
        if (first != null) openFound(first.value, at, home, found["q:${first.key}"]) else elsewhere(sources.targets, ui.query).firstOrNull()?.let { home.actions.elsewhere(it, ui.query) }
    }
    val saved = home.file?.drawer?.view ?: DrawerView.SECTIONS
    // Shown at once; the file catches up when the write lands.
    var view by remember(saved) { mutableStateOf(saved) }
    // A pull down at the top of the list moves the drawer; home's root measures it (launcher polish 7.1).
    val pullToClose = remember {
        object : NestedScrollConnection {
            // A drag on the list puts the keyboard away so the list can be read (as Kvaesitso does).
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f && focused) {
                    // Hidden as well as unfocused, so the keyboard goes whatever losing focus does.
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Bg).swallowTaps().cellInsetsPadding().padding(top = 12.dp)) {
        OutlinedTextField(
            field,
            { field = it; ui.query = it.text },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focus).onFocusChanged { focused = it.isFocused },
            placeholder = { Text("Search", color = Dim) },
            singleLine = true,
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Txt, unfocusedTextColor = Txt, cursorColor = Txt,
                // DESIGN.md's field: an Edge border, amber at 70% when focused.
                focusedBorderColor = Or.copy(alpha = 0.7f), unfocusedBorderColor = Edge,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { go() }),
        )
        // A quick answer (launcher polish 5.4) heads the results; it opens nothing, so Go passes it by.
        if (searching) results.answer?.let { AnswerCard(it) }
        // Recent searches, while the empty field has focus (launcher polish 5.5).
        if (!searching && focused && stats.searches.isNotEmpty()) {
            RecentSearches(stats.searches, { ui.query = it }, home.actions::clearSearches)
        }
        if (!searching) {
            ViewSwitch(view) { v ->
                view = v
                home.actions.setView(v)
            }
        } else if (nothing) {
            NoResults(ui.query, ways, line = results.answer == null) { home.actions.elsewhere(it, ui.query) }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = LocalHomeLook.current.columnsFor(maxWidth.value).drawer
            val now = remember(apps, stats) { System.currentTimeMillis() }
            val goKey = if (focused && current) results.go?.key else null
            val built = remember(view, apps, home.file, stats, columns, searching, results, goKey) {
                if (searching) searchEntries(results, goKey)
                else build(view, apps, home.file, stats, columns, now)
            }
            val grid = rememberLazyGridState()
            // A new view starts at its top.
            LaunchedEffect(view) { grid.scrollToItem(0) }
            LazyVerticalGrid(
                GridCells.Fixed(columns),
                // A swipe the list can't take, at its top or its end, moves the grid (lor-qdmy; 7.1's pull down).
                Modifier.fillMaxSize().nestedScroll(pullToClose).scrollsInGrid(grid),
                state = grid,
                contentPadding = PaddingValues(start = 8.dp, end = if (built.letters.isEmpty()) 8.dp else 28.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(built.entries, key = { it.key }, span = { if (it is Entry.Tile) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) { e ->
                    when (e) {
                        is Entry.Header -> SectionHeader(e.title, e.reason)
                        // A main-profile app can be picked up and dragged onto home (launcher polish 4.6); refs can't name a work-profile one.
                        is Entry.Tile -> Box(
                            Modifier.dragSource(home.drag, e.key) { b ->
                                if (e.app.mainProfile) Source(b, Slot.DrawerApp(e.app.key), 2) else null
                            },
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            // Go's target, while the field has focus (launcher polish 5.1): DESIGN.md's selection.
                            if (e.go) Box(Modifier.matchParentSize().selection())
                            MenuTile(e.app, at, home, dot = e.dot, icon = found.getOrPut(e.key) { IconSpot() })
                        }
                        is Entry.Row -> ResultRow(e.value, e.go, at, home, found.getOrPut(e.key) { IconSpot() })
                        is Entry.HiddenApps -> Text(
                            "Hidden apps (${e.count})",
                            style = Type.body.copy(color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().clickable { home.actions.settings() }.padding(vertical = 20.dp),
                        )
                    }
                }
            }
            if (built.letters.isNotEmpty()) LetterRail(built.letters, grid, Modifier.align(Alignment.CenterEnd))
        }
    }
}

/** The recent searches as chips, the view switch's look, with Clear last; a tap types one in. */
@Composable
private fun RecentSearches(searches: List<String>, pick: (String) -> Unit, clear: () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        searches.forEach { q -> Chip(q, Txt, Edge) { pick(q) } }
        Chip("Clear", Mute, Color.Transparent, clear)
    }
}

@Composable
private fun Chip(text: String, color: Color, edge: Color, onClick: () -> Unit) {
    Text(
        text,
        style = Type.body.copy(color = color, fontSize = 13.sp),
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, edge, RectangleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ViewSwitch(view: DrawerView, choose: (DrawerView) -> Unit) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(DrawerView.SECTIONS to "Sections", DrawerView.AZ to "A–Z", DrawerView.USED to "Most used").forEach { (v, label) ->
            val on = v == view
            Text(
                label,
                style = Type.body.copy(color = if (on) Txt else Mute, fontSize = 13.sp, fontWeight = if (on) FontWeight.Medium else FontWeight.Normal),
                modifier = Modifier
                    .background(if (on) Or.copy(alpha = 0.08f) else Color.Transparent, RectangleShape)
                    .border(1.dp, if (on) Or.copy(alpha = 0.55f) else Edge, RectangleShape)
                    .semantics { role = Role.Tab; selected = on }
                    .clickable { if (!on) choose(v) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, reason: String?) {
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp).semantics(mergeDescendants = true) { heading() }) {
        Text(title.uppercase(), style = Type.title)
        reason?.let { Text(it, style = Type.bodySmall.copy(color = Dim), modifier = Modifier.padding(top = 2.dp)) }
    }
}

/**
 * The A–Z rail: a tap or a drag on a letter scrolls the grid to that letter's header, after
 * Launcher3's `RecyclerViewFastScroller` (Apache-2.0), which jumps by the touch's place on the rail.
 */
@Composable
private fun LetterRail(letters: List<Pair<String, Int>>, grid: LazyGridState, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var height by remember { mutableIntStateOf(1) }
    var current by remember { mutableStateOf<String?>(null) }
    fun jump(y: Float) {
        val i = (y / height * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val (letter, index) = letters[i]
        if (letter != current) {
            current = letter
            scope.launch { grid.scrollToItem(index) }
        }
    }
    Column(
        modifier
            .fillMaxHeight()
            .width(28.dp)
            .padding(vertical = 8.dp)
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            // Every vertical drag here is the rail's, never the grid's.
            .scrollsInGrid { true }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    jump(down.position.y)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        jump(change.position.y)
                        change.consume()
                    }
                    current = null
                }
            }
            .semantics { contentDescription = "Jump to a letter" },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { (letter, _) ->
            Text(letter, style = Type.body.copy(color = if (letter == current) Or else Mute, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        }
    }
}
