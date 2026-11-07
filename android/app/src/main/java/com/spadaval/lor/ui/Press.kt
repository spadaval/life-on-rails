package com.spadaval.lor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * The pressed state of a tile (lor-gnk0): while a finger is down on it, what the click node draws
 * shrinks to [pressed] about its centre, and grows back when the finger lifts or the press is
 * cancelled, each over [Motion.QUICK]. One-shot, nothing loops; at the animator duration scale 0
 * it jumps (Compose's MotionDurationScale reaches the node's animations).
 *
 * It is the click node's own [androidx.compose.foundation.Indication] (in place of the ripple), so
 * a tile keeps one click node for TalkBack and agent-device, and the press doesn't touch the
 * gestures: the tap, the long press and a drag's pickup see the same pointer events as before. It
 * only changes how the content is drawn, so the layout's bounds (which an app's launch grows from,
 * and drag and drop hit-test against) stay where they are.
 *
 * After Launcher3's `FastBitmapDrawable` (Apache-2.0, via Lawnchair): its icon answers the
 * pressed state by animating its scale over a short feedback duration and animating back on
 * release. Launcher3 scales up; DESIGN.md's quiet feedback (and Sumeet's Smart Launcher) shrink.
 */
class PressScale(private val pressed: Float) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = Node(interactionSource, pressed)

    override fun equals(other: Any?) = other is PressScale && other.pressed == pressed

    override fun hashCode() = pressed.hashCode()

    private class Node(private val source: InteractionSource, private val pressed: Float) : Modifier.Node(), DrawModifierNode {
        private val scale = Animatable(1f)

        override fun onAttach() {
            coroutineScope.launch {
                var down = 0
                source.interactions.collect { i ->
                    when (i) {
                        is PressInteraction.Press -> down++
                        is PressInteraction.Release, is PressInteraction.Cancel -> down = (down - 1).coerceAtLeast(0)
                        else -> return@collect
                    }
                    // A new target cancels the running animation, from where it is.
                    launch { scale.animateTo(if (down > 0) pressed else 1f, Motion.quick()) { invalidateDraw() } }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            val s = scale.value
            if (s == 1f) return drawContent()
            val content = this
            scale(s) { content.drawContent() }
        }
    }

    companion object {
        /** An app or folder tile (home, the dock, a folder, the drawer, search's apps). */
        val Tile = PressScale(0.9f)

        /** A full-width row of search results: the same feedback, by fewer dp. */
        val Row = PressScale(0.97f)
    }
}
