package com.spadaval.lor.concepts.context

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.ui.draw.*
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.MenuItem
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.lab.LocalWallpaperKind
import com.spadaval.lor.ui.LocalClock
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.BasicText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.T3Card
import com.spadaval.lor.concepts.kit.Backdrop
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.KeyboardHeight
import com.spadaval.lor.concepts.kit.LocalKitLook
import com.spadaval.lor.concepts.kit.PageShell
import com.spadaval.lor.concepts.kit.ShellState
import com.spadaval.lor.concepts.kit.ToastHost
import com.spadaval.lor.lab.LabScene
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.Opened
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.Version
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lor.LorTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Instant

/*
 * Context (docs/design/lab/brief.md, "### 4. Context"): everything about what you are doing, in one
 * place. The other directions organise by urgency, by the clock or by screen division; this one
 * gathers around the thing being worked on. The current context is chosen by the running timer or,
 * when nothing runs, by the agent's one thing; its title and timer, its next task, its other tasks,
 * the work it gave the agent, its people, its page and its time this week arrive together.
 *
 * Two pages: Now, left of home, and home. Now leads with what waits on him from T3 (as the spec
 * has it), then the current context as one raised surface, then the next fixed event with what fits
 * before it, and last, flat and quiet, what waits elsewhere, each opening in its own context. A bar
 * of contexts at its top (the current one, the recent ones, Loose ends, All) switches the whole
 * surface: the signature move. Home is icons and search, with one card for the current context:
 * its title and timer, its next task, the next event. Unfolded, the context is on the left and its
 * thread, page or person on the right.
 *
 * Screen state lives in [CtxUi], made once per state above the cover/inner branch, so a fold keeps
 * the page, the chosen context, the open thread, a draft and every list's scroll.
 */
object Context {
    val version = Version("context", "Concept: Context. Everything about what you are doing in one place: the current project, person or slot gathered on one surface, switched from a bar of contexts") { ContextPhone(it) }
}

/** What floats over every page: one at a time. */
sealed interface Sheet {
    data object Timer : Sheet
    data object QuickSend : Sheet
    data object Contexts : Sheet
    data class Settings(val section: String? = "sync") : Sheet
    data class Folder(val name: String) : Sheet
}

/** An open menu beside [at] (screen coordinates). */
sealed interface Menu {
    val at: Rect
    data class App(val ref: String, override val at: Rect) : Menu
    data class Row(val key: String, override val at: Rect) : Menu
    data class Page(override val at: Rect) : Menu
}

class CtxUi(val store: ConceptStore, val scope: CoroutineScope) {
    val shell = ShellState(pages = 2, home = HOME)
    var sheet: Sheet? by mutableStateOf(null)
    var menu: Menu? by mutableStateOf(null)
    var menuFor: String? by mutableStateOf(null)

    /** The context chosen from the bar or a link; null follows the timer and the one thing. */
    var chosen: String? by mutableStateOf(null)
    /** What is open on Now: a card key, `thread:<id>`, `page:<context title>` or `person:<name>`. A thread or page opens beside the context unfolded and in a sheet on the cover. */
    var open: String? by mutableStateOf(null)
    /** The task opened in place, in its row. */
    var expanded: String? by mutableStateOf(null)
    /** Whether the crowded queue shows every card, not the first three. */
    var allCards by mutableStateOf(false)
    /** The uncommon things of a context, collapsed until asked for, by context key. */
    val more = mutableStateMapOf<String, Boolean>()

    /** Links accepted here: a task under nothing to a context, a timer label to a context. Undo takes them back. */
    val links = mutableStateMapOf<String, String>()
    val labelLinks = mutableStateMapOf<String, String>()
    val model: Model get() = Model(store, links, labelLinks)

    val search = TextFieldState()
    var searchApp: String? by mutableStateOf(null)
    var appSheet: String? by mutableStateOf(null)
    val quick = TextFieldState()
    var switching by mutableStateOf(false)

    var focused: TextFieldState? by mutableStateOf(null)
    var onEnter: (() -> Unit)? = null
    val keyboard: Boolean get() = focused != null
    var focusSearch by mutableStateOf(false)
    var wide by mutableStateOf(false)

    private val fields = mutableMapOf<String, TextFieldState>()
    fun field(name: String): TextFieldState = fields.getOrPut(name) { TextFieldState() }
    var sent: ConceptStore.SendResult? by mutableStateOf(null)
    var sentFor by mutableStateOf("")
    var project by mutableStateOf(store.projects.firstOrNull() ?: "life-on-rails")
    val picked = mutableStateMapOf<String, Set<String>>()

    /** The context Now shows, and why. */
    fun shown(): Pair<Ctx, Why> {
        val m = model
        chosen?.let { k -> m.byKey(k, store.now)?.let { return it to Why.CHOSEN } }
        return m.current()
    }

    /** Turns Now to [c]: the whole surface changes, and what was open in the last one closes. */
    fun switchTo(c: Ctx, then: (() -> Unit)? = null) {
        chosen = if (c.key == model.current().first.key) null else c.key
        expanded = null
        if (open?.startsWith("page:") == true || open?.startsWith("person:") == true) open = null
        then?.invoke()
    }

    /** Back to the context the timer or the one thing picks. */
    fun followNow() { chosen = null; expanded = null }

    /** Links a task under nothing to [title], with Undo. */
    fun link(t: LorTasks.Task, title: String) {
        if (!saves()) return
        store.undoable("Linked to $title", { links[t.id] = title }, { links.remove(t.id) })
    }

    /** Links a timer label (or, with "", the running timer that has none) to [title], with Undo: the time so far counts there too. */
    fun linkLabel(label: String, title: String) {
        if (!saves()) return
        store.undoable(if (label.isEmpty()) "This time counts toward $title" else "“$label” counts toward $title", { labelLinks[label] = title }, { labelLinks.remove(label) })
    }

    /** Names for what both pages draw: plain on the page showing, `name@page` on the one beside it. */
    fun anchorsOn(page: Int): (String) -> String = { if (shell.pager.currentPage == page) it else "$it@$page" }

    companion object {
        const val NOW = 0
        const val HOME = 1
    }
}

// ---- The phone ----

@Composable
fun ContextPhone(scene: LabScene) {
    val scope = rememberCoroutineScope()
    val store = remember(scene.fixture) { ConceptStore(scene.fixture, scope) }
    val ui = remember(scene.fixture) { CtxUi(store, scope) }
    LaunchedEffect(ui) { ui.openWhere() }
    val wide = scene.widthDp >= 600
    ui.wide = wide
    CompositionLocalProvider(LocalKitLook provides C.kit) {
        Box(Modifier.fillMaxSize()) {
            val behind by animateFloatAsState(if (ui.sheet != null || ui.appSheet != null || (!wide && ui.open != null)) 1f else 0f, tween(220))
            Backdrop({ behind }) {
                PageShell(
                    ui.shell,
                    onDoubleTap = { ui.sheet = Sheet.QuickSend },
                    drawer = { close -> Drawer(store, ui, wide, close) },
                ) { page ->
                    when (page) {
                        CtxUi.NOW -> NowPage(store, ui, wide)
                        else -> Home(store, ui, wide)
                    }
                }
            }
            StatusBar(store, ink = statusInk(ui, scene.wallpaper))
            Shade(ui.shell)
            OpenSheet(store, ui, wide)
            Sheets(store, ui, wide)
            ui.menu?.let { MenuFor(store, ui, it) }
            ToastHost(store, Modifier.align(Alignment.BottomCenter).padding(bottom = if (ui.keyboard) KeyboardHeight + 16.dp else 92.dp))
        }
    }
}

/** Dark status-bar ink over home's pale top; light everywhere else. */
@Composable
private fun statusInk(ui: CtxUi, wallpaper: Wallpaper) =
    if (wallpaper != Wallpaper.Dark && ui.shell.pager.currentPage == CtxUi.HOME && ui.shell.drawer.value < 0.5f && ui.sheet == null) C.inkDark else C.text1

/** Opens what the state says is open. */
private suspend fun CtxUi.openWhere() {
    val w = store.fixture.where
    when (w.open) {
        Opened.DRAWER -> shell.drawer.snapTo(1f)
        Opened.SEARCH -> { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) }; focusSearch = true }
        Opened.APP_MENU -> if (w.typed.isNotEmpty()) { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) } } else menuFor = w.target
        Opened.QUICK_SEND -> { quick.edit { replace(0, length, w.typed) }; sheet = Sheet.QuickSend }
        Opened.APP_TAB -> sheet = when (w.target) { "timer" -> Sheet.Timer; "sensors" -> Sheet.Settings("sensors"); "t3" -> Sheet.Settings("connections"); else -> Sheet.Settings("sync") }
        Opened.HOME_SETTINGS -> sheet = Sheet.Settings("home")
        Opened.THREAD -> {
            shell.pager.scrollToPage(CtxUi.NOW)
            open = store.cards.firstOrNull { it.thread.id == w.target }?.key ?: "thread:${w.target}"
        }
        else -> {}
    }
    // The demo layout's agent page is [0, 0] and its LoR panel [3, 0]: both are Now here.
    when (w.cell) {
        listOf(0, 0) -> shell.pager.scrollToPage(CtxUi.NOW)
        listOf(3, 0) -> { shell.pager.scrollToPage(CtxUi.NOW); if (w.target == "people") store.people?.overdue?.firstOrNull()?.let { open = "person:${it.name}" } }
        else -> {}
    }
}

// ---- Time ----

/** The store's clock, read again every second, so every readout agrees. */
@Composable
fun liveNow(store: ConceptStore): Instant {
    var now by remember { mutableStateOf(store.clock()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            withFrameMillis { now = store.clock() }
        }
    }
    return now
}

/** "47:12", "1:05:30". */
fun readout(d: Duration): String {
    val s = d.inWholeSeconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    fun two(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "$m:${two(sec)}"
}

/** "47 min", "1 h 12". */
fun spoken(d: Duration): String {
    val m = d.inWholeMinutes.coerceAtLeast(0)
    if (m < 1) return "under a minute"
    return if (m < 60) "$m min" else "${m / 60} h${if (m % 60 > 0) " ${m % 60}" else ""}"
}

fun ago(at: Instant?, now: Instant): String {
    at ?: return ""
    val m = (now - at).inWholeMinutes
    return when {
        m < 1 -> "just now"
        m < 60 -> "$m min ago"
        m < 24 * 60 -> "${m / 60} h ago"
        m < 48 * 60 -> "yesterday"
        else -> "${m / (24 * 60)} days ago"
    }
}

fun parseOrNull(iso: String?): Instant? = iso?.let { runCatching { Instant.parse(it) }.getOrNull() }

/** A launch, in the lab: the phone would leave home for the app; the lab says so. */
fun ConceptStore.launch(label: String) = toast("Opens $label")

fun CtxUi.later(scope: CoroutineScope, block: suspend CtxUi.() -> Unit) { scope.launch { block() } }

/** LoR can't save while it isn't answering: a write says so instead of pretending. */
fun CtxUi.saves(): Boolean {
    if (store.homeLoad == Load.OK) return true
    store.toast("LoR isn't answering, so nothing can be saved yet")
    return false
}

fun ConceptStore.timerOk(): Boolean = timerReachable && lorLive

fun ConceptStore.knowsLor(): Boolean = home != null
fun ConceptStore.knowsT3(): Boolean = t3Status == T3Status.LIVE || (t3Status == T3Status.RETRYING && t3SeenAt != null)

/** What waits on him: T3's cards and LoR's flags. */
fun ConceptStore.waitingCount(): Int = cards.size + home?.needsYou.orEmpty().size

/** The card or thread a key on Now opens. */
fun ConceptStore.cardOrThread(key: String): Pair<T3Card?, com.spadaval.lor.t3.T3Thread?> {
    cards.firstOrNull { it.key == key }?.let { return it to it.thread }
    val t = if (key.startsWith("thread:")) threads.firstOrNull { it.id == key.removePrefix("thread:") }
    else threads.firstOrNull { t -> "failed:${t.id}" == key || t.requests.any { it.requestId == key } }
    return null to t
}
