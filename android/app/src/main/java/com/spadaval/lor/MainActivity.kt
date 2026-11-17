package com.spadaval.lor

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spadaval.lor.ui.AppShell
import com.spadaval.lor.ui.AppTabs
import com.spadaval.lor.ui.DashboardScreen
import com.spadaval.lor.ui.LorTheme
import com.spadaval.lor.ui.SensorsScreen
import com.spadaval.lor.ui.SettingsScreen
import com.spadaval.lor.ui.TimerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light bar icons: the app is dark whatever the system's mode (launcher polish 6.1).
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        // Exported, so any app can pass any number: keep it to a tab that exists.
        val start = intent.getIntExtra(EXTRA_TAB, 0).coerceIn(0, TABS.lastIndex)
        setContent {
            LorTheme {
                var tab by rememberSaveable { mutableIntStateOf(start) }
                val home = rememberHomeRole()
                AppShell(tab, { tab = it }, home.held, home.ask) {
                    when (it) {
                        AppTabs.TIMER -> TimerScreen()
                        AppTabs.SENSORS -> SensorsScreen()
                        AppTabs.SETTINGS -> SettingsScreen()
                        else -> DashboardScreen()
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_TAB = "tab"
        private val TABS = AppTabs.names
        const val SETTINGS = AppTabs.SETTINGS

        /** The LoR app in its own task, on [tab] (the Settings tab for home's set-up card). */
        fun open(context: Context, tab: Int) = context.startActivity(
            Intent(context, MainActivity::class.java).putExtra(EXTRA_TAB, tab)
                // CLEAR_TOP so a running copy starts again on the tab asked for.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }
}

/** Whether LoR is the default home app. */
fun isHome(context: Context): Boolean = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)

/** The system's own question, "Set LoR as your default home app?" (Kvaesitso's `PermissionsManager` asks the same way). */
@Composable
fun rememberAskForHome(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onResult() }
    return { launcher.launch(context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)) }
}

/** Whether LoR holds the home role, read on each resume, and the system's question that asks for it. */
private class HomeRole(val held: Boolean, val ask: () -> Unit)

/**
 * The first run's first step (launcher polish 3.5): while LoR isn't the home app, [AppShell]'s
 * strip above the tabs offers to make it so. Checked on each resume, so it goes once the role is held.
 */
@Composable
private fun rememberHomeRole(): HomeRole {
    val context = LocalContext.current
    var home by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        home = isHome(context)
        onPauseOrDispose {}
    }
    val ask = rememberAskForHome { home = isHome(context) }
    return HomeRole(home, ask)
}
