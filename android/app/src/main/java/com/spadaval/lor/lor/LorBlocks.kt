package com.spadaval.lor.lor

import androidx.compose.runtime.Composable
import com.spadaval.lor.agent.panelUi
import com.spadaval.lor.agent.rememberAgentModel
import com.spadaval.lor.agent.rememberPanelActions
import com.spadaval.lor.home.GridPlace
import com.spadaval.lor.home.Panel
import com.spadaval.lor.home.StackBlock

/**
 * LoR's blocks in a stack (lor-lz97, decision lor-p86v). Their drawing is homecore's
 * (`LorBlockView.kt`, lor-rgx4). Past its rows a block says "N more", whose tap moves [grid] to the
 * layout's `t3` panel (`needs-you`, `running`) or `lor` panel (`t3-queue`), when it has one. Each
 * holds what it shows while composed: LoR's reader, and the T3 connection for the two that show T3.
 */
@Composable
internal fun LorBlock(block: StackBlock, grid: GridPlace) {
    val (kind, t3, more) = when (block) {
        StackBlock.Today -> Triple(LorBlockKind.TODAY, false, null)
        StackBlock.NeedsYou -> Triple(LorBlockKind.NEEDS_YOU, true, goTo<Panel.T3>(grid))
        StackBlock.Running -> Triple(LorBlockKind.RUNNING, true, goTo<Panel.T3>(grid))
        StackBlock.T3Queue -> Triple(LorBlockKind.T3_QUEUE, false, goTo<Panel.Lor>(grid))
        else -> return
    }
    val model = rememberAgentModel(t3 = t3)
    LorBlock(kind, panelUi(model), rememberPanelActions(model), more)
}

/** A move to the layout's panel of type [P], or null when it has none. */
private inline fun <reified P : Panel> goTo(grid: GridPlace): (() -> Unit)? =
    grid.panels.firstOrNull { it is P }?.let { p -> { grid.goTo(p.at, animate = true) } }
