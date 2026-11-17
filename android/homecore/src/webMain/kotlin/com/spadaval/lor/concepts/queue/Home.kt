package com.spadaval.lor.concepts.queue

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.spadaval.lor.lab.LocalWallpaperKind
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock
import kotlin.math.roundToInt

/** Home's words over the wallpaper: dark on the pale one, light with a soft shadow on the rest. */
private class Ink(val text: Color, val muted: Color, val shadow: Shadow?)

@Composable
private fun ink(): Ink = if (LocalWallpaperKind.current == Wallpaper.Pale) Ink(Q.inkDark, Q.inkDark2, null)
else Ink(Color.White, Color.White.copy(alpha = 0.82f), Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f), 6f))

/*
 * Home (the brief: "home stays icons and search, with one line at the top"). The clock, the date,
 * the weather and the next event; the one line (the running timer with Stop, and how many things
 * wait, which opens the queue); the start page's icons, near the thumb; and search at the foot.
 * A swipe up opens the drawer, a swipe down is the shade's, a double tap opens quick send.
 */
@Composable
fun Home(store: ConceptStore, ui: QueueUi, wide: Boolean) {
    val ink = ink()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(top = 32.dp).padding(horizontal = if (wide) 40.dp else Q.gutter)) {
        Spacer(Modifier.height(if (wide) 28.dp else 44.dp))
        Glance(store, ink)
        Spacer(Modifier.height(Q.s5))
        OneLine(store, ui, Modifier.widthIn(max = 560.dp))
        Spacer(Modifier.weight(1f))
        Icons(store, ui, if (wide) 6 else 4, ink)
        Spacer(Modifier.height(Q.s4))
        SearchPill(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 560.dp), onSearch = {
            ui.focusSearch = true
            ui.later(scope) { shell.openDrawer() }
        }, onAll = {
            ui.focusSearch = false
            ui.later(scope) { shell.openDrawer() }
        })
        Spacer(Modifier.height(28.dp))
    }
}

/** The time, the day, the weather and the next event: the glance. */
@Composable
private fun Glance(store: ConceptStore, ink: Ink) {
    val now = liveNow(store)
    val zone = store.zone
    BasicText(LocalClock.time(now, zone), style = Q.clock.copy(color = ink.text, shadow = ink.shadow), modifier = Modifier.labAnchor("home:clock"))
    val day = listOfNotNull(LocalClock.day(now, zone), store.weather).joinToString("  ·  ")
    BasicText(day, style = Q.body.copy(color = ink.text, fontWeight = FontWeight.Medium, shadow = ink.shadow))
    val next = store.home?.day?.events?.firstOrNull { it.end > now }
    if (next != null) {
        val starts = if (next.start <= now) "now, until ${LocalClock.time(next.end, zone)}" else "at ${LocalClock.time(next.start, zone)} · in ${spoken(next.start - now)}"
        Spacer(Modifier.height(2.dp))
        BasicText("${next.title} $starts", style = Q.label.copy(color = ink.muted, shadow = ink.shadow), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The one line: the running timer with Stop (or Start), and what waits, which opens the queue. A
 * failed sync takes the second half of the line, because it makes what LoR shows untrue.
 */
@Composable
fun OneLine(store: ConceptStore, ui: QueueUi, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val now = liveNow(store)
    val running = store.timer
    val waiting = store.waiting()
    val shape = Q.rLg
    Row(
        modifier.fillMaxWidth().clip(shape).background(Q.float).border(1.dp, Q.line2, shape)
            .heightIn(min = 64.dp).padding(start = 6.dp, end = 8.dp).labAnchor("home:line"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clip(Q.rMd).clickable { ui.sheet = Sheet.Timer }.padding(horizontal = 10.dp, vertical = 10.dp).labAnchor("home:timer"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (running != null) Q.amber else Q.text3))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val head = if (running != null) "${running.label ?: "No label"}  ·  ${spoken(now - running.start)}" else "No timer running"
                BasicText(head, style = Q.body.copy(fontWeight = FontWeight.Medium, color = if (running != null) Q.text1 else Q.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                when {
                    store.syncFailed -> BasicText("Phone sync failed · Fix", Modifier.clickable { ui.sheet = Sheet.Settings("sync") }.labAnchor("home:sync"), style = Q.label.copy(color = Q.red))
                    !store.lorLive && store.homeLoad != com.spadaval.lor.lab.Load.LOADING -> BasicText("LoR isn't answering · as of ${store.homeReadAt?.let { LocalClock.time(it, store.zone) } ?: "never"}", style = Q.label.copy(color = Q.red))
                    else -> BasicText(
                        if (waiting == 0) "Nothing waiting" else "$waiting waiting in the queue ›",
                        Modifier.clickable { ui.later(scope) { shell.goTo(QueueUi.QUEUE) } }.labAnchor("home:waiting"),
                        style = Q.label.copy(color = if (waiting > 0 && store.cards.isNotEmpty()) Q.cyan else Q.text2),
                    )
                }
            }
        }
        if (running != null) QButton("Stop", { store.stop() }, kind = Kind.PLAIN, glyph = Glyph.Stop, height = 40.dp, anchor = "home:stop", enabled = store.timerReachable)
        else QButton("Start", { ui.sheet = Sheet.Timer }, kind = Kind.QUIET, glyph = Glyph.Play, height = 40.dp, anchor = "home:start", enabled = store.timerReachable)
    }
}

/**
 * The start page's icons. A tap opens the app (the lab says so); a long press opens its menu, and
 * the menu stays while the finger rests; a drag from there lifts the icon, closes the menu and
 * carries it to a new place, the others making room.
 */
@Composable
private fun Icons(store: ConceptStore, ui: QueueUi, columns: Int, ink: Ink) {
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
                        style = Q.label.copy(color = ink.text, textAlign = TextAlign.Center, shadow = ink.shadow, fontWeight = FontWeight.Medium),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The icon's square inside its cell, which the menu sits beside. */
private fun Rect.iconRect() = Rect(center.x - height * 0.33f, top, center.x + height * 0.33f, top + height * 0.74f)

/** Search at the thumb: one field that finds, adds and asks; the grid button opens every app. */
@Composable
private fun SearchPill(modifier: Modifier, onSearch: () -> Unit, onAll: () -> Unit) {
    val shape = Q.rPill
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(shape).background(Q.float).border(1.dp, Q.line2, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).fillMaxSize().clickable { onSearch() }.padding(start = 18.dp).labAnchor("home:search"), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Search, Q.text2, size = 20.dp)
            Spacer(Modifier.width(12.dp))
            BasicText("Search apps, people, or ask", style = Q.body.copy(color = Q.text2))
        }
        IconButton(Glyph.Grid, "All apps", onAll, anchor = "home:all-apps", modifier = Modifier.padding(end = 4.dp))
    }
}

/** The phone's status bar: the clock and the radios, as the phone draws them. */
@Composable
fun StatusBar(store: ConceptStore, ink: Color) {
    val now = liveNow(store)
    Row(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(LocalClock.time(now, store.zone), style = TextStyle(color = ink, fontSize = Q.label.fontSize, fontFamily = Q.label.fontFamily, fontWeight = FontWeight.Medium))
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
                    Box(Modifier.weight(1f).height(56.dp).clip(Q.rLg).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        BasicText(it, style = Q.label.copy(color = Q.text1))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            BasicText("Android's notification shade", style = Q.body.copy(fontWeight = FontWeight.Medium))
            BasicText("A swipe down on home opens it, as the brief's first decision says. The drawer is a swipe up.", style = Q.label)
        }
    }
    com.spadaval.lor.home.BackHandler(shell.shade) { shell.shade = false }
}
