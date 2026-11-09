package com.spadaval.lor.home

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
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
 * between by a swipe that follows the finger on both axes, the vertical axis wrapping. On the inner
 * screen (lor-el51) a screen is a full panel or two halves side by side, and a move goes a screen. The rules
 * of where a move goes are [GridMoves]'; this file holds where home is, draws the cell on screen
 * and its neighbours, and moves with the finger through one [Follow] per move (lor-eo1g).
 */

/**
 * A move from [GridPlace.cell] to [to], which lies [way] on screen. On the inner screen [to] is the
 * cell standing for the inner screen it lands on, and [focus] that screen's focus (lor-j87u's [InnerPlace]).
 */
data class GridMove(val to: Cell, val way: Way, val focus: Cell? = null)

/**
 * Where home is on the grid: the [cell] on screen and the [move] under way, whose [follow] runs
 * from 0 (at [cell]) to 1 (at [GridMove.to]). Set from anywhere (a home press arrives outside
 * composition); [SettleGrid] finishes each move.
 *
 * Where halves [paired] (the inner screen, lor-el51), home stands on an inner screen rather than a
 * cell: [cell] is its [InnerPlace.focusCell] and [focus] its focus, so the screen on show is the one
 * holding [cell]. A move goes a whole inner screen, by [InnerScreens]' rules; otherwise every cell is
 * a screen of its own and the rules are [GridMoves]'.
 */
class GridPlace {
    var cell by mutableStateOf(Layout.HOME)
    var move by mutableStateOf<GridMove?>(null)
    val follow = Follow()
    /** The last vertical move that arrived here, so a move back returns where it came from (lor-y7qi). */
    var back by mutableStateOf<Arrival?>(null)

    /** The panels drawn, a drag's new one included; set by [HomeScreen] through [edited]. */
    var panels by mutableStateOf<List<Panel>>(emptyList())
    /** Where a home press goes: the layout's `start` for this screen. */
    var start by mutableStateOf(Layout.HOME)

    /** Whether halves share a screen: where the smallest width is 600 dp or more (decision note on lor-ph30). Set through [pair]. */
    var paired by mutableStateOf(false)
        private set
    /** The inner screen's focus, when [paired]; null when none is set. */
    var focus by mutableStateOf<Cell?>(null)
        private set

    private val innerScreens = derivedStateOf { InnerScreens.of(panels) }
    /** The inner screens of [panels], row by row. */
    val screens: List<InnerScreen> get() = innerScreens.value

    /** Called on arriving at a cell from the drawer (the search clears, as the drawer's close did). */
    var onLeftDrawer: () -> Unit = {}

    val cells: List<Cell> get() = panels.map { it.at }
    fun panel(at: Cell): Panel? = panels.firstOrNull { it.at == at }

    /** The screen [at] is drawn on: its inner screen when [paired], else itself alone. */
    fun screenOf(at: Cell): InnerScreen = if (paired) InnerScreens.holding(screens, at) ?: InnerScreen(at) else InnerScreen(at)

    /** Where home stands on the inner screen. */
    val inner: InnerPlace get() = InnerPlace(screenOf(cell), focus)

    /** The move [way] from here, or null when there is none. */
    fun nextMove(way: Way): GridMove? =
        if (!paired) GridMoves.next(cells, cell, way, back)?.let { GridMove(it, way) }
        else InnerScreens.next(screens, inner, way, back)?.let { GridMove(it.to.focusCell, way, it.to.focus) }

    /** The cell a move [way] from here reaches (on the inner screen, the one standing for its screen), or null. */
    fun next(way: Way): Cell? = nextMove(way)?.to

    /** Where home is going: the move's cell while it heads there, else [cell]. */
    val target: Cell get() = move?.takeIf { follow.target == 1f }?.to ?: cell

    /** Whether home is on, or going to, the screen that holds [at]. */
    fun heading(at: Cell): Boolean = screenOf(target) == screenOf(at)

    /**
     * Goes to [to], with a settle when [animate], else at once. On the inner screen it lands on the
     * screen holding [to], with [focus] as its focus (none for a home press or back, lor-j87u's
     * [InnerScreens.place]). A move under way toward that screen or back to this one is turned there;
     * one going elsewhere is dropped.
     */
    fun goTo(to: Cell, animate: Boolean, way: Way? = null, focus: Cell? = null) {
        val dest = if (paired) focus ?: screenOf(to).left else to
        val m = move
        val here = screenOf(cell)
        val there = screenOf(dest)
        when {
            !animate || (m != null && screenOf(m.to) != there && here != there) -> {
                if (here != there) back = null
                cell = dest
                this.focus = focus
                move = null
                follow.position = 0f
                follow.target = 0f
            }
            m != null -> if (screenOf(m.to) == there) {
                move = m.copy(to = dest, focus = focus)
                follow.target = 1f
            } else {
                // Turned back to the screen on show: it stays, with the focus asked for.
                cell = dest
                this.focus = focus
                follow.target = 0f
            }
            here != there -> {
                move = GridMove(dest, way ?: GridMoves.wayTo(cell, dest, back), focus)
                follow.target = 1f
            }
            else -> {
                cell = dest
                this.focus = focus
            }
        }
    }

    /** The move reached its cell; returns the cell it left, or null with no move. */
    internal fun arrive(): Cell? {
        val m = move ?: return null
        val from = cell
        back = if (m.way.horizontal) null else Arrival(from, m.way)
        cell = m.to
        focus = m.focus
        move = null
        follow.position = 0f
        follow.target = 0f
        if (panel(from) is Panel.Drawer && panel(m.to) !is Panel.Drawer) onLeftDrawer()
        return from
    }

    /**
     * Halves start or stop sharing a screen (an unfold, a fold). Home stays on its cell: unfolded,
     * on the inner screen holding it, with it as the focus ([InnerScreens.unfold]); folded, on the
     * focus cell, which [cell] already is ([InnerScreens.fold]). A move under way is dropped.
     * Returns whether the pairing changed.
     */
    fun pair(on: Boolean): Boolean {
        if (paired == on) return false
        paired = on
        move = null
        follow.position = 0f
        follow.target = 0f
        focus = if (on) cell else null
        return true
    }

    /**
     * A press at [x] on a screen [w] px wide (lor-ozj2): on a pair, the half under it becomes the
     * focus, with no motion, so a fold lands there. A press while a move settles touches nothing.
     * Returns the half that became the focus, or null when the focus didn't change.
     */
    fun pressed(x: Float, w: Int): Cell? {
        if (!paired || move != null) return null
        val s = screenOf(cell)
        val right = s.right ?: return null
        val at = if (x < w / 2f) s.left else right
        val now = inner.touched(at)
        if (now.focus == focus) return null
        cell = now.focusCell
        focus = now.focus
        return at
    }

    /**
     * The panels changed (an edit, a drag's new page, a file read): a move to a cell that went is
     * dropped, and home stays on [cell] while it is there, else on the nearest in its row, else on
     * [start]. On the inner screen, when the edit changed the screen home stands on (which panels
     * pair there), home's place is built again on the screen that now holds that cell, with no
     * focus (lor-j87u's review, lor-70hj); an edit elsewhere leaves the place and its focus alone.
     */
    fun edited(new: List<Panel>, start: Cell) {
        val before = screens
        panels = new
        val cells = cells
        if (move?.to?.let { it !in cells } == true) goTo(cell, animate = false, focus = focus)
        val stay = GridMoves.stay(cells, cell, start)
        if (paired && InnerScreens.holding(screens, stay) != InnerScreens.holding(before, cell)) InnerScreens.place(screens, stay)?.let { goTo(it.focusCell, animate = false) }
        else if (stay != cell) goTo(stay, animate = false)
    }

    /** The cells kept composed: those of the screen on show, of a move's screen, and of each screen a move from here reaches. */
    fun composed(): List<Cell> {
        val m = move
        val here = screenOf(cell)
        val going = m?.let { screenOf(it.to) }
        val near = Way.entries.mapNotNull { nextMove(it)?.let { n -> screenOf(n.to) } }
        // The drawer is composed only while a move involves it, as it was before the grid.
        val reached = near.filter { s -> s.cells.none { panel(it) is Panel.Drawer } }
        return (listOf(here) + listOfNotNull(going) + reached).distinct().flatMap { it.cells }.filter { panel(it) != null }
    }

    /** Where [at] is drawn, in px from the screen's origin, for a [w] by [h] screen; null when it isn't placed. */
    fun offsetOf(at: Cell, w: Int, h: Int): IntOffset? {
        val s = screenOf(at)
        val base = screenOffset(s, w, h) ?: return null
        return if (at == s.right) base + IntOffset(w / 2, 0) else base
    }

    /** How wide [at] is drawn on a screen [w] px wide: half of it on a pair, else all of it. */
    fun widthOf(at: Cell, w: Int): Int {
        val s = screenOf(at)
        return when {
            s.right == null -> w
            at == s.left -> w / 2
            else -> w - w / 2
        }
    }

    /** Which of the screen's sides [at] touches: both, or the left or right for a half of a pair. */
    fun sidesOf(at: Cell): Sides {
        val s = screenOf(at)
        return when {
            s.right == null -> Sides.BOTH
            at == s.left -> Sides.LEFT
            else -> Sides.RIGHT
        }
    }

    private fun screenOffset(s: InnerScreen, w: Int, h: Int): IntOffset? {
        val m = move
        val p = if (m == null) 0f else follow.position
        val way = m?.way
        fun along(way: Way, share: Float) = if (way.horizontal) IntOffset((way.sign * share * w).roundToInt(), 0) else IntOffset(0, (way.sign * share * h).roundToInt())
        val here = if (way == null) IntOffset.Zero else along(way, -p)
        return when {
            s == screenOf(cell) -> here
            m != null && s == screenOf(m.to) -> along(m.way, 1f - p)
            // During a move only its two screens are placed: a straight move (a home press from page 2)
            // brings a screen that isn't the neighbour that way, which would share its slot (lor-glg9).
            m != null -> null
            // Otherwise a neighbour waits off screen where a move would bring it from.
            else -> Way.entries.firstOrNull { nextMove(it)?.let { n -> screenOf(n.to) } == s }?.let { here + along(it, 1f) }
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
            if (p == 1f && t == 1f) grid.arrive()?.let { from ->
                // On the inner screen, the screen's cells too, which the emulator checks read.
                android.util.Log.d("LorGrid", "at ${grid.cell} from $from" + if (grid.paired) " on ${grid.inner.screen.cells}" else "")
            }
            else if (p == 0f && t == 0f) grid.move = null
        }
    }
}

/**
 * The blocks that scroll vertically, where they are and whether they can scroll each way: a
 * vertical swipe that one of them can take is its own, not the grid's (the strategy's Shape).
 */
class GridScrolls {
    private val areas = HashMap<Any, Pair<LayoutCoordinates, (Offset, Way) -> Boolean>>()

    /** [scrolls] gets where the finger pressed within [at], in its px, and the way the grid would move. */
    fun put(key: Any, at: LayoutCoordinates, scrolls: (Offset, Way) -> Boolean) {
        areas[key] = at to scrolls
    }

    fun remove(key: Any) {
        areas.remove(key)
    }

    /** Whether a block under [at] takes a swipe that would move the grid [way]. */
    fun takes(at: Offset, way: Way): Boolean =
        areas.values.any { (c, scrolls) ->
            c.isAttached && c.boundsInRoot().let { b -> b.contains(at) && scrolls(at - b.topLeft, way) }
        }
}

/** Home's scrolling blocks, for a page drawn in a cell (the agent page) to report its own. */
val LocalGridScrolls = staticCompositionLocalOf<GridScrolls?> { null }

/** Reports this as a block that takes a vertical swipe while [scrolls] says it can scroll that way. */
fun Modifier.scrollsInGrid(scrolls: (Way) -> Boolean): Modifier = scrollsInGridAt { _, way -> scrolls(way) }

/**
 * Reports this as a block that takes a vertical swipe pressed at a point of it (in its px) while
 * [scrolls] says what is there can scroll that way: a widget's list, not the widget around it (lor-r0f9).
 */
fun Modifier.scrollsInGridAt(scrolls: (Offset, Way) -> Boolean): Modifier = composed {
    val registry = LocalGridScrolls.current ?: return@composed Modifier
    val key = remember { Any() }
    DisposableEffect(registry, key) { onDispose { registry.remove(key) } }
    onGloballyPositioned { registry.put(key, it) { at, way -> !way.horizontal && scrolls(at, way) } }
}

/** Reports [state]'s list as a block that takes a vertical swipe until it reaches its edge. */
fun Modifier.scrollsInGrid(state: ScrollableState): Modifier =
    scrollsInGrid { way -> listTakes(way, state.canScrollForward, state.canScrollBackward, reversed = false) }

/**
 * Whether a list that can scroll [forward] and [backward] takes a swipe that would move the grid
 * [way]. A [reversed] list (the drawer's search results, which run upward from the field) scrolls
 * forward toward its top, so it takes a finger going down forward.
 */
internal fun listTakes(way: Way, forward: Boolean, backward: Boolean, reversed: Boolean): Boolean =
    // A finger going up moves the grid down, and scrolls a list toward its end: forward, or backward when reversed.
    if ((way == Way.DOWN) != reversed) forward else backward

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
        // Over the minimap the grid stays where it is: its drags move tiles, not home (lor-6get).
        if (ui.minimap) return@awaitEachGesture
        // Any press on a half makes it the one a fold lands on.
        grid.pressed(down.position.x, size.width)?.let { android.util.Log.d("LorGrid", "touched $it on ${grid.inner.screen.cells}") }
        // A widget being resized, and the widget picker over home, have the finger (lor-r0f9); a
        // press something took first (a press outside a resize frame, which only closes it) is not the grid's.
        if (down.isConsumed || ui.drag.held != null || ui.folder != null || ui.resizing != null || ui.widgetPicker != null || down.position.y < topPx()) return@awaitEachGesture
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
            grid.nextMove(way) ?: return@awaitEachGesture
        }
        val from = grid.cell
        val sizePx = (if (move.way.horizontal) size.width else size.height).toFloat()
        val finger = FollowFinger(grid.follow, sizePx, density, vertical = !move.way.horizontal, onTick = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }) { toOne, dpS ->
            // One line per release, which the emulator checks read (as the drawer's did, launcher polish 7.1).
            android.util.Log.d("LorMotion", "grid $from to ${move.to} let go at ${grid.follow.position} of ${(sizePx / density).toInt()} dp, $dpS dp/s: ${if (toOne) "goes" else "stays"}")
        }
        // Held before the move is set, so the settle never sees a move nobody has.
        grid.move = move
        followAlong(down, first, finger, tracker, PointerEventPass.Initial, move.way.horizontal, move.way.sign)
    }
}

/**
 * The cells home keeps composed ([GridPlace.composed]), each where [GridPlace.offsetOf] puts it, full
 * screen or, as a half of a pair on the inner screen, half of it (lor-el51): the screen on show, the
 * one a move goes to, and the screens a move from here reaches (the strategy's Shape: so a home
 * press stays instant however many panels there are). Each cell is keyed by its place, in the same
 * spot in the tree on both screen classes, so a fold moves a panel rather than recreating it.
 * Placed rather than drawn with an offset, so what each cell reports of where it is (drop zones,
 * sources) follows it; during a move only the move's two screens are placed, and the others keep
 * the off-screen place they last reported.
 */
@Composable
internal fun GridCells(grid: GridPlace, cell: @Composable (Panel) -> Unit) {
    val shown = grid.composed().mapNotNull(grid::panel)
    ComposeLayout({
        shown.forEach { panel ->
            key(panel.at) {
                DisposableEffect(Unit) {
                    android.util.Log.d("LorGrid", "composed ${panel.at} ${panel.type}")
                    onDispose { android.util.Log.d("LorGrid", "left ${panel.at}") }
                }
                CompositionLocalProvider(LocalCellSides provides grid.sidesOf(panel.at)) { cell(panel) }
            }
        }
    }) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val placeables = measurables.mapIndexed { i, m -> m.measure(Constraints.fixed(grid.widthOf(shown[i].at, w), h)) }
        layout(w, h) {
            // Each cell on its own layer, so a move only moves layers rather than redrawing the cells
            // into home's (the drawer's first frame, 7.8); still placed, so what they report follows them.
            placeables.forEachIndexed { i, p -> grid.offsetOf(shown[i].at, w, h)?.let { p.placeWithLayer(it) } }
        }
    }
}

/** Which of the screen's sides a cell touches: a half of a pair touches one, a cell alone both. */
enum class Sides { BOTH, LEFT, RIGHT }

/** The sides the cell being drawn touches, for [cellInsetsPadding]. */
val LocalCellSides = compositionLocalOf { Sides.BOTH }

/**
 * The system's bars and cutout, without the keyboard. Home's window is `adjustResize` (lor-sqfe),
 * so the keyboard arrives as an inset instead of panning the window: home pads by these, and only
 * a page with a field (the drawer, the agent page) pads by the keyboard too, so the grid and the
 * pages beside it stay where they are.
 */
val WindowInsets.Companion.bars: WindowInsets
    @Composable get() = systemBars.union(displayCutout)

/**
 * Pads for the system's insets (bars, cutout; not the keyboard, see [bars]) on the sides this cell
 * touches only: a half of a pair pads its outer side, top and bottom, so no gap opens where the
 * halves meet (lor-el51).
 */
fun Modifier.cellInsetsPadding(): Modifier = composed {
    val sides = when (LocalCellSides.current) {
        Sides.BOTH -> WindowInsetsSides.Horizontal
        Sides.LEFT -> WindowInsetsSides.Left
        Sides.RIGHT -> WindowInsetsSides.Right
    }
    windowInsetsPadding(WindowInsets.bars.only(sides + WindowInsetsSides.Vertical))
}

/**
 * The empty page shown while something is held (launcher polish 4.5), after the last in [layout]'s
 * start row, or null when there's no room. Its own inner screen (lor-aow8): paired with a lone half,
 * it would turn that half narrow under the finger.
 */
fun heldPanel(layout: Layout): Panel? =
    layout.takeIf { it.roomForPanel }?.withNewPanel(emptyList())?.first?.panels?.last()?.let { (it as Panel.Stack).copy(wide = Wide.FULL) }

/** A panel's type as the file writes it. */
val Panel.type: String
    get() = when (this) {
        is Panel.Stack -> "stack"
        is Panel.T3 -> "t3"
        is Panel.Drawer -> "drawer"
        is Panel.Lor -> "lor"
    }
