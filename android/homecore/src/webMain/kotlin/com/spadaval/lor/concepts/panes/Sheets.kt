package com.spadaval.lor.concepts.panes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.T3Card
import com.spadaval.lor.concepts.kit.AppIcon
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.KeyboardHeight
import com.spadaval.lor.concepts.kit.MenuItem
import com.spadaval.lor.concepts.kit.StandInKeyboard
import com.spadaval.lor.home.BackHandler
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.labAnchor
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import com.spadaval.lor.concepts.kit.Sheet as KitSheet

/**
 * What floats over every page, one at a time, and the stand-in keyboard over all of it while a
 * field has focus: the timer, quick send, settings, a folder, and (on the cover) an app's pane
 * from search. The pages behind blur while one is up ([Backdrop] in [PanesPhone]).
 */
@Composable
fun Sheets(store: ConceptStore, ui: PanesUi, wide: Boolean) {
    val focusManager = LocalFocusManager.current
    val sheet = ui.sheet
    var shown by remember { mutableStateOf(sheet) }
    if (sheet != null) shown = sheet
    val close = { focusManager.clearFocus(); ui.sheet = null; ui.switching = false }
    LaunchedEffect(sheet) { if (sheet == null) focusManager.clearFocus() }
    Box(Modifier.fillMaxSize().padding(bottom = if (ui.keyboard) KeyboardHeight else 0.dp)) {
        when (val s = shown) {
            Sheet.Timer -> KitSheet(sheet == Sheet.Timer, close, "sheet:timer", detents = listOf(if (store.timer != null && !ui.switching) 0.42f else 0.55f)) { TimerSheet(store, ui) }
            Sheet.QuickSend -> KitSheet(sheet == Sheet.QuickSend, { close(); ui.sent = null }, "sheet:quick", detents = listOf(0.52f)) { QuickSend(store, ui) }
            // On the cover, Back (or the scrim) from a section returns to the section list first.
            is Sheet.Settings -> KitSheet(sheet is Sheet.Settings, { if (!wide && s.section != null && ui.focused == null) ui.sheet = Sheet.Settings(null) else close() }, "sheet:settings", detents = listOf(0.93f), maxWidth = 820.dp) { Settings(store, ui, s.section, wide) }
            is Sheet.Folder -> KitSheet(sheet is Sheet.Folder, close, "sheet:folder", detents = listOf(0.5f)) { Folder(store, s.name, close) }
            null -> {}
        }
        val app = ui.appSheet
        var appShown by remember { mutableStateOf(app) }
        if (app != null) appShown = app
        appShown?.let(store::app)?.let { a ->
            KitSheet(app != null && !wide, { ui.appSheet = null }, "sheet:app", detents = listOf(0.6f)) {
                AppPane(store, ui, a, { ui.later(ui.scope) { shell.closeDrawer() } }, inSheet = true)
            }
        }
    }
    ui.focused?.let { f ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            StandInKeyboard(f, Modifier.labAnchor("keyboard"), onEnter = { ui.onEnter?.invoke() })
        }
        BackHandler(true) { focusManager.clearFocus() }
    }
}

@Composable
fun SheetTitle(title: String, detail: String? = null) {
    Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = P.s3)) {
        BasicText(title, style = P.head)
        if (detail != null) BasicText(detail, style = P.small)
    }
}

/**
 * The one timer (the brief's third decision): running, what it counts, its readout, Stop and
 * Switch; idle or switching, the labels to start under. "No label" is the one name for a timer
 * without one, and it can be named later.
 */
@Composable
private fun TimerSheet(store: ConceptStore, ui: PanesUi) {
    val now = liveNow(store)
    val running = store.timer
    val other = ui.field("timer:other")
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp).verticalScroll(rememberScrollState()).labAnchor("timer")) {
        if (!store.timerOk()) {
            Note("LoR isn't answering", "The timer can't start or stop until it does.", P.red, Glyph.Warning)
            Spacer(Modifier.height(P.s4))
        }
        if (running != null && !ui.switching) {
            Kicker("Running since ${LocalClockTime(store, running.start)}", P.amber)
            Spacer(Modifier.height(P.s2))
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(running.label ?: "No label", Modifier.weight(1f), style = P.head, maxLines = 2, overflow = TextOverflow.Ellipsis)
                BasicText(readout(now - running.start), style = P.title.copy(color = P.amber), modifier = Modifier.labAnchor("timer:readout"))
            }
            Spacer(Modifier.height(P.s5))
            Row(horizontalArrangement = Arrangement.spacedBy(P.s2)) {
                PButton("Stop", { if (ui.saves()) { store.stop(); ui.sheet = null } }, Modifier.weight(1f), kind = Kind.PRIMARY, glyph = Glyph.Stop, anchor = "timer:stop", enabled = store.timerOk())
                PButton("Switch to…", { ui.switching = true }, Modifier.weight(1f), glyph = Glyph.Swap, anchor = "timer:switch", enabled = store.timerOk())
            }
        } else {
            val switching = ui.switching && running != null
            BasicText(if (switching) "Switch to" else "Start a timer", style = P.head)
            BasicText(if (switching) "${running?.label ?: "No label"} ends now; the new one starts now." else "Pick what it counts toward, or start with no label and name it later.", style = P.small)
            Spacer(Modifier.height(P.s4))
            val go = { label: String? ->
                if (ui.saves()) { if (switching) store.switch(label) else store.start(label) }
                ui.switching = false; ui.sheet = null
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(P.s2), verticalArrangement = Arrangement.spacedBy(P.s2)) {
                store.labels.filter { it != running?.label }.forEach { l -> Pill(l, selected = false, onClick = { go(l) }, anchor = "label:$l", enabled = store.timerOk()) }
                Pill("No label", selected = false, onClick = { go(null) }, anchor = "label:none", enabled = store.timerOk())
            }
            Spacer(Modifier.height(P.s4))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Field(ui, other, "Something else", anchor = "timer:field", onEnter = { other.text.toString().trim().ifEmpty { null }?.let { go(it); other.edit { replace(0, length, "") } } }) }
                Spacer(Modifier.width(P.s2))
                PButton(if (switching) "Switch" else "Start", { go(other.text.toString().trim().ifEmpty { null }); other.edit { replace(0, length, "") } }, kind = Kind.PRIMARY, glyph = Glyph.Play, enabled = store.timerOk(), anchor = "timer:go")
            }
            if (switching) { Spacer(Modifier.height(P.s3)); PButton("Keep ${running?.label ?: "the running one"}", { ui.switching = false }, kind = Kind.QUIET) }
        }
    }
}

private fun LocalClockTime(store: ConceptStore, at: kotlin.time.Instant) = com.spadaval.lor.ui.LocalClock.time(at, store.zone)

/**
 * Quick send (a double tap on home, or "ask" from search): words to the agent, the project, Send.
 * It says when it went, and when it didn't, why, keeping the words.
 */
@Composable
private fun QuickSend(store: ConceptStore, ui: PanesUi) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp).labAnchor("quick")) {
        BasicText("Send to the agent", style = P.head)
        BasicText("It starts a thread in the project you pick.", style = P.small)
        Spacer(Modifier.height(P.s4))
        Field(ui, ui.quick, "What should it do?", anchor = "quick:field", lines = 4)
        Spacer(Modifier.height(P.s3))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(P.s2)) {
            store.projects.forEach { p -> Pill(p, selected = p == ui.project, onClick = { ui.project = p }, anchor = "project:$p") }
        }
        Spacer(Modifier.height(P.s4))
        val send = {
            ui.sent = store.quickSend(ui.quick.text.toString(), ui.project)
            if (ui.sent is ConceptStore.SendResult.Sent) ui.quick.edit { replace(0, length, "") }
            ui.sentFor = ui.quick.text.toString()
        }
        when (val r = ui.sent?.takeIf { ui.quick.text.toString() == ui.sentFor }) {
            is ConceptStore.SendResult.Sent -> Note("Sent to ${r.project}", "The agent starts a thread; it shows on the agent page.", P.cyan, Glyph.Check, anchor = "quick:sent")
            is ConceptStore.SendResult.Failed -> Note("Not sent", "${r.why} Your words are kept here.", P.red, Glyph.Warning, action = "Try again", onAction = send, anchor = "quick:failed")
            null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (store.t3Live) P.green else P.red))
                Spacer(Modifier.width(8.dp))
                BasicText(if (store.t3Live) "T3 is connected" else "T3 isn't connected", Modifier.weight(1f), style = P.meta.copy(color = if (store.t3Live) P.text3 else P.red))
                PButton("Send", send, kind = Kind.PRIMARY, glyph = Glyph.Send, enabled = ui.quick.text.isNotBlank(), anchor = "quick:send")
            }
        }
    }
}

private val SECTIONS = listOf("sync" to "This phone's sync", "connections" to "Connections", "home" to "Home", "sensors" to "Sensors")

/**
 * The one settings surface (the brief's third decision), shaped like every other page: its
 * sections as a list, and the chosen one beside it unfolded. On the cover the section slides over
 * the list as a second pane, and the list steps back and blurs behind it; Back returns to the
 * list. A failed sync is first, because it is the one that makes LoR untrue.
 */
@Composable
private fun Settings(store: ConceptStore, ui: PanesUi, section: String?, wide: Boolean) {
    val pick = { s: String? -> ui.sheet = Sheet.Settings(s) }
    if (wide) {
        Row(Modifier.fillMaxSize().labAnchor("settings")) {
            Column(Modifier.width(250.dp).fillMaxHeight().padding(start = 12.dp)) {
                SheetTitle("Settings", "LoR on this phone")
                SECTIONS.forEach { (id, name) -> SectionRow(store, id, name, (section ?: "sync") == id) { pick(id) } }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(P.line))
            Box(Modifier.weight(1f).fillMaxHeight()) { Section(store, ui, section ?: "sync", wide = true) }
        }
        return
    }
    val front by animateFloatAsState(if (section != null) 1f else 0f, tween(220))
    Box(Modifier.fillMaxSize().labAnchor("settings")) {
        Column(
            Modifier.fillMaxSize().graphicsLayer {
                val s = 1f - 0.04f * front
                scaleX = s; scaleY = s
                val r = 5.dp.toPx() * front
                renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
                alpha = 1f - 0.4f * front
            },
        ) {
            SheetTitle("Settings", "LoR on this phone")
            Column(Modifier.padding(horizontal = 12.dp)) { SECTIONS.forEach { (id, name) -> SectionRow(store, id, name, false) { pick(id) } } }
        }
        AnimatedContent(
            section, Modifier.fillMaxSize(),
            transitionSpec = { (slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally(tween(180)) { it / 3 } + fadeOut(tween(180))) },
        ) { s ->
            if (s != null) Column(Modifier.fillMaxSize().clip(P.rLg).background(P.sheet).border(1.dp, P.line2, P.rLg).labAnchor("settings:pane")) {
                Row(Modifier.padding(start = 6.dp, top = 4.dp)) { PButton("Settings", { pick(null) }, kind = Kind.QUIET, glyph = Glyph.Back, anchor = "settings:back", compact = true) }
                Section(store, ui, s, wide = false)
            }
        }
    }
}

/** A section in the list, with its state on the right, so a failed sync is seen before it is opened. */
@Composable
private fun SectionRow(store: ConceptStore, id: String, name: String, selected: Boolean, onClick: () -> Unit) {
    val (state, bad) = when (id) {
        "sync" -> if (store.syncFailed) "Failed" to true else "Synced" to false
        "connections" -> if (!store.lorLive || store.t3Status != T3Status.LIVE) "Needs a look" to true else "LoR and T3" to false
        "sensors" -> store.missing.size.let { if (it > 0) "$it need permission" to true else "${store.sensors.count { s -> s.on }} on" to false }
        else -> "${store.icons.size} on home" to false
    }
    Row(
        Modifier.paneRow(selected, "settings:$id", onClick).heightIn(min = P.row).padding(start = 12.dp, end = 8.dp).labAnchor("section:$id"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(when (id) { "sync" -> Glyph.Sync; "connections" -> Glyph.Phone; "sensors" -> Glyph.Wifi; else -> Glyph.Grid }, if (bad) P.red else P.text2, size = 18.dp)
        Spacer(Modifier.width(14.dp))
        BasicText(name, Modifier.weight(1f), style = P.body)
        BasicText(state, style = P.meta.copy(color = if (bad) P.red else P.text3))
        Spacer(Modifier.width(6.dp))
        Icon(Glyph.Chevron, P.text3, size = 14.dp)
    }
}

@Composable
private fun Section(store: ConceptStore, ui: PanesUi, id: String, wide: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = if (wide) 6.dp else 4.dp, bottom = 24.dp).labAnchor("settings:$id")) {
        BasicText(SECTIONS.first { it.first == id }.second, style = P.head)
        Spacer(Modifier.height(P.s3))
        when (id) {
            "sync" -> { SyncRows(store); Spacer(Modifier.height(P.s4)); SensorRows(store, missingOnly = true) }
            "connections" -> Group {
                Row2("LoR", store.fixture.settings.serverUrl.ifEmpty { "No address set" }, if (store.lorLive) "Reached" else store.homeError ?: "Not reached", if (store.lorLive) P.text3 else P.red)
                Hairline()
                val t3 = when (store.t3Status) {
                    T3Status.LIVE -> "Paired · live"; T3Status.RETRYING -> "Paired · reconnecting"; T3Status.NOT_PAIRED -> "Not paired"
                    T3Status.REJECTED -> "Refused"; else -> "Connecting…"
                }
                Row2("T3", store.fixture.t3.server.ifEmpty { "No server" }, t3, if (store.t3Status == T3Status.LIVE) P.text3 else P.red, action = if (store.t3Status == T3Status.NOT_PAIRED || store.t3Status == T3Status.REJECTED) "Pair" else null) {
                    store.toast("Pairing opens the camera to scan T3's code")
                }
            }
            "home" -> Group {
                var labels by remember { mutableStateOf(true) }
                Row2("Icon pack", null, store.fixture.settings.iconPack ?: "Apps' own")
                Hairline()
                Toggle("App labels", "Names under home's icons", labels, "toggle:labels") { labels = it }
                Hairline()
                Row2("Hidden apps", null, "${store.hidden.size}")
            }
            else -> SensorRows(store, missingOnly = false)
        }
    }
}

@Composable
private fun SyncRows(store: ConceptStore) {
    val sync = store.sync
    val failed = store.syncFailed
    Group {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).labAnchor(if (failed) "sync:failed" else "sync:ok"), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (failed) P.red else P.green))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(if (failed) "Last sync failed" else "Synced", style = P.rowTitle)
                val at = sync.lastAt?.let(::parseOrNull)
                BasicText(if (failed) "${sync.pending} readings waiting · tried ${ago(at, store.now)}" else "${ago(at, store.now).ifEmpty { "never" }} · ${sync.uploaded} sent", style = P.meta)
            }
            PButton("Sync now", { store.syncNow() }, glyph = Glyph.Sync, compact = true, anchor = "sync:now")
        }
        sync.errors.forEach { e -> BasicText(e, Modifier.padding(start = 22.dp, bottom = 10.dp), style = P.meta.copy(color = P.red)) }
    }
}

@Composable
private fun SensorRows(store: ConceptStore, missingOnly: Boolean) {
    val missing = store.missing
    if (missingOnly) {
        if (missing.isEmpty()) return
        BasicText("What the sync needs", style = P.group)
        Spacer(Modifier.height(P.s2))
        Group {
            missing.forEachIndexed { i, s ->
                if (i > 0) Hairline()
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Glyph.Warning, P.amber, size = 18.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(s.key.replaceFirstChar { it.uppercase() }, style = P.body)
                        BasicText(s.status ?: "Needs permission", style = P.meta)
                    }
                    PButton(s.action ?: "Allow", { store.allow(s.key) }, kind = Kind.PRIMARY, compact = true, anchor = "allow:${s.key}")
                }
            }
        }
        return
    }
    Group {
        store.sensors.forEachIndexed { i, s ->
            if (i > 0) Hairline()
            Toggle(s.key.replaceFirstChar { it.uppercase() }, s.status ?: if (s.allowed) null else "Needs permission", s.on, "sensor:${s.key}", detailTone = if (!s.allowed) P.amber else P.text2) { store.toggleSensor(s.key) }
        }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(P.rMd).background(P.raise).border(1.dp, P.line, P.rMd).padding(horizontal = 16.dp), content = content)
}

@Composable
private fun Row2(title: String, detail: String?, value: String, valueColor: Color = P.text2, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = P.body)
            if (detail != null) BasicText(detail, style = P.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BasicText(value, Modifier.padding(start = 12.dp).widthIn(max = 200.dp), style = P.meta.copy(color = valueColor), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (action != null) { Spacer(Modifier.width(8.dp)); PButton(action, onAction, kind = Kind.PRIMARY, compact = true) }
    }
}

@Composable
private fun Folder(store: ConceptStore, name: String, close: () -> Unit) {
    val folder = store.icons.firstOrNull { it is ConceptStore.Icon.Folder && it.name == name } as? ConceptStore.Icon.Folder ?: return
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).labAnchor("folder:$name")) {
        SheetTitle(name, "${folder.apps.size} apps")
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 4) {
            folder.apps.forEach { a ->
                Column(Modifier.weight(1f).clip(P.rMd).clickable { store.launch(a.label); close() }.padding(vertical = 10.dp).labAnchor("folder-app:${a.label}"), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(a, 52.dp)
                    Spacer(Modifier.height(6.dp))
                    BasicText(a.label, style = P.meta.copy(color = P.text1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The menu a long press or a visible ⋯ opened. */
@Composable
fun MenuFor(store: ConceptStore, ui: PanesUi, menu: Menu) {
    val close = { ui.menu = null }
    when (menu) {
        is Menu.App -> {
            val app = store.app(menu.ref)
            val folder = store.icons.firstOrNull { it.key == menu.ref } as? ConceptStore.Icon.Folder
            val label = app?.label ?: folder?.name ?: "App"
            val shortcuts = app?.let { a -> store.fixture.search.shortcuts.filter { it.pkg == a.pkg }.take(2) }.orEmpty()
            com.spadaval.lor.concepts.kit.Menu(
                menu.at,
                header = { MenuHeader(label, if (app != null) "Hold and drag to move it" else "Folder · ${folder?.apps?.size ?: 0} apps") },
                items = listOfNotNull(if (app != null) MenuItem("Open", Glyph.Chevron) { close(); store.launch(label) } else null) +
                    shortcuts.map { s -> MenuItem(s.label, if (s.conversation) Glyph.Person else Glyph.Chevron) { close(); store.launch("$label: ${s.label}") } } +
                    listOf(
                        MenuItem("App info", Glyph.Settings) { close(); store.toast("Opens Android's page for $label") },
                        MenuItem("Remove from home", Glyph.Close, danger = true) { close(); store.toast("Removed $label from home") },
                    ),
                onDismiss = close,
                danger = P.red,
            )
        }
        is Menu.Row -> {
            val entries = buildList {
                val agent = store.agentRow(menu.key)
                val today = if (agent == null) store.todayRow(menu.key) else null
                add(MenuItem("Open", Glyph.Chevron) { close(); if (agent != null || (menu.key.startsWith("flag:") && ui.shell.pager.currentPage == PanesUi.AGENT)) ui.openAgent = menu.key else ui.openToday = menu.key })
                when (agent) {
                    is AgentRow.Card -> {
                        add(MenuItem("Open in T3", Glyph.Agent) { close(); store.launch("T3 Code: ${agent.card.thread.title}") })
                        if (agent.card.kind == T3Card.Kind.FAILED) add(MenuItem("Dismiss", Glyph.Close) { close(); store.dismiss(agent.card) })
                    }
                    is AgentRow.Thread -> add(MenuItem("Open in T3", Glyph.Agent) { close(); store.launch("T3 Code: ${agent.thread.title}") })
                    null -> {}
                }
                val (id, title) = when (today) { is TodayRow.Flag -> today.need.id to today.need.title; is TodayRow.Task -> today.task.id to today.task.title; else -> null to null }
                if (id != null && title != null) {
                    add(MenuItem("Start a timer on it", Glyph.Play, enabled = store.timerOk()) { close(); if (ui.saves()) { if (store.timer != null) store.switch(title) else store.start(title) } })
                    add(MenuItem("Due tomorrow", Glyph.Calendar, enabled = store.lorLive) { close(); if (ui.saves()) store.setDue(id, store.today.plus(DatePeriod(days = 1))) })
                }
            }
            com.spadaval.lor.concepts.kit.Menu(menu.at, entries, onDismiss = close, danger = P.red)
        }
        is Menu.Page -> com.spadaval.lor.concepts.kit.Menu(
            menu.at,
            header = { MenuHeader(if (menu.page == PanesUi.AGENT) "The agent" else "Today", "What isn't needed every day") },
            items = if (menu.page == PanesUi.AGENT) listOf(
                MenuItem("Start a thread in a project", Glyph.Agent) { close(); ui.sheet = Sheet.QuickSend },
                MenuItem("Pairing with T3", Glyph.Phone) { close(); ui.sheet = Sheet.Settings("connections") },
                MenuItem("Open T3 Code", Glyph.Chevron) { close(); store.launch("T3 Code") },
            ) else listOf(
                MenuItem("Done today (${store.done.size})", Glyph.Check) { close(); store.toast("Opens Today in LoR") },
                MenuItem("This phone's sync", Glyph.Sync) { close(); ui.sheet = Sheet.Settings("sync") },
                MenuItem("Settings", Glyph.Settings) { close(); ui.sheet = Sheet.Settings(null) },
            ),
            onDismiss = close,
        )
    }
}

@Composable
private fun MenuHeader(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)) {
        BasicText(title, style = P.small.copy(color = P.text1, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(detail, style = P.meta.copy(color = P.text3))
    }
    Hairline(Modifier.padding(bottom = 4.dp))
}
