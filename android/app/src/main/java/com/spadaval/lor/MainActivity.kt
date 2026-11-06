package com.spadaval.lor

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spadaval.lor.ui.DashboardScreen
import com.spadaval.lor.ui.LorTheme
import com.spadaval.lor.ui.SensorsScreen
import com.spadaval.lor.ui.SettingsScreen
import com.spadaval.lor.ui.TimerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Exported, so any app can pass any number: keep it to a tab that exists.
        val start = intent.getIntExtra(EXTRA_TAB, 0).coerceIn(0, TABS.lastIndex)
        setContent {
            LorTheme {
                var tab by rememberSaveable { mutableIntStateOf(start) }
                Scaffold(
                    topBar = {
                        Column(Modifier.statusBarsPadding()) {
                            HomeRoleStrip()
                            PrimaryTabRow(selectedTabIndex = tab) {
                                TABS.forEachIndexed { i, label ->
                                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                                }
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (tab) {
                            0 -> TimerScreen()
                            1 -> SensorsScreen()
                            2 -> SettingsScreen()
                            else -> DashboardScreen()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_TAB = "tab"
        private val TABS = listOf("Timer", "Sensors", "Settings", "Dashboard")
        const val SETTINGS = 2

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

/**
 * The first run's first step (launcher polish 3.5): while LoR isn't the home app, a strip above the
 * tabs offers to make it so. Checked on each resume, so it goes once the role is held.
 */
@Composable
private fun HomeRoleStrip() {
    val context = LocalContext.current
    var home by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        home = isHome(context)
        onPauseOrDispose {}
    }
    val ask = rememberAskForHome { home = isHome(context) }
    if (home) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Use LoR as your home screen", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = ask) { Text("Set as home") }
        }
    }
}
