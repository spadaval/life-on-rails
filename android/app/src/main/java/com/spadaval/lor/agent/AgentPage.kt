package com.spadaval.lor.agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/*
 * The agent page (T3 Launcher, Increment 3.3), the left-most page of home. Its drawing is homecore's
 * (`AgentPageView.kt`, lor-rgx4); this holds what it shows while it is on screen ([AgentModel]) and
 * carries out its taps ([rememberPanelActions]).
 */

@Composable
fun AgentPage() {
    val model = rememberAgentModel(t3 = true)
    AgentPage(panelUi(model), rememberPanelActions(model), HomeGrid)
}

/**
 * The model of a screen showing the agent page's sections: the page, LoR's blocks and the `lor`
 * panel (lor-lz97). While the screen is composed and home is started it holds LoR's reader (with
 * the lists when [lists]) and, when [t3], the T3 connection.
 */
@Composable
internal fun rememberAgentModel(t3: Boolean, lists: Boolean = false): AgentModel {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val model = remember { AgentModel(context, scope) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, t3, lists) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { model.whileVisible(t3, lists) } }
    DisposableEffect(model) { onDispose { model.dispose() } }
    return model
}
