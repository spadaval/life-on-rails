package com.spadaval.lor.concepts.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The height the stand-in keyboard takes, for laying out what sits above it. */
val KeyboardHeight = 236.dp

/*
 * A stand-in for the phone's keyboard, so "with the keyboard up" can be drawn and judged: what is
 * above it has the room the phone would leave. Its keys type into [state], as does the computer's
 * keyboard. It has Gboard's dark colours, not a concept's: it is the system's, not ours.
 */
@Composable
fun StandInKeyboard(state: TextFieldState, modifier: Modifier = Modifier, onEnter: () -> Unit = {}) {
    val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    val key = Color(0xFF3B3D43)
    val special = Color(0xFF2A2C31)
    val label = TextStyle(color = Color(0xFFE8E9EC), fontSize = 19.sp, textAlign = TextAlign.Center)
    Column(
        modifier.fillMaxWidth().height(KeyboardHeight).background(Color(0xFF1E2024)).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEachIndexed { i, row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = if (i == 1) 14.dp else 0.dp)) {
                if (i == 2) Key("\u21E7", 1.4f, special, label) {}
                row.forEach { c -> Key(c.toString(), 1f, key, label) { state.edit { append(c.toString()) } } }
                if (i == 2) Key("\u232B", 1.4f, special, label) { state.edit { if (length > 0) delete(length - 1, length) } }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Key("?123", 1.4f, special, label) {}
            Key(",", 1f, special, label) { state.edit { append(",") } }
            Key("", 4.6f, key, label) { state.edit { append(" ") } }
            Key(".", 1f, special, label) { state.edit { append(".") } }
            Key("\u21B5", 1.4f, Color(0xFF8AB4F8).copy(alpha = 0.35f), label) { onEnter() }
        }
    }
}

@Composable
private fun RowScope.Key(text: String, weight: Float, color: Color, label: TextStyle, onClick: () -> Unit) {
    Box(
        Modifier.weight(weight).height(44.dp).padding(horizontal = 3.dp).clip(RoundedCornerShape(6.dp)).background(color).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { BasicText(text, style = label.copy(fontSize = if (text.length > 1) 14.sp else 19.sp)) }
}
