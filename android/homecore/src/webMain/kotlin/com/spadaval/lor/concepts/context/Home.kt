package com.spadaval.lor.concepts.context

import androidx.compose.foundation.*
import androidx.compose.ui.draw.*
import com.spadaval.lor.concepts.kit.MenuItem
import com.spadaval.lor.agent.T3Card
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

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
class Ink(val text: Color, val muted: Color, val shadow: Shadow?)

val darkInk = Ink(C.inkDark, C.inkDark2, null)
val lightInk = Ink(Color.White, Color.White.copy(alpha = 0.85f), Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1f), 6f))

/** The ink at the foot of home (the icons' labels): the mixed wallpaper is dark ground below. */
@Composable
private fun ink(): Ink = if (LocalWallpaperKind.current == Wallpaper.Pale) darkInk else lightInk

/*
 * Home: icons and search, which are 252 of 298 launches, with search at the thumb. Above the icons,
 * the time and one card for the current context, three lines at most: what it is with its timer,
 * its next task with a check, and the next fixed event. What makes the card untrue (a failed sync,
 * LoR or T3 away) is a fourth line, in red, with its fix. A tap on the card opens Now; the chip at
 * the top names Now with what waits there. A swipe up opens the drawer, a swipe down the shade, a
 * double tap (or the agent button in the search bar) quick send.
 */
@Composable
fun Home(store: ConceptStore, ui: CtxUi, wide: Boolean) {
    val ink = ink()
    val topInk = if (LocalWallpaperKind.current == Wallpaper.Dark) lightInk else darkInk
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(top = 32.dp).labAnchor("home")) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = if (wide) 40.dp else 16.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NowChip(store, ui, topInk) { ui.later(scope) { shell.goTo(CtxUi.NOW) } }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(if (wide) 12.dp else 18.dp))
            Clock(store, topInk)
            Spacer(Modifier.height(14.dp))
            ContextCard(store, ui, Modifier.widthIn(max = 560.dp))
            Spacer(Modifier.weight(1f))
            if (store.appsLoading) LoadingIcons(if (wide) 6 else 4) else Icons(store, ui, if (wide) 6 else 4, ink)
        }
        Dock(store, ui, CtxUi.HOME, wide, onWallpaper = true)
    }
}

/** The way to Now, visible: its name and how much waits there, in the agent's colour. */
@Composable
private fun NowChip(store: ConceptStore, ui: CtxUi, ink: Ink, onClick: () -> Unit) {
    val n = store.waitingCount()
    Row(
        Modifier.clip(C.rPill).background(C.float).border(1.dp, C.line2, C.rPill).clickable { onClick() }
            .heightIn(min = 40.dp).padding(start = 10.dp, end = 14.dp).labAnchor("home:now"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Glyph.Back, C.text2, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        BasicText("Now", style = C.meta.copy(color = C.text1, fontWeight = FontWeight.Medium))
        if (n > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(C.rPill).background(C.cyan.copy(alpha = 0.2f)).padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
                BasicText("$n waiting", style = C.meta.copy(color = C.cyan, fontWeight = FontWeight.SemiBold), modifier = Modifier.labAnchor("home:waiting:$n"))
            }
        }
    }
}

/** The time and the day, with the weather: what a glance at the phone is most often for. */
@Composable
private fun Clock(store: ConceptStore, ink: Ink) {
    val now = liveNow(store)
    Row(verticalAlignment = Alignment.Bottom) {
        BasicText(LocalClock.time(now, store.zone), Modifier.labAnchor("home:clock"), style = C.ctx.copy(color = ink.text, shadow = ink.shadow, fontSize = C.ctx.fontSize * 1.6f, lineHeight = C.ctx.lineHeight * 1.5f, fontWeight = FontWeight.Medium))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.padding(bottom = 8.dp)) {
            BasicText(LocalClock.day(now, store.zone), style = C.meta.copy(color = ink.text, shadow = ink.shadow, fontWeight = FontWeight.Medium))
            store.weather?.let { BasicText(it, style = C.meta.copy(color = ink.muted, shadow = ink.shadow)) }
        }
    }
}

/**
 * The current context's card: one surface of frosted glass, three lines. Its title and timer (the
 * readout and Stop while it runs, Start when not); its next task, with a check; the next event and
 * how long until it. Trouble, when there is some, is a fourth line in red with its fix.
 */
@Composable
private fun ContextCard(store: ConceptStore, ui: CtxUi, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val (ctx, why) = ui.shown()
    val m = ui.model
    val now = liveNow(store)
    val running = store.timer
    val here = running != null && m.ctxOfLabel(running.label).key == ctx.key
    val toNow = { ui.later(scope) { shell.goTo(CtxUi.NOW) } }
    Column(
        // Glass with no drop shadow: under a translucent face a shadow shows through as a dark slab.
        modifier.fillMaxWidth().clip(C.rFloat).background(C.float).border(1.dp, if (here) C.amberEdge else C.line2, C.rFloat).labAnchor("home:card"),
    ) {
        // Line 1: the context and its timer.
        Row(Modifier.fillMaxWidth().clickable { toNow() }.heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(if (here) "NOW · TIMING" else if (why == Why.ONE) "NOW · THE ONE THING" else "NOW", style = C.label.copy(color = if (here) C.amber else C.text3, fontSize = C.meta.fontSize * 0.85f))
                BasicText(ctx.title, Modifier.labAnchor("home:card:title"), style = C.title.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (running != null && here) {
                BasicText(readout(now - running.start), Modifier.labAnchor("home:card:readout"), style = C.num.copy(fontSize = C.head.fontSize))
                IconButton(Glyph.Stop, "Stop", { if (ui.saves()) store.stop() }, tint = C.amber, anchor = "home:card:stop", enabled = store.timerOk())
            } else {
                CButton(if (running != null) "Switch" else "Start", {
                    if (running != null) { ui.switching = true }
                    ui.sheet = Sheet.Timer
                }, glyph = if (running != null) Glyph.Swap else Glyph.Play, compact = true, kind = Tone.QUIET, anchor = "home:card:start", enabled = store.timerOk())
            }
        }
        // Line 2: the next task.
        val lead = if (ctx.kind == Kind.UNNAMED || ctx.kind == Kind.LABEL) null else m.mineOf(ctx).firstOrNull()
        Hairline()
        if (lead != null) {
            val done = store.done[lead.id] == true
            Row(Modifier.fillMaxWidth().clickable { ui.expanded = lead.id; toNow() }.heightIn(min = 48.dp).padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Check(done, { if (ui.saves()) { if (done) store.untick(lead.id) else store.tick(lead.id) } }, anchor = "home:card:tick", enabled = store.lorLive)
                TaskTitle(lead.title, done, Modifier.weight(1f).labAnchor("home:card:next"), maxLines = 1)
                lead.due?.let { BasicText(dueWords(lead, store), style = C.meta.copy(color = if (lead.overdue) C.red else C.text3)) }
            }
        } else {
            Row(Modifier.fillMaxWidth().clickable { toNow() }.heightIn(min = 48.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    when (ctx.kind) { Kind.UNNAMED -> "This timer has no name yet: name it on Now"; Kind.LABEL -> "Counts toward no context yet"; else -> "Nothing open here" },
                    Modifier.weight(1f).labAnchor("home:card:next"), style = C.body.copy(color = C.text2), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Icon(Glyph.Chevron, C.text3, size = 14.dp)
            }
        }
        // Line 3: the next fixed event.
        Hairline()
        val next = m.next(now)
        Row(Modifier.fillMaxWidth().clickable { next?.let { (e, _) -> ui.switchTo(Ctx(Kind.SLOT, e.title, e)) }; toNow() }.heightIn(min = 44.dp).padding(horizontal = 16.dp).labAnchor("home:card:event"), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Calendar, C.cyan, size = 15.dp)
            Spacer(Modifier.width(12.dp))
            BasicText(
                when { store.home == null -> "Calendar shows once LoR answers"; next == null -> "Nothing else fixed today"; else -> "${next.first.title} at ${LocalClock.time(next.first.start, store.zone)}" },
                Modifier.weight(1f), style = C.meta.copy(color = C.text1), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            next?.let { BasicText("in ${spoken(it.second)}", style = C.meta.copy(color = C.cyan)) }
        }
        Trouble(store, ui)
    }
}

/** What makes the card untrue, each with its fix: a failed sync first, then LoR, then T3. */
@Composable
private fun Trouble(store: ConceptStore, ui: CtxUi) {
    val scope = rememberCoroutineScope()
    val troubles = listOfNotNull(
        if (store.syncFailed) Quad("Phone sync failed", "${store.sync.pending} readings waiting", "home:sync") { ui.sheet = Sheet.Settings("sync") } else null,
        when (store.homeLoad) {
            Load.STALE -> Quad("LoR isn't answering", "As of ${store.homeReadAt?.let { LocalClock.time(it, store.zone) } ?: "earlier"}", "home:stale") { ui.later(scope) { shell.goTo(CtxUi.NOW) } }
            Load.FAILED -> Quad("Can't reach LoR", store.homeError ?: "No answer", "home:lor") { ui.sheet = Sheet.Settings("connections") }
            else -> null
        },
        when (store.t3Status) {
            T3Status.RETRYING -> Quad("T3 is reconnecting", "Cards as of ${ago(store.t3SeenAt, store.now)}", "home:t3") { ui.later(scope) { shell.goTo(CtxUi.NOW) } }
            T3Status.REJECTED -> Quad("T3 refused this phone", "Pair again to see its cards", "home:t3") { ui.sheet = Sheet.Settings("connections") }
            else -> null
        },
    )
    troubles.forEach { (words, detail, anchor, act) ->
        Hairline()
        Row(
            Modifier.fillMaxWidth().clickable { act() }.background(C.redSoft).heightIn(min = 48.dp).padding(start = 16.dp, end = 10.dp).labAnchor(anchor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Warning, C.red, size = 15.dp)
            Spacer(Modifier.width(12.dp))
            BasicText(words, style = C.meta.copy(color = C.text1, fontWeight = FontWeight.Medium), maxLines = 1)
            BasicText("  ·  $detail", Modifier.weight(1f), style = C.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(if (anchor == "home:sync") "Fix" else "Open", Modifier.padding(horizontal = 6.dp).labAnchor("$anchor:fix"), style = C.meta.copy(color = C.red, fontWeight = FontWeight.SemiBold))
        }
    }
}

/**
 * Search at the thumb, on both pages: the one field that finds, adds and asks; all apps; and the
 * agent, so quick send is never only a double tap.
 */
@Composable
fun Dock(store: ConceptStore, ui: CtxUi, page: Int, wide: Boolean, onWallpaper: Boolean = false) {
    val a = ui.anchorsOn(page)
    val scope = rememberCoroutineScope()
    val open = { search: Boolean -> ui.focusSearch = search; ui.later(scope) { if (shell.pager.currentPage != CtxUi.HOME) shell.goTo(CtxUi.HOME); shell.openDrawer() } }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (wide) 40.dp else 12.dp).padding(top = 8.dp, bottom = 18.dp)
            .height(52.dp).clip(C.rPill).background(if (onWallpaper) C.float else C.floatHi).border(1.dp, C.line2, C.rPill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).fillMaxHeight().clickable { open(true) }.padding(start = 18.dp).labAnchor(a("dock:search")), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Search, C.text2, size = 20.dp)
            Spacer(Modifier.width(12.dp))
            BasicText("Search, add or ask", style = C.body.copy(color = C.text2), maxLines = 1)
        }
        IconButton(Glyph.Agent, "Ask the agent", { ui.sent = null; ui.sheet = Sheet.QuickSend }, tint = C.cyan, anchor = a("dock:ask"))
        IconButton(Glyph.Grid, "All apps", { open(false) }, anchor = a("dock:apps"), modifier = Modifier.padding(end = 4.dp))
    }
}

private data class Quad(val words: String, val detail: String, val anchor: String, val act: () -> Unit)

/**
 * The start page's icons. A tap opens the app (the lab says so); a long press opens its menu, and
 * the menu stays while the finger rests; a drag from there lifts the icon, closes the menu and
 * carries it to a new place, the others making room.
 */
@Composable
private fun Icons(store: ConceptStore, ui: CtxUi, columns: Int, ink: Ink) {
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
                        style = C.meta.copy(color = ink.text, textAlign = TextAlign.Center, shadow = ink.shadow, fontWeight = FontWeight.Medium),
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
        BasicText(LocalClock.time(now, store.zone), style = TextStyle(color = ink, fontSize = C.meta.fontSize, fontFamily = C.meta.fontFamily, fontWeight = FontWeight.Medium))
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
                    Box(Modifier.weight(1f).height(56.dp).clip(C.rLg).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        BasicText(it, style = C.meta.copy(color = C.text1))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            BasicText("Android's notification shade", style = C.body.copy(fontWeight = FontWeight.Medium))
            BasicText("A swipe down on home opens it, as the brief's first decision says. The drawer is a swipe up.", style = C.meta)
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
