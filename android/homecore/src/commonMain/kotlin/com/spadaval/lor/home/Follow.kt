package com.spadaval.lor.home

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.spadaval.lor.ui.Motion

/*
 * Something a finger moves and a spring finishes (launcher polish 7.1, made neutral for the
 * panels' grid after Increment 7's review, R9): each move between the grid's cells (lor-qdmy),
 * the drawer's included.
 * The rules and their sources are [FollowMotion]'s.
 */

/**
 * One position from 0 to 1 and where it is going. A finger sets [position] while [held];
 * otherwise [SettleFollow] moves it to [target], with the finger's [fling] or at once when [snap] is set.
 */
class Follow(initial: Float = 0f) {
    var position by mutableFloatStateOf(initial)
    var target by mutableFloatStateOf(initial)
    var held by mutableStateOf(false)

    /** The velocity (positions per second, toward 1 positive) a finger let go with; the settle takes it. */
    var fling = 0f

    /** Jump to [target] rather than animate (a home press from elsewhere); the settle takes it. */
    var snap by mutableStateOf(false)
}

/**
 * A finger moving a [Follow] (launcher polish 7.1): 1:1 with its travel over [sizePx], [onTick] at
 * the commit point either way, and on release the target [FollowMotion.settles] picks, from the
 * finger's velocity. It takes the follow from where it is, going away from its target: a finger
 * on a follow headed for 1 is taking it toward 0. [onCommit] gets whether it ends at 1, and the
 * velocity in dp/s toward 1, before the follow is let go. A [vertical] move takes the vertical
 * rule, in dp of its size (lor-83uq).
 */
class FollowFinger(
    private val follow: Follow,
    private val sizePx: Float,
    private val density: Float,
    vertical: Boolean = false,
    private val onTick: () -> Unit,
    private val onCommit: (toOne: Boolean, dpS: Float) -> Unit,
) {
    private val fromOne = follow.target == 1f
    private val start = follow.position
    private val sizeDp = if (vertical) sizePx / density else null
    private val point = FollowMotion.commitPoint(fromOne, start, sizeDp)
    init {
        follow.held = true
    }

    /** The finger is [travel] px from where it pressed, positive toward 0. */
    fun move(travel: Float) {
        val before = follow.position
        val now = (start - travel / sizePx).coerceIn(0f, 1f)
        follow.position = now
        if (FollowMotion.crossed(point, before, now)) onTick()
    }

    /** Let go at [v] px/s, positive toward 0. */
    fun release(v: Float) {
        val dpS = -v / density
        val toOne = FollowMotion.settles(fromOne, start, follow.position, dpS, sizeDp)
        follow.fling = -v / sizePx
        follow.target = if (toOne) 1f else 0f
        onCommit(toOne, dpS)
        follow.held = false
    }
}

/** Moves [follow] to its target whenever no finger has it: the one settle effect for each [Follow]. */
@Composable
fun SettleFollow(follow: Follow) {
    LaunchedEffect(follow, follow.target, follow.held, follow.snap) {
        if (follow.held) return@LaunchedEffect
        val to = follow.target
        val fling = follow.fling
        follow.fling = 0f
        if (follow.snap) {
            follow.snap = false
            follow.position = to
        } else if (follow.position != to) {
            // A finger that takes it mid-settle owns it from that frame, before this effect is cancelled.
            animate(follow.position, to, fling, Motion.settle()) { v, _ -> if (!follow.held) follow.position = v }
        }
    }
}

/**
 * Waits for [down]'s finger to pass the touch slop; returns that move, or null (a tap, a lift, or
 * a move something else took). The caller picks the axis from it (Kvaesitso's `LauncherScaffold`
 * decides once, here, by the larger component).
 */
internal suspend fun AwaitPointerEventScope.awaitSlop(down: PointerInputChange, tracker: VelocityTracker, pass: PointerEventPass): PointerInputChange? {
    tracker.addPosition(down.uptimeMillis, down.position)
    while (true) {
        val c = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id } ?: return null
        if (!c.pressed || c.isConsumed) return null
        tracker.addPosition(c.uptimeMillis, c.position)
        if ((c.position - down.position).getDistance() > viewConfiguration.touchSlop) return c
    }
}

/**
 * Moves [finger] with [down]'s finger from [first] on, measured from where it pressed along one
 * axis ([horizontal] or not), and lets it go on the lift. [sign] is the way along that axis that
 * takes the follow toward 1 when the finger moves the other way: 1 when a finger moving left or up
 * takes it toward 1 (the cell to the right, the row below), -1 for the reverse.
 */
internal suspend fun AwaitPointerEventScope.followAlong(down: PointerInputChange, first: PointerInputChange, finger: FollowFinger, tracker: VelocityTracker, pass: PointerEventPass, horizontal: Boolean, sign: Int) {
    fun travel(c: PointerInputChange) = (if (horizontal) c.position.x - down.position.x else c.position.y - down.position.y) * sign
    first.consume()
    finger.move(travel(first))
    var v = 0f
    // Released however the gesture ends, a cancelled pointer coroutine too, so nothing is left held.
    try {
        while (true) {
            val c = awaitPointerEvent(pass).changes.firstOrNull { it.id == down.id } ?: return
            c.consume()
            tracker.addPosition(c.uptimeMillis, c.position)
            finger.move(travel(c))
            if (!c.pressed) {
                val velocity = tracker.calculateVelocity()
                v = (if (horizontal) velocity.x else velocity.y) * sign
                return
            }
        }
    } finally {
        finger.release(v)
    }
}
