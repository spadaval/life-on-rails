package com.spadaval.lor.concepts.panes

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
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorPeople
import com.spadaval.lor.lor.LorTasks
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Instant

/*
 * Panes (docs/design/lab/brief.md, "### 3. Panes"): nothing leaves the screen. Every panel is a list
 * and its detail. On the Fold's inner screen, the reason for this direction, they sit side by side:
 * the agent page is T3's cards and threads on the left and the open thread on the right; Today (the
 * LoR panel) is the timer, what LoR flagged, the tasks due and the people to get back to, beside the
 * open one; search sits beside home as a pane, results on the left and the chosen app's shortcuts
 * and conversations on the right. On the cover the same panes become a list with a sheet that slides
 * up over it and rests at half height, so both show.
 *
 * Pages, left to right: the agent page, home, Today. A quiet switcher at the top of every page names
 * them, and a dock at the thumb on every page holds search and the timer, which stops or switches
 * from anywhere. The common jobs (approve, deny, done, seen, tick, stop) are on the rows and open
 * nothing; the detail is for reading and changing what a row can't.
 *
 * Screen state lives in [PanesUi], made once per state above the cover/inner branch, so a fold
 * keeps the page, the open card, thread, task or person, a draft and every list's scroll.
 */
object Panes {
    val version = Version("panes", "Concept: Panes. Nothing leaves the screen: every panel is a list and its detail, side by side unfolded and a half-height sheet over the list on the cover") { PanesPhone(it) }
}

/** What floats over every page: one at a time. The open item of a panel is not one of these: it is its pane. */
sealed interface Sheet {
    data object Timer : Sheet
    data object QuickSend : Sheet
    data class Settings(val section: String? = "sync") : Sheet
    data class Folder(val name: String) : Sheet
}

/** An open menu beside [at] (screen coordinates). */
sealed interface Menu {
    val at: Rect
    data class App(val ref: String, override val at: Rect) : Menu
    data class Row(val key: String, override val at: Rect) : Menu
    data class Page(val page: Int, override val at: Rect) : Menu
}

class PanesUi(val store: ConceptStore, val scope: CoroutineScope) {
    val shell = ShellState(pages = 3, home = HOME)
    var sheet: Sheet? by mutableStateOf(null)
    var menu: Menu? by mutableStateOf(null)
    /** The app whose menu a state opens, once its icon has a place on screen. */
    var menuFor: String? by mutableStateOf(null)

    /** The open item of each panel: a card key or `thread:<id>` on the agent page; `task:`, `flag:` or `person:` on Today. */
    var openAgent: String? by mutableStateOf(null)
    var openToday: String? by mutableStateOf(null)
    /** On the cover, whether the open item's sheet is at full height rather than half. Kept, so a fold and back keeps it. */
    var full = mutableStateMapOf<Int, Boolean>()

    val search = TextFieldState()
    /** The app chosen in search's results, whose pane shows its shortcuts and conversations. */
    var searchApp: String? by mutableStateOf(null)
    /** On the cover, the app whose pane is open as a sheet over search. */
    var appSheet: String? by mutableStateOf(null)
    val quick = TextFieldState()
    /** The timer sheet opened to switch rather than start. */
    var switching by mutableStateOf(false)

    /** The field with focus, which the stand-in keyboard types into, and what its Enter does. */
    var focused: TextFieldState? by mutableStateOf(null)
    var onEnter: (() -> Unit)? = null
    val keyboard: Boolean get() = focused != null
    /** Search should take focus when the drawer opens (it was opened from the search field). */
    var focusSearch by mutableStateOf(false)
    var wide by mutableStateOf(false)

    private val fields = mutableMapOf<String, TextFieldState>()
    /** A named field's state, kept here so what is typed survives a fold. */
    fun field(name: String): TextFieldState = fields.getOrPut(name) { TextFieldState() }
    var sent: ConceptStore.SendResult? by mutableStateOf(null)
    var sentFor by mutableStateOf("")
    var project by mutableStateOf(store.projects.firstOrNull() ?: "life-on-rails")

    /** Picked options of a multi-answer question, by card key. */
    val picked = mutableStateMapOf<String, Set<String>>()
    /** Open "More" sections of a detail pane, by item key: the uncommon things, collapsed until asked for. */
    val more = mutableStateMapOf<String, Boolean>()

    /**
     * Names for what every page draws (the switcher, the dock): plain on the page showing, and
     * `name@page` on the pages beside it, which the pager keeps composed off screen.
     */
    fun anchorsOn(page: Int): (String) -> String = { if (shell.pager.currentPage == page) it else "$it@$page" }

    fun open(page: Int): String? = if (page == AGENT) openAgent else openToday
    fun setOpen(page: Int, key: String?) { if (page == AGENT) openAgent = key else openToday = key }

    companion object {
        const val AGENT = 0
        const val HOME = 1
        const val TODAY = 2
    }
}

// ---- What each panel lists ----

/** A row of the agent page: a card that waits on him, or a thread. */
sealed interface AgentRow {
    val key: String
    data class Card(val card: T3Card) : AgentRow { override val key get() = card.key }
    data class Thread(val thread: T3Thread) : AgentRow { override val key get() = "thread:${thread.id}" }
}

/** Threads with no card: running ones first, then the rest by when they last changed. */
fun ConceptStore.otherThreads(): List<T3Thread> {
    val carded = cards.map { it.thread.id }.toSet()
    return threads.filter { it.id !in carded }.sortedWith(compareBy<T3Thread> { if (it.state == ThreadState.RUNNING) 0 else 1 }.thenByDescending { it.updatedAt })
}

/** What a key on the agent page opens: its card while it waits, then its thread (an approval sent leaves the thread open). */
fun ConceptStore.agentRow(key: String): AgentRow? {
    cards.firstOrNull { it.key == key }?.let { return AgentRow.Card(it) }
    val t = if (key.startsWith("thread:")) threads.firstOrNull { it.id == key.removePrefix("thread:") }
    else threads.firstOrNull { t -> "failed:${t.id}" == key || t.requests.any { it.requestId == key } }
    return t?.let { AgentRow.Thread(it) }
}

/** A row of Today: a flag, a task due, or a person to get back to. */
sealed interface TodayRow {
    val key: String
    data class Flag(val need: LorHome.Need) : TodayRow { override val key get() = "flag:${need.id}" }
    data class Task(val task: LorTasks.Task) : TodayRow { override val key get() = "task:${task.id}" }
    data class Person(val person: LorPeople.Person) : TodayRow { override val key get() = "person:${person.id}" }
}

/** Tasks due today or before, flagged ones aside; a ticked one stays, struck, so it can be unticked. */
fun ConceptStore.due(): List<LorTasks.Task> {
    val flagged = home?.needsYou.orEmpty().map { it.id }.toSet()
    return tasks.filter { t -> t.due != null && t.due!! <= today && t.id !in flagged }.sortedBy { it.due }
}

/** People past their cadence, then birthdays coming. */
fun ConceptStore.peopleDue(): List<LorPeople.Person> = people?.let { it.overdue + it.birthdays.filter { b -> it.overdue.none { o -> o.id == b.id } } }.orEmpty()

fun ConceptStore.todayRow(key: String): TodayRow? {
    val id = key.substringAfter(':')
    return when {
        key.startsWith("flag:") -> home?.needsYou?.firstOrNull { it.id == id }?.let { TodayRow.Flag(it) }
            ?: tasks.firstOrNull { it.id == id }?.let { TodayRow.Task(it) }
        key.startsWith("task:") -> tasks.firstOrNull { it.id == id }?.let { TodayRow.Task(it) }
        key.startsWith("person:") -> peopleDue().firstOrNull { it.id == id }?.let { TodayRow.Person(it) }
        else -> null
    }
}

/** Whether an empty list means nothing waits: LoR's list was read, and T3's cards are live. */
fun ConceptStore.knowsLor(): Boolean = home != null
fun ConceptStore.knowsT3(): Boolean = t3Status == T3Status.LIVE || (t3Status == T3Status.RETRYING && t3SeenAt != null)

/** How many things on Today want him: flags and tasks due, not yet done. */
fun ConceptStore.todayCount(): Int = home?.needsYou.orEmpty().size + due().count { done[it.id] != true }

// ---- The phone ----

@Composable
fun PanesPhone(scene: LabScene) {
    val scope = rememberCoroutineScope()
    val store = remember(scene.fixture) { ConceptStore(scene.fixture, scope) }
    val ui = remember(scene.fixture) { PanesUi(store, scope) }
    LaunchedEffect(ui) { ui.openWhere(scene) }
    val wide = scene.widthDp >= 600
    ui.wide = wide
    CompositionLocalProvider(LocalKitLook provides P.kit) {
        Box(Modifier.fillMaxSize()) {
            val behind by animateFloatAsState(if (ui.sheet != null || ui.appSheet != null) 1f else 0f, tween(220))
            Backdrop({ behind }) {
                PageShell(
                    ui.shell,
                    onDoubleTap = { ui.sheet = Sheet.QuickSend },
                    drawer = { close -> Drawer(store, ui, wide, close) },
                ) { page ->
                    when (page) {
                        PanesUi.AGENT -> AgentPage(store, ui, wide)
                        PanesUi.TODAY -> TodayPage(store, ui, wide)
                        else -> Home(store, ui, wide)
                    }
                }
            }
            StatusBar(store, ink = statusInk(ui, scene.wallpaper))
            Shade(ui.shell)
            Sheets(store, ui, wide)
            ui.menu?.let { MenuFor(store, ui, it) }
            ToastHost(store, Modifier.align(Alignment.BottomCenter).padding(bottom = if (ui.keyboard) KeyboardHeight + 16.dp else 96.dp))
        }
    }
}

/** Dark status-bar ink over home's pale top (the pale and mixed wallpapers); light everywhere else. */
@Composable
private fun statusInk(ui: PanesUi, wallpaper: Wallpaper) =
    if (wallpaper != Wallpaper.Dark && ui.shell.pager.currentPage == PanesUi.HOME && ui.shell.drawer.value < 0.5f && ui.sheet == null) P.inkDark else P.text1

/** Opens what the state says is open: the drawer, search, a menu, a sheet, a panel, a thread. */
private suspend fun PanesUi.openWhere(scene: LabScene) {
    val w = scene.fixture.where
    when (w.open) {
        Opened.DRAWER -> shell.drawer.snapTo(1f)
        Opened.SEARCH -> { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) }; focusSearch = true }
        Opened.APP_MENU -> if (w.typed.isNotEmpty()) { shell.drawer.snapTo(1f); search.edit { replace(0, length, w.typed) } } else menuFor = w.target
        Opened.QUICK_SEND -> { quick.edit { replace(0, length, w.typed) }; sheet = Sheet.QuickSend }
        Opened.APP_TAB -> sheet = when (w.target) { "timer" -> Sheet.Timer; "sensors" -> Sheet.Settings("sensors"); "t3" -> Sheet.Settings("connections"); else -> Sheet.Settings("sync") }
        Opened.HOME_SETTINGS -> sheet = Sheet.Settings("home")
        Opened.THREAD -> {
            shell.pager.scrollToPage(PanesUi.AGENT)
            val card = store.cards.firstOrNull { it.thread.id == w.target }
            openAgent = card?.key ?: "thread:${w.target}"
            // A question opens at full height on the cover: its answers are why it is opened.
            if (card?.kind == T3Card.Kind.QUESTION) full[PanesUi.AGENT] = true
        }
        else -> {}
    }
    // The demo layout's agent page is [0, 0] and its LoR panel [3, 0]: Panes' agent page and Today.
    when (w.cell) {
        listOf(0, 0) -> shell.pager.scrollToPage(PanesUi.AGENT)
        listOf(3, 0) -> { shell.pager.scrollToPage(PanesUi.TODAY); if (w.target == "people") store.peopleDue().firstOrNull()?.let { openToday = "person:${it.id}" } }
        else -> {}
    }
}

// ---- Time, as Panes writes it ----

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

/** "47:12", "1:05:30": a running timer's readout. */
fun readout(d: Duration): String {
    val s = d.inWholeSeconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    fun two(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(sec)}" else "$m:${two(sec)}"
}

/** "47 min", "1 h 12 min". */
fun spoken(d: Duration): String {
    val m = d.inWholeMinutes.coerceAtLeast(0)
    if (m < 1) return "under a minute"
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

/** Runs [block] in [scope]; for taps that move the pager or the drawer. */
fun PanesUi.later(scope: CoroutineScope, block: suspend PanesUi.() -> Unit) { scope.launch { block() } }

/** LoR can't save while it isn't answering: a write says so instead of pretending. */
fun PanesUi.saves(): Boolean {
    if (store.homeLoad == Load.OK) return true
    store.toast("LoR isn't answering, so nothing can be saved yet")
    return false
}

/** Whether the timer can start, stop or switch: it is reachable and LoR answers, so the change would save. */
fun ConceptStore.timerOk(): Boolean = timerReachable && lorLive
