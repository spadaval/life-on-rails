package com.spadaval.lor.concepts.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import com.spadaval.lor.home.BackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*
 * The phone's frame of pages: pages side by side that follow the finger (a pager), a drawer that
 * rises from the bottom with the finger on the home page, and a swipe down on home kept for the
 * notification shade, as the brief's first decision has it. A direction gives the pages and the
 * drawer; the shell owns the gestures.
 *
 * Make the state with [ShellState] above anything that branches on the screen (cover or inner), so a
 * fold keeps the page and the drawer.
 */
class ShellState(pages: Int, val home: Int) {
    val pager = PagerState(currentPage = home) { pages }

    /** The drawer: 0 closed, 1 open. It follows the finger between. */
    val drawer = Animatable(0f)

    /** The stand-in for Android's shade, shown after a swipe down on home: the lab can't open the real one. */
    var shade by mutableStateOf(false)

    val drawerOpen: Boolean get() = drawer.targetValue > 0.5f

    /** The shell's height in px, which a drag on the drawer measures against; PageShell sets it. */
    internal var heightPx = 1f

    suspend fun openDrawer(ms: Int = 240) = drawer.animateTo(1f, tween(ms))
    suspend fun closeDrawer(ms: Int = 200) = drawer.animateTo(0f, tween(ms))
    suspend fun goTo(page: Int) = pager.animateScrollToPage(page)
}

/**
 * The pages and the drawer. [page] draws one page; [drawer] draws the drawer's surface, which the
 * shell slides up by [ShellState.drawer]. A double tap on home calls [onDoubleTap]; a swipe down on
 * home calls [onSwipeDown] (a concept shows the shade's stand-in).
 */
@Composable
fun PageShell(
    state: ShellState,
    modifier: Modifier = Modifier,
    onDoubleTap: () -> Unit = {},
    onSwipeDown: () -> Unit = { state.shade = true },
    drawer: @Composable (close: () -> Unit) -> Unit,
    page: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val height = with(LocalDensity.current) { maxHeight.toPx() }
        state.heightPx = height
        HorizontalPager(state.pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { i ->
            Box(
                Modifier.fillMaxSize().then(
                    if (i == state.home) Modifier
                        .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
                        .pointerInput(height) { drawerDrag(state, height, scope, onSwipeDown) }
                    else Modifier,
                ),
            ) { page(i) }
        }
        if (state.drawer.value > 0f || state.drawer.targetValue > 0f) {
            val close: () -> Unit = { scope.launch { state.closeDrawer() } }
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { translationY = (1f - state.drawer.value) * height }
                    .nestedScroll(remember(state, height) { DrawerScroll(state, height, scope) }),
            ) { drawer(close) }
            BackHandler(state.drawerOpen) { close() }
        }
    }
}

@Composable
private fun <T> remember(a: Any?, b: Any?, make: () -> T): T = androidx.compose.runtime.remember(a, b) { make() }

/** A vertical drag on home: up raises the drawer with the finger; down is the shade's. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.drawerDrag(state: ShellState, height: Float, scope: CoroutineScope, onSwipeDown: () -> Unit) {
    var total = 0f
    var down = false
    val tracker = VelocityTracker()
    detectVerticalDragGestures(
        onDragStart = { total = 0f; down = false; tracker.resetTracking() },
        onDragEnd = {
            if (down) { onSwipeDown(); return@detectVerticalDragGestures }
            val v = tracker.calculateVelocity().y
            scope.launch { if (v < -600f || (v < 600f && state.drawer.value > 0.3f)) state.openDrawer(200) else state.closeDrawer() }
        },
        onDragCancel = { scope.launch { state.closeDrawer() } },
    ) { change, dy ->
        tracker.addPosition(change.uptimeMillis, change.position)
        total += dy
        if (total > 0f && state.drawer.value == 0f) { down = total > 24f; return@detectVerticalDragGestures }
        down = false
        change.consume()
        scope.launch { state.drawer.snapTo((-total / height).coerceIn(0f, 1f)) }
    }
}

/**
 * Lets the drawer's own list close it: a drag down with the list at its top moves the drawer down
 * with the finger, and the fling decides whether it closes.
 */
private class DrawerScroll(val state: ShellState, val height: Float, val scope: CoroutineScope) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < 0f && state.drawer.value < 1f) {
            scope.launch { state.drawer.snapTo((state.drawer.value - available.y / height).coerceIn(0f, 1f)) }
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 0f && source == NestedScrollSource.UserInput) {
            scope.launch { state.drawer.snapTo((state.drawer.value - available.y / height).coerceIn(0f, 1f)) }
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.drawer.value in 0.001f..0.999f) {
            if (available.y > 600f || state.drawer.value < 0.7f) state.closeDrawer() else state.openDrawer(160)
            return available
        }
        return Velocity.Zero
    }
}

/**
 * Lets a part of the drawer that doesn't scroll (its search field, its header) close it by a drag
 * down that follows the finger, as its list does.
 */
@Composable
fun Modifier.dragsDrawer(state: ShellState): Modifier {
    val scope = rememberCoroutineScope()
    return pointerInput(state) {
        val tracker = VelocityTracker()
        detectVerticalDragGestures(
            onDragStart = { tracker.resetTracking() },
            onDragEnd = {
                val v = tracker.calculateVelocity().y
                scope.launch { if (v > 600f || state.drawer.value < 0.7f) state.closeDrawer() else state.openDrawer(160) }
            },
        ) { change, dy ->
            tracker.addPosition(change.uptimeMillis, change.position)
            change.consume()
            scope.launch { state.drawer.snapTo((state.drawer.value - dy / state.heightPx).coerceIn(0f, 1f)) }
        }
    }
}
