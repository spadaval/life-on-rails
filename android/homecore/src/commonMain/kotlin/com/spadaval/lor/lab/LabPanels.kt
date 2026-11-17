package com.spadaval.lor.lab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.AgentPage
import com.spadaval.lor.agent.Kept
import com.spadaval.lor.agent.Loaded
import com.spadaval.lor.agent.LorUi
import com.spadaval.lor.agent.PairCalls
import com.spadaval.lor.agent.PanelActions
import com.spadaval.lor.agent.PanelUi
import com.spadaval.lor.agent.RepliesUi
import com.spadaval.lor.agent.StartIntent
import com.spadaval.lor.agent.T3Link
import com.spadaval.lor.agent.T3Ui
import com.spadaval.lor.agent.TaskWrite
import com.spadaval.lor.agent.ThreadRead
import com.spadaval.lor.home.LorReach
import com.spadaval.lor.home.SetupActions
import com.spadaval.lor.home.SetupCard
import com.spadaval.lor.home.SetupUi
import com.spadaval.lor.lor.LorBlock
import com.spadaval.lor.lor.LorBlockKind
import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.lor.LorPanel
import com.spadaval.lor.lor.LorReplies
import com.spadaval.lor.lor.LorTab
import com.spadaval.lor.quicksend.QuickSendSheet
import com.spadaval.lor.quicksend.SheetActions
import com.spadaval.lor.quicksend.SheetUi
import com.spadaval.lor.quicksend.Shot
import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadMessage
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.LorTheme
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.PanelFace
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant
import com.spadaval.lor.t3.Question as T3Question
import com.spadaval.lor.t3.ThreadState as T3State

/*
 * The agent page, the LoR panel, LoR's blocks, the set-up card and the quick-send sheet in the lab
 * (lor-rgx4), drawn by the same functions the app calls, from the fixture's parts ([LabPanels]).
 * Until home's grid draws in the module (lor-xdoi), `current` draws one of them alone when the state
 * points at it:
 *
 * - `where.cell` at a `t3` panel of the layout: the agent page; with `where.open` THREAD, the thread
 *   `where.target` chosen (the inner screen shows it beside the page).
 * - `where.cell` at a `lor` panel: the LoR panel; `where.target` "people" opens its People tab.
 * - `where.cell` at a stack with LoR's blocks: those blocks, one under another.
 * - `where.open` QUICK_SEND: the sheet over the wallpaper; `where.target` "assist" opens it as the
 *   assistant does, with a screenshot.
 * - No layout file and set-up not done (a first run): the set-up card at the foot of the screen.
 *
 * Taps change the drawn state as the app's calls would once they succeed: an approval or answer
 * clears its card, the timer starts and stops, a tick marks a task done. Nothing is sent anywhere.
 */

/** Draws the panel the scene's state points at, and says whether it drew one. */
@Composable
fun PanelScreen(scene: LabScene): Boolean {
    val f = scene.fixture
    val target = LabPanels.target(f) ?: return false
    var ui by remember(f) { mutableStateOf(LabPanels.ui(f)) }
    val on = remember(f) { LabPanels.actions(f, { ui }, { ui = it }) }
    LorTheme {
        when (target) {
            Target.AGENT -> AgentPage(ui, on)
            Target.LOR -> LorPanel(ui, on)
            is Target.Blocks -> Column(Modifier.fillMaxSize().padding(top = 24.dp)) {
                target.kinds.forEach { LorBlock(it, ui, on, more = {}) }
            }
            Target.SHEET -> QuickSendSheet(LabPanels.sheet(f), SheetActions(send = { _, _, _, _, done -> done(null) }))
            Target.SETUP -> Box(Modifier.fillMaxSize()) {
                SetupCard(LabPanels.setup(f), SetupActions(pair = LabPanels.pairCalls), Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 16.dp))
            }
        }
    }
    return true
}

/** What a state points the lab at. */
sealed interface Target {
    data object AGENT : Target
    data object LOR : Target
    data class Blocks(val kinds: List<LorBlockKind>) : Target
    data object SHEET : Target
    data object SETUP : Target
}

/** A fixture's parts as the panels' state, and the lab's answers to their taps. */
object LabPanels {
    private val blockTypes = mapOf(
        "today" to LorBlockKind.TODAY,
        "needs-you" to LorBlockKind.NEEDS_YOU,
        "running" to LorBlockKind.RUNNING,
        "t3-queue" to LorBlockKind.T3_QUEUE,
    )

    /** Which panel [f] points at, or null for none of these. */
    fun target(f: Fixture): Target? {
        if (f.where.open == Opened.QUICK_SEND) return Target.SHEET
        val cell = f.where.cell
        if (cell == null) return if (f.layout == null && !f.settings.setupDone) Target.SETUP else null
        val panel = panelAt(f.layout ?: return null, cell) ?: return null
        return when (panel["type"]?.jsonPrimitive?.content) {
            "t3" -> Target.AGENT
            "lor" -> Target.LOR
            "stack" -> panel["blocks"]?.jsonArray.orEmpty()
                .mapNotNull { blockTypes[it.jsonObject["type"]?.jsonPrimitive?.content] }
                .takeIf { it.isNotEmpty() }?.let { Target.Blocks(it) }
            else -> null
        }
    }

    /** The active layout's panel at [cell], `[column, row]`. */
    private fun panelAt(layout: JsonObject, cell: List<Int>): JsonObject? {
        val active = layout["active"]?.jsonPrimitive?.content
        val layouts = layout["layouts"]?.jsonArray.orEmpty().map { it.jsonObject }
        val chosen = layouts.firstOrNull { it["name"]?.jsonPrimitive?.content == active } ?: layouts.firstOrNull() ?: return null
        return chosen["panels"]?.jsonArray.orEmpty().map { it.jsonObject }.firstOrNull { p ->
            p["at"]?.jsonArray?.map { it.jsonPrimitive.content.toInt() } == cell
        }
    }

    fun ui(f: Fixture): PanelUi {
        val home = f.lor.home
        val now = Instant.parse(f.clock.now)
        return PanelUi(
            lor = LorUi(
                home = home.reply?.let(LorReplies::home),
                error = home.error.takeIf { home.load == Load.STALE || home.load == Load.FAILED },
                current = home.load == Load.OK,
                tasks = kept(f.lor.tasks, LorReplies::tasks),
                people = kept(f.lor.people, LorReplies::people),
                now = now,
                zone = f.clock.zone,
            ),
            t3 = t3(f.t3),
            chosen = f.where.target.takeIf { f.where.open == Opened.THREAD },
            tab = if (f.where.target == "people") LorTab.PEOPLE else LorTab.TASKS,
        )
    }

    private fun <T> kept(read: Read, parse: (JsonObject) -> T) = Kept(
        value = read.reply?.let(parse),
        error = read.error.takeIf { read.load == Load.STALE || read.load == Load.FAILED },
        current = read.load == Load.OK,
        at = read.readAt?.let(Instant::parse),
    )

    private fun link(status: T3Status) = when (status) {
        T3Status.NOT_PAIRED -> T3Link.NOT_PAIRED
        T3Status.STOPPED, T3Status.CONNECTING -> T3Link.CONNECTING
        T3Status.LIVE -> T3Link.LIVE
        T3Status.RETRYING -> T3Link.RETRYING
        T3Status.REJECTED -> T3Link.REJECTED
    }

    fun t3(t: T3) = T3Ui(
        link = link(t.status),
        error = t.error.takeIf { t.status == T3Status.REJECTED },
        // The shell's snapshot has arrived when live, or when what was seen before stays listed.
        loaded = t.status == T3Status.LIVE || t.threads.isNotEmpty(),
        threads = t.threads.map { th ->
            T3Thread(
                id = th.id,
                title = th.title,
                projectId = th.project,
                projectTitle = th.project,
                state = T3State.valueOf(th.state.name),
                updatedAt = th.updatedAt,
                lastError = th.lastError,
                requests = th.requests.map { PendingRequest(th.id, it.id, it.kind) },
                runStartedAt = th.runStartedAt,
            )
        },
    )

    /** The questions a fixture's request asks, as T3 sends them. */
    private fun questions(t: T3, requestId: String): List<T3Question> = t.questions[requestId].orEmpty().map { q ->
        T3Question(q.id, q.header, q.question, q.options.map { T3Question.Option(it.label, it.description) }, q.multiSelect, q.allowCustomAnswer)
    }

    private fun messages(t: T3, threadId: String): List<ThreadMessage> = t.messages[threadId].orEmpty().mapIndexed { i, m ->
        ThreadMessage("$threadId-$i", m.fromUser, m.text, m.at, m.streaming)
    }

    /** The Start menu's recent intents: the demo set's. */
    private val intents = listOf(StartIntent("i-spec", "Offline maps spec"), StartIntent("i-review", "Design review"), StartIntent("i-portuguese", "Portuguese"))

    /** Pairing in the lab: a scan finds nothing, and a link pairs at once. */
    val pairCalls = PairCalls(scan = { _, failed -> failed("The lab has no camera.") }, pair = { _, done -> done(null) })

    /** The taps' answers: each changes the drawn state as the app's call would on success. */
    fun actions(f: Fixture, get: () -> PanelUi, set: (PanelUi) -> Unit): PanelActions {
        fun lor(change: (LorUi) -> LorUi) = set(get().let { it.copy(lor = change(it.lor)) })
        fun home(change: (LorHome) -> LorHome) = lor { l -> l.copy(home = l.home?.let(change)) }
        return PanelActions(
            timer = { action, intentId ->
                home { h ->
                    val running = h.day.running
                    val next = when (action) {
                        "start" -> LorHome.Running(intents.firstOrNull { it.id == intentId }?.title ?: "Untitled", h.now, paused = false)
                        "pause" -> running?.copy(paused = true)
                        "resume" -> running?.copy(paused = false)
                        else -> null
                    }
                    h.copy(day = h.day.copy(running = next))
                }
            },
            recentIntents = { Loaded(intents) },
            task = { id, _, action, _, _, _ ->
                when (action) {
                    TaskWrite.DONE -> {
                        lor { it.copy(done = it.done + (id to true)) }
                        home { h -> h.copy(needsYou = h.needsYou.filterNot { n -> n.id == id }) }
                    }
                    TaskWrite.REOPEN -> lor { it.copy(done = it.done + (id to false)) }
                    TaskWrite.SEEN -> home { h -> h.copy(needsYou = h.needsYou.filterNot { n -> n.id == id }) }
                    TaskWrite.UNQUEUE -> home { h -> h.copy(agentQueue = h.agentQueue.filterNot { t -> t.id == id }) }
                    TaskWrite.QUEUE -> {}
                }
            },
            approve = { card, _ -> card.request?.let { r -> set(get().let { it.copy(t3 = it.t3.copy(answered = it.t3.answered + r.requestId)) }) } },
            answer = { card, _ -> card.request?.let { r -> set(get().let { it.copy(t3 = it.t3.copy(answered = it.t3.answered + r.requestId)) }) } },
            questions = { request, live -> if (live) Loaded(questions(f.t3, request.requestId)) else null },
            choose = { id -> set(get().copy(chosen = id)) },
            readThread = { thread, live -> if (live || f.t3.messages.containsKey(thread.id)) ThreadRead(thread.id, messages(f.t3, thread.id), null) else null },
            editReply = { id, text -> set(get().let { it.copy(replies = it.replies.copy(drafts = it.replies.drafts + (id to text))) }) },
            sendReply = { id -> set(get().let { it.copy(replies = RepliesUi(it.replies.drafts - id)) }) },
            pair = pairCalls,
            tab = { set(get().copy(tab = it)) },
        )
    }

    /** The set-up card on a first run: the home role from the phone's permissions, T3's pairing, and LoR's read of `/device/home`. */
    fun setup(f: Fixture): SetupUi {
        val home = f.lor.home
        return SetupUi(
            home = f.phone.permissions["home"] == true,
            paired = link(f.t3.status).let { it != T3Link.NOT_PAIRED && it != T3Link.REJECTED },
            lor = when (home.load) {
                Load.OK -> LorReach.Reached
                Load.LOADING -> LorReach.Checking
                Load.STALE, Load.FAILED -> LorReach.Failed(home.error ?: "")
            },
            where = f.settings.serverUrl.substringAfter("://").substringBefore('/').ifEmpty { f.settings.serverUrl },
        )
    }

    /** The sheet as the state opens it: the projects of its threads, and the assistant's screenshot when `where.target` is "assist". */
    fun sheet(f: Fixture): SheetUi {
        val threads = f.t3.threads
        val projects = (listOf("assistant") + threads.map { it.project }).distinct().associateBy { "p-$it" }
        return SheetUi(
            link = link(f.t3.status),
            loaded = f.t3.status == T3Status.LIVE || threads.isNotEmpty(),
            projects = projects,
            threads = threads.associate { it.id to "p-${it.project}" },
            shot = if (f.where.target == "assist") Shot(screenshot(), done = true, appLabel = "Chrome") else null,
            initial = f.where.typed,
        )
    }

    /** A stand-in screenshot: a phone screen in the app's colours, 90 by 200. */
    private fun screenshot(): ImageBitmap {
        val image = ImageBitmap(90, 200)
        val canvas = Canvas(image)
        canvas.drawRect(0f, 0f, 90f, 200f, Paint().apply { color = PanelFace })
        canvas.drawRect(0f, 0f, 90f, 18f, Paint().apply { color = Edge })
        for (i in 0 until 6) canvas.drawRect(8f, 30f + i * 26f, 82f, 44f + i * 26f, Paint().apply { color = if (i == 2) Or else Edge })
        return image
    }
}
