package com.spadaval.lor.concepts.timeline

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
import androidx.compose.foundation.layout.fillMaxHeight
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

/** Home's words over the wallpaper: dark where it is pale, light with a soft shadow where it is dark. */
private class Ink(val text: Color, val muted: Color, val shadow: Shadow?)

private val darkInk = Ink(T.inkDark, T.inkDark2, null)
private val lightInk = Ink(Color.White, Color.White.copy(alpha = 0.85f), Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f), 6f))

/** The ink at the top of home (the glance): the mixed wallpaper is pale sky above dark ground. */
@Composable
private fun topInk(): Ink = if (LocalWallpaperKind.current != Wallpaper.Dark) darkInk else lightInk

/** The ink at the foot of home (the icons' labels). */
@Composable
private fun ink(): Ink = if (LocalWallpaperKind.current == Wallpaper.Pale) darkInk else lightInk

/*
 * Home (the brief: "home's header is a strip of the day with now, the next event, the time left
 * and the running timer"). The time and the date over the wallpaper; the strip, a glass card
 * holding the waking day as a bar with now on it, what is next, and the timer with Start or Stop;
 * the start page's icons near the thumb; and search at the foot. A tap on the strip opens the day
 * page. A swipe up opens the drawer, a swipe down is the shade's, a double tap opens quick send.
 */
@Composable
fun Home(store: ConceptStore, ui: TimelineUi, wide: Boolean) {
    val ink = ink()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(top = 32.dp).padding(horizontal = if (wide) 40.dp else T.gutter)) {
        Spacer(Modifier.height(if (wide) 20.dp else 36.dp))
        Glance(store, topInk())
        Spacer(Modifier.height(T.s4))
        DayStrip(store, ui, Modifier.widthIn(max = 600.dp))
        Spacer(Modifier.weight(1f))
        if (store.appsLoading) LoadingIcons(if (wide) 6 else 4) else Icons(store, ui, if (wide) 6 else 4, ink)
        Spacer(Modifier.height(T.s4))
        SearchPill(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 600.dp), onSearch = {
            ui.focusSearch = true
            ui.later(scope) { shell.openDrawer() }
        }, onAll = {
            ui.focusSearch = false
            ui.later(scope) { shell.openDrawer() }
        })
        Spacer(Modifier.height(28.dp))
    }
}

/** The time, the date and the weather, over the wallpaper. */
@Composable
private fun Glance(store: ConceptStore, ink: Ink) {
    val now = liveNow(store)
    val zone = store.zone
    Row(verticalAlignment = Alignment.Bottom) {
        BasicText(LocalClock.time(now, zone), style = T.clock.copy(color = ink.text, shadow = ink.shadow), modifier = Modifier.labAnchor("home:clock"))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.padding(bottom = 6.dp)) {
            BasicText(LocalClock.day(now, zone), style = T.body.copy(color = ink.text, fontWeight = FontWeight.Medium, shadow = ink.shadow), maxLines = 1)
            store.weather?.let { BasicText(it, style = T.label.copy(color = ink.muted, shadow = ink.shadow), maxLines = 1) }
        }
    }
}

/**
 * The strip of the day: the waking day as a bar (tracked time amber, unlabelled time dim, what is
 * planned dashed cyan, the running timer bright) with now on it; what is next and the time left;
 * then the timer, with Start or Stop. What waits, or what makes the day untrue (a failed sync, LoR
 * not answering), is a chip at its top right. A tap anywhere else opens the day page.
 */
@Composable
fun DayStrip(store: ConceptStore, ui: TimelineUi, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val now = liveNow(store)
    val day = store.home?.day
    val shape = T.rFloat
    val toDay = { ui.later(scope) { shell.goTo(TimelineUi.DAY) } }
    Column(
        modifier.fillMaxWidth().clip(shape).background(T.float).border(1.dp, T.line2, shape)
            .clickable { toDay() }.padding(top = 14.dp, bottom = 6.dp).labAnchor("home:strip"),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            val next = day?.events?.firstOrNull { it.end > now }
            Column(Modifier.weight(1f)) {
                BasicText(
                    when {
                        next == null -> if (store.home == null) "The day shows once LoR answers" else "Nothing else on the calendar"
                        next.start <= now -> "${next.title} until ${LocalClock.time(next.end, store.zone)}"
                        else -> "${next.title} at ${LocalClock.time(next.start, store.zone)}"
                    },
                    style = T.body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val left = day?.wakeEnd?.let { it - now }?.takeIf { it.isPositive() }
                BasicText(
                    listOfNotNull(next?.takeIf { it.start > now }?.let { "in ${spoken(it.start - now)}" }, left?.let { leftWords(it) })
                        .joinToString(" \u00B7 "),
                    style = T.label, maxLines = 1,
                )
            }
            StatusChip(store, ui, toDay)
        }
        Spacer(Modifier.height(12.dp))
        DayBar(store, ui, now, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(T.line))
        TimerRow(store, ui, now)
    }
}

/** The one chip on the strip: what makes the day untrue first, then how many things wait. */
@Composable
private fun StatusChip(store: ConceptStore, ui: TimelineUi, toDay: () -> Unit) {
    val waiting = store.waitingItems().size
    val load = store.homeLoad
    val (words, tone, anchor, act) = when {
        store.syncFailed -> Quad("Sync failed \u203A", T.red, "home:sync") { ui.sheet = Sheet.Settings("sync") }
        store.fixture.settings.serverUrl.isEmpty() -> Quad("Set up \u203A", T.amber, "home:setup") { ui.sheet = Sheet.Settings("connections") }
        load == com.spadaval.lor.lab.Load.STALE -> Quad("LoR as of ${store.homeReadAt?.let { LocalClock.time(it, store.zone) }}", T.red, "home:stale", toDay)
        load == com.spadaval.lor.lab.Load.FAILED -> Quad("No LoR \u203A", T.red, "home:lor") { ui.sheet = Sheet.Settings("connections") }
        waiting > 0 -> Quad("$waiting waiting \u203A", if (store.cards.isNotEmpty()) T.cyan else T.amber, "home:waiting", toDay)
        else -> return
    }
    Row(
        Modifier.clip(T.rPill).background(tone.copy(alpha = 0.14f)).border(1.dp, tone.copy(alpha = 0.35f), T.rPill)
            .clickable { act() }.heightIn(min = 32.dp).padding(horizontal = 12.dp).labAnchor(anchor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tone))
        Spacer(Modifier.width(8.dp))
        BasicText(words, style = T.label.copy(color = T.text1, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** The waking day as a bar: recorded time, what is running, what is planned, and now. */
@Composable
private fun DayBar(store: ConceptStore, ui: TimelineUi, now: kotlin.time.Instant, modifier: Modifier = Modifier) {
    val day = store.home?.day
    val from = day?.wakeStart ?: return
    val to = day.wakeEnd
    val span = (to - from).inWholeSeconds.toFloat().coerceAtLeast(1f)
    fun x(t: kotlin.time.Instant, w: Float) = ((t - from).inWholeSeconds / span).coerceIn(0f, 1f) * w
    val blocks = ui.blocks
    val events = day.events
    val plans = ui.plans.toList()
    val timer = store.timer
    val seen = store.seenUntil()
    Column(modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(22.dp).labAnchor("home:bar")) {
            val w = size.width
            val h = size.height
            val barTop = 5.dp.toPx(); val barH = h - 10.dp.toPx()
            val r = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            drawRoundRect(T.wash2, androidx.compose.ui.geometry.Offset(0f, barTop), androidx.compose.ui.geometry.Size(w, barH), r)
            blocks.forEach { b ->
                val c = when (b.tone) { DayBlock.Tone.TRACKED -> T.amber; DayBlock.Tone.PROPOSED -> T.amber.copy(alpha = 0.45f); DayBlock.Tone.BARE -> T.unl }
                val a = x(b.start, w); val z = x(b.end, w)
                if (z > a) drawRect(c, androidx.compose.ui.geometry.Offset(a, barTop), androidx.compose.ui.geometry.Size((z - a - 1f).coerceAtLeast(1f), barH))
            }
            if (timer != null) { val a = x(timer.start, w); val z = x(now, w); drawRect(T.amberHi, androidx.compose.ui.geometry.Offset(a, barTop - 2f), androidx.compose.ui.geometry.Size((z - a).coerceAtLeast(2f), barH + 4f)) }
            if (seen < now) { val a = x(seen, w); val z = x(now, w); drawRect(T.red.copy(alpha = 0.25f), androidx.compose.ui.geometry.Offset(a, barTop), androidx.compose.ui.geometry.Size(z - a, barH)) }
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
            (events.map { it.start to it.end } + plans.map { it.start to it.end }).forEach { (s, e) ->
                val a = x(maxOf(s, from), w); val z = x(minOf(e, to), w)
                if (z > a) {
                    drawRect(T.cyanSoft, androidx.compose.ui.geometry.Offset(a, barTop), androidx.compose.ui.geometry.Size(z - a - 1f, barH))
                    drawRect(T.cyan, androidx.compose.ui.geometry.Offset(a + 0.5f, barTop + 0.5f), androidx.compose.ui.geometry.Size(z - a - 2f, barH - 1f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx(), pathEffect = dash))
                }
            }
            val n = x(now, w)
            drawLine(T.amber, androidx.compose.ui.geometry.Offset(n, 0f), androidx.compose.ui.geometry.Offset(n, h), 2.dp.toPx())
            drawCircle(T.amber, 3.5.dp.toPx(), androidx.compose.ui.geometry.Offset(n, 0f + 2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
            BasicText(LocalClock.time(from, store.zone), style = T.time)
            Spacer(Modifier.weight(1f))
            BasicText(LocalClock.time(to, store.zone), style = T.time)
        }
    }
}

/** The timer on the strip: running with its time, Switch and Stop; idle with Start. The row opens its sheet. */
@Composable
private fun TimerRow(store: ConceptStore, ui: TimelineUi, now: kotlin.time.Instant) {
    val running = store.timer
    Row(
        Modifier.fillMaxWidth().clickable { ui.sheet = Sheet.Timer }.heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp).labAnchor("home:timer"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (running != null) T.amber else T.text3.copy(alpha = 0.6f)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                running?.let { it.label ?: "No label yet" } ?: if (store.timerReachable) "Nothing running" else "Timer unavailable",
                style = T.body.copy(fontWeight = FontWeight.Medium, color = if (running != null) T.text1 else T.text2), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (running == null && !store.timerReachable) BasicText("LoR isn't answering", style = T.label)
        }
        if (running != null) {
            BasicText(readout(now - running.start), Modifier.labAnchor("home:readout").labAnchor("home:at:${(now - running.start).inWholeSeconds}"), style = T.timeLg)
            Spacer(Modifier.width(4.dp))
            IconButton(Glyph.Stop, "Stop", { ui.stopTimer() }, anchor = "home:stop", tint = T.text1)
        } else TButton("Start", { ui.sheet = Sheet.Timer }, kind = Kind.QUIET, glyph = Glyph.Play, height = 40.dp, anchor = "home:start", enabled = store.timerReachable)
    }
}

private data class Quad(val words: String, val tone: Color, val anchor: String, val act: () -> Unit)

/**
 * The start page's icons. A tap opens the app (the lab says so); a long press opens its menu, and
 * the menu stays while the finger rests; a drag from there lifts the icon, closes the menu and
 * carries it to a new place, the others making room.
 */
@Composable
private fun Icons(store: ConceptStore, ui: TimelineUi, columns: Int, ink: Ink) {
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
                        style = T.label.copy(color = ink.text, textAlign = TextAlign.Center, shadow = ink.shadow, fontWeight = FontWeight.Medium),
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
    val shape = T.rPill
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(shape).background(T.float).border(1.dp, T.line2, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).fillMaxSize().clickable { onSearch() }.padding(start = 18.dp).labAnchor("home:search"), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Search, T.text2, size = 20.dp)
            Spacer(Modifier.width(12.dp))
            BasicText("Search apps, people, or ask", style = T.body.copy(color = T.text2))
        }
        IconButton(Glyph.Grid, "All apps", onAll, anchor = "home:all-apps", modifier = Modifier.padding(end = 4.dp))
    }
}

/** The phone's status bar: the clock and the radios, as the phone draws them. */
@Composable
fun StatusBar(store: ConceptStore, ink: Color) {
    val now = liveNow(store)
    Row(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(LocalClock.time(now, store.zone), style = TextStyle(color = ink, fontSize = T.label.fontSize, fontFamily = T.label.fontFamily, fontWeight = FontWeight.Medium))
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
                    Box(Modifier.weight(1f).height(56.dp).clip(T.rLg).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        BasicText(it, style = T.label.copy(color = T.text1))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            BasicText("Android's notification shade", style = T.body.copy(fontWeight = FontWeight.Medium))
            BasicText("A swipe down on home opens it, as the brief's first decision says. The drawer is a swipe up.", style = T.label)
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
