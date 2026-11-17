package com.spadaval.lor.concepts.queue

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
 * Queue's button, with every state: hover and focus lighten it, a press darkens and shrinks it a
 * little, disabled fades it, and [busy] says so in its words instead of a spinner.
 */
@Composable
fun QButton(
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
        Kind.PRIMARY -> Q.amber to Q.onAmber
        Kind.PLAIN -> Q.wash2 to Q.text1
        Kind.QUIET -> Color.Transparent to Q.text2
        Kind.DANGER -> Q.redSoft to Q.red
    }
    val lift = when { pressed -> -0.06f; hovered -> 0.06f; else -> 0f }
    val bg by animateColorAsState(if (kind == Kind.QUIET && (hovered || pressed)) Q.wash2 else lighten(fill, lift), tween(120))
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120))
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .heightIn(min = height)
            .clip(Q.rMd)
            .background(bg)
            .hoverable(source)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (kind == Kind.QUIET) 12.dp else 18.dp)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) { Icon(glyph, ink, size = 18.dp, weight = 2f); if (text.isNotEmpty()) Spacer(Modifier.width(8.dp)) }
        if (text.isNotEmpty()) BasicText(text, style = Q.body.copy(color = ink, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** A round button holding only an icon, 44 dp so it is easy to hit; [label] is its name for scripts and the screen reader. */
@Composable
fun IconButton(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Q.text2, anchor: String? = null, size: Dp = 44.dp) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    Box(
        modifier.size(size).clip(CircleShape).background(if (pressed) Q.wash2 else if (hovered) Q.wash else Color.Transparent)
            .hoverable(source).clickable(source, indication = null, onClick = onClick)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icon(glyph, tint, size = 20.dp) }
}

/** A round check, 24 dp in a 48 dp target: empty, or filled with a tick. */
@Composable
fun Check(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, anchor: String? = null) {
    val fill by animateColorAsState(if (checked) Q.text1 else Color.Transparent, tween(150))
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick).then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(fill).border(1.6.dp, if (checked) Q.text1 else Q.text3, CircleShape), contentAlignment = Alignment.Center) {
            if (checked) Icon(Glyph.Check, Q.ground, size = 16.dp, weight = 2.6f)
        }
    }
}

/** A choice in a row of them: an option of a question, a label to switch the timer to. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, detail: String? = null, anchor: String? = null, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val bg = when { selected -> Q.text1; pressed -> Q.line2; hovered -> Q.wash2; else -> Q.wash }
    Column(
        modifier.clip(Q.rMd).background(bg).border(1.dp, if (selected) Q.text1 else Q.line2, Q.rMd)
            .hoverable(source).clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp).padding(horizontal = 14.dp, vertical = if (detail != null) 9.dp else 11.dp)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(text, style = Q.body.copy(color = if (selected) Q.ground else Q.text1, fontWeight = FontWeight.Medium))
        if (detail != null) BasicText(detail, style = Q.label.copy(color = if (selected) Q.ground.copy(alpha = 0.7f) else Q.text2))
    }
}

/** A kicker: what a card is and where it comes from, in small caps, coloured by its job. */
@Composable
fun Kicker(text: String, color: Color = Q.text3, modifier: Modifier = Modifier) {
    BasicText(text.uppercase(), modifier, style = Q.caps.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A section's label in the list. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = Q.s5, bottom = Q.s2), verticalAlignment = Alignment.Bottom) {
        BasicText(text.uppercase(), Modifier.weight(1f), style = Q.caps)
        if (trailing != null) BasicText(trailing, style = Q.label.copy(color = Q.text3))
    }
}

/**
 * A line that says something is wrong or out of date, at the top of what it makes untrue: its
 * words, why, and the one action that fixes it.
 */
@Composable
fun Note(title: String, detail: String?, tone: Color, glyph: Glyph, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null, anchor: String? = null) {
    Row(
        modifier.fillMaxWidth().clip(Q.rMd).background(tone.copy(alpha = 0.10f)).border(1.dp, tone.copy(alpha = 0.28f), Q.rMd)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .then(if (anchor != null) Modifier.labAnchor(anchor) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, tone, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Q.body.copy(fontWeight = FontWeight.Medium))
            if (detail != null) BasicText(detail, style = Q.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (action != null && onAction != null) QButton(action, onAction, kind = Kind.QUIET, height = 40.dp, anchor = anchor?.let { "$it:action" })
    }
}

/** A card's frame: flat on the page, or raised with the lead's amber edge and shadow. */
fun Modifier.card(lead: Boolean = false, shape: Shape = Q.rLg): Modifier =
    if (lead) this
        .shadow(2.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
        .shadow(22.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.7f), spotColor = Color.Black.copy(alpha = 0.7f))
        .clip(shape).background(Q.surface2).border(1.dp, Q.amberEdge, shape)
    else this.clip(shape).background(Q.surface1.copy(alpha = 0.92f)).border(1.dp, Q.line, shape)

/** Text struck through when done, faded. */
@Composable
fun TaskTitle(text: String, done: Boolean, modifier: Modifier = Modifier, maxLines: Int = 2, style: androidx.compose.ui.text.TextStyle = Q.row.copy(fontWeight = FontWeight.Normal)) {
    BasicText(
        text, modifier,
        style = style.copy( color = if (done) Q.text3 else Q.text1, textDecoration = if (done) TextDecoration.LineThrough else null),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    )
}

/** A thin divider between rows. */
@Composable
fun Hairline(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Q.line))

private fun lighten(c: Color, by: Float): Color = if (c.alpha == 0f) c else if (by >= 0) Color(c.red + (1 - c.red) * by, c.green + (1 - c.green) * by, c.blue + (1 - c.blue) * by, c.alpha)
else Color(c.red * (1 + by), c.green * (1 + by), c.blue * (1 + by), c.alpha)
