package com.spadaval.lor.concepts.timeline

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
import androidx.compose.runtime.mutableStateListOf
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
import com.spadaval.lor.concepts.kit.LocalKitLook
import com.spadaval.lor.concepts.kit.PageShell
import com.spadaval.lor.concepts.kit.ShellState
import com.spadaval.lor.concepts.kit.ToastHost
import com.spadaval.lor.lab.LabScene
import com.spadaval.lor.lab.Opened
import com.spadaval.lor.lab.Version
import com.spadaval.lor.lab.Wallpaper
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorTasks
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Instant

/*
 * Timeline (docs/design/lab/brief.md, "### 2. Timeline"): the day is the spine. Home's header is a
 * strip of the day with now, the next event, the time left and the running timer. The page left of
 * home is the day: what waits on Sumeet pinned above it (T3's cards first, then LoR's flags), and
 * below, the route from waking to sleep, where the timer is the now line, past blocks are named
 * where they are, and free gaps can take a task. The drawer is below home.
 * Unfolded, the route is on the left and the waiting cards, the untimed tasks or a thread on the right.
 *
 * Its screen state lives in [TimelineUi], made once per state above the cover/inner branch, so a
 * fold keeps the page, the open card or thread, the draft and the route's scroll.
 */
object Timeline {
    val version = Version("timeline", "Concept: Timeline. The day is the spine: home has a strip of the day; left of home, what waits is pinned over the route, where the timer is the now line") { TimelinePhone(it) }
}

/** What floats over the pages: one sheet at a time. */
sealed interface Sheet {
    data class Item(val key: String) : Sheet
    data class Block(val id: String) : Sheet
    data class Plan(val start: Instant, val end: Instant) : Sheet
    data object Waiting : Sheet
    data object Untimed : Sheet
    data object Timer : Sheet
    data object QuickSend : Sheet
    data class Settings(val section: String? = null) : Sheet
    data class Folder(val name: String) : Sheet
}

/** An open menu beside [at] (screen coordinates): an app's on home or in the drawer, a waiting card's, or the day's. */
sealed interface Menu {
    val at: Rect
    data class App(val ref: String, override val at: Rect) : Menu
    data class Item(val key: String, override val at: Rect) : Menu
    data class Day(override val at: Rect) : Menu
}

/** A task planned into a free gap on the route: dashed in cyan until its time comes. */
data class Plan(val taskId: String, val title: String, val start: Instant, val end: Instant)

class TimelineUi(val store: ConceptStore) {
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
    val keyboard: Boolean get() = focused != null
    /** Search should take focus when the drawer opens (it was opened from the search field). */
    var focusSearch by mutableStateOf(false)
    /** The screen is wide (the inner one, or turned): an open card or thread takes the right pane. */
    var wide by mutableStateOf(false)

    /** Whether a sheet covers the pages: an open card is a pane, not a sheet, when the screen is wide. */
    fun sheetShown(): Boolean = when (sheet) {
        null -> false
        is Sheet.Item, Sheet.Waiting, Sheet.Untimed -> !wide
        else -> true
    }

    private val fields = mutableMapOf<String, TextFieldState>()
    /** A named field's state, kept here so what is typed survives a fold. */
    fun field(name: String): TextFieldState = fields.getOrPut(name) { TextFieldState() }
    var sent: ConceptStore.SendResult? by mutableStateOf(null)
    /** The field's words when [sent] was set: the result shows until they change, then Send is back. */
    var sentFor by mutableStateOf("")
    var project by mutableStateOf(store.projects.firstOrNull() ?: "life-on-rails")

    /** Picked options of a multi-answer question, by card key. */
    val picked = mutableStateMapOf<String, Set<String>>()

    /** The day's blocks as recorded, with relabels applied (each with Undo). */
    var blocks: List<DayBlock> by mutableStateOf(demoBlocks(store))
    /** Tasks planned into gaps. */
    val plans = mutableStateListOf<Plan>()
    /** The block lifted on the route (its sheet is open). */
    var selected: String? by mutableStateOf(null)

    /** Whether the route has been put with now a third of the way down, once. */
    var placed = false

    companion object {
        const val DAY = 0
        const val HOME = 1
    }
}

/** One thing that waits on him, pinned above the day: T3's cards first (a turn is blocked on them), then LoR's flags. */
sealed interface Item {
    val key: String
    data class Card(val card: T3Card) : Item { override val key get() = card.key }
    data class Flag(val need: LorHome.Need) : Item { override val key get() = "flag:${need.id}" }
    data class Due(val task: LorTasks.Task) : Item { override val key get() = "task:${task.id}" }
}

/** What waits on him, in order: T3's approvals, questions and failed turns, then LoR's flags. Tasks are not here; they are untimed. */
fun ConceptStore.waitingItems(): List<Item> = cards.map { Item.Card(it) } + home?.needsYou.orEmpty().map { Item.Flag(it) }

/** Tasks due today or before that have no time: the untimed column. A ticked one stays, struck, so it can be unticked. */
fun ConceptStore.untimed(plans: List<Plan>): List<LorTasks.Task> {
    val flagged = home?.needsYou.orEmpty().map { it.id }.toSet()
    val planned = plans.map { it.taskId }.toSet()
    return tasks.filter { t -> t.due != null && t.due!! <= today && t.id !in flagged && t.id !in planned && t.waitingOn == null }.sortedBy { it.due }
}

/** The item a key opens: a waiting card or flag, or any open task by `task:<id>`; null once it has gone. */
fun ConceptStore.item(key: String): Item? {
    waitingItems().firstOrNull { it.key == key }?.let { return it }
    val id = key.removePrefix("task:").takeIf { key.startsWith("task:") } ?: return null
    home?.needsYou?.firstOrNull { it.id == id }?.let { return Item.Flag(it) }
    return tasks.firstOrNull { it.id == id }?.let { Item.Due(it) }
}

/** Whether an empty waiting list means nothing waits: LoR's list was read, and T3's cards are live. */
fun ConceptStore.knows(): Boolean = home != null && t3Status == com.spadaval.lor.lab.T3Status.LIVE

@Composable
fun TimelinePhone(scene: LabScene) {
    val scope = rememberCoroutineScope()
    val store = remember(scene.fixture) { ConceptStore(scene.fixture, scope) }
    val ui = remember(scene.fixture) { TimelineUi(store) }
    LaunchedEffect(ui) { ui.openWhere(scene) }
    val wide = scene.widthDp >= 600
    ui.wide = wide
    CompositionLocalProvider(LocalKitLook provides T.kit) {
        Box(Modifier.fillMaxSize()) {
            val behind by animateFloatAsState(if (ui.sheetShown()) 1f else 0f, tween(200))
            Backdrop({ behind }) {
                PageShell(
                    ui.shell,
                    onDoubleTap = { ui.sheet = Sheet.QuickSend },
                    drawer = { close -> Drawer(store, ui, wide, close) },
                ) { page ->
                    when (page) {
                        TimelineUi.DAY -> DayPage(store, ui, wide)
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
private fun statusInk(ui: TimelineUi, wallpaper: Wallpaper) =
    if (wallpaper != Wallpaper.Dark && ui.shell.pager.currentPage == TimelineUi.HOME && ui.shell.drawer.value < 0.5f && !ui.sheetShown()) T.inkDark else T.text1

/** Opens what the state says is open: the drawer, search, a menu, a sheet, the day page, a thread. */
private suspend fun TimelineUi.openWhere(scene: LabScene) {
    val w = scene.fixture.where
    when (w.open) {
        Opened.DRAWER -> shell.drawer.snapTo(1f)
        Opened.SEARCH -> { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) }; focusSearch = true }
        Opened.APP_MENU -> if (w.typed.isNotEmpty()) { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) } } else menuFor = w.target
        Opened.QUICK_SEND -> { quick.edit { replace(0, length, w.typed) }; sheet = Sheet.QuickSend }
        Opened.APP_TAB -> sheet = if (w.target == "timer") Sheet.Timer else Sheet.Settings(w.target)
        Opened.HOME_SETTINGS -> sheet = Sheet.Settings("home")
        Opened.THREAD -> {
            shell.pager.scrollToPage(TimelineUi.DAY)
            val key = store.cards.firstOrNull { it.thread.id == w.target }?.key ?: "thread:${w.target}"
            store.open = key
            sheet = Sheet.Item(key)
        }
        else -> {}
    }
    // The agent page's cell and the LoR panel's (the demo layout's [0, 0] and [3, 0]) are Timeline's day page.
    if (w.cell != null && w.cell != listOf(1, 0)) shell.pager.scrollToPage(TimelineUi.DAY)
}

// ---- Time, as Timeline writes it ----

/** The store's clock ([ConceptStore.clock]), read again every second, so every readout agrees. */
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

/** "47:12", "1:05:30": a running timer's readout. */
fun readout(d: Duration): String {
    val s = d.inWholeSeconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    fun two(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "$m:${two(sec)}"
}

/** "47 min", "1 h 12 min": a length of time in words. */
fun spoken(d: Duration): String {
    val m = d.inWholeMinutes.coerceAtLeast(0)
    return if (m < 60) "$m min" else "${m / 60} h${if (m % 60 > 0) " ${m % 60} min" else ""}"
}

/** What is left of the waking day, to the nearest ten minutes: "7 h left", "2 h 30 min left". */
fun leftWords(d: Duration): String = spoken(with(Duration) { ((d.inWholeMinutes + 5) / 10 * 10).minutes }) + " left"

/** "47m", "1h 12m": a length on the route, where room is short. */
fun short(d: Duration): String {
    val m = d.inWholeMinutes.coerceAtLeast(0)
    return if (m < 60) "${m}m" else "${m / 60}h${if (m % 60 > 0) " ${m % 60}m" else ""}"
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

/** Runs [block] in [scope]; for taps that move the pager or the drawer. */
fun TimelineUi.later(scope: kotlinx.coroutines.CoroutineScope, block: suspend TimelineUi.() -> Unit) { scope.launch { block() } }
