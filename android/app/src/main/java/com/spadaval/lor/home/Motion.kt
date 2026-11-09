package com.spadaval.lor.home

/*
 * The launcher's motion rules that need no Compose (launcher polish 7.1), so the JVM tests reach
 * them. A [Follow] (each move between the grid's cells, the drawer's too) is one position from 0 to 1 that a finger
 * sets and a spring finishes, after Launcher3 / Lawnchair's `AllAppsTransitionController`
 * (Apache-2.0) and Kvaesitso's `LauncherScaffold` (GPL-3.0; ideas only: the tick at the commit point).
 * "Open" below is at 1, as the drawer is.
 */
object FollowMotion {
    /**
     * The share of the travel left, from where a gesture started toward the other end, past which
     * a horizontal release commits: 30% of the way from fully closed or open (see [commitPoint]).
     */
    const val COMMIT = 0.3f

    /** A horizontal fling faster than this (dp/s) decides by its direction alone. */
    const val FLING = 800f

    /**
     * A vertical move (the drawer's included) commits once the finger has gone this far (dp)
     * toward the other cell (lor-83uq): on the Fold's cover, 30% of the screen was about 290 dp,
     * and swipes of 150 to 190 dp fell back.
     */
    const val COMMIT_DP = 110f

    /** A vertical fling faster than this (dp/s) decides by its direction, once it has gone [FLING_MIN_DP]. */
    const val FLING_V = 400f

    /** How far (dp) a vertical fling toward the other cell must have gone to commit, so a flick in place doesn't. */
    const val FLING_MIN_DP = 24f

    /**
     * Where a gesture that started at [start] commits: 30% of the travel left toward the other
     * end, so a follow caught mid-settle near one end still has a point short of the other. A
     * vertical move passes its height as [sizeDp] and commits [COMMIT_DP] from [start] instead,
     * or sooner when that is past the 30% point.
     */
    fun commitPoint(fromOpen: Boolean, start: Float, sizeDp: Float? = null): Float {
        val left = if (fromOpen) start else 1f - start
        val share = if (sizeDp == null) COMMIT * left else minOf(COMMIT_DP / sizeDp, COMMIT * left)
        return if (fromOpen) start - share else start + share
    }

    /**
     * Whether it ends open (at 1) when a finger that started at [start] (taking it from open or
     * not: [fromOpen]) lets go at [shown], moving toward 1 at [upDpS] dp/s (negative: toward 0).
     * [sizeDp] is a vertical move's height, which takes the vertical rule ([COMMIT_DP], [FLING_V]).
     */
    fun settles(fromOpen: Boolean, start: Float, shown: Float, upDpS: Float, sizeDp: Float? = null): Boolean {
        val point = commitPoint(fromOpen, start, sizeDp)
        if (sizeDp == null) return when {
            upDpS > FLING -> true
            upDpS < -FLING -> false
            else -> shown > point
        }
        // Toward the other end: up (toward 1) from closed, down from open.
        val away = if (fromOpen) -upDpS else upDpS
        val gone = (if (fromOpen) start - shown else shown - start) * sizeDp
        val commits = when {
            away < -FLING_V -> false
            away > FLING_V && gone >= FLING_MIN_DP -> true
            else -> if (fromOpen) shown < point else shown > point
        }
        return commits != fromOpen
    }

    /** Whether a move from [before] to [after] crossed [point], either way (a tick, once per crossing). */
    fun crossed(point: Float, before: Float, after: Float): Boolean = (before < point) != (after < point)
}

/**
 * Where a folder's card is drawn as it opens from its tile and closes back into it (launcher polish
 * 7.2; Launcher3's `FolderAnimationManager`, Apache-2.0: the content scales from the icon's size).
 * At [shown] 0 the card sits on the tile at the tile's size; at 1 it is in its own place. Without a
 * tile (it went away) it only fades.
 */
object FolderMotion {
    class Frame(val scale: Float, val dx: Float, val dy: Float, val alpha: Float)

    /**
     * The card's bounds and the tile's, as left, top, right, bottom; [tile] null to fade. [back] is
     * predictive back's scale (launcher polish 7.3), applied about the tile's centre (the card's
     * own without a tile), so the card leans toward where it will close.
     */
    fun at(shown: Float, card: FloatArray, tile: FloatArray?, back: Float = 1f): Frame {
        val cx = (card[0] + card[2]) / 2f
        val cy = (card[1] + card[3]) / 2f
        if (tile == null || card[2] <= card[0]) return Frame(back, 0f, 0f, shown)
        val s0 = (tile[2] - tile[0]) / (card[2] - card[0])
        val rest = 1f - shown
        val px = (tile[0] + tile[2]) / 2f
        val py = (tile[1] + tile[3]) / 2f
        val dx = (px - cx) * rest
        val dy = (py - cy) * rest
        // Back's scale about the tile's centre moves the card's centre toward it.
        val bx = px + back * (cx + dx - px) - cx
        val by = py + back * (cy + dy - py) - cy
        // The card is fully drawn by a third of the way, so its contents don't swim in a fog.
        return Frame((s0 + (1f - s0) * shown) * back, bx, by, (shown * 3f).coerceAtMost(1f))
    }
}

/**
 * A key per item for home's tiles (launcher polish 7.5), so a tile keeps its state when an edit
 * moves it to another row or slot: an app by its ref, a folder by its name, each with the
 * occurrence of that key so far, since folder names may repeat (Increment 1's R4).
 */
fun itemKeys(items: List<LayoutItem>): List<String> {
    val seen = HashMap<String, Int>()
    return items.map { item ->
        val base = when (item) {
            is AppRef -> "app:$item"
            is Folder -> "folder:${item.name}"
        }
        val n = seen.merge(base, 1, Int::plus)!!
        "$base#$n"
    }
}
