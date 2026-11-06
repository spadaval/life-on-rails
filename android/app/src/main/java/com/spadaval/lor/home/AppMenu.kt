package com.spadaval.lor.home

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
 */

/** Where an app was opened or long-pressed: the launch record's `from`, folder and query. */
data class At(val from: LaunchFrom, val folder: String? = null, val query: String? = null)

/** An app shortcut for the menu, with its icon. */
class MenuShortcut(val info: ShortcutInfo, val icon: ImageBitmap?)

/** What home does for its screens, implemented by [HomeActivity]. */
interface HomeActions {
    fun open(app: HomeApp, at: At)

    /** The app's shortcuts for its menu; empty unless LoR is the default home. */
    suspend fun shortcuts(app: HomeApp): List<MenuShortcut>
    fun openShortcut(app: HomeApp, shortcut: ShortcutInfo, at: At)
    fun appInfo(app: HomeApp)
    fun uninstall(app: HomeApp)

    /** Home settings ([HomeSettingsActivity]). */
    fun settings()

    /** The system's wallpaper picker. */
    fun wallpaper()

    /** Applies [change] to `layout.json`; a refusal shows as a toast. [done] is said on success. */
    fun edit(done: String?, change: (LayoutFile) -> LayoutFile)
}

/** What a screen of home needs to draw apps and their menus. */
class HomeContext(val actions: HomeActions, val file: LayoutFile?, val apps: List<HomeApp>) {
    /** How Add writes [app]: its package name when that is its one launchable activity. */
    fun refFor(app: HomeApp): AppRef =
        if (apps.count { it.mainProfile && it.packageName == app.packageName } == 1) AppRef(app.packageName)
        else AppRef(app.packageName, app.id.className)

    /** Hidden by the layout file: left out of the drawer and search. */
    fun hidden(app: HomeApp): Boolean = app.mainProfile && file?.isHidden(app.id) == true
}

/**
 * Draws [content] with a long press that opens [app]'s menu by it, with a haptic tick. [content]
 * gets the long-press handler to put on its own click target, so the app stays one node with
 * both actions for TalkBack and agent-device.
 */
@Composable
fun WithAppMenu(app: HomeApp, at: At, home: HomeContext, content: @Composable (onLongClick: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box {
        content {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            open = true
        }
        if (open) AppMenu(app, at, home) { open = false }
    }
}

private val MenuInk = Color(0xFFECEBE6)
private val MenuPanel = Color(0xFF1B1F25)
private val MenuLine = Color(0x1FFFFFFF)

@Composable
private fun AppMenu(app: HomeApp, at: At, home: HomeContext, close: () -> Unit) {
    var shortcuts by remember(app) { mutableStateOf<List<MenuShortcut>>(emptyList()) }
    LaunchedEffect(app) { shortcuts = home.actions.shortcuts(app) }
    val actions = home.actions
    val file = home.file
    val main = app.mainProfile && file != null
    val places = if (main) file!!.placesOf(app.id) else null
    val removable = (app.info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    // "Move to section" turns the menu into the list of sections (launcher polish 2.3).
    var picking by remember(app) { mutableStateOf(false) }
    if (picking && file != null) {
        val current = file.drawer.sectionOf(app.id, app.category)
        DropdownMenu(true, close, containerColor = MenuPanel, shape = RoundedCornerShape(16.dp)) {
            file.drawer.allSections().forEach { s ->
                val here = s.name == current
                Item(if (here) "${s.name} ✓" else s.name) {
                    close()
                    if (!here) actions.edit("${app.label} is in ${s.name} now.") { it.moveToSection(app.id, home.refFor(app), app.category, s.name) }
                }
            }
        }
        return
    }
    DropdownMenu(true, close, containerColor = MenuPanel, shape = RoundedCornerShape(16.dp)) {
        shortcuts.forEach { s ->
            Item(s.info.shortLabel?.toString() ?: s.info.longLabel?.toString() ?: "Shortcut", s.icon) {
                close(); actions.openShortcut(app, s.info, at)
            }
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider(color = MenuLine)
        Item("App info") { close(); actions.appInfo(app) }
        if (removable) Item("Uninstall") { close(); actions.uninstall(app) }
        if (places != null) {
            val ref = home.refFor(app)
            if (places.folder != null) Item("Remove from folder") { close(); actions.edit(null) { it.removeFromFolder(app.id) } }
            when {
                places.home -> Item("Remove from home") { close(); actions.edit(null) { it.removeFromHome(app.id) } }
                places.folder == null -> Item("Add to home") { close(); actions.edit(null) { it.addToHome(app.id, ref) } }
            }
            if (places.dock) Item("Remove from dock") { close(); actions.edit(null) { it.removeFromDock(app.id) } }
            else Item("Add to dock") { close(); actions.edit(null) { it.addToDock(app.id, ref) } }
            Item("Move to section…") { picking = true }
            Item("Hide") {
                close(); actions.edit("${app.label} is hidden. Home settings brings it back.") { it.hide(app.packageName) }
            }
        }
    }
}

/** The menu for a long press on empty home, at [at] within home's content (launcher polish 1.3). */
@Composable
fun HomeMenu(at: Offset, home: HomeContext, close: () -> Unit) {
    Box(Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }) {
        DropdownMenu(true, close, containerColor = MenuPanel, shape = RoundedCornerShape(16.dp)) {
            Item("Wallpaper") { close(); home.actions.wallpaper() }
            Item("Home settings") { close(); home.actions.settings() }
        }
    }
}

@Composable
private fun Item(text: String, icon: ImageBitmap? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = TextStyle(color = MenuInk, fontSize = 15.sp)) },
        onClick = onClick,
        leadingIcon = if (icon != null) {
            { Image(icon, contentDescription = null, modifier = Modifier.size(22.dp)) }
        } else null,
    )
}
