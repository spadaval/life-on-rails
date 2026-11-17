package com.spadaval.lor.concepts.queue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.LocalKitLook
import com.spadaval.lor.lab.LocalWallpaperKind
import com.spadaval.lor.concepts.kit.PageShell
import com.spadaval.lor.concepts.kit.ShellState
import com.spadaval.lor.concepts.kit.ToastHost
import com.spadaval.lor.lab.LabScene
import com.spadaval.lor.lab.LabScreen
import com.spadaval.lor.lab.Opened
import com.spadaval.lor.lab.Version
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorTasks
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/*
 * Queue (docs/design/lab/brief.md, "### 1. Queue"): one list of what wants Sumeet, read top to
 * bottom, and the job is to clear it. Home stays icons and search with one line at the top; the
 * queue is the page left of home, merging the agent page and the LoR panel; the drawer is below.
 * Unfolded, the queue is the left pane and the open card the right.
 *
 * Its screen state lives in [QueueUi], made once per state above the cover/inner branch, so a fold
 * keeps the page, the open card, the draft and the scroll.
 */
object Queue {
    val version = Version("queue", "Concept: Queue. One list of what wants you; home keeps icons and search with one line; the queue is left of home") { QueuePhone(it) }
}

/** What floats over the pages: one sheet at a time. */
sealed interface Sheet {
    data class Item(val key: String) : Sheet
    data object Timer : Sheet
    data object QuickSend : Sheet
    data class Settings(val section: String? = null) : Sheet
    data class Folder(val name: String) : Sheet
}

/** An open menu: an app's on home or in the drawer, or a queue item's, beside [at] (screen coordinates). */
sealed interface Menu {
    val at: Rect
    data class App(val ref: String, override val at: Rect) : Menu
    data class Item(val key: String, override val at: Rect) : Menu
}

class QueueUi(val store: ConceptStore) {
    val shell = ShellState(pages = 2, home = HOME)
    var sheet: Sheet? by mutableStateOf(null)
    var menu: Menu? by mutableStateOf(null)

    /** The app whose menu a state opens, once its icon has a place on screen. */
    var menuFor: String? by mutableStateOf(null)

    val search = TextFieldState()
    val quick = TextFieldState()

    /** The field with focus, which the stand-in keyboard types into, and what its Enter does. */
    var focused: TextFieldState? by mutableStateOf(null)
    var onEnter: (() -> Unit)? = null
    /** The stand-in keyboard is up: a field has focus. */
    val keyboard: Boolean get() = focused != null
    /** Search should take focus when the drawer opens (it was opened from the search field). */
    var focusSearch by mutableStateOf(false)
    /** The screen is wide (the inner one, or turned): items open in the right pane. */
    var wide by mutableStateOf(false)

    /** Whether a sheet is up: an item opens in the right pane instead when the screen is wide. */
    fun sheetShown(store: ConceptStore): Boolean = when (val s = sheet) {
        null -> false
        is Sheet.Item -> !(wide && store.items().any { it.key == s.key })
        else -> true
    }

    private val fields = mutableMapOf<String, TextFieldState>()
    /** A named field's state, kept here so what is typed survives a fold. */
    fun field(name: String): TextFieldState = fields.getOrPut(name) { TextFieldState() }
    var sent: ConceptStore.SendResult? by mutableStateOf(null)
    var project by mutableStateOf(store.projects.firstOrNull() ?: "life-on-rails")

    /** Picked options of a multi-answer question, by card key. */
    val picked = androidx.compose.runtime.mutableStateMapOf<String, Set<String>>()

    companion object {
        const val QUEUE = 0
        const val HOME = 1
    }
}

/** One thing in the queue, in its ranking. */
sealed interface Item {
    val key: String
    data class Card(val card: T3Card) : Item { override val key get() = card.key }
    data class Flag(val need: LorHome.Need) : Item { override val key get() = "flag:${need.id}" }
    data class Due(val task: LorTasks.Task) : Item { override val key get() = "task:${task.id}" }
}

/**
 * The ranking (the brief's): T3 approvals and questions first, because a turn is blocked on them,
 * then failed turns; then LoR's flags; then tasks due today or before, the oldest due first. A task
 * ticked here stays, struck, so it can be unticked.
 */
fun ConceptStore.items(): List<Item> {
    val flags = home?.needsYou.orEmpty()
    val flagged = flags.map { it.id }.toSet()
    val due = tasks.filter { t -> t.due != null && t.due!! <= today && t.id !in flagged && t.waitingOn == null }.sortedBy { it.due }
    return cards.map { Item.Card(it) } + flags.map { Item.Flag(it) } + due.map { Item.Due(it) }
}

/** How many things want him: what the queue holds less what is ticked. */
fun ConceptStore.waiting(): Int = items().count { it !is Item.Due || done[it.task.id] != true }

@Composable
fun QueuePhone(scene: LabScene) {
    val scope = rememberCoroutineScope()
    val store = remember(scene.fixture) { ConceptStore(scene.fixture, scope) }
    val ui = remember(scene.fixture) { QueueUi(store) }
    LaunchedEffect(ui) { ui.openWhere(scene) }
    val wide = scene.widthDp >= 600
    ui.wide = wide
    CompositionLocalProvider(LocalKitLook provides Q.kit) {
        Box(Modifier.fillMaxSize()) {
            val behind by androidx.compose.animation.core.animateFloatAsState(if (ui.sheetShown(store)) 1f else 0f, androidx.compose.animation.core.tween(220))
            com.spadaval.lor.concepts.kit.Backdrop({ behind }) {
                PageShell(
                    ui.shell,
                    onDoubleTap = { ui.sheet = Sheet.QuickSend },
                    drawer = { close -> Drawer(store, ui, wide, close) },
                ) { page ->
                    when (page) {
                        QueueUi.QUEUE -> QueuePage(store, ui, wide)
                        else -> Home(store, ui, wide)
                    }
                }
            }
            StatusBar(store, ink = statusInk(ui, scene.wallpaper))
            Shade(ui.shell)
            Sheets(store, ui, wide)
            ui.menu?.let { MenuFor(store, ui, it) }
            ToastHost(store, Modifier.align(Alignment.BottomCenter).padding(bottom = if (ui.keyboard) 250.dp else 88.dp))
        }
    }
}

/** Dark status-bar ink over home's pale tops (the pale and mixed wallpapers); light everywhere else. */
@Composable
private fun statusInk(ui: QueueUi, wallpaper: Wallpaper) =
    if (wallpaper != Wallpaper.Dark && ui.shell.pager.currentPage == QueueUi.HOME && ui.shell.drawer.value < 0.5f && ui.sheet == null) Q.inkDark else Q.text1

/** Opens what the state says is open: the drawer, search, a menu, a sheet, the queue, a thread. */
private suspend fun QueueUi.openWhere(scene: LabScene) {
    val w = scene.fixture.where
    when (w.open) {
        Opened.DRAWER -> shell.drawer.snapTo(1f)
        Opened.SEARCH -> { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) }; focusSearch = true }
        Opened.APP_MENU -> if (w.typed.isNotEmpty()) { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) } } else menuFor = w.target
        Opened.QUICK_SEND -> { quick.edit { replace(0, length, w.typed) }; sheet = Sheet.QuickSend }
        Opened.APP_TAB -> sheet = if (w.target == "timer") Sheet.Timer else Sheet.Settings(w.target)
        Opened.HOME_SETTINGS -> sheet = Sheet.Settings("home")
        Opened.THREAD -> {
            shell.pager.scrollToPage(QueueUi.QUEUE)
            val key = store.cards.firstOrNull { it.thread.id == w.target }?.key ?: "thread:${w.target}"
            store.open = key
            if (scene.screen == LabScreen.Cover && !scene.landscape) sheet = Sheet.Item(key)
        }
        else -> {}
    }
    // The agent page's cell and the LoR panel's (the demo layout's [0, 0] and [3, 0]) are Queue's queue page.
    if (w.cell != null && w.cell != listOf(1, 0)) shell.pager.scrollToPage(QueueUi.QUEUE)
}

// ---- Time, as Queue writes it ----

/** The fixture's moment, moving on with the wall clock, so a running timer counts. */
@Composable
fun liveNow(store: ConceptStore): Instant {
    var now by remember { mutableStateOf(store.now) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            kotlinx.coroutines.delay(1000)
            withFrameMillis { now = store.now + (it - start).milliseconds }
        }
    }
    return now
}

/** "47:12", "1:05:30": a running timer's readout. */
fun readout(d: Duration): String {
    val s = d.inWholeSeconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    fun two(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "$m:${two(sec)}"
}

/** "47 min", "1 h 12 min": a running timer in words. */
fun spoken(d: Duration): String {
    val m = d.inWholeMinutes.coerceAtLeast(0)
    return if (m < 60) "$m min" else "${m / 60} h${if (m % 60 > 0) " ${m % 60} min" else ""}"
}

/** "just now", "8 min ago", "2 h ago", "yesterday". */
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

/** Runs [block] in the shell's scope; for taps that move the pager or the drawer. */
fun QueueUi.later(scope: kotlinx.coroutines.CoroutineScope, block: suspend QueueUi.() -> Unit) { scope.launch { block() } }
