package com.spadaval.lor.lab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One named state on the device, drawn by the lab's version from the same fixtures as the browser,
 * over the real wallpaper, with a banner that says it is a fixture. It reads nothing: no server, no
 * T3, no layout file. Back or home leaves it.
 */
class LabStateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val state = intent.getStringExtra(LabStates.STATE) ?: Lab.states.first().name
        val shown = LabStates.resolve(state, intent.getStringExtra(LabStates.VERSION))
        Log.i(TAG, "fixture ${shown.fixture.name} in ${shown.version.name}" + (shown.problem?.let { " ($it)" } ?: ""))
        setContent {
            val wide = LocalConfiguration.current.screenWidthDp >= 600
            Box(Modifier.fillMaxSize()) {
                shown.version.draw(LabScene(shown.fixture, if (wide) LabScreen.Inner else LabScreen.Cover))
                Text(
                    "FIXTURE · ${shown.version.name}/${shown.fixture.name}" + (shown.problem?.let { " · $it" } ?: ""),
                    color = Color.Black,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 2.dp)
                        .background(Color(0xE6F08A2C)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }

    private companion object {
        const val TAG = "LabState"
    }
}
