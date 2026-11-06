package com.spadaval.lor.home

import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spadaval.lor.LorApp
import com.spadaval.lor.MainActivity
import com.spadaval.lor.collect.Launch
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.collect.Launches
import com.spadaval.lor.quicksend.QuickSend
import com.spadaval.lor.ui.LorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LoR's home app (T3 Launcher): the agent page, home from the layout file, and the drawer
 * ([HomeScreen]); the app list and layout file live in [HomeStore], seen through [HomeModel]. A
 * home press while already on home, with nothing open, opens quick send.
 *
 * Launching follows Launcher3 / Lawnchair (`startActivitySafely`, Apache-2.0):
 * `LauncherApps.startMainActivity`, catching the failures of an app that went away before the
 * list refreshed. A crash on home is the worst outcome, so every failure ends in a line in the bar.
 */
class HomeActivity : ComponentActivity() {
    private lateinit var launcherApps: LauncherApps
    private lateinit var model: HomeModel
    private val ui = HomeUi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Home's labels are light over the wallpaper, so the system bar icons are too.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        launcherApps = getSystemService(LauncherApps::class.java)
        val icons = Icons(this, (HomeLook.ICON_SIZES.last * resources.displayMetrics.density).toInt())
        model = HomeModel((application as LorApp).home, lifecycleScope, icons)
        // Back on home goes nowhere; HomeScreen's handlers close what is open first.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
        setContent {
            val apps by model.apps.collectAsStateWithLifecycle()
            val layout by model.layout.collectAsStateWithLifecycle()
            val packIndex by (application as LorApp).home.packs.index.collectAsStateWithLifecycle()
            val stats by model.stats.collectAsStateWithLifecycle()
            val appsLoad by model.appsLoad.collectAsStateWithLifecycle()
            val reads by model.reads.collectAsStateWithLifecycle()
            val search by model.search.collectAsStateWithLifecycle()
            LorTheme {
                ProvideIcons(icons, layout.shown?.look, packIndex) {
                    HomeScreen(apps, appsLoad, layout, reads, stats, search, ui, actions)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != Intent.ACTION_MAIN || !intent.hasCategory(Intent.CATEGORY_HOME)) return
        // Launcher3's alreadyOnHome: home had focus and the press didn't bring it to the front.
        val alreadyOnHome = hasWindowFocus() && intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT == 0
        if (alreadyOnHome && ui.atRest) QuickSend.open(this, QuickSend.From.HOME) else ui.homePressed()
    }

    /**
     * A drag cut off by home pausing or losing window focus puts the item back, as Launcher3 does
     * on cancel. Shown for the notification shade. Compose turns a cancelled press into a release,
     * so a cut-off whose cancel reaches Compose before this (screen off, a call, gesture navigation
     * taking the touch: not tried) may still drop where the finger was; Increment 8 holds that.
     */
    override fun onPause() {
        super.onPause()
        ui.drag.held = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && ui.drag.held != null) {
            android.util.Log.i("LorHome", "a drag was put back: home lost window focus")
            ui.drag.held = null
        }
    }

    override fun onResume() {
        super.onResume()
        model.resume()
    }

    /** Search rows' shortcut icons (launcher polish 5.2). */
    private val shortcutIcons = android.util.LruCache<String, ImageBitmap>(100)

    private val actions = object : HomeActions {
        override fun open(app: HomeApp, at: At) {
            try {
                launcherApps.startMainActivity(app.info.componentName, app.user, null, null)
                record(app, at, shortcut = null)
            } catch (e: RuntimeException) {
                // ActivityNotFoundException, SecurityException, or a profile that was turned off.
                fail("${app.label} can't be opened.")
            }
        }

        override suspend fun shortcuts(app: HomeApp): List<MenuShortcut> = withContext(Dispatchers.IO) {
            try {
                // Only the default home may read other apps' shortcuts.
                if (!launcherApps.hasShortcutHostPermission()) return@withContext emptyList()
                val query = LauncherApps.ShortcutQuery()
                    .setPackage(app.packageName)
                    .setActivity(app.info.componentName)
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
                val density = resources.displayMetrics.densityDpi
                val size = (22 * resources.displayMetrics.density).toInt()
                launcherApps.getShortcuts(query, app.user).orEmpty()
                    .filter { it.isEnabled }
                    .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
                    .take(5)
                    .map { s ->
                        val icon = try {
                            launcherApps.getShortcutIconDrawable(s, density)?.toBitmap(size, size)?.asImageBitmap()
                        } catch (e: RuntimeException) {
                            null
                        }
                        MenuShortcut(s, icon)
                    }
            } catch (e: RuntimeException) {
                // A locked profile (IllegalStateException) or no longer the default home (SecurityException).
                emptyList()
            }
        }

        override suspend fun shortcutIcon(shortcut: ShortcutInfo): ImageBitmap? {
            val key = "${shortcut.`package`}/${shortcut.id}#${shortcut.userHandle}"
            shortcutIcons.get(key)?.let { return it }
            return withContext(Dispatchers.IO) {
                try {
                    val size = (36 * resources.displayMetrics.density).toInt()
                    launcherApps.getShortcutIconDrawable(shortcut, resources.displayMetrics.densityDpi)?.toBitmap(size, size)?.asImageBitmap()
                        ?.also { shortcutIcons.put(key, it) }
                } catch (e: RuntimeException) {
                    null
                }
            }
        }

        override fun openShortcut(app: HomeApp, shortcut: ShortcutInfo, at: At) {
            try {
                launcherApps.startShortcut(shortcut, null, null)
                record(app, at, shortcut.id)
            } catch (e: RuntimeException) {
                // As open: the shortcut went away, its profile is locked, or home lost the permission.
                fail("${shortcut.shortLabel ?: app.label} can't be opened.")
            }
        }

        override fun appInfo(app: HomeApp) {
            try {
                launcherApps.startAppDetailsActivity(app.info.componentName, app.user, null, null)
            } catch (e: RuntimeException) {
                fail("${app.label}'s info can't be opened.")
            }
        }

        override fun uninstall(app: HomeApp) {
            // As Launcher3's uninstall: the package, and the profile it is installed in.
            val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                .putExtra(Intent.EXTRA_USER, app.user)
            try {
                startActivity(intent)
            } catch (e: RuntimeException) {
                fail("${app.label} can't be uninstalled from here.")
            }
        }

        override fun settings() = HomeSettingsActivity.open(this@HomeActivity)

        override fun wallpaper() {
            try {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
            } catch (e: RuntimeException) {
                fail("The wallpaper picker can't be opened.")
            }
        }

        override fun edit(done: String?, after: () -> Unit, change: (LayoutFile) -> LayoutFile) {
            lifecycleScope.launch {
                model.edit(change = change)
                    .onSuccess { if (it.undoable) ui.undo.show(done ?: "Home changed.") }
                    .onFailure { say("Can't: ${it.message ?: "the layout couldn't be changed"}.") }
                after()
            }
        }

        override fun refused(reason: String) = say("Can't: $reason.")

        override fun undo() {
            lifecycleScope.launch {
                model.undo().onFailure { say("Can't: ${it.message ?: "the edit couldn't be undone"}.") }
            }
        }

        override fun setView(view: DrawerView) {
            ui.undo.message = null
            lifecycleScope.launch { model.edit(undoable = false) { it.copy(drawer = it.drawer.copy(view = view)) } }
        }

        override fun openSetting(page: SettingsPage, query: String) {
            if (page.lor) store.searched(query)
            when (page.action) {
                SettingsPage.LOR_HOME -> return HomeSettingsActivity.open(this@HomeActivity)
                SettingsPage.LOR_APP -> return MainActivity.open(this@HomeActivity, MainActivity.SETTINGS)
            }
            startFromSearch(Intent(page.action), query, "${page.title} can't be opened.")
        }

        override fun openContact(contact: SearchContact, way: ContactWay, query: String) =
            startFromSearch(contactIntent(contact, way), query, "${contact.name} can't be opened.")

        override fun clearSearches() = store.clearSearches()

        override fun elsewhere(way: Elsewhere, query: String) {
            val intent = intentFor(way, query)
            if (intent == null) {
                store.searched(query)
                QuickSend.open(this@HomeActivity, QuickSend.From.HOME, text = query.trim())
                return
            }
            startFromSearch(intent, query, "That search can't be opened.")
        }

        override fun restore() {
            lifecycleScope.launch {
                model.restore()
                    .onSuccess { say("layout.json is put right. The broken one is layout.json.broken.") }
                    .onFailure { say("Can't: ${it.message ?: "the layout couldn't be put right"}.") }
            }
        }
    }

    /**
     * Starts [intent] from search (a settings page, the web or store, a contact): a launch with
     * `from: search`, the [query], and the package that took it, as a link from the agent page
     * (null when Android asked which).
     */
    private fun startFromSearch(intent: Intent, query: String, failure: String) {
        try {
            startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            store.searched(query)
            val pkg = takenBy(packageManager, intent)
            Launches.record(this, Launch(LaunchFrom.SEARCH, pkg, label = pkg?.let { Launches.label(this, it) }, query = query))
        } catch (e: RuntimeException) {
            fail(failure)
        }
    }

    private fun record(app: HomeApp, at: At, shortcut: String?) {
        store.launched(app)
        if (at.from == LaunchFrom.SEARCH) at.query?.let(store::searched)
        Launches.record(
            this,
            Launch(
                at.from, app.packageName, app.info.componentName.className, app.label,
                folder = at.folder, query = at.query, shortcut = shortcut,
            ),
        )
    }

    /** The process's home store (launcher polish P1). */
    private val store: HomeStore get() = (application as LorApp).home

    /** A refusal or a plain notice, in the bar without Undo (launcher polish 6.2: no toasts). */
    private fun say(text: String) = ui.undo.say(text, failed = false)

    /** Something that failed: the bar with 3.6's coral dot. */
    private fun fail(text: String) = ui.undo.say(text, failed = true)
}
