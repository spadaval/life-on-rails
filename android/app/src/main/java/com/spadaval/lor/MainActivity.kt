package com.spadaval.lor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.spadaval.lor.ui.DashboardScreen
import com.spadaval.lor.ui.LorTheme
import com.spadaval.lor.ui.SensorsScreen
import com.spadaval.lor.ui.SettingsScreen
import com.spadaval.lor.ui.TimerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LorTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val tabs = listOf("Timer", "Sensors", "Settings", "Dashboard")
                Scaffold(
                    topBar = {
                        PrimaryTabRow(selectedTabIndex = tab, modifier = Modifier.statusBarsPadding()) {
                            tabs.forEachIndexed { i, label ->
                                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
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
}
