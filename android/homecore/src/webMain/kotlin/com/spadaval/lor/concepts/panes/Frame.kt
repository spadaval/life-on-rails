package com.spadaval.lor.concepts.panes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.BlurredWallpaper
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.KeyboardHeight
import com.spadaval.lor.home.BackHandler
import com.spadaval.lor.lab.labAnchor
import kotlinx.coroutines.launch

/** The cover sheet's two rest points, as fractions of the page's height: half shows the list and the item; full is for reading. */
const val HALF = 0.52f
const val FULL = 0.93f

/** The ground of a panel page: the wallpaper blurred under most of the ground's colour, with the dashboard's two faint glows. */
@Composable
fun PanelGround() {
    BlurredWallpaper({ 1f })
    Box(
        Modifier.fillMaxSize().background(P.veil).drawBehind {
            drawRect(Brush.radialGradient(listOf(P.glowBlue, Color.Transparent), center = Offset(size.width * 0.9f, -size.height * 0.05f), radius = size.width * 1.1f))
            drawRect(Brush.radialGradient(listOf(P.glowAmber, Color.Transparent), center = Offset(-size.width * 0.1f, size.height * 1.05f), radius = size.width * 0.95f))
        },
    )
}

/**
 * A panel: its list pane and its detail. Unfolded they sit side by side, the detail sliding in over
 * the page's overview when something opens. On the cover the list is the page and the detail is a
 * sheet that rises over it and rests at half height, so the list above stays live; pulled to full,
 * the list steps back and blurs, so where you are stays visible.
 *
 * [head] is the list pane's header, [list] its rows, [detail] the open item (or null), [overview]
 * what the detail side shows with nothing open (inner screen only; on the cover it ends the list).
 */
@Composable
fun PanelPage(
    store: ConceptStore,
    ui: PanesUi,
    page: Int,
    wide: Boolean,
    name: String,
    head: @Composable () -> Unit,
    overview: @Composable () -> Unit,
    detail: @Composable (key: String, inSheet: Boolean) -> Unit,
    list: LazyListScope.() -> Unit,
) {
    val open = ui.open(page)
    val listState = store.list("$name:list")
    BoxWithConstraints(Modifier.fillMaxSize().labAnchor(name)) {
        val screenPx = with(LocalDensity.current) { maxHeight.toPx() }
        PanelGround()
        if (wide) {
            Column(Modifier.fillMaxSize().padding(top = 32.dp)) {
                PageNav(store, ui, page)
                Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = P.gap), horizontalArrangement = Arrangement.spacedBy(P.gap)) {
                    ListPane(Modifier.width(372.dp).fillMaxHeight(), head) {
                        LazyColumn(Modifier.fillMaxSize().labAnchor("$name:rows"), state = listState, contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 16.dp)) { list() }
                    }
                    // The pane makes room for the keyboard, as the phone resizes a window for it.
                    Box(Modifier.weight(1f).fillMaxHeight().padding(bottom = if (ui.keyboard) KeyboardHeight - 72.dp else 0.dp)) {
                        AnimatedContent(
                            open, Modifier.fillMaxSize(),
                            transitionSpec = {
                                (slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally(tween(160)) { it / 10 } + fadeOut(tween(160)))
                            },
                            contentKey = { it },
                        ) { key ->
                            if (key == null) RestPane(Modifier.fillMaxSize()) { overview() }
                            else DetailPane(Modifier.fillMaxSize()) { detail(key, false) }
                        }
                    }
                }
                Dock(store, ui, page, wide)
            }
        } else {
            val sheet = remember { Animatable(0f) }
            val behind = { ((sheet.value - HALF) / (FULL - HALF)).coerceIn(0f, 1f) }
            Column(
                Modifier.fillMaxSize().padding(top = 32.dp).graphicsLayer {
                    // Pulled to full, the list steps back and blurs, so the sheet in front reads as nearer.
                    val t = behind()
                    val s = 1f - 0.035f * t
                    scaleX = s; scaleY = s
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    val r = 6.dp.toPx() * t
                    renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
                    alpha = 1f - 0.25f * t
                },
            ) {
                PageNav(store, ui, page)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = P.gap)) {
                    val pad = if (open != null) maxHeight * HALF else 16.dp
                    ListPane(Modifier.fillMaxSize(), head) {
                        LazyColumn(Modifier.fillMaxSize().labAnchor("$name:rows"), state = listState, contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = pad)) {
                            list()
                            item(key = "overview") { Box(Modifier.padding(top = P.s4)) { overview() } }
                        }
                    }
                }
                Dock(store, ui, page, wide)
            }
            PaneSheet(ui, page, sheet, name, open) { key -> detail(key, true) }
            KeepOpenRowInView(listState, open, screenPx)
        }
    }
}

/** On the cover, opening a row lower than the sheet's top scrolls the list so the row shows above it. */
@Composable
private fun KeepOpenRowInView(list: androidx.compose.foundation.lazy.LazyListState, open: String?, screen: Float) {
    val density = LocalDensity.current
    LaunchedEffect(open) {
        val key = open ?: return@LaunchedEffect
        kotlinx.coroutines.delay(60)
        val r = rowRects[key] ?: return@LaunchedEffect
        val top = screen * (1f - HALF) - with(density) { 24.dp.toPx() }
        if (r.bottom > top) list.animateScrollBy(r.bottom - top, tween(220))
    }
}

/** Where each row is on screen, by key: for the cover's sheet to keep the open row in view, and for menus. */
val rowRects = mutableMapOf<String, androidx.compose.ui.geometry.Rect>()

fun Modifier.rowRect(key: String): Modifier = onGloballyPositioned { rowRects[key] = it.boundsInRoot() }

/** The list pane: a frosted layer with its own header and its own scroll. */
@Composable
fun ListPane(modifier: Modifier, head: @Composable () -> Unit, body: @Composable () -> Unit) {
    Column(modifier.clip(P.rLg).background(P.pane).border(1.dp, P.line, P.rLg)) {
        head()
        Box(Modifier.weight(1f).fillMaxWidth()) { body() }
    }
}

/** The detail pane, unfolded: a step lighter than the list, lifted, with an amber edge for the open item. */
@Composable
fun DetailPane(modifier: Modifier, body: @Composable () -> Unit) {
    Box(
        modifier.shadow(20.dp, P.rLg, clip = false, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(P.rLg).background(P.sheet).border(1.dp, P.line2, P.rLg).labAnchor("pane:detail"),
    ) { body() }
}

/** The detail side with nothing open: recessed, quieter than the list, holding the page's overview. */
@Composable
fun RestPane(modifier: Modifier, body: @Composable () -> Unit) {
    Box(modifier.clip(P.rLg).background(P.paneRest).border(1.dp, P.line, P.rLg).labAnchor("pane:rest")) { body() }
}

/** A list pane's header: its title in condensed caps with a count, a line under it, and its actions. */
@Composable
fun PaneHead(title: String, count: Int?, sub: String, countTone: Color = P.text2, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = P.s4, end = 6.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(title.uppercase(), style = P.paneTitle)
                if (count != null && count > 0) { Spacer(Modifier.width(10.dp)); Count(count, countTone) }
            }
            Spacer(Modifier.height(2.dp))
            BasicText(sub, style = P.meta, maxLines = 1)
        }
        actions()
    }
}

/**
 * The open item's sheet on the cover. It rises to half height without a scrim, so the list above
 * stays live and another row can be opened into it; pulled up (by its handle, its list, or the
 * chevron) it rests at full and the list behind blurs. Back, the ×, a pull below half, or a tap on
 * the stepped-back list closes it. Anchors: `sheet:<name>`, `sheet:<name>:expand`, `sheet:<name>:close`.
 */
@Composable
private fun PaneSheet(ui: PanesUi, page: Int, shown: Animatable<Float, *>, name: String, open: String?, content: @Composable (String) -> Unit) {
    val scope = rememberCoroutineScope()
    // What it showed, kept while it leaves.
    var showing by remember { mutableStateOf(open) }
    if (open != null) showing = open
    val full = ui.full[page] == true
    LaunchedEffect(open, full) {
        if (open != null) shown.animateTo(if (full) FULL else HALF, tween(240)) else shown.animateTo(0f, tween(180))
    }
    if (open == null && shown.value == 0f) return
    val close = { ui.setOpen(page, null); ui.full[page] = false }
    BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = if (ui.keyboard) KeyboardHeight else 0.dp)) {
        val height = with(LocalDensity.current) { maxHeight.toPx() }
        val behind = ((shown.value - HALF) / (FULL - HALF)).coerceIn(0f, 1f)
        // Only once it covers the list is there a scrim; a tap there brings the list forward again.
        if (behind > 0.01f) Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f * behind)).labAnchor("sheet:$name:scrim")
                .clickable(remember { MutableInteractionSource() }, indication = null) { ui.full[page] = false },
        )
        fun settle(v: Float) = scope.launch {
            val at = shown.value
            val target = when {
                v > 1100f && at < HALF + 0.05f -> 0f
                at < HALF * 0.62f -> 0f
                v > 900f -> HALF
                v < -900f -> FULL
                else -> if (kotlin.math.abs(at - HALF) < kotlin.math.abs(at - FULL)) HALF else FULL
            }
            if (target == 0f) close() else { ui.full[page] = target == FULL; shown.animateTo(target, tween(200)) }
        }
        val connection = remember(height) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < 0f && shown.value < FULL && source == NestedScrollSource.UserInput) {
                        scope.launch { shown.snapTo((shown.value - available.y / height).coerceAtMost(FULL)) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0f && source == NestedScrollSource.UserInput) {
                        scope.launch { shown.snapTo((shown.value - available.y / height).coerceAtLeast(0f)) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
                override suspend fun onPreFling(available: Velocity): Velocity {
                    val at = shown.value
                    if (kotlin.math.abs(at - HALF) > 0.002f && kotlin.math.abs(at - FULL) > 0.002f) { settle(available.y); return available }
                    return Velocity.Zero
                }
            }
        }
        val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        val tracker = remember { VelocityTracker() }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .height(maxHeight * maxOf(shown.value, HALF))
                .graphicsLayer { translationY = (HALF - shown.value).coerceAtLeast(0f) * height }
                .shadow(28.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape).background(P.sheet).border(1.dp, P.line2, shape)
                .nestedScroll(connection)
                .labAnchor("sheet:$name").labAnchor(if (full) "sheet:$name:full" else "sheet:$name:half"),
        ) {
            // The handle row: drag it, or use its two buttons, which say what a drag would do.
            Box(
                Modifier.fillMaxWidth().height(44.dp).pointerInput(height) {
                    detectVerticalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = { settle(tracker.calculateVelocity().y) },
                    ) { change, dy ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        scope.launch { shown.snapTo((shown.value - dy / height).coerceIn(0f, FULL)) }
                    }
                }.labAnchor("sheet:$name:handle"),
            ) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size(36.dp, 4.dp).clip(P.rPill).background(P.line3))
                Row(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp, top = 6.dp)) {
                    IconButton(if (full) Glyph.ChevronDown else Glyph.ChevronUp, if (full) "Half height" else "Full height", { ui.full[page] = !full }, anchor = "sheet:$name:expand", size = 40.dp)
                    IconButton(Glyph.Close, "Close", close, anchor = "sheet:$name:close", size = 40.dp)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { showing?.let { content(it) } }
        }
        BackHandler(open != null && ui.focused == null) { close() }
    }
}

/**
 * The switcher at the top of every page: the agent page, home and Today, with what waits on each.
 * The nav is tertiary and quiet; the page you are on is the one raised.
 */
@Composable
fun PageNav(store: ConceptStore, ui: PanesUi, page: Int, onWallpaper: Boolean = false) {
    val a = ui.anchorsOn(page)
    val scope = rememberCoroutineScope()
    val current = ui.shell.pager.targetPage
    val cards = store.cards.size + store.home?.needsYou.orEmpty().size
    val today = store.todayCount()
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(P.rPill).background(if (onWallpaper) P.float.copy(alpha = 0.72f) else P.well).border(1.dp, P.line2, P.rPill).padding(3.dp).labAnchor(a("nav")),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(Triple(PanesUi.AGENT, "Agent", cards), Triple(PanesUi.HOME, "Home", 0), Triple(PanesUi.TODAY, "Today", today)).forEach { (to, label, n) ->
                val on = current == to
                val bg by animateColorAsState(if (on) P.raise2.copy(alpha = 0.16f) else Color.Transparent, tween(180))
                Row(
                    Modifier.clip(P.rPill).background(bg).then(if (on) Modifier.border(1.dp, P.line2, P.rPill) else Modifier)
                        .clickable { ui.later(scope) { shell.goTo(to) } }.heightIn(min = 36.dp).padding(horizontal = 14.dp).labAnchor(a("nav:$label")),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(label, style = P.small.copy(color = if (on) P.text1 else P.text2, fontWeight = FontWeight.Medium))
                    if (n > 0) {
                        Spacer(Modifier.width(6.dp))
                        val tone = if (to == PanesUi.AGENT) P.cyan else P.amber
                        Box(Modifier.clip(P.rPill).background(tone.copy(alpha = 0.16f)).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                            BasicText("$n", style = P.meta.copy(color = tone, fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }
    }
}

/**
 * The dock at the thumb on every page: the one field that finds, adds and asks, and the timer, which
 * shows what runs and stops or switches it from anywhere.
 */
@Composable
fun Dock(store: ConceptStore, ui: PanesUi, page: Int, wide: Boolean, onWallpaper: Boolean = false) {
    val a = ui.anchorsOn(page)
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (wide) 40.dp else 12.dp).padding(top = 10.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bg = if (onWallpaper) P.float.copy(alpha = 0.8f) else P.float
        Row(
            Modifier.weight(1f).height(52.dp).clip(P.rPill).background(bg).border(1.dp, P.line2, P.rPill),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).fillMaxHeight().clickable { ui.focusSearch = true; ui.later(scope) { if (shell.pager.currentPage != PanesUi.HOME) shell.goTo(PanesUi.HOME); shell.openDrawer() } }
                    .padding(start = 18.dp).labAnchor(a("dock:search")),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Glyph.Search, P.text2, size = 20.dp)
                Spacer(Modifier.width(12.dp))
                BasicText("Search, add or ask", style = P.body.copy(color = P.text2), maxLines = 1)
            }
            IconButton(Glyph.Grid, "All apps", { ui.focusSearch = false; ui.later(scope) { if (shell.pager.currentPage != PanesUi.HOME) shell.goTo(PanesUi.HOME); shell.openDrawer() } }, anchor = a("dock:apps"), modifier = Modifier.padding(end = 4.dp))
        }
        Spacer(Modifier.width(8.dp))
        TimerChip(store, ui, bg, a)
    }
}

/** The timer, at the thumb: running, its time, Switch and Stop; idle, Start. The chip's body opens the timer. */
@Composable
fun TimerChip(store: ConceptStore, ui: PanesUi, bg: Color, a: (String) -> String) {
    val running = store.timer
    if (running == null) {
        Row(
            Modifier.height(52.dp).clip(P.rPill).background(bg).border(1.dp, P.line2, P.rPill)
                .clickable(enabled = store.timerOk()) { ui.sheet = Sheet.Timer }
                .graphicsLayer { alpha = if (store.timerOk()) 1f else 0.45f }
                .padding(start = 16.dp, end = 18.dp).labAnchor(a("chip:start")),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Play, P.text1, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            BasicText("Start", style = P.small.copy(color = P.text1, fontWeight = FontWeight.Medium))
        }
        return
    }
    val now = liveNow(store)
    Row(
        Modifier.height(52.dp).clip(P.rPill).background(Color(0xF0281A0E)).border(1.dp, P.amberEdge, P.rPill).padding(start = 4.dp, end = 4.dp).labAnchor(a("chip:timer")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.fillMaxHeight().clip(P.rPill).clickable { ui.sheet = Sheet.Timer }.padding(start = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(P.amber))
            Spacer(Modifier.width(8.dp))
            BasicText(readout(now - running.start), Modifier.labAnchor(a("chip:readout")), style = P.body.copy(color = P.amber, fontWeight = FontWeight.SemiBold))
        }
        Box(Modifier.size(1.dp, 24.dp).background(P.amberEdge.copy(alpha = 0.4f)))
        IconButton(Glyph.Swap, "Switch", { ui.sheet = Sheet.Timer; ui.switching = true }, tint = P.text1, anchor = a("chip:switch"), size = 42.dp, enabled = store.timerOk())
        IconButton(Glyph.Stop, "Stop", { if (ui.saves()) store.stop() }, tint = P.text1, anchor = a("chip:stop"), size = 42.dp, enabled = store.timerOk())
    }
}

/** A row in a list pane: rounded, raised while pressed, amber-washed with an amber edge while it is the open one. */
@Composable
fun Modifier.paneRow(selected: Boolean, key: String, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(when { selected -> P.amberSoft; pressed -> P.raise2; else -> Color.Transparent }, tween(150))
    val line by animateColorAsState(if (selected) P.amberEdge else Color.Transparent, tween(150))
    return this.fillMaxWidth().rowRect(key).clip(P.rMd).background(bg).border(1.dp, line, P.rMd)
        .clickable(source, indication = null, onClick = onClick).labAnchor("row:$key")
}
