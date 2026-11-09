package com.spadaval.lor.home

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.spadaval.lor.ui.*
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
import androidx.compose.runtime.key
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.round
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.lerp
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import com.spadaval.lor.ui.Motion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.layoutId
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
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
import com.spadaval.lor.lor.LorBlock
import com.spadaval.lor.lor.LorPanel
import com.spadaval.lor.collect.LaunchFrom
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/*
 * The home shell (T3 Launcher, Increment 3.2), drawn from the layout's grid of panels since
 * lor-qdmy ([GridHost.kt]): the agent page left of home, home's pages to its right, the drawer with
 * search under home; stacks draw the layout file's apps, folders, and header by block.
 *
 * The shape follows Launcher3 / Lawnchair (Apache-2.0): a workspace with the -1 page on its left
 * (there `LauncherOverlay`, here the `t3` panel left of home), a hotseat (`Hotseat.java`) fixed under the grid, a
 * folder icon previewing its first four apps on a 2x2 grid (`FolderIcon`, `ClippedFolderIconLayoutRule`)
 * that opens over the wallpaper (full screen and blurred since lor-gnk0, as Smart Launcher's), and all apps reached by a swipe up with search at its top,
 * where the keyboard's action opens the first result (`AllAppsSearchBarController.onEditorAction`).
 * A home press closes whatever is open and returns to home, as `Launcher.onNewIntent` does.
 * Motion is one-shot only: cells slide in and out with the finger, nothing loops (strategy: calm).
 */

/** Home's look from `layout.json` (launcher polish 6.4), for everything home and the drawer draw. */
val LocalHomeLook = staticCompositionLocalOf { HomeLook() }

/** The `package#user` keys of apps with a notification dot (launcher polish 6.6). */
val LocalDots = staticCompositionLocalOf { emptySet<String>() }

/**
 * A dot on an icon's corner: a new app (2.2) or a notification (6.6), one dot either way. Neutral,
 * Starlight edged in Deep Space (Sumeet, 2026-11-06), so amber keeps its one job.
 */
@Composable
internal fun BoxScope.Dot() {
    Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(Txt, CircleShape).border(1.5.dp, Bg, CircleShape))
}

/** The icon size the look asks for. */
internal val iconSize: Dp @Composable get() = LocalHomeLook.current.iconSize.dp

/** An app's or folder's label in the ink of what it stands on (launcher polish 6.3: the wallpaper's on home). */
internal val labelStyle: TextStyle
    @Composable get() = LocalWallInk.current.let { TextStyle(fontFamily = Saira, color = it.text, fontSize = LocalHomeLook.current.labelSize.sp, shadow = it.shadow) }

/** What the activity can reset on a home press. */
class HomeUi {
    /**
     * Where home is on the grid of panels and the move under way (lor-qdmy). A finger moves it;
     * otherwise [HomeScreen]'s [SettleGrid] finishes the move. The drawer is one of its cells.
     */
    val grid = GridPlace().also { it.onLeftDrawer = ::leaveDrawer }

    /** The blocks that take a vertical swipe while they can scroll (the drawer's list, the agent page). */
    val scrolls = GridScrolls()

    /** Whether home is on the drawer's cell, or going there. */
    val drawer: Boolean get() = grid.panel(grid.target) is Panel.Drawer

    /** How much of the drawer is on screen, 0 to 1 (launcher polish 7.1's position, now the grid's). */
    val drawerShown: Float get() = grid.drawerShown

    /**
     * Opens the drawer: the one a swipe up reaches, else a swipe down (decision lor-y7qi), else
     * any drawer in the grid.
     */
    fun openDrawer() {
        if (drawer) return
        val move = listOf(Way.DOWN, Way.UP).mapNotNull(grid::nextMove).firstOrNull { grid.panel(it.to) is Panel.Drawer }
        if (move != null) grid.goTo(move.to, true, move.way, move.focus)
        else grid.panels.firstOrNull { it is Panel.Drawer }?.let { grid.goTo(it.at, true) }
    }

    /** Leaves the drawer for the cell it was opened from while that is there, else `start`. */
    fun closeDrawer() {
        if (!drawer) return
        // On its way in: back where it came from.
        if (grid.move != null) return grid.goTo(grid.cell, true, focus = grid.focus)
        // The way back is the vertical move back (lor-y7qi), which on the inner screen keeps the focus.
        val back = grid.back?.takeIf { grid.panel(it.from) != null }?.let { grid.nextMove(it.way.opposite) }
        if (back != null) grid.goTo(back.to, true, back.way, back.focus) else grid.goTo(grid.start, true)
    }

    /** Set by a home press that should jump rather than animate. */
    var snapFolder = false

    /** The open folder's tile icon, in root coordinates, when it was tapped (launcher polish 7.2). */
    var folderFrom: Rect? = null

    /** Set by the folder menu's Rename…: the folder opens with its name field focused (lor-gnk0). */
    var folderRename = false

    /**
     * How far a back gesture has gone, eased, from 0 to 1 (launcher polish 7.3): the drawer or the
     * open folder is drawn scaled by `1 - 0.1 × back`. Back to 0 when either opens again.
     */
    var back by mutableFloatStateOf(0f)

    /**
     * The open folder: its place in the file (launcher polish 4.1: by place, since names may
     * repeat), its name and its apps, so an edit that moves another item into that place closes it.
     */
    var folder by mutableStateOf<OpenFolder?>(null)
    var query by mutableStateOf("")

    /** The drawer's search button grown into its field, the tabs folded away (lor-iyc3). */
    var searchOpen by mutableStateOf(false)

    /** The drawer's tab on screen, by index into `drawer.tabs`. */
    var tab by mutableIntStateOf(0)

    /** Folds the drawer's search back into its button, the text gone. */
    fun foldSearch() {
        query = ""
        searchOpen = false
    }

    /** What leaving the drawer resets: the search, and the tab, so the drawer opens on the first. */
    fun leaveDrawer() {
        foldSearch()
        tab = 0
    }
    var homePresses by mutableStateOf(0)
    /**
     * Home goes to its start after a screen-off (lor-ozj2), once composed with the screen it came
     * back on, so the start is read for that screen's width.
     */
    var toStart by mutableStateOf(false)
    /** Home settings' Panels arrived while [toStart] waits: the minimap opens after that home press. */
    var minimapAfterStart = false
    /** Where in the window a long press on empty home opened the home menu, which points at it. */
    var homeMenu by mutableStateOf<Offset?>(null)
    /** The cell that long press was on, so only that cell draws the menu. */
    var homeMenuCell by mutableStateOf<Cell?>(null)
    /**
     * The open app or folder menu (with a folder's Rename dialog and Cover list), as the token of
     * the tile it was opened from; at most one (launcher polish 7.3, Increment 1's R5).
     */
    var menu by mutableStateOf<Any?>(null)
    /** The stack whose home menu chose Add widget: the picker sheet is open for it (lor-r0f9). */
    var widgetPicker by mutableStateOf<Cell?>(null)
    /** The widget (by its id) whose resize frame is up; the grid leaves the finger to it (lor-r0f9). */
    var resizing by mutableStateOf<Int?>(null)
    /** The resize frame with its handle, in root coordinates: a press outside it closes the frame. */
    var resizeFrame: Rect? = null
    /** The Undo bar after an edit (launcher polish 4.2). */
    val undo = UndoBarState()
    /** The long-press menu open over home (lor-fhk5), drawn at home's root. */
    val popovers = PopoverHost()
    /** What is held, and edit mode (launcher polish 4.3). */
    val drag = DragState()
    /**
     * The minimap is open over home (lor-6get): from the home menu's Panels, or Home settings'.
     * Home stays on its cell under it.
     */
    var minimap by mutableStateOf(false)
        private set

    /** Opens the minimap, closing what floats over home and putting a held item back. */
    fun openMinimap() {
        drag.held = null
        drag.settling = null
        drag.editing = false
        folder = null
        homeMenu = null
        menu = null
        minimap = true
    }

    /** Closes the minimap: home stays on the cell it is on (an edit that removed it moved home already). */
    fun closeMinimap() {
        minimap = false
    }

    /** Home with nothing open over it: a home press here opens quick send instead. */
    val atRest: Boolean get() = !minimap && folder == null && homeMenu == null && menu == null && widgetPicker == null && resizing == null && grid.heading(grid.start) && grid.move == null && !drag.editing && drag.held == null

    /**
     * A home press ([animate] false when home wasn't on screen before it: then everything shuts
     * at once, since there's nothing to watch).
     */
    fun homePressed(animate: Boolean = true) {
        // A home press puts a held item back and leaves edit mode, and goes to `start` (lor-qdmy).
        snapFolder = !animate && folder != null
        drag.held = null
        drag.settling = null
        drag.editing = false
        grid.goTo(grid.start, animate)
        folder = null
        homeMenu = null
        minimap = false
        widgetPicker = null
        resizing = null
        // Every floating view closes, as Launcher3's onNewIntent closes them (launcher polish 7.3).
        menu = null
        leaveDrawer()
        homePresses++
    }
}

/** What a back gesture will close (launcher polish 7.3). */
private enum class Back { FOLDER, EDIT, SEARCH, DRAWER, PAGE }

/** How much smaller back draws the drawer or a folder at the gesture's end (Material's predictive back). */
private const val BACK_SCALE = 0.1f

/** The back gesture's progress eased as Android's `PathInterpolator(0, 0, 0, 1)`: quick at first, then slower. */
private val BackEasing = CubicBezierEasing(0f, 0f, 0f, 1f)

/**
 * The one job that eases [HomeUi.back] to 0 (launcher polish 7.3): for a cancelled gesture and for
 * a new open alike, and stopped when a back gesture starts, so two writers never alternate.
 */
private class BackEase(private val ui: HomeUi, private val scope: CoroutineScope) {
    private var job: Job? = null

    fun start() {
        if (ui.back == 0f) return
        job?.cancel()
        job = scope.launch { animate(ui.back, 0f, animationSpec = Motion.quick()) { v, _ -> ui.back = v } }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

/**
 * The open folder as it was last drawn. A folder at [spot] is still it when its name or its apps
 * are the same: a rename keeps the apps, and an app added or dragged out keeps the name.
 */
data class OpenFolder(val spot: Spot.Apps, val name: String, val apps: List<AppRef>) {
    fun isOf(f: ResolvedFolder) = f.at.spot == spot && (f.folder.name == name || f.folder.apps == apps)

    companion object {
        fun of(f: ResolvedFolder) = OpenFolder(f.at.spot as Spot.Apps, f.folder.name, f.folder.apps)
    }
}

/** A folder with its installed apps, each with its place in the file; [cover] is its cover app if installed. */
class ResolvedFolder(val folder: Folder, val at: From.At, val apps: List<Pair<HomeApp, From.At>>, val cover: HomeApp?)

/** A layout item with its installed apps and its place in the file; refs to apps that aren't installed are left out. */
internal sealed interface Placed {
    val at: From.At
    class App(val app: HomeApp, override val at: From.At) : Placed
    class InFolder(val folder: ResolvedFolder) : Placed {
        override val at get() = folder.at
    }
}

/** Home's `apps` blocks as drawn: each item with the place it holds in the file. */
internal class PlacedHome(val blocks: Map<BlockAt, List<Placed>>) {
    val count: Int get() = blocks.values.sumOf { it.size }
    val all: List<Placed> get() = blocks.values.flatten()
}

private fun resolve(ref: AppRef, apps: List<HomeApp>): HomeApp? =
    apps.firstOrNull { ref.matches(it.id) }

private fun place(layout: Layout, apps: List<HomeApp>): PlacedHome {
    val blocks = layout.appsBlocks.associateWith { p ->
        layout.items(p)!!.mapIndexedNotNull { i, item ->
            val at = From.At(Spot.Apps(p, i), item)
            when (item) {
                is AppRef -> resolve(item, apps)?.let { Placed.App(it, at) }
                is Folder -> placeFolder(item, at, apps)?.let { Placed.InFolder(it) }
            }
        }
    }
    return PlacedHome(blocks)
}

/** The folder [item] at [at] with its installed apps, or null when none is installed. */
private fun placeFolder(item: Folder, at: From.At, apps: List<HomeApp>): ResolvedFolder? {
    val spot = at.spot as Spot.Apps
    return item.apps.mapIndexedNotNull { a, ref -> resolve(ref, apps)?.let { it to From.At(Spot.InFolder(spot.at, spot.index, a), ref) } }
        .takeIf { it.isNotEmpty() }
        ?.let { ResolvedFolder(item, at, it, item.cover?.let { c -> resolve(c, apps) }) }
}

/** What a press picks up: where it is in the file, the item, and the app or folder drawn. */
private class Picked(val from: From, val item: LayoutItem, val app: HomeApp?, val folder: ResolvedFolder?)

/**
 * What a press on [slot] picks up, looked up in the layout drawn now rather than kept from when
 * its tile was laid out (4's review item (a)); null when it has gone or isn't installed.
 */
private fun HomeContext.pick(slot: Slot): Picked? {
    if (slot is Slot.DrawerApp) return apps.firstOrNull { it.key == slot.app }?.let { a -> refFor(a).let { Picked(From.Drawer(a.id, it), it, a, null) } }
    val at = file?.current?.find(slot) ?: return null
    return when (val item = at.item) {
        is AppRef -> resolve(item, apps)?.let { Picked(at, item, it, null) }
        is Folder -> placeFolder(item, at, apps)?.let { Picked(at, item, null, it) }
    }
}

@Composable
fun HomeScreen(apps: List<HomeApp>, appsLoad: AppsLoad, layout: LayoutState, reads: Int, stats: LaunchStats, search: SearchSources, ui: HomeUi, actions: HomeActions) {
    // While the file is broken, everything draws from the last good layout (launcher polish 3.1).
    // While an item is held, home draws the drag's working copy; just after a drop, what it wrote (launcher polish 4.3).
    val drag = ui.drag
    val held = drag.held
    // What home draws straight on the wallpaper takes the wallpaper's ink (launcher polish 6.3).
    val ink = wallInk(rememberWallpaperInk())
    val file = held?.work ?: drag.landing ?: layout.shown
    val home = remember(actions, file, apps) { HomeContext(actions, file, apps, ui) }
    val placed = remember(apps, file) { file?.let { place(it.current, apps) } }
    val folder = ui.folder?.let { open ->
        placed?.all?.firstNotNullOfOrNull { (it as? Placed.InFolder)?.folder?.takeIf(open::isOf) }
    }
    // The open folder as drawn now, so the next edit (a rename, an added app) is matched against it.
    SideEffect { if (folder != null && ui.folder != OpenFolder.of(folder)) ui.folder = OpenFolder.of(folder) }
    // A LoR write from a panel or block takes home's one Undo bar, as a layout edit does (lor-rz50).
    val lorBar = (LocalContext.current.applicationContext as com.spadaval.lor.LorApp).writes.bar
    // A write that failed takes its line back, unless something else has the bar by now.
    val lastLorBar = remember { arrayOfNulls<UndoMessage>(1) }
    LaunchedEffect(lorBar) {
        if (lorBar != null) ui.undo.message = lorBar
        else if (ui.undo.message === lastLorBar[0]) ui.undo.message = null
        lastLorBar[0] = lorBar
    }
    // An edit that empties the open folder removes it, so the panel closes.
    LaunchedEffect(ui.folder, folder, placed) { if (ui.folder != null && folder == null && placed != null) ui.folder = null }
    // The grid drawn (lor-qdmy): the file's, else the default grid with nothing on home yet.
    val grid = ui.grid
    val drawn = file?.current ?: remember { com.spadaval.lor.home.Layout.grid("default", emptyList(), emptyList()) }
    // While something is held, an empty page follows the last in the start row, for a drop that makes one (launcher polish 4.5).
    val newPanel = remember(held?.work) { held?.work?.current?.let(::heldPanel) }
    val panels = drawn.panels + listOfNotNull(newPanel)
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val wideScreen = config.screenWidthDp >= 600
    val start = if (wideScreen) drawn.start.wide ?: drawn.start.narrow else drawn.start.narrow
    // Halves share a screen only where the smallest width is 600 dp or more: the inner screen in
    // either orientation, never a phone turned sideways (decision note on lor-ph30).
    // A fold or unfold lands where lor-j87u's rules say; the lines the fold checks read (lor-ozj2).
    if (grid.pair(config.smallestScreenWidthDp >= 600)) {
        android.util.Log.d("LorGrid", if (grid.paired) "unfolded: at ${grid.cell} on ${grid.inner.screen.cells}" else "folded: at ${grid.cell}")
    }
    // The first layout puts home on its start; after that a cell that went leaves home on the nearest
    // in its row, so it stands on a cell that exists (launcher polish 4.5).
    // Set here rather than in an effect, so the frame drawn is never one behind the file (each write
    // only when it changes something, so composition settles).
    val first = remember { booleanArrayOf(true) }
    if (grid.start != start) grid.start = start
    LaunchedEffect(ui.toStart) {
        if (ui.toStart) {
            ui.toStart = false
            ui.homePressed(animate = false)
            if (ui.minimapAfterStart) {
                ui.minimapAfterStart = false
                ui.openMinimap()
            }
        }
    }
    if (grid.panels != panels) grid.edited(panels, start)
    if (first[0] && file != null) {
        first[0] = false
        grid.goTo(start, animate = false)
    }
    // A file changed from outside (an agent, adb) may hold another folder at the open one's place.
    // It closes at once: a reread puts things in place without motion (DESIGN.md's Motion).
    LaunchedEffect(reads) {
        if (ui.folder == null) return@LaunchedEffect
        ui.snapFolder = true
        ui.folder = null
    }
    // The system bars' icons follow the ink: dark over a light wallpaper on home's pages, light on
    // the agent page's veil, the drawer and an open folder (6's review, R1).
    // The drawer counts once any of it is under the status bar (launcher polish 7.1), decided in the
    // frame it crosses the bar rather than when a close starts.
    var heightPx by remember { mutableIntStateOf(0) }
    val barPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    val underBar by remember(heightPx, barPx) { derivedStateOf { grid.panel(grid.underBar(heightPx, barPx)) } }
    // An open folder, opening or closing, holds the glass (launcher polish 7.2).
    var panel by remember { mutableStateOf<ResolvedFolder?>(null) }
    SideEffect { if (folder != null) panel = folder }
    // The minimap's glass comes in and goes as a folder's does, home fading under it (lor-6get).
    val mapShown = remember { Animatable(0f) }
    LaunchedEffect(ui.minimap) { mapShown.animateTo(if (ui.minimap) 1f else 0f, if (ui.minimap) Motion.move() else Motion.quick()) }
    val darkBarIcons = ink.ink == Ink.DARK && underBar is Panel.Stack && folder == null && panel == null && !ui.minimap && mapShown.value == 0f
    val view = androidx.compose.ui.platform.LocalView.current
    var barsDark by remember { mutableStateOf<Boolean?>(null) }
    SideEffect {
        if (barsDark == darkBarIcons) return@SideEffect
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkBarIcons
            isAppearanceLightNavigationBars = darkBarIcons
        }
        barsDark = darkBarIcons
    }
    // Each move goes to its target, unless a finger has it; a home press from elsewhere jumps (7.1).
    SettleGrid(grid)
    // A target waits 300 ms under the finger before the working copy takes it (Kvaesitso's attemptMove).
    LaunchedEffect(held, held?.pending) {
        val t = held?.pending ?: return@LaunchedEffect
        delay(300)
        held.step(t)
        held.pending = null
    }
    // Held at the screen's edge for 500 ms, home moves a cell that way in its row, only to a stack
    // with apps (never to the agent page; launcher polish 4.5).
    LaunchedEffect(held, held?.edge) {
        val e = held?.edge ?: 0
        if (e == 0) return@LaunchedEffect
        while (true) {
            delay(500)
            val to = grid.next(if (e > 0) Way.RIGHT else Way.LEFT) ?: break
            if (grid.panel(to)?.hasApps != true) break
            // A turn once started finishes, or home would stop between cells (no finger moves the grid while held).
            withContext(NonCancellable) {
                grid.goTo(to, animate = true)
                snapshotFlow { grid.move }.first { it == null }
            }
        }
    }
    val haptics = LocalHapticFeedback.current
    fun dropHeld(h: Held, t: Target?) {
        drag.held = null
        var final = h.work
        // A drawer app is added wherever it lands, even after steps put it in the working copy.
        var said: String? = if (h.fromDrawer) "Added." else "Moved."
        // Where the icon settles (launcher polish 7.5): put back, where the working copy has it.
        var at: Spot? = h.spot
        var refused = false
        if (t != null) {
            try {
                val d = h.work.drop(h.from, t, h.home.installed, strip = false, nameFor = h.home::nameFor)
                final = d.file
                at = d.at
                said = dropMessage(t, if (h.fromDrawer) null else h.spot, h.work)
            } catch (e: LayoutError) {
                refused = true
                actions.refused(e.message ?: "it can't go there")
            }
        }
        // Off home, a drawer app a step took out of its folder stays in it (lor-83uq's review, R1).
        h.drawer?.let { d ->
            try {
                drawerDragResult(h.original, final, d, at, h.home.installed).let { final = it.file; at = it.at }
            } catch (e: LayoutError) {
                refused = true
                final = h.original
                actions.refused(e.message ?: "it can't go there")
            }
        }
        val stripped = final.withCurrent(final.current.stripEmptyPanels(h.original.current))
        // A page that went may have been before the one it settles on; then it fades where it is.
        if (stripped.current.pages.size != final.current.pages.size) at = null
        final = stripped
        haptics.performHapticFeedback(if (refused) HapticFeedbackType.Reject else HapticFeedbackType.GestureEnd)
        val to = at
        drag.settling = when {
            to == null -> Settle(h.app, h.folder, h.finger, null, Settle.Kind.FADE)
            // Into the app or folder it was dropped on: to that tile, which is the folder now.
            !refused && t is Target.Onto && to is Spot.InFolder -> Settle(h.app, h.folder, h.finger, to.folder, Settle.Kind.ONTO)
            else -> Settle(h.app, h.folder, h.finger, to, Settle.Kind.SLOT)
        }
        if (final == h.original) return
        val out = h.outOf
        if (out != null && !refused && h.app != null && final.placesOf(h.app.id, h.home.installed).home) said = "${h.app.label} is on home, out of $out."
        drag.landing = final
        actions.edit(said ?: "Added.", after = { drag.landing = null }) { now -> landOver(now, h.original, final) }
    }
    // The icon let go goes to where it went, then is gone (launcher polish 7.5).
    LaunchedEffect(drag.settling) {
        val s = drag.settling ?: return@LaunchedEffect
        s.progress.animateTo(1f, if (s.kind == Settle.Kind.FADE) Motion.quick() else Motion.move())
        if (drag.settling === s) drag.settling = null
    }
    // A locked home has no edit mode (lor-zzyy): locking from the home menu, or from outside, leaves it.
    val locked = file?.locked == true
    LaunchedEffect(locked) { if (locked) drag.editing = false }
    // What is left of a back gesture's scale eases to 0 in one job (7's third review).
    val backScope = rememberCoroutineScope()
    val backEase = remember(backScope) { BackEase(ui, backScope) }
    // A folder opens from its tile and closes back into it, or fades where it is if its tile went away (7.2).
    val folderShown = remember { Animatable(0f) }
    val folderLayer = rememberGraphicsLayer()
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    var folderFading by remember { mutableStateOf(false) }
    val folderOpen = folder != null
    // The wallpaper behind an open folder blurs where the window can (lor-gnk0), with the motion.
    val blurred = rememberCrossWindowBlur()
    BlurBehind(blurred, view) { maxOf(folderShown.value, mapShown.value) }
    LaunchedEffect(folderOpen, ui.homePresses) {
        if (folderOpen) {
            folderFading = false
            ui.snapFolder = false
            backEase.start()
            // The panel's first frame is its slowest (on the emulator, 140 ms); start the clock after it,
            // or the motion would be over before it is seen. It is drawn at nothing until then.
            if (folderShown.value == 0f) repeat(2) { withFrameNanos {} }
            folderShown.animateTo(1f, Motion.move())
            return@LaunchedEffect
        }
        val last = panel ?: return@LaunchedEffect
        if (ui.snapFolder) {
            ui.snapFolder = false
            folderShown.snapTo(0f)
        } else {
            val spot = last.at.spot
            val stays = placed?.all?.any { (it as? Placed.InFolder)?.folder?.let { f -> f.at.spot == spot && f.folder.name == last.folder.name } == true } == true
            folderFading = !stays
            folderShown.animateTo(0f, if (stays) Motion.move() else Motion.quick())
        }
        panel = null
    }
    // Back closes what is open, in this order, and shows where it is going while the gesture lasts
    // for the drawer and a folder (launcher polish 7.3): each scales toward 0.9 with the progress
    // (Kvaesitso's `LauncherScaffold`, GPL-3.0, ideas only; Lawnchair's `onBackProgressed`, Apache-2.0).
    // Before Android 14 only the commit arrives, and it is the whole behaviour.
    // A new open (a drawer opened or caught, a folder tapped mid-close) starts whole, though a back
    // gesture's close was caught before it ended (7's review, R2), as a cancelled gesture does.
    LaunchedEffect(ui.drawer, grid.follow.held) { if (ui.drawer || grid.follow.held) backEase.start() }
    PredictiveBackHandler(enabled = ui.folder != null || ui.drawer || !grid.heading(grid.start) || drag.editing) { events ->
        // The gesture owns the scale from its start (7's third review).
        backEase.stop()
        val what = when {
            ui.folder != null -> Back.FOLDER
            drag.editing -> Back.EDIT
            ui.drawer && ui.searchOpen -> Back.SEARCH
            ui.drawer -> Back.DRAWER
            else -> Back.PAGE
        }
        try {
            events.collect { e -> if (what == Back.FOLDER || what == Back.DRAWER) ui.back = BackEasing.transform(e.progress) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // A cancelled gesture returns it to full size.
            backEase.start()
            throw e
        }
        // The commit: the close runs from where the gesture left it; the scale goes with it.
        when (what) {
            Back.FOLDER -> ui.folder = null
            Back.EDIT -> drag.editing = false
            // Back folds the drawer's search first, the drawer staying open (lor-iyc3).
            Back.SEARCH -> ui.foldSearch()
            Back.DRAWER -> { ui.closeDrawer(); ui.leaveDrawer() }
            Back.PAGE -> grid.goTo(grid.start, animate = true)
        }
    }
    // Once what back scaled has gone, the next one starts whole.
    val backDone by remember { derivedStateOf { ui.drawerShown == 0f && ui.folder == null } }
    LaunchedEffect(backDone, panel) { if (backDone && panel == null) ui.back = 0f }
    // The drag handler lives across recompositions, so it reads the latest of these.
    val latestHome by rememberUpdatedState(home)
    val latestDrop by rememberUpdatedState<(Held, Target?) -> Unit>(::dropHeld)
    val dots by Dots.withDots.collectAsState()
    CompositionLocalProvider(LocalHomeLook provides (file?.look ?: HomeLook()), LocalDots provides dots) {
    val barTop = rememberUpdatedState(barPx)
    val shownApps = remember(apps, home) { apps.filterNot(home::hidden) }
    Box(Modifier.fillMaxSize().onSizeChanged { heightPx = it.height }.closeResizeOutside(ui).dragging(ui, haptics, { latestHome }, { h, t -> latestDrop(h, t) }).gridFinger(ui, haptics) { barTop.value }) {
        // Home gives way to an open folder (lor-gnk0, as Smart Launcher's does): it fades out as the
        // folder opens, so the glass shows only the wallpaper, and back in as it closes.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - maxOf(folderShown.value, mapShown.value) }) {
            CompositionLocalProvider(LocalGridScrolls provides ui.scrolls) {
                GridCells(grid) { p ->
                    when (p) {
                        is Panel.T3 -> AgentPage()
                        is Panel.Lor -> LorPanel()
                        // The drawer, wherever the finger or its settle has it (launcher polish 7.1).
                        is Panel.Drawer -> Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                // Predictive back (7.3): smaller about its centre as the gesture goes.
                                val b = if (ui.folder == null && panel == null && grid.cell == p.at) 1f - BACK_SCALE * ui.back else 1f
                                scaleX = b
                                scaleY = b
                            },
                        ) {
                            Drawer(shownApps, stats, search, ui, home)
                        }
                        is Panel.Stack -> CompositionLocalProvider(LocalWallInk provides ink) {
                            StackPanel(p, p.at == start, placed, layout, appsLoad, ui, home)
                        }
                    }
                }
            }
            // The dots of the row on screen, and of the row a vertical move brings (launcher polish 6.8:
            // on the agent page they sit on its dark veil, not the wallpaper). Under the keyboard, as
            // the page they mark is (lor-sqfe), rather than over a field the keyboard pushed up.
            RowDots(grid, grid.cell, ink, { heightPx }, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.bars).padding(bottom = DotsAbove))
            grid.move?.takeIf { !it.way.horizontal }?.let { RowDots(grid, it.to, ink, { heightPx }, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.bars).padding(bottom = DotsAbove)) }
        }
        if (folder != null) {
            FolderPanel(folder, home, blurred, onClose = { ui.folder = null }, { folderShown.value }, { 1f - BACK_SCALE * ui.back }, ui.folderFrom, folderLayer) { cardBounds = it }
        } else if (panel != null) {
            // Closing, it is a picture of itself: no drop zones, sources or taps, so a drag that left it
            // can't drop into it and a tap reaches home.
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val p = folderShown.value
                    folderGlass(p, blurred)
                    val c = cardBounds ?: return@drawBehind
                    val f = FolderMotion.at(p, c.toArray(), if (folderFading) null else ui.folderFrom?.toArray(), 1f - BACK_SCALE * ui.back)
                    translate(c.left + f.dx, c.top + f.dy) {
                        scale(f.scale, f.scale, pivot = Offset(c.width / 2f, c.height / 2f)) {
                            folderLayer.alpha = f.alpha
                            drawLayer(folderLayer)
                        }
                    }
                },
            )
        }
        // The minimap over home (lor-6get), on the open folder's glass.
        if (ui.minimap || mapShown.value > 0f) Box(Modifier.fillMaxSize().drawBehind { folderGlass(mapShown.value, blurred) })
        if (ui.minimap) Minimap(file, ui, home)
        // Over home, under the bars and the Undo bar that confirms what it added (lor-r0f9).
        ui.widgetPicker?.let { WidgetPicker(it, home) { ui.widgetPicker = null } }
        // Edit mode's bar, and Remove while something is held (launcher polish 4.3).
        TopBar(ui, Modifier.align(Alignment.TopCenter).safeDrawingPadding())
        held?.let { HeldItem(it) }
        drag.settling?.let { SettleItem(it, drag, file?.current) { it.progress.value } }
        // Above the drawer and folders, clear of the page dots.
        UndoBar(ui.undo, actions::undo, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 112.dp))
        // Over everything: a long-press menu dims home and points at what opened it (lor-fhk5).
        PopoverLayer(ui.popovers)
    }
    }
}

/**
 * A stack panel: its blocks top to bottom, the `apps` block taking the space between (lor-qdmy:
 * drawn by block, not by page). A stack without a `clock` starts where home's grid would, as
 * home's pages after the first did (launcher polish 6.7). Home's notes (the broken file, set-up,
 * empty home) are drawn on the [start] cell's stack.
 *
 * Blocks taller than the panel scroll (lor-r0f9): they scroll together in [StackScroll], and the
 * grid moves at the scroll's edges.
 */
@Composable
private fun StackPanel(panel: Panel.Stack, start: Boolean, placed: PlacedHome?, layout: LayoutState, appsLoad: AppsLoad, ui: HomeUi, home: HomeContext) {
    val haptics = LocalHapticFeedback.current
    val cell = panel.at
    // A long press that no app, folder or widget took, on empty home, opens the home menu, and a
    // double tap opens quick send (launcher polish 7.6), not in edit mode. Drawn under what it
    // serves: overlapping siblings get a touch only when the ones drawn above it don't, so a tap
    // on an app is never delayed or taken.
    val emptyHome = @Composable { mod: Modifier ->
        val at = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
        Box(
            mod.onGloballyPositioned { at[0] = it }.pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { if (!ui.drag.editing) home.actions.quickSend() },
                    onLongPress = { p ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val mine = at[0]
                        ui.homeMenu = if (mine != null && mine.isAttached) mine.localToWindow(p) else p
                        ui.homeMenuCell = cell
                    },
                )
            },
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize().cellInsetsPadding()) {
        emptyHome(Modifier.matchParentSize())
        val wide = maxWidth >= 600.dp
        val widthDp = maxWidth.value
        val panelHeight = maxHeight.value
        val headerStyle = LocalHomeLook.current.header
        // Clear of the Remove bar, so a drop meant for the first row lands there (6.8's critique).
        val clear = @Composable { Spacer(Modifier.height(if (wide || headerStyle == HomeLook.Header.NONE) BarClear else 104.dp)) }
        val header = @Composable {
            if (headerStyle != HomeLook.Header.NONE) HomeHeader(headerStyle, if (wide) 40 else 30, home.actions::calendar, home.actions::alarms) else clear()
        }
        val body = homeBody(appsLoad, home.file, placed?.count ?: 0)
        // The apps block takes the space the other blocks leave ([FillColumn]'s fill), and no less
        // than it needs when they leave too little, so it scrolls with them.
        // Home's notes on the start stack: the broken-file banner, and until the first run is done
        // (launcher polish 3.5) the set-up card, with the block [notesBlock] names.
        val notes = @Composable {
            if (layout is LayoutState.Broken) {
                BrokenBanner(
                    layout, appsLoad == AppsLoad.LOADED, Type.body.copy(color = Txt, fontSize = 13.sp),
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth()
                        .framedPanel(StateFill)
                        .padding(horizontal = 14.dp),
                    home.actions::restore,
                )
            }
            SetupCard(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp))
        }
        val notesAt = if (start) notesBlock(panel) else -1
        val apps = @Composable { at: BlockAt ->
            val columns = LocalHomeLook.current.columnsFor(widthDp).home
            FillColumn(Modifier.layoutId(Fill).fillMaxWidth()) {
                if (at.block == notesAt) notes()
                val drops = body == HomeBody.GRID || body == HomeBody.EMPTY
                // The block's space, from its first row to what follows it, takes a drop after its last item.
                Box(
                    Modifier.layoutId(Fill).fillMaxWidth()
                        .dropZone(home.drag, "apps" to at) { b -> if (drops) DropMark.Area(Space.Apps(at), b) else null },
                ) {
                    when (body) {
                        HomeBody.NOTHING -> {}
                        HomeBody.SKELETON -> Skeleton(home.file!!.current.items(at).orEmpty(), columns)
                        HomeBody.APPS_DIDNT_LOAD -> if (start) HomeNote("Apps didn't load. Trying again.", null)
                        HomeBody.EMPTY -> if (start) HomeNote("Home is empty.", "Swipe up for your apps, then drag one here, or long-press it and choose Add to home.") { ui.openDrawer() }
                        HomeBody.GRID -> HomeGrid(placed?.blocks?.get(at).orEmpty(), columns, ui, home)
                    }
                }
            }
        }
        // Blocks, each with its place in the stack, top to bottom; with no `apps` block, the space
        // goes after the others, so they sit under the header. The clock is home's header, drawn
        // where it stands in the stack (lor-9uc1 orders it); a column that starts with anything else
        // starts clear of the Remove bar.
        val blocks = @Composable { mod: Modifier, list: List<IndexedValue<StackBlock>> ->
            StackScroll(mod, background = emptyHome) {
                if (list.firstOrNull()?.value !is StackBlock.Clock) clear()
                list.forEach { (i, block) ->
                    when (block) {
                        StackBlock.Clock -> header()
                        is StackBlock.Apps -> apps(BlockAt(cell, i))
                        is StackBlock.Widget -> key(block.id) { WidgetBlock(block, panelHeight, ui, home) }
                        StackBlock.Today, StackBlock.NeedsYou, StackBlock.Running, StackBlock.T3Queue -> key(block.type) { LorBlock(block, ui.grid) }
                    }
                    // With no `apps` block, the notes go under the block [notesBlock] names (lor-bdus's review, R4).
                    if (i == notesAt && block !is StackBlock.Apps) notes()
                }
                if (list.none { it.value is StackBlock.Apps }) Spacer(Modifier.layoutId(Fill))
            }
        }
        val all = panel.blocks.withIndex().toList()
        if (wide) {
            // The platform's flow (lor-j87u): the clock and the first half of the blocks left, the
            // rest right; a page of only apps is one column. Each column scrolls on its own.
            Row(Modifier.fillMaxSize()) {
                StackFlow.wide(all).forEach { column ->
                    blocks(Modifier.weight(1f).fillMaxSize(), column)
                }
            }
        } else {
            blocks(Modifier.fillMaxSize(), all)
        }
        ui.homeMenu?.takeIf { ui.homeMenuCell == cell }?.let { at ->
            // Not while home is locked (decision lor-73li): a widget is an edit to home.
            val addWidget = if (home.file?.locked == true) null else ({ ui.widgetPicker = cell })
            HomeMenu(at, home, onEdit = { ui.drag.editing = true }, onPanels = ui::openMinimap, addWidget = addWidget) { ui.homeMenu = null }
        }
    }
}

/**
 * The block of a start stack that home's notes (the broken-file banner, the set-up card) are drawn
 * with: its `apps` block, at the top of it, else its first block, under it. So a start stack
 * without apps, as an old dock-only one converts to (lor-bdus), still shows them.
 */
internal fun notesBlock(stack: Panel.Stack): Int = stack.blocks.indexOfFirst { it is StackBlock.Apps }.coerceAtLeast(0)

/** Marks the child of a [FillColumn] that takes the space the others leave. */
internal object Fill

/**
 * A column whose [Fill] child takes the height the others leave of the column's minimum, and no
 * less than its own: a `weight(1f)` that still works inside a scroll, where a weight gets nothing
 * once the others overflow. Without a minimum it is a plain column. Children are as wide as the
 * column allows at most, placed at its start.
 */
@Composable
internal fun FillColumn(modifier: Modifier = Modifier, endPad: () -> Int = { 0 }, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val fill = measurables.indexOfFirst { it.layoutId == Fill }
        val placeables = arrayOfNulls<androidx.compose.ui.layout.Placeable>(measurables.size)
        measurables.forEachIndexed { i, m -> if (i != fill) placeables[i] = m.measure(loose) }
        val others = placeables.sumOf { it?.height ?: 0 }
        if (fill >= 0) {
            val left = (constraints.minHeight - others).coerceAtLeast(0)
            val max = if (constraints.hasBoundedHeight) (constraints.maxHeight - others).coerceAtLeast(left) else Constraints.Infinity
            placeables[fill] = measurables[fill].measure(loose.copy(minHeight = left, maxHeight = max))
        }
        // Taller than its minimum (a stack that overflows), it ends [endPad] px lower; one that fits is unchanged.
        val sum = placeables.sumOf { it!!.height }
        val height = (if (sum > constraints.minHeight) sum + endPad() else sum).coerceIn(constraints.minHeight, constraints.maxHeight)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeables.maxOfOrNull { it!!.width } ?: 0
        layout(width, height) {
            var y = 0
            placeables.forEach {
                it!!.place(0, y)
                y += it.height
            }
        }
    }
}

/**
 * A stack's blocks (lor-r0f9, reused by lor-clgw's blocks): a [FillColumn] at least
 * the space's height, so the `apps` block (or the space before the blocks) fills what is left as
 * before; taller than the space, it scrolls, reporting [scrollsInGrid] so the grid moves at its
 * edges. Scrolling is off while it fits, so a swipe on a page that fits is the grid's as before.
 * [background] is drawn under the blocks, at their full height, to take presses on empty home.
 *
 * Scrolling, its content ends with room for home's page dots, so the last row scrolls clear of
 * them (lor-leh4): the dots stand [DotsAbove] above the panel's bottom, which is its parent's
 * (the stack's column), and what lies below the scroll, if anything, already covers part of that.
 */
@Composable
internal fun StackScroll(modifier: Modifier, background: @Composable (Modifier) -> Unit, content: @Composable () -> Unit) {
    val dotsTop = with(LocalDensity.current) { (DotsAbove + DotSize + DotsGap).roundToPx() }
    // The scroll's height above the parent's bottom, in px: what lies under it there.
    var below by remember { mutableIntStateOf(0) }
    BoxWithConstraints(
        modifier.onGloballyPositioned { c ->
            val parent = c.parentLayoutCoordinates ?: return@onGloballyPositioned
            below = (parent.size.height - c.positionInParent().y.roundToInt() - c.size.height).coerceAtLeast(0)
        },
    ) {
        val scroll = rememberScrollState()
        val space = maxHeight
        Box(Modifier.fillMaxWidth().scrollsInGrid(scroll).verticalScroll(scroll, enabled = scroll.maxValue > 0)) {
            background(Modifier.matchParentSize())
            FillColumn(Modifier.fillMaxWidth().heightIn(min = space), endPad = { (dotsTop - below).coerceAtLeast(0) }, content = content)
        }
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
                        Column(Modifier.fillMaxWidth().widthIn(max = 76.dp).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(iconSize).background(Line, if (item is Folder) RectangleShape else CircleShape))
                            // The label's height, so the rows sit where the tiles will.
                            Spacer(Modifier.height(5.dp))
                            Text("", style = labelStyle)
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A line or two in home's grid area, on a card over the wallpaper; [onClick], if any, makes it a button. */
@Composable
private fun HomeNote(title: String, detail: String?, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .framedPanel(StateFill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, style = Type.body.copy(color = Txt, fontSize = 16.sp))
        detail?.let { Text(it, style = Type.body.copy(color = Mute, fontSize = 14.sp), modifier = Modifier.padding(top = 4.dp)) }
    }
}


/** Home's items in reading order, [columns] to a row: Launcher3's cells without coordinates. */
@Composable
private fun HomeGrid(items: List<Placed>, columns: Int, ui: HomeUi, home: HomeContext) {
    val icon = rememberTileIcon()
    val drag = home.drag
    // Keyed by what each tile is, so a tile moved by a drag eases from its old place (launcher polish 7.5);
    // the same keys name it to a drag (4's review item (a)).
    val layout = home.file?.current
    val slots = remember(items, layout) { items.map { layout?.slotOf(it.at.spot) as? Slot.Tile } }
    CellGrid(columns, rowGap = 14.dp, modifier = Modifier.padding(horizontal = 8.dp)) {
        items.forEachIndexed { i, item ->
            val slot = slots[i] ?: return@forEachIndexed
            key(slot.key) {
                val spot = item.at.spot as Spot.Apps
                val tile = slot.block to slot.key
                Box(
                    Modifier
                        .dropZone(drag, tile) { b -> DropMark.Item(slot, b, b.top + icon.top, icon.size) }
                        .dragSource(drag, tile) { b -> Source(b, slot, 0) }
                        // After the zone and source, so they report the slot, not where the tile is drawn mid-ease.
                        .animatePlacement(drag)
                        .alpha(if (drag.hides(spot)) 0f else 1f)
                        // DESIGN.md's selection on the item a drop would go onto.
                        .then(if (drag.held?.lit == Target.Onto(spot)) Modifier.selection() else Modifier),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    when (item) {
                        is Placed.App -> MenuTile(item.app, At(LaunchFrom.HOME), home, spot = item.at, label = LocalHomeLook.current.labels)
                        is Placed.InFolder -> FolderTile(item.folder, home) { from ->
                            ui.folderFrom = from
                            ui.folder = OpenFolder.of(item.folder)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tiles in rows of [columns] equal cells, as one layout (launcher polish 7.5): a `Row` per row
 * would recompose a tile that moves to another row from scratch, losing where it was drawn.
 */
@Composable
internal fun CellGrid(columns: Int, modifier: Modifier = Modifier, rowGap: Dp = 0.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val cell = width / columns
        val gap = rowGap.roundToPx()
        val rows = measurables.map { it.measure(Constraints.fixedWidth(cell)) }.chunked(columns)
        val heights = rows.map { r -> r.maxOf { it.height } }
        val height = heights.sum() + gap * (rows.size - 1).coerceAtLeast(0)
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            rows.forEachIndexed { i, r ->
                r.forEachIndexed { j, p -> p.place(j * cell, y) }
                y += heights[i] + gap
            }
        }
    }
}

/**
 * Eases a tile from its old place in its parent to its new one over `MOVE` (launcher polish 7.5;
 * Compose's `onPlaced` and `Animatable` pattern), only while something is held or settling, so
 * home never moves on its own: a reread, an Undo, a look change or a cold start puts tiles in
 * place at once.
 */
internal fun Modifier.animatePlacement(drag: DragState): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<IntOffset?>(null) }
    // Not state: replacing it during placement shouldn't ask for another layout.
    val anim = remember { arrayOfNulls<Animatable<IntOffset, AnimationVector2D>>(1) }
    onPlaced { target = it.positionInParent().round() }
        .offset {
            val t = target ?: return@offset IntOffset.Zero
            val a = anim[0]
            when {
                a == null || (a.targetValue != t && drag.held == null && drag.settling == null) -> {
                    // Nothing held: the tile is where it is now.
                    anim[0] = Animatable(t, IntOffset.VectorConverter)
                    IntOffset.Zero
                }
                else -> {
                    if (a.targetValue != t) scope.launch { a.animateTo(t, Motion.move()) }
                    a.value - t
                }
            }
        }
}

/**
 * Dots for the screens of [of]'s row, left to right, the one on screen lit (the most of it, in a
 * move across) and the agent page's square; none for a row of one. On the inner screen a dot
 * stands for an inner screen, a pair's or a full panel's (lor-el51). They move up and down with the row.
 */
@Composable
private fun RowDots(grid: GridPlace, of: Cell, ink: WallInk, heightPx: () -> Int, modifier: Modifier) {
    val row = grid.panels.filter { it.at.row == of.row }.sortedBy { it.at.column }.map { grid.screenOf(it.at) }.distinct()
    if (row.size < 2) return
    val lit by remember(grid, of) {
        derivedStateOf { grid.screenOf(grid.move?.takeIf { it.way.horizontal && grid.follow.position >= 0.5f }?.to ?: of) }
    }
    // On the agent page and the lor panel the dots sit on their dark veil, not the wallpaper (6.8: dark ink vanished there).
    val mark = grid.panel(lit.left).let { if (it is Panel.T3 || it is Panel.Lor) SurfaceInk.mark else ink.mark }
    Row(modifier.offset { IntOffset(0, grid.offsetOf(of, 0, heightPx())?.y ?: 0) }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        row.forEach { s ->
            val color = if (s == lit) mark.copy(alpha = 0.8f) else mark.copy(alpha = 0.5f)
            Box(Modifier.size(DotSize).background(color, if (grid.panel(s.left) is Panel.T3) RectangleShape else CircleShape))
        }
    }
}

/** Home's page dots: [DotSize] tall, [DotsAbove] above the bars, as a panel's content ends. */
internal val DotsAbove = 96.dp
internal val DotSize = 6.dp
/** The room an overflowing stack leaves between its last row and the dots. */
private val DotsGap = 8.dp

/** An app tile whose tap opens it from [at] and whose long press opens its menu. */
@Composable
internal fun MenuTile(app: HomeApp, at: At, home: HomeContext, width: Dp = 76.dp, dot: Boolean = false, spot: From.At? = null, label: Boolean = true, icon: IconSpot = remember { IconSpot() }, beforeOpen: () -> Unit = {}) {
    WithAppMenu(app, at, home, spot, icon) { menu ->
        // In edit mode a tap opens nothing (launcher polish 4.3).
        AppTile(app, width, menu, dot, label, icon) { if (!home.drag.editing) { beforeOpen(); home.actions.open(app, at, icon) } }
    }
}

@Composable
internal fun AppTile(app: HomeApp, width: Dp = 76.dp, onLongClick: (() -> Unit)? = null, dot: Boolean = false, label: Boolean = true, icon: IconSpot? = null, onClick: () -> Unit) {
    Column(
        // The cell's width, at most [width]: a tile never overflows a narrow column (launcher polish 6.4).
        Modifier.fillMaxWidth().widthIn(max = width)
            // Without a label the tile keeps its name for TalkBack.
            .then(if (label) Modifier else Modifier.semantics { contentDescription = app.label })
            // The pressed state (lor-gnk0) is this click node's indication, so the tile stays one node.
            .combinedClickable(interactionSource = null, indication = PressScale.Tile, onLongClickLabel = onLongClick?.let { "Menu" }, onLongClick = onLongClick, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AppIcon(app, iconSize, if (icon != null) Modifier.iconSpot(icon) else Modifier)
            // A new app's dot (launcher polish 2.2), until it is first opened from the launcher.
            if (dot || app.dotKey in LocalDots.current) Dot()
        }
        if (label) {
            Spacer(Modifier.height(5.dp))
            Text(app.label, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun FolderTile(folder: ResolvedFolder, home: HomeContext, onClick: (Rect?) -> Unit) {
    // Where its icon is, so the folder opens from it (launcher polish 7.2).
    val icon = remember { arrayOfNulls<Rect>(1) }
    // A long press opens the folder's menu (launcher polish 4.4); moving on from it picks the folder up.
    var menu by rememberFloatingMenu(home.ui)::open
    val haptics = LocalHapticFeedback.current
    val held = home.drag.held
    LaunchedEffect(held) { if (held != null) menu = false }
    // Rename… opens the folder with its name field focused (lor-gnk0), from its tile like a tap.
    val spot = remember { IconSpot() }
    if (menu) FolderMenu(folder, home, spot, rename = { home.ui.folderRename = true; onClick(icon[0]) }) { menu = false }
    val label = LocalHomeLook.current.labels
    Column(
        Modifier.fillMaxWidth().widthIn(max = 76.dp)
            .then(if (label) Modifier else Modifier.semantics { contentDescription = folder.folder.name })
            .combinedClickable(interactionSource = null, indication = PressScale.Tile, onLongClickLabel = if (home.locked) null else "Menu", onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                // Every entry of a folder's menu edits home, so a locked home opens none (lor-zzyy).
                if (home.locked) home.sayLocked() else menu = true
            }, onClick = { onClick(icon[0]) })
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.onGloballyPositioned { icon[0] = it.boundsInRoot() }.iconSpot(spot)) {
            FolderIcon(folder)
            // A folder has a dot when an app in it has one (Launcher3's FolderIcon).
            val dots = LocalDots.current
            if (folder.apps.any { it.first.dotKey in dots }) Dot()
        }
        if (label) {
            Spacer(Modifier.height(5.dp))
            Text(folder.folder.name, style = labelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * A folder's icon: its cover app (launcher polish 4.1), else its first four apps as a bare 2×2 of
 * mini icons in the icon's square, with no backing (lor-gnk0, as Smart Launcher draws it). The
 * label under it is all that is drawn in the wallpaper's ink.
 */
@Composable
private fun FolderIcon(folder: ResolvedFolder) {
    if (folder.cover != null) return AppIcon(folder.cover, iconSize)
    val gap = 4.dp
    val mini = (iconSize - gap) / 2
    Column(Modifier.size(iconSize), verticalArrangement = Arrangement.spacedBy(gap)) {
        folder.apps.take(4).map { it.first }.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) { pair.forEach { AppIcon(it, mini) } }
        }
    }
}

/** The free strip kept under an open folder's apps, so a held app can leave below them (lor-gnk0). */
private val FolderStrip = 96.dp

/**
 * The open folder's glass (lor-gnk0): Deep Space at [Palette.FOLDER_GLASS] over the wallpaper,
 * which the window blurs behind it where it can ([BlurBehind]); where it can't, the same face over
 * a dim of black at [Palette.FOLDER_DIM]. Both come in with the folder's motion ([shown]).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.folderGlass(shown: Float, blurred: Boolean) {
    if (!blurred) drawRect(Color.Black, alpha = Palette.FOLDER_DIM.toFloat() * shown)
    drawRect(Bg, alpha = Palette.FOLDER_GLASS.toFloat() * shown)
}

/**
 * Blurs the wallpaper behind home's window by [shown] of DESIGN.md's 24 dp while [on], and clears
 * it when the folder has gone or blur goes off. Home's window is opaque (it shows the wallpaper),
 * and Android blurs only behind a translucent window's background (`setBackgroundBlurRadius`), so
 * this asks the window manager to blur what is behind the window (`FLAG_BLUR_BEHIND` with
 * `blurBehindRadius`, Android 12 and up), with no dim.
 */
@Composable
private fun BlurBehind(on: Boolean, view: android.view.View, shown: () -> Float) {
    val full = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(on, full) {
        val window = (view.context as? android.app.Activity)?.window
        if (!on || window == null || android.os.Build.VERSION.SDK_INT < 31) return@LaunchedEffect
        fun blur(radius: Int) {
            if (radius > 0) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radius }
        }
        var radius = 0
        try {
            snapshotFlow { (shown() * full).roundToInt() }.collect {
                if (it != radius) blur(it)
                radius = it
            }
        } finally {
            if (radius != 0) blur(0)
        }
    }
}

/**
 * An open folder (lor-gnk0, after Smart Launcher's): it fills the screen as dark glass, with its
 * name, which is edited in place, a + to add apps, and its apps under them. The name row and the
 * apps are the part that opens from the tile (launcher polish 7.2) and takes a drop after the last
 * app; a held app that stays 400 ms outside them leaves ([FolderExit]). A folder too full for the
 * screen scrolls, keeping [FolderStrip] free below. A tap on the glass closes it.
 */
@Composable
private fun FolderPanel(folder: ResolvedFolder, home: HomeContext, blurred: Boolean, onClose: () -> Unit, shown: () -> Float, back: () -> Float, from: Rect?, layer: GraphicsLayer, onCard: (Rect) -> Unit) {
    var card by remember { mutableStateOf<Rect?>(null) }
    var adding by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().drawBehind { folderGlass(shown(), blurred) }.pointerInput(onClose) { detectTapGestures { onClose() } }) {
        val drag = home.drag
        val icon = rememberTileIcon()
        val layout = home.file?.current
        val tile = remember(folder, layout) { layout?.slotOf(folder.at.spot) as? Slot.Tile }
        // Its labels are on the glass, not the wallpaper.
        if (!adding) CompositionLocalProvider(LocalWallInk provides SurfaceInk) {
            BoxWithConstraints(
                Modifier.fillMaxSize().safeDrawingPadding().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = FolderStrip),
                contentAlignment = Alignment.Center,
            ) {
                val columns = if (maxWidth >= 600.dp) 6 else 4
                Column(
                    Modifier
                        .widthIn(max = 100.dp * columns)
                        .fillMaxWidth()
                        // Its bounds as laid out (before the motion's transform), and drawn from its tile (7.2),
                        // recorded each frame so the close can draw it as a picture.
                        .onGloballyPositioned { c -> c.boundsInRoot().let { card = it; onCard(it) } }
                        .graphicsLayer {
                            val c = card
                            val f = if (c == null) FolderMotion.Frame(1f, 0f, 0f, shown()) else FolderMotion.at(shown(), c.toArray(), from?.toArray(), back())
                            scaleX = f.scale
                            scaleY = f.scale
                            translationX = f.dx
                            translationY = f.dy
                            alpha = f.alpha
                        }
                        .drawWithContent {
                            layer.record { this@drawWithContent.drawContent() }
                            // The close's picture fades the layer; drawn live, it is whole (the card's own layer fades it).
                            layer.alpha = 1f
                            drawLayer(layer)
                        }
                        // Its name row and apps take a drop after the last app (launcher polish 4.4).
                        .dropZone(drag, "card") { b -> tile?.let { DropMark.Area(Space.Card(it), b) } }
                        .swallowTaps(),
                ) {
                    FolderName(folder, home) { adding = true }
                    folder.folder.reason?.let {
                        Text(it, style = Type.body.copy(color = Txt, fontSize = 13.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    val slots = remember(folder, layout) { folder.apps.map { layout?.slotOf(it.second.spot) as? Slot.InFolder } }
                    Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        CellGrid(columns, Modifier.fillMaxWidth().padding(bottom = 10.dp), rowGap = 10.dp) {
                            folder.apps.forEachIndexed { i, (app, at) ->
                                val slot = slots[i] ?: return@forEachIndexed
                                key(slot.key) {
                                    Box(
                                        Modifier
                                            .dropZone(drag, "folder" to slot.key) { b -> DropMark.Item(slot, b, b.top + icon.top, icon.size) }
                                            .dragSource(drag, "folder" to slot.key) { b -> Source(b, slot, 1) }
                                            .animatePlacement(drag)
                                            .alpha(if (drag.hides(at.spot)) 0f else 1f),
                                        contentAlignment = Alignment.TopCenter,
                                    ) {
                                        MenuTile(app, At(LaunchFrom.FOLDER, folder.folder.name), home, width = 72.dp, spot = at, label = LocalHomeLook.current.labels, beforeOpen = onClose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else AppPicker(folder, home) { adding = false }
    }
}

/**
 * The open folder's name, edited in place (lor-gnk0), with the + that adds apps beside it. A tap
 * on the name edits it; Done, a tap away or the folder closing writes it with Undo, unless it is
 * blank or unchanged. Opened by the folder menu's Rename…, it starts focused with the name selected.
 */
@Composable
private fun FolderName(folder: ResolvedFolder, home: HomeContext, add: () -> Unit) {
    val name = folder.folder.name
    // Locked, the name is text and there is no + (lor-zzyy), as Lawnchair's `FolderNameEditText`
    // takes no touch while locked (Apache-2.0). The spacers keep it where the field would be.
    if (home.locked) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(48.dp))
            Text(name, style = Type.body.copy(color = Txt, fontSize = 20.sp, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            Spacer(Modifier.size(48.dp))
        }
        return
    }
    var text by remember(name) { mutableStateOf(TextFieldValue(name)) }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val latest by rememberUpdatedState(text.text)
    val at by rememberUpdatedState(folder.at)
    // The name written from this field while the folder still has [name], so a Done and the close
    // after it write once. Cleared whenever the name changes (the write landed, or an Undo put it
    // back), so the same text may be written again (review R1, lor-0368). One array for the field's
    // life, not one per name: the close's write runs the first composition's commit().
    val wrote = remember { arrayOfNulls<String>(1) }
    LaunchedEffect(name) { wrote[0] = null }
    fun commit() {
        val now = at.item as Folder
        val t = latest.trim()
        if (t.isEmpty() || t == now.name || t == wrote[0]) return
        wrote[0] = t
        home.actions.edit("${now.name} is $t now.") { it.renameFolder(at, t) }
    }
    DisposableEffect(Unit) { onDispose { commit() } }
    LaunchedEffect(Unit) {
        if (!home.ui.folderRename) return@LaunchedEffect
        home.ui.folderRename = false
        withFrameNanos {}
        text = text.copy(selection = TextRange(0, text.text.length))
        focus.requestFocus()
        keyboard?.show()
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(48.dp))
        BasicTextField(
            text,
            { text = it },
            Modifier.weight(1f)
                .focusRequester(focus)
                .onFocusChanged {
                    if (focused && !it.isFocused) commit()
                    focused = it.isFocused
                }
                .semantics { contentDescription = "Folder name" },
            singleLine = true,
            textStyle = Type.body.copy(color = Txt, fontSize = 20.sp, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Or),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            decorationBox = { inner ->
                // DESIGN.md's input focus: an amber edge at 70%, here the rule under the name.
                Box(
                    Modifier.fillMaxWidth().drawBehind { if (focused) drawRect(Or.copy(alpha = 0.7f), Offset(0f, size.height - 1.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { inner() }
            },
        )
        Box(
            Modifier.size(48.dp)
                .clickable(interactionSource = null, indication = PressScale.Tile, role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Add apps") { focusManager.clearFocus(); add() }
                .semantics { contentDescription = "Add apps to $name" }
                .drawBehind {
                    val arm = 9.dp.toPx()
                    val w = 1.5.dp.toPx()
                    drawLine(Txt, center - Offset(arm, 0f), center + Offset(arm, 0f), w)
                    drawLine(Txt, center - Offset(0f, arm), center + Offset(0f, arm), w)
                },
        )
    }
}

/**
 * The + of an open folder (lor-gnk0): the apps not in it, by name, to pick and add as one edit
 * with Undo. An app already on home elsewhere says where, and moves into the folder ([addToFolder]).
 * Back or Cancel goes back to the folder.
 */
@Composable
private fun AppPicker(folder: ResolvedFolder, home: HomeContext, done: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = done)
    val name = folder.folder.name
    val inFolder = remember(folder) { folder.apps.map { it.first.id }.toSet() }
    val apps = remember(home.apps, inFolder) {
        home.apps.filter { !home.hidden(it) && it.id !in inFolder }.sortedBy { it.label.lowercase() }
    }
    var picked by remember { mutableStateOf(listOf<AppId>()) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp).framedPanel().swallowTaps().padding(vertical = 16.dp),
    ) {
        Text("ADD TO ${name.uppercase()}", style = Type.title, modifier = Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.weight(1f).padding(top = 12.dp)) {
            items(apps, key = { it.key }) { app ->
                val on = app.id in picked
                val where = remember(home.file, app) {
                    home.file?.placesOf(app.id, home.installed)?.let { p -> p.folder?.let { "In $it" } ?: if (p.home) "On home" else null }
                }
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (on) Modifier.selection() else Modifier)
                        .toggleable(on, role = androidx.compose.ui.semantics.Role.Checkbox) { picked = if (on) picked - app.id else picked + app.id }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app, 36.dp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(app.label, style = Type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        where?.let { Text(it, style = Type.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = done, shape = RectangleShape, colors = DismissColors) { Text("Cancel") }
            androidx.compose.material3.Button(
                onClick = {
                    addToFolder(folder, home, picked)
                    done()
                },
                enabled = picked.isNotEmpty(),
                shape = RectangleShape,
            ) { Text(if (picked.isEmpty()) "Add" else "Add ${picked.size}") }
        }
    }
}

/**
 * Adds [picked] to [folder] with Undo. Home draws the result at once, and the open folder follows
 * it to its place, which moves up when an app ahead of it on its page went into it.
 */
private fun addToFolder(folder: ResolvedFolder, home: HomeContext, picked: List<AppId>) {
    val file = home.file ?: return
    val adding = picked.mapNotNull { id -> home.apps.firstOrNull { it.id == id }?.let { id to home.refFor(it) } }
    val d = try {
        file.addToFolder(folder.at, adding, home.installed)
    } catch (e: LayoutError) {
        return home.actions.refused(e.message ?: "they can't go there")
    }
    val at = d.at as Spot.Apps
    val added = adding.size
    home.drag.landing = d.file
    home.ui.folder = OpenFolder(at, folder.folder.name, (d.file.itemAt(at) as Folder).apps)
    val said = if (added == 1) "${home.apps.first { it.id == adding[0].first }.label} is in ${folder.folder.name}." else "$added apps added to ${folder.folder.name}."
    home.actions.edit(said, after = { home.drag.landing = null }) { now -> landOver(now, file, d.file) }
}

/**
 * Keeps taps from reaching what is under a panel. Unlike an empty `clickable`, it adds no
 * semantics, so the panel's apps stay separate nodes for TalkBack and agent-device.
 */
internal fun Modifier.swallowTaps(): Modifier = pointerInput(Unit) { detectTapGestures {} }

/** Where an app or folder tile's icon sits in its cell: 4 dp down, the look's icon size across. */
private class TileIcon(val top: Float, val size: Float)

@Composable
private fun rememberTileIcon(): TileIcon {
    val density = LocalDensity.current
    val size = iconSize
    return remember(density, size) { with(density) { TileIcon(4.dp.toPx(), size.toPx()) } }
}

private fun Rect.toBounds() = Bounds(left, top, right, bottom)

private fun Rect.toArray() = floatArrayOf(left, top, right, bottom)

/** Reports where this is as a drop zone while it is laid out; null for none. */
@Composable
internal fun Modifier.dropZone(drag: DragState, key: Any, zone: (Bounds) -> DropMark?): Modifier {
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
 * pass so nothing under it (the grid's finger, a list's scroll) sees the moves it takes.
 * A press on a source that is held past the long press (its menu opens) and then moves 16 dp picks
 * the source up; a lift or a move past the touch slop before the long press leaves the press to
 * whatever is under it.
 */
private fun Modifier.dragging(ui: HomeUi, haptics: HapticFeedback, home: () -> HomeContext, drop: (Held, Target?) -> Unit): Modifier = pointerInput(Unit) {
    val drag = ui.drag
    val start = 16.dp.toPx()
    val edge = 24.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        // The minimap's own drags aren't home's (lor-6get); under an open menu a press only closes it.
        if (ui.minimap || ui.popovers.open != null) return@awaitEachGesture
        val layer = when {
            ui.drawer -> 2
            ui.folder != null -> 1
            else -> 0
        }
        val source = drag.sourceAt(down.position, layer) ?: return@awaitEachGesture
        if (home().file == null || drag.held != null) return@awaitEachGesture
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
                // What is there now, in the file home draws now (4's review item (a)).
                val now = home()
                val file = now.file ?: return@awaitEachGesture
                val picked = now.pick(source.slot) ?: return@awaitEachGesture
                if (file.locked) {
                    // Locked, nothing is picked up (lor-zzyy), as Lawnchair's `Workspace.beginDragShared`
                    // starts no drag (Apache-2.0); here the bar says why, once. A folder's long press said it.
                    if (picked.item !is Folder) now.sayLocked()
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    // The rest of the press is taken, so the grid doesn't move under it either.
                    while (true) {
                        val m = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                        if (!m.pressed) return@awaitEachGesture
                        m.consume()
                    }
                }
                // A new pickup ends a settle at once, and ticks (launcher polish 7.5).
                drag.settling = null
                drag.held = Held(file, picked.from, picked.item, picked.app, picked.folder, now).also { it.finger = c.position }
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                if (source.layer == 2) {
                    ui.closeDrawer()
                    ui.leaveDrawer()
                }
                break
            }
        }
        val held = drag.held ?: return@awaitEachGesture
        val exit = FolderExit()
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
            val zones = held.work.current.zones(drag.zones.values.filter { !it.box.empty && it.box.centerX in 0f..width })
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
            // Out of an open folder's name row and apps for 400 ms closes it (lor-gnk0's rule).
            val area = ui.folder?.let { open -> zones.firstOrNull { (it as? Zone.Area)?.place == Place.InFolder(open.spot) } }
            if (area != null && exit.closes(area.box, c.position.x, c.position.y, c.uptimeMillis)) ui.folder = null
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
    val size = iconSize
    val half = with(density) { (size / 2).toPx() }
    Box(Modifier.offset { IntOffset((held.finger.x - half).roundToInt(), (held.finger.y - half).roundToInt()) }) {
        when {
            held.app != null -> AppIcon(held.app, size)
            held.folder != null -> FolderIcon(held.folder)
        }
    }
}

/**
 * The icon let go (launcher polish 7.5), from where the finger left it to its tile's icon, read
 * from the laid-out sources each frame so it goes where home now draws the tile. With no tile on
 * screen to go to, it fades where it is.
 */
@Composable
private fun SettleItem(s: Settle, drag: DragState, layout: Layout?, progress: () -> Float) {
    val density = LocalDensity.current
    val size = iconSize
    val half = with(density) { (size / 2).toPx() }
    val iconTop = with(density) { 4.dp.toPx() }
    var width by remember { mutableIntStateOf(0) }
    // The tile that is at [Settle.to] in the layout drawn, by what it is (4's review item (a)).
    val slot = remember(s, layout) { s.to?.let { layout?.slotOf(it) } }
    fun target(): Offset? {
        if (s.to == null) return null
        if (s.kind == Settle.Kind.FADE || slot == null) return null
        val src = drag.sources.values.firstOrNull { it.slot == slot && it.layer != 2 && !it.box.empty && it.box.centerX in 0f..width.toFloat() } ?: return null
        val b = src.box
        // A tile's icon sits 4 dp down.
        return Offset(b.centerX, b.top + iconTop + half)
    }
    Box(Modifier.fillMaxSize().onSizeChanged { width = it.width }) {
        Box(
            Modifier
                .offset {
                    val p = progress()
                    val at = target()?.let { lerp(s.from, it, p) } ?: s.from
                    IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt())
                }
                .graphicsLayer {
                    val p = progress()
                    val gone = target() == null
                    // Onto a tile it fades over the motion's last QUICK; with nowhere to go, over all of it.
                    val fadeFrom = 1f - Motion.QUICK.toFloat() / Motion.MOVE
                    alpha = when {
                        gone -> 1f - p
                        s.kind == Settle.Kind.ONTO -> if (p < fadeFrom) 1f else 1f - (p - fadeFrom) / (1f - fadeFrom)
                        else -> 1f
                    }
                },
        ) {
            when {
                s.app != null -> AppIcon(s.app, size)
                s.folder != null -> FolderIcon(s.folder)
            }
        }
    }
}

/** The Remove bar's height and margin. */
private val RemoveBarHeight = 56.dp
private val TopBarMargin = 12.dp

/** How far below the top a stack's first row starts, so the Remove bar covers none of it (6.8's critique). */
internal val BarClear = RemoveBarHeight + TopBarMargin * 2

/** Edit mode's bar, or Remove while something is held. */
@Composable
private fun TopBar(ui: HomeUi, modifier: Modifier) {
    val drag = ui.drag
    val held = drag.held
    if (held == null && !drag.editing) return
    val shape = RectangleShape
    if (held != null) {
        val lit = held.lit == Target.Remove
        Box(
            modifier.padding(TopBarMargin).fillMaxWidth().height(RemoveBarHeight)
                .background(PanelFace, shape)
                .background(if (lit) Or.copy(alpha = 0.08f) else Color.Transparent, shape)
                .border(1.dp, if (lit) Or.copy(alpha = 0.55f) else Edge, shape)
                .dropZone(drag, "remove") { b -> DropMark.Remove(b) },
            contentAlignment = Alignment.Center,
        ) {
            Text("Remove", style = Type.body.copy(color = if (lit) Txt else Mute, fontSize = 15.sp))
        }
        return
    }
    Row(
        modifier.padding(12.dp).fillMaxWidth().background(PanelFace, shape).border(1.dp, Edge, shape).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Drag apps to move them. Drop one on another to make a folder.",
            style = Type.body.copy(color = Txt, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.padding(start = 8.dp).background(Or).clickable { drag.editing = false }
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text("Done", style = Type.body.copy(color = OnOr, fontSize = 15.sp))
        }
    }
}

/**
 * A folder's menu (launcher polish 4.4): Rename…, Cover app…, Remove from home. Rename… opens the
 * folder with its name field focused ([rename]; lor-gnk0), where the name is edited in place.
 */
@Composable
private fun FolderMenu(folder: ResolvedFolder, home: HomeContext, icon: IconSpot, rename: () -> Unit, close: () -> Unit) {
    var covering by remember { mutableStateOf(false) }
    val at = folder.at
    val name = folder.folder.name
    val anchor = { icon.bounds()?.toBounds() }
    when {
        covering -> Popover(home.ui.popovers, anchor, close) {
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
        else -> Popover(home.ui.popovers, anchor, close) {
            FolderMenuItem("Rename…") { close(); rename() }
            FolderMenuItem("Cover app…") { covering = true }
            FolderMenuItem("Remove from home") { close(); home.actions.edit("$name is off home.") { it.removeAt(at) } }
        }
    }
}

@Composable
private fun FolderMenuItem(text: String, onClick: () -> Unit) = PopoverItem(text, onClick = onClick)
