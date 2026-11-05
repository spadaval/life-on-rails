package com.spadaval.lor.home

import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val Ink = Color(0xFFECEBE6)
private val Muted = Color(0xFF9097A1)
private val Dim = Color(0xFF616871)
private val Line = Color(0x14FFFFFF)
private val Panel = Color(0xF215181D)
private val Sheet = Color(0xFF0B0D10)
private val LabelStyle = TextStyle(color = Ink, fontSize = 12.sp, shadow = Shadow(Color(0x99000000), Offset(0f, 1f), 3f))

/** What the activity can reset on a home press. */
class HomeUi {
    var drawer by mutableStateOf(false)
    var folder by mutableStateOf<ResolvedFolder?>(null)
    var query by mutableStateOf("")
    var homePresses by mutableStateOf(0)

    fun homePressed() {
        drawer = false
        folder = null
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

private fun resolve(ref: AppRef, apps: List<HomeApp>): HomeApp? {
    val me = Process.myUserHandle()
    return apps.firstOrNull { it.user == me && ref.matches(it.packageName, it.info.componentName.className) }
}

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
fun HomeScreen(apps: List<HomeApp>, layout: LayoutState, ui: HomeUi, launch: (HomeApp) -> Unit) {
    val pager = rememberPagerState(initialPage = 1) { 2 }
    LaunchedEffect(ui.homePresses) { if (ui.homePresses > 0) pager.animateScrollToPage(1) }
    BackHandler(enabled = ui.folder != null || ui.drawer || pager.currentPage != 1) {
        when {
            ui.folder != null -> ui.folder = null
            ui.drawer -> { ui.drawer = false; ui.query = "" }
            else -> ui.homePresses++
        }
    }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            if (page == 0) AgentPagePlaceholder() else HomePage(apps, layout, ui, launch)
        }
        PageDots(pager.currentPage, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 96.dp))
        AnimatedVisibility(
            ui.drawer,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            Drawer(apps, ui, launch)
        }
        ui.folder?.let { FolderPanel(it, launch, onClose = { ui.folder = null }) }
    }
}

@Composable
private fun AgentPagePlaceholder() {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 28.dp)) {
        Text("Agent page", style = LabelStyle.copy(fontSize = 22.sp))
        Spacer(Modifier.height(8.dp))
        Text("Today, running turns and what needs you will be here.", style = LabelStyle.copy(fontSize = 14.sp))
    }
}

@Composable
private fun HomePage(apps: List<HomeApp>, layout: LayoutState, ui: HomeUi, launch: (HomeApp) -> Unit) {
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    val placed = remember(apps, layout) { (layout as? LayoutState.Ready)?.let { place(it.file.current, apps) } }
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
        val wide = maxWidth >= 600.dp
        val header = @Composable { DateHeader(if (wide) 40 else 30) }
        val body = @Composable { mod: Modifier ->
            Column(mod) {
                when (layout) {
                    LayoutState.Loading -> {}
                    is LayoutState.Broken -> Text(
                        "layout.json can't be used: ${layout.message}",
                        style = TextStyle(color = Muted, fontSize = 13.sp),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    is LayoutState.Ready -> HomeGrid(placed!!.first, if (wide) 5 else 4, ui, launch)
                }
                Spacer(Modifier.weight(1f))
                Dock(placed?.second.orEmpty(), launch)
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
    }
}

@Composable
private fun DateHeader(size: Int) {
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM")) }
    Text(today, style = LabelStyle.copy(fontSize = size.sp), modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 28.dp))
}

/** Home's items in reading order, [columns] to a row: Launcher3's cells without coordinates. */
@Composable
private fun HomeGrid(items: List<Placed>, columns: Int, ui: HomeUi, launch: (HomeApp) -> Unit) {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        when (item) {
                            is Placed.App -> AppTile(item.app) { launch(item.app) }
                            is Placed.InFolder -> FolderTile(item.folder) { ui.folder = item.folder }
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Dock(apps: List<HomeApp>, launch: (HomeApp) -> Unit) {
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
        apps.forEach { app -> AppIcon(app, 48.dp, Modifier.semantics { contentDescription = app.label }.clickable { launch(app) }) }
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

@Composable
private fun AppTile(app: HomeApp, width: Dp = 76.dp, onClick: () -> Unit) {
    Column(
        Modifier.width(width).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, 52.dp)
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
private fun FolderPanel(folder: ResolvedFolder, launch: (HomeApp) -> Unit, onClose: () -> Unit) {
    val none = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable(none, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 360.dp)
                .padding(24.dp)
                .background(Panel, RoundedCornerShape(24.dp))
                .border(1.dp, Line, RoundedCornerShape(24.dp))
                .clickable(none, indication = null) {}
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
                            AppTile(app, width = 72.dp) { onClose(); launch(app) }
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Drawer(apps: List<HomeApp>, ui: HomeUi, launch: (HomeApp) -> Unit) {
    val results = remember(apps, ui.query) { AppSearch.search(ui.query, apps) { it.label } }
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
    Column(
        Modifier.fillMaxSize().background(Sheet).clickable(remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding().padding(top = 12.dp),
    ) {
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
            keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let(launch) }),
        )
        if (results.isEmpty() && ui.query.isNotBlank()) {
            Text("No app called \"${ui.query.trim()}\".", style = TextStyle(color = Muted, fontSize = 14.sp), modifier = Modifier.padding(24.dp))
        }
        LazyVerticalGrid(
            GridCells.Adaptive(80.dp),
            Modifier.fillMaxSize().nestedScroll(pullToClose),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(results, key = { it.key }) { app ->
                Box(contentAlignment = Alignment.TopCenter) { AppTile(app) { launch(app) } }
            }
        }
    }
}
