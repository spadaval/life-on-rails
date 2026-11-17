package com.spadaval.lor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The parts the agent page, the LoR panel, its blocks, the set-up card and the quick-send sheet
 * draw with (lor-rgx4): the text sizes, the pill button, the text field, a section's head, and the
 * state lines (failed, unseen, unreachable with Retry, "N more").
 */

/** The panels' body text: DESIGN.md's body at 14 sp. */
val BodyText = Type.body.copy(fontSize = 14.sp)

/** The panels' small text: DESIGN.md's small body at 12 sp, muted. */
val SmallText = Type.bodySmall.copy(fontSize = 12.sp)

/**
 * A pill button: amber when [primary], else outlined; a disabled one is the same pill at 45%. It
 * stays small, and the space around it makes the touch target 48 dp, as Material's buttons do.
 * [dense] is the quick-send sheet's: no 48 dp target, one line, and when disabled only its label
 * dims (the fill and the edge stay).
 */
@Composable
fun Pill(text: String, primary: Boolean = false, enabled: Boolean = true, dense: Boolean = false, onClick: () -> Unit) {
    val shape = RectangleShape
    val base = if (primary) Modifier.background(Or, shape) else Modifier.border(1.dp, if (enabled) Edge else Line, shape)
    val style = Type.button.copy(
        color = when {
            primary -> OnOr
            enabled -> Txt
            else -> Dim
        },
        fontSize = 12.sp,
        fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
    )
    if (dense) {
        Text(
            text,
            style = style,
            maxLines = 1,
            modifier = (if (enabled) base else base.alpha(0.45f))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        )
    } else {
        Text(
            text,
            style = style,
            modifier = Modifier.minimumInteractiveComponentSize().then(if (enabled) Modifier else Modifier.alpha(0.45f)).then(base).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

/** A text field: square, edged, amber when focused, its [placeholder] dim. */
@Composable
fun Field(value: String, placeholder: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, onChange: (String) -> Unit) {
    OutlinedTextField(
        value,
        onChange,
        modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Dim) },
        textStyle = BodyText,
        shape = RectangleShape,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Txt, unfocusedTextColor = Txt, cursorColor = Or,
            focusedBorderColor = Or.copy(alpha = 0.7f), unfocusedBorderColor = Edge,
        ),
    )
}

/** A section's head: DESIGN.md's title, condensed caps, with a count or date at its end. */
@Composable
fun SectionHeader(title: String, right: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
        Text(title.uppercase(), style = Type.title.copy(color = Txt, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        right?.let { Text(it, style = Type.title) }
    }
}

/** Machinery that failed, the same everywhere (launcher polish 3.6): a coral dot and the reason in the muted colour. */
@Composable
fun Failed(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(Bad, CircleShape))
        Text(text, style = SmallText, modifier = Modifier.padding(start = 8.dp))
    }
}

/** A source a list can't see, or why it is empty, in place of its rows. */
@Composable
fun Unseen(text: String) {
    Text(text, style = SmallText, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
}

/** Why LoR can't be read, with a Retry that reads at once ([retrying] while that read runs). */
@Composable
fun Unreachable(text: String, retrying: Boolean, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(Bad, CircleShape))
        Text(text, style = SmallText, modifier = Modifier.weight(1f).padding(start = 8.dp, end = 10.dp))
        Pill(if (retrying) "Trying…" else "Retry", enabled = !retrying, onClick = onRetry)
    }
}

/** "N more" under a block's rows; a tap moves the grid to the panel that shows them all, when the layout has one. */
@Composable
fun More(count: Int, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$count more", style = SmallText.copy(color = if (onClick != null) Txt else Mute, fontSize = 13.sp))
    }
}

/** A row of a dropdown menu. */
@Composable
fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, style = Type.body.copy(color = Txt, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = onClick)
}
