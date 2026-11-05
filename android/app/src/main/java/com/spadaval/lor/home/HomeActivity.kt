package com.spadaval.lor.home

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spadaval.lor.ui.LorTheme

/**
 * The least that lets LoR be chosen as the home app (T3 Launcher, Increment 1.1): every
 * launchable app in a plain list, opened on a tap.
 *
 * The app list follows Kvaesitso (github.com/MM2-0/Kvaesitso, GPL-3.0, `data/applications`):
 * `LauncherApps.getActivityList` for every user profile, refreshed by a `LauncherApps.Callback`.
 * Launching follows Launcher3 / Lawnchair (`startActivitySafely`): `LauncherApps.startMainActivity`,
 * catching the failures of an app that went away before the list refreshed.
 */
class HomeActivity : ComponentActivity() {
    private lateinit var launcherApps: LauncherApps
    private val apps = mutableStateOf(emptyList<HomeApp>())

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launcherApps = getSystemService(LauncherApps::class.java)
        launcherApps.registerCallback(callback)
        refresh()
        // Back on home goes nowhere.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
        setContent {
            LorTheme {
                LazyColumn(Modifier.fillMaxSize().safeDrawingPadding()) {
                    items(apps.value, key = { it.key }) { app ->
                        Text(
                            app.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { launch(app) }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        launcherApps.unregisterCallback(callback)
        super.onDestroy()
    }

    private fun refresh() {
        apps.value = launchableApps(launcherApps)
    }

    private fun launch(app: HomeApp) {
        try {
            launcherApps.startMainActivity(app.component, app.user, null, null)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "${app.label} can't be opened.", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "${app.label} can't be opened.", Toast.LENGTH_SHORT).show()
        }
    }
}

/** One launchable activity, its label read once (`getLabel` loads it from the package each call). */
class HomeApp(val label: String, val component: ComponentName, val user: UserHandle) {
    val key = "${component.flattenToString()}#$user"
}

fun launchableApps(launcherApps: LauncherApps): List<HomeApp> =
    launcherApps.profiles
        .flatMap { launcherApps.getActivityList(null, it) }
        .map { HomeApp(it.label.toString(), it.componentName, it.user) }
        .sortedBy { it.label.lowercase() }
