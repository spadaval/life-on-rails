package com.spadaval.lor.concepts.panes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spadaval.lor.concepts.kit.Glyph
import com.spadaval.lor.concepts.kit.Icon
import com.spadaval.lor.lab.labAnchor

/** A button's weight: the one main action of a pane, a plain one, a quiet one, or one that warns. */
enum class Kind { PRIMARY, PLAIN, QUIET, DANGER, AGENT }

private fun Modifier.anchored(name: String?) = if (name != null) labAnchor(name) else this

/**
 * Panes' button, with every state: hover lightens it, a press darkens and shrinks it a little,
 * disabled fades it. [compact] is the row's size (40 dp), the default the pane's (44 dp).
 */
@Composable
fun PButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: Kind = Kind.PLAIN,
    glyph: Glyph? = null,
    enabled: Boolean = true,
    anchor: String? = null,
    compact: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val (fill, ink, edge) = when (kind) {
        Kind.PRIMARY -> Triple(P.amber, P.amberInk, Color.Transparent)
        Kind.PLAIN -> Triple(P.raise2, P.text1, P.line2)
        Kind.QUIET -> Triple(Color.Transparent, P.text2, Color.Transparent)
        Kind.DANGER -> Triple(P.redSoft, P.red, P.redEdge)
        Kind.AGENT -> Triple(P.cyanSoft, P.cyan, P.cyanEdge)
    }
    val lift = when { !enabled -> 0f; pressed -> -0.08f; hovered -> 0.07f; else -> 0f }
    val bg by animateColorAsState(if (kind == Kind.QUIET && (hovered || pressed) && enabled) P.raise2 else shade(fill, lift), tween(120))
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, tween(120))
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.38f }
            .heightIn(min = if (compact) 40.dp else 44.dp)
            .clip(P.rSm)
            .background(bg)
            .border(1.dp, edge, P.rSm)
            .hoverable(source)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (kind == Kind.QUIET) 10.dp else if (compact) 14.dp else 18.dp)
            .anchored(anchor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) { Icon(glyph, ink, size = 16.dp, weight = 2f); if (text.isNotEmpty()) Spacer(Modifier.width(8.dp)) }
        if (text.isNotEmpty()) BasicText(text, style = P.small.copy(color = ink, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** A round button holding only an icon, 44 dp so it is easy to hit; [label] names it for scripts. */
@Composable
fun IconButton(glyph: Glyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = P.text2, anchor: String? = null, size: Dp = 44.dp, fill: Color = Color.Transparent, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    Box(
        modifier.size(size).graphicsLayer { alpha = if (enabled) 1f else 0.38f }.clip(CircleShape)
            .background(if (pressed && enabled) P.raise2 else if (hovered && enabled) P.raise else fill)
            .hoverable(source).clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .anchored(anchor ?: "button:$label"),
        contentAlignment = Alignment.Center,
    ) { Icon(glyph, tint, size = 20.dp) }
}

/** A square check with rounded corners, 22 dp in a 48 dp target, as the dashboard's. */
@Composable
fun Check(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, anchor: String? = null, enabled: Boolean = true) {
    val fill by animateColorAsState(if (checked) P.amber else Color.Transparent, tween(150))
    Box(
        modifier.size(48.dp).graphicsLayer { alpha = if (enabled) 1f else 0.38f }.clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick).anchored(anchor),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(22.dp).clip(P.rXs).background(fill).border(1.5.dp, if (checked) P.amber else P.line3, P.rXs), contentAlignment = Alignment.Center) {
            if (checked) Icon(Glyph.Check, P.amberInk, size = 16.dp, weight = 2.6f)
        }
    }
}

/** A choice among several: an option of a question (with its description), a label for the timer. */
@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, detail: String? = null, anchor: String? = null, enabled: Boolean = true, multi: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val bg = when { selected -> P.amberSoft; pressed -> P.raise2; hovered -> P.raise; else -> Color.Transparent }
    Row(
        modifier.clip(P.rMd).background(bg).border(1.dp, if (selected) P.amberEdge else P.line2, P.rMd)
            .hoverable(source).clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .defaultMinSize(minHeight = 48.dp).padding(horizontal = 14.dp, vertical = if (detail != null) 10.dp else 12.dp)
            .anchored(anchor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multi) {
            Box(Modifier.size(18.dp).clip(P.rXs).background(if (selected) P.amber else Color.Transparent).border(1.5.dp, if (selected) P.amber else P.line3, P.rXs), contentAlignment = Alignment.Center) {
                if (selected) Icon(Glyph.Check, P.amberInk, size = 14.dp, weight = 2.6f)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f, fill = false)) {
            BasicText(text, style = P.body.copy(fontWeight = FontWeight.Medium, color = P.text1))
            if (detail != null) BasicText(detail, style = P.small.copy(color = P.text2))
        }
    }
}

/** A pill-sized choice in a row of them (a project, a timer label). */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, anchor: String? = null, enabled: Boolean = true, glyph: Glyph? = null) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier.clip(P.rPill).background(if (selected) P.amberSoft else if (pressed) P.raise2 else P.raise)
            .border(1.dp, if (selected) P.amberEdge else P.line2, P.rPill)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .heightIn(min = 40.dp).padding(horizontal = 14.dp).anchored(anchor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) { Icon(glyph, if (selected) P.amber else P.text2, size = 16.dp); Spacer(Modifier.width(6.dp)) }
        BasicText(text, style = P.small.copy(color = if (selected) P.text1 else P.text1, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** One of a few, side by side in a well, as the dashboard's due-date control: the chosen one raised. */
@Composable
fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onPick: (T) -> Unit, modifier: Modifier = Modifier, anchor: String = "seg", enabled: Boolean = true) {
    Row(modifier.clip(P.rMd).background(P.well).border(1.dp, P.line, P.rMd).padding(3.dp).graphicsLayer { alpha = if (enabled) 1f else 0.38f }) {
        options.forEach { (label, value) ->
            val on = value == selected
            val bg by animateColorAsState(if (on) P.raise2 else Color.Transparent, tween(150))
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(P.rSm).background(bg)
                    .then(if (on) Modifier.border(1.dp, P.line2, P.rSm) else Modifier)
                    .clickable(enabled = enabled) { onPick(value) }.padding(horizontal = 4.dp).labAnchor("$anchor:$label"),
                contentAlignment = Alignment.Center,
            ) { BasicText(label, style = P.small.copy(color = if (on) P.text1 else P.text2, fontWeight = FontWeight.Medium), maxLines = 1) }
        }
    }
}

/** A count beside a label: "Needs you  2". */
@Composable
fun Count(n: Int, tone: Color = P.text2, modifier: Modifier = Modifier) {
    Box(modifier.clip(P.rPill).background(P.raise2).padding(horizontal = 7.dp, vertical = 1.dp), contentAlignment = Alignment.Center) {
        BasicText("$n", style = P.meta.copy(color = tone, fontWeight = FontWeight.Medium))
    }
}

/** A group's label in a list, with its count: sentence case, quiet, as the dashboard has it. */
@Composable
fun GroupLabel(text: String, count: Int? = null, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = P.s4, bottom = P.s1).heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(text, style = P.group)
        if (count != null && count > 0) { Spacer(Modifier.width(8.dp)); Count(count) }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** A kicker over a pane's title: what the open thing is, in its job's colour. */
@Composable
fun Kicker(text: String, color: Color = P.text3, modifier: Modifier = Modifier) {
    BasicText(text.uppercase(), modifier, style = P.meta.copy(color = color, fontWeight = FontWeight.SemiBold, letterSpacing = androidx.compose.ui.unit.TextUnit(0.08f, androidx.compose.ui.unit.TextUnitType.Em)), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * A line that says something is wrong or out of date, at the top of what it makes untrue: what,
 * why, and the one action that fixes it.
 */
@Composable
fun Note(title: String, detail: String?, tone: Color, glyph: Glyph, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null, anchor: String? = null) {
    Row(
        modifier.fillMaxWidth().clip(P.rMd).background(tone.copy(alpha = 0.09f)).border(1.dp, tone.copy(alpha = 0.3f), P.rMd)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp).anchored(anchor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, tone, size = 18.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = P.small.copy(color = P.text1, fontWeight = FontWeight.Medium))
            if (detail != null) BasicText(detail, style = P.meta, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (action != null && onAction != null) PButton(action, onAction, kind = Kind.QUIET, compact = true, anchor = anchor?.let { "$it:action" })
    }
}

/** Text struck through when done, faded. */
@Composable
fun TaskTitle(text: String, done: Boolean, modifier: Modifier = Modifier, maxLines: Int = 2, style: TextStyle = P.body) {
    BasicText(
        text, modifier,
        style = style.copy(color = if (done) P.text3 else P.text1, textDecoration = if (done) TextDecoration.LineThrough else null),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(P.line))

/**
 * A text field in Panes' style: a well, a placeholder, and the stand-in keyboard while it has
 * focus. Its [state] lives in [PanesUi], so what is typed survives a fold.
 */
@Composable
fun Field(
    ui: PanesUi,
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    anchor: String? = null,
    lines: Int = 1,
    onEnter: (() -> Unit)? = null,
    leading: Glyph? = null,
    enabled: Boolean = true,
    pill: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    // The stand-in keyboard's Return calls ui.onEnter: keep it the latest callback while focused,
    // since one made at focus would act on what was typed then.
    SideEffect { if (focused && ui.focused === state) ui.onEnter = onEnter }
    val edge by animateColorAsState(if (focused) P.focus.copy(alpha = 0.6f) else P.line2, tween(150))
    val shape = if (pill) P.rPill else P.rMd
    BasicTextField(
        state,
        modifier.fillMaxWidth().onFocusChanged { f ->
            focused = f.isFocused
            if (f.isFocused) { ui.focused = state; ui.onEnter = onEnter } else if (ui.focused === state) ui.focused = null
        }.anchored(anchor),
        enabled = enabled,
        textStyle = P.body,
        cursorBrush = SolidColor(P.amber),
        lineLimits = if (lines == 1) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(1, lines),
        onKeyboardAction = { onEnter?.invoke() },
        decorator = { inner ->
            Row(
                Modifier.fillMaxWidth().clip(shape).background(if (focused) P.well.copy(alpha = 0.35f) else P.well)
                    .border(1.dp, edge, shape).heightIn(min = 48.dp).padding(horizontal = if (pill) 18.dp else 14.dp, vertical = 12.dp),
                verticalAlignment = if (lines == 1) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (leading != null) { Icon(leading, P.text2, size = 18.dp); Spacer(Modifier.width(10.dp)) }
                Box(Modifier.weight(1f)) {
                    if (state.text.isEmpty()) BasicText(placeholder, style = P.body.copy(color = P.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            }
        },
    )
}

/** A switch: the platform's control, drawn in Panes' colours. */
@Composable
fun Toggle(title: String, detail: String?, on: Boolean, anchor: String, detailTone: Color = P.text2, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.heightIn(min = 56.dp).padding(vertical = 8.dp).labAnchor(anchor), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = P.body)
            if (detail != null) BasicText(detail, style = P.meta.copy(color = detailTone))
        }
        val x by animateDpAsState(if (on) 22.dp else 2.dp, tween(150))
        Box(Modifier.size(48.dp, 28.dp).clip(P.rPill).background(if (on) P.amber else P.raise2).border(1.dp, if (on) Color.Transparent else P.line2, P.rPill)) {
            Box(Modifier.padding(start = x, top = 2.dp).size(24.dp).clip(CircleShape).background(if (on) P.amberInk else P.text2))
        }
    }
}

/** The visible ⋯ that opens a row's or a pane's menu, so nothing is only behind a long press. */
@Composable
fun MoreButton(ui: PanesUi, menu: (Rect) -> Menu, anchor: String, size: Dp = 40.dp) {
    var at by remember { mutableStateOf(Rect.Zero) }
    IconButton(Glyph.More, "More", { ui.menu = menu(at) }, modifier = Modifier.onGloballyPositioned { at = it.boundsInRoot() }, anchor = anchor, size = size)
}

/** A thin bar down a row's or a pane's leading edge: amber for the open item, cyan for the agent's. */
fun Modifier.edge(color: Color, width: Dp = 3.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.18f), size = androidx.compose.ui.geometry.Size(w, size.height * 0.64f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2))
}

private fun shade(c: Color, by: Float): Color = if (c.alpha == 0f) c else if (by >= 0) Color(c.red + (1 - c.red) * by, c.green + (1 - c.green) * by, c.blue + (1 - c.blue) * by, c.alpha)
else Color(c.red * (1 + by), c.green * (1 + by), c.blue * (1 + by), c.alpha)
