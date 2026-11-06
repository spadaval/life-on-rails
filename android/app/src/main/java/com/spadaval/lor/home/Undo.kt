package com.spadaval.lor.home

import com.spadaval.lor.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/*
 * The Undo bar after an edit (launcher polish 4.2). As Kvaesitso's widget column (GPL-3.0,
 * `WidgetColumn.kt`) and Launcher3's Remove (Apache-2.0, `DropTargetHandler`) do, the edit is
 * made at once and the bar offers to take it back; here it is every edit, and taking it back
 * writes the copy of the file kept before it ([HomeStore.undo]). The look is DESIGN.md's
 * underlined "undo" in the ink colour.
 */

/**
 * What the bar says; a new one (even with the same words) restarts its time. Without [undoable]
 * it is a refusal or a notice (launcher polish 6.2: the launcher shows no toasts), and [failed]
 * draws 3.6's coral dot for machinery that failed.
 */
class UndoMessage(val text: String, val undoable: Boolean = true, val failed: Boolean = false)

/** The bar's state, held by the screen that shows it. */
class UndoBarState {
    var message by mutableStateOf<UndoMessage?>(null)

    fun show(text: String) {
        message = UndoMessage(text)
    }

    fun say(text: String, failed: Boolean) {
        message = UndoMessage(text, undoable = false, failed = failed)
    }
}

/** How long the bar stays: Material's short snackbar. */
private const val SHOWN_MS = 4_000L

@Composable
fun UndoBar(state: UndoBarState, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    val message = state.message ?: return
    LaunchedEffect(message) {
        delay(SHOWN_MS)
        if (state.message === message) state.message = null
    }
    Row(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // Opaque, unlike StateFill: it sits over icons on home and over text in Home settings.
            .background(PanelFace).border(1.dp, Edge)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.failed) Spacer(Modifier.padding(end = 10.dp).size(6.dp).background(Bad, CircleShape))
        Text(message.text, style = Type.body.copy(color = Txt, fontSize = 14.sp), modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 0.dp).padding(end = 16.dp))
        if (message.undoable) Text(
            "Undo",
            style = Type.body.copy(color = Txt, fontSize = 14.sp, textDecoration = TextDecoration.Underline),
            modifier = Modifier
                .clickable {
                    state.message = null
                    onUndo()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}
