package com.spadaval.lor.concepts.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.spadaval.lor.home.BackHandler
import com.spadaval.lor.lab.labAnchor

/** A menu entry: words, an icon, and what it does; [danger] draws it in the look's warning colour. */
class MenuItem(val label: String, val glyph: Glyph? = null, val danger: Boolean = false, val enabled: Boolean = true, val onClick: () -> Unit)

/*
 * A menu that floats beside what opened it: below [anchor] when it fits, above it when not, kept on
 * the screen. [anchor] is in the phone screen's coordinates (`boundsInRoot`). A tap outside or Back
 * closes it. Anchors for scripts: `menu` and `menu:<label>`.
 *
 * [header] is drawn above the entries (an app's name, a card's title); [above] is what a concept
 * draws over the anchor while the menu is open (the icon, lifted).
 */
@Composable
fun Menu(anchor: Rect, items: List<MenuItem>, onDismiss: () -> Unit, header: (@Composable () -> Unit)? = null, danger: Color = Color(0xFFEF7A5F)) {
    val look = LocalKitLook.current
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(160)) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val d = LocalDensity.current
        val w = with(d) { maxWidth.toPx() }
        val h = with(d) { maxHeight.toPx() }
        val gap = with(d) { 8.dp.toPx() }
        val below = anchor.bottom + gap + size.height <= h - with(d) { 24.dp.toPx() }
        val y = if (below) anchor.bottom + gap else (anchor.top - gap - size.height).coerceAtLeast(gap)
        val x = (anchor.center.x - size.width / 2f).coerceIn(gap, (w - size.width - gap).coerceAtLeast(gap))
        Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { onDismiss() })
        val shape = RoundedCornerShape(look.radius * 0.75f)
        Column(
            Modifier.offset { IntOffset(x.toInt(), y.toInt()) }
                .onSizeChanged { size = it }
                .graphicsLayer {
                    alpha = shown.value
                    val s = 0.92f + 0.08f * shown.value
                    scaleX = s; scaleY = s
                    transformOrigin = TransformOrigin(((anchor.center.x - x) / size.width.coerceAtLeast(1)).coerceIn(0f, 1f), if (below) 0f else 1f)
                }
                .width(248.dp)
                .clip(shape)
                .background(look.glass)
                .border(1.dp, look.glassLine, shape)
                .padding(vertical = 6.dp)
                .labAnchor("menu"),
        ) {
            if (header != null) { header() }
            items.forEach { item ->
                val color = when { !item.enabled -> look.muted.copy(alpha = 0.5f); item.danger -> danger; else -> look.text }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(enabled = item.enabled) { item.onClick() }
                        .padding(horizontal = 16.dp)
                        .labAnchor("menu:${item.label}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.glyph != null) { Icon(item.glyph, color.copy(alpha = if (item.danger) 1f else 0.8f), size = 20.dp); Spacer(Modifier.width(14.dp)) }
                    BasicText(item.label, style = look.body.copy(color = color))
                }
            }
        }
        BackHandler(true) { onDismiss() }
    }
}
