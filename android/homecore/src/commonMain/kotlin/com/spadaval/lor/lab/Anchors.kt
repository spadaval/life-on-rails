package com.spadaval.lor.lab

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity

/*
 * Named places on the screen, for gesture scripts (scripts/lab/gesture.mjs). Compose for the web
 * draws to one canvas, so a script can't find a DOM node; it asks for an anchor by name instead,
 * and the lab page publishes every anchor's bounds in its body's `data-anchors`. In the app the
 * modifier only records the bounds, which nothing reads.
 */

object LabAnchors {
    /**
     * Set by the lab page before anything draws. Off, [labAnchor] adds nothing to a modifier, so the
     * screens the app draws from this module (home, since lor-xdoi) carry no anchors on the phone.
     */
    var on = false

    private val rects = mutableStateMapOf<String, Rect>()

    /** Every anchor on screen now: its name and its visible bounds in dp from the screen's top left. */
    val all: Map<String, Rect> get() = rects

    internal fun put(name: String, rect: Rect) {
        if (rects[name] != rect) rects[name] = rect
    }

    internal fun remove(name: String) {
        rects.remove(name)
    }
}

/** Names this element for gesture scripts: its visible bounds are [LabAnchors.all]`[name]` while it is on screen. */
fun Modifier.labAnchor(name: String): Modifier = if (LabAnchors.on) this then AnchorElement(name) else this

private data class AnchorElement(val name: String) : ModifierNodeElement<AnchorNode>() {
    override fun create() = AnchorNode(name)
    override fun update(node: AnchorNode) {
        if (node.name != name) {
            LabAnchors.remove(node.name)
            node.name = name
        }
    }
}

private class AnchorNode(var name: String) : Modifier.Node(), GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val d = currentValueOf(LocalDensity).density
        val b = coordinates.boundsInRoot()
        LabAnchors.put(name, Rect(b.left / d, b.top / d, b.right / d, b.bottom / d))
    }

    override fun onDetach() = LabAnchors.remove(name)
}
