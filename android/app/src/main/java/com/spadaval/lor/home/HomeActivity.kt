package com.spadaval.lor.home

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
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
 * Launching follows Launcher3 / Lawnchair (`ItemClickHandler`): `LauncherApps.startMainActivity`.
 */
class HomeActivity : ComponentActivity() {
    private lateinit var launcherApps: LauncherApps
    private val apps = mutableStateOf(emptyList<LauncherActivityInfo>())

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
                    items(apps.value, key = { it.componentName.flattenToString() + it.user.hashCode() }) { app ->
                        Text(
                            app.label.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { launcherApps.startMainActivity(app.componentName, app.user, null, null) }
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
        apps.value = launchableApps(this, launcherApps)
    }
}

fun launchableApps(context: Context, launcherApps: LauncherApps): List<LauncherActivityInfo> =
    context.getSystemService(UserManager::class.java).userProfiles
        .flatMap { launcherApps.getActivityList(null, it) }
        .sortedBy { it.label.toString().lowercase() }
