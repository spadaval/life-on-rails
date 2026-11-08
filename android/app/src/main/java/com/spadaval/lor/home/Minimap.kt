package com.spadaval.lor.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.Frame
import com.spadaval.lor.ui.Motion
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.PanelFace
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.selection
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/*
 * The minimap (lor-6get, in lor-f3vn): the phone's editor for the grid of panels, drawn over home
 * as the open folder is. It draws the active layout's grid as a map, one tile a panel; panel types
 * drag from a palette into empty cells, panels drag to move or swap, and a selected panel shows
 * Show, Move, Full or Half, the phone's and the inner screen's start, and Remove. Its padlock locks
 * and unlocks home (decision lor-73li). Every change is one of lor-v692's grid edits ([GridEdit.kt]),
 * written through [HomeActions.edit] with Undo, and a refusal is the edit's own reason.
 *
 * Read before building (Launcher polish's rule; lor-6get's Context): the map is Smart Launcher's
 * page manager (docs/research/launcher-vs-smart-launcher.md, "Page manager"), which is closed source,
 * so only its idea. Lawnchair (Apache-2.0, aeb92c1) has Launcher3's `EDIT_MODE`, which shrinks
 * home's pages side by side with no two-way map, and makes and removes pages only by drops
 * (`Workspace.addExtraEmptyScreenOnDrag`, `stripEmptyScreens`); Neo Launcher ships the same
 * `PagedView` (GPL-3.0); Kvaesitso has no pages. No code is borrowed: the map is drawn in
 * DESIGN.md's terms (framed tiles, dashed for what is not yet there, the amber selection).
 */

/** The minimap's rules that need no Compose, so the JVM tests reach them. */
object MinimapRules {
    /** A panel type as the minimap names it. */
    fun name(type: String): String = when (type) {
        "stack" -> "Stack"
        "t3" -> "Agent page"
        "drawer" -> "Drawer"
        else -> type.replaceFirstChar { it.uppercase() }
    }

    /** The apps an `apps` block holds, a folder's counted one by one. */
    fun appCount(items: List<LayoutItem>): Int = items.sumOf { if (it is Folder) it.apps.size else 1 }

    /** A block as a tile lists it: "Clock", "12 apps", "Dock". */
    fun blockName(block: StackBlock): String = when (block) {
        is StackBlock.Clock -> "Clock"
        is StackBlock.Apps -> appCount(block.items).let { if (it == 1) "1 app" else "$it apps" }
        is StackBlock.Dock -> "Dock"
        is StackBlock.Widget -> "Widget"
    }

    /**
     * A stack's blocks, top to bottom: "Clock · 12 apps · Dock". Each name, and the dot after it,
     * hold together with no-break spaces, so a narrow tile wraps only after a dot.
     */
    fun blocks(stack: Panel.Stack): String = stack.blocks.joinToString("\u00A0· ") { blockName(it).replace(' ', '\u00A0') }

    /** Where the inner screen starts: `start.wide`, else the narrow start. */
    fun innerStart(layout: Layout): Cell = layout.start.wide ?: layout.start.narrow

    /** One cell of the map: the [panel] there, if any, and which starts it is. */
    data class Tile(val cell: Cell, val panel: Panel?, val phoneStart: Boolean, val innerStart: Boolean) {
        /** What a tile is called: its panel's type name, or nothing for an empty cell. */
        val name: String? get() = panel?.let { MinimapRules.name(it.type) }

        /** Its blocks for a stack, else null. */
        val blocks: String? get() = (panel as? Panel.Stack)?.let { MinimapRules.blocks(it) }

        /** "Full" or "Half", for a panel. */
        val wide: String? get() = panel?.wide?.let { if (it == Wide.FULL) "Full" else "Half" }

        /** What TalkBack reads for the tile. */
        val description: String
            get() = if (panel == null) "Empty cell $cell" else listOfNotNull(
                "$name at $cell",
                blocks?.replace('\u00A0', ' '),
                wide,
                "Phone start".takeIf { phoneStart },
                "Inner start".takeIf { innerStart },
            ).joinToString(". ")
    }

    /** The map's cells, row by row, each row by column: every cell of [Layout.reach]. */
    fun rows(layout: Layout): List<List<Tile>> {
        val reach = layout.reach
        val inner = innerStart(layout)
        return reach.rows.map { r ->
            reach.columns.map { c ->
                val cell = Cell(c, r)
                Tile(cell, layout.panelAt(cell), cell == layout.start.narrow, cell == inner)
            }
        }
    }

    /** The halves that share an inner screen ([InnerScreens.of]), as left and right; drawn joined. */
    fun pairs(layout: Layout): List<Pair<Cell, Cell>> = InnerScreens.of(layout.panels).mapNotNull { s -> s.right?.let { s.left to it } }

    /** A palette entry: a panel [type], its [name], and why it can't be added ([taken]), or null when it can. */
    data class Kind(val type: String, val name: String, val taken: String?)

    /** Every type [Panel.TYPES] holds, a type the layout already holds its one of with the reason. */
    fun palette(layout: Layout): List<Kind> = Panel.TYPES.map { Kind(it, name(it), layout.alreadyHolds(it)) }

    /**
     * A selected panel's actions; Blocks (lor-9uc1) only on a stack. Locked (decision lor-73li),
     * Show and Blocks, which then only lists the blocks.
     */
    enum class Action { SHOW, BLOCKS, MOVE, FULL, HALF, PHONE_START, INNER_START, REMOVE }

    fun actions(locked: Boolean, stack: Boolean): List<Action> =
        (if (locked) listOf(Action.SHOW, Action.BLOCKS) else Action.entries).filter { it != Action.BLOCKS || stack }

    /** A widget as the minimap names it when its provider's label isn't known: its provider. */
    val PROVIDER: (StackBlock.Widget) -> String = { it.provider }

    /**
     * A block as the block list names it: its type's name, or a widget's provider's [label]
     * (lor-r0f9 looks it up). The apps count and a widget's height are [blockDetail].
     */
    fun blockTitle(block: StackBlock, label: (StackBlock.Widget) -> String = PROVIDER): String = when (block) {
        is StackBlock.Clock -> "Clock"
        is StackBlock.Apps -> "Apps"
        is StackBlock.Dock -> "Dock"
        is StackBlock.Widget -> label(block)
    }

    /** What the block list says under a block's name: an `apps` block's count ("12 apps"), a widget's height ("Widget, 180 dp"). */
    fun blockDetail(block: StackBlock): String? = when (block) {
        is StackBlock.Apps -> blockName(block)
        is StackBlock.Widget -> "Widget, ${block.height} dp"
        else -> null
    }

    /** A stack as Move to… names it: "Column 2", with its row when it isn't row 0. */
    fun stackName(cell: Cell): String = if (cell.row == 0) "Column ${cell.column}" else "Column ${cell.column}, row ${cell.row}"

    /** A Move to… entry: "Column 2: 3 apps · Dock". */
    fun stackEntry(stack: Panel.Stack): String = "${stackName(stack.at)}: ${blocks(stack).replace('\u00A0', ' ')}"

    /** The stacks a block of the stack at [from] can be moved to: every other one, by row, then column. */
    fun otherStacks(layout: Layout, from: Cell): List<Panel.Stack> =
        layout.panels.filterIsInstance<Panel.Stack>().filter { it.at != from }.sortedWith(compareBy({ it.at.row }, { it.at.column }))

    /** Where a block moved or added to [stack] by a tap goes: before its `dock`, else last. */
    fun tapIndex(stack: Panel.Stack): Int = stack.blocks.indexOfFirst { it is StackBlock.Dock }.takeIf { it >= 0 } ?: stack.blocks.size

    /** The block palette: each type that takes no field ([StackBlock.bare]) that [stack] doesn't hold. */
    fun blockPalette(stack: Panel.Stack): List<String> = StackBlock.TYPES.filter { StackBlock.bare(it) != null && stack.takes(it) }

    /** What the Undo bar says after block [from] of [blocks] moves to place [to] in its stack: "Clock moved under Apps." */
    fun movedWithin(blocks: List<StackBlock>, from: Int, to: Int, label: (StackBlock.Widget) -> String = PROVIDER): String {
        val now = blocks.toMutableList().apply { add(to, removeAt(from)) }
        val name = blockTitle(blocks[from], label)
        return if (to == 0) "$name moved to the top." else "$name moved under ${blockTitle(now[to - 1], label)}."
    }

    /** What the Undo bar says after [block] is removed from the stack at [at]. */
    fun removed(block: StackBlock, at: Cell, label: (StackBlock.Widget) -> String = PROVIDER): String = when (block) {
        is StackBlock.Apps -> "Apps removed from ${stackName(at)}; ${blockName(block)} left home."
        else -> "${blockTitle(block, label)} removed from ${stackName(at)}."
    }

    /** Where a block dropped at [y] goes in a list whose rows' middles, top to bottom, are [middles]. */
    fun dropIndex(middles: List<Float>, y: Float): Int = middles.count { it < y }

    /**
     * A block row being dragged by its handle, as Kvaesitso's `WidgetColumn` edit mode does (GPL-3.0,
     * ideas only): the list on screen in [order] (block indices), the dragged one at [at], drawn
     * [offset] px from its slot. It swaps with a neighbour once it passes that neighbour's middle:
     * its top above the one over it, or its bottom below the one under it. Only the list on screen
     * changes; letting go writes one `moveBlock`, so one drag is one Undo.
     */
    data class Reorder(val order: List<Int>, val at: Int, val offset: Float) {
        /** The dragged block's index in the file. */
        val block: Int get() = order[at]

        /** After a drag of [dy] px, with each block's row [height] (its gap included) by its index. */
        fun dragged(dy: Float, height: (Int) -> Float): Reorder {
            val o = order.toMutableList()
            var a = at
            var off = offset + dy
            while (a > 0 && off < -height(o[a - 1]) / 2) {
                off += height(o[a - 1])
                o.add(a - 1, o.removeAt(a))
                a--
            }
            while (a < o.lastIndex && off > height(o[a + 1]) / 2) {
                off -= height(o[a + 1])
                o.add(a + 1, o.removeAt(a))
                a++
            }
            return Reorder(o, a, off)
        }

        companion object {
            /** The drag of block [block] of [count] starting, nothing moved yet. */
            fun start(block: Int, count: Int) = Reorder((0 until count).toList(), block, 0f)
        }
    }

    /** What the Undo bar says after a panel of [name] moves from [from] to [to]: into an empty cell, or a swap. */
    fun moved(name: String, from: Cell, to: Cell, swapped: Boolean): String =
        if (swapped) "Swapped $from and $to." else "$name moved to $to."
}

/** What a tap on a cell does: selects a panel, adds the palette's [type], or moves the panel [from]. */
private sealed interface MapMode {
    data object Pick : MapMode
    data class Adding(val type: String) : MapMode
    data class Moving(val from: Cell) : MapMode
}

/**
 * A tile or palette entry under the finger: a [panel] picked up from its cell, or a palette [type]
 * (a block type when [block], from the block palette), drawn from [home] (its bounds in root coordinates), held at [grab] within it.
 */
private class Lifted(val panel: Panel?, val type: String?, val home: Rect, val grab: Offset, val block: Boolean = false) {
    var finger by mutableStateOf(home.topLeft + grab)
    var settling by mutableStateOf(false)
    val pos = Animatable(home.topLeft, Offset.VectorConverter)

    /** Where its top-left is drawn: under the finger, or on its way back. */
    val drawnAt: Offset get() = if (settling) pos.value else finger - grab
}

/** The map's cells fit without scrolling up to this many columns and rows. */
private const val FIT_COLUMNS = 6
private const val FIT_ROWS = 4
private val GAP = 6.dp

/** A tile's narrowest at font scale 1: six fit across a 411 dp phone. Scaled with the font. */
private val TILE_MIN = 56.dp

/**
 * The minimap over home: its padlock and Done, the map, the selected panel's actions and the
 * palette. Back, a home press ([HomeUi.closeMinimap]) and Done close it, leaving home where it was.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Minimap(file: LayoutFile?, ui: HomeUi, home: HomeContext) {
    BackHandler { ui.closeMinimap() }
    val layout = file?.current
    val locked = file?.locked == true
    val actions = home.actions
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // The cell home is on starts selected.
    var selected by remember { mutableStateOf<Cell?>(ui.grid.cell) }
    var mode by remember { mutableStateOf<MapMode>(MapMode.Pick) }
    var lifted by remember { mutableStateOf<Lifted?>(null) }
    // The selected stack's block list (lor-9uc1) is open in place of the panel's actions.
    var blocksOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = blocksOpen) { blocksOpen = false }
    // Each cell's and palette entry's bounds in root coordinates, for a drop's target.
    val bounds = remember { mutableStateMapOf<Any, Rect>() }
    // The pointer handlers outlive a composition; they read the layout drawn now.
    val drawn by rememberUpdatedState(layout)
    LaunchedEffect(locked) {
        if (locked) {
            mode = MapMode.Pick
            lifted = null
        }
    }

    fun cellAt(p: Offset): Cell? = bounds.entries.firstOrNull { (k, r) -> k is Cell && r.contains(p) }?.key as Cell?

    fun add(type: String, at: Cell) {
        mode = MapMode.Pick
        // The selection moves to the new panel only once the add is taken, so a refused add leaves
        // the actions where they were (lor-6get's review).
        var added = false
        actions.edit("${MinimapRules.name(type)} added at $at.", { if (added) selected = at }) { it.addPanel(at, type).also { added = true } }
    }

    fun move(from: Cell, panel: Panel, to: Cell) {
        mode = MapMode.Pick
        selected = to
        val swapped = drawn?.panelAt(to) != null
        actions.edit(MinimapRules.moved(MinimapRules.name(panel.type), from, to, swapped)) { it.movePanel(from, panel, to) }
    }

    fun settle(l: Lifted) {
        l.settling = true
        scope.launch {
            l.pos.snapTo(l.finger - l.grab)
            // DESIGN.md's 220 ms for a thing taking its place; at the animator duration scale 0 it ends at once.
            l.pos.animateTo(l.home.topLeft, Motion.move())
            if (lifted === l) lifted = null
        }
    }

    fun drop(l: Lifted) {
        val to = cellAt(l.finger)
        val panel = l.panel
        val lands = when {
            to == null -> false
            panel != null -> to != panel.at
            else -> drawn?.panelAt(to) == null
        }
        Log.d(TAG, "dropped ${panel?.at ?: l.type} on ${to ?: "nothing"}: ${if (lands) "lands" else "settles back"}")
        if (!lands || to == null) return settle(l)
        lifted = null
        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
        if (panel != null) move(panel.at, panel, to) else add(l.type!!, to)
    }

    fun lift(l: Lifted) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        Log.d(TAG, "lifted ${l.panel?.at ?: l.type}")
        lifted = l
    }

    fun tapped(cell: Cell) {
        val now = drawn ?: return
        val panel = now.panelAt(cell)
        when (val m = mode) {
            is MapMode.Adding -> if (panel == null) add(m.type, cell) else {
                mode = MapMode.Pick
                selected = cell
            }
            is MapMode.Moving -> {
                val moving = now.panelAt(m.from)
                if (moving != null && cell != m.from) move(m.from, moving, cell) else mode = MapMode.Pick
            }
            MapMode.Pick -> if (panel != null) {
                selected = cell
                if (panel !is Panel.Stack) blocksOpen = false
            }
        }
    }

    Box(Modifier.fillMaxSize().swallowTaps()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp), contentAlignment = Alignment.TopCenter) {
            val width = min(maxWidth, 600.dp)
            val height = maxHeight
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    // Room to scroll everything clear of the Undo bar, which sits 112 dp up.
                    .padding(top = 16.dp, bottom = 176.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Header(locked, onPadlock = {
                    actions.edit(if (locked) "Home is unlocked." else "Home is locked.") { it.setLocked(!locked) }
                }) { ui.closeMinimap() }
                if (layout == null) {
                    Text("Home's layout hasn't loaded yet.", style = Type.body.copy(color = Mute))
                    return@Column
                }
                MapGrid(
                    layout, width, height, selected, lifted, mode, bounds,
                    onTap = ::tapped,
                    tileGestures = { tile ->
                        val panel = tile.panel
                        if (locked || panel == null) Modifier else Modifier.pointerInput(panel) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { at -> bounds[panel.at]?.let { lift(Lifted(panel, null, it, at)) } },
                                onDrag = { change, amount ->
                                    change.consume()
                                    lifted?.let { it.finger += amount }
                                },
                                onDragEnd = { lifted?.let(::drop) },
                                onDragCancel = { lifted?.let(::settle) },
                            )
                        }
                    },
                )
                Hint(locked, mode, layout) { mode = MapMode.Pick }
                val panel = selected?.let(layout::panelAt)
                if (blocksOpen && panel is Panel.Stack) {
                    BlockEditor(
                        panel, layout, locked, home, lifted, bounds,
                        lift = ::lift,
                        settle = ::settle,
                        land = {
                            lifted = null
                            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        },
                        close = { blocksOpen = false },
                    )
                    return@Column
                }
                if (panel != null) PanelActions(panel, layout, locked, mode, ui, home, onBlocks = { blocksOpen = true }) { mode = it }
                if (!locked) Palette(layout, mode, bounds, onPick = { mode = it }, lift = ::lift, drop = ::drop, settle = ::settle, onDrag = { amount -> lifted?.let { it.finger += amount } }, refused = actions::refused)
            }
        }
        lifted?.let { LiftedTile(it) }
    }
}

/** The title, the padlock and Done; wrapping onto two lines when the font is large. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(locked: Boolean, onPadlock: () -> Unit, onDone: () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text("PANELS", style = Type.headline, modifier = Modifier.weight(1f))
        MapButton(if (locked) "Locked" else "Unlocked", glyph = { Padlock(locked) }, describe = if (locked) "Locked. Tap to unlock home." else "Unlocked. Tap to lock home.", onClick = onPadlock)
        MapButton("Done", onClick = onDone)
    }
}

/**
 * The map: a cell for each column from 0 to one past the last panel's and each row from 0 to one
 * past the last ([Layout.reach]). Up to [FIT_COLUMNS] (fewer at a large font) by [FIT_ROWS] fit the width and half the
 * height; a wider map scrolls sideways, and a taller one with the page. Halves sharing an inner
 * screen are joined by a band across the gap between them.
 */
@Composable
private fun MapGrid(
    layout: Layout,
    width: Dp,
    height: Dp,
    selected: Cell?,
    lifted: Lifted?,
    mode: MapMode,
    bounds: MutableMap<Any, Rect>,
    onTap: (Cell) -> Unit,
    tileGestures: (MinimapRules.Tile) -> Modifier,
) {
    val rows = remember(layout) { MinimapRules.rows(layout) }
    val pairs = remember(layout) { MinimapRules.pairs(layout) }
    val columns = rows.first().size
    // A tile is at least TILE_MIN wide at the font's scale, so a large font shows fewer columns
    // and scrolls rather than breaking a word across lines.
    val minW = TILE_MIN * LocalDensity.current.fontScale
    val fitColumns = columns.coerceAtMost(FIT_COLUMNS).coerceAtMost(((width + GAP) / (minW + GAP)).toInt().coerceAtLeast(1))
    val fitRows = rows.size.coerceAtMost(FIT_ROWS)
    val cellW = (width - GAP * (fitColumns - 1)) / fitColumns
    val cellH = min(cellW * 1.3f, (height * 0.5f - GAP * (fitRows - 1)) / fitRows).coerceAtLeast(56.dp)
    var origin by remember { mutableStateOf(Offset.Zero) }
    // The cell a held tile would drop on, outlined as the selection is.
    val over = lifted?.takeIf { !it.settling && !it.block }?.finger?.let { f -> bounds.entries.firstOrNull { (k, r) -> k is Cell && r.contains(f) }?.key as Cell? }
    val scroll = rememberScrollState()
    Box(if (columns > fitColumns) Modifier.horizontalScroll(scroll) else Modifier) {
        Column(
            Modifier
                .onGloballyPositioned { origin = it.positionInRoot() }
                .drawBehind {
                    // The band joining a pair: a face with the tiles' top and bottom edges, across the gap.
                    pairs.forEach { (l, r) ->
                        val a = bounds[l] ?: return@forEach
                        val b = bounds[r] ?: return@forEach
                        val top = maxOf(a.top, b.top) - origin.y
                        val bottom = minOf(a.bottom, b.bottom) - origin.y
                        val left = a.right - origin.x
                        val right = b.left - origin.x
                        if (right <= left || bottom <= top) return@forEach
                        drawRect(PanelFace, Offset(left, top), Size(right - left, bottom - top))
                        val hair = 1.dp.toPx()
                        drawRect(Frame, Offset(left, top), Size(right - left, hair))
                        drawRect(Frame, Offset(left, bottom - hair), Size(right - left, hair))
                    }
                },
            verticalArrangement = Arrangement.spacedBy(GAP),
        ) {
            rows.forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    row.forEach { tile ->
                        // A cell that leaves the map (an edit shrank the reach) takes its bounds with it,
                        // so a drop never finds a cell that isn't drawn.
                        DisposableEffect(tile.cell) { onDispose { bounds.remove(tile.cell) } }
                        CellTile(
                            tile,
                            selected = tile.panel != null && tile.cell == selected,
                            over = tile.cell == over,
                            held = lifted?.panel?.at == tile.cell,
                            target = (mode is MapMode.Adding && tile.panel == null) || (mode is MapMode.Moving && tile.cell != mode.from),
                            modifier = Modifier
                                .width(cellW)
                                .heightIn(min = cellH)
                                .fillMaxHeight()
                                .onGloballyPositioned { bounds[tile.cell] = it.boundsInRoot() }
                                .then(tileGestures(tile)),
                            onTap = { onTap(tile.cell) },
                        )
                    }
                }
            }
        }
    }
}

/** One cell: a framed tile for a panel, a dashed one for an empty cell. */
@Composable
private fun CellTile(tile: MinimapRules.Tile, selected: Boolean, over: Boolean, held: Boolean, target: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val edged = when {
        selected || over -> Modifier.selection()
        tile.panel != null -> Modifier.background(PanelFace).border(1.dp, Frame)
        // DESIGN.md's Shapes: dashed is not yet there.
        else -> Modifier.drawBehind {
            val w = 1.dp.toPx()
            drawRect(Frame, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
        }
    }
    Box(
        modifier
            .then(if (tile.panel != null && (selected || over)) Modifier.background(PanelFace) else Modifier)
            .then(edged)
            .alpha(if (held) 0.4f else 1f)
            .clickable(role = Role.Button, onClick = onTap)
            .semantics(mergeDescendants = true) {
                contentDescription = tile.description
                this.selected = selected
            },
    ) {
        if (tile.panel != null) TileFace(tile)
        else if (target) Text("+", style = Type.body.copy(color = Dim), modifier = Modifier.align(Alignment.Center))
    }
}

/** A panel tile's words: its name, a stack's blocks, Full or Half, and its starts. */
@Composable
private fun TileFace(tile: MinimapRules.Tile) {
    Column(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(tile.name.orEmpty().uppercase(), style = Type.label.copy(color = Txt, lineHeight = 15.sp))
        tile.blocks?.let { Text(it, style = Type.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 14.sp)) }
        Spacer(Modifier.weight(1f))
        tile.wide?.let { Text(it.uppercase(), style = Type.label.copy(fontSize = 11.sp, lineHeight = 14.sp)) }
        if (tile.phoneStart) Text("PHONE START", style = Type.label.copy(color = Txt, fontSize = 11.sp, lineHeight = 14.sp))
        if (tile.innerStart) Text("INNER START", style = Type.label.copy(color = Txt, fontSize = 11.sp, lineHeight = 14.sp))
    }
}

/** What a tap does now, with a way out of adding or moving. */
@Composable
private fun Hint(locked: Boolean, mode: MapMode, layout: Layout, cancel: () -> Unit) {
    val text = when {
        locked -> "Home is locked. Unlock it to change the grid."
        mode is MapMode.Adding -> "Tap an empty cell to add a ${MinimapRules.name(mode.type).lowercase()}."
        mode is MapMode.Moving -> "Tap a cell to move the ${layout.panelAt(mode.from)?.let { MinimapRules.name(it.type).lowercase() } ?: "panel"} there. A panel there swaps with it."
        else -> "Tap a panel for its actions. Hold one to move it."
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = Type.bodySmall, modifier = Modifier.weight(1f))
        if (mode !is MapMode.Pick) MapButton("Cancel", onClick = cancel)
    }
}

/** The selected panel's actions: Show, a stack's Blocks; and unlocked, Move, Full and Half, the two starts, and Remove. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelActions(panel: Panel, layout: Layout, locked: Boolean, mode: MapMode, ui: HomeUi, home: HomeContext, onBlocks: () -> Unit, setMode: (MapMode) -> Unit) {
    val cell = panel.at
    val name = MinimapRules.name(panel.type)
    val edit = home.actions::edit
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${name.uppercase()} $cell", style = Type.title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MinimapRules.actions(locked, panel is Panel.Stack).forEach { a ->
                when (a) {
                    MinimapRules.Action.SHOW -> MapButton("Show") {
                        ui.closeMinimap()
                        ui.grid.goTo(cell, animate = true)
                    }
                    MinimapRules.Action.BLOCKS -> MapButton("Blocks", onClick = onBlocks)
                    MinimapRules.Action.MOVE -> {
                        val moving = mode == MapMode.Moving(cell)
                        MapButton("Move", on = moving) { setMode(if (moving) MapMode.Pick else MapMode.Moving(cell)) }
                    }
                    MinimapRules.Action.FULL -> MapButton("Full", on = panel.wide == Wide.FULL) {
                        if (panel.wide != Wide.FULL) edit("$name at $cell is full on the inner screen.", {}) { it.setWide(cell, panel, Wide.FULL) }
                    }
                    MinimapRules.Action.HALF -> MapButton("Half", on = panel.wide == Wide.HALF) {
                        if (panel.wide != Wide.HALF) edit("$name at $cell is half on the inner screen.", {}) { it.setWide(cell, panel, Wide.HALF) }
                    }
                    MinimapRules.Action.PHONE_START -> MapButton("Phone start", on = layout.start.narrow == cell) {
                        if (layout.start.narrow != cell) edit("Home starts at $cell on the phone.", {}) { it.setStart(cell, StartScreen.NARROW) }
                    }
                    MinimapRules.Action.INNER_START -> MapButton("Inner start", on = MinimapRules.innerStart(layout) == cell) {
                        if (MinimapRules.innerStart(layout) != cell) edit("The inner screen starts at $cell.", {}) { it.setStart(cell, StartScreen.WIDE) }
                    }
                    MinimapRules.Action.REMOVE -> MapButton("Remove", danger = true) {
                        edit("${name} at $cell removed.", {}) { it.removePanel(cell, panel) }
                    }
                }
            }
        }
    }
}

/**
 * The palette: every panel type, dragged onto an empty cell or tapped and then a cell tapped. A
 * type the layout holds its one of is dimmed, and a tap on it says why.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Palette(
    layout: Layout,
    mode: MapMode,
    bounds: MutableMap<Any, Rect>,
    onPick: (MapMode) -> Unit,
    lift: (Lifted) -> Unit,
    drop: (Lifted) -> Unit,
    settle: (Lifted) -> Unit,
    onDrag: (Offset) -> Unit,
    refused: (String) -> Unit,
) {
    val kinds = remember(layout) { MinimapRules.palette(layout) }
    var held by remember { mutableStateOf<Lifted?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ADD A PANEL", style = Type.title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            kinds.forEach { k ->
                val key = "palette:${k.type}"
                val taken = k.taken
                val picking = mode == MapMode.Adding(k.type)
                val drags = if (taken != null) Modifier else Modifier.pointerInput(k.type) {
                    detectDragGestures(
                        onDragStart = { at -> bounds[key]?.let { Lifted(null, k.type, it, at).also { l -> held = l; lift(l) } } },
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount)
                        },
                        onDragEnd = { held?.let(drop); held = null },
                        onDragCancel = { held?.let(settle); held = null },
                    )
                }
                MapButton(
                    k.name,
                    on = picking,
                    modifier = Modifier.onGloballyPositioned { bounds[key] = it.boundsInRoot() }.then(drags).alpha(if (taken != null) 0.45f else 1f),
                    describe = if (taken != null) "${k.name}: $taken" else null,
                ) {
                    if (taken != null) refused(taken) else onPick(if (picking) MapMode.Pick else MapMode.Adding(k.type))
                }
            }
        }
    }
}

/**
 * A stack's blocks (lor-9uc1), in place of its panel's actions: each block top to bottom, a drag
 * on its handle to reorder, a menu of Move up, Move down, Move to… and Remove, and a palette of
 * the block types the stack doesn't hold. Every change is one of lor-v692's block edits through
 * [HomeActions.edit], with Undo. Locked, it only lists the blocks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockEditor(
    stack: Panel.Stack,
    layout: Layout,
    locked: Boolean,
    home: HomeContext,
    lifted: Lifted?,
    bounds: MutableMap<Any, Rect>,
    lift: (Lifted) -> Unit,
    settle: (Lifted) -> Unit,
    land: () -> Unit,
    close: () -> Unit,
) {
    val cell = stack.at
    val actions = home.actions
    val haptics = LocalHapticFeedback.current
    val gap = 6.dp
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    // A drag on a handle: the order on screen until the edit it writes lands or is refused.
    var reorder by remember(stack) { mutableStateOf<MinimapRules.Reorder?>(null) }
    val heights = remember { mutableStateMapOf<Int, Float>() }
    val rows = remember { mutableStateMapOf<Int, Rect>() }
    var list by remember { mutableStateOf(Rect.Zero) }
    val order = reorder?.order ?: stack.blocks.indices.toList()
    val current by rememberUpdatedState(stack)
    // A widget's provider's label, as its long-press menu on home names it (lor-r0f9); its
    // provider while it isn't bound.
    val widgets = rememberWidgets()
    val pm = LocalContext.current.packageManager
    val labels = remember(stack, home.apps) {
        stack.blocks.filterIsInstance<StackBlock.Widget>().associate { w -> w.id to (widgets.info(w.id, w.provider)?.loadLabel(pm) ?: w.provider) }
    }
    val label: (StackBlock.Widget) -> String = { labels[it.id] ?: it.provider }

    fun moveWithin(from: Int, to: Int, after: () -> Unit = {}) {
        val s = current
        actions.edit(MinimapRules.movedWithin(s.blocks, from, to, label), after) { it.moveBlock(BlockAt(s.at, from), s.blocks[from], s.at, to) }
    }

    // Where a block chip from the palette would go: the list under the finger, between the rows' middles.
    val reach = with(LocalDensity.current) { 24.dp.toPx() }
    fun insertAt(l: Lifted?): Int? {
        if (l == null || !l.block || l.settling) return null
        val f = l.finger
        if (f.x !in list.left..list.right || f.y !in (list.top - reach)..(list.bottom + reach)) return null
        return MinimapRules.dropIndex(order.mapNotNull { rows[it]?.center?.y }, f.y)
    }
    val insert = insertAt(lifted)

    fun add(type: String, index: Int) {
        val s = current
        actions.edit("${MinimapRules.blockTitle(StackBlock.bare(type)!!)} added to ${MinimapRules.stackName(s.at)}.") { it.addBlock(s.at, index, type) }
    }

    fun dropChip(l: Lifted) {
        val at = insertAt(l)
        Log.d(TAG, "dropped block ${l.type} ${if (at == null) "outside the list: settles back" else "at $at"}")
        if (at == null) return settle(l)
        land()
        add(l.type!!, at)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${MinimapRules.name(stack.type).uppercase()} $cell · BLOCKS", style = Type.title, modifier = Modifier.weight(1f))
            MapButton("Back", describe = "Back to the panel's actions", onClick = close)
        }
        if (!locked) Text("Drag a handle to reorder. Tap a block for its menu.", style = Type.bodySmall)
        Column(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { list = it.boundsInRoot() }
                .drawWithContent {
                    drawContent()
                    // Where a dragged palette chip would land: an amber rule in the gap.
                    val i = insert ?: return@drawWithContent
                    val y = (if (i < order.size) rows[order[i]]?.top?.minus(gapPx / 2) else rows[order.last()]?.bottom?.plus(gapPx / 2))
                        ?: return@drawWithContent
                    drawRect(Or, Offset(0f, y - list.top - 1.dp.toPx()), Size(size.width, 2.dp.toPx()))
                },
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            order.forEachIndexed { place, b ->
                key(b) {
                    val block = stack.blocks[b]
                    val r = reorder
                    BlockRow(
                        block, b, stack, layout, locked, home, label,
                        dragging = r?.block == b,
                        offset = if (r?.block == b) r.offset else 0f,
                        first = place == 0,
                        last = place == order.lastIndex,
                        modifier = Modifier.onGloballyPositioned {
                            rows[b] = it.boundsInRoot()
                            heights[b] = it.size.height + gapPx
                        },
                        handle = Modifier.pointerInput(stack, b) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    reorder = MinimapRules.Reorder.start(b, current.blocks.size)
                                },
                                onVerticalDrag = { change, dy ->
                                    change.consume()
                                    val was = reorder ?: return@detectVerticalDragGestures
                                    val now = was.dragged(dy) { heights[it] ?: 0f }
                                    if (now.at != was.at) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    reorder = now
                                },
                                onDragEnd = {
                                    val done = reorder ?: return@detectVerticalDragGestures
                                    Log.d(TAG, "block $b dragged to place ${done.at}")
                                    if (done.at == b) {
                                        reorder = null
                                        return@detectVerticalDragGestures
                                    }
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    // The list keeps the dropped order until the file says otherwise.
                                    reorder = done.copy(offset = 0f)
                                    moveWithin(b, done.at) { reorder = null }
                                },
                                onDragCancel = { reorder = null },
                            )
                        },
                        moveWithin = { to -> moveWithin(b, to) },
                    )
                }
            }
        }
        if (locked) return@Column
        val types = MinimapRules.blockPalette(stack)
        Text("ADD A BLOCK", style = Type.title)
        if (types.isEmpty()) Text("This stack holds each block type already.", style = Type.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type -> key(type) {
                val slot = "block:$type"
                var held by remember { mutableStateOf<Lifted?>(null) }
                val name = MinimapRules.blockTitle(StackBlock.bare(type)!!)
                MapButton(
                    name,
                    modifier = Modifier
                        .onGloballyPositioned { bounds[slot] = it.boundsInRoot() }
                        .pointerInput(type) {
                            detectDragGestures(
                                onDragStart = { at -> bounds[slot]?.let { Lifted(null, type, it, at, block = true).also { l -> held = l; lift(l) } } },
                                onDrag = { change, amount ->
                                    change.consume()
                                    held?.let { it.finger += amount }
                                },
                                onDragEnd = { held?.let(::dropChip); held = null },
                                onDragCancel = { held?.let(settle); held = null },
                            )
                        },
                ) { add(type, MinimapRules.tapIndex(current)) }
            } }
        }
    }
}

/**
 * One block in the list: its handle, its name (an `apps` block's count under it), and, unlocked,
 * a menu on a tap. While dragged it rides [offset] px from its slot, raised by a shadow.
 */
@Composable
private fun BlockRow(
    block: StackBlock,
    index: Int,
    stack: Panel.Stack,
    layout: Layout,
    locked: Boolean,
    home: HomeContext,
    label: (StackBlock.Widget) -> String,
    dragging: Boolean,
    offset: Float,
    first: Boolean,
    last: Boolean,
    modifier: Modifier,
    handle: Modifier,
    moveWithin: (Int) -> Unit,
) {
    val actions = home.actions
    val cell = stack.at
    val title = MinimapRules.blockTitle(block, label)
    var menu by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val close = { menu = false; picking = false }
    Box(
        modifier
            .zIndex(if (dragging) 1f else 0f)
            .offset { IntOffset(0, offset.roundToInt()) }
            .fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(if (dragging) Modifier.shadow(8.dp) else Modifier)
                .background(PanelFace)
                .then(if (dragging) Modifier.selection() else Modifier.border(1.dp, Frame))
                .then(if (locked) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = "open its menu") { menu = true })
                // TalkBack reads the merged words: "Apps, 10 apps".
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!locked) Box(handle.size(48.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) { Grip() }
            Column(Modifier.weight(1f).padding(start = if (locked) 12.dp else 0.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)) {
                Text(title, style = Type.body.copy(color = Txt))
                MinimapRules.blockDetail(block)?.let { Text(it, style = Type.bodySmall) }
            }
        }
        if (menu) DropdownMenu(true, close, containerColor = Raised, shape = RectangleShape) {
            if (picking) {
                val others = MinimapRules.otherStacks(layout, cell)
                if (others.isEmpty()) MenuItem("No other stack", enabled = false) {}
                others.forEach { to ->
                    MenuItem(MinimapRules.stackEntry(to)) {
                        close()
                        actions.edit("$title moved to ${MinimapRules.stackName(to.at)}.") {
                            it.moveBlock(BlockAt(cell, index), block, to.at, MinimapRules.tapIndex(to))
                        }
                    }
                }
                return@DropdownMenu
            }
            MenuItem("Move up", enabled = !first) { close(); moveWithin(index - 1) }
            MenuItem("Move down", enabled = !last) { close(); moveWithin(index + 1) }
            MenuItem("Move to…") {
                // A stack's last block stays (GridEdit's moveBlock), so say so before listing the stacks.
                if (stack.blocks.size == 1) {
                    close()
                    actions.refused(LAST_BLOCK)
                } else picking = true
            }
            MenuItem("Remove", danger = true) {
                close()
                actions.edit(MinimapRules.removed(block, cell, label)) { it.removeBlock(BlockAt(cell, index), block) }
            }
        }
    }
}

/** A menu entry in the app menu's style. */
@Composable
private fun MenuItem(text: String, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = Type.body.copy(color = (if (danger) Bad else Txt).copy(alpha = if (enabled) 1f else 0.38f), fontSize = 15.sp)) },
        onClick = onClick,
        enabled = enabled,
    )
}

/** A drag handle: two columns of three dots in Mute. */
@Composable
private fun Grip() {
    Canvas(Modifier.size(width = 10.dp, height = 16.dp)) {
        val r = 1.5.dp.toPx()
        for (c in 0..1) for (row in 0..2) drawCircle(Mute, r, Offset(r + c * (size.width - 2 * r), r + row * (size.height - 2 * r) / 2))
    }
}

/** What is under the finger: the tile, or the palette entry, as it was drawn, with a float shadow. */
@Composable
private fun LiftedTile(l: Lifted) {
    val d = LocalDensity.current
    val w = with(d) { l.home.width.toDp() }
    val h = with(d) { l.home.height.toDp() }
    Box(
        Modifier
            .offset { l.drawnAt.let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }
            .size(w, h)
            .shadow(8.dp)
            .background(PanelFace)
            .selection(),
    ) {
        val panel = l.panel
        if (panel != null) TileFace(MinimapRules.Tile(panel.at, panel, phoneStart = false, innerStart = false))
        else if (l.block) Text(MinimapRules.blockTitle(StackBlock.bare(l.type!!)!!), style = Type.button, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
        else Text(MinimapRules.name(l.type!!), style = Type.button, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
    }
}

/**
 * DESIGN.md's button: square, a 1 dp Bulkhead Edge, sentence case. [on] takes the selection
 * treatment (the current Full or Half, a start already there, the type being added); [danger] is
 * coral, for Remove.
 */
@Composable
private fun MapButton(
    text: String,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    danger: Boolean = false,
    describe: String? = null,
    glyph: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .then(if (on) Modifier.selection() else Modifier.border(1.dp, if (danger) Bad.copy(alpha = 0.6f) else Edge))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                selected = on
                if (describe != null) contentDescription = describe
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        glyph?.invoke()
        Text(text, style = Type.button.copy(color = if (danger) Bad else Txt))
    }
}

/** A padlock in Starlight: shut when [locked], its shackle lifted and open when not. */
@Composable
private fun Padlock(locked: Boolean) {
    Canvas(Modifier.size(14.dp)) {
        val s = size.width
        val stroke = 1.5.dp.toPx()
        val bodyTop = s * 0.45f
        drawRect(Txt, Offset(s * 0.1f, bodyTop), Size(s * 0.8f, s * 0.55f))
        val lift = if (locked) 0f else s * 0.18f
        val left = s * 0.25f
        val right = s * 0.75f
        drawArc(Txt, 180f, 180f, false, Offset(left, s * 0.05f - lift), Size(right - left, s * 0.5f), style = Stroke(stroke))
        drawLine(Txt, Offset(right, s * 0.3f - lift), Offset(right, bodyTop), stroke)
        if (locked) drawLine(Txt, Offset(left, s * 0.3f), Offset(left, bodyTop), stroke)
    }
}

private const val TAG = "LorMinimap"
