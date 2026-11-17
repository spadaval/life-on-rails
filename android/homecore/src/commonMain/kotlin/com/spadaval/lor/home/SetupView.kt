package com.spadaval.lor.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.agent.PairActions
import com.spadaval.lor.agent.PairCalls
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Pill
import com.spadaval.lor.ui.StateFill
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.framedPanel

/*
 * Home's set-up card (launcher polish 3.5). Not a wizard: as Kvaesitso shows each missing piece
 * where it is needed (`MissingPermissionBanner`), the card lists the three things a fresh install
 * needs, each checked live, with the one action that does it. It stays until all three hold at
 * once or Skip set-up is tapped, and then never shows again: the app's `SetupCard` decides that and
 * checks each step; this draws it (lor-rgx4).
 */

/** Where the read of LoR stands. */
sealed interface LorReach {
    data object Checking : LorReach
    data object Reached : LorReach
    data class Failed(val reason: String) : LorReach
}

enum class SetupStep { HOME, T3, LOR }

object Setup {
    /** The steps not done yet, in order; the first one's action is the card's primary one. */
    fun left(home: Boolean, paired: Boolean, lor: LorReach): List<SetupStep> = listOfNotNull(
        SetupStep.HOME.takeUnless { home },
        SetupStep.T3.takeUnless { paired },
        SetupStep.LOR.takeUnless { lor == LorReach.Reached },
    )
}

/** The card's three steps: LoR is the home app, T3 is paired, LoR is reached at [where] (the address's host). */
data class SetupUi(val home: Boolean, val paired: Boolean, val lor: LorReach, val where: String)

/** The card's actions. */
class SetupActions(
    val skip: () -> Unit = {},
    val makeHome: () -> Unit = {},
    val checkAgain: () -> Unit = {},
    /** Opens the LoR app's Settings tab. */
    val changeAddress: () -> Unit = {},
    val pair: PairCalls = PairCalls(),
)

@Composable
fun SetupCard(ui: SetupUi, on: SetupActions, modifier: Modifier = Modifier) {
    val left = Setup.left(ui.home, ui.paired, ui.lor)
    val where = ui.where
    Column(
        modifier
            .fillMaxWidth()
            .framedPanel(StateFill)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Set up this phone", style = Type.body.copy(color = Txt, fontSize = 15.sp), modifier = Modifier.weight(1f))
            Pill("Skip set-up", onClick = on.skip)
        }
        val primary = left.firstOrNull()
        val home = ui.home
        Step(if (home) Mark.DONE else Mark.TODO, if (home) "LoR is your home app." else "Home app", if (home) null else "LoR isn't the default home yet.") {
            if (!home) Pill("Make LoR home", primary = primary == SetupStep.HOME, onClick = on.makeHome)
        }
        val paired = ui.paired
        Step(if (paired) Mark.DONE else Mark.TODO, if (paired) "Paired with T3." else "T3", if (paired) null else "Scan the code in T3's Connections settings, or paste its pairing link.") {
            if (!paired) PairActions(on.pair, primary = primary == SetupStep.T3)
        }
        when (val l = ui.lor) {
            LorReach.Checking -> Step(Mark.TODO, "LoR", "Checking $where…")
            LorReach.Reached -> Step(Mark.DONE, "Reaches LoR at $where.")
            is LorReach.Failed -> Step(Mark.FAILED, "LoR", "Can't reach $where. ${l.reason}") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Check again", primary = primary == SetupStep.LOR, onClick = on.checkAgain)
                    Pill("Change address", onClick = on.changeAddress)
                }
            }
        }
    }
}

private enum class Mark { DONE, TODO, FAILED }

/** One step: its mark, a line, an optional detail, and its actions under them. */
@Composable
private fun Step(mark: Mark, title: String, detail: String? = null, actions: @Composable () -> Unit = {}) {
    Row {
        StepMark(mark)
        Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = Type.body.copy(color = if (mark == Mark.DONE) Mute else Txt, fontSize = 14.sp))
            detail?.let { Text(it, style = Type.body.copy(color = Mute, fontSize = 12.sp)) }
            actions()
        }
    }
}

/** A tick for a step that's done, an open ring for one to do, a coral dot for LoR not answering. */
@Composable
private fun StepMark(mark: Mark) {
    Canvas(Modifier.padding(top = 2.dp).size(16.dp)) {
        val w = size.width
        val line = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        when (mark) {
            Mark.DONE -> {
                drawLine(Mute, Offset(0.18f * w, 0.52f * w), Offset(0.42f * w, 0.76f * w), line.width, StrokeCap.Round)
                drawLine(Mute, Offset(0.42f * w, 0.76f * w), Offset(0.84f * w, 0.26f * w), line.width, StrokeCap.Round)
            }
            Mark.TODO -> drawCircle(Dim, radius = 0.4f * w, style = line)
            // Machinery that failed: the coral dot every failed state uses.
            Mark.FAILED -> drawCircle(Bad, radius = 3.dp.toPx())
        }
    }
}
