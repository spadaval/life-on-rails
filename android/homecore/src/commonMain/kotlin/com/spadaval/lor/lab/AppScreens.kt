@file:OptIn(ExperimentalTime::class)

package com.spadaval.lor.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spadaval.lor.data.Sensor
import com.spadaval.lor.data.syncSummary
import com.spadaval.lor.home.BrokenUi
import com.spadaval.lor.home.ContactsUi
import com.spadaval.lor.home.HiddenUi
import com.spadaval.lor.home.HomeLook
import com.spadaval.lor.home.HomeSettings
import com.spadaval.lor.home.HomeSettingsActions
import com.spadaval.lor.home.HomeSettingsUi
import com.spadaval.lor.home.IconPackUi
import com.spadaval.lor.home.SettingsFileUi
import com.spadaval.lor.home.TabKind
import com.spadaval.lor.home.TabUi
import com.spadaval.lor.home.tabLetter
import com.spadaval.lor.ui.AppShell
import com.spadaval.lor.ui.AppTabs
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.LiveUpdatesUi
import com.spadaval.lor.ui.LorTheme
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.RunningTimer
import com.spadaval.lor.ui.SensorAccess
import com.spadaval.lor.ui.SensorsActions
import com.spadaval.lor.ui.SensorsTab
import com.spadaval.lor.ui.SensorsUi
import com.spadaval.lor.ui.SettingsActions
import com.spadaval.lor.ui.SettingsTab
import com.spadaval.lor.ui.SettingsUi
import com.spadaval.lor.ui.SyncUi
import com.spadaval.lor.ui.T3DebugActions
import com.spadaval.lor.ui.T3DebugTab
import com.spadaval.lor.ui.T3DebugUi
import com.spadaval.lor.ui.T3OptionUi
import com.spadaval.lor.ui.T3QuestionUi
import com.spadaval.lor.ui.T3RequestUi
import com.spadaval.lor.ui.T3ThreadUi
import com.spadaval.lor.ui.TimerActions
import com.spadaval.lor.ui.TimerReply
import com.spadaval.lor.ui.TimerTab
import com.spadaval.lor.ui.TimerUi
import com.spadaval.lor.ui.Type
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.spadaval.lor.ui.TimerIntent as TabIntent

/*
 * The app's own screens in the lab: the LoR app on a tab (`where.open` APP_TAB, `where.target`
 * timer, sensors, settings or t3) and Home settings (HOME_SETTINGS), drawn by the same functions
 * the app calls, from the fixture's parts. Taps change only what the screen itself keeps (typed
 * text, the open tab or thread); nothing is sent anywhere.
 *
 * What a part has no words for, the app's are written here: the T3 connection's and threads'
 * states, and a broken file's lines. Home settings reads the few keys of `layout.json` it shows
 * (hidden, drawer, look) here too, until the common reader of the file (lor-xdoi) replaces it.
 */

/** The surface [scene] opens on, or false when it is one this file doesn't draw. */
@Composable
fun AppScreen(scene: LabScene): Boolean {
    when (scene.fixture.where.open) {
        Opened.APP_TAB -> LabApp(scene)
        Opened.HOME_SETTINGS -> LabHomeSettings(scene)
        else -> return false
    }
    return true
}

private fun ms(iso: String?): Long = iso?.let { Instant.parse(it).toEpochMilliseconds() } ?: 0

/** The LoR app: the tab shell on the fixture's tab, which taps change. */
@Composable
private fun LabApp(scene: LabScene) {
    val f = scene.fixture
    val target = f.where.target
    var tab by remember { mutableIntStateOf(if (target == "t3") AppTabs.SETTINGS else tabs.indexOf(target).takeIf { it >= 0 } ?: AppTabs.TIMER) }
    var t3 by remember { mutableStateOf(target == "t3") }
    LorTheme {
        AppShell(tab, { tab = it }, isHome = f.phone.permissions["home"] ?: true, setHome = {}) {
            when (it) {
                AppTabs.TIMER -> LabTimer(f)
                AppTabs.SENSORS -> LabSensors(f)
                AppTabs.SETTINGS -> if (t3) LabT3(f) { t3 = false } else LabSettings(f) { t3 = true }
                else -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("The dashboard: a WebView of LoR's web app, which the lab doesn't draw.", style = Type.bodySmall)
                }
            }
        }
    }
}

private val tabs = listOf("timer", "sensors", "settings", "dashboard")

@Composable
private fun LabTimer(f: Fixture) {
    val t = f.timer
    var title by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val now = ms(f.clock.now)
    fun intents(list: List<TimerIntent>) = list.map { TabIntent(it.id, it.title, it.type) }
    val reply = when (t.state) {
        TimerState.UNREACHABLE -> null
        TimerState.IDLE -> TimerReply(t.login, null, intents(t.recent), intents(t.intents))
        TimerState.RUNNING, TimerState.PAUSED -> TimerReply(
            t.login,
            RunningTimer(t.intent, t.title, t.start?.let(::ms) ?: now, if (t.state == TimerState.PAUSED) ms(t.pausedAt ?: f.clock.now) else null),
            intents(t.recent),
            intents(t.intents),
        )
    }
    TimerTab(
        TimerUi(reply, if (t.state == TimerState.UNREACHABLE) "Cannot reach LoR: ${t.error}" else null, title = title, showAll = showAll, now = now),
        TimerActions(title = { title = it }, showAll = { showAll = !showAll }),
    )
}

@Composable
private fun LabSensors(f: Fixture) {
    val p = f.phone
    val rows = p.sensors.mapNotNull { row -> Sensor.entries.firstOrNull { it.key == row.key }?.let { it to row } }.toMap()
    val on = remember { mutableStateMapOf<Sensor, Boolean>().apply { putAll(rows.mapValues { it.value.on }) } }
    val collected = p.sync.collected.mapNotNull { (k, n) -> Sensor.entries.firstOrNull { it.key == k }?.let { it to n } }.toMap()
    val at = ms(p.sync.lastAt)
    SensorsTab(
        SensorsUi(
            SyncUi(
                lastSyncAt = at,
                summary = if (p.sync.lastAt == null) "Never synced." else syncSummary(collected, p.sync.uploaded, p.sync.pending),
                uploadedTotal = p.sync.uploaded.toLong(),
                lastUploadAt = if (p.sync.uploaded > 0) at else 0,
                pending = p.sync.pending,
                errors = p.sync.errors.joinToString("\n"),
            ),
            on = on,
            access = rows.mapValues { (_, r) ->
                SensorAccess(r.allowed, r.status ?: if (r.allowed) "Allowed." else "Needs permission.", r.action ?: if (r.allowed) null else "Allow")
            },
            now = ms(f.clock.now),
        ),
        SensorsActions(turn = { s, v -> on[s] = v }),
    )
}

@Composable
private fun LabSettings(f: Fixture, openT3: () -> Unit) {
    val s = f.settings
    var server by remember { mutableStateOf(s.serverUrl) }
    var device by remember { mutableStateOf(s.deviceName) }
    val perms = f.phone.permissions
    SettingsTab(
        SettingsUi(server, device, s.result, liveUpdates = LiveUpdatesUi(perms["notifications"] ?: true, perms["live-updates"] ?: true), version = "(lab)"),
        SettingsActions(server = { server = it }, device = { device = it }, openT3 = openT3),
    )
}

/** The connection's state as the debug view's Connection card says it (the app's T3DebugScreen). */
private fun statusLine(t3: T3): String = when (t3.status) {
    T3Status.NOT_PAIRED -> "Not paired."
    T3Status.STOPPED -> "Not connected."
    T3Status.CONNECTING -> "Connecting to ${t3.server}…"
    T3Status.LIVE -> "Live: ${t3.server}"
    T3Status.RETRYING -> "Disconnected (${t3.error}). Retrying in 30s."
    T3Status.REJECTED -> "The server refused this phone (${t3.error}). Pair again."
}

/** A thread's state in words (the app's `ThreadState.label`). */
private val ThreadState.label: String
    get() = when (this) {
        ThreadState.WAITING_APPROVAL -> "Waiting for approval"
        ThreadState.WAITING_ANSWER -> "Waiting for an answer"
        ThreadState.RUNNING -> "Running"
        ThreadState.FAILED -> "Failed"
        ThreadState.IDLE -> "Idle"
    }

@Composable
private fun LabT3(f: Fixture, back: () -> Unit) {
    val t3 = f.t3
    var server by remember { mutableStateOf(t3.server) }
    var code by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(t3.threads.firstOrNull { it.requests.isNotEmpty() }?.id) }
    T3DebugTab(
        T3DebugUi(
            server = server,
            code = code,
            status = statusLine(t3),
            live = t3.status == T3Status.LIVE,
            threads = t3.threads.map { t ->
                T3ThreadUi(t.id, t.title, t.project, t.state.label, t.lastError?.takeIf { t.state == ThreadState.FAILED }, t.requests.firstOrNull()?.let { T3RequestUi(it.id, it.kind) })
            },
            selected = selected,
            projects = t3.projects,
        ),
        T3DebugActions(
            back = back,
            server = { server = it },
            code = { code = it },
            select = { selected = it },
            questions = { r ->
                Result.success(t3.questions[r.id].orEmpty().map { q -> T3QuestionUi(q.id, q.question, q.options.map { T3OptionUi(it.label) }, q.multiSelect, q.allowCustomAnswer) })
            },
            defaults = { "Defaults: no model set on the server, full-access." },
        ),
    )
}

// ---- Home settings ----

@Composable
private fun LabHomeSettings(scene: LabScene) {
    val f = scene.fixture
    val read = remember(f) { readSettings(f) }
    LorTheme {
        Surface(Modifier.fillMaxSize()) {
            HomeSettings(
                HomeSettingsUi(
                    file = read.file,
                    broken = read.broken,
                    wide = scene.widthDp >= HomeLook.WIDE_DP,
                    packs = f.settings.iconPacks.map { IconPackUi(packOf(it), it) },
                    dots = f.phone.permissions["notifications"] ?: true,
                    contacts = ContactsUi(on = f.phone.permissions["contacts"] ?: true),
                ),
                HomeSettingsActions(icon = { Placeholder(it.label) }),
            )
        }
    }
}

/** A lab icon pack's package name, made from its label. */
private fun packOf(label: String) = "lab.pack." + label.lowercase().filter { it.isLetterOrDigit() }

/** A hidden app's icon in the lab: its first letter in a square. */
@Composable
private fun Placeholder(label: String) {
    Box(Modifier.size(40.dp).background(Raised).border(1.dp, Edge), contentAlignment = Alignment.Center) {
        Text(tabLetter(label), style = Type.title)
    }
}

internal class SettingsRead(val file: SettingsFileUi?, val broken: BrokenUi?)

/**
 * The parts of the fixture's `layout.json` Home settings shows. No file is a first run's, which
 * the app starts with the default tabs. A version other than 2 is the broken file, shown over the
 * last good one, with the parser's words for it.
 */
internal fun readSettings(f: Fixture): SettingsRead {
    val layout = f.layout ?: return SettingsRead(SettingsFileUi(tabs = DEFAULT_TABS), null)
    val version = layout["version"]?.jsonPrimitive?.intOrNull
    if (version != 2) {
        return SettingsRead(
            null,
            BrokenUi(
                line = "layout.json has an error. Showing the last good layout.",
                message = "version: must be 2 (or 1, which is converted), got ${version ?: "nothing"}",
                action = "Use the last good layout",
            ),
        )
    }
    val hidden = (layout["hidden"] as? JsonArray).orEmpty().map { it.jsonPrimitive.content }.map { ref ->
        val work = ref.startsWith("work:")
        val name = ref.removePrefix("work:")
        val app = f.apps.list.firstOrNull { it.work == work && (it.ref.removePrefix("work:") == name || it.pkg == name) }
        HiddenUi(ref, app?.label ?: ref, name.substringBefore('/'), work, app != null)
    }
    val drawer = layout["drawer"] as? JsonObject
    val tabs = (drawer?.get("tabs") as? JsonArray)?.map { it as JsonObject }?.map { t ->
        val kind = when (t.text("kind")) { "quick" -> TabKind.QUICK; "all" -> TabKind.ALL; else -> TabKind.LIST }
        TabUi(t.text("name") ?: "", t.text("icon") ?: "", kind, (t["apps"] as? JsonArray)?.size ?: 0)
    } ?: DEFAULT_TABS
    val iconPack = f.settings.iconPack?.let(::packOf)
    return SettingsRead(
        SettingsFileUi(hidden, tabs, tabs.size < MAX_TABS, readLook(layout["look"] as? JsonObject).let { if (iconPack != null) it.copy(iconPack = iconPack) else it }, drawer?.bool("keyboard") ?: false),
        null,
    )
}

/** `look`, leniently: what is there and reads, else the default. */
private fun readLook(look: JsonObject?): HomeLook {
    look ?: return HomeLook()
    val d = HomeLook()
    val grid = look["grid"] as? JsonObject
    fun cols(name: String, def: HomeLook.Columns) = (grid?.get(name) as? JsonObject)?.let { c -> HomeLook.Columns(c.int("home") ?: def.home, c.int("drawer") ?: def.drawer) } ?: def
    return HomeLook(
        grid = HomeLook.Grid(cols("narrow", HomeLook.NARROW), cols("wide", HomeLook.WIDE)),
        labels = look.bool("labels") ?: d.labels,
        labelSize = look.int("labelSize") ?: d.labelSize,
        iconSize = look.int("iconSize") ?: d.iconSize,
        iconPack = look.text("iconPack"),
        themedIcons = look.bool("themedIcons") ?: d.themedIcons,
        iconShape = HomeLook.IconShape.entries.firstOrNull { it.id == look.text("iconShape") } ?: d.iconShape,
        header = HomeLook.Header.entries.firstOrNull { it.id == look.text("header") } ?: d.header,
    )
}

private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
private fun JsonObject.int(key: String) = (get(key) as? JsonPrimitive)?.intOrNull
private fun JsonObject.bool(key: String) = (get(key) as? JsonPrimitive)?.booleanOrNull

/** The app's `Drawer.DEFAULT_TABS` and `Drawer.MAX_TABS`. */
private val DEFAULT_TABS = listOf(TabUi("Quick dial", "bolt", TabKind.QUICK), TabUi("All", "all", TabKind.ALL))
private const val MAX_TABS = 8
