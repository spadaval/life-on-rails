package com.spadaval.lor.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.DismissColors

/**
 * A broken layout file (launcher polish 3.1) as its banner says it: the banner's [line], the
 * parser's [message], why the last good file can't be put back as it is ([leftOut], or null), and
 * the restore its dialog offers ([action], or null for none). The app's BrokenLayout.kt writes
 * these from the file's state.
 */
data class BrokenUi(val line: String, val message: String, val leftOut: String? = null, val action: String? = null)

/** The banner: a coral status dot, one line in [style], and "Details", at least 48 dp tall; a tap opens its dialog. */
@Composable
fun BrokenBanner(ui: BrokenUi, style: TextStyle, modifier: Modifier = Modifier, restore: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier.clickable(onClickLabel = "Show the error") { open = true }.heightIn(min = 48.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(6.dp).background(Bad, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(ui.line, style = style, modifier = Modifier.weight(1f))
        Text("Details", style = style.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(start = 12.dp))
    }
    if (open) BrokenLayoutDialog(ui, { open = false; restore() }) { open = false }
}

@Composable
private fun BrokenLayoutDialog(ui: BrokenUi, restore: () -> Unit, close: () -> Unit) {
    val action = ui.action
    AlertDialog(
        onDismissRequest = close,
        title = { Text("layout.json has an error") },
        text = {
            Column {
                Text(ui.message)
                if (ui.leftOut != null) Text(ui.leftOut, modifier = Modifier.padding(top = 12.dp))
                if (action != null) {
                    Text(
                        "$action sets the broken file aside as layout.json.broken.",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = { if (action != null) TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = restore) { Text(action) } },
        dismissButton = { TextButton(shape = androidx.compose.ui.graphics.RectangleShape, colors = DismissColors, onClick = close) { Text("Close") } },
    )
}
