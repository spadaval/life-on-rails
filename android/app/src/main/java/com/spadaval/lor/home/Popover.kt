package com.spadaval.lor.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.ui.Chamfer
import com.spadaval.lor.ui.OrDeep
import com.spadaval.lor.ui.Line
import com.spadaval.lor.ui.Motion
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Home's long-press menus as one popover (lor-fhk5, the mock's frame 4 in
 * docs/missions/launcher-use/mocks/drawer-search-popover.html): centred on what was pressed, with
 * an arrow on its centre, a chamfered frame (DESIGN.md's panel), a shadow, a light dim over home
 * and an amber ring on the source. After Launcher3's `ArrowPopup` (Apache-2.0), which centres its
 * popup on the icon, keeps it inside the screen and moves its arrow to stay on the icon.
 *
 * It is drawn in home's own window by [PopoverLayer] at home's root, not in a popup window, so it
 * and what it points at share one set of coordinates.
 */

/** Where a popover goes, in px: its body's box, and the arrow's tip. */
data class PopoverPlacement(
    val left: Float,
    /** The body's top, under the arrow when [below]. */
    val bodyTop: Float,
    val width: Float,
    /** The body's height: its own, or the room on its side when that is less (then it scrolls). */
    val height: Float,
    /** The body is below the anchor, and the arrow on its top edge. */
    val below: Boolean,
    /** The arrow's tip: x from the body's left (on the source, so it may lie past the body's edge), y in the area's coordinates. */
    val tipX: Float,
    val tipY: Float,
    /** The middle of the arrow's base, from the body's left: on the body's straight edge, under the tip when it can be. */
    val baseX: Float,
)

object PopoverPlace {
    /**
     * Places a body [width] × [height] for [anchor] inside [area] (all px, one coordinate space).
     * The body is centred on the anchor's centre, kept [margin] inside the area. It opens below an
     * anchor whose centre is in the area's top half and above one in the bottom half. That side is
     * always the roomier one, so a body it can't hold fits the other side no better: it is clamped
     * to the room and scrolls, but never below [minHeight] (or the area): with less room than that
     * (a press at the screen's edge, a widget filling the panel) the body moves in over the anchor.
     * The arrow ([arrowHeight] tall, [arrowHalf] wide each side) starts [gap] from the anchor. Its
     * tip is always on the anchor's centre; its base stays on the body's straight edge ([chamfer]
     * is the cut corner on the edge it sits on), so near a screen edge the arrow leans.
     */
    fun place(anchor: Bounds, width: Float, height: Float, area: Bounds, margin: Float, arrowHalf: Float, arrowHeight: Float, gap: Float, chamfer: Float, minHeight: Float = 0f): PopoverPlacement {
        val w = min(width, area.right - area.left - 2 * margin)
        val cx = (anchor.left + anchor.right) / 2
        val left = (cx - w / 2).coerceIn(area.left + margin, max(area.left + margin, area.right - margin - w))
        val roomBelow = max(0f, area.bottom - margin - (anchor.bottom + gap + arrowHeight))
        val roomAbove = max(0f, (anchor.top - gap - arrowHeight) - (area.top + margin))
        val below = (anchor.top + anchor.bottom) / 2 < (area.top + area.bottom) / 2
        val most = max(0f, area.bottom - area.top - 2 * margin - arrowHeight)
        val h = min(height, max(if (below) roomBelow else roomAbove, min(minHeight, most)))
        // The cut corner is the top-left one (on the top edge, where the arrow sits when below) and
        // the bottom-right one (on the bottom edge, when above).
        val lo = arrowHalf + if (below) chamfer else 0f
        val hi = w - arrowHalf - if (below) 0f else chamfer
        val tipX = cx - left
        val baseX = tipX.coerceIn(lo, max(lo, hi))
        // Kept inside the area when the minimum height took more than the room: the arrow goes with it.
        val bodyTop = if (below) min(anchor.bottom + gap + arrowHeight, area.bottom - margin - h)
        else max(anchor.top - gap - arrowHeight - h, area.top + margin)
        val tipY = if (below) bodyTop - arrowHeight else bodyTop + h + arrowHeight
        return PopoverPlacement(left, bodyTop, w, h, below, tipX, tipY, baseX)
    }
}

/** How the source is ringed in amber: an icon's rounded square, or nothing (the finger). */
enum class PopoverRing { ICON, BOX, NONE }

/** One open popover: what it points at (in the window) and what it holds. */
class PopoverEntry {
    var anchor: () -> Bounds? = { null }
    /** What the ring goes round, when not the anchor (a widget's box, when the menu points at the press). */
    var source: (() -> Bounds?)? = null
    var ring: PopoverRing = PopoverRing.ICON
    var close: () -> Unit = {}
    var content: (@Composable ColumnScope.() -> Unit)? by mutableStateOf(null)
}

/** Home's one open popover, which [PopoverLayer] draws. */
class PopoverHost {
    var open by mutableStateOf<PopoverEntry?>(null)
}

/**
 * Opens a popover on [host] while this is composed: it points at [anchor] (in the window, read when
 * it opens) and rings [source] (the anchor when null), and [close] is called on a tap outside it,
 * back, or a change of home's size (a fold, a rotation), which would leave it pointing at where
 * things were. The caller decides when it is open, as it did with `DropdownMenu`.
 */
@Composable
fun Popover(host: PopoverHost, anchor: () -> Bounds?, close: () -> Unit, ring: PopoverRing = PopoverRing.ICON, source: (() -> Bounds?)? = null, content: @Composable ColumnScope.() -> Unit) {
    val entry = remember { PopoverEntry() }
    entry.anchor = anchor
    entry.source = source
    entry.ring = ring
    entry.close = close
    SideEffect { entry.content = content }
    DisposableEffect(entry) {
        host.open = entry
        onDispose { if (host.open === entry) host.open = null }
    }
}

/** A popover's sizes (dp). */
private object PopoverSize {
    val width = 248.dp
    val margin = 8.dp
    val arrowHalf = 11.dp
    val arrowHeight = 10.dp
    val gap = 4.dp
    val chamfer = 6.dp
    val ringOut = 5.dp
    /** The least of a body that a cramped side still shows: two rows, the rest by scrolling. */
    val minHeight = 104.dp
}

/** The dim over home behind a popover: light, so home still reads under it. */
private val PopoverDim = Color(0xFF050608).copy(alpha = 0.35f)

/** Draws [host]'s open popover over everything else on home; put it last in home's root box. */
@Composable
fun PopoverLayer(host: PopoverHost) {
    val entry = host.open ?: return
    val content = entry.content ?: return
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing
    var origin by remember { mutableStateOf(Offset.Zero) }
    val anchorInWindow = remember(entry) { entry.anchor() }
    val sourceInWindow = remember(entry) { entry.source?.invoke() ?: anchorInWindow }
    // The size home had when it opened: a fold or a rotation keeps this composition but moves everything.
    val openedAt = remember(entry) { arrayOfNulls<IntSize>(1) }
    // A change of density, font scale or screen size can lay the icons out again at the same pixel size,
    // so the anchor taken at opening would be stale: the menu closes then too (review lor-6vjm).
    val configuration = LocalConfiguration.current
    val layoutKey = listOf(density.density, density.fontScale, configuration.screenWidthDp, configuration.screenHeightDp, configuration.orientation)
    val openedWith = remember(entry) { layoutKey }
    LaunchedEffect(entry, layoutKey) { if (layoutKey != openedWith) entry.close() }
    val shown = remember(entry) { Animatable(0f) }
    LaunchedEffect(entry) { shown.animateTo(1f, Motion.quick()) }
    BackHandler { entry.close() }
    Box(
        Modifier.fillMaxSize().onPlaced { origin = it.positionInWindow() }
            .onSizeChanged { if (openedAt[0] == null) openedAt[0] = it else if (openedAt[0] != it) entry.close() }
            // Any touch outside the body closes it and reaches nothing under it, as the menu's own window did.
            .pointerInput(entry) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    do {
                        val e = awaitPointerEvent()
                        e.changes.forEach { it.consume() }
                    } while (e.changes.any { it.pressed })
                    entry.close()
                }
            },
    ) {
        val anchor = anchorInWindow?.let { Bounds(it.left - origin.x, it.top - origin.y, it.right - origin.x, it.bottom - origin.y) }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(PopoverDim, alpha = shown.value)
            val source = sourceInWindow?.let { Bounds(it.left - origin.x, it.top - origin.y, it.right - origin.x, it.bottom - origin.y) }
            if (source != null && entry.ring != PopoverRing.NONE) drawRing(source, entry.ring, shown.value)
        }
        if (anchor == null) return@Box
        var placed by remember(entry) { mutableStateOf<PopoverPlacement?>(null) }
        val scroll = rememberScrollState()
        Layout(
            content = {
                Column(
                    Modifier
                        .graphicsLayer {
                            val p = placed ?: return@graphicsLayer
                            val a = PopoverSize.arrowHeight.toPx()
                            alpha = shown.value
                            val s = 0.85f + 0.15f * shown.value
                            scaleX = s; scaleY = s
                            // From the arrow's tip.
                            val h = maxOf(1f, p.height)
                            transformOrigin = TransformOrigin(p.tipX / p.width, if (p.below) -a / h else 1f + a / h)
                        }
                        .shadow(8.dp, Chamfer, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                        .drawBehind { placed?.let { drawPopoverFrame(it) } }
                        .verticalScroll(scroll)
                        .padding(vertical = 4.dp),
                    content = content,
                )
            },
        ) { measurables, constraints ->
            val body = measurables.single()
            val w = PopoverSize.width.roundToPx().toFloat()
            val natural = body.maxIntrinsicHeight(w.roundToInt()).toFloat()
            val area = Bounds(
                insets.getLeft(this, layoutDirection).toFloat(), insets.getTop(this).toFloat(),
                constraints.maxWidth - insets.getRight(this, layoutDirection).toFloat(), constraints.maxHeight - insets.getBottom(this).toFloat(),
            )
            val p = PopoverPlace.place(
                anchor, w, natural, area, PopoverSize.margin.toPx(), PopoverSize.arrowHalf.toPx(), PopoverSize.arrowHeight.toPx(),
                PopoverSize.gap.toPx(), PopoverSize.chamfer.toPx(), PopoverSize.minHeight.toPx(),
            )
            if (placed != p) placed = p
            val placeable = body.measure(Constraints.fixed(p.width.roundToInt(), p.height.roundToInt().coerceAtLeast(0)))
            layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(p.left.roundToInt(), p.bodyTop.roundToInt()) }
        }
    }
}

/** The amber ring round the pressed icon (or widget), DESIGN.md's selection colour as a line. */
private fun DrawScope.drawRing(anchor: Bounds, ring: PopoverRing, shown: Float) {
    val out = PopoverSize.ringOut.toPx()
    val tl = Offset(anchor.left - out, anchor.top - out)
    val size = Size(anchor.right - anchor.left + 2 * out, anchor.bottom - anchor.top + 2 * out)
    val radius = if (ring == PopoverRing.ICON) CornerRadius(size.minDimension * 0.28f) else CornerRadius.Zero
    drawRoundRect(Or, tl, size, radius, style = Stroke(1.5.dp.toPx()), alpha = shown)
}

/**
 * The body's face and 1 dp frame as one outline with the arrow: the panel's chamfer (top-left and
 * bottom-right), and the arrow out of the top edge (below) or the bottom edge (above).
 */
private fun DrawScope.drawPopoverFrame(p: PopoverPlacement) {
    val c = PopoverSize.chamfer.toPx()
    val a = PopoverSize.arrowHeight.toPx()
    val half = PopoverSize.arrowHalf.toPx()
    // Inset by half the stroke, so the 1 dp line falls inside the body.
    val s = 0.5.dp.toPx()
    val l = s; val t = s; val r = size.width - s; val b = size.height - s
    val x = p.tipX
    val base = p.baseX
    val path = Path().apply {
        moveTo(l + c, t)
        if (p.below) { lineTo(base - half, t); lineTo(x, t - a); lineTo(base + half, t) }
        lineTo(r, t)
        lineTo(r, b - c)
        lineTo(r - c, b)
        if (!p.below) { lineTo(base + half, b); lineTo(x, b + a); lineTo(base - half, b) }
        lineTo(l, b)
        lineTo(l, t + c)
        close()
    }
    drawPath(path, Raised)
    // The approved mock's bronze (Banked Amber), brighter than panels' Frame Bronze: a menu floats.
    drawPath(path, OrDeep, style = Stroke(1.dp.toPx()))
}

/** A popover's row: a label, with an icon (an app shortcut's) before it when given. */
@Composable
fun PopoverItem(text: String, icon: ImageBitmap? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = Type.body.copy(color = Txt, fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A rule between a popover's groups. */
@Composable
fun PopoverDivider() {
    Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).drawBehind { drawRect(Line) })
}

/** A row of [PopoverAction]s: an app's App info, Remove, Hide and Uninstall. */
@Composable
fun PopoverActions(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), content = content)
}

/** The glyphs of [PopoverAction]s, after the mock's: drawn as lines, so they need no icon set. */
enum class Glyph { INFO, REMOVE, HIDE, UNINSTALL }

/** One of a [PopoverActions] row: a glyph over a short label. */
@Composable
fun RowScope.PopoverAction(label: String, glyph: Glyph, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(20.dp)) { drawGlyph(glyph) }
        Spacer(Modifier.height(3.dp))
        Text(label, style = Type.body.copy(color = Mute, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Draws [glyph] in a 24-unit square scaled to this canvas, in Starlight 1.8-unit lines. */
private fun DrawScope.drawGlyph(glyph: Glyph) {
    val u = size.minDimension / 24f
    val line = Stroke(1.8f * u, cap = StrokeCap.Butt)
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun seg(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(Txt, p(x1, y1), p(x2, y2), 1.8f * u)
    when (glyph) {
        Glyph.INFO -> {
            drawCircle(Txt, 9f * u, p(12f, 12f), style = line)
            seg(12f, 11f, 12f, 17f); seg(12f, 7f, 12f, 8.6f)
        }
        Glyph.REMOVE -> {
            drawRect(Txt, p(3f, 3f), Size(18f * u, 18f * u), style = line)
            seg(8f, 12f, 16f, 12f)
        }
        Glyph.HIDE -> {
            val eye = Path().apply {
                moveTo(2f * u, 12f * u)
                quadraticTo(12f * u, 1f * u, 22f * u, 12f * u)
                quadraticTo(12f * u, 23f * u, 2f * u, 12f * u)
                close()
            }
            drawPath(eye, Txt, style = line)
            seg(4f, 20f, 20f, 4f)
        }
        Glyph.UNINSTALL -> {
            seg(4f, 7f, 20f, 7f)
            val lid = Path().apply { moveTo(9f * u, 7f * u); lineTo(9f * u, 4f * u); lineTo(15f * u, 4f * u); lineTo(15f * u, 7f * u) }
            drawPath(lid, Txt, style = line)
            val can = Path().apply { moveTo(6f * u, 7f * u); lineTo(7f * u, 20f * u); lineTo(17f * u, 20f * u); lineTo(18f * u, 7f * u) }
            drawPath(can, Txt, style = line)
        }
    }
}
