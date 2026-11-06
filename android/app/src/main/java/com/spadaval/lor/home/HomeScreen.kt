package com.spadaval.lor.home

import androidx.compose.ui.graphics.RectangleShape
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
import kotlinx.coroutines.launch
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
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
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
    /** The drawer's target: open or not. Set from anywhere; [HomeScreen] moves [drawerShown] to it. */
    var drawer by mutableStateOf(false)

    /**
     * Where the drawer is, from 0 (closed) to 1 (open) (launcher polish 7.1). A finger sets it
     * while [drawerHeld]; otherwise one effect in [HomeScreen] animates it to [drawer].
     */
    var drawerShown by mutableFloatStateOf(0f)
    var drawerHeld by mutableStateOf(false)

    /** The velocity (drawer heights per second, up positive) a finger let the drawer go with. */
    var drawerFling = 0f

    /** Set by a home press that should jump rather than animate; each effect takes its own. */
    var snapDrawer = false
    var snapPager = false
    var snapFolder = false

    /** The open folder's tile icon, in root coordinates, when it was tapped (launcher polish 7.2). */
    var folderFrom: Rect? = null

    /**
     * How far a back gesture has gone, eased, from 0 to 1 (launcher polish 7.3): the drawer or the
     * open folder is drawn scaled by `1 - 0.1 × back`. Back to 0 when either opens again.
     */
    var back by mutableFloatStateOf(0f)

    /**
     * Raised when something opens while a back gesture's scale is still on it (a drawer caught, a
     * folder tapped mid-close): HomeScreen eases [back] to 0 rather than jumping it (7's review).
     */
    var backResets by mutableIntStateOf(0)

    /** The drawer's list is at its top, so a pull down there moves the drawer (launcher polish 7.1). */
    var drawerAtTop = true
    /** Where the drawer's list is, in root coordinates, its letter rail left out: where that pull starts. */
    var drawerPullArea: Rect? = null
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
    /**
     * The open app or folder menu (with a folder's Rename dialog and Cover list), as the token of
     * the tile it was opened from; at most one (launcher polish 7.3, Increment 1's R5).
     */
    var menu by mutableStateOf<Any?>(null)
    /** The Undo bar after an edit (launcher polish 4.2). */
    val undo = UndoBarState()
    /** What is held, and edit mode (launcher polish 4.3). */
    val drag = DragState()
    /** The pager's page showing: 0 is the agent page, 1 is home, 2 on are the pages after it. */
    var page by mutableStateOf(1)

    /** Home with nothing open over it: a home press here opens quick send instead. */
    val atRest: Boolean get() = !drawer && folder == null && homeMenu == null && menu == null && page == 1 && !drag.editing && drag.held == null

    /**
     * A home press ([animate] false when home wasn't on screen before it: then everything shuts
     * at once, since there's nothing to watch).
     */
    fun homePressed(animate: Boolean = true) {
        // A home press puts a held item back and leaves edit mode.
        snapDrawer = !animate
        snapPager = !animate
        snapFolder = !animate && folder != null
        drag.held = null
        drag.settling = null
        drag.editing = false
        drawer = false
        folder = null
        homeMenu = null
        // Every floating view closes, as Launcher3's onNewIntent closes them (launcher polish 7.3).
        menu = null
        query = ""
        homePresses++
    }
}

/** What a back gesture will close (launcher polish 7.3). */
private enum class Back { FOLDER, EDIT, DRAWER, PAGE }

/** How much smaller back draws the drawer or a folder at the gesture's end (Material's predictive back). */
private const val BACK_SCALE = 0.1f

/** The back gesture's progress eased as Android's `PathInterpolator(0, 0, 0, 1)`: quick at first, then slower. */
private val BackEasing = CubicBezierEasing(0f, 0f, 0f, 1f)

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
        placed?.pages?.getOrNull(open.spot.page)?.firstNotNullOfOrNull { (it as? Placed.InFolder)?.folder?.takeIf { f -> f.at.spot == open.spot && f.folder.name == open.name } }
    }
    // An edit that empties the open folder removes it, so the panel closes.
    LaunchedEffect(ui.folder, folder, placed) { if (ui.folder != null && folder == null && placed != null) ui.folder = null }
    // While something is held, an empty page follows the last, for a drop that makes one (launcher polish 4.5).
    val filePages = file?.current?.allPages?.size ?: 1
    val pageCount = filePages + if (held != null && held.work.current.pages.size < LayoutFile.MAX_PAGES) 1 else 0
    // A file changed from outside (an agent, adb) may hold another folder at the open one's place.
    // It closes at once: a reread puts things in place without motion (DESIGN.md's Motion).
    LaunchedEffect(reads) {
        if (ui.folder == null) return@LaunchedEffect
        ui.snapFolder = true
        ui.folder = null
    }
    val pager = rememberPagerState(initialPage = 1) { 1 + pageCount }
    // The system bars' icons follow the ink: dark over a light wallpaper on home's pages, light on
    // the agent page's veil, the drawer and an open folder (6's review, R1).
    // The drawer counts once any of it is under the status bar (launcher polish 7.1), decided in the
    // frame it crosses the bar rather than when a close starts.
    var heightPx by remember { mutableIntStateOf(0) }
    val barPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    val drawerUnderBar by remember(heightPx, barPx) { derivedStateOf { (1f - ui.drawerShown) * heightPx < barPx } }
    // An open folder, opening or closing, holds the scrim (launcher polish 7.2).
    var panel by remember { mutableStateOf<ResolvedFolder?>(null) }
    SideEffect { if (folder != null) panel = folder }
    val darkBarIcons = ink.ink == Ink.DARK && pager.currentPage >= 1 && !drawerUnderBar && folder == null && panel == null
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
    // The drawer goes to its target, unless a finger has it; a home press from elsewhere jumps (7.1).
    LaunchedEffect(ui.drawer, ui.drawerHeld, ui.homePresses) {
        if (ui.drawerHeld) return@LaunchedEffect
        val to = if (ui.drawer) 1f else 0f
        val fling = ui.drawerFling
        ui.drawerFling = 0f
        // An open starts whole, though a back gesture's close was caught before it ended (7's review, R2).
        if (to == 1f && ui.back > 0f) ui.backResets++
        if (ui.snapDrawer) {
            ui.snapDrawer = false
            ui.drawerShown = to
        } else if (ui.drawerShown != to) {
            // A finger that takes the drawer mid-settle owns it from that frame, before this effect is cancelled.
            animate(ui.drawerShown, to, fling, Motion.settle()) { v, _ -> if (!ui.drawerHeld) ui.drawerShown = v }
        }
    }
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
        val stripped = final.withCurrent(final.current.stripEmptyPages())
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
        drag.landing = final
        actions.edit(said ?: "Added.", after = { drag.landing = null }) { now ->
            if (now != h.original) throw LayoutError(HOME_CHANGED)
            final
        }
    }
    // The icon let go goes to where it went, then is gone (launcher polish 7.5).
    LaunchedEffect(drag.settling) {
        val s = drag.settling ?: return@LaunchedEffect
        s.progress.animateTo(1f, if (s.kind == Settle.Kind.FADE) Motion.quick() else Motion.move())
        if (drag.settling === s) drag.settling = null
    }
    // A folder opens from its tile and closes back into it, or fades where it is if its tile went away (7.2).
    val folderShown = remember { Animatable(0f) }
    val folderLayer = rememberGraphicsLayer()
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    var folderFading by remember { mutableStateOf(false) }
    val folderOpen = folder != null
    LaunchedEffect(folderOpen, ui.homePresses) {
        if (folderOpen) {
            folderFading = false
            ui.snapFolder = false
            if (ui.back > 0f) ui.backResets++
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
            val stays = placed?.pages?.getOrNull((spot as Spot.Page).page)?.any { (it as? Placed.InFolder)?.folder?.let { f -> f.at.spot == spot && f.folder.name == last.folder.name } == true } == true
            folderFading = !stays
            folderShown.animateTo(0f, if (stays) Motion.move() else Motion.quick())
        }
        panel = null
    }
    LaunchedEffect(ui.homePresses) {
        if (ui.homePresses == 0) return@LaunchedEffect
        if (ui.snapPager) {
            ui.snapPager = false
            pager.scrollToPage(1)
        } else pager.animateScrollToPage(1)
    }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { ui.page = it } }
    // Back closes what is open, in this order, and shows where it is going while the gesture lasts
    // for the drawer and a folder (launcher polish 7.3): each scales toward 0.9 with the progress
    // (Kvaesitso's `LauncherScaffold`, GPL-3.0, ideas only; Lawnchair's `onBackProgressed`, Apache-2.0).
    // Before Android 14 only the commit arrives, and it is the whole behaviour.
    val backScope = rememberCoroutineScope()
    // A new open eases away what is left of a back gesture's scale, as a cancelled gesture does.
    LaunchedEffect(ui.backResets) {
        if (ui.back > 0f) animate(ui.back, 0f, animationSpec = Motion.quick()) { v, _ -> ui.back = v }
    }
    PredictiveBackHandler(enabled = ui.folder != null || ui.drawer || pager.currentPage != 1 || drag.editing) { events ->
        val what = when {
            ui.folder != null -> Back.FOLDER
            drag.editing -> Back.EDIT
            ui.drawer -> Back.DRAWER
            else -> Back.PAGE
        }
        try {
            events.collect { e -> if (what == Back.FOLDER || what == Back.DRAWER) ui.back = BackEasing.transform(e.progress) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // A cancelled gesture returns it to full size.
            val from = ui.back
            backScope.launch { animate(from, 0f, animationSpec = Motion.quick()) { v, _ -> ui.back = v } }
            throw e
        }
        // The commit: the close runs from where the gesture left it; the scale goes with it.
        when (what) {
            Back.FOLDER -> ui.folder = null
            Back.EDIT -> drag.editing = false
            Back.DRAWER -> { ui.drawer = false; ui.query = "" }
            Back.PAGE -> ui.homePresses++
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
    Box(Modifier.fillMaxSize().onSizeChanged { heightPx = it.height }.dragging(ui, haptics, { latestHome }, { h, t -> latestDrop(h, t) }).drawerPull(ui, haptics)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, userScrollEnabled = held == null) { page ->
            if (page == 0) AgentPage() else CompositionLocalProvider(LocalWallInk provides ink) { HomePage(page - 1, placed, layout, appsLoad, ui, home) }
        }
        // On the agent page the dots sit on its dark veil, not the wallpaper (6.8: dark ink vanished there).
        PageDots(pager.currentPage, 1 + pageCount, if (pager.currentPage == 0) SurfaceInk.mark else ink.mark, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 96.dp))
        // The drawer, wherever the finger or its settle has it (launcher polish 7.1); home stays still under it.
        val drawerShows by remember { derivedStateOf { ui.drawerShown > 0f } }
        if (drawerShows) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    translationY = (1f - ui.drawerShown) * size.height
                    // Predictive back (7.3): smaller about its centre as the gesture goes.
                    val b = if (ui.folder == null && panel == null) 1f - BACK_SCALE * ui.back else 1f
                    scaleX = b
                    scaleY = b
                },
            ) {
                Drawer(remember(apps, home) { apps.filterNot(home::hidden) }, stats, search, ui, home)
            }
        }
        if (folder != null) {
            FolderPanel(folder, home, onClose = { ui.folder = null }, { folderShown.value }, { 1f - BACK_SCALE * ui.back }, ui.folderFrom, folderLayer) { cardBounds = it }
        } else if (panel != null) {
            // Closing, it is a picture of itself: no drop zones, sources or taps, so a drag that left it
            // can't drop into it and a tap reaches home.
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val p = folderShown.value
                    drawRect(Scrim, alpha = p)
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
        // Edit mode's bar, and Remove while something is held (launcher polish 4.3).
        TopBar(ui, Modifier.align(Alignment.TopCenter).safeDrawingPadding())
        held?.let { HeldItem(it) }
        drag.settling?.let { SettleItem(it, drag) { it.progress.value } }
        // Above the drawer and folders, clear of the dock.
        UndoBar(ui.undo, actions::undo, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 112.dp))
    }
    }
}

@Composable
private fun HomePage(n: Int, placed: PlacedHome?, layout: LayoutState, appsLoad: AppsLoad, ui: HomeUi, home: HomeContext) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // A swipe up brings the drawer with the finger (launcher polish 7.1).
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (ui.drawer || ui.drag.held != null) return@awaitEachGesture
                    val tracker = VelocityTracker()
                    val first = awaitVerticalSlop(down, tracker, PointerEventPass.Main) ?: return@awaitEachGesture
                    // Only a swipe that starts upward is the drawer's.
                    if (first.position.y > down.position.y) return@awaitEachGesture
                    followDrawer(down, first, DrawerFinger(ui, haptics, size.height.toFloat(), density, fromOpen = false), tracker, PointerEventPass.Main)
                }
            }
            .safeDrawingPadding(),
    ) {
        // Under everything: a long press that no app or folder took, on empty home, opens the home
        // menu, and a double tap opens quick send (launcher polish 7.6), not in edit mode.
        // Overlapping siblings get a touch only when the ones drawn above it don't, so a tap on an
        // app is never delayed or taken.
        Box(
            Modifier.matchParentSize().pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { if (!ui.drag.editing) home.actions.quickSend() },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        ui.homeMenu = it
                        ui.homeMenuPage = n
                    },
                )
            },
        )
        val wide = maxWidth >= 600.dp
        val widthDp = maxWidth.value
        // Only home's first page has the header (launcher polish 6.7: date, clock or none); the pages after it start where its grid would.
        val headerStyle = LocalHomeLook.current.header
        val header = @Composable {
            if (n == 0 && headerStyle != HomeLook.Header.NONE) HomeHeader(headerStyle, if (wide) 40 else 30, home.actions::calendar)
            else Spacer(Modifier.height(if (wide || headerStyle == HomeLook.Header.NONE) 24.dp else 104.dp))
        }
        val body = @Composable { mod: Modifier ->
            Column(mod) {
                val columns = LocalHomeLook.current.columnsFor(widthDp).home
                if (n == 0 && layout is LayoutState.Broken) {
                    BrokenBanner(
                        layout, appsLoad == AppsLoad.LOADED, Type.body.copy(color = Txt, fontSize = 13.sp),
                        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth()
                            .framedPanel(StateFill)
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

@Composable
private fun DockSkeleton(count: Int) {
    if (count == 0) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .framedPanel(PanelFace.copy(alpha = 0.92f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(count) { Box(Modifier.size(iconSize).background(Line, CircleShape)) }
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
    // Keyed by what each tile is, so a tile moved by a drag eases from its old place (launcher polish 7.5).
    val keys = remember(items) { itemKeys(items.map { it.at.item }) }
    CellGrid(columns, rowGap = 14.dp, modifier = Modifier.padding(horizontal = 8.dp)) {
        items.forEachIndexed { i, item ->
            key(keys[i]) {
                val spot = item.at.spot
                val tile = (spot as Spot.Page).page to keys[i]
                Box(
                    Modifier
                        .dropZone(drag, tile) { b -> Zone.Item(spot, b, b.top + icon.top, icon.size) }
                        .dragSource(drag, tile) { b ->
                            Source(b, item.at, 0, (item as? Placed.App)?.app, (item as? Placed.InFolder)?.folder)
                        }
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
                            ui.folder = OpenFolder(item.folder.at.spot as Spot.Page, item.folder.folder.name)
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

@Composable
private fun Dock(apps: List<Pair<HomeApp, From.At>>, home: HomeContext, page: Int) {
    // An empty dock shows while something is held, so it can take it.
    if (apps.isEmpty() && home.drag.held == null) return
    val drag = home.drag
    val dockIcon = iconSize
    val iconPx = with(LocalDensity.current) { dockIcon.toPx() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .framedPanel(PanelFace.copy(alpha = 0.92f))
            .dropZone(drag, "dock$page") { b -> Zone.Area(Place.Dock, b) }
            .padding(vertical = 10.dp)
            .height(dockIcon),
    ) {
        val at = At(LaunchFrom.DOCK)
        val keys = remember(apps) { itemKeys(apps.map { it.second.item }) }
        apps.forEachIndexed { i, (app, spot) -> key(keys[i]) {
            // Equal cells, so the space between icons belongs to one of them.
            Box(
                Modifier.weight(1f)
                    // Keyed by page too: the dock is drawn on every page, and neighbours stay composed.
                    .dropZone(drag, "dock$page" to keys[i]) { b -> Zone.Item(spot.spot, b, b.top, iconPx) }
                    .dragSource(drag, "dock$page" to keys[i]) { b -> Source(b, spot, 0, app, null) }
                    .animatePlacement(drag)
                    .alpha(if (drag.hides(spot.spot)) 0f else 1f),
                contentAlignment = Alignment.Center,
            ) {
                val icon = remember { IconSpot() }
                WithAppMenu(app, at, home, spot, icon) { menu ->
                    Box {
                        AppIcon(
                            app, dockIcon,
                            Modifier.iconSpot(icon).semantics { contentDescription = app.label }
                                .combinedClickable(onLongClickLabel = "Menu", onLongClick = menu) { if (!drag.editing) home.actions.open(app, at, icon) },
                        )
                        if (app.dotKey in LocalDots.current) Dot()
                    }
                }
            }
        } }
    }
}

@Composable
private fun PageDots(current: Int, count: Int, mark: Color, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until count).forEach { page ->
            val color = if (page == current) mark.copy(alpha = 0.8f) else mark.copy(alpha = 0.5f)
            Box(Modifier.size(6.dp).background(color, if (page == 0) RectangleShape else CircleShape))
        }
    }
}

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
            .combinedClickable(onLongClickLabel = onLongClick?.let { "Menu" }, onLongClick = onLongClick, onClick = onClick)
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
    if (menu) FolderMenu(folder, home) { menu = false }
    val label = LocalHomeLook.current.labels
    Column(
        Modifier.fillMaxWidth().widthIn(max = 76.dp)
            .then(if (label) Modifier else Modifier.semantics { contentDescription = folder.folder.name })
            .combinedClickable(onLongClickLabel = "Menu", onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menu = true
            }, onClick = { onClick(icon[0]) })
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.onGloballyPositioned { icon[0] = it.boundsInRoot() }) {
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

/** A folder's icon: its cover app (launcher polish 4.1), else its first four apps. */
@Composable
private fun FolderIcon(folder: ResolvedFolder) {
    if (folder.cover != null) {
            AppIcon(folder.cover, iconSize)
        } else Column(
            Modifier
                .size(iconSize)
                .background(LocalWallInk.current.text.copy(alpha = 0.09f)).border(1.dp, Edge)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            folder.apps.take(4).map { it.first }.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { pair.forEach { AppIcon(it, (iconSize - 16.dp) / 2) } }
            }
        }
}

@Composable
private fun FolderPanel(folder: ResolvedFolder, home: HomeContext, onClose: () -> Unit, shown: () -> Float, back: () -> Float, from: Rect?, layer: GraphicsLayer, onCard: (Rect) -> Unit) {
    var card by remember { mutableStateOf<Rect?>(null) }
    Box(
        Modifier.fillMaxSize().drawBehind { drawRect(Scrim, alpha = shown()) }.pointerInput(onClose) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        val drag = home.drag
        val icon = rememberTileIcon()
        val spot = folder.at.spot as Spot.Page
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .padding(24.dp)
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
                .framedPanel(PanelFace.copy(alpha = 0.95f))
                // The card takes a drop after the folder's last app (launcher polish 4.4).
                .dropZone(drag, "card") { b -> Zone.Area(Place.InFolder(spot), b) }
                .swallowTaps()
                .padding(16.dp),
        ) {
            Text(folder.folder.name, style = Type.body.copy(color = Txt, fontSize = 18.sp), modifier = Modifier.padding(start = 6.dp))
            folder.folder.reason?.let {
                Text(it, style = Type.body.copy(color = Mute, fontSize = 13.sp), modifier = Modifier.padding(start = 6.dp, top = 4.dp))
            }
            Spacer(Modifier.height(14.dp))
            val keys = remember(folder) { itemKeys(folder.apps.map { it.second.item }) }
            CellGrid(4, Modifier.fillMaxWidth().padding(bottom = 10.dp), rowGap = 10.dp) {
                folder.apps.forEachIndexed { i, (app, at) ->
                    key(keys[i]) {
                        Box(
                            Modifier
                                .dropZone(drag, "folder" to keys[i]) { b -> Zone.Item(at.spot, b, b.top + icon.top, icon.size) }
                                .dragSource(drag, "folder" to keys[i]) { b -> Source(b, at, 1, app, null) }
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
private fun Modifier.dragging(ui: HomeUi, haptics: HapticFeedback, home: () -> HomeContext, drop: (Held, Target?) -> Unit): Modifier = pointerInput(Unit) {
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
                // A new pickup ends a settle at once, and ticks (launcher polish 7.5).
                drag.settling = null
                drag.held = Held(file, source.from, item, source.app, source.folder, home()).also { it.finger = c.position }
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
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
private fun SettleItem(s: Settle, drag: DragState, progress: () -> Float) {
    val density = LocalDensity.current
    val size = iconSize
    val half = with(density) { (size / 2).toPx() }
    val iconTop = with(density) { 4.dp.toPx() }
    var width by remember { mutableIntStateOf(0) }
    fun target(): Offset? {
        val to = s.to ?: return null
        if (s.kind == Settle.Kind.FADE) return null
        val src = drag.sources.values.firstOrNull { (it.from as? From.At)?.spot == to && it.layer != 2 && !it.box.empty && it.box.centerX in 0f..width.toFloat() } ?: return null
        val b = src.box
        // The dock's icons are centred in their cells; a tile's sits 4 dp down.
        return Offset(b.centerX, if (to is Spot.Dock) (b.top + b.bottom) / 2 else b.top + iconTop + half)
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
            modifier.padding(12.dp).fillMaxWidth().height(56.dp)
                .background(PanelFace, shape)
                .background(if (lit) Or.copy(alpha = 0.08f) else Color.Transparent, shape)
                .border(1.dp, if (lit) Or.copy(alpha = 0.55f) else Edge, shape)
                .dropZone(drag, "remove") { b -> Zone.Remove(b) },
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
                    TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { close(); if (text.trim() != name) home.actions.edit("$name is ${text.trim()} now.") { it.renameFolder(at, text) } }) { Text("Rename") }
                },
                dismissButton = { TextButton(shape = androidx.compose.ui.graphics.RectangleShape, colors = DismissColors, onClick = close) { Text("Cancel") } },
            )
        }
        covering -> DropdownMenu(true, close, containerColor = Raised, shape = androidx.compose.ui.graphics.RectangleShape) {
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
        else -> DropdownMenu(true, close, containerColor = Raised, shape = androidx.compose.ui.graphics.RectangleShape) {
            FolderMenuItem("Rename…") { renaming = true }
            FolderMenuItem("Cover app…") { covering = true }
            FolderMenuItem("Remove from home") { close(); home.actions.edit("$name is off home.") { it.removeAt(at) } }
        }
    }
}

@Composable
private fun FolderMenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, style = Type.body.copy(color = Txt, fontSize = 15.sp)) }, onClick = onClick)
}

/**
 * A finger moving the drawer (launcher polish 7.1): 1:1 with its travel, a tick at the commit
 * point either way, and on release the drawer settles where [DrawerMotion.settles] says, from the
 * finger's velocity.
 */
private class DrawerFinger(val ui: HomeUi, val haptics: HapticFeedback, val heightPx: Float, val density: Float, val fromOpen: Boolean) {
    private val start = ui.drawerShown
    private val point = DrawerMotion.commitPoint(fromOpen, start)
    init {
        ui.drawerHeld = true
        if (ui.back > 0f) ui.backResets++
    }

    /** The finger is [travel] px below where it pressed (up negative). */
    fun move(travel: Float) {
        val before = ui.drawerShown
        val now = (start - travel / heightPx).coerceIn(0f, 1f)
        ui.drawerShown = now
        if (DrawerMotion.crossed(point, before, now)) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    /** Let go at [vy] px/s (down positive). */
    fun release(vy: Float) {
        val open = DrawerMotion.settles(fromOpen, start, ui.drawerShown, -vy / density)
        android.util.Log.d("LorMotion", "drawer let go at ${ui.drawerShown} from $start, ${-vy / density} dp/s up: ${if (open) "open" else "closed"}")
        ui.drawerFling = -vy / heightPx
        ui.drawer = open
        if (!open) ui.query = ""
        ui.drawerHeld = false
    }
}

/**
 * The pull down that closes the open drawer from the top of its list (launcher polish 7.1),
 * measured on home's root, which doesn't move, in the Initial pass as [dragging] is: the list moves
 * with the drawer, so its own scroll deltas would make the drawer lag. A press in the list (not its
 * letter rail) with the list at its top, moved down past the touch slop, is taken.
 */
private fun Modifier.drawerPull(ui: HomeUi, haptics: HapticFeedback): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val area = ui.drawerPullArea
        if (!ui.drawer || ui.drawerShown < 1f || !ui.drawerAtTop || ui.drag.held != null || area == null || !area.contains(down.position)) return@awaitEachGesture
        val tracker = VelocityTracker()
        // Only a pull that starts downward is the drawer's.
        val first = awaitVerticalSlop(down, tracker, PointerEventPass.Initial) ?: return@awaitEachGesture
        if (first.position.y < down.position.y) return@awaitEachGesture
        followDrawer(down, first, DrawerFinger(ui, haptics, size.height.toFloat(), density, fromOpen = true), tracker, PointerEventPass.Initial)
    }
}

/**
 * Waits for [down]'s finger to pass the touch slop; returns that move when it went mostly
 * vertically, else null (a tap, a lift, a sideways swipe, or a move something else took).
 */
private suspend fun AwaitPointerEventScope.awaitVerticalSlop(down: PointerInputChange, tracker: VelocityTracker, pass: PointerEventPass): PointerInputChange? {
    tracker.addPosition(down.uptimeMillis, down.position)
    while (true) {
        val c = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id } ?: return null
        if (!c.pressed || c.isConsumed) return null
        tracker.addPosition(c.uptimeMillis, c.position)
        val d = c.position - down.position
        if (d.getDistance() > viewConfiguration.touchSlop) return if (kotlin.math.abs(d.y) > kotlin.math.abs(d.x)) c else null
    }
}

/** Moves the drawer with [down]'s finger from [first] on, measured from where it pressed, and lets it go on the lift. */
private suspend fun AwaitPointerEventScope.followDrawer(down: PointerInputChange, first: PointerInputChange, finger: DrawerFinger, tracker: VelocityTracker, pass: PointerEventPass) {
    first.consume()
    finger.move(first.position.y - down.position.y)
    var vy = 0f
    // Released however the gesture ends, a cancelled pointer coroutine too, so the drawer is never left held.
    try {
        while (true) {
            val c = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id } ?: return
            c.consume()
            tracker.addPosition(c.uptimeMillis, c.position)
            finger.move(c.position.y - down.position.y)
            if (!c.pressed) {
                vy = tracker.calculateVelocity().y
                return
            }
        }
    } finally {
        finger.release(vy)
    }
}
