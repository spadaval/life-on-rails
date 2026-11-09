package com.spadaval.lor.home

import androidx.compose.ui.graphics.RectangleShape
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import com.spadaval.lor.ui.selection
import com.spadaval.lor.ui.DismissColors
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Well
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spadaval.lor.LorApp
import com.spadaval.lor.MainActivity
import com.spadaval.lor.ui.LorTheme
import kotlinx.coroutines.launch

/**
 * Home settings (launcher polish 1.3): the launcher's own settings, opened from the home menu and
 * the drawer's "Hidden apps" row. It starts in home's task, so back returns to home. It reads and
 * writes through [HomeStore], as home does. Later increments add their settings here, not to the
 * LoR tabs.
 */
class HomeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val store = (application as LorApp).home
        setContent {
            val apps by store.apps.collectAsStateWithLifecycle()
            val layout by store.layout.collectAsStateWithLifecycle()
            val packIndex by store.packs.index.collectAsStateWithLifecycle()
            val appsLoad by store.appsLoad.collectAsStateWithLifecycle()
            LorTheme {
                ProvideIcons(store.icons, layout.shown?.look, packIndex, drawSize = 40.dp) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize()) {
                            HomeSettings(apps, layout, appsLoad == AppsLoad.LOADED, ::edit, ::restore, ::open)
                            UndoBar(undo, ::undo, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(bottom = 16.dp))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as LorApp).home.resume()
    }

    private val undo = UndoBarState()

    /** Applies [change] to `layout.json`; the Undo bar says [done] on success, a refusal says why (launcher polish 4.2). */
    private fun edit(done: String?, change: (LayoutFile) -> LayoutFile) {
        lifecycleScope.launch {
            (application as LorApp).home.edit(change = change)
                .onSuccess { if (it.undoable) undo.show(done ?: "Home changed.") }
                .onFailure { say("Can't: ${it.message}.") }
        }
    }

    private fun undo() {
        lifecycleScope.launch {
            (application as LorApp).home.undo().onFailure { say("Can't: ${it.message}.") }
        }
    }

    private fun restore() {
        lifecycleScope.launch {
            (application as LorApp).home.restore()
                .onSuccess { say("layout.json is put right. The broken one is layout.json.broken.") }
                .onFailure { say("Can't: ${it.message}.") }
        }
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: RuntimeException) {
            fail("That screen can't be opened.")
        }
    }

    /** A refusal or a plain notice, in the bar without Undo (launcher polish 6.2: no toasts). */
    private fun say(text: String) = undo.say(text, failed = false)

    /** Something that failed: the bar with 3.6's coral dot. */
    private fun fail(text: String) = undo.say(text, failed = true)

    companion object {
        fun open(context: Context) = context.startActivity(Intent(context, HomeSettingsActivity::class.java))
    }
}

/** A hidden entry: the installed app it names, if any (the first activity, for a package name). */
private class Hidden(val ref: AppRef, val app: HomeApp?) {
    val label: String get() = app?.label ?: ref.toString()
}

@Composable
private fun HomeSettings(
    apps: List<HomeApp>,
    layout: LayoutState,
    appsLoaded: Boolean,
    edit: (String?, (LayoutFile) -> LayoutFile) -> Unit,
    restore: () -> Unit,
    open: (Intent) -> Unit,
) {
    val file = (layout as? LayoutState.Ready)?.file
    val hidden = remember(apps, file) {
        file?.hidden.orEmpty().map { ref ->
            Hidden(ref, apps.firstOrNull { ref.matches(it.id) })
        }
    }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding()) {
        item {
            Text("HOME SETTINGS", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp, 24.dp, 20.dp, 8.dp))
        }
        // The minimap (lor-6get): home comes to the front with it open. It is the way in when the grid
        // holds no stack, whose home menu is the other.
        item {
            val context = LocalContext.current
            Link("Panels", "Arrange home's grid: add, move and remove panels, and lock it") { HomeActivity.openMinimap(context) }
        }
        // A broken file: its banner and the restore in place of the sections, whose edits would fail (launcher polish 3.1).
        if (layout is LayoutState.Broken) item {
            BrokenBanner(layout, appsLoaded, MaterialTheme.typography.bodyMedium, Modifier.padding(horizontal = 20.dp), restore)
        }
        if (layout !is LayoutState.Broken) item { Section("Hidden apps") }
        when {
            layout is LayoutState.Broken -> {}
            file == null -> item { Note("Loading…") }
            hidden.isEmpty() -> item { Note("None. Hide an app from its long-press menu.") }
            else -> items(hidden, key = { it.ref.toString() }) { h ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (h.app != null) AppIcon(h.app, 40.dp) else Spacer(Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(h.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { edit("${h.label} is back in the drawer.") { it.unhide(h.ref.packageName, h.ref.work) } }) { Text("Unhide") }
                }
            }
        }
        if (file != null) {
            item { Section("Drawer tabs") }
            val tabs = file.drawer.tabs
            // Keyed by name (unique, ignoring case), so a row's open dialog stays with its tab.
            items(tabs.size, key = { "tab:" + tabs[it].name.lowercase() }) { i ->
                TabRow(tabs[i], i, tabs.size, edit)
            }
            if (tabs.size < Drawer.MAX_TABS) item {
                var naming by remember { mutableStateOf(false) }
                TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { naming = true }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("New tab") }
                if (naming) NameDialog("New tab", "") { name ->
                    naming = false
                    if (name != null) edit("$name is a tab, before All.") { it.newTab(name) }
                }
            }
        }
        if (file != null) {
            item { Section("Look") }
            // A change applies to the look the file holds when written, so two quick taps both land (6 review, R3).
            item { LookRows(file.look) { done, change -> edit(done) { it.copy(look = change(it.look)) } } }
        }
        // Android's notification access is the switch; it doesn't depend on the file (launcher polish 6.6).
        item { DotsRow(open) }
        item { Section("Search") }
        if (file != null) {
            item {
                val on = file.drawer.keyboard
                Toggle("Open the drawer with the keyboard up", "Ready to type a search", on) {
                    edit(if (on) "The drawer opens without the keyboard." else "The drawer opens with the keyboard up.") {
                        it.copy(drawer = it.drawer.copy(keyboard = !on))
                    }
                }
            }
        }
        // The permission doesn't depend on the file, so it shows while the file loads or is broken.
        item { ContactsRow(open) }
        item { HorizontalDivider(Modifier.padding(top = 12.dp)) }
        item {
            Link("Default home app", "Choose which app is home") { open(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
        item {
            val context = LocalContext.current
            Link("LoR app settings", "Timer, sensors, server and T3") { open(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
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
 * (denied for good), it opens LoR's App info instead. Search itself never asks.
 */
@Composable
private fun ContactsRow(open: (Intent) -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    var on by remember { mutableStateOf(granted()) }
    // After a denial: whether Android would still ask (null before any answer this time).
    var asksAgain by remember { mutableStateOf<Boolean?>(null) }
    LifecycleResumeEffect(Unit) {
        on = granted()
        onPauseOrDispose {}
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        on = ok
        asksAgain = if (ok) null else ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
    }
    val about = when {
        on -> "On: search finds contacts by name or number"
        asksAgain == false -> "Off: Android won't ask again. Allow Contacts in App info, Permissions"
        asksAgain == true -> "Off: denied. Allow asks again"
        else -> "Off: search shows no contacts"
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Contacts in search", style = MaterialTheme.typography.bodyLarge)
            Text(about, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!on) TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = {
            if (asksAgain == false) {
                open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            } else {
                ask.launch(Manifest.permission.READ_CONTACTS)
            }
        }) { Text(if (asksAgain == false) "App info" else "Allow") }
    }
}

/** Home's look (launcher polish 6.4): columns per class of screen, labels, label and icon size. */
@Composable
private fun LookRows(look: HomeLook, set: (String, (HomeLook) -> HomeLook) -> Unit) {
    val here = if (LocalConfiguration.current.screenWidthDp >= HomeLook.WIDE_DP) "wide" else "narrow"
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
        IconPackRow(look.iconPack) { set(if (it == null) "Apps draw their own icons." else "Icons come from the pack.") { l -> l.copy(iconPack = it) } }
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
private fun DotsRow(open: (Intent) -> Unit) {
    val context = LocalContext.current
    var on by remember { mutableStateOf(Dots.granted(context)) }
    LifecycleResumeEffect(Unit) {
        on = Dots.granted(context)
        onPauseOrDispose {}
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Notification dots", style = MaterialTheme.typography.bodyLarge)
            Text(
                if (on) "On: an app with a notification has a dot" else "Off: allow LoR under Notification access",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = {
            open(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, Dots.component(context).flattenToString()),
            )
        }) { Text(if (on) "Settings" else "Allow") }
    }
}

/** The icon pack: None, then each installed pack (launcher polish 6.5), read when shown. */
@Composable
private fun IconPackRow(chosen: String?, pick: (String?) -> Unit) {
    val context = LocalContext.current
    val packs by produceState<List<InstalledPack>?>(null) { value = withContext(Dispatchers.IO) { installedPacks(context.packageManager) } }
    val list = packs
    val names = listOf("None") + list.orEmpty().map { it.label }
    val at = if (chosen == null) 0 else list?.indexOfFirst { it.packageName == chosen }?.takeIf { it >= 0 }?.plus(1) ?: -1
    val about = when {
        list == null -> "Looking for packs…"
        chosen != null && at < 0 -> "$chosen isn't installed; apps draw their own icons"
        list.isEmpty() -> "No icon packs are installed (ADW or Nova packs)"
        else -> "ADW and Nova packs; apps a pack lacks keep their own icon"
    }
    Options("Icon pack", about, names, at) { i -> pick(if (i == 0) null else list!![i - 1].packageName) }
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
private fun TabRow(t: DrawerTab, i: Int, count: Int, edit: (String?, (LayoutFile) -> LayoutFile) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val list = t.kind == TabKind.LIST
    val about = when (t.kind) {
        TabKind.QUICK -> "Quick dial, always first"
        TabKind.ALL -> "Every app, A–Z, always last"
        TabKind.LIST -> if (t.apps.size == 1) "1 app" else "${t.apps.size} apps"
    }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { TabGlyph(t, MaterialTheme.colorScheme.onSurface) }
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
                if (list && i > 1) DropdownMenuItem(text = { Text("Move left") }, onClick = { menu = false; edit(null) { it.moveTab(t.name, -1) } })
                if (list && i < count - 2) DropdownMenuItem(text = { Text("Move right") }, onClick = { menu = false; edit(null) { it.moveTab(t.name, 1) } })
                if (list) DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; edit("${t.name} is deleted; its apps are still in All.") { it.deleteTab(t.name) } })
            }
        }
    }
    if (renaming) NameDialog("Rename ${t.name}", t.name) { name ->
        renaming = false
        if (name != null && name != t.name) edit(null) { it.renameTab(t.name, name) }
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
                                    if (!on) edit(null) { it.setTabIcon(t.name, icon) }
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
