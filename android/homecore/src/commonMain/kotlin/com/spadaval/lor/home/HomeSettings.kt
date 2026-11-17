package com.spadaval.lor.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spadaval.lor.ui.DismissColors
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Well
import com.spadaval.lor.ui.selection

/*
 * Home settings (launcher polish 1.3): the launcher's own settings. This draws them from plain
 * state and reports taps; the app's HomeSettingsActivity reads `layout.json`, Android's
 * permissions and the installed icon packs, and turns each edit into a change of the file.
 */

/** A hidden entry: how the file names it ([key], [pkg], [work]), its label, and whether an installed app answers to it. */
data class HiddenUi(val key: String, val label: String, val pkg: String, val work: Boolean = false, val installed: Boolean = true)

/** A drawer tab as its row shows it: its name, icon, kind and, for a list tab, how many apps it holds. */
data class TabUi(val name: String, val icon: String, val kind: TabKind, val apps: Int = 0)

/** The parts of `layout.json` Home settings shows, when the file has been read and parses. */
data class SettingsFileUi(
    val hidden: List<HiddenUi> = emptyList(),
    val tabs: List<TabUi> = emptyList(),
    /** Whether there is room for one more tab (`Drawer.MAX_TABS`). */
    val canAddTab: Boolean = true,
    val look: HomeLook = HomeLook(),
    /** `drawer.keyboard`: the drawer opens with the keyboard up. */
    val keyboard: Boolean = false,
)

/** An installed icon pack: its package, which the look names, and its label. */
data class IconPackUi(val pkg: String, val label: String)

/** Contacts in search: whether Android allows it, and after a denial whether it would ask again (null before any answer). */
data class ContactsUi(val on: Boolean = false, val asksAgain: Boolean? = null)

/**
 * Home settings: the file ([file], null while it loads or while it is [broken]), whether this
 * screen is a [wide] one, the icon packs found (null while looking), and Android's notification
 * access ([dots]) and contacts permission.
 */
data class HomeSettingsUi(
    val file: SettingsFileUi? = null,
    val broken: BrokenUi? = null,
    val wide: Boolean = false,
    val packs: List<IconPackUi>? = null,
    val dots: Boolean = false,
    val contacts: ContactsUi = ContactsUi(),
)

/** A change Home settings asks of `layout.json`; the app applies it to the file. */
sealed interface SettingsEdit {
    data class Unhide(val pkg: String, val work: Boolean) : SettingsEdit
    data class NewTab(val name: String) : SettingsEdit
    data class RenameTab(val name: String, val to: String) : SettingsEdit
    data class SetTabIcon(val name: String, val icon: TabIcon) : SettingsEdit
    data class MoveTab(val name: String, val by: Int) : SettingsEdit
    data class DeleteTab(val name: String) : SettingsEdit
    data class Keyboard(val on: Boolean) : SettingsEdit
    /** A change of the look the file holds when written, so two quick taps both land (6 review, R3). */
    class Look(val change: (HomeLook) -> HomeLook) : SettingsEdit
}

/**
 * What Home settings' taps ask for. [edit] changes the file, with what the Undo bar says on
 * success (null for nothing). [contacts] asks Android, or opens App info once Android has
 * stopped asking. [icon] draws a hidden app's icon, 40 dp.
 */
class HomeSettingsActions(
    val edit: (String?, SettingsEdit) -> Unit = { _, _ -> },
    val restore: () -> Unit = {},
    val panels: () -> Unit = {},
    val dots: () -> Unit = {},
    val contacts: () -> Unit = {},
    val homeApp: () -> Unit = {},
    val lorApp: () -> Unit = {},
    val icon: @Composable (HiddenUi) -> Unit = {},
)

@Composable
fun HomeSettings(ui: HomeSettingsUi, on: HomeSettingsActions) {
    val file = ui.file
    val broken = ui.broken
    val edit = on.edit
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding()) {
        item {
            Text("HOME SETTINGS", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp, 24.dp, 20.dp, 8.dp))
        }
        // The minimap (lor-6get): home comes to the front with it open. It is the way in when the grid
        // holds no stack, whose home menu is the other.
        item {
            Link("Panels", "Arrange home's grid: add, move and remove panels, and lock it", on.panels)
        }
        // A broken file: its banner and the restore in place of the sections, whose edits would fail (launcher polish 3.1).
        if (broken != null) item {
            BrokenBanner(broken, MaterialTheme.typography.bodyMedium, Modifier.padding(horizontal = 20.dp), on.restore)
        }
        if (broken == null) item { Section("Hidden apps") }
        when {
            broken != null -> {}
            file == null -> item { Note("Loading…") }
            file.hidden.isEmpty() -> item { Note("None. Hide an app from its long-press menu.") }
            else -> items(file.hidden, key = { it.key }) { h ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (h.installed) on.icon(h) else Spacer(Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(h.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { edit("${h.label} is back in the drawer.", SettingsEdit.Unhide(h.pkg, h.work)) }) { Text("Unhide") }
                }
            }
        }
        if (file != null) {
            item { Section("Drawer tabs") }
            val tabs = file.tabs
            // Keyed by name (unique, ignoring case), so a row's open dialog stays with its tab.
            items(tabs.size, key = { "tab:" + tabs[it].name.lowercase() }) { i ->
                TabRow(tabs[i], i, tabs.size, edit)
            }
            if (file.canAddTab) item {
                var naming by remember { mutableStateOf(false) }
                TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { naming = true }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("New tab") }
                if (naming) NameDialog("New tab", "") { name ->
                    naming = false
                    if (name != null) edit("$name is a tab, before All.", SettingsEdit.NewTab(name))
                }
            }
        }
        if (file != null) {
            item { Section("Look") }
            item { LookRows(file.look, ui.wide, ui.packs) { done, change -> edit(done, SettingsEdit.Look(change)) } }
        }
        // Android's notification access is the switch; it doesn't depend on the file (launcher polish 6.6).
        item { DotsRow(ui.dots, on.dots) }
        item { Section("Search") }
        if (file != null) {
            item {
                val up = file.keyboard
                Toggle("Open the drawer with the keyboard up", "Ready to type a search", up) {
                    edit(if (up) "The drawer opens without the keyboard." else "The drawer opens with the keyboard up.", SettingsEdit.Keyboard(!up))
                }
            }
        }
        // The permission doesn't depend on the file, so it shows while the file loads or is broken.
        item { ContactsRow(ui.contacts, on.contacts) }
        item { HorizontalDivider(Modifier.padding(top = 12.dp)) }
        item {
            Link("Default home app", "Choose which app is home", on.homeApp)
        }
        item {
            Link("LoR app settings", "Timer, sensors, server and T3", on.lorApp)
        }
    }
}

@Composable
private fun Section(title: String) {
    // DESIGN.md: section heads in condensed caps, Dust.
    Text(
        title.uppercase(),
        style = com.spadaval.lor.ui.Type.title,
        modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp),
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

/**
 * Contacts in search (launcher polish 5.6): Allow asks Android; once Android has stopped asking
 * (denied for good), the button opens LoR's App info instead. Search itself never asks.
 */
@Composable
private fun ContactsRow(ui: ContactsUi, tap: () -> Unit) {
    val about = when {
        ui.on -> "On: search finds contacts by name or number"
        ui.asksAgain == false -> "Off: Android won't ask again. Allow Contacts in App info, Permissions"
        ui.asksAgain == true -> "Off: denied. Allow asks again"
        else -> "Off: search shows no contacts"
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Contacts in search", style = MaterialTheme.typography.bodyLarge)
            Text(about, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!ui.on) TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = tap) { Text(if (ui.asksAgain == false) "App info" else "Allow") }
    }
}

/** Home's look (launcher polish 6.4): columns per class of screen, labels, label and icon size. */
@Composable
private fun LookRows(look: HomeLook, wide: Boolean, packs: List<IconPackUi>?, set: (String, (HomeLook) -> HomeLook) -> Unit) {
    val here = if (wide) "wide" else "narrow"
    fun mark(cls: String) = if (cls == here) " (this screen)" else ""
    Column {
        Choices("Home grid, narrow screens${mark("narrow")}", "Columns under 600 dp wide", HomeLook.NARROW_HOME.toList(), look.grid.narrow.home) {
            set("Home has $it columns on narrow screens.") { l -> l.copy(grid = l.grid.copy(narrow = l.grid.narrow.copy(home = it))) }
        }
        Choices("Home grid, wide screens${mark("wide")}", "Columns from 600 dp wide", HomeLook.WIDE_HOME.toList(), look.grid.wide.home) {
            set("Home has $it columns on wide screens.") { l -> l.copy(grid = l.grid.copy(wide = l.grid.wide.copy(home = it))) }
        }
        Choices("Drawer grid, narrow screens${mark("narrow")}", "Columns under 600 dp wide", HomeLook.NARROW_DRAWER.toList(), look.grid.narrow.drawer) {
            set("The drawer has $it columns on narrow screens.") { l -> l.copy(grid = l.grid.copy(narrow = l.grid.narrow.copy(drawer = it))) }
        }
        Choices("Drawer grid, wide screens${mark("wide")}", "Columns from 600 dp wide", HomeLook.WIDE_DRAWER.toList(), look.grid.wide.drawer) {
            set("The drawer has $it columns on wide screens.") { l -> l.copy(grid = l.grid.copy(wide = l.grid.wide.copy(drawer = it))) }
        }
        Toggle("Labels on home", "Names under apps and folders on home; the drawer always has them", look.labels) {
            set(if (look.labels) "Home has no labels." else "Home has labels.") { l -> l.copy(labels = !look.labels) }
        }
        Choices("Label size", "In sp", HomeLook.LABEL_SIZES.toList(), look.labelSize) {
            set("Labels are $it sp.") { l -> l.copy(labelSize = it) }
        }
        Choices("Icon size", "In dp, on home, in folders and the drawer", HomeLook.ICON_SIZES.step(4).toList(), look.iconSize) {
            set("Icons are $it dp.") { l -> l.copy(iconSize = it) }
        }
        Options("Header", "Over home's first page; a tap opens the calendar", HomeLook.Header.entries.map { it.title }, look.header.ordinal) {
            set("The header shows ${HomeLook.Header.entries[it].title.lowercase()}.") { l -> l.copy(header = HomeLook.Header.entries[it]) }
        }
        IconPackRow(look.iconPack, packs) { set(if (it == null) "Apps draw their own icons." else "Icons come from the pack.") { l -> l.copy(iconPack = it) } }
        Toggle("Themed icons", "Apps with a monochrome icon draw it in LoR's colours", look.themedIcons) {
            set(if (look.themedIcons) "Themed icons are off." else "Themed icons are on.") { l -> l.copy(themedIcons = !look.themedIcons) }
        }
        Options("Icon shape", "For apps' own icons; a pack's keep theirs", HomeLook.IconShape.entries.map { it.title }, look.iconShape.ordinal) {
            set("Icons are ${HomeLook.IconShape.entries[it].title.lowercase()}.") { l -> l.copy(iconShape = HomeLook.IconShape.entries[it]) }
        }
    }
}

/** Notification dots: on with LoR's notification access, else a way to the system's page for it. */
@Composable
private fun DotsRow(on: Boolean, open: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Notification dots", style = MaterialTheme.typography.bodyLarge)
            Text(
                if (on) "On: an app with a notification has a dot" else "Off: allow LoR under Notification access",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = open) { Text(if (on) "Settings" else "Allow") }
    }
}

/** The icon pack: None, then each installed pack (launcher polish 6.5); [packs] is null while they are looked for. */
@Composable
private fun IconPackRow(chosen: String?, packs: List<IconPackUi>?, pick: (String?) -> Unit) {
    val list = packs
    val names = listOf("None") + list.orEmpty().map { it.label }
    val at = if (chosen == null) 0 else list?.indexOfFirst { it.pkg == chosen }?.takeIf { it >= 0 }?.plus(1) ?: -1
    val about = when {
        list == null -> "Looking for packs…"
        chosen != null && at < 0 -> "$chosen isn't installed; apps draw their own icons"
        list.isEmpty() -> "No icon packs are installed (ADW or Nova packs)"
        else -> "ADW and Nova packs; apps a pack lacks keep their own icon"
    }
    Options("Icon pack", about, names, at) { i -> pick(if (i == 0) null else list!![i - 1].pkg) }
}

/** A row of named choices, the chosen one in DESIGN.md's selection. */
@Composable
private fun Options(title: String, subtitle: String, options: List<String>, chosen: Int, pick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, name ->
                val on = i == chosen
                Box(
                    Modifier.heightIn(min = 48.dp)
                        .then(if (on) Modifier.selection() else Modifier.border(1.dp, MaterialTheme.colorScheme.outline))
                        .selectable(on, role = Role.RadioButton) { if (!on) pick(i) }
                        .semantics { contentDescription = "$title: $name" }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(name, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

/** A row of numbers to pick one from, the chosen one in DESIGN.md's selection. */
@Composable
private fun Choices(title: String, subtitle: String, options: List<Int>, chosen: Int, pick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { n ->
                val on = n == chosen
                Box(
                    Modifier.size(48.dp)
                        .then(if (on) Modifier.selection() else Modifier.border(1.dp, MaterialTheme.colorScheme.outline))
                        .selectable(on, role = Role.RadioButton) { if (!on) pick(n) }
                        .semantics { contentDescription = "$title: $n" },
                    contentAlignment = Alignment.Center,
                ) { Text("$n", style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

/** A setting that is on or off: the whole row toggles it, read as one switch. */
@Composable
private fun Toggle(title: String, subtitle: String, on: Boolean, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(on, role = Role.Switch, onValueChange = { toggle() }).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        // DESIGN.md: the off state reads (Dust edge and thumb on Well), on is amber.
        Switch(on, onCheckedChange = null, colors = androidx.compose.material3.SwitchDefaults.colors(uncheckedThumbColor = Mute, uncheckedBorderColor = Mute, uncheckedTrackColor = Well))
    }
}

@Composable
private fun Link(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One drawer tab, with its edits behind a menu (lor-iyc3): every tab can be renamed and given an
 * icon; a list tab also moves between quick dial and All, and can be deleted.
 */
@Composable
private fun TabRow(t: TabUi, i: Int, count: Int, edit: (String?, SettingsEdit) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val list = t.kind == TabKind.LIST
    val about = when (t.kind) {
        TabKind.QUICK -> "Quick dial, always first"
        TabKind.ALL -> "Every app, A–Z, always last"
        TabKind.LIST -> if (t.apps == 1) "1 app" else "${t.apps} apps"
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { TabGlyph(t.name, t.icon, MaterialTheme.colorScheme.onSurface) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = MaterialTheme.typography.bodyLarge)
            Text(about, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { menu = true }) { Text("Edit") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Rename…") }, onClick = { menu = false; renaming = true })
                DropdownMenuItem(text = { Text("Icon…") }, onClick = { menu = false; picking = true })
                // A list tab moves only between quick dial (first) and All (last).
                if (list && i > 1) DropdownMenuItem(text = { Text("Move left") }, onClick = { menu = false; edit(null, SettingsEdit.MoveTab(t.name, -1)) })
                if (list && i < count - 2) DropdownMenuItem(text = { Text("Move right") }, onClick = { menu = false; edit(null, SettingsEdit.MoveTab(t.name, 1)) })
                if (list) DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; edit("${t.name} is deleted; its apps are still in All.", SettingsEdit.DeleteTab(t.name)) })
            }
        }
    }
    if (renaming) NameDialog("Rename ${t.name}", t.name) { name ->
        renaming = false
        if (name != null && name != t.name) edit(null, SettingsEdit.RenameTab(t.name, name))
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { picking = false }) { Text("Cancel") } },
            title = { Text("${t.name}'s icon") },
            text = {
                // Two rows of four, so every icon shows at a phone's width.
                Column { TabIcon.entries.chunked(4).forEach { row -> Row {
                    row.forEach { icon ->
                        val on = icon.id == t.icon
                        Box(
                            Modifier.size(48.dp)
                                .then(if (on) Modifier.selection() else Modifier)
                                .semantics { contentDescription = icon.id }
                                .selectable(on, role = Role.RadioButton) {
                                    picking = false
                                    if (!on) edit(null, SettingsEdit.SetTabIcon(t.name, icon))
                                },
                            contentAlignment = Alignment.Center,
                        ) { IconGlyph(icon, MaterialTheme.colorScheme.onSurface) }
                    }
                } } }
            },
        )
    }
}

/** Asks for a tab's name; [done] gets it, or null when cancelled. */
@Composable
private fun NameDialog(title: String, initial: String, done: (String?) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { done(null) },
        confirmButton = { TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { done(text.trim()) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(shape = androidx.compose.ui.graphics.RectangleShape, colors = DismissColors, onClick = { done(null) }) { Text("Cancel") } },
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name") }) },
    )
}
