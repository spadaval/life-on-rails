package com.spadaval.lor.home

/*
 * The launcher's motion rules that need no Compose (launcher polish 7.1), so the JVM tests reach
 * them. The drawer follows the finger: one position from 0 (closed) to 1 (open) that a finger sets
 * and a spring finishes, after Launcher3 / Lawnchair's `AllAppsTransitionController` (Apache-2.0)
 * and Kvaesitso's `LauncherScaffold` (GPL-3.0; ideas only: the tick at the commit point).
 */
object DrawerMotion {
    /** How far from where a gesture started a release commits, as a share of the drawer's height. */
    const val COMMIT = 0.3f

    /** A fling faster than this (dp/s) decides by its direction alone. */
    const val FLING = 800f

    /** Where a gesture that started at [start] commits: 30% of the travel toward the other end. */
    fun commitPoint(fromOpen: Boolean, start: Float): Float = if (fromOpen) start - COMMIT else start + COMMIT

    /**
     * Whether the drawer ends open when a finger that started at [start] (open or not: [fromOpen])
     * lets go at [shown], moving up at [upDpS] dp/s (negative: down).
     */
    fun settles(fromOpen: Boolean, start: Float, shown: Float, upDpS: Float): Boolean = when {
        upDpS > FLING -> true
        upDpS < -FLING -> false
        fromOpen -> shown > commitPoint(true, start)
        else -> shown > commitPoint(false, start)
    }

    /** Whether a move from [before] to [after] crossed [point], either way (a tick, once per crossing). */
    fun crossed(point: Float, before: Float, after: Float): Boolean = (before < point) != (after < point)
}
