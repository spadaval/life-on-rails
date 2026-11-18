package com.spadaval.lor.concepts.panes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.AppIcon
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.FolderIcon
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.LocalWallpaperKind
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock
import kotlin.math.roundToInt

/** Home's words over the wallpaper: dark where it is pale, light with a soft shadow where it is dark. */
private class Ink(val text: Color, val muted: Color, val shadow: Shadow?)

private val darkInk = Ink(P.inkDark, P.inkDark2, null)
private val lightInk = Ink(Color.White, Color.White.copy(alpha = 0.85f), Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f), 6f))

/** The ink at the foot of home (the icons' labels): the mixed wallpaper is dark ground below. */
@Composable
private fun ink(): Ink = if (LocalWallpaperKind.current == Wallpaper.Pale) darkInk else lightInk

/*
 * Home: icons and search, which are 252 of 298 launches, near the thumb. Above them one glance pane
 * (the time, the day, the weather and what is next) that also carries what makes Today untrue (a
 * failed sync, LoR not answering), with its fix. The switcher at the top names the agent page and
 * Today with what waits on each; the dock holds search and the timer. A swipe up opens the drawer,
 * a swipe down the shade, a double tap quick send.
 */
@Composable
fun Home(store: ConceptStore, ui: PanesUi, wide: Boolean) {
    val ink = ink()
    Column(Modifier.fillMaxSize().padding(top = 32.dp).labAnchor("home")) {
        PageNav(store, ui, PanesUi.HOME, onWallpaper = true)
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (wide) 40.dp else 16.dp)) {
            Spacer(Modifier.height(if (wide) 8.dp else 20.dp))
            Glance(store, ui, Modifier.widthIn(max = 560.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.weight(1f))
            if (store.appsLoading) LoadingIcons(if (wide) 6 else 4) else Icons(store, ui, if (wide) 6 else 4, ink)
        }
        Dock(store, ui, PanesUi.HOME, wide, onWallpaper = true)
    }
}

/**
 * The glance: a pane of glass over the wallpaper with the time and the day, the weather, and the
 * next event. What makes the day untrue (a failed sync, LoR not answering, T3 away) is its last
 * line, in red, with the one action that fixes it.
 */
@Composable
private fun Glance(store: ConceptStore, ui: PanesUi, modifier: Modifier = Modifier) {
    val now = liveNow(store)
    val zone = store.zone
    Column(modifier.fillMaxWidth().clip(P.rLg).background(P.float.copy(alpha = 0.78f)).border(1.dp, P.line2, P.rLg).labAnchor("home:glance")) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(LocalClock.time(now, zone), style = P.title, modifier = Modifier.labAnchor("home:clock"))
                BasicText(LocalClock.day(now, zone), style = P.small)
            }
            store.weather?.let { w ->
                Column(horizontalAlignment = Alignment.End) {
                    val (t, words) = w.split(' ', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    BasicText(t, style = P.head.copy(fontWeight = FontWeight.Medium))
                    BasicText(words, style = P.meta)
                }
            }
        }
        val next = store.home?.day?.events?.firstOrNull { it.end > now }
        if (next != null || store.home != null) {
            Hairline()
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Glyph.Calendar, P.cyan, size = 16.dp)
                Spacer(Modifier.width(10.dp))
                BasicText(
                    when {
                        next == null -> "Nothing else on the calendar today"
                        next.start <= now -> "${next.title}, until ${LocalClock.time(next.end, zone)}"
                        else -> "${next.title} at ${LocalClock.time(next.start, zone)}"
                    },
                    Modifier.weight(1f), style = P.small.copy(color = P.text1), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (next != null && next.start > now) BasicText("in ${spoken(next.start - now)}", style = P.meta)
            }
        }
        Trouble(store, ui)
    }
}

/** What makes home's numbers untrue, each with its fix: a failed sync first, then LoR, then T3. */
@Composable
private fun Trouble(store: ConceptStore, ui: PanesUi) {
    val scope = rememberCoroutineScope()
    val troubles = listOfNotNull(
        if (store.syncFailed) Quad("Phone sync failed", "${store.sync.pending} readings waiting", "home:sync") { ui.sheet = Sheet.Settings("sync") } else null,
        when (store.homeLoad) {
            Load.STALE -> Quad("LoR isn't answering", "Today is as of ${store.homeReadAt?.let { LocalClock.time(it, store.zone) } ?: "earlier"}", "home:stale") { ui.later(scope) { shell.goTo(PanesUi.TODAY) } }
            Load.FAILED -> Quad("Can't reach LoR", store.homeError ?: "No answer", "home:lor") { ui.sheet = Sheet.Settings("connections") }
            else -> null
        },
        when (store.t3Status) {
            T3Status.RETRYING -> Quad("T3 is reconnecting", "Cards as of ${ago(store.t3SeenAt, store.now)}", "home:t3") { ui.later(scope) { shell.goTo(PanesUi.AGENT) } }
            T3Status.REJECTED -> Quad("T3 refused this phone", "Pair again to see its cards", "home:t3") { ui.sheet = Sheet.Settings("connections") }
            else -> null
        },
    )
    troubles.forEach { (words, detail, anchor, act) ->
        Hairline()
        Row(
            Modifier.fillMaxWidth().clickable { act() }.heightIn(min = 52.dp).padding(start = 18.dp, end = 10.dp).labAnchor(anchor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(P.red))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(words, style = P.small.copy(color = P.text1, fontWeight = FontWeight.Medium), maxLines = 1)
                BasicText(detail, style = P.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicText(if (anchor == "home:sync") "Fix" else "Open", Modifier.padding(horizontal = 8.dp).labAnchor("$anchor:fix"), style = P.small.copy(color = P.red, fontWeight = FontWeight.SemiBold))
            Icon(Glyph.Chevron, P.red, size = 16.dp)
        }
    }
}

private data class Quad(val words: String, val detail: String, val anchor: String, val act: () -> Unit)

/**
 * The start page's icons. A tap opens the app (the lab says so); a long press opens its menu, and
 * the menu stays while the finger rests; a drag from there lifts the icon, closes the menu and
 * carries it to a new place, the others making room.
 */
@Composable
private fun Icons(store: ConceptStore, ui: PanesUi, columns: Int, ink: Ink) {
    val icons = store.icons
    var held by remember { mutableStateOf<String?>(null) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var target by remember { mutableStateOf(-1) }
    // Where each icon is on screen, for its menu; and whether the press under way became a long one.
    val rects = remember { mutableMapOf<String, Rect>() }
    var longPressed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    // A state that opens an app's menu: once that icon has a place, open it there.
    androidx.compose.runtime.LaunchedEffect(ui.menuFor) {
        val ref = ui.menuFor ?: return@LaunchedEffect
        while (rects[ref] == null) androidx.compose.runtime.withFrameMillis { }
        ui.menu = Menu.App(ref, rects.getValue(ref)); ui.menuFor = null
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cellW = maxWidth / columns
        val cellH = 92.dp
        val rows = (icons.size + columns - 1) / columns
        val cellWpx = with(density) { cellW.toPx() }
        val cellHpx = with(density) { cellH.toPx() }
        Box(Modifier.fillMaxWidth().height(cellH * rows)) {
            val from = icons.indexOfFirst { it.key == held }
            icons.forEachIndexed { i, icon ->
                // Where each icon draws: while one is held, the rest shift to leave its target free.
                val slot = if (held == null || from < 0 || i == from) i else when {
                    target in (from + 1)..i && i > from -> i - 1
                    target in i until from && i < from -> i + 1
                    else -> i
                }
                val x by animateFloatAsState(slot % columns * cellWpx, tween(180))
                val y by animateFloatAsState(slot / columns * cellHpx, tween(180))
                val isHeld = icon.key == held
                val lift by animateFloatAsState(if (isHeld) 1f else 0f, tween(150))
                val label = when (icon) { is ConceptStore.Icon.AppIcon -> icon.app.label; is ConceptStore.Icon.Folder -> icon.name }
                Column(
                    Modifier
                        .offset { if (isHeld) IntOffset((i % columns * cellWpx + drag.x).roundToInt(), (i / columns * cellHpx + drag.y).roundToInt()) else IntOffset(x.roundToInt(), y.roundToInt()) }
                        .size(cellW, cellH)
                        .graphicsLayer { val s = 1f + 0.1f * lift; scaleX = s; scaleY = s; shadowElevation = 16f * lift }
                        .onGloballyPositioned { c ->
                            if (!isHeld) rects[icon.key] = c.boundsInRoot().iconRect()
                        }
                        .pointerInput(icon.key, icons) {
                            detectTapGestures(onPress = { longPressed = false }, onTap = {
                                if (longPressed) return@detectTapGestures
                                when (icon) {
                                    is ConceptStore.Icon.AppIcon -> store.launch(icon.app.label)
                                    is ConceptStore.Icon.Folder -> ui.sheet = Sheet.Folder(icon.name)
                                }
                            })
                        }
                        .pointerInput(icon.key, icons) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    longPressed = true
                                    held = icon.key; drag = Offset.Zero; target = i
                                    ui.menu = Menu.App(icon.key, rects[icon.key] ?: Rect.Zero)
                                },
                                onDragEnd = {
                                    if (held != null && target != i && ui.menu == null) store.move(icon.key, target)
                                    held = null; drag = Offset.Zero
                                },
                                onDragCancel = { held = null; drag = Offset.Zero },
                            ) { change, d ->
                                change.consume()
                                drag += d
                                // The menu stays through a small drift; past it the icon is lifted and the menu goes.
                                if (drag.getDistance() > 24.dp.toPx()) ui.menu = null
                                if (ui.menu == null) {
                                    val cx = i % columns * cellWpx + drag.x + cellWpx / 2
                                    val cy = i / columns * cellHpx + drag.y + cellHpx / 2
                                    target = ((cy / cellHpx).toInt().coerceIn(0, rows - 1) * columns + (cx / cellWpx).toInt().coerceIn(0, columns - 1)).coerceIn(0, icons.size - 1)
                                }
                            }
                        }
                        .labAnchor("home:$label"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(6.dp))
                    when (icon) {
                        is ConceptStore.Icon.AppIcon -> AppIcon(icon.app, 56.dp)
                        is ConceptStore.Icon.Folder -> FolderIcon(icon.apps, Color.White.copy(alpha = 0.22f), 56.dp)
                    }
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        label, Modifier.padding(horizontal = 4.dp),
                        style = P.meta.copy(color = ink.text, textAlign = TextAlign.Center, shadow = ink.shadow, fontWeight = FontWeight.Medium),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The icon's square inside its cell, which the menu sits beside. */
private fun Rect.iconRect() = Rect(center.x - height * 0.33f, top, center.x + height * 0.33f, top + height * 0.74f)

/** The phone's status bar: the clock and the radios, as the phone draws them. */
@Composable
fun StatusBar(store: ConceptStore, ink: Color) {
    val now = liveNow(store)
    Row(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(LocalClock.time(now, store.zone), style = TextStyle(color = ink, fontSize = P.meta.fontSize, fontFamily = P.meta.fontFamily, fontWeight = FontWeight.Medium))
        Spacer(Modifier.weight(1f))
        Icon(Glyph.Wifi, ink, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        Icon(Glyph.Battery, ink, size = 18.dp)
    }
}

/**
 * What a swipe down on home opens: Android's notification shade. The lab can't open the phone's,
 * so a stand-in says what would be there, and a tap or Back closes it.
 */
@Composable
fun Shade(shell: com.spadaval.lor.concepts.kit.ShellState) {
    val shown by animateFloatAsState(if (shell.shade) 1f else 0f, tween(220))
    if (shown == 0f) return
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = shown }.background(Color.Black.copy(alpha = 0.55f)).clickable { shell.shade = false }.labAnchor("shade")) {
        Column(
            Modifier.fillMaxWidth().graphicsLayer { translationY = (shown - 1f) * 300f }.background(Color(0xF2202227)).padding(top = 44.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Wi-Fi", "Bluetooth", "Flashlight", "Do not disturb").forEach {
                    Box(Modifier.weight(1f).height(56.dp).clip(P.rLg).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        BasicText(it, style = P.meta.copy(color = P.text1))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            BasicText("Android's notification shade", style = P.body.copy(fontWeight = FontWeight.Medium))
            BasicText("A swipe down on home opens it, as the brief's first decision says. The drawer is a swipe up.", style = P.meta)
        }
    }
    com.spadaval.lor.home.BackHandler(shell.shade) { shell.shade = false }
}

/** While the app list is read: the start page's shape in faint tiles, so home doesn't jump when the icons arrive. */
@Composable
private fun LoadingIcons(columns: Int) {
    Column(Modifier.fillMaxWidth().labAnchor("home:loading"), verticalArrangement = Arrangement.spacedBy(36.dp)) {
        repeat(3) {
            Row(Modifier.fillMaxWidth()) {
                repeat(columns) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = 0.08f)))
                    }
                }
            }
        }
        Spacer(Modifier.height(0.dp))
    }
}
