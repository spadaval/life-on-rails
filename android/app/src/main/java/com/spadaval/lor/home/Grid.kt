package com.spadaval.lor.home

/*
 * How home moves between the grid's cells (the Panels mission's Shape; decision lor-y7qi), with
 * no Compose so the JVM tests reach it. [GridHost.kt] draws the cell on screen and its neighbours
 * and moves between them with the finger.
 *
 * Read before building (Launcher polish's D1): Neo Launcher's `PagedView` (Launcher3's,
 * Apache-2.0 in AOSP, GPL-3.0 as Neo ships it) snaps to the next page on a significant move or a
 * fling and stops at the ends unless its "cycle scrolling" is on; here the ends stop too, and the
 * commit rule is the drawer's own ([FollowMotion]). Kvaesitso's `LauncherScaffold` (GPL-3.0, ideas
 * only) picks a swipe's axis once, at the touch slop, by its larger component, and lets a vertical
 * swipe through to a scrolling page only when that page is at its edge (`isAtTop`/`isAtBottom`).
 */

/**
 * Where the cell a move goes to lies, on screen. [RIGHT] is the next column (a swipe left
 * reaches it); [DOWN] is the next row, which a swipe up reaches, as the drawer is under home.
 */
enum class Way(val horizontal: Boolean, val sign: Int) {
    LEFT(true, -1), RIGHT(true, 1), UP(false, -1), DOWN(false, 1);

    val opposite: Way get() = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        UP -> DOWN
        DOWN -> UP
    }

    companion object {
        /** The way a finger that moved ([dx], [dy]) takes the grid: the other way from the finger, along its larger component. */
        fun ofSwipe(dx: Float, dy: Float): Way =
            if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) (if (dx < 0) RIGHT else LEFT) else (if (dy < 0) DOWN else UP)
    }
}

/** A vertical move that arrived: from [from], going [way]. A move back the other way returns there. */
data class Arrival(val from: Cell, val way: Way)

object GridMoves {
    /**
     * The cell a move [way] from [from] reaches among [cells], or null when there is none.
     * - Across: the nearest cell that way in the row, past any gap; the ends stop (no wrap).
     * - Up and down (decision lor-y7qi): the next row that way, wrapping, that holds any cell;
     *   back the way the last vertical move came ([back]), the cell it came from while it is still
     *   there; otherwise the cell in the same column, else the nearest by column, the left one on a tie.
     */
    fun next(cells: Collection<Cell>, from: Cell, way: Way, back: Arrival? = null): Cell? {
        if (way.horizontal) {
            val row = cells.filter { it.row == from.row && (it.column - from.column) * way.sign > 0 }
            return row.minByOrNull { kotlin.math.abs(it.column - from.column) }
        }
        val rows = cells.map { it.row }.filter { it != from.row }.distinct().sorted()
        if (rows.isEmpty()) return null
        val row = if (way.sign > 0) rows.firstOrNull { it > from.row } ?: rows.first() else rows.lastOrNull { it < from.row } ?: rows.last()
        if (back != null && way == back.way.opposite && back.from.row == row && back.from in cells) return back.from
        return nearest(cells, row, from.column)
    }

    /** The cell of [row] in [column], else the nearest by column, the left one on a tie; null for an empty row. */
    fun nearest(cells: Collection<Cell>, row: Int, column: Int): Cell? =
        cells.filter { it.row == row }.minWithOrNull(compareBy({ kotlin.math.abs(it.column - column) }, { it.column }))

    /**
     * Where home stands when [cell] may have gone (an edit removed its panel): [cell] while it is
     * there, else the nearest in its row, else [start].
     */
    fun stay(cells: Collection<Cell>, cell: Cell, start: Cell): Cell =
        if (cell in cells) cell else nearest(cells, cell.row, cell.column) ?: start

    /**
     * The way to draw a move from [from] straight to [to] (a home press, back): across in a row;
     * back to the row the last vertical move ([back]) came from, the way it came; else by rows.
     */
    fun wayTo(from: Cell, to: Cell, back: Arrival? = null): Way = when {
        from.row == to.row -> if (to.column < from.column) Way.LEFT else Way.RIGHT
        back != null && back.from.row == to.row -> back.way.opposite
        to.row < from.row -> Way.UP
        else -> Way.DOWN
    }

    /** The cells kept composed: [at] and each cell a move from it reaches. */
    fun window(cells: Collection<Cell>, at: Cell, back: Arrival?): Set<Cell> =
        setOf(at) + Way.entries.mapNotNull { next(cells, at, it, back) }
}
