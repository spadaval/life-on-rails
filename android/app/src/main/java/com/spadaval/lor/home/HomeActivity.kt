package com.spadaval.lor.home

import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spadaval.lor.LorApp
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
 * list refreshed. A crash on home is the worst outcome, so every failure ends in a toast.
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
        val icons = Icons((56 * resources.displayMetrics.density).toInt())
        model = HomeModel((application as LorApp).home, lifecycleScope, icons)
        // Back on home goes nowhere; HomeScreen's handlers close what is open first.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
        setContent {
            val apps by model.apps.collectAsStateWithLifecycle()
            val layout by model.layout.collectAsStateWithLifecycle()
            val stats by model.stats.collectAsStateWithLifecycle()
            val appsLoad by model.appsLoad.collectAsStateWithLifecycle()
            LorTheme {
                CompositionLocalProvider(LocalIcons provides icons) {
                    HomeScreen(apps, appsLoad, layout, stats, ui, actions)
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

    override fun onResume() {
        super.onResume()
        model.resume()
    }

    private val actions = object : HomeActions {
        override fun open(app: HomeApp, at: At) {
            try {
                launcherApps.startMainActivity(app.info.componentName, app.user, null, null)
                record(app, at, shortcut = null)
            } catch (e: RuntimeException) {
                // ActivityNotFoundException, SecurityException, or a profile that was turned off.
                toast("${app.label} can't be opened.")
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

        override fun openShortcut(app: HomeApp, shortcut: ShortcutInfo, at: At) {
            try {
                launcherApps.startShortcut(shortcut, null, null)
                record(app, at, shortcut.id)
            } catch (e: RuntimeException) {
                // As open: the shortcut went away, its profile is locked, or home lost the permission.
                toast("${shortcut.shortLabel ?: app.label} can't be opened.")
            }
        }

        override fun appInfo(app: HomeApp) {
            try {
                launcherApps.startAppDetailsActivity(app.info.componentName, app.user, null, null)
            } catch (e: RuntimeException) {
                toast("${app.label}'s info can't be opened.")
            }
        }

        override fun uninstall(app: HomeApp) {
            // As Launcher3's uninstall: the package, and the profile it is installed in.
            val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                .putExtra(Intent.EXTRA_USER, app.user)
            try {
                startActivity(intent)
            } catch (e: RuntimeException) {
                toast("${app.label} can't be uninstalled from here.")
            }
        }

        override fun settings() = HomeSettingsActivity.open(this@HomeActivity)

        override fun wallpaper() {
            try {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
            } catch (e: RuntimeException) {
                toast("The wallpaper picker can't be opened.")
            }
        }

        override fun edit(done: String?, change: (LayoutFile) -> LayoutFile) {
            lifecycleScope.launch {
                model.edit(change)
                    .onSuccess { if (done != null) toast(done) }
                    .onFailure { toast("Can't: ${it.message ?: "the layout couldn't be changed"}.") }
            }
        }

        override fun elsewhere(way: Elsewhere, query: String) {
            val intent = way.intent
            if (intent == null) {
                QuickSend.open(this@HomeActivity, QuickSend.From.HOME, text = query.trim())
                return
            }
            try {
                startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                // As a link from the agent page: the app that took it, or null when Android asked.
                val pkg = takenBy(packageManager, intent)
                Launches.record(this@HomeActivity, Launch(LaunchFrom.SEARCH, pkg, label = pkg?.let { Launches.label(this@HomeActivity, it) }, query = query))
            } catch (e: RuntimeException) {
                toast("That search can't be opened.")
            }
        }

        override fun restore() {
            lifecycleScope.launch {
                model.restore()
                    .onSuccess { toast("layout.json is put right. The broken one is layout.json.broken.") }
                    .onFailure { toast("Can't: ${it.message ?: "the layout couldn't be put right"}.") }
            }
        }
    }

    private fun record(app: HomeApp, at: At, shortcut: String?) {
        (application as LorApp).home.launched(app)
        Launches.record(
            this,
            Launch(
                at.from, app.packageName, app.info.componentName.className, app.label,
                folder = at.folder, query = at.query, shortcut = shortcut,
            ),
        )
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
