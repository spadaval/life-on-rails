package com.spadaval.lor.home

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
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
import androidx.core.content.ContextCompat
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
    /** The configuration last seen, so each change logs what it changed. */
    private lateinit var config: Configuration
    /** The screen went off; when it comes back on, home goes to its start (decision note on lor-ph30). */
    private var slept = false

    /**
     * A screen-off is remembered, and acted on when the screen comes back on, so the start is that
     * screen's: a fold or unfold while the screen was off has changed it by then (lor-ozj2).
     */
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> slept = true
                Intent.ACTION_SCREEN_ON -> if (slept) {
                    slept = false
                    ui.homePressed(animate = false)
                    Log.i(TAG, "screen on: home goes to its start (${describe(resources.configuration)})")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = Configuration(resources.configuration)
        Log.i(TAG, "HomeActivity created (${describe(config)})")
        // Light system bar icons to start; HomeScreen turns them dark over a light wallpaper (6's review, R1).
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        launcherApps = getSystemService(LauncherApps::class.java)
        model = HomeModel((application as LorApp).home)
        val icons = model.icons
        ContextCompat.registerReceiver(
            this, screen, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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

    /**
     * A fold, an unfold, a theme or a font change is handled here rather than by recreating home
     * (the manifest's `configChanges`; decision note on lor-ph30): Compose takes the new
     * configuration and density, and every state stays as it is.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "configuration changed: ${configChanges(config.diff(newConfig))} (${describe(newConfig)})")
        config = Configuration(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screen)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action != Intent.ACTION_MAIN || !intent.hasCategory(Intent.CATEGORY_HOME)) return
        // Launcher3's alreadyOnHome: home had focus and the press didn't bring it to the front.
        val broughtToFront = intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0
        val alreadyOnHome = hasWindowFocus() && !broughtToFront
        // Home on screen before the press closes what is open with motion; brought from elsewhere,
        // there is nothing to watch, so it shuts at once (launcher polish 7.1).
        val wasVisible = !broughtToFront && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        if (alreadyOnHome && ui.atRest) QuickSend.open(this, QuickSend.From.HOME) else ui.homePressed(animate = wasVisible)
    }

    /**
     * A drag cut off by home pausing or losing window focus puts the item back, as Launcher3 does
     * on cancel. Shown for the notification shade. Compose turns a cancelled press into a release,
     * so a cut-off whose cancel reaches Compose before this (screen off, a call, gesture navigation
     * taking the touch: not tried) may still drop where the finger was; lor-kxxz holds that.
     */
    override fun onPause() {
        super.onPause()
        ui.drag.held = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && ui.drag.held != null) {
            Log.i(TAG, "a drag was put back: home lost window focus")
            ui.drag.held = null
        }
    }

    override fun onResume() {
        super.onResume()
        model.resume()
    }

    // Widgets get their updates while home is started (Launcher3's LauncherWidgetHolder).
    override fun onStart() {
        super.onStart()
        store.widgets.startListening()
    }

    override fun onStop() {
        super.onStop()
        store.widgets.stopListening()
    }

    /** The widget on its way to home, held by the process's widget host (lor-r0f9), not by this activity. */
    private var adding: WidgetHost.Adding?
        get() = store.widgets.adding
        set(v) {
            store.widgets.adding = v
        }

    /** Android's "Allow LoR to create widgets" dialog, when the bind needs it. */
    private val bindWidget = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        val a = adding ?: return@registerForActivityResult
        if (r.resultCode == RESULT_OK) {
            Log.i(WIDGETS, "bound ${a.id} to ${a.info.provider.flattenToShortString()} (the dialog allowed it)")
            configureWidget(a)
        } else cancelWidget(a, "the bind dialog was cancelled")
    }

    /** The widget's configure screen when it has one, else straight to home (Kvaesitso's WidgetPickerSheet). */
    private fun configureWidget(a: WidgetHost.Adding) {
        if (a.info.configure == null) return placeWidget(a)
        try {
            store.widgets.host.startAppWidgetConfigureActivityForResult(this, a.id, 0, CONFIGURE_WIDGET, configureOptions())
            Log.i(WIDGETS, "configuring ${a.id}")
        } catch (e: RuntimeException) {
            cancelWidget(a, "its configure screen can't be opened (${e.javaClass.simpleName})")
        }
    }

    /**
     * API 34 starts a widget's configure screen from the host's PendingIntent only when allowed to.
     * ALLOWED is deprecated in API 36 for ALLOW_ALWAYS, which it still means there.
     */
    @Suppress("DEPRECATION")
    private fun configureOptions(): Bundle? =
        if (android.os.Build.VERSION.SDK_INT < 34) null
        else ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle()

    @Deprecated("The widget host's configure screen reports here; it has no ActivityResult contract.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CONFIGURE_WIDGET -> {
                // A result for another id (a widget's own result carrying it) isn't this add's.
                val said = data?.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                val a = adding?.takeIf { said == -1 || said == it.id } ?: return Log.i(WIDGETS, "configure result for $said with no add waiting for it").let {}
                Log.i(WIDGETS, "configure ${a.id}: ${if (resultCode == RESULT_OK) "done" else "cancelled"}")
                if (resultCode == RESULT_OK) placeWidget(a) else cancelWidget(a, "its configure screen was cancelled")
            }
            RECONFIGURE_WIDGET -> Log.i(WIDGETS, "settings ${data?.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 0)}: ${if (resultCode == RESULT_OK) "done" else "cancelled"}")
        }
    }

    /** Puts the bound widget in its stack with Undo, at its provider's minimum height; a refused edit deletes the id. */
    private fun placeWidget(a: WidgetHost.Adding) {
        adding = null
        val info = store.widgets.manager.getAppWidgetInfo(a.id) ?: a.info
        val height = WidgetSize.newHeight(info.minHeight / resources.displayMetrics.density)
        val label = info.loadLabel(packageManager)
        lifecycleScope.launch {
            model.edit { it.addWidget(a.at, a.id, info.provider.flattenToString(), height) }
                .onSuccess {
                    Log.i(WIDGETS, "added ${a.id} (${info.provider.flattenToShortString()}) at ${a.at}, $height dp")
                    ui.undo.show("$label is on home.")
                }
                .onFailure {
                    store.widgets.delete(a.id, "the edit was refused")
                    say("Can't: ${it.message ?: "the widget couldn't be added"}.")
                }
        }
    }

    private fun cancelWidget(a: WidgetHost.Adding, why: String) {
        adding = null
        store.widgets.delete(a.id, why)
    }

    /** Search rows' shortcut icons (launcher polish 5.2). */
    private val shortcutIcons = android.util.LruCache<String, ImageBitmap>(100)

    private val actions = object : HomeActions {
        override fun open(app: HomeApp, at: At, icon: IconSpot?) {
            try {
                val from = icon?.bounds()
                launcherApps.startMainActivity(app.info.componentName, app.user, from?.let(::onScreen), launchOptions(from))
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

        override fun openShortcut(app: HomeApp, shortcut: ShortcutInfo, at: At, icon: IconSpot?) {
            try {
                val from = icon?.bounds()
                launcherApps.startShortcut(shortcut, from?.let(::onScreen), launchOptions(from))
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

        override fun calendar() {
            // ACTION_VIEW on the calendar provider's time/<millis>, which calendar apps open at that day.
            val intent = Intent(Intent.ACTION_VIEW, android.provider.CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(System.currentTimeMillis().toString()).build())
            try {
                startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                val pkg = takenBy(packageManager, intent)
                Launches.record(this@HomeActivity, Launch(LaunchFrom.HOME, pkg, label = pkg?.let { Launches.label(this@HomeActivity, it) }))
            } catch (e: RuntimeException) {
                fail("No calendar app can open today.")
            }
        }

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
                    .onFailure {
                        // A refused write ticks as a refused drop does (launcher polish 7.5).
                        window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                        say("Can't: ${it.message ?: "the layout couldn't be changed"}.")
                    }
                after()
            }
        }

        override fun refused(reason: String) = say("Can't: $reason.")

        override fun quickSend() = QuickSend.open(this@HomeActivity, QuickSend.From.HOME)

        override fun addWidget(at: Cell, provider: android.appwidget.AppWidgetProviderInfo) {
            val widgets = store.widgets
            // One at a time: a flow cut off without a result leaves its id to the next start's sweep.
            adding?.let { cancelWidget(it, "another widget was picked") }
            val a = try {
                WidgetHost.Adding(widgets.allocate(), at, provider)
            } catch (e: RuntimeException) {
                return fail("No widget can be added now.")
            }
            adding = a
            if (widgets.manager.bindAppWidgetIdIfAllowed(a.id, provider.profile, provider.provider, null)) {
                Log.i(WIDGETS, "bound ${a.id} to ${provider.provider.flattenToShortString()} (allowed already)")
                return configureWidget(a)
            }
            Log.i(WIDGETS, "asking to bind ${a.id} to ${provider.provider.flattenToShortString()}")
            try {
                bindWidget.launch(
                    Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, a.id)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile),
                )
            } catch (e: RuntimeException) {
                cancelWidget(a, "the bind dialog can't be opened")
                fail("The widget can't be added.")
            }
        }

        override fun widgetSettings(id: Int) {
            try {
                store.widgets.host.startAppWidgetConfigureActivityForResult(this@HomeActivity, id, 0, RECONFIGURE_WIDGET, configureOptions())
            } catch (e: RuntimeException) {
                fail("The widget's settings can't be opened.")
            }
        }

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

    /**
     * The window grows out of the icon at [icon] (window coordinates), or nothing when no icon is on
     * screen (launcher polish 7.4). Launcher3's `getActivityLaunchOptions` (Lawnchair, Apache-2.0)
     * passes a clip reveal from the icon's bounds when it can't run its own remote animation, which
     * needs a system permission; Kvaesitso's `SearchableItemVM.launch` (GPL-3.0, ideas only) passes
     * this scale-up, which is what "launch animations from the icon" asks.
     */
    private fun launchOptions(icon: android.graphics.Rect?): Bundle? =
        icon?.let { ActivityOptions.makeScaleUpAnimation(window.decorView, it.left, it.top, it.width(), it.height()).toBundle() }

    /** [r] in window coordinates, moved to the screen's, as an app's source bounds are given. */
    private fun onScreen(r: android.graphics.Rect): android.graphics.Rect {
        val at = IntArray(2)
        window.decorView.getLocationOnScreen(at)
        return android.graphics.Rect(r).apply { offset(at[0], at[1]) }
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

    private companion object {
        const val TAG = "LorHome"
        const val WIDGETS = "LorWidgets"
        const val CONFIGURE_WIDGET = 1024
        const val RECONFIGURE_WIDGET = 1025

        /** What a log line needs of [c]: the smallest width, density, night mode and font scale. */
        fun describe(c: Configuration) =
            "sw ${c.smallestScreenWidthDp} dp, ${c.screenWidthDp}x${c.screenHeightDp} dp, ${c.densityDpi} dpi, " +
                "night ${if (c.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) "yes" else "no"}, font ${c.fontScale}"

        /** The names of the `configChanges` bits in [diff]. */
        fun configChanges(diff: Int): String = listOf(
            ActivityInfo.CONFIG_ORIENTATION to "orientation", ActivityInfo.CONFIG_SCREEN_SIZE to "screenSize",
            ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE to "smallestScreenSize", ActivityInfo.CONFIG_SCREEN_LAYOUT to "screenLayout",
            ActivityInfo.CONFIG_DENSITY to "density", ActivityInfo.CONFIG_UI_MODE to "uiMode", ActivityInfo.CONFIG_FONT_SCALE to "fontScale",
            ActivityInfo.CONFIG_KEYBOARD to "keyboard", ActivityInfo.CONFIG_KEYBOARD_HIDDEN to "keyboardHidden",
            ActivityInfo.CONFIG_NAVIGATION to "navigation", ActivityInfo.CONFIG_LOCALE to "locale",
            ActivityInfo.CONFIG_FONT_WEIGHT_ADJUSTMENT to "fontWeightAdjustment",
        ).filter { (bit, _) -> diff and bit != 0 }.joinToString(" ") { it.second }.ifEmpty { "nothing named" }
    }
}
