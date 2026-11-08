package com.spadaval.lor.home

import androidx.compose.ui.graphics.RectangleShape
import com.spadaval.lor.ui.*
import android.content.pm.ApplicationInfo
import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.collect.LaunchFrom
import kotlin.math.roundToInt

/*
 * The long-press menu on an app (launcher polish 1.2).
 *
 * After Launcher3 / Lawnchair (Apache-2.0): `PopupContainerWithArrow` opens by the icon with the
 * app's shortcuts first (`PopupPopulator`: manifest shortcuts before dynamic ones, then by rank,
 * a few at most; queried as `ShortcutRequest.PUBLISHED`, `FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST`),
 * then the system shortcuts (`SystemShortcut`: App info, Uninstall). And Kvaesitso (GPL-3.0,
 * `LauncherApp.kt`): App info through `LauncherApps.startAppDetailsActivity`, Uninstall as an
 * `ACTION_DELETE` intent. Here the popup is Material's `DropdownMenu`, which closes on a tap
 * elsewhere or back.
 *
 * The layout lock (lor-zzyy, decision lor-73li) follows Lawnchair (Apache-2.0, at aeb92c1):
 * `LauncherOptionsPopup.kt` toggles `lockHomeScreen` from the home menu and drops edit mode (and
 * Widgets) from it while locked, and `LawnchairShortcut.kt` drops an app's editing entries. Here
 * Uninstall stays, since it isn't a change to the layout.
 */

/** What the Undo bar says when a press would have edited a locked home (lor-zzyy). */
const val LOCKED_SAY = "Home is locked. Unlock it from the home menu."

/** The entries of an app's menu that edit `layout.json`, in the order drawn. */
enum class AppEdit { REMOVE_FROM_FOLDER, REMOVE_FROM_HOME, ADD_TO_HOME, REMOVE_FROM_DOCK, ADD_TO_DOCK, MOVE_TO_SECTION, HIDE }

/**
 * The edit entries an app at [places] shows. While [locked], only those [LayoutStore.edit] would
 * take: Move to section…, and Hide of an app on none of home, a folder and the dock (decision lor-73li).
 */
fun appEdits(places: Places, locked: Boolean): List<AppEdit> {
    val placed = places.home || places.folder != null || places.dock
    if (locked) return listOfNotNull(AppEdit.MOVE_TO_SECTION, AppEdit.HIDE.takeIf { !placed })
    return listOfNotNull(
        AppEdit.REMOVE_FROM_FOLDER.takeIf { places.folder != null },
        when {
            places.home -> AppEdit.REMOVE_FROM_HOME
            places.folder == null -> AppEdit.ADD_TO_HOME
            else -> null
        },
        if (places.dock) AppEdit.REMOVE_FROM_DOCK else AppEdit.ADD_TO_DOCK,
        AppEdit.MOVE_TO_SECTION,
        AppEdit.HIDE,
    )
}

/** Where an app was opened or long-pressed: the launch record's `from`, folder and query. */
data class At(val from: LaunchFrom, val folder: String? = null, val query: String? = null)

/** An app shortcut for the menu, with its icon. */
class MenuShortcut(val info: ShortcutInfo, val icon: ImageBitmap?)

/** What home does for its screens, implemented by [HomeActivity]. */
interface HomeActions {
    /** Opens [app], growing out of its [icon] (in the window) when one is on screen (launcher polish 7.4). */
    fun open(app: HomeApp, at: At, icon: IconSpot? = null)

    /** The app's shortcuts for its menu; empty unless LoR is the default home. */
    suspend fun shortcuts(app: HomeApp): List<MenuShortcut>
    fun openShortcut(app: HomeApp, shortcut: ShortcutInfo, at: At, icon: IconSpot? = null)

    /** A shortcut's icon for a search row, drawn off the main thread and cached; null when it has none. */
    suspend fun shortcutIcon(shortcut: ShortcutInfo): ImageBitmap?
    fun appInfo(app: HomeApp)
    fun uninstall(app: HomeApp)

    /** Home settings ([HomeSettingsActivity]). */
    fun settings()

    /** The system's wallpaper picker. */
    fun wallpaper()

    /** The calendar at today, from home's header (launcher polish 6.7). */
    fun calendar()

    /** The clock app's alarm list, from the clock block's alarm line (lor-leh4). */
    fun alarms()

    /**
     * Applies [change] to `layout.json`; a refusal shows as a toast. On success the Undo bar says
     * [done], or "Home changed." (launcher polish 4.2).
     */
    fun edit(done: String?, after: () -> Unit = {}, change: (LayoutFile) -> LayoutFile)

    /** Says why a drop was refused ("Can't: …"), as a refused edit does. */
    fun refused(reason: String)

    /** Takes back the last edit ([HomeStore.undo]). */
    fun undo()

    /** Writes the drawer's view: a view, not a change to home, so it shows no Undo and drops the pending one. */
    fun setView(view: DrawerView)

    /** Opens a settings page search found (launcher polish 5.3), recording a launch unless it is LoR's own. */
    fun openSetting(page: SettingsPage, query: String)

    /** Opens, dials or texts a contact search found (launcher polish 5.6), recording a launch. */
    fun openContact(contact: SearchContact, way: ContactWay, query: String)

    /** Empties the recent searches the empty search field offers (launcher polish 5.5). */
    fun clearSearches()

    /** Takes a search that matched no app somewhere else ([Elsewhere]): the web, a store, or quick send. */
    fun elsewhere(way: Elsewhere, query: String)

    /** Puts a broken `layout.json` right ([HomeStore.restore]); the outcome shows as a toast. */
    fun restore()

    /** Opens quick send, as a home press on home does (launcher polish 7.6: double tap on empty home). */
    fun quickSend()

    /**
     * Adds a widget of [provider] to the stack at [at] (lor-r0f9): an id bound through Android's
     * bind dialog when it asks, then the widget's own configure screen when it has one, then the
     * block with Undo. A cancel at either step deletes the id and changes nothing.
     */
    fun addWidget(at: Cell, provider: android.appwidget.AppWidgetProviderInfo)

    /** Opens the widget [id]'s configure screen again (its menu's Settings); a cancel changes nothing. */
    fun widgetSettings(id: Int)
}

/**
 * Where an icon is drawn (launcher polish 7.4): its coordinates are kept as laid out and read at the
 * tap, so a drawer or folder still moving reports where the icon is then.
 */
class IconSpot {
    private var at: LayoutCoordinates? = null

    /** The icon's bounds in the window, or null when it isn't on screen. */
    fun bounds(): android.graphics.Rect? = at?.takeIf { it.isAttached }?.boundsInWindow()?.takeIf { it.width > 0f && it.height > 0f }?.let {
        android.graphics.Rect(it.left.roundToInt(), it.top.roundToInt(), it.right.roundToInt(), it.bottom.roundToInt())
    }

    fun place(c: LayoutCoordinates) {
        at = c
    }
}

/** Reports where this icon is to [spot]. */
fun Modifier.iconSpot(spot: IconSpot): Modifier = onGloballyPositioned(spot::place)

/** What a screen of home needs to draw apps and their menus. */
class HomeContext(val actions: HomeActions, val file: LayoutFile?, val apps: List<HomeApp>, val ui: HomeUi) {
    val drag: DragState get() = ui.drag

    /** The main-profile apps in label order, as the edits resolve refs (launcher polish 4.1). */
    val installed: List<AppId> = apps.filter { it.mainProfile }.map { it.id }

    /** A new folder's name from the two apps' category. */
    fun nameFor(a: AppRef, b: AppRef): String {
        fun cat(r: AppRef) = r.resolve(installed)?.let { id -> apps.firstOrNull { it.mainProfile && it.id == id } }?.category
        return folderName(cat(a), cat(b))
    }

    /** How Add writes [app]: its package name when that is its one launchable activity. */
    fun refFor(app: HomeApp): AppRef =
        if (apps.count { it.mainProfile && it.packageName == app.packageName } == 1) AppRef(app.packageName)
        else AppRef(app.packageName, app.id.className)

    /** Home is locked (decision lor-73li): what would edit it is hidden or does nothing. */
    val locked: Boolean get() = file?.locked == true

    /** Says why a press did nothing on a locked home. */
    fun sayLocked() = ui.undo.say(LOCKED_SAY, failed = false)

    /** Hidden by the layout file: left out of the drawer and search. */
    fun hidden(app: HomeApp): Boolean = app.mainProfile && file?.isHidden(app.id) == true
}

/**
 * Draws [content] with a long press that opens [app]'s menu by it, with a haptic tick. [content]
 * gets the long-press handler to put on its own click target, so the app stays one node with
 * both actions for TalkBack and agent-device.
 */
@Composable
fun WithAppMenu(app: HomeApp, at: At, home: HomeContext, spot: From.At? = null, icon: IconSpot, content: @Composable (onLongClick: () -> Unit) -> Unit) {
    var open by rememberFloatingMenu(home.ui)::open
    val haptics = LocalHapticFeedback.current
    // Picking the app up closes its menu (launcher polish 4.3).
    val held = home.drag.held
    LaunchedEffect(held) { if (held != null) open = false }
    Box {
        content {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            open = true
        }
        if (open) AppMenu(app, at, home, spot, icon) { open = false }
    }
}


/**
 * A tile's menu, open while [HomeUi.menu] holds it, so a home press closes it with everything else
 * that floats over home (launcher polish 7.3, Increment 1's R5) and home isn't at rest under it.
 */
class FloatingMenu(private val ui: HomeUi) {
    var open: Boolean
        get() = ui.menu === this
        set(v) {
            if (v) ui.menu = this else if (ui.menu === this) ui.menu = null
        }
}

/** A [FloatingMenu] for this place in the composition; it closes when the tile leaves. */
@Composable
fun rememberFloatingMenu(ui: HomeUi): FloatingMenu {
    val menu = remember(ui) { FloatingMenu(ui) }
    DisposableEffect(menu) { onDispose { menu.open = false } }
    return menu
}

@Composable
private fun AppMenu(app: HomeApp, at: At, home: HomeContext, spot: From.At?, icon: IconSpot, close: () -> Unit) {
    var shortcuts by remember(app) { mutableStateOf<List<MenuShortcut>>(emptyList()) }
    LaunchedEffect(app) { shortcuts = home.actions.shortcuts(app) }
    val actions = home.actions
    val file = home.file
    val main = app.mainProfile && file != null
    val installed = home.installed
    val places = if (main) file!!.placesOf(app.id, installed) else null
    val removable = (app.info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    // "Move to section" turns the menu into the list of sections (launcher polish 2.3).
    var picking by remember(app) { mutableStateOf(false) }
    if (picking && file != null) {
        val current = file.drawer.sectionOf(app.id, app.category)
        DropdownMenu(true, close, containerColor = Raised, shape = androidx.compose.ui.graphics.RectangleShape) {
            file.drawer.allSections().forEach { s ->
                val here = s.name == current
                Item(if (here) "${s.name} ✓" else s.name) {
                    close()
                    if (!here) actions.edit("${app.label} is in ${s.name} now.") { it.moveToSection(app.id, home.refFor(app), app.category, s.name, installed) }
                }
            }
        }
        return
    }
    DropdownMenu(true, close, containerColor = Raised, shape = androidx.compose.ui.graphics.RectangleShape) {
        shortcuts.forEach { s ->
            Item(s.info.shortLabel?.toString() ?: s.info.longLabel?.toString() ?: "Shortcut", s.icon) {
                close(); actions.openShortcut(app, s.info, at, icon)
            }
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider(color = Line)
        Item("App info") { close(); actions.appInfo(app) }
        if (removable) Item("Uninstall") { close(); actions.uninstall(app) }
        if (places != null) {
            val ref = home.refFor(app)
            // Opened from a tile, the tile's own place is edited (and checked); otherwise by what the refs draw as.
            val here = spot?.spot
            appEdits(places, home.locked).forEach { e ->
                when (e) {
                    AppEdit.REMOVE_FROM_FOLDER -> Item("Remove from folder") {
                        close()
                        actions.edit("${app.label} is out of ${places.folder}.") { if (here is Spot.InFolder) it.removeAt(spot) else it.removeFromFolder(app.id, installed) }
                    }
                    AppEdit.REMOVE_FROM_HOME -> Item("Remove from home") {
                        close()
                        actions.edit("${app.label} is off home.") { if (here is Spot.Apps) it.removeAt(spot) else it.removeFromHome(app.id, installed) }
                    }
                    AppEdit.ADD_TO_HOME -> Item("Add to home") { close(); actions.edit("${app.label} is on home.") { it.addToHome(app.id, ref, installed) } }
                    AppEdit.REMOVE_FROM_DOCK -> Item("Remove from dock") {
                        close()
                        actions.edit("${app.label} is out of the dock.") { if (here is Spot.Dock) it.removeAt(spot) else it.removeFromDock(app.id, installed) }
                    }
                    AppEdit.ADD_TO_DOCK -> Item("Add to dock") { close(); actions.edit("${app.label} is in the dock.") { it.addToDock(app.id, ref, installed) } }
                    AppEdit.MOVE_TO_SECTION -> Item("Move to section…") { picking = true }
                    AppEdit.HIDE -> Item("Hide") {
                        close(); actions.edit("${app.label} is hidden. Home settings brings it back.") { it.hide(app.packageName) }
                    }
                }
            }
        }
    }
}

/**
 * The menu for a long press on empty home, at [at] within home's content (launcher polish 1.3).
 * [addWidget] is null where the menu offers no Add widget: while home is locked (decision lor-73li).
 */
@Composable
fun HomeMenu(at: Offset, home: HomeContext, onEdit: () -> Unit, onPanels: () -> Unit, addWidget: (() -> Unit)?, close: () -> Unit) {
    val locked = home.locked
    Box(Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }) {
        DropdownMenu(true, close, containerColor = Raised, shape = androidx.compose.ui.graphics.RectangleShape) {
            // Locked, what edits home is left out, as Lawnchair drops edit mode and Widgets; the
            // caller passes no addWidget then.
            if (!locked) Item("Edit home") { close(); onEdit() }
            if (addWidget != null) Item("Add widget") { close(); addWidget() }
            // The minimap (lor-6get) opens locked too: it draws the map, with Show and its padlock.
            Item("Panels") { close(); onPanels() }
            Item("Wallpaper") { close(); home.actions.wallpaper() }
            Item("Home settings") { close(); home.actions.settings() }
            // HomeScreen leaves edit mode when the file it draws is locked.
            if (locked) Item("Unlock home") { close(); home.actions.edit("Home is unlocked.") { it.setLocked(false) } }
            else Item("Lock home") { close(); home.actions.edit("Home is locked.") { it.setLocked(true) } }
        }
    }
}

@Composable
internal fun Item(text: String, icon: ImageBitmap? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = Type.body.copy(color = Txt, fontSize = 15.sp)) },
        onClick = onClick,
        leadingIcon = if (icon != null) {
            { Image(icon, contentDescription = null, modifier = Modifier.size(22.dp)) }
        } else null,
    )
}
