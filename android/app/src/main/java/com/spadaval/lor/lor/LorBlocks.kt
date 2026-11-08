package com.spadaval.lor.lor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.AgentModel
import com.spadaval.lor.agent.Clock
import com.spadaval.lor.agent.NeedsYou
import com.spadaval.lor.agent.Running
import com.spadaval.lor.agent.Small
import com.spadaval.lor.agent.Today
import com.spadaval.lor.agent.Unreachable
import com.spadaval.lor.agent.rememberAgentModel
import com.spadaval.lor.home.GridPlace
import com.spadaval.lor.home.Panel
import com.spadaval.lor.home.StackBlock
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.StateFill
import com.spadaval.lor.ui.framedPanel

/**
 * LoR's blocks in a stack (lor-lz97, decision lor-p86v), each the agent page's own section in a
 * card over the wallpaper: `today`, `needs-you` (3 cards), `running` (3 turns) and `t3-queue`
 * (5 tasks). Past its rows a block says "N more", whose tap moves [grid] to the layout's `t3`
 * panel (`needs-you`, `running`) or `lor` panel (`t3-queue`), when it has one. Each holds what it
 * shows while composed: LoR's reader, and the T3 connection for the two that show T3.
 */
@Composable
internal fun LorBlock(block: StackBlock, grid: GridPlace) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().framedPanel(StateFill).padding(bottom = 10.dp)) {
        when (block) {
            StackBlock.Today -> rememberAgentModel(t3 = false).let { Today(it); Notice(it) }
            StackBlock.NeedsYou -> rememberAgentModel(t3 = true).let { NeedsYou(it, max = 3, more = goTo<Panel.T3>(grid), block = true); Stale(it); Notice(it) }
            StackBlock.Running -> rememberAgentModel(t3 = true).let { Running(it, max = 3, more = goTo<Panel.T3>(grid), block = true); Notice(it) }
            StackBlock.T3Queue -> rememberAgentModel(t3 = false).let { AgentTasks(it, max = 5, more = goTo<Panel.Lor>(grid)); Stale(it); Notice(it) }
            else -> {}
        }
    }
}

/** A move to the layout's panel of type [P], or null when it has none. */
private inline fun <reified P : Panel> goTo(grid: GridPlace): (() -> Unit)? =
    grid.panels.firstOrNull { it is P }?.let { p -> { grid.goTo(p.at, animate = true) } }

/**
 * Why LoR can't be read and the time of what the block still shows, with Retry, as `today` says it
 * (launcher polish 3.3's rule): each block may stand alone in a stack, so each says it. Nothing
 * when LoR was read, or when nothing was ever read (the block's own line says so then).
 */
@Composable
private fun Stale(model: AgentModel) {
    val lor = model.lor ?: return
    val error = model.lorError ?: return
    Unreachable("$error Showing what it said at ${lor.now.atZone(lor.zone).format(Clock)}.", model)
}

/** What the block couldn't do outside a row, such as open T3. */
@Composable
private fun Notice(model: AgentModel) {
    model.notice?.let { Text(it, style = Small.copy(color = Bad), modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) }
}
