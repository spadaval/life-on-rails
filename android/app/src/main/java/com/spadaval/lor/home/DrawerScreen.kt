package com.spadaval.lor.home

import androidx.compose.ui.graphics.RectangleShape
import com.spadaval.lor.ui.*
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.collect.LaunchFrom
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.spadaval.lor.LorApp
import java.time.Instant
import java.time.ZoneId

/*
 * The drawer (lor-iyc3): tabs from `drawer.tabs`, quick dial first and All last, with a floating
 * bar of their icons and a search button over the list (TabBar.kt), where the thumb is (lor-sqfe).
 * A list tab shows its apps in the file's order; All is A–Z with letter headers and a rail that
 * jumps to a letter, as Launcher3's all-apps fast scroller does. The search button grows into the
 * field, the tabs folding away; results run upward from it, the best nearest it, as Kvaesitso's
 * bottom search bar has them (GPL-3.0, ideas only: `SearchColumn`'s `reverseLayout`, padded by the
 * keyboard in an `adjustResize` window). One layout for both widths: at full width on the inner
 * screen, `look.grid.wide`'s columns and the controls at most 560 dp wide, centred.
 */

/** One cell or full-width line of the drawer's grid. Keys are unique. */
internal sealed interface Entry {
    val key: String

    /** A section's title, with [sub] beside it (quick dial's "Sunday evening"). */
    class Header(val title: String, override val key: String, val sub: String? = null) : Entry
    class Tile(val app: HomeApp, val dot: Boolean, override val key: String, val go: Boolean = false) : Entry
    /** A search result drawn as a row: a shortcut, a settings page, a contact (launcher polish 5.2 on). */
    class Row(val value: Any, override val key: String, val go: Boolean) : Entry
    /** A line saying why a tab is empty. */
    class Note(val text: String, override val key: String) : Entry
    /** Quick dial's card for what's next from LoR. */
    class Card(val card: QuickCard) : Entry {
        override val key = "quick-card"
    }
    /** Quick dial's people row. */
    class People(val people: List<SearchShortcut>) : Entry {
        override val key = "quick-people"
    }
    class HiddenApps(val count: Int) : Entry {
        override val key = "hidden-apps"
    }
}

/** The grid's entries for a tab, and All's letters with the index of each header. */
internal class Built(val entries: List<Entry>, val letters: List<Pair<String, Int>>)

/** What quick dial shows besides the app list: its card and people, read in [Drawer]. */
internal class QuickParts(val opens: AppOpens, val card: QuickCard?, val people: List<SearchShortcut>)

private fun build(tab: DrawerTab, apps: List<HomeApp>, file: LayoutFile?, stats: LaunchStats, quick: QuickParts, now: Long): Built {
    val launched = stats.entries
    // New, and not opened since it was installed: from the launcher, or anywhere usage events saw.
    // Main-profile apps only: a work-profile app's install time can't be read (see HomeApp.firstInstalled).
    fun dot(app: HomeApp) = app.mainProfile &&
        isNew(app.firstInstalled, maxOf(launched[app.key]?.last ?: 0, quick.opens.last[opensKey(app.id)] ?: 0), now)
    val out = mutableListOf<Entry>()
    fun header(title: String, sub: String? = null) = out.add(Entry.Header(title, "h${out.size}:$title", sub))
    fun tiles(prefix: String, list: List<HomeApp>) = list.forEach { out.add(Entry.Tile(it, dot(it), "$prefix:${it.key}")) }
    fun note(text: String) = out.add(Entry.Note(text, "n${out.size}"))
    val letters = mutableListOf<Pair<String, Int>>()
    when (tab.kind) {
        // Quick dial (lor-2imw): the card, the apps for now, the people, the new installs.
        TabKind.QUICK -> {
            quick.card?.let { out.add(Entry.Card(it)) }
            val zone = ZoneId.systemDefault()
            header("Now", HourSlot.of(Instant.ofEpochMilli(now), zone).words(Instant.ofEpochMilli(now), zone))
            val ranked = rankForNow(apps, { it.id }, quick.opens, now, zone)
            if (ranked.isEmpty()) note("The apps you open at this time of day will show here.")
            else tiles("now", ranked)
            if (quick.people.isNotEmpty()) {
                header("People")
                out.add(Entry.People(quick.people))
            }
            val new = apps.filter(::dot).sortedByDescending { it.firstInstalled }
            if (new.isNotEmpty()) { header("New"); tiles("new", new) }
        }
        TabKind.LIST -> {
            header(tab.name)
            val shown = tab.shown(apps) { it.id }
            if (shown.isEmpty()) note("No apps here yet. Long-press an app and choose Move to tab.")
            else tiles("t", shown)
        }
        TabKind.ALL -> {
            apps.groupBy { letterOf(it.label) }.forEach { (letter, list) ->
                letters.add(letter to out.size)
                header(letter)
                tiles("az", list)
            }
            val hidden = file?.hidden?.size ?: 0
            if (hidden > 0) out.add(Entry.HiddenApps(hidden))
        }
    }
    return Built(out, letters)
}

/**
 * The grid's entries for search results, for a grid laid out bottom up (the first entry nearest
 * the field): each block's entries best first, then its header, which a reversed grid draws above
 * them, when more than one block shows.
 */
internal fun searchEntries(results: Results<Any>, goKey: String?): Built {
    val out = mutableListOf<Entry>()
    val headers = results.blocks.size > 1
    results.blocks.forEach { block ->
        block.entries.forEach { e ->
            when (val v = e.value) {
                is HomeApp -> out.add(Entry.Tile(v, false, "q:${e.key}", go = e.key == goKey))
                else -> out.add(Entry.Row(v, "q:${e.key}", e.key == goKey))
            }
        }
        if (headers) out.add(Entry.Header(block.kind.title, "qh:${block.kind}"))
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
    val tabs = home.file?.drawer?.tabs ?: Drawer.DEFAULT_TABS
    val current = ui.tab.coerceIn(tabs.indices)
    val lor = LocalContext.current.applicationContext as LorApp
    // Quick dial's counts, read up to now each time the drawer opens (lor-2imw).
    val opens by lor.home.opens.collectAsStateWithLifecycle()
    LaunchedEffect(ui.drawer) { if (ui.drawer) lor.home.readUsage() }
    // Quick dial's card holds LoR's reader while it shows, as home's header does.
    val quickShown = ui.drawer && tabs[current].kind == TabKind.QUICK
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(quickShown, lifecycle) { if (quickShown) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { lor.reads.hold() } }
    val reads = lor.reads
    val card = quickCard(reads.home?.one, reads.current, reads.homeRead.at, ZoneId.systemDefault())
    val people = remember(apps, sources.shortcuts) { quickPeople(apps, sources, System.currentTimeMillis()) }
    val quick = QuickParts(opens, card, people)
    val open = ui.searchOpen
    // Something typed makes it a search, recorded with the text as typed.
    val searching = open && ui.query.isNotBlank()
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
    val fresh = results.query == ui.query
    // While the app list loads (launcher polish 3.2), nothing matches, but there's nothing to say yet.
    val nothing = searching && fresh && results.nothing && apps.isNotEmpty()
    val ways = remember(nothing, ui.query, sources.targets) { if (nothing) elsewhere(sources.targets, ui.query) else emptyList() }
    var focused by remember { mutableStateOf(false) }
    // The field's text and cursor; a chip or a home press changes ui.query, which puts the cursor at the end.
    var field by remember { mutableStateOf(TextFieldValue(ui.query, TextRange(ui.query.length))) }
    if (field.text != ui.query) field = TextFieldValue(ui.query, TextRange(ui.query.length))
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Opened with the field grown, when the file asks (launcher polish 5.5), once the drawer has
    // settled open (7.1), so a swipe that goes back down leaves no keyboard.
    val settledOpen by remember { derivedStateOf { ui.drawer && !ui.grid.follow.held && ui.drawerShown >= 1f } }
    LaunchedEffect(settledOpen) {
        if (settledOpen && home.file?.drawer?.keyboard == true) ui.searchOpen = true
    }
    // The field takes the keyboard as it grows; folded, the keyboard goes with it.
    LaunchedEffect(open) {
        if (open) {
            focus.requestFocus()
            keyboard?.show()
        } else if (focused) {
            keyboard?.hide()
            focusManager.clearFocus()
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
    val spots = remember(results) { HashMap<String, IconSpot>() }
    fun go() {
        val r = if (fresh) results else searchAll(ui.query, apps, sources, stats)
        val first = r.go
        if (first != null) openFound(first.value, at, home, spots["q:${first.key}"]) else elsewhere(sources.targets, ui.query).firstOrNull()?.let { home.actions.elsewhere(it, ui.query) }
    }
    // The tab's name, shown above the bar for a moment after a switch; the time makes each switch its own.
    var flash by remember { mutableStateOf<Pair<String, Long>?>(null) }
    LaunchedEffect(flash) {
        if (flash != null) {
            delay(FLASH_MS)
            flash = null
        }
    }
    val drag = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                // A drag on the list puts the keyboard away so the list can be read (as Kvaesitso does).
                if (focused) {
                    // Hidden as well as unfocused, so the keyboard goes whatever losing focus does.
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
                // A swipe down while the field is empty folds it back.
                if (available.y > 0 && ui.searchOpen && ui.query.isBlank()) ui.foldSearch()
                return Offset.Zero
            }
        }
    }
    // The list and the controls end at the keyboard: only the drawer lifts, never the grid (lor-sqfe).
    BoxWithConstraints(Modifier.fillMaxSize().background(Bg).swallowTaps().cellInsetsPadding().imePadding()) {
        val columns = LocalHomeLook.current.columnsFor(maxWidth.value).drawer
        val goKey = if (focused && fresh) results.go?.key else null
        val tab = tabs[current]
        val built = remember(tab, apps, home.file, stats, searching, results, goKey, quick.opens, quick.card, quick.people, ui.drawer) {
            if (searching) searchEntries(results, goKey)
            // Now is read again each time the drawer opens, so quick dial ranks for the hour it opens in.
            else build(tab, apps, home.file, stats, quick, System.currentTimeMillis())
        }
        // A tab's place and the results' are kept apart, so clearing a search goes back to where the tab was.
        val browse = rememberLazyGridState()
        val found = rememberLazyGridState()
        // A new tab starts at its top; new results at their best, by the field.
        LaunchedEffect(current) { browse.scrollToItem(0) }
        LaunchedEffect(results) { found.scrollToItem(0) }
        val grid = if (searching) found else browse
        // Read by the grid's finger at each swipe, so it asks the list on screen.
        val list by rememberUpdatedState(grid)
        val reversed by rememberUpdatedState(searching)
        val folds by rememberUpdatedState(open && ui.query.isBlank())
        // The floating controls' height, which the list keeps clear below its last row.
        var controlsPx by remember { mutableIntStateOf(0) }
        val controls = with(LocalDensity.current) { controlsPx.toDp() }
        LazyVerticalGrid(
            GridCells.Fixed(columns),
            // A swipe the list can't take, at its top or its end, moves the grid (lor-qdmy; 7.1's pull
            // down), except a swipe down while the search is open and empty, which folds it.
            Modifier.fillMaxSize().nestedScroll(drag).scrollsInGrid { (folds && it == Way.UP) || listTakes(it, list.canScrollForward, list.canScrollBackward, reversed) },
            state = grid,
            contentPadding = PaddingValues(start = 8.dp, end = if (built.letters.isEmpty()) 8.dp else 28.dp, top = 8.dp, bottom = controls + 28.dp),
            // Results run upward from the field, the best nearest it (Kvaesitso's bottom search bar).
            reverseLayout = searching,
            verticalArrangement = Arrangement.spacedBy(12.dp, if (searching) Alignment.Bottom else Alignment.Top),
        ) {
            items(built.entries, key = { it.key }, span = { if (it is Entry.Tile) GridItemSpan(1) else GridItemSpan(maxLineSpan) }) { e ->
                when (e) {
                    is Entry.Header -> SectionHeader(e.title, e.sub)
                    is Entry.Card -> QuickCardView(e.card, home.actions::calendar)
                    is Entry.People -> PeopleRow(e.people, at, home)
                    // An app of either profile can be picked up and dragged onto home (launcher polish 4.6, lor-72c3).
                    is Entry.Tile -> Box(
                        Modifier.dragSource(home.drag, e.key) { b -> Source(b, Slot.DrawerApp(e.app.key), 2) },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        // Go's target, while the field has focus (launcher polish 5.1): DESIGN.md's selection.
                        if (e.go) Box(Modifier.matchParentSize().selection())
                        MenuTile(e.app, at, home, dot = e.dot, icon = spots.getOrPut(e.key) { IconSpot() })
                    }
                    is Entry.Row -> ResultRow(e.value, e.go, at, home, spots.getOrPut(e.key) { IconSpot() })
                    is Entry.Note -> Text(
                        e.text,
                        style = Type.body.copy(color = Dim, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    is Entry.HiddenApps -> Text(
                        "Hidden apps (${e.count})",
                        style = Type.body.copy(color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().clickable { home.actions.settings() }.padding(vertical = 20.dp),
                    )
                }
            }
        }
        if (built.letters.isNotEmpty()) LetterRail(built.letters, browse, Modifier.align(Alignment.CenterEnd).padding(bottom = controls))
        // The list fades out under the controls, as the mock's does.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(controls + 24.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.6f to Bg.copy(alpha = 0.9f))),
        )
        // The tab's name, just above the bar.
        val name = flash?.first
        AnimatedVisibility(
            name != null && !open,
            Modifier.align(Alignment.BottomCenter).padding(bottom = BarHeight + 28.dp),
            enter = fadeIn(Motion.quick()),
            exit = fadeOut(Motion.move()),
        ) {
            Text((name ?: "").uppercase(), style = Type.label.copy(color = Mute, fontSize = 12.sp, letterSpacing = 0.1.em))
        }
        // The controls, upward from the bottom: the bar with the search button, or the field; above
        // the field, the recent searches while it is empty, the answer, or the ways out of an empty
        // search. At most 560 dp wide, centred, within the thumb's reach.
        Column(
            Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).fillMaxWidth().padding(bottom = 16.dp).onSizeChanged { controlsPx = it.height },
        ) {
            if (nothing) NoResults(ui.query, ways, line = results.answer == null) { home.actions.elsewhere(it, ui.query) }
            // A quick answer (launcher polish 5.4) is the result nearest the field; it opens nothing, so Go passes it by.
            if (searching) results.answer?.let { AnswerCard(it) }
            // Recent searches, while the field is open and empty (launcher polish 5.5).
            if (open && !searching && stats.searches.isNotEmpty()) {
                RecentSearches(stats.searches, { ui.query = it }, home.actions::clearSearches)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), contentAlignment = Alignment.Center) {
                // The field grows out of the button as the tabs fold away.
                val grown by animateFloatAsState(if (open) 1f else 0f, Motion.move(), label = "search")
                val full = maxWidth
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(!open, enter = fadeIn(Motion.move()) + expandHorizontally(Motion.move()), exit = fadeOut(Motion.quick()) + shrinkHorizontally(Motion.move())) {
                        Row {
                            // Search keeps its size; the tabs take what is left (lor-81d4).
                            TabBar(tabs, current, tabWidth(full.value, tabs.size).dp) { i ->
                                ui.tab = i
                                flash = tabs[i].name to System.nanoTime()
                            }
                            Spacer(Modifier.width(BarSize.GAP.dp))
                        }
                    }
                    if (open) {
                        SearchField(
                            lerp(BarHeight, full, grown),
                            field,
                            { field = it; ui.query = it.text },
                            focus,
                            { focused = it },
                            ::go,
                            ui::foldSearch,
                        )
                    } else {
                        SearchButton { ui.searchOpen = true }
                    }
                }
            }
        }
    }
}

/** How long a tab's name shows above the bar after a switch. */
private const val FLASH_MS = 1200L

/** The recent searches as chips, with Clear last; a tap types one in. */
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
            .background(Bg.copy(alpha = 0.9f))
            .border(1.dp, edge, RectangleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun SectionHeader(title: String, sub: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp).semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.Bottom) {
        Text(title.uppercase(), style = Type.title, modifier = Modifier.weight(1f))
        sub?.let { Text(it, style = Type.body.copy(color = Dim, fontSize = 12.sp)) }
    }
}

/**
 * Quick dial's people (lor-2imw): the conversation shortcuts search holds, cached ones included,
 * of apps the drawer lists, by [recentPeople].
 */
internal fun quickPeople(apps: List<HomeApp>, sources: SearchSources, now: Long): List<SearchShortcut> {
    val owners = apps.map { Owner(it.packageName, it.id.className, it.user.toString()) to it }
    val chats = sources.shortcuts.mapNotNull { f ->
        val hit = f.value as ShortcutHit
        if (!hit.conversation) return@mapNotNull null
        val i = hit.info
        ownerOf(owners, i.`package`, i.activity?.className, i.userHandle.toString())?.let { Chat(hit.label, i.lastChangedTimestamp, SearchShortcut(hit, it), manifest = i.isDeclaredInManifest, rank = i.rank) }
    }
    return recentPeople(chats, now).map { it.value }
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
