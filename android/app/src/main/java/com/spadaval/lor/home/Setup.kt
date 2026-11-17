package com.spadaval.lor.home

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.spadaval.lor.MainActivity
import com.spadaval.lor.agent.pairCalls
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.isHome
import com.spadaval.lor.net.LorClient
import com.spadaval.lor.rememberAskForHome
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Home's set-up card (launcher polish 3.5), the app's half: each step checked live (the home role,
 * T3's pairing, a read of LoR), and whether the card shows at all (`Prefs.setupDone`). Its drawing
 * and the order of its steps are homecore's (`SetupView.kt`, lor-rgx4).
 */

/** Paired, whether or not the connection is up just now; a refused session needs pairing again. */
fun Setup.paired(status: T3Connection.Status): Boolean =
    status !is T3Connection.Status.NotPaired && status !is T3Connection.Status.Rejected

@Composable
internal fun SetupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var shown by remember { mutableStateOf(!prefs.setupDone) }
    if (!shown) return
    val connection = remember { T3Connection.get(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val status by connection.status.collectAsState()
    var home by remember { mutableStateOf(isHome(context)) }
    var lor by remember { mutableStateOf<LorReach>(LorReach.Checking) }
    var checks by remember { mutableIntStateOf(0) }
    // Whether a read of LoR has ended yet: only the first is waited for before drawing.
    var checked by remember { mutableStateOf(false) }
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
        checked = true
    }
    val left = Setup.left(home, Setup.paired(status), lor)
    LaunchedEffect(left.isEmpty()) {
        if (left.isEmpty()) {
            prefs.setupDone = true
            shown = false
        }
    }
    val ask = rememberAskForHome { home = isHome(context) }
    // An install that already has the role and T3 (an update to a set-up phone) waits for the first
    // read of LoR before drawing, so the card doesn't flash up and go when that read succeeds. Later
    // checks (Check again, a resume) keep the card drawn, showing the step's own "Checking".
    if (home && Setup.paired(status) && !checked) return
    val where = Uri.parse(address).authority ?: address
    SetupCard(
        SetupUi(home, Setup.paired(status), lor, where),
        SetupActions(
            skip = {
                prefs.setupDone = true
                shown = false
            },
            makeHome = ask,
            checkAgain = { checks++ },
            changeAddress = { MainActivity.open(context, MainActivity.SETTINGS) },
            pair = remember(connection) { pairCalls(context, connection, scope) },
        ),
        modifier,
    )
}
