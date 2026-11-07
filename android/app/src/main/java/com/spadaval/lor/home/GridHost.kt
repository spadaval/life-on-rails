package com.spadaval.lor.home

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout as ComposeLayout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/*
 * The grid host (lor-qdmy): home drawn from the layout's grid of panels, one cell on screen, moved
 * between by a swipe that follows the finger on both axes, the vertical axis wrapping. The rules
 * of where a move goes are [GridMoves]'; this file holds where home is, draws the cell on screen
 * and its neighbours, and moves with the finger through one [Follow] per move (lor-eo1g).
 */

/** A move from [GridPlace.cell] to [to], which lies [way] on screen. */
data class GridMove(val to: Cell, val way: Way)

/**
 * Where home is on the grid: the [cell] on screen and the [move] under way, whose [follow] runs
 * from 0 (at [cell]) to 1 (at [GridMove.to]). Set from anywhere (a home press arrives outside
 * composition); [SettleGrid] finishes each move.
 */
class GridPlace {
    var cell by mutableStateOf(Layout.HOME)
    var move by mutableStateOf<GridMove?>(null)
    val follow = Follow()
    /** The last vertical move that arrived here, so a move back returns where it came from (lor-y7qi). */
    var back by mutableStateOf<Arrival?>(null)

    /** The panels drawn, a drag's new one included; set by [HomeScreen]. */
    var panels by mutableStateOf<List<Panel>>(emptyList())
    /** Where a home press goes: the layout's `start` for this screen. */
    var start by mutableStateOf(Layout.HOME)

    /** Called on arriving at a cell from the drawer (the search clears, as the drawer's close did). */
    var onLeftDrawer: () -> Unit = {}

    val cells: List<Cell> get() = panels.map { it.at }
    fun panel(at: Cell): Panel? = panels.firstOrNull { it.at == at }

    /** The cell a move [way] from here reaches, or null. */
    fun next(way: Way): Cell? = GridMoves.next(cells, cell, way, back)

    /** Where home is going: the move's cell while it heads there, else [cell]. */
    val target: Cell get() = move?.takeIf { follow.target == 1f }?.to ?: cell

    /**
     * Goes to [to], with a settle when [animate], else at once. A move under way toward [to] or back
     * to it is turned there; one going elsewhere is dropped.
     */
    fun goTo(to: Cell, animate: Boolean, way: Way = GridMoves.wayTo(cell, to)) {
        val m = move
        when {
            !animate || (m != null && m.to != to && cell != to) -> {
                if (cell != to) back = null
                cell = to
                move = null
                follow.position = 0f
                follow.target = 0f
            }
            m != null -> follow.target = if (m.to == to) 1f else 0f
            cell != to -> {
                move = GridMove(to, way)
                follow.target = 1f
            }
        }
    }

    /** The move reached its cell. */
    internal fun arrive() {
        val m = move ?: return
        val from = cell
        back = if (m.way.horizontal) null else Arrival(from, m.way)
        cell = m.to
        move = null
        follow.position = 0f
        follow.target = 0f
        if (panel(from) is Panel.Drawer && panel(m.to) !is Panel.Drawer) onLeftDrawer()
        android.util.Log.d("LorGrid", "at ${m.to} from $from")
    }

    /** Where [at] is drawn, in px from the screen's origin, for a [w] by [h] screen; null when it isn't placed. */
    fun offsetOf(at: Cell, w: Int, h: Int): IntOffset? {
        val m = move
        val p = if (m == null) 0f else follow.position
        val way = m?.way
        fun along(way: Way, share: Float) = if (way.horizontal) IntOffset((way.sign * share * w).roundToInt(), 0) else IntOffset(0, (way.sign * share * h).roundToInt())
        val here = if (way == null) IntOffset.Zero else along(way, -p)
        return when {
            at == cell -> here
            m != null && at == m.to -> along(m.way, 1f - p)
            // A neighbour waits off screen where a move would bring it from.
            else -> Way.entries.firstOrNull { next(it) == at }?.let { here + along(it, 1f) }
        }
    }

    /** How much of the drawer is on screen, 0 to 1. */
    val drawerShown: Float
        get() {
            val m = move
            val p = if (m == null) 0f else follow.position
            return when {
                panel(cell) is Panel.Drawer -> 1f - p
                m != null && panel(m.to) is Panel.Drawer -> p
                else -> 0f
            }
        }

    /** The cell under the status bar ([barPx] tall) on a screen [h] tall. */
    fun underBar(h: Int, barPx: Int): Cell {
        val m = move ?: return cell
        val p = follow.position
        if (m.way.horizontal) return if (p < 0.5f) cell else m.to
        val y = -m.way.sign * p * h
        return if (y > 0 || y + h < barPx) m.to else cell
    }
}

/** Finishes each move: the follow's settle, then arriving at its cell or staying. */
@Composable
fun SettleGrid(grid: GridPlace) {
    SettleFollow(grid.follow)
    LaunchedEffect(grid) {
        snapshotFlow { Triple(grid.follow.position, grid.follow.target, grid.follow.held) }.collect { (p, t, held) ->
            if (held || grid.move == null) return@collect
            if (p == 1f && t == 1f) grid.arrive()
            else if (p == 0f && t == 0f) grid.move = null
        }
    }
}

/**
 * The blocks that scroll vertically, where they are and whether they can scroll each way: a
 * vertical swipe that one of them can take is its own, not the grid's (the strategy's Shape).
 */
class GridScrolls {
    private val areas = HashMap<Any, Pair<LayoutCoordinates, (Way) -> Boolean>>()

    fun put(key: Any, at: LayoutCoordinates, scrolls: (Way) -> Boolean) {
        areas[key] = at to scrolls
    }

    fun remove(key: Any) {
        areas.remove(key)
    }

    /** Whether a block under [at] takes a swipe that would move the grid [way]. */
    fun takes(at: Offset, way: Way): Boolean =
        areas.values.any { (c, scrolls) -> c.isAttached && c.boundsInRoot().contains(at) && scrolls(way) }
}

/** Home's scrolling blocks, for a page drawn in a cell (the agent page) to report its own. */
val LocalGridScrolls = staticCompositionLocalOf<GridScrolls?> { null }

/** Reports this as a block that takes a vertical swipe while [scrolls] says it can scroll that way. */
fun Modifier.scrollsInGrid(scrolls: (Way) -> Boolean): Modifier = composed {
    val registry = LocalGridScrolls.current ?: return@composed Modifier
    val key = remember { Any() }
    DisposableEffect(registry, key) { onDispose { registry.remove(key) } }
    onGloballyPositioned { registry.put(key, it) { way -> !way.horizontal && scrolls(way) } }
}

/** Reports [state]'s list as a block that takes a vertical swipe until it reaches its edge. */
fun Modifier.scrollsInGrid(state: ScrollableState): Modifier =
    scrollsInGrid { way -> if (way == Way.DOWN) state.canScrollForward else state.canScrollBackward }

/**
 * The finger on the grid, on home's root in the Initial pass as the drag is, measured where nothing
 * moves (the cells move with it). Past the touch slop it picks the axis by the larger component; a
 * vertical swipe a scrolling block can take, or one from the status bar ([topPx]: the shade's),
 * is left alone. A press while a move settles takes it from where it is, on its axis.
 */
internal fun Modifier.gridFinger(ui: HomeUi, haptics: HapticFeedback, topPx: () -> Int): Modifier = pointerInput(ui) {
    val grid = ui.grid
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (ui.drag.held != null || ui.folder != null || down.position.y < topPx()) return@awaitEachGesture
        val tracker = VelocityTracker()
        val first = awaitSlop(down, tracker, PointerEventPass.Initial) ?: return@awaitEachGesture
        if (ui.drag.held != null) return@awaitEachGesture
        val way = (first.position - down.position).let { Way.ofSwipe(it.x, it.y) }
        val under = grid.move
        val move = if (under != null) {
            if (under.way.horizontal != way.horizontal) return@awaitEachGesture
            under
        } else {
            if (!way.horizontal && ui.scrolls.takes(down.position, way)) return@awaitEachGesture
            GridMove(grid.next(way) ?: return@awaitEachGesture, way)
        }
        val from = grid.cell
        val sizePx = (if (move.way.horizontal) size.width else size.height).toFloat()
        val finger = FollowFinger(grid.follow, sizePx, density, onTick = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }) { toOne, dpS ->
            // One line per release, which the emulator checks read (as the drawer's did, launcher polish 7.1).
            android.util.Log.d("LorMotion", "grid $from to ${move.to} let go at ${grid.follow.position}, $dpS dp/s: ${if (toOne) "goes" else "stays"}")
        }
        // Held before the move is set, so the settle never sees a move nobody has.
        grid.move = move
        followAlong(down, first, finger, tracker, PointerEventPass.Initial, move.way.horizontal, move.way.sign)
    }
}

/**
 * The cells home keeps composed, each full screen where [GridPlace.offsetOf] puts it: the cell on
 * screen, the one a move goes to, and the cells a move from here reaches (the strategy's Shape: so
 * a home press stays instant however many panels there are). The drawer is composed only while a
 * move involves it, as it was before the grid. Placed rather than drawn with an offset, so what
 * each cell reports of where it is (drop zones, sources) follows it.
 */
@Composable
internal fun GridCells(grid: GridPlace, cell: @Composable (Panel) -> Unit) {
    val m = grid.move
    val around = GridMoves.window(grid.cells, grid.cell, grid.back).filter { at ->
        at == grid.cell || at == m?.to || grid.panel(at) !is Panel.Drawer
    }
    val shown = (around + listOfNotNull(m?.to)).distinct().mapNotNull(grid::panel)
    ComposeLayout({
        shown.forEach { panel ->
            key(panel.at) {
                DisposableEffect(Unit) {
                    android.util.Log.d("LorGrid", "composed ${panel.at} ${panel.type}")
                    onDispose { android.util.Log.d("LorGrid", "left ${panel.at}") }
                }
                cell(panel)
            }
        }
    }) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val placeables = measurables.map { it.measure(Constraints.fixed(w, h)) }
        layout(w, h) {
            placeables.forEachIndexed { i, p -> grid.offsetOf(shown[i].at, w, h)?.let { p.place(it) } }
        }
    }
}

/** A panel's type as the file writes it. */
val Panel.type: String
    get() = when (this) {
        is Panel.Stack -> "stack"
        is Panel.T3 -> "t3"
        is Panel.Drawer -> "drawer"
    }
