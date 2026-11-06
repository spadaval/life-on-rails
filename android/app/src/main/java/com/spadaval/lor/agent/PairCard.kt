package com.spadaval.lor.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.t3.PairScan
import com.spadaval.lor.t3.Pairing
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pairing with T3 where it's needed (launcher polish 3.3): on the agent page while T3 isn't paired
 * or refused this phone, and in home's set-up card. Scan T3's QR code ([PairScan]) or paste its
 * pairing link; either pairs at once, and the card goes when the connection does. A link only: a
 * bare token needs a server address, which the debug screen in LoR's Settings still takes.
 */
@Composable
fun PairCard(connection: T3Connection, status: T3Connection.Status, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pasting by rememberSaveable { mutableStateOf(false) }
    var link by rememberSaveable { mutableStateOf("") }
    var pairing by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    fun pair(code: String) {
        if (!code.contains("://")) {
            failure = "That isn't a T3 pairing link."
            return
        }
        pairing = true
        failure = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { connection.pair(Pairing.parse("", code)) } }
            pairing = false
            result.fold({ link = ""; pasting = false }, { failure = "Can't pair: ${T3Connection.describe(it)}" })
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(CardFill, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (status is T3Connection.Status.Rejected) "T3 refused this phone: ${status.error}. Pair again." else "Pair with T3",
            style = Body.copy(fontSize = 14.sp),
        )
        Text("Scan the code in T3's Connections settings, or paste its pairing link.", style = Small)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(if (pairing) "Pairing…" else "Scan code", primary = true, enabled = !pairing) {
                failure = null
                PairScan.scan(context, { found, _ -> pair(found) }, { failure = it })
            }
            Pill(if (pasting) "Hide" else "Paste a link", enabled = !pairing) { pasting = !pasting }
        }
        if (pasting) {
            Field(link, "Pairing link") { link = it; failure = null }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Pill("Pair", enabled = link.isNotBlank() && !pairing) { pair(link) }
            }
        }
        failure?.let { Text(it, style = Small.copy(color = Red)) }
    }
}
