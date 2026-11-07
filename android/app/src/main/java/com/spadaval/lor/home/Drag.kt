package com.spadaval.lor.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/*
 * Dragging on home (launcher polish 4.3 to 4.6).
 *
 * One drag at a time, owned by home rather than by the item, as Launcher3's `DragController` and
 * `DragLayer` do (Lawnchair, Apache-2.0): a pointer handler on home's root follows the press from
 * the source it started on to whatever takes it, so the source may leave the screen (the drawer
 * closing, a folder panel closing). Launcher3's `PopupContainerWithArrow.createPreDragCondition`
 * gives the start: a long press opens the app's menu, and moving 16 dp from the press closes it
 * and picks the item up. A drop near an icon's centre goes onto it (`Workspace`'s folder-creation
 * radius), Remove sits at the top (`DeleteDropTarget`), and an open folder closes when the finger
 * has been outside it for 400 ms (`Folder.ON_EXIT_CLOSE_DELAY`).
 *
 * From Kvaesitso (GPL-3.0, `ui/component/dragndrop/DragAndDropGrid.kt`): the target is found by
 * hit-testing laid-out bounds against the finger, the held item is drawn at the finger above
 * everything, and a move is made after the finger has stayed 300 ms over its target
 * (`attemptMove`), on a working copy that home draws while the item is held. Here that copy is
 * a [LayoutFile] (`work`), moved by the pure [drop] edit, and the drop is one write.
 */

/** A rectangle in home's root coordinates, in px. Plain numbers, so the JVM tests reach [targetAt]. */
data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(x: Float, y: Float) = x in left..right && y in top..bottom
    val centerX: Float get() = (left + right) / 2
    val empty: Boolean get() = right <= left || bottom <= top
}

/** Something a held item can be dropped on, where it is on screen. */
sealed interface Zone {
    val box: Bounds

    /**
     * An item in an `apps` block, in the dock or in the open folder, at [spot]. Its icon's top is [iconTop]
     * and it is [iconSize] across, centred in [box].
     */
    data class Item(val spot: Spot, override val box: Bounds, val iconTop: Float, val iconSize: Float) : Zone

    /** A list's space: an `apps` block's grid, the dock, the open folder's card, the new panel. */
    data class Area(val place: Place, override val box: Bounds) : Zone

    data class Remove(override val box: Bounds) : Zone
}

/** How close to an icon's centre, as a share of its size, a drop goes onto it (Launcher3's folder radius). */
const val ONTO_SHARE = 0.55f

/**
 * The target under ([x], [y]). Remove first; with a folder open, only that folder's apps and card;
 * then an item (onto its icon's centre, else in front of it, or after it on its right half), then
 * a list's space after its items. Null where nothing takes a drop, so the item goes back.
 */
fun targetAt(x: Float, y: Float, zones: List<Zone>, heldIsFolder: Boolean, openFolder: Spot.Apps?, heldAt: Spot? = null): Target? {
    zones.firstOrNull { it is Zone.Remove && it.box.contains(x, y) }?.let { return Target.Remove }
    val shown = zones.filter { z ->
        when (z) {
            is Zone.Item -> if (openFolder != null) (z.spot as? Spot.InFolder)?.folder == openFolder else z.spot !is Spot.InFolder
            is Zone.Area -> if (openFolder != null) (z.place as? Place.InFolder)?.folder == openFolder else z.place !is Place.InFolder
            is Zone.Remove -> false
        }
    }
    shown.firstOrNull { it is Zone.Item && it.box.contains(x, y) }?.let { z ->
        z as Zone.Item
        val spot = z.spot
        val cy = z.iconTop + z.iconSize / 2
        val dx = x - z.box.centerX
        val dy = y - cy
        // Never onto the held item's own (empty) slot: that is where it already is.
        if (spot is Spot.Apps && spot != heldAt && !heldIsFolder && dx * dx + dy * dy <= (ONTO_SHARE * z.iconSize).let { it * it }) return Target.Onto(spot)
        return Target.Before(if (x > z.box.centerX) spot.next() else spot)
    }
    return shown.firstOrNull { it is Zone.Area && it.box.contains(x, y) }?.let { Target.AtEnd((it as Zone.Area).place) }
}

/** The place after this one in its list (which may be one past its end: in front of nothing). */
fun Spot.next(): Spot = when (this) {
    is Spot.Apps -> copy(index = index + 1)
    is Spot.Dock -> copy(index = index + 1)
    is Spot.InFolder -> copy(app = app + 1)
}

/** What a press can pick up, where it is on screen. [layer]: 0 home and the dock, 1 the open folder, 2 the drawer. */
class Source(val box: Bounds, val from: From, val layer: Int, val app: HomeApp?, val folder: ResolvedFolder?)

/** An item being held. */
class Held(
    /** The file home drew when the drag started; the drop refuses if the file is no longer this. */
    val original: LayoutFile,
    start: From,
    val item: LayoutItem,
    val app: HomeApp?,
    val folder: ResolvedFolder?,
    /** Edits a step and the drop need: the installed apps and a new folder's name. */
    val home: HomeContext,
) {
    var from: From by mutableStateOf(start)
    /** The working copy home draws while the item is held. */
    var work: LayoutFile by mutableStateOf(original)
    var finger: Offset by mutableStateOf(Offset.Zero)
    /** The target under the finger, waiting its 300 ms before it is applied to [work]. */
    var pending: Target? by mutableStateOf(null)
    /** An `Onto` or `Remove` target under the finger that would take the item. */
    var lit: Target? by mutableStateOf(null)
    /** -1 or 1 while the finger is at the screen's left or right edge, else 0. */
    var edge: Int by mutableStateOf(0)

    /** Picked up in the drawer: not yet on home, so the drop adds rather than moves. */
    val fromDrawer: Boolean = start is From.Drawer

    /** Where the held item sits in [work], when it sits in it. */
    val spot: Spot? get() = (from as? From.At)?.spot

    val isFolder: Boolean get() = item is Folder

    /** Applies [to] to [work] (a step while held); a refused step leaves it. */
    fun step(to: Target) {
        try {
            val d = work.drop(from, to, home.installed, strip = false, nameFor = home::nameFor)
            work = d.file
            // What the copy holds there now: a moved folder is pinned in it, and the next step checks against that.
            d.at?.let { from = From.At(it, d.file.itemAt(it) ?: item) }
        } catch (e: LayoutError) {
            // A full dock or page, a folder into the dock: the item stays where it is.
        }
    }

    /** Whether [to] would take the item. */
    fun takes(to: Target): Boolean = try {
        work.drop(from, to, home.installed, strip = false, nameFor = home::nameFor)
        true
    } catch (e: LayoutError) {
        false
    }
}

/**
 * The icon a drop let go of (launcher polish 7.5), drawn from where the finger left it ([from], its
 * centre in root coordinates) to the tile at [to]: into its slot ([Kind.SLOT], hidden until it
 * arrives), onto the app or folder it went into ([Kind.ONTO], fading as it gets there), or fading
 * where it is ([Kind.FADE], removed, or with no slot to go to). Kvaesitso's drop eases the item
 * back the same way (GPL-3.0, ideas only).
 */
class Settle(val app: HomeApp?, val folder: ResolvedFolder?, val from: Offset, val to: Spot?, val kind: Kind) {
    enum class Kind { SLOT, ONTO, FADE }

    /** From 0 (let go) to 1 (there); its own, so a new settle never draws a frame at the last one's end. */
    val progress = androidx.compose.animation.core.Animatable(0f)
}

/** The drag's state for home: what is held, what can be picked up and dropped on, and edit mode. */
class DragState {
    var held: Held? by mutableStateOf(null)

    /** Edit mode (4.3): a bar with Done, and taps that open no app. */
    var editing: Boolean by mutableStateOf(false)

    /** The icon let go, on its way to where it went (launcher polish 7.5). */
    var settling: Settle? by mutableStateOf(null)

    /** Whether the tile at [spot] is hidden: it is held there, or the settling icon is on its way to it. */
    fun hides(spot: Spot): Boolean = held?.spot == spot || settling?.let { it.kind == Settle.Kind.SLOT && it.to == spot } == true

    /** What a drop wrote, drawn until the store shows it, so home doesn't flash back to before it. */
    var landing: LayoutFile? by mutableStateOf(null)

    /** Laid-out sources and drop zones, by a key per composable; each removes its own when it leaves. */
    val sources = HashMap<Any, Source>()
    val zones = HashMap<Any, Zone>()

    /** The source under [at] in [layer], the top one showing. */
    fun sourceAt(at: Offset, layer: Int): Source? =
        sources.values.firstOrNull { it.layer == layer && !it.box.empty && it.box.contains(at.x, at.y) }
}

/** The Undo bar's words for a drop on [to] of an item at [from] in [file] (null: from the drawer, not placed yet). */
fun dropMessage(to: Target?, from: Spot?, file: LayoutFile): String = when (to) {
    Target.Remove -> if (from is Spot.Dock) "Removed from the dock." else "Removed from home."
    is Target.Onto -> when (val under = file.itemAt(to.spot)) {
        is Folder -> "Added to ${under.name}."
        else -> "Folder made."
    }
    else -> if (from == null) "Added." else "Moved."
}
