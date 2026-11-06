package com.spadaval.lor.home

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spadaval.lor.MainActivity
import com.spadaval.lor.agent.PairActions
import com.spadaval.lor.agent.Pill
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.isHome
import com.spadaval.lor.net.LorClient
import com.spadaval.lor.rememberAskForHome
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Home's set-up card (launcher polish 3.5). Not a wizard: as Kvaesitso shows each missing piece
 * where it is needed (`MissingPermissionBanner`), the card lists the three things a fresh install
 * needs, each checked live, with the one action that does it. It stays until all three hold at
 * once or Skip set-up is tapped, and then never shows again (`Prefs.setupDone`).
 */

/** Where the read of LoR stands. */
sealed interface LorReach {
    data object Checking : LorReach
    data object Reached : LorReach
    data class Failed(val reason: String) : LorReach
}

enum class SetupStep { HOME, T3, LOR }

object Setup {
    /** Paired, whether or not the connection is up just now; a refused session needs pairing again. */
    fun paired(status: T3Connection.Status): Boolean =
        status !is T3Connection.Status.NotPaired && status !is T3Connection.Status.Rejected

    /** The steps not done yet, in order; the first one's action is the card's primary one. */
    fun left(home: Boolean, paired: Boolean, lor: LorReach): List<SetupStep> = listOfNotNull(
        SetupStep.HOME.takeUnless { home },
        SetupStep.T3.takeUnless { paired },
        SetupStep.LOR.takeUnless { lor == LorReach.Reached },
    )
}

@Composable
internal fun SetupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var shown by remember { mutableStateOf(!prefs.setupDone) }
    if (!shown) return
    val connection = remember { T3Connection.get(context.applicationContext) }
    val status by connection.status.collectAsState()
    var home by remember { mutableStateOf(isHome(context)) }
    var lor by remember { mutableStateOf<LorReach>(LorReach.Checking) }
    var checks by remember { mutableIntStateOf(0) }
    val address = prefs.serverUrl
    // The role and LoR are read again on each resume: the role may have changed in Settings, the
    // address in the LoR app's Settings tab (Change address).
    LifecycleResumeEffect(Unit) {
        home = isHome(context)
        checks++
        onPauseOrDispose {}
    }
    LaunchedEffect(checks) {
        if (checks == 0) return@LaunchedEffect
        lor = LorReach.Checking
        lor = try {
            val url = prefs.serverUrl
            val device = prefs.deviceName
            withContext(Dispatchers.IO) { LorClient(url).home(device) }
            LorReach.Reached
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LorReach.Failed(LorClient.describe(e))
        }
    }
    val left = Setup.left(home, Setup.paired(status), lor)
    LaunchedEffect(left.isEmpty()) {
        if (left.isEmpty()) {
            prefs.setupDone = true
            shown = false
        }
    }
    val ask = rememberAskForHome { home = isHome(context) }
    // An install that already has the role and T3 (an update to a set-up phone) waits for the read
    // of LoR before drawing, so the card doesn't flash up and go when that read succeeds.
    if (home && Setup.paired(status) && lor == LorReach.Checking) return
    val where = Uri.parse(address).authority ?: address

    Column(
        modifier
            .fillMaxWidth()
            .background(StateFill, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Set up this phone", style = TextStyle(color = Ink, fontSize = 15.sp), modifier = Modifier.weight(1f))
            Pill("Skip set-up") {
                prefs.setupDone = true
                shown = false
            }
        }
        val primary = left.firstOrNull()
        Step(if (home) Mark.DONE else Mark.TODO, if (home) "LoR is your home app." else "Home app", if (home) null else "LoR isn't the default home yet.") {
            if (!home) Pill("Make LoR home", primary = primary == SetupStep.HOME, onClick = ask)
        }
        val paired = Setup.paired(status)
        Step(if (paired) Mark.DONE else Mark.TODO, if (paired) "Paired with T3." else "T3", if (paired) null else "Scan the code in T3's Connections settings, or paste its pairing link.") {
            if (!paired) PairActions(connection, primary = primary == SetupStep.T3)
        }
        when (val l = lor) {
            LorReach.Checking -> Step(Mark.TODO, "LoR", "Checking $where…")
            LorReach.Reached -> Step(Mark.DONE, "Reaches LoR at $where.")
            is LorReach.Failed -> Step(Mark.FAILED, "LoR", "Can't reach $where. ${l.reason}") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Check again", primary = primary == SetupStep.LOR) { checks++ }
                    Pill("Change address") { MainActivity.open(context, MainActivity.SETTINGS) }
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
            Text(title, style = TextStyle(color = if (mark == Mark.DONE) Muted else Ink, fontSize = 14.sp))
            detail?.let { Text(it, style = TextStyle(color = Muted, fontSize = 12.sp)) }
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
                drawLine(Muted, Offset(0.18f * w, 0.52f * w), Offset(0.42f * w, 0.76f * w), line.width, StrokeCap.Round)
                drawLine(Muted, Offset(0.42f * w, 0.76f * w), Offset(0.84f * w, 0.26f * w), line.width, StrokeCap.Round)
            }
            Mark.TODO -> drawCircle(Dim, radius = 0.4f * w, style = line)
            // Machinery that failed: the coral dot every failed state uses.
            Mark.FAILED -> drawCircle(Coral, radius = 3.dp.toPx())
        }
    }
}
