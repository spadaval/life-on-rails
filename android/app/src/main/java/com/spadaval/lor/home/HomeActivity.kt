package com.spadaval.lor.home

import android.content.Intent
import android.content.pm.LauncherApps
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spadaval.lor.LorApp
import com.spadaval.lor.collect.Launch
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.collect.Launches
import com.spadaval.lor.quicksend.QuickSend
import com.spadaval.lor.ui.LorTheme

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
            LorTheme {
                CompositionLocalProvider(LocalIcons provides icons) {
                    HomeScreen(apps, layout, ui, ::launch)
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

    private fun launch(app: HomeApp, from: LaunchFrom, folder: String?, query: String?) {
        try {
            launcherApps.startMainActivity(app.info.componentName, app.user, null, null)
            Launches.record(
                this,
                Launch(from, app.packageName, app.info.componentName.className, app.label, folder = folder, query = query),
            )
        } catch (e: RuntimeException) {
            // ActivityNotFoundException, SecurityException, or a profile that was turned off.
            Toast.makeText(this, "${app.label} can't be opened.", Toast.LENGTH_SHORT).show()
        }
    }
}
