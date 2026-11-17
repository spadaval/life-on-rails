package com.spadaval.lor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The LoR app's tabs, in order. The app draws [DASHBOARD] itself: it is a WebView of the dashboard. */
object AppTabs {
    const val TIMER = 0
    const val SENSORS = 1
    const val SETTINGS = 2
    const val DASHBOARD = 3
    val names = listOf("Timer", "Sensors", "Settings", "Dashboard")
}

/**
 * The LoR app's frame: while LoR isn't the home app ([isHome] false), a strip offering to make it
 * so ([setHome]); the tab row on [tab]; and [content], which draws the tab it is given.
 */
@Composable
fun AppShell(tab: Int, choose: (Int) -> Unit, isHome: Boolean, setHome: () -> Unit, content: @Composable (Int) -> Unit) {
    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                if (!isHome) HomeRoleStrip(setHome)
                PrimaryTabRow(selectedTabIndex = tab) {
                    AppTabs.names.forEachIndexed { i, label ->
                        Tab(
                            selected = tab == i, onClick = { choose(i) }, text = { Text(label) },
                            // DESIGN.md: the active one amber, the rest Dust.
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            content(tab)
        }
    }
}

/** The first run's first step (launcher polish 3.5): a strip above the tabs that offers to make LoR the home app. */
@Composable
private fun HomeRoleStrip(setHome: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Use LoR as your home screen", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Button(shape = androidx.compose.ui.graphics.RectangleShape, onClick = setHome) { Text("Set as home") }
        }
    }
}
