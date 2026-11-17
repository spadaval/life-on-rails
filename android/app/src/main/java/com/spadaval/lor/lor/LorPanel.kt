package com.spadaval.lor.lor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spadaval.lor.agent.HomeGrid
import com.spadaval.lor.agent.panelUi
import com.spadaval.lor.agent.rememberAgentModel
import com.spadaval.lor.agent.rememberPanelActions

/*
 * The `lor` panel (lor-lz97, decision lor-p86v): LoR's dashboard shaped for the phone. Its drawing
 * is homecore's (`LorPanelView.kt`, lor-rgx4); this holds what it shows while it is on screen and
 * carries out its taps and writes (through [com.spadaval.lor.lor.LorWrites]).
 */

/**
 * The tab the wide form shows: screen state, kept in memory for the process and never written to
 * `layout.json` (decision lor-p86v), so a fold and unfold, or a move away and back, keep it.
 */
object LorPanelState {
    var tab by mutableStateOf(LorTab.TASKS)
}

/** The `lor` panel. While composed and home is started, it holds LoR's reader with the lists. */
@Composable
fun LorPanel() {
    val model = rememberAgentModel(t3 = false, lists = true)
    LorPanel(panelUi(model), rememberPanelActions(model), HomeGrid)
}
