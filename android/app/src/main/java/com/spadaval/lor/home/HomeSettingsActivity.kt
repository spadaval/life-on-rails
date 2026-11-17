package com.spadaval.lor.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spadaval.lor.LorApp
import com.spadaval.lor.MainActivity
import com.spadaval.lor.ui.LorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home settings (launcher polish 1.3): the launcher's own settings, opened from the home menu and
 * the drawer's "Hidden apps" row. It starts in home's task, so back returns to home. It reads and
 * writes through [HomeStore], as home does; the shared module's [HomeSettings] draws it. Later
 * increments add their settings here, not to the LoR tabs.
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
                            HomeSettingsScreen(apps, layout, appsLoad == AppsLoad.LOADED, ::edit, ::restore, ::open)
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

/** What Home settings draws from: the file, its broken state, the screen's class, the packs and Android's two permissions. */
@Composable
private fun HomeSettingsScreen(
    apps: List<HomeApp>,
    layout: LayoutState,
    appsLoaded: Boolean,
    edit: (String?, (LayoutFile) -> LayoutFile) -> Unit,
    restore: () -> Unit,
    open: (Intent) -> Unit,
) {
    val context = LocalContext.current
    val file = (layout as? LayoutState.Ready)?.file
    // A hidden entry: the installed app it names, if any (the first activity, for a package name).
    val hidden = remember(apps, file) {
        file?.hidden.orEmpty().map { ref -> ref to apps.firstOrNull { ref.matches(it.id) } }
    }
    val installed = hidden.mapNotNull { (ref, app) -> app?.let { ref.toString() to it } }.toMap()
    val packs by produceState<List<InstalledPack>?>(null) { value = withContext(Dispatchers.IO) { installedPacks(context.packageManager) } }
    val dots = rememberDotsAccess(open)
    val contacts = rememberContactsAccess(open)
    HomeSettings(
        HomeSettingsUi(
            file = file?.let { f ->
                SettingsFileUi(
                    hidden = hidden.map { (ref, app) -> HiddenUi(ref.toString(), app?.label ?: ref.toString(), ref.packageName, ref.work, app != null) },
                    tabs = f.drawer.tabs.map { TabUi(it.name, it.icon, it.kind, it.apps.size) },
                    canAddTab = f.drawer.tabs.size < Drawer.MAX_TABS,
                    look = f.look,
                    keyboard = f.drawer.keyboard,
                )
            },
            broken = (layout as? LayoutState.Broken)?.let { brokenUi(it, appsLoaded) },
            wide = LocalConfiguration.current.screenWidthDp >= HomeLook.WIDE_DP,
            packs = packs?.map { IconPackUi(it.packageName, it.label) },
            dots = dots.on,
            contacts = contacts.ui,
        ),
        HomeSettingsActions(
            edit = { done, change -> edit(done, change.toFileChange()) },
            restore = restore,
            panels = { HomeActivity.openMinimap(context) },
            dots = dots.open,
            contacts = contacts.tap,
            homeApp = { open(Intent(Settings.ACTION_HOME_SETTINGS)) },
            lorApp = { open(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
            icon = { h -> installed[h.key]?.let { LauncherIcon(it, 40.dp) } },
        ),
    )
}

/** The change to `layout.json` that a Home settings edit asks for. */
private fun SettingsEdit.toFileChange(): (LayoutFile) -> LayoutFile = when (this) {
    is SettingsEdit.Unhide -> { f -> f.unhide(pkg, work) }
    is SettingsEdit.NewTab -> { f -> f.newTab(name) }
    is SettingsEdit.RenameTab -> { f -> f.renameTab(name, to) }
    is SettingsEdit.SetTabIcon -> { f -> f.setTabIcon(name, icon) }
    is SettingsEdit.MoveTab -> { f -> f.moveTab(name, by) }
    is SettingsEdit.DeleteTab -> { f -> f.deleteTab(name) }
    is SettingsEdit.Keyboard -> { f -> f.copy(drawer = f.drawer.copy(keyboard = on)) }
    is SettingsEdit.Look -> { f -> f.copy(look = change(f.look)) }
}

private class DotsAccess(val on: Boolean, val open: () -> Unit)

/** Notification dots: whether LoR has notification access, read again on each resume, and the system's page for it. */
@Composable
private fun rememberDotsAccess(open: (Intent) -> Unit): DotsAccess {
    val context = LocalContext.current
    var on by remember { mutableStateOf(Dots.granted(context)) }
    LifecycleResumeEffect(Unit) {
        on = Dots.granted(context)
        onPauseOrDispose {}
    }
    return DotsAccess(on) {
        open(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, Dots.component(context).flattenToString()),
        )
    }
}

private class ContactsAccess(val ui: ContactsUi, val tap: () -> Unit)

/**
 * Contacts in search (launcher polish 5.6): Allow asks Android; once Android has stopped asking
 * (denied for good), it opens LoR's App info instead. Search itself never asks.
 */
@Composable
private fun rememberContactsAccess(open: (Intent) -> Unit): ContactsAccess {
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
    return ContactsAccess(ContactsUi(on, asksAgain)) {
        if (asksAgain == false) {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        } else {
            ask.launch(Manifest.permission.READ_CONTACTS)
        }
    }
}
