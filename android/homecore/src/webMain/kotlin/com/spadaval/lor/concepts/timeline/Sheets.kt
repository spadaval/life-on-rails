package com.spadaval.lor.concepts.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.concepts.kit.AppIcon
import com.spadaval.lor.concepts.kit.ConceptStore
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.concepts.kit.KeyboardHeight
import com.spadaval.lor.concepts.kit.Sheet as KitSheet
import com.spadaval.lor.concepts.kit.StandInKeyboard
import com.spadaval.lor.home.BackHandler
import com.spadaval.lor.lab.Load
import com.spadaval.lor.lab.T3Status
import com.spadaval.lor.lab.labAnchor
import com.spadaval.lor.ui.LocalClock

/**
 * Every sheet, and the stand-in keyboard over all of it while a field has focus. One sheet is open
 * at a time ([TimelineUi.sheet]); it sits above the keyboard while the keyboard is up.
 */
@Composable
fun Sheets(store: ConceptStore, ui: TimelineUi, wide: Boolean) {
    val focusManager = LocalFocusManager.current
    val sheet = ui.sheet
    // Remembered so a sheet leaving still draws what it showed.
    var shown by remember { mutableStateOf(sheet) }
    if (sheet != null) shown = sheet
    val close = { focusManager.clearFocus(); ui.sheet = null }
    LaunchedEffect(sheet) { if (sheet == null) focusManager.clearFocus() }
    // An item cleared while open (approved, Done, Seen) closes its sheet; an open thread, or a
    // task opened from search, stays while it exists.
    val gone = (sheet as? Sheet.Item)?.key?.let { k -> !k.startsWith("thread:") && store.item(k) == null } == true
    LaunchedEffect(sheet, gone) { if (gone) ui.sheet = null }
    Box(Modifier.fillMaxSize().padding(bottom = if (ui.keyboard) KeyboardHeight else 0.dp)) {
        when (val s = shown) {
            is Sheet.Item -> KitSheet(sheet is Sheet.Item && !wide, close, "sheet:item", detents = listOf(0.7f, 0.94f)) {
                Detail(store, ui, s.key, pane = false)
            }
            is Sheet.Block -> KitSheet(sheet is Sheet.Block, { ui.selected = null; close() }, "sheet:block", detents = listOf(0.62f, 0.94f)) { BlockSheet(store, ui, s.id) }
            is Sheet.Plan -> KitSheet(sheet is Sheet.Plan, close, "sheet:plan", detents = listOf(0.6f, 0.94f)) { PlanSheet(store, ui, s) }
            Sheet.Waiting -> KitSheet(sheet == Sheet.Waiting && !wide, close, "sheet:waiting", detents = listOf(0.7f, 0.94f)) { WaitingSheet(store, ui) }
            Sheet.Untimed -> KitSheet(sheet == Sheet.Untimed && !wide, close, "sheet:untimed", detents = listOf(0.62f, 0.94f)) { UntimedSheet(store, ui) }
            Sheet.Timer -> KitSheet(sheet == Sheet.Timer, close, "sheet:timer", detents = listOf(if (store.timer != null) 0.42f else 0.5f)) { TimerSheet(store, ui) }
            Sheet.QuickSend -> KitSheet(sheet == Sheet.QuickSend, { close(); ui.sent = null }, "sheet:quick", detents = listOf(0.5f)) { QuickSend(store, ui) }
            is Sheet.Settings -> KitSheet(sheet is Sheet.Settings, close, "sheet:settings", detents = listOf(0.94f)) { Settings(store, ui, s.section) }
            is Sheet.Folder -> KitSheet(sheet is Sheet.Folder, close, "sheet:folder", detents = listOf(0.5f)) { Folder(store, s.name, close) }
            null -> {}
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
fun SheetTitle(title: String, detail: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = T.s3), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = T.title.copy(fontWeight = FontWeight.SemiBold))
            if (detail != null) BasicText(detail, style = T.label)
        }
        trailing?.invoke()
    }
}

/**
 * The one timer (the brief's third decision): running, its readout, Stop and Switch; idle, the
 * labels to start under. "No label" is the one name for a timer without one.
 */
@Composable
private fun TimerSheet(store: ConceptStore, ui: TimelineUi) {
    val now = liveNow(store)
    val running = store.timer
    var switching by remember { mutableStateOf(false) }
    val other = ui.field("timer:other")
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).labAnchor("timer")) {
        if (!store.timerReachable) {
            Note("LoR isn't answering", "The timer can't start or stop until it does. ${store.homeError.orEmpty()}", T.red, Glyph.Warning)
            Spacer(Modifier.height(T.s4))
        }
        if (running != null && !switching) {
            Kicker("Running since ${LocalClock.time(running.start, store.zone)}", T.amber)
            Spacer(Modifier.height(T.s2))
            BasicText(running.label ?: "No label", style = T.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(T.s2))
            BasicText(readout(now - running.start), style = T.readout.copy(fontSize = 56.sp, lineHeight = 60.sp), modifier = Modifier.labAnchor("timer:readout").labAnchor("timer:at:${(now - running.start).inWholeSeconds}"))
            Spacer(Modifier.height(T.s5))
            Row(horizontalArrangement = Arrangement.spacedBy(T.s2)) {
                TButton("Stop", { store.stop(); ui.sheet = null }, kind = Kind.PRIMARY, glyph = Glyph.Stop, anchor = "timer:stop", enabled = store.timerReachable)
                TButton("Switch to…", { switching = true }, glyph = Glyph.Swap, anchor = "timer:switch", enabled = store.timerReachable)
            }
        } else {
            BasicText(if (switching) "Switch to" else "Start a timer", style = T.title.copy(fontWeight = FontWeight.SemiBold))
            BasicText(if (switching) "The running block ends now; the new one starts now." else "Pick what it counts toward, or start with no label and name it later.", style = T.label)
            Spacer(Modifier.height(T.s4))
            val go = { label: String? -> if (switching) store.switch(label) else store.start(label); switching = false; ui.sheet = null }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(T.s2), verticalArrangement = Arrangement.spacedBy(T.s2)) {
                store.labels.filter { it != running?.label }.forEach { l -> Chip(l, selected = false, onClick = { go(l) }, anchor = "label:$l", enabled = store.timerReachable) }
                Chip("No label", selected = false, onClick = { go(null) }, anchor = "label:none", enabled = store.timerReachable)
            }
            Spacer(Modifier.height(T.s4))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Field(ui, other, "Something else", anchor = "timer:field", onEnter = { other.text.toString().trim().ifEmpty { null }?.let { go(it); other.edit { replace(0, length, "") } } }) }
                Spacer(Modifier.width(T.s2))
                TButton(if (switching) "Switch" else "Start", { go(other.text.toString().trim().ifEmpty { null }); other.edit { replace(0, length, "") } }, kind = Kind.PRIMARY, glyph = Glyph.Play, enabled = store.timerReachable, anchor = "timer:go")
            }
            if (switching) { Spacer(Modifier.height(T.s3)); TButton("Keep the running one", { switching = false }, kind = Kind.QUIET) }
        }
    }
}

/**
 * Quick send (a double tap on home, or "ask" from search): words to the agent, the project they go
 * to, and Send. It says when it went, and when it didn't, why, keeping the words.
 */
@Composable
private fun QuickSend(store: ConceptStore, ui: TimelineUi) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).labAnchor("quick")) {
        BasicText("Send to the agent", style = T.title.copy(fontWeight = FontWeight.SemiBold))
        BasicText("It starts a thread in the project you pick.", style = T.label)
        Spacer(Modifier.height(T.s4))
        Field(ui, ui.quick, "What should it do?", anchor = "quick:field", lines = 4)
        Spacer(Modifier.height(T.s3))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.s2)) {
            store.projects.forEach { p -> Chip(p, selected = p == ui.project, onClick = { ui.project = p }, anchor = "project:$p") }
        }
        Spacer(Modifier.height(T.s4))
        when (val r = ui.sent) {
            is ConceptStore.SendResult.Sent -> Note("Sent to ${r.project}", "The agent starts a thread; it shows on the day page if it needs you.", T.cyan, Glyph.Check, anchor = "quick:sent")
            is ConceptStore.SendResult.Failed -> Note("Not sent", "${r.why} Your words are kept here.", T.red, Glyph.Warning, action = "Try again", onAction = {
                ui.sent = store.quickSend(ui.quick.text.toString(), ui.project)
            }, anchor = "quick:failed")
            null -> Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(if (store.t3Status == T3Status.LIVE) "T3 is connected" else "T3 isn't connected", Modifier.weight(1f), style = T.label.copy(color = if (store.t3Live) T.text3 else T.red))
                TButton("Send", {
                    ui.sent = store.quickSend(ui.quick.text.toString(), ui.project)
                    if (ui.sent is ConceptStore.SendResult.Sent) ui.quick.edit { replace(0, length, "") }
                }, kind = Kind.PRIMARY, glyph = Glyph.Send, enabled = ui.quick.text.isNotBlank(), anchor = "quick:send")
            }
        }
    }
}

/**
 * The one settings surface (the brief's third decision), in one style: this phone's sync and what
 * it lacks first, because a failed sync is the one that makes LoR untrue; then the connections,
 * home, sensors. [section] scrolls to where it was opened for.
 */
@Composable
private fun Settings(store: ConceptStore, ui: TimelineUi, section: String?) {
    val list = store.list("settings")
    // The list's items, in order, by name, so a section is found by its name, not a counted index.
    val order = listOf("title", "sync", "connections", "home", "sensors")
    LaunchedEffect(section) {
        // Every opening goes where it was opened for; the sync (and the top) by default.
        val name = when (section) { "connections", "server address", "t3 pairing", "t3" -> "connections"; "home" -> "home"; "sensors" -> "sensors"; "sync" -> "sync"; else -> "title" }
        list.scrollToItem(order.indexOf(name))
    }
    LazyColumn(Modifier.fillMaxSize().labAnchor("settings"), state = list, contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "title") { SheetTitle("Settings", "LoR on this phone") }
        item(key = "sync") { Group("This phone's sync") { SyncRows(store) } }
        item(key = "connections") {
            Group("Connections") {
                Row2("LoR", store.fixture.settings.serverUrl.ifEmpty { "No address set" }, if (store.lorLive) "Reached" else store.homeError ?: "Not reached", if (store.lorLive) T.text3 else T.red)
                Hairline()
                val t3 = when (store.t3Status) {
                    T3Status.LIVE -> "Paired · live"; T3Status.RETRYING -> "Paired · reconnecting"; T3Status.NOT_PAIRED -> "Not paired"
                    T3Status.REJECTED -> "Refused: ${store.t3Error}"; else -> "Connecting…"
                }
                Row2("T3", store.fixture.t3.server.ifEmpty { "No server" }, t3, if (store.t3Status == T3Status.LIVE) T.text3 else T.red, action = if (store.t3Status == T3Status.NOT_PAIRED || store.t3Status == T3Status.REJECTED) "Pair" else null) {
                    store.toast("Pairing opens the camera to scan T3's code")
                }
            }
        }
        item(key = "home") {
            Group("Home") {
                var labels by remember { mutableStateOf(true) }
                Row2("Icon pack", null, store.fixture.settings.iconPack ?: "Apps' own")
                Hairline()
                Toggle("App labels", "Names under home's icons", labels, "toggle:labels") { labels = it }
                Hairline()
                Row2("Hidden apps", null, "${store.hidden.size}")
            }
        }
        item(key = "sensors") {
            Group("Sensors") {
                store.sensors.forEachIndexed { i, s ->
                    if (i > 0) Hairline()
                    Toggle(s.key.replaceFirstChar { it.uppercase() }, s.status ?: if (s.allowed) null else "Needs permission", s.on, "sensor:${s.key}") { store.toggleSensor(s.key) }
                }
            }
        }
    }
}

@Composable
private fun SyncRows(store: ConceptStore) {
    val sync = store.sync
    val failed = store.syncFailed
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).labAnchor(if (failed) "sync:failed" else "sync:ok"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (failed) T.red else Color(0xFF5BBF7A)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(if (failed) "Last sync failed" else "Synced", style = T.body.copy(fontWeight = FontWeight.Medium))
            val at = sync.lastAt?.let(::parseOrNull)
            BasicText(
                if (failed) "${sync.pending} readings waiting · tried ${ago(at, store.now)}" else "${ago(at, store.now).ifEmpty { "never" }} · ${sync.uploaded} sent",
                style = T.label,
            )
        }
        TButton("Sync now", { store.syncNow() }, glyph = Glyph.Sync, height = 40.dp, anchor = "sync:now")
    }
    sync.errors.forEach { e -> BasicText(e, Modifier.padding(start = 22.dp, bottom = 6.dp), style = T.label.copy(color = T.red)) }
    store.missing.forEach { s ->
        Hairline()
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Warning, T.amber, size = 18.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(s.key.replaceFirstChar { it.uppercase() }, style = T.body)
                BasicText(s.status ?: "Needs permission", style = T.label)
            }
            TButton(s.action ?: "Allow", { store.allow(s.key) }, kind = Kind.PRIMARY, height = 40.dp, anchor = "allow:${s.key}")
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = T.gutter)) {
        SectionLabel(title)
        Column(Modifier.fillMaxWidth().clip(T.rLg).background(T.surface1.copy(alpha = 0.8f)).border(1.dp, T.line, T.rLg).padding(horizontal = 16.dp)) { content() }
    }
}

@Composable
private fun Row2(title: String, detail: String?, value: String, valueColor: Color = T.text2, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = T.body)
            if (detail != null) BasicText(detail, style = T.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BasicText(value, Modifier.padding(start = 12.dp), style = T.label.copy(color = valueColor), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (action != null) { Spacer(Modifier.width(8.dp)); TButton(action, onAction, kind = Kind.PRIMARY, height = 40.dp) }
    }
}

/** A switch: a pill that slides, the platform's control drawn in Timeline's colours. */
@Composable
private fun Toggle(title: String, detail: String?, on: Boolean, anchor: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.heightIn(min = 56.dp).padding(vertical = 8.dp).labAnchor(anchor), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = T.body)
            if (detail != null) BasicText(detail, style = T.label.copy(color = if (detail.startsWith("Needs")) T.amber else T.text2))
        }
        val x by androidx.compose.animation.core.animateDpAsState(if (on) 22.dp else 2.dp, androidx.compose.animation.core.tween(150))
        Box(Modifier.size(48.dp, 28.dp).clip(T.rPill).background(if (on) T.text1 else T.wash2)) {
            Box(Modifier.padding(start = x, top = 2.dp).size(24.dp).clip(CircleShape).background(if (on) T.ground else T.text2))
        }
    }
}

@Composable
private fun Folder(store: ConceptStore, name: String, close: () -> Unit) {
    val folder = store.icons.firstOrNull { it is ConceptStore.Icon.Folder && it.name == name } as? ConceptStore.Icon.Folder ?: return
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).labAnchor("folder:$name")) {
        SheetTitle(name, "${folder.apps.size} apps")
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 4) {
            folder.apps.forEach { a ->
                Column(Modifier.weight(1f).clip(T.rMd).clickable { store.launch(a.label); close() }.padding(vertical = 10.dp).labAnchor("folder-app:${a.label}"), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(a, 52.dp)
                    Spacer(Modifier.height(6.dp))
                    BasicText(a.label, style = T.label.copy(color = T.text1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
