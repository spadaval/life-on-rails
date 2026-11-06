package com.spadaval.lor.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    /** The open folder's name. */
    var folder by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var homePresses by mutableStateOf(0)
    /** Where on home a long press on empty space opened the home menu. */
    var homeMenu by mutableStateOf<Offset?>(null)
    /** The page showing: 0 is the agent page, 1 is home. */
    var page by mutableStateOf(1)

    /** Home with nothing open over it: a home press here opens quick send instead. */
    val atRest: Boolean get() = !drawer && folder == null && homeMenu == null && page == 1

    fun homePressed() {
        drawer = false
        folder = null
        homeMenu = null
        query = ""
        homePresses++
    }
}

class ResolvedFolder(val folder: Folder, val apps: List<HomeApp>)

/** A layout item with its installed apps; refs to apps that aren't installed are left out. */
private sealed interface Placed {
    class App(val app: HomeApp) : Placed
    class InFolder(val folder: ResolvedFolder) : Placed
}

private fun resolve(ref: AppRef, apps: List<HomeApp>): HomeApp? =
    apps.firstOrNull { it.mainProfile && ref.matches(it.packageName, it.info.componentName.className) }

private fun place(layout: Layout, apps: List<HomeApp>): Pair<List<Placed>, List<HomeApp>> {
    val home = layout.home.mapNotNull { item ->
        when (item) {
            is AppRef -> resolve(item, apps)?.let(Placed::App)
            is Folder -> item.apps.mapNotNull { resolve(it, apps) }.takeIf { it.isNotEmpty() }
                ?.let { Placed.InFolder(ResolvedFolder(item, it)) }
        }
    }
    return home to layout.dock.mapNotNull { resolve(it, apps) }
}

@Composable
fun HomeScreen(apps: List<HomeApp>, appsLoad: AppsLoad, layout: LayoutState, stats: LaunchStats, ui: HomeUi, actions: HomeActions) {
    // While the file is broken, everything draws from the last good layout (launcher polish 3.1).
    val file = layout.shown
    val home = remember(actions, file, apps) { HomeContext(actions, file, apps) }
    val placed = remember(apps, file) { file?.let { place(it.current, apps) } }
    val folder = ui.folder?.let { name -> placed?.first?.firstNotNullOfOrNull { (it as? Placed.InFolder)?.folder?.takeIf { f -> f.folder.name == name } } }
    // An edit that empties the open folder removes it, so the panel closes.
    LaunchedEffect(ui.folder, folder, placed) { if (ui.folder != null && folder == null && placed != null) ui.folder = null }
    val pager = rememberPagerState(initialPage = 1) { 2 }
    LaunchedEffect(ui.homePresses) { if (ui.homePresses > 0) pager.animateScrollToPage(1) }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { ui.page = it } }
    BackHandler(enabled = ui.folder != null || ui.drawer || pager.currentPage != 1) {
        when {
            ui.folder != null -> ui.folder = null
            ui.drawer -> { ui.drawer = false; ui.query = "" }
            else -> ui.homePresses++
        }
    }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            if (page == 0) AgentPage() else HomePage(placed, layout, appsLoad, ui, home)
        }
        PageDots(pager.currentPage, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 96.dp))
        AnimatedVisibility(
            ui.drawer,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            Drawer(remember(apps, home) { apps.filterNot(home::hidden) }, stats, ui, home)
        }
        folder?.let { FolderPanel(it, home, onClose = { ui.folder = null }) }
    }
}

@Composable
private fun HomePage(placed: Pair<List<Placed>, List<HomeApp>>?, layout: LayoutState, appsLoad: AppsLoad, ui: HomeUi, home: HomeContext) {
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
                })
            },
        )
        val wide = maxWidth >= 600.dp
        val header = @Composable { DateHeader(if (wide) 40 else 30) }
        val body = @Composable { mod: Modifier ->
            Column(mod) {
                val columns = if (wide) 5 else 4
                if (layout is LayoutState.Broken) {
                    BrokenBanner(
                        layout, appsLoad == AppsLoad.LOADED, TextStyle(color = Ink, fontSize = 13.sp),
                        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth()
                            .background(StateFill, RoundedCornerShape(14.dp)).border(1.dp, Line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp),
                        home.actions::restore,
                    )
                }
                // Until the first run is done (launcher polish 3.5); the default layout is drawn under it.
                SetupCard(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp))
                val body = homeBody(appsLoad, home.file, placed?.first?.size ?: 0)
                when (body) {
                    HomeBody.NOTHING -> {}
                    HomeBody.SKELETON -> Skeleton(home.file!!.current.home, columns)
                    HomeBody.APPS_DIDNT_LOAD -> HomeNote("Apps didn't load. Trying again.", null)
                    HomeBody.EMPTY -> HomeNote("Home is empty.", "Swipe up for your apps. Long-press one and choose Add to home.") { ui.drawer = true }
                    HomeBody.GRID -> HomeGrid(placed!!.first, columns, ui, home)
                }
                Spacer(Modifier.weight(1f))
                if (body == HomeBody.SKELETON) DockSkeleton(home.file!!.current.dock.size) else Dock(placed?.second.orEmpty(), home)
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
        ui.homeMenu?.let { HomeMenu(it, home) { ui.homeMenu = null } }
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
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        when (item) {
                            is Placed.App -> MenuTile(item.app, At(LaunchFrom.HOME), home)
                            is Placed.InFolder -> FolderTile(item.folder) { ui.folder = item.folder.folder.name }
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Dock(apps: List<HomeApp>, home: HomeContext) {
    if (apps.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .background(Color(0x10FFFFFF), RoundedCornerShape(22.dp))
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        val at = At(LaunchFrom.DOCK)
        apps.forEach { app ->
            WithAppMenu(app, at, home) { menu ->
                AppIcon(
                    app, 48.dp,
                    Modifier.semantics { contentDescription = app.label }
                        .combinedClickable(onLongClickLabel = "Menu", onLongClick = menu) { home.actions.open(app, at) },
                )
            }
        }
    }
}

@Composable
private fun PageDots(current: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..1).forEach { page ->
            val color = if (page == current) Color(0xB0FFFFFF) else Color(0x30FFFFFF)
            Box(Modifier.size(6.dp).background(color, if (page == 0) RoundedCornerShape(2.dp) else CircleShape))
        }
    }
}

/** An app tile whose tap opens it from [at] and whose long press opens its menu. */
@Composable
internal fun MenuTile(app: HomeApp, at: At, home: HomeContext, width: Dp = 76.dp, dot: Boolean = false, beforeOpen: () -> Unit = {}) {
    WithAppMenu(app, at, home) { menu ->
        AppTile(app, width, menu, dot) { beforeOpen(); home.actions.open(app, at) }
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
private fun FolderTile(folder: ResolvedFolder, onClick: () -> Unit) {
    Column(
        Modifier.width(76.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .size(52.dp)
                .background(Color(0x18FFFFFF), RoundedCornerShape(15.dp))
                .border(1.dp, Line, RoundedCornerShape(15.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            folder.apps.take(4).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { pair.forEach { AppIcon(it, 18.dp) } }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(folder.folder.name, style = LabelStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FolderPanel(folder: ResolvedFolder, home: HomeContext, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).pointerInput(onClose) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .padding(24.dp)
                .background(Panel, RoundedCornerShape(24.dp))
                .border(1.dp, Line, RoundedCornerShape(24.dp))
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
                    row.forEach { app ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            MenuTile(app, At(LaunchFrom.FOLDER, folder.folder.name), home, width = 72.dp, beforeOpen = onClose)
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
