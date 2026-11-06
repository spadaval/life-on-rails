package com.spadaval.lor.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/*
 * A broken layout file (launcher polish 3.1): home keeps drawing the last good layout, and a
 * one-line banner says so. A tap shows the parser's error, which names the place, and offers to
 * put the file right: the last good layout, or the default when there is none.
 */

/** Coral: machinery that failed (DESIGN.md's colour jobs), never "nothing to show". */
internal val Coral = Color(0xFFE2806F)

/** The banner's line for [state]. */
internal fun brokenLine(state: LayoutState.Broken): String =
    if (state.lastGood != null) "layout.json has an error. Showing the last good layout." else "layout.json has an error."

/**
 * The banner: a coral status dot and one line in [style]; a tap opens [BrokenLayoutDialog].
 * [appsLoaded] says whether the default layout can be made, for a file with no last good layout.
 */
@Composable
internal fun BrokenBanner(state: LayoutState.Broken, appsLoaded: Boolean, style: TextStyle, modifier: Modifier = Modifier, restore: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier.clickable(onClickLabel = "Show the error") { open = true }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(6.dp).background(Coral, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(brokenLine(state), style = style)
    }
    if (open) BrokenLayoutDialog(state, appsLoaded, { open = false; restore() }) { open = false }
}

@Composable
private fun BrokenLayoutDialog(state: LayoutState.Broken, appsLoaded: Boolean, restore: () -> Unit, close: () -> Unit) {
    val action = when {
        state.lastGood != null -> "Use the last good layout"
        appsLoaded -> "Start from the default layout"
        else -> null
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("layout.json has an error") },
        text = {
            Column {
                Text(state.message)
                if (action != null) {
                    Text(
                        "$action sets the broken file aside as layout.json.broken.",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = { if (action != null) TextButton(onClick = restore) { Text(action) } },
        dismissButton = { TextButton(onClick = close) { Text("Close") } },
    )
}
