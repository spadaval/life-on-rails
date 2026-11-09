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
 * has been outside its apps for 400 ms (`Folder.ON_EXIT_CLOSE_DELAY`; [FolderExit]).
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
     * An item in an `apps` block or in the open folder, at [spot]. Its icon's top is [iconTop]
     * and it is [iconSize] across, centred in [box].
     */
    data class Item(val spot: Spot, override val box: Bounds, val iconTop: Float, val iconSize: Float) : Zone

    /** A list's space: an `apps` block's grid, the open folder's card, the new panel. */
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

/**
 * When a held app leaves an open folder (lor-gnk0, after Launcher3's `Folder.ON_EXIT_CLOSE_DELAY`,
 * Apache-2.0). The open folder fills the screen, so there is no space outside a card: the folder's
 * area is its name row and its grid of apps, [area] as laid out (the part of a scrolling grid on
 * screen). Once the finger has been outside it, above the name row or below the last row (or off
 * its sides), for [CLOSE_MS] together, the folder closes and the drag goes on over home.
 */
class FolderExit {
    private var since: Long? = null

    /** Whether the folder closes now, with the finger at ([x], [y]) at [now] ms. */
    fun closes(area: Bounds, x: Float, y: Float, now: Long): Boolean {
        if (area.contains(x, y)) {
            since = null
            return false
        }
        val from = since ?: now.also { since = it }
        return now - from >= CLOSE_MS
    }

    companion object {
        /** Launcher3's `ON_EXIT_CLOSE_DELAY`. */
        const val CLOSE_MS = 400L
    }
}

/** The place after this one in its list (which may be one past its end: in front of nothing). */
fun Spot.next(): Spot = when (this) {
    is Spot.Apps -> copy(index = index + 1)
    is Spot.InFolder -> copy(app = app + 1)
}

/**
 * What a drag source or drop zone stands for, by what it is rather than by what it held when it
 * was last laid out (4's review item (a)): a tile's `onGloballyPositioned` doesn't run again when
 * only its item changes (a folder pinned by a move away and back keeps its bounds), so a snapshot
 * of its item goes stale. A [key] is [itemKeys]' for the item in its list; [Layout.find] looks the
 * slot up in the layout drawn when it is used.
 */
sealed interface Slot {
    /** The item keyed [key] in the `apps` block [block]. */
    data class Tile(val block: BlockAt, val key: String) : Slot
    /** The app keyed [key] in the folder at [folder]. */
    data class InFolder(val folder: Tile, val key: String) : Slot
    /** The drawer's app whose [HomeApp.key] is [app]; not in the layout. */
    data class DrawerApp(val app: String) : Slot
}

/** The item at [slot] in this layout, with its place now, or null when it has gone (or [slot] is the drawer's). */
fun Layout.find(slot: Slot): From.At? = when (slot) {
    is Slot.Tile -> items(slot.block)?.let { keyed(it, slot.key) }?.let { i -> From.At(Spot.Apps(slot.block, i), items(slot.block)!![i]) }
    is Slot.InFolder -> find(slot.folder)?.let { f ->
        val folder = f.item as? Folder ?: return null
        val spot = f.spot as Spot.Apps
        keyed(folder.apps, slot.key)?.let { a -> From.At(Spot.InFolder(spot.at, spot.index, a), folder.apps[a]) }
    }
    is Slot.DrawerApp -> null
}

/** The slot standing for what is at [spot] in this layout now, or null when nothing is there. */
fun Layout.slotOf(spot: Spot): Slot? = when (spot) {
    is Spot.Apps -> items(spot.at)?.let { itemKeys(it).getOrNull(spot.index) }?.let { Slot.Tile(spot.at, it) }
    is Spot.InFolder -> (slotOf(spot.folder) as? Slot.Tile)?.let { tile ->
        (items(spot.at)?.getOrNull(spot.index) as? Folder)?.let { itemKeys(it.apps).getOrNull(spot.app) }?.let { Slot.InFolder(tile, it) }
    }
}

private fun keyed(items: List<LayoutItem>, key: String): Int? = itemKeys(items).indexOf(key).takeIf { it >= 0 }

/** A list's space a zone stands for: an `apps` block's (or the new panel's), an open folder's card. */
sealed interface Space {
    data class Apps(val block: BlockAt) : Space
    data class Card(val folder: Slot.Tile) : Space
}

/** A drop zone as laid out: its bounds and what it stands for, which [Layout.zones] resolves. */
sealed interface DropMark {
    val box: Bounds
    /** An item at [slot]: its icon's top is [iconTop] and it is [iconSize] across, centred in [box]. */
    data class Item(val slot: Slot, override val box: Bounds, val iconTop: Float, val iconSize: Float) : DropMark
    data class Area(val space: Space, override val box: Bounds) : DropMark
    data class Remove(override val box: Bounds) : DropMark
}

/** [marks] as the zones they are in this layout now; a mark whose item has gone is left out. */
fun Layout.zones(marks: Collection<DropMark>): List<Zone> = marks.mapNotNull { m ->
    when (m) {
        is DropMark.Item -> find(m.slot)?.let { Zone.Item(it.spot, m.box, m.iconTop, m.iconSize) }
        is DropMark.Remove -> Zone.Remove(m.box)
        is DropMark.Area -> when (val space = m.space) {
            // The new panel a drag shows isn't in the file yet.
            is Space.Apps -> Zone.Area(if (items(space.block) != null) Place.InApps(space.block) else Place.NewPanel, m.box)
            is Space.Card -> (find(space.folder)?.spot as? Spot.Apps)?.let { Zone.Area(Place.InFolder(it), m.box) }
        }
    }
}

/** What a press can pick up: where it is on screen, and what it stands for. [layer]: 0 home, 1 the open folder, 2 the drawer. */
class Source(val box: Bounds, val slot: Slot, val layer: Int)

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

    /** The folder on home a drawer app sits in, which a drop on home takes it out of (lor-83uq). */
    val outOf: String? = (start as? From.Drawer)?.let { d -> original.folderSpotOf(d.app, home.installed)?.let { (original.itemAt(it.folder) as Folder).name } }

    /** Where it was picked up in the drawer, or null when it came from home. */
    val drawer: From.Drawer? = start as? From.Drawer

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
            // A full page, a folder into a folder: the item stays where it is.
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

    /**
     * Laid-out sources and drop zones, by a key per composable; each removes its own when it
     * leaves. Each holds only where it is and what it stands for; the item is looked up when used.
     */
    val sources = HashMap<Any, Source>()
    val zones = HashMap<Any, DropMark>()

    /** The source under [at] in [layer], the top one showing. */
    fun sourceAt(at: Offset, layer: Int): Source? =
        sources.values.firstOrNull { it.layer == layer && !it.box.empty && it.box.contains(at.x, at.y) }
}

/** The Undo bar's words for a drop on [to] of an item at [from] in [file] (null: from the drawer, not placed yet). */
fun dropMessage(to: Target?, from: Spot?, file: LayoutFile): String = when (to) {
    Target.Remove -> "Removed from home."
    is Target.Onto -> when (val under = file.itemAt(to.spot)) {
        is Folder -> "Added to ${under.name}."
        else -> "Folder made."
    }
    else -> if (from == null) "Added." else "Moved."
}

/**
 * What a drop or a folder's + writes over [now], the file as the edit finds it: [result], made
 * from [original], the file home drew. Refused with [HOME_CHANGED] when [now] differs from
 * [original] in more than its lock. [now]'s lock is kept, so a lock written from outside since home
 * read the file reaches [LayoutStore.edit], which refuses with [LOCKED] (lor-zzyy).
 */
fun landOver(now: LayoutFile, original: LayoutFile, result: LayoutFile): LayoutFile {
    if (now.copy(locked = original.locked) != original) throw LayoutError(HOME_CHANGED)
    return result.copy(locked = now.locked)
}
