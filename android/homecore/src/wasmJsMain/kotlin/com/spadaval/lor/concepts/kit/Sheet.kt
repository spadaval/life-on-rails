package com.spadaval.lor.concepts.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.spadaval.lor.home.BackHandler
import com.spadaval.lor.lab.labAnchor
import kotlinx.coroutines.launch

/*
 * A bottom sheet over the page: it rises over a scrim (wrap the page in [Backdrop] to blur it), follows the finger
 * by its handle or by its list once the list is at its top, rests at its [detents] (fractions of the
 * screen's height, smallest first), and goes on a drag below the lowest, a tap on the scrim or Back.
 * On a wide screen (the inner one) it is centred and at most [maxWidth] wide.
 *
 * It draws while [open] or while leaving; the caller keeps [open] in state above the screen's
 * branches. [anchor] names the sheet for gesture scripts (`<anchor>` the sheet, `<anchor>-scrim`).
 */
@Composable
fun Sheet(
    open: Boolean,
    onDismiss: () -> Unit,
    anchor: String,
    detents: List<Float> = listOf(0.9f),
    maxWidth: Dp = 560.dp,
    content: @Composable () -> Unit,
) {
    val look = LocalKitLook.current
    val shown = remember { Animatable(0f) }
    var rest by remember { mutableStateOf(detents.first()) }
    LaunchedEffect(open) {
        if (open) { rest = detents.first(); shown.animateTo(rest, tween(look.motionMs)) } else shown.animateTo(0f, tween(look.motionMs - 40))
    }
    if (!open && shown.value == 0f) return
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = with(LocalDensity.current) { maxHeight.toPx() }
        val fade = { (shown.value / detents.first()).coerceIn(0f, 1f) }
        fun settle(velocity: Float) = scope.launch {
            val at = shown.value
            val target = when {
                velocity > 900f || at < detents.first() * 0.6f -> 0f
                velocity < -900f -> detents.last()
                else -> detents.minBy { kotlin.math.abs(it - at) }
            }
            if (target == 0f) onDismiss() else { rest = target; shown.animateTo(target, tween(look.motionMs - 40)) }
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = fade() }.background(look.scrim).labAnchor("$anchor-scrim")
                .clickable(remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        )
        val connection = remember(height) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < 0f && shown.value < rest) {
                        scope.launch { shown.snapTo((shown.value - available.y / height).coerceAtMost(detents.last())) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0f && source == NestedScrollSource.UserInput) {
                        scope.launch { shown.snapTo((shown.value - available.y / height).coerceAtLeast(0f)) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (shown.value < rest - 0.001f || shown.value > rest + 0.001f) { settle(available.y); return available }
                    return Velocity.Zero
                }
            }
        }
        val shape = RoundedCornerShape(topStart = look.radius, topEnd = look.radius)
        Column(
            Modifier.align(Alignment.BottomCenter).widthIn(max = maxWidth).fillMaxWidth()
                .height(maxHeight * detents.last())
                .graphicsLayer { translationY = (detents.last() - shown.value) * height }
                .shadow(24.dp, shape, clip = false)
                .clip(shape)
                .background(look.glass)
                .border(1.dp, look.glassLine, shape)
                .nestedScroll(connection)
                .labAnchor(anchor),
        ) {
            val tracker = remember { VelocityTracker() }
            Box(
                Modifier.fillMaxWidth().height(28.dp).pointerInput(height) {
                    detectVerticalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = { settle(tracker.calculateVelocity().y) },
                    ) { change, dy ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        scope.launch { shown.snapTo((shown.value - dy / height).coerceIn(0f, detents.last())) }
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(look.muted.copy(alpha = 0.5f)))
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(bottom = 8.dp)) { content() }
        }
        BackHandler(open) { onDismiss() }
    }
}
