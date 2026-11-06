package com.spadaval.lor.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spadaval.lor.agent.AgentPage
import com.spadaval.lor.collect.LaunchFrom
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/*
 * The home shell (T3 Launcher, Increment 3.2): two pages, the agent page left of home; home draws
 * the layout file's apps, folders and dock; a swipe up opens the drawer with search.
 *
 * The shape follows Launcher3 / Lawnchair (Apache-2.0): a workspace with the -1 page on its left
 * (there `LauncherOverlay`, here a pager page), a hotseat (`Hotseat.java`) fixed under the grid, a
 * folder icon previewing its first four apps on a 2x2 grid (`FolderIcon`, `ClippedFolderIconLayoutRule`)
 * that opens as a panel over a scrim, and all apps reached by a swipe up with search at its top,
 * where the keyboard's action opens the first result (`AllAppsSearchBarController.onEditorAction`).
 * A home press closes whatever is open and returns to home, as `Launcher.onNewIntent` does.
 * Motion is one-shot only: the drawer slides in and out, nothing loops (strategy: calm).
 */

internal val Ink = Color(0xFFECEBE6)
internal val Muted = Color(0xFF9097A1)
internal val Dim = Color(0xFF616871)
internal val Line = Color(0x14FFFFFF)
private val Panel = Color(0xF215181D)
internal val Sheet = Color(0xFF0B0D10)
internal val Amber = Color(0xFFF08A2C)
/** The backing of a state drawn on home (the broken banner, a home note, the set-up card), so it reads on any wallpaper. */
internal val StateFill = Color(0xDD15181D)
internal val LabelStyle = TextStyle(color = Ink, fontSize = 12.sp, shadow = Shadow(Color(0x99000000), Offset(0f, 1f), 3f))

/** What the activity can reset on a home press. */
class HomeUi {
    var drawer by mutableStateOf(false)
    /**
     * The open folder: its place in the file (launcher polish 4.1: by place, since names may
     * repeat) and its name, so an edit that moves another item into that place closes it.
     */
    var folder by mutableStateOf<OpenFolder?>(null)
    var query by mutableStateOf("")
    var homePresses by mutableStateOf(0)
    /** Where on home a long press on empty space opened the home menu. */
    var homeMenu by mutableStateOf<Offset?>(null)
    /** The home page that long press was on, so only that page draws the menu. */
    var homeMenuPage by mutableStateOf(0)
    /** The Undo bar after an edit (launcher polish 4.2). */
    val undo = UndoBarState()
    /** What is held, and edit mode (launcher polish 4.3). */
    val drag = DragState()
    /** The pager's page showing: 0 is the agent page, 1 is home, 2 on are the pages after it. */
    var page by mutableStateOf(1)

    /** Home with nothing open over it: a home press here opens quick send instead. */
    val atRest: Boolean get() = !drawer && folder == null && homeMenu == null && page == 1 && !drag.editing && drag.held == null

    fun homePressed() {
        // A home press puts a held item back and leaves edit mode.
        drag.held = null
        drag.editing = false
        drawer = false
        folder = null
        homeMenu = null
        query = ""
        homePresses++
    }
}

/** A folder with its installed apps, each with its place in the file; [cover] is its cover app if installed. */
data class OpenFolder(val spot: Spot.Page, val name: String)

class ResolvedFolder(val folder: Folder, val at: From.At, val apps: List<Pair<HomeApp, From.At>>, val cover: HomeApp?)

/** A layout item with its installed apps and its place in the file; refs to apps that aren't installed are left out. */
internal sealed interface Placed {
    val at: From.At
    class App(val app: HomeApp, override val at: From.At) : Placed
    class InFolder(val folder: ResolvedFolder) : Placed {
        override val at get() = folder.at
    }
}

/** Home's pages and dock as drawn: each item with the place it holds in the file. */
internal class PlacedHome(val pages: List<List<Placed>>, val dock: List<Pair<HomeApp, From.At>>) {
    val count: Int get() = pages.sumOf { it.size }
}

private fun resolve(ref: AppRef, apps: List<HomeApp>): HomeApp? =
    apps.firstOrNull { it.mainProfile && ref.matches(it.packageName, it.info.componentName.className) }

private fun place(layout: Layout, apps: List<HomeApp>): PlacedHome {
    val pages = layout.allPages.mapIndexed { p, items ->
        items.mapIndexedNotNull { i, item ->
            val at = From.At(Spot.Page(p, i), item)
            when (item) {
                is AppRef -> resolve(item, apps)?.let { Placed.App(it, at) }
                is Folder -> item.apps.mapIndexedNotNull { a, ref -> resolve(ref, apps)?.let { it to From.At(Spot.InFolder(p, i, a), ref) } }
                    .takeIf { it.isNotEmpty() }
                    ?.let { Placed.InFolder(ResolvedFolder(item, at, it, item.cover?.let { c -> resolve(c, apps) })) }
            }
        }
    }
    return PlacedHome(pages, layout.dock.mapIndexedNotNull { i, ref -> resolve(ref, apps)?.let { it to From.At(Spot.Dock(i), ref) } })
}

@Composable
fun HomeScreen(apps: List<HomeApp>, appsLoad: AppsLoad, layout: LayoutState, reads: Int, stats: LaunchStats, ui: HomeUi, actions: HomeActions) {
    // While the file is broken, everything draws from the last good layout (launcher polish 3.1).
    // While an item is held, home draws the drag's working copy; just after a drop, what it wrote (launcher polish 4.3).
    val drag = ui.drag
    val held = drag.held
    val file = held?.work ?: drag.landing ?: layout.shown
    val home = remember(actions, file, apps) { HomeContext(actions, file, apps, drag) }
    val placed = remember(apps, file) { file?.let { place(it.current, apps) } }
    val folder = ui.folder?.let { open ->
        placed?.pages?.getOrNull(open.spot.page)?.firstNotNullOfOrNull { (it as? Placed.InFolder)?.folder?.takeIf { f -> f.at.spot == open.spot && f.folder.name == open.name } }
    }
    // An edit that empties the open folder removes it, so the panel closes.
    LaunchedEffect(ui.folder, folder, placed) { if (ui.folder != null && folder == null && placed != null) ui.folder = null }
    // While something is held, an empty page follows the last, for a drop that makes one (launcher polish 4.5).
    val filePages = file?.current?.allPages?.size ?: 1
    val pageCount = filePages + if (held != null && held.work.current.pages.size < LayoutFile.MAX_PAGES) 1 else 0
    // A file changed from outside (an agent, adb) may hold another folder at the open one's place.
    LaunchedEffect(reads) { ui.folder = null }
    val pager = rememberPagerState(initialPage = 1) { 1 + pageCount }
    // A target waits 300 ms under the finger before the working copy takes it (Kvaesitso's attemptMove).
    LaunchedEffect(held, held?.pending) {
        val t = held?.pending ?: return@LaunchedEffect
        delay(300)
        held.step(t)
        held.pending = null
    }
    // Held at the screen's edge for 500 ms, the pager turns a page that way, never to the agent page (launcher polish 4.5).
    LaunchedEffect(held, held?.edge) {
        val e = held?.edge ?: 0
        if (e == 0) return@LaunchedEffect
        while (true) {
            delay(500)
            val to = pager.currentPage + e
            if (to !in 1 until pager.pageCount) break
            // A turn once started finishes, or the pager would stop between pages (user scroll is off while held).
            withContext(NonCancellable) { pager.animateScrollToPage(to) }
        }
    }
    fun dropHeld(h: Held, t: Target?) {
        drag.held = null
        var final = h.work
        // A drawer app is added wherever it lands, even after steps put it in the working copy.
        var said: String? = if (h.fromDrawer) "Added." else "Moved."
        if (t != null) {
            try {
                final = h.work.drop(h.from, t, h.home.installed, strip = false, nameFor = h.home::nameFor).file
                said = dropMessage(t, if (h.fromDrawer) null else h.spot, h.work)
            } catch (e: LayoutError) {
                actions.refused(e.message ?: "it can't go there")
            }
        }
        final = final.withCurrent(final.current.stripEmptyPages())
        if (final == h.original) return
        drag.landing = final
        actions.edit(said ?: "Added.", after = { drag.landing = null }) { now ->
            if (now != h.original) throw LayoutError(HOME_CHANGED)
            final
        }
    }
    LaunchedEffect(ui.homePresses) { if (ui.homePresses > 0) pager.animateScrollToPage(1) }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { ui.page = it } }
    BackHandler(enabled = ui.folder != null || ui.drawer || pager.currentPage != 1 || drag.editing) {
        when {
            ui.folder != null -> ui.folder = null
            drag.editing -> drag.editing = false
            ui.drawer -> { ui.drawer = false; ui.query = "" }
            else -> ui.homePresses++
        }
    }
    // The drag handler lives across recompositions, so it reads the latest of these.
    val latestHome by rememberUpdatedState(home)
    val latestDrop by rememberUpdatedState<(Held, Target?) -> Unit>(::dropHeld)
    Box(Modifier.fillMaxSize().dragging(ui, { latestHome }, { h, t -> latestDrop(h, t) })) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, userScrollEnabled = held == null) { page ->
            if (page == 0) AgentPage() else HomePage(page - 1, placed, layout, appsLoad, ui, home)
        }
        PageDots(pager.currentPage, 1 + pageCount, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 96.dp))
        AnimatedVisibility(
            ui.drawer,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            Drawer(remember(apps, home) { apps.filterNot(home::hidden) }, stats, ui, home)
        }
        folder?.let { FolderPanel(it, home, onClose = { ui.folder = null }) }
        // Edit mode's bar, and Remove while something is held (launcher polish 4.3).
        TopBar(ui, Modifier.align(Alignment.TopCenter).safeDrawingPadding())
        held?.let { HeldItem(it) }
        // Above the drawer and folders, clear of the dock.
        UndoBar(ui.undo, actions::undo, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 112.dp))
    }
}

@Composable
private fun HomePage(n: Int, placed: PlacedHome?, layout: LayoutState, appsLoad: AppsLoad, ui: HomeUi, home: HomeContext) {
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    var dragged by remember { mutableStateOf(0f) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(onDragStart = { dragged = 0f }) { change, dy ->
                    dragged += dy
                    if (dragged < -threshold && !ui.drawer) {
                        change.consume()
                        ui.drawer = true
                    }
                }
            }
            .safeDrawingPadding(),
    ) {
        // Under everything: a long press that no app or folder took, on empty home, opens the home
        // menu. Overlapping siblings get a touch only when the ones drawn above it don't.
        val haptics = LocalHapticFeedback.current
        Box(
            Modifier.matchParentSize().pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    ui.homeMenu = it
                    ui.homeMenuPage = n
                })
            },
        )
        val wide = maxWidth >= 600.dp
        // Only home's first page has the date; the pages after it start where its grid starts.
        val header = @Composable { if (n == 0) DateHeader(if (wide) 40 else 30) else Spacer(Modifier.height(if (wide) 0.dp else 104.dp)) }
        val body = @Composable { mod: Modifier ->
            Column(mod) {
                val columns = if (wide) 5 else 4
                if (n == 0 && layout is LayoutState.Broken) {
                    BrokenBanner(
                        layout, appsLoad == AppsLoad.LOADED, TextStyle(color = Ink, fontSize = 13.sp),
                        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth()
                            .background(StateFill, RoundedCornerShape(14.dp)).border(1.dp, Line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp),
                        home.actions::restore,
                    )
                }
                // Until the first run is done (launcher polish 3.5); the default layout is drawn under it.
                if (n == 0) SetupCard(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp))
                val body = homeBody(appsLoad, home.file, placed?.count ?: 0)
                val drops = body == HomeBody.GRID || body == HomeBody.EMPTY
                // The page's space, from its first row to the dock, takes a drop after its last item.
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .dropZone(home.drag, "page$n") { b ->
                            // The page past the file's last is the new one a drag shows.
                            val place = if (n >= (home.file?.current?.allPages?.size ?: 1)) Place.NewPage else Place.OnPage(n)
                            if (drops) Zone.Area(place, b) else null
                        },
                ) {
                    when (body) {
                        HomeBody.NOTHING -> {}
                        HomeBody.SKELETON -> Skeleton(home.file!!.current.allPages.getOrElse(n) { emptyList() }, columns)
                        HomeBody.APPS_DIDNT_LOAD -> if (n == 0) HomeNote("Apps didn't load. Trying again.", null)
                        HomeBody.EMPTY -> if (n == 0) HomeNote("Home is empty.", "Swipe up for your apps, then drag one here, or long-press it and choose Add to home.") { ui.drawer = true }
                        HomeBody.GRID -> HomeGrid(placed!!.pages.getOrElse(n) { emptyList() }, columns, ui, home)
                    }
                }
                if (body == HomeBody.SKELETON) DockSkeleton(home.file!!.current.dock.size) else Dock(placed?.dock.orEmpty(), home, n)
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { header() }
                body(Modifier.weight(1f).padding(top = 24.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                header()
                body(Modifier.weight(1f))
            }
        }
        ui.homeMenu?.takeIf { ui.homeMenuPage == n }?.let { HomeMenu(it, home, onEdit = { ui.drag.editing = true }) { ui.homeMenu = null } }
    }
}

/**
 * Home while the app list loads (launcher polish 3.2): a still slot where each of the file's items
 * will be, the size of its icon, so nothing moves when the icons arrive. Nothing shimmers (calm).
 */
@Composable
private fun Skeleton(items: List<LayoutItem>, columns: Int) {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.take(LayoutFile.MAX_HOME).chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.width(76.dp).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(52.dp).background(Line, if (item is Folder) RoundedCornerShape(15.dp) else CircleShape))
                            // The label's height, so the rows sit where the tiles will.
                            Spacer(Modifier.height(5.dp))
                            Text("", style = LabelStyle)
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DockSkeleton(count: Int) {
    if (count == 0) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .background(Color(0x10FFFFFF), RoundedCornerShape(22.dp))
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(count) { Box(Modifier.size(48.dp).background(Line, CircleShape)) }
    }
}

/** A line or two in home's grid area, on a card over the wallpaper; [onClick], if any, makes it a button. */
@Composable
private fun HomeNote(title: String, detail: String?, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .background(StateFill, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, style = TextStyle(color = Ink, fontSize = 16.sp))
        detail?.let { Text(it, style = TextStyle(color = Muted, fontSize = 14.sp), modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun DateHeader(size: Int) {
    val today = rememberToday().format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
    Text(today,style = LabelStyle.copy(fontSize = size.sp), modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 28.dp))
}

/**
 * Today's date, read again when home resumes and when the system says the date, time or zone
 * changed, so the header turns over at midnight while home is on screen.
 */
@Composable
private fun rememberToday(): LocalDate {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var today by remember { mutableStateOf(LocalDate.now()) }
    DisposableEffect(context, lifecycle) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                today = LocalDate.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val resumed = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now() }
        lifecycle.addObserver(resumed)
        onDispose {
            context.unregisterReceiver(receiver)
            lifecycle.removeObserver(resumed)
        }
    }
    return today
}

/** Home's items in reading order, [columns] to a row: Launcher3's cells without coordinates. */
@Composable
private fun HomeGrid(items: List<Placed>, columns: Int, ui: HomeUi, home: HomeContext) {
    val icon = rememberTileIcon()
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    val spot = item.at.spot
                    Box(
                        Modifier.weight(1f)
                            .dropZone(home.drag, spot) { b -> Zone.Item(spot, b, b.top + icon.top, icon.size) }
                            .dragSource(home.drag, spot) { b ->
                                Source(b, item.at, 0, (item as? Placed.App)?.app, (item as? Placed.InFolder)?.folder)
                            }
                            .alpha(if (home.drag.held?.spot == spot) 0f else 1f)
                            // DESIGN.md's selection on the item a drop would go onto.
                            .then(
                                if (home.drag.held?.lit == Target.Onto(spot as Spot.Page)) {
                                    Modifier.background(Color(0x14F08A2C), RoundedCornerShape(16.dp)).border(1.dp, Color(0x8CF08A2C), RoundedCornerShape(16.dp))
                                } else Modifier,
                            ),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        when (item) {
                            is Placed.App -> MenuTile(item.app, At(LaunchFrom.HOME), home, spot = item.at)
                            is Placed.InFolder -> FolderTile(item.folder, home) { ui.folder = OpenFolder(item.folder.at.spot as Spot.Page, item.folder.folder.name) }
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Dock(apps: List<Pair<HomeApp, From.At>>, home: HomeContext, page: Int) {
    // An empty dock shows while something is held, so it can take it.
    if (apps.isEmpty() && home.drag.held == null) return
    val drag = home.drag
    val iconPx = with(LocalDensity.current) { 48.dp.toPx() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .background(Color(0x10FFFFFF), RoundedCornerShape(22.dp))
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .dropZone(drag, "dock$page") { b -> Zone.Area(Place.Dock, b) }
            .padding(vertical = 10.dp)
            .height(48.dp),
    ) {
        val at = At(LaunchFrom.DOCK)
        apps.forEach { (app, spot) ->
            // Equal cells, so the space between icons belongs to one of them.
            Box(
                Modifier.weight(1f)
                    // Keyed by page too: the dock is drawn on every page, and neighbours stay composed.
                    .dropZone(drag, page to spot.spot) { b -> Zone.Item(spot.spot, b, b.top, iconPx) }
                    .dragSource(drag, page to spot.spot) { b -> Source(b, spot, 0, app, null) }
                    .alpha(if (drag.held?.spot == spot.spot) 0f else 1f),
                contentAlignment = Alignment.Center,
            ) {
                WithAppMenu(app, at, home, spot) { menu ->
                    AppIcon(
                        app, 48.dp,
                        Modifier.semantics { contentDescription = app.label }
                            .combinedClickable(onLongClickLabel = "Menu", onLongClick = menu) { if (!drag.editing) home.actions.open(app, at) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(current: Int, count: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until count).forEach { page ->
            val color = if (page == current) Color(0xB0FFFFFF) else Color(0x30FFFFFF)
            Box(Modifier.size(6.dp).background(color, if (page == 0) RoundedCornerShape(2.dp) else CircleShape))
        }
    }
}

/** An app tile whose tap opens it from [at] and whose long press opens its menu. */
@Composable
internal fun MenuTile(app: HomeApp, at: At, home: HomeContext, width: Dp = 76.dp, dot: Boolean = false, spot: From.At? = null, beforeOpen: () -> Unit = {}) {
    WithAppMenu(app, at, home, spot) { menu ->
        // In edit mode a tap opens nothing (launcher polish 4.3).
        AppTile(app, width, menu, dot) { if (!home.drag.editing) { beforeOpen(); home.actions.open(app, at) } }
    }
}

@Composable
internal fun AppTile(app: HomeApp, width: Dp = 76.dp, onLongClick: (() -> Unit)? = null, dot: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.width(width).combinedClickable(onLongClickLabel = onLongClick?.let { "Menu" }, onLongClick = onLongClick, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AppIcon(app, 52.dp)
            // A new app's dot (launcher polish 2.2), until it is first opened from the launcher.
            if (dot) Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(Amber, CircleShape).border(1.5.dp, Sheet, CircleShape))
        }
        Spacer(Modifier.height(5.dp))
        Text(app.label, style = LabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FolderTile(folder: ResolvedFolder, home: HomeContext, onClick: () -> Unit) {
    // A long press opens the folder's menu (launcher polish 4.4); moving on from it picks the folder up.
    var menu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val held = home.drag.held
    LaunchedEffect(held) { if (held != null) menu = false }
    if (menu) FolderMenu(folder, home) { menu = false }
    Column(
        Modifier.width(76.dp)
            .combinedClickable(onLongClickLabel = "Menu", onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menu = true
            }, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FolderIcon(folder)
        Spacer(Modifier.height(5.dp))
        Text(folder.folder.name, style = LabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A folder's icon: its cover app (launcher polish 4.1), else its first four apps. */
@Composable
private fun FolderIcon(folder: ResolvedFolder) {
    if (folder.cover != null) {
            AppIcon(folder.cover, 52.dp)
        } else Column(
            Modifier
                .size(52.dp)
                .background(Color(0x18FFFFFF), RoundedCornerShape(15.dp))
                .border(1.dp, Line, RoundedCornerShape(15.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            folder.apps.take(4).map { it.first }.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { pair.forEach { AppIcon(it, 18.dp) } }
            }
        }
}

@Composable
private fun FolderPanel(folder: ResolvedFolder, home: HomeContext, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).pointerInput(onClose) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        val drag = home.drag
        val icon = rememberTileIcon()
        val spot = folder.at.spot as Spot.Page
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .padding(24.dp)
                .background(Panel, RoundedCornerShape(24.dp))
                .border(1.dp, Line, RoundedCornerShape(24.dp))
                // The card takes a drop after the folder's last app (launcher polish 4.4).
                .dropZone(drag, "card") { b -> Zone.Area(Place.InFolder(spot), b) }
                .swallowTaps()
                .padding(16.dp),
        ) {
            Text(folder.folder.name, style = TextStyle(color = Ink, fontSize = 18.sp), modifier = Modifier.padding(start = 6.dp))
            folder.folder.reason?.let {
                Text(it, style = TextStyle(color = Muted, fontSize = 13.sp), modifier = Modifier.padding(start = 6.dp, top = 4.dp))
            }
            Spacer(Modifier.height(14.dp))
            folder.apps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    row.forEach { (app, at) ->
                        Box(
                            Modifier.weight(1f)
                                .dropZone(drag, at.spot) { b -> Zone.Item(at.spot, b, b.top + icon.top, icon.size) }
                                .dragSource(drag, at.spot) { b -> Source(b, at, 1, app, null) }
                                .alpha(if (drag.held?.spot == at.spot) 0f else 1f),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            MenuTile(app, At(LaunchFrom.FOLDER, folder.folder.name), home, width = 72.dp, spot = at, beforeOpen = onClose)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * Keeps taps from reaching what is under a panel. Unlike an empty `clickable`, it adds no
 * semantics, so the panel's apps stay separate nodes for TalkBack and agent-device.
 */
internal fun Modifier.swallowTaps(): Modifier = pointerInput(Unit) { detectTapGestures {} }

/** Where an app or folder tile's icon sits in its cell: 4 dp down, 52 dp across. */
private class TileIcon(val top: Float, val size: Float)

@Composable
private fun rememberTileIcon(): TileIcon {
    val density = LocalDensity.current
    return remember(density) { with(density) { TileIcon(4.dp.toPx(), 52.dp.toPx()) } }
}

private fun Rect.toBounds() = Bounds(left, top, right, bottom)

/** Reports where this is as a drop zone while it is laid out; null for none. */
@Composable
internal fun Modifier.dropZone(drag: DragState, key: Any, zone: (Bounds) -> Zone?): Modifier {
    val k = "zone" to key
    DisposableEffect(k) { onDispose { drag.zones.remove(k) } }
    return onGloballyPositioned { c ->
        val z = zone(c.boundsInRoot().toBounds())
        if (z != null) drag.zones[k] = z else drag.zones.remove(k)
    }
}

/** Reports where this is as something a long press and a move can pick up. */
@Composable
internal fun Modifier.dragSource(drag: DragState, key: Any, source: (Bounds) -> Source?): Modifier {
    val k = "source" to key
    DisposableEffect(k) { onDispose { drag.sources.remove(k) } }
    return onGloballyPositioned { c ->
        val s = source(c.boundsInRoot().toBounds())
        if (s != null) drag.sources[k] = s else drag.sources.remove(k)
    }
}

/**
 * Follows a drag from the press that starts it to its drop (launcher polish 4.3), in the Initial
 * pass so nothing under it (the pager, the drawer swipe, a list's scroll) sees the moves it takes.
 * A press on a source that is held past the long press (its menu opens) and then moves 16 dp picks
 * the source up; a lift or a move past the touch slop before the long press leaves the press to
 * whatever is under it.
 */
private fun Modifier.dragging(ui: HomeUi, home: () -> HomeContext, drop: (Held, Target?) -> Unit): Modifier = pointerInput(Unit) {
    val drag = ui.drag
    val start = 16.dp.toPx()
    val edge = 24.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val layer = when {
            ui.drawer -> 2
            ui.folder != null -> 1
            else -> 0
        }
        val source = drag.sourceAt(down.position, layer) ?: return@awaitEachGesture
        val file = home().file ?: return@awaitEachGesture
        if (drag.held != null) return@awaitEachGesture
        // Up to the long press: a lift or a move past the slop is a tap, a scroll or a swipe.
        val early = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var ended = false
            while (!ended) {
                val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                ended = c == null || !c.pressed || (c.position - down.position).getDistance() > viewConfiguration.touchSlop
            }
            true
        }
        if (early != null) return@awaitEachGesture
        // The menu is open; 16 dp more picks the item up and closes it.
        while (true) {
            val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            if (!c.pressed) return@awaitEachGesture
            c.consume()
            if ((c.position - down.position).getDistance() > start) {
                val item = when (val f = source.from) {
                    is From.At -> f.item
                    is From.Drawer -> f.ref
                }
                drag.held = Held(file, source.from, item, source.app, source.folder, home()).also { it.finger = c.position }
                if (source.layer == 2) {
                    ui.drawer = false
                    ui.query = ""
                }
                break
            }
        }
        val held = drag.held ?: return@awaitEachGesture
        var outside = 0L
        while (true) {
            val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
            // A home press put it back.
            if (drag.held !== held) return@awaitEachGesture
            if (c == null) {
                drag.held = null
                return@awaitEachGesture
            }
            c.consume()
            held.finger = c.position
            val width = size.width.toFloat()
            val zones = drag.zones.values.filter { !it.box.empty && it.box.centerX in 0f..width }
            val t = targetAt(c.position.x, c.position.y, zones, held.isFolder, ui.folder?.spot, held.spot)
            if (!c.pressed) {
                drop(held, t)
                return@awaitEachGesture
            }
            if (t is Target.Onto || t == Target.Remove) {
                held.pending = null
                held.lit = t.takeIf { held.takes(it) }
            } else {
                held.lit = null
                held.pending = t
            }
            // Out of an open folder's card for 400 ms closes it (Launcher3's ON_EXIT_CLOSE_DELAY).
            val card = ui.folder?.let { open -> zones.firstOrNull { (it as? Zone.Area)?.place == Place.InFolder(open.spot) } }
            if (card != null && !card.box.contains(c.position.x, c.position.y)) {
                if (outside == 0L) outside = c.uptimeMillis else if (c.uptimeMillis - outside >= 400) ui.folder = null
            } else outside = 0L
            held.edge = when {
                c.position.x < edge -> -1
                c.position.x > width - edge -> 1
                else -> 0
            }
        }
    }
}

/** The held item, under the finger, above everything. */
@Composable
private fun HeldItem(held: Held) {
    val density = LocalDensity.current
    val half = with(density) { 26.dp.toPx() }
    Box(Modifier.offset { IntOffset((held.finger.x - half).roundToInt(), (held.finger.y - half).roundToInt()) }) {
        when {
            held.app != null -> AppIcon(held.app, 52.dp)
            held.folder != null -> FolderIcon(held.folder)
        }
    }
}

/** The top bars' backing: opaque, since they sit over the date header. */
private val BarFill = Color(0xFF15181D)

/** Edit mode's bar, or Remove while something is held. */
@Composable
private fun TopBar(ui: HomeUi, modifier: Modifier) {
    val drag = ui.drag
    val held = drag.held
    if (held == null && !drag.editing) return
    val shape = RoundedCornerShape(14.dp)
    if (held != null) {
        val lit = held.lit == Target.Remove
        Box(
            modifier.padding(12.dp).fillMaxWidth().height(56.dp)
                .background(BarFill, shape)
                .background(if (lit) Color(0x14F08A2C) else Color.Transparent, shape)
                .border(1.dp, if (lit) Color(0x8CF08A2C) else Line, shape)
                .dropZone(drag, "remove") { b -> Zone.Remove(b) },
            contentAlignment = Alignment.Center,
        ) {
            Text("Remove", style = TextStyle(color = if (lit) Ink else Muted, fontSize = 15.sp))
        }
        return
    }
    Row(
        modifier.padding(12.dp).fillMaxWidth().background(BarFill, shape).border(1.dp, Line, shape).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Drag apps to move them. Drop one on another to make a folder.",
            style = TextStyle(color = Ink, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.padding(start = 8.dp).background(Amber, RoundedCornerShape(10.dp)).clickable { drag.editing = false }
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text("Done", style = TextStyle(color = Color(0xFF1A0D02), fontSize = 15.sp))
        }
    }
}

/** A folder's menu (launcher polish 4.4): Rename…, Cover app…, Remove from home. */
@Composable
private fun FolderMenu(folder: ResolvedFolder, home: HomeContext, close: () -> Unit) {
    var renaming by remember { mutableStateOf(false) }
    var covering by remember { mutableStateOf(false) }
    val at = folder.at
    val name = folder.folder.name
    when {
        renaming -> {
            var text by remember { mutableStateOf(name) }
            AlertDialog(
                onDismissRequest = close,
                title = { Text("Rename folder") },
                text = { OutlinedTextField(text, { text = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = { close(); if (text.trim() != name) home.actions.edit("$name is ${text.trim()} now.") { it.renameFolder(at, text) } }) { Text("Rename") }
                },
                dismissButton = { TextButton(onClick = close) { Text("Cancel") } },
            )
        }
        covering -> DropdownMenu(true, close, containerColor = Color(0xFF1B1F25), shape = RoundedCornerShape(16.dp)) {
            val cover = folder.folder.cover
            folder.apps.forEach { (app, a) ->
                val ref = a.item as AppRef
                FolderMenuItem(if (ref == cover) "${app.label} ✓" else app.label) {
                    close(); if (ref != cover) home.actions.edit("${app.label} is $name's cover.") { it.setCover(at, ref) }
                }
            }
            FolderMenuItem(if (cover == null) "None (show four) ✓" else "None (show four)") {
                close(); if (cover != null) home.actions.edit("$name shows four apps.") { it.setCover(at, null) }
            }
        }
        else -> DropdownMenu(true, close, containerColor = Color(0xFF1B1F25), shape = RoundedCornerShape(16.dp)) {
            FolderMenuItem("Rename…") { renaming = true }
            FolderMenuItem("Cover app…") { covering = true }
            FolderMenuItem("Remove from home") { close(); home.actions.edit("$name is off home.") { it.removeAt(at) } }
        }
    }
}

@Composable
private fun FolderMenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, style = TextStyle(color = Ink, fontSize = 15.sp)) }, onClick = onClick)
}
