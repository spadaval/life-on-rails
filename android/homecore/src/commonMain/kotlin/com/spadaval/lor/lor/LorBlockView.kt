package com.spadaval.lor.lor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.LorUi
import com.spadaval.lor.agent.NeedsYou
import com.spadaval.lor.agent.PanelActions
import com.spadaval.lor.agent.PanelUi
import com.spadaval.lor.agent.Running
import com.spadaval.lor.agent.Today
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.LocalClock
import com.spadaval.lor.ui.SmallText
import com.spadaval.lor.ui.StateFill
import com.spadaval.lor.ui.Unreachable
import com.spadaval.lor.ui.framedPanel

/** LoR's blocks a home stack can hold (`StackBlock`'s `today`, `needs-you`, `running`, `t3-queue`). */
enum class LorBlockKind { TODAY, NEEDS_YOU, RUNNING, T3_QUEUE }

/**
 * LoR's blocks in a stack (lor-lz97, decision lor-p86v), each the agent page's own section in a
 * card over the wallpaper: `today`, `needs-you` (3 cards), `running` (3 turns) and `t3-queue`
 * (5 tasks). Past its rows a block says "N more", whose tap ([more]) moves home's grid to the
 * layout's `t3` panel (`needs-you`, `running`) or `lor` panel (`t3-queue`), null when it has none.
 */
@Composable
fun LorBlock(kind: LorBlockKind, ui: PanelUi, on: PanelActions, more: (() -> Unit)?) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().framedPanel(StateFill).padding(bottom = 10.dp)) {
        when (kind) {
            LorBlockKind.TODAY -> { Today(ui.lor, on); Notice(ui.notice) }
            LorBlockKind.NEEDS_YOU -> { NeedsYou(ui, on, max = 3, more = more, block = true); Stale(ui.lor, on); Notice(ui.notice) }
            LorBlockKind.RUNNING -> { Running(ui, on, max = 3, more = more, block = true); Notice(ui.notice) }
            LorBlockKind.T3_QUEUE -> { AgentTasks(ui, on, max = 5, more = more); Stale(ui.lor, on); Notice(ui.notice) }
        }
    }
}

/**
 * Why LoR can't be read and the time of what the block still shows, with Retry, as `today` says it
 * (launcher polish 3.3's rule): each block may stand alone in a stack, so each says it. Nothing
 * when LoR was read, or when nothing was ever read (the block's own line says so then).
 */
@Composable
private fun Stale(lor: LorUi, on: PanelActions) {
    val home = lor.home ?: return
    val error = lor.error ?: return
    Unreachable("$error Showing what it said at ${LocalClock.time(home.now, home.zone)}.", lor.retrying, on.retry)
}

/** What the block couldn't do outside a row, such as open T3. */
@Composable
private fun Notice(notice: String?) {
    notice?.let { Text(it, style = SmallText.copy(color = Bad), modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) }
}
