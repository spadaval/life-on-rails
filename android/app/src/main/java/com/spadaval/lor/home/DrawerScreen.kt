package com.spadaval.lor.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch

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
    class Tile(val app: HomeApp, val dot: Boolean, override val key: String) : Entry
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

/** The A–Z header an app sits under: its first letter without accents, or # for anything else. */
internal fun letterOf(label: String): String {
    val c = AppSearch.normalize(label).firstOrNull()?.uppercaseChar()
    return if (c != null && c in 'A'..'Z') c.toString() else "#"
}

@Composable
internal fun Drawer(apps: List<HomeApp>, stats: LaunchStats, ui: HomeUi, home: HomeContext) {
    // Something typed makes it a search, recorded with the text as typed.
    val searching = ui.query.isNotBlank()
    val at = if (searching) At(LaunchFrom.SEARCH, query = ui.query) else At(LaunchFrom.DRAWER)
    val results = remember(apps, ui.query) { AppSearch.search(ui.query, apps) { it.label } }
    // While the app list loads (launcher polish 3.2), nothing matches, but there's nothing to say yet.
    val nothing = searching && results.isEmpty() && apps.isNotEmpty()
    val pm = LocalContext.current.packageManager
    val ways = remember(nothing, ui.query) { if (nothing) elsewhere(pm, ui.query) else emptyList() }
    val saved = home.file?.drawer?.view ?: DrawerView.SECTIONS
    // Shown at once; the file catches up when the write lands.
    var view by remember(saved) { mutableStateOf(saved) }
    val closeAt = with(LocalDensity.current) { 120.dp.toPx() }
    // A pull down past the top of the list closes the drawer.
    val pullToClose = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    pulled = if (available.y > 0) pulled + available.y else 0f
                    if (pulled > closeAt) { pulled = 0f; ui.drawer = false; ui.query = "" }
                }
                return Offset.Zero
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Sheet).swallowTaps().safeDrawingPadding().padding(top = 12.dp)) {
        OutlinedTextField(
            ui.query,
            { ui.query = it },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search apps", color = Dim) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Ink, unfocusedTextColor = Ink, cursorColor = Ink,
                focusedBorderColor = Muted, unfocusedBorderColor = Line,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                results.firstOrNull()?.let { home.actions.open(it, at) } ?: ways.firstOrNull()?.let { home.actions.elsewhere(it, ui.query) }
            }),
        )
        if (!searching) {
            ViewSwitch(view) { v ->
                view = v
                home.actions.setView(v)
            }
        } else if (nothing) {
            NoResults(ui.query, ways) { home.actions.elsewhere(it, ui.query) }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = if (maxWidth >= 600.dp) 6 else 4
            val now = remember(apps, stats) { System.currentTimeMillis() }
            val built = remember(view, apps, home.file, stats, columns, searching, results) {
                if (searching) Built(results.map { Entry.Tile(it, false, "q:${it.key}") }, emptyList())
                else build(view, apps, home.file, stats, columns, now)
            }
            val grid = rememberLazyGridState()
            // A new view starts at its top.
            LaunchedEffect(view) { grid.scrollToItem(0) }
            LazyVerticalGrid(
                GridCells.Fixed(columns),
                Modifier.fillMaxSize().nestedScroll(pullToClose),
                state = grid,
                contentPadding = PaddingValues(start = 8.dp, end = if (built.letters.isEmpty()) 8.dp else 28.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(built.entries, key = { it.key }, span = { if (it is Entry.Tile) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) { e ->
                    when (e) {
                        is Entry.Header -> SectionHeader(e.title, e.reason)
                        is Entry.Tile -> Box(contentAlignment = Alignment.TopCenter) { MenuTile(e.app, at, home, dot = e.dot) }
                        is Entry.HiddenApps -> Text(
                            "Hidden apps (${e.count})",
                            style = TextStyle(color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth().clickable { home.actions.settings() }.padding(vertical = 20.dp),
                        )
                    }
                }
            }
            if (built.letters.isNotEmpty()) LetterRail(built.letters, grid, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun ViewSwitch(view: DrawerView, choose: (DrawerView) -> Unit) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(DrawerView.SECTIONS to "Sections", DrawerView.AZ to "A–Z", DrawerView.USED to "Most used").forEach { (v, label) ->
            val on = v == view
            Text(
                label,
                style = TextStyle(color = if (on) Ink else Muted, fontSize = 13.sp, fontWeight = if (on) FontWeight.Medium else FontWeight.Normal),
                modifier = Modifier
                    .background(if (on) Amber.copy(alpha = 0.08f) else Color.Transparent, RoundedCornerShape(10.dp))
                    .border(1.dp, if (on) Amber.copy(alpha = 0.55f) else Line, RoundedCornerShape(10.dp))
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
        Text(title.uppercase(), style = TextStyle(color = Muted, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium))
        reason?.let { Text(it, style = TextStyle(color = Dim, fontSize = 12.sp), modifier = Modifier.padding(top = 2.dp)) }
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
            Text(letter, style = TextStyle(color = if (letter == current) Amber else Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium))
        }
    }
}
