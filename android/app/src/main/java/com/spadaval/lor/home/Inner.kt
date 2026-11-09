package com.spadaval.lor.home

/*
 * The inner screen's rules (the Panels mission's Shape, "The inner screen" and "Folding"; decision
 * lor-b1dx), with no Compose so the JVM tests reach them: which panels share an inner screen, where
 * a move between inner screens goes, where a fold or an unfold lands, and how a stack flows its
 * blocks into two columns. [GridMoves] stays the rule between cells; these build on it.
 */

/** One inner screen: [left] alone, drawn at full width, or [left] and [right], two halves side by side. */
data class InnerScreen(val left: Cell, val right: Cell? = null) {
    val cells: List<Cell> get() = listOfNotNull(left, right)
    val row: Int get() = left.row
    operator fun contains(cell: Cell): Boolean = cell == left || cell == right
}

/**
 * Where home is on the inner screen: the [screen] on show and its [focus], the cell up and down
 * moves start from and a fold lands on; null when none is set, and then [focusCell] is the left.
 */
data class InnerPlace(val screen: InnerScreen, val focus: Cell? = null) {
    val focusCell: Cell get() = focus ?: screen.left

    /** A touch on a half of this screen makes it the focus. */
    fun touched(cell: Cell): InnerPlace = if (cell in screen) copy(focus = cell) else this
}

/** A move between inner screens: where it lands, the way it goes, and the way back a vertical move leaves. */
data class InnerMove(val to: InnerPlace, val way: Way, val back: Arrival?)

object InnerScreens {
    /**
     * Every inner screen of [panels], row by row, left to right by column (decision lor-b1dx): a
     * `full` panel is one screen; a `half` shares one with the next panel in its row, past any gap,
     * when that panel is also a `half` not yet paired; otherwise it is alone and drawn full.
     */
    fun of(panels: Collection<Panel>): List<InnerScreen> =
        panels.groupBy { it.at.row }.toSortedMap().values.flatMap { row ->
            val screens = mutableListOf<InnerScreen>()
            var half: Cell? = null
            for (p in row.sortedBy { it.at.column }) {
                val waiting = half
                when {
                    p.wide == Wide.FULL -> {
                        if (waiting != null) screens += InnerScreen(waiting)
                        half = null
                        screens += InnerScreen(p.at)
                    }
                    waiting != null -> {
                        screens += InnerScreen(waiting, p.at)
                        half = null
                    }
                    else -> half = p.at
                }
            }
            half?.let { screens += InnerScreen(it) }
            screens
        }

    /** The inner screen holding [cell], or null when no panel is there. */
    fun holding(screens: Collection<InnerScreen>, cell: Cell): InnerScreen? = screens.firstOrNull { cell in it }

    /**
     * The inner screen holding [cell] with no focus: where a home press, back, or an edit puts home.
     * Null when no panel is at [cell].
     */
    fun place(screens: Collection<InnerScreen>, cell: Cell): InnerPlace? = holding(screens, cell)?.let { InnerPlace(it) }

    /**
     * Where a move [way] from [from] goes, or null when there is none or [from]'s screen isn't one of [screens].
     * - Across: the next screen in the row; the ends stop. It arrives with no focus.
     * - Up and down: [GridMoves.next] from the focus cell, with its way back ([back]), lands on the
     *   screen holding the cell it returns, with that cell as the focus.
     */
    fun next(screens: List<InnerScreen>, from: InnerPlace, way: Way, back: Arrival? = null): InnerMove? {
        // A screen not among [screens] (an edit re-paired its row) has no moves (lor-70hj).
        if (from.screen !in screens) return null
        if (way.horizontal) {
            val row = screens.filter { it.row == from.screen.row }
            val to = row.getOrNull(row.indexOf(from.screen) + way.sign) ?: return null
            return InnerMove(InnerPlace(to), way, null)
        }
        val start = from.focusCell
        val cell = GridMoves.next(screens.flatMap { it.cells }, start, way, back) ?: return null
        return InnerMove(InnerPlace(holding(screens, cell)!!, cell), way, Arrival(start, way))
    }

    /** Unfolding from [cell]: the inner screen holding it, with it as the focus. Null when no panel is there. */
    fun unfold(screens: Collection<InnerScreen>, cell: Cell): InnerPlace? = holding(screens, cell)?.let { InnerPlace(it, cell) }

    /** Folding from [place]: a full screen's cell, a pair's focus cell (the left when it has none). */
    fun fold(place: InnerPlace): Cell = place.focusCell
}

object StackFlow {
    /**
     * A stack's blocks in its wide form, as columns left to right, each top to bottom: split in
     * reading order, the left column taking the first half, rounded up. One block is a single
     * column across the panel. Generic, so the rule is tested on more blocks than the file has types.
     */
    fun <T> wide(blocks: List<T>): List<List<T>> {
        if (blocks.size <= 1) return listOf(blocks)
        val split = (blocks.size + 1) / 2
        return listOf(blocks.take(split), blocks.drop(split))
    }
}
