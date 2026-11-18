package com.spadaval.lor.concepts.timeline

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.labAnchor

/** A button's weight: the one primary action on the lead card, a plain one, a quiet one, or one that warns. */
enum class Kind { PRIMARY, PLAIN, QUIET, DANGER }

/**
 * Timeline's button, with every state: hover and focus lighten it, a press darkens and shrinks it a
 * little, disabled fades it, and [busy] says so in its words instead of a spinner.
 */
@Composable
fun TButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: Kind = Kind.PLAIN,
    glyph: Glyph? = null,
    enabled: Boolean = true,
    anchor: String? = null,
    height: Dp = 44.dp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val (fill, ink) = when (kind) {
        Kind.PRIMARY -> T.amber to T.onAmber
        Kind.PLAIN -> T.wash2 to T.text1
        Kind.QUIET -> Color.Transparent to T.text2
        Kind.DANGER -> T.redSoft to T.red
    }
    val lift = when { pressed -> -0.06f; hovered -> 0.06f; else -> 0f }
    val bg by animateColorAsState(if (kind == Kind.QUIET && (hovered || pressed)) T.wash2 else lighten(fill, lift), tween(120))
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120))
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .heightIn(min = height)
            .clip(T.rMd)
            .background(bg)
            .hoverable(source)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (kind == Kind.QUIET) 12.dp else 18.dp)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) { Icon(glyph, ink, size = 18.dp, weight = 2f); if (text.isNotEmpty()) Spacer(Modifier.width(8.dp)) }
        if (text.isNotEmpty()) BasicText(text, style = T.body.copy(color = ink, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** A round button holding only an icon, 44 dp so it is easy to hit; [label] is its name for scripts and the screen reader. */
@Composable
fun IconButton(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = T.text2, anchor: String? = null, size: Dp = 44.dp) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    Box(
        modifier.size(size).clip(CircleShape).background(if (pressed) T.wash2 else if (hovered) T.wash else Color.Transparent)
            .hoverable(source).clickable(source, indication = null, onClick = onClick)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icon(glyph, tint, size = 20.dp) }
}

/** A round check, 24 dp in a 48 dp target: empty, or filled with a tick. */
@Composable
fun Check(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, anchor: String? = null, enabled: Boolean = true) {
    val fill by animateColorAsState(if (checked) T.text1 else Color.Transparent, tween(150))
    Box(
        modifier.size(48.dp).graphicsLayer { alpha = if (enabled) 1f else 0.4f }.clip(CircleShape).clickable(enabled = enabled, onClick = onClick).then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(fill).border(1.6.dp, if (checked) T.text1 else T.text3, CircleShape), contentAlignment = Alignment.Center) {
            if (checked) Icon(Glyph.Check, T.ground, size = 16.dp, weight = 2.6f)
        }
    }
}

/** A choice in a row of them: an option of a question, a label to switch the timer to. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, detail: String? = null, anchor: String? = null, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val bg = when { selected -> T.text1; pressed -> T.line2; hovered -> T.wash2; else -> T.wash }
    Column(
        modifier.clip(T.rMd).background(bg).border(1.dp, if (selected) T.text1 else T.line2, T.rMd)
            .hoverable(source).clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp).padding(horizontal = 14.dp, vertical = if (detail != null) 9.dp else 11.dp)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(text, style = T.body.copy(color = if (selected) T.ground else T.text1, fontWeight = FontWeight.Medium))
        if (detail != null) BasicText(detail, style = T.label.copy(color = if (selected) T.ground.copy(alpha = 0.7f) else T.text2))
    }
}

/** A kicker: what a card is and where it comes from, in small caps, coloured by its job. */
@Composable
fun Kicker(text: String, color: Color = T.text3, modifier: Modifier = Modifier) {
    BasicText(text.uppercase(), modifier, style = T.caps.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A section's label in the list. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = T.s5, bottom = T.s2), verticalAlignment = Alignment.Bottom) {
        BasicText(text.uppercase(), Modifier.weight(1f), style = T.caps)
        if (trailing != null) BasicText(trailing, style = T.label.copy(color = T.text3))
    }
}

/**
 * A line that says something is wrong or out of date, at the top of what it makes untrue: its
 * words, why, and the one action that fixes it.
 */
@Composable
fun Note(title: String, detail: String?, tone: Color, glyph: Glyph, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null, anchor: String? = null) {
    Row(
        modifier.fillMaxWidth().clip(T.rMd).background(tone.copy(alpha = 0.10f)).border(1.dp, tone.copy(alpha = 0.28f), T.rMd)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, tone, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = T.body.copy(fontWeight = FontWeight.Medium))
            if (detail != null) BasicText(detail, style = T.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (action != null && onAction != null) TButton(action, onAction, kind = Kind.QUIET, height = 40.dp, anchor = anchor?.let { "$it:action" })
    }
}

/** A card's frame: flat on the page, or raised with the lead's amber edge and shadow. */
fun Modifier.card(lead: Boolean = false, shape: Shape = T.rLg): Modifier =
    if (lead) this
        .shadow(2.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
        .shadow(22.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.7f), spotColor = Color.Black.copy(alpha = 0.7f))
        .clip(shape).background(T.surface2).border(1.dp, T.amberEdge, shape)
    else this.clip(shape).background(T.surface1.copy(alpha = 0.92f)).border(1.dp, T.line, shape)

/** Text struck through when done, faded. */
@Composable
fun TaskTitle(text: String, done: Boolean, modifier: Modifier = Modifier, maxLines: Int = 2, style: androidx.compose.ui.text.TextStyle = T.row.copy(fontWeight = FontWeight.Normal)) {
    BasicText(
        text, modifier,
        style = style.copy( color = if (done) T.text3 else T.text1, textDecoration = if (done) TextDecoration.LineThrough else null),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    )
}

/** A thin divider between rows. */
@Composable
fun Hairline(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(T.line))

private fun lighten(c: Color, by: Float): Color = if (c.alpha == 0f) c else if (by >= 0) Color(c.red + (1 - c.red) * by, c.green + (1 - c.green) * by, c.blue + (1 - c.blue) * by, c.alpha)
else Color(c.red * (1 + by), c.green * (1 + by), c.blue * (1 + by), c.alpha)
