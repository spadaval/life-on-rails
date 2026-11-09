package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Which target is under the finger (launcher polish 4.3). */
class DragTest {
    // Two tiles on page 0, 100 wide, icons 52 across from 4 down; the page's space under them; Remove on top.
    private val a = Zone.Item(Spot.Apps(page(0), 0), Bounds(0f, 100f, 100f, 200f), 104f, 52f)
    private val b = Zone.Item(Spot.Apps(page(0), 1), Bounds(100f, 100f, 200f, 200f), 104f, 52f)
    private val page = Zone.Area(Place.InApps(page(0)), Bounds(0f, 100f, 400f, 600f))
    private val remove = Zone.Remove(Bounds(0f, 0f, 400f, 60f))
    private val inFolder = Zone.Item(Spot.InFolder(page(0), 1, 0), Bounds(50f, 300f, 150f, 400f), 304f, 52f)
    private val card = Zone.Area(Place.InFolder(Spot.Apps(page(0), 1)), Bounds(0f, 280f, 400f, 500f))
    private val zones = listOf(a, b, page, remove, inFolder, card)

    private fun at(x: Float, y: Float, folder: Boolean = false, open: Spot.Apps? = null) = targetAt(x, y, zones, folder, open)

    @Test
    fun ontoNearAnIconsCentreElseBeforeOrAfter() {
        assertEquals(Target.Onto(Spot.Apps(page(0), 0)), at(50f, 130f))
        assertEquals("a held folder only reorders", Target.Before(Spot.Apps(page(0), 0)), at(45f, 130f, folder = true))
        assertEquals(Target.Before(Spot.Apps(page(0), 0)), at(5f, 190f))
        assertEquals("the right half is after it", Target.Before(Spot.Apps(page(0), 1)), at(95f, 190f))
        assertEquals("after the last item: one past its end", Target.Before(Spot.Apps(page(0), 2)), at(195f, 190f))
    }

    @Test
    fun listsTakeADropAfterTheirItems() {
        assertEquals(Target.AtEnd(Place.InApps(page(0))), at(300f, 400f))
        assertEquals(Target.Remove, at(200f, 30f))
        assertNull("under the grid's space", at(200f, 650f))
    }

    @Test
    fun anOpenFolderTakesOnlyItsOwn() {
        assertEquals(Target.Before(Spot.InFolder(page(0), 1, 0)), at(60f, 350f, open = Spot.Apps(page(0), 1)))
        assertEquals(Target.AtEnd(Place.InFolder(Spot.Apps(page(0), 1))), at(300f, 350f, open = Spot.Apps(page(0), 1)))
        assertNull("outside the card, nothing until it closes", at(50f, 130f, open = Spot.Apps(page(0), 1)))
        assertEquals("closed, the folder's apps aren't targets", Target.AtEnd(Place.InApps(page(0))), at(60f, 350f))
    }

    @Test
    fun neverOntoTheHeldItemsOwnSlot() {
        assertEquals(Target.Before(Spot.Apps(page(0), 0)), targetAt(45f, 130f, zones, false, null, heldAt = Spot.Apps(page(0), 0)))
        assertEquals(Target.Onto(Spot.Apps(page(0), 1)), targetAt(150f, 130f, zones, false, null, heldAt = Spot.Apps(page(0), 0)))
    }

    // 4's review item (a), lor-ozj2: sources and zones hold what they stand for, and the item is looked up when used.

    private fun ref(s: String) = AppRef.parse(s)!!
    private fun file(items: List<LayoutItem>) = LayoutFile("a", listOf(Layout.grid("a", items, listOf(emptyList()))))

    @Test
    fun aFolderDraggedAwayAndBackIsPickedUpAsItIsNow() {
        // An unpinned folder moved to the front and back again: its tile is where it was, but the
        // file now holds it pinned. Picked up by its slot, it is the pinned folder, and it moves.
        val f = file(listOf(ref("a.a"), Folder("F", listOf(ref("x.x"), ref("y.y")))))
        val slot = f.current.slotOf(Spot.Apps(page(0), 1))!!
        assertEquals(Slot.Tile(page(0), "folder:F#1"), slot)
        val away = f.drop(f.current.find(slot)!!, Target.Before(Spot.Apps(page(0), 0))).file
        val back = away.drop(away.current.find(slot)!!, Target.AtEnd(Place.InApps(page(0)))).file
        val stale = f.current.find(slot)!!
        val now = back.current.find(slot)!!
        assertEquals(Spot.Apps(page(0), 1), now.spot)
        assertTrue((now.item as Folder).pinned)
        try {
            back.drop(stale, Target.AtEnd(Place.NewPanel)); fail("the snapshot isn't what the file holds")
        } catch (e: LayoutError) {
            assertEquals(HOME_CHANGED, e.message)
        }
        assertEquals(listOf(ref("a.a")), back.drop(now, Target.AtEnd(Place.NewPanel)).file.current.home)
    }

    @Test
    fun slotsFollowTheirItemAndGoWithIt() {
        val f = file(listOf(ref("a.a"), Folder("F", listOf(ref("x.x"), ref("y.y"))), ref("a.a")))
        val l = f.current
        // Repeats are told apart in order, as the tiles' keys are.
        assertEquals(Slot.Tile(page(0), "app:a.a#2"), l.slotOf(Spot.Apps(page(0), 2)))
        val inFolder = l.slotOf(Spot.InFolder(page(0), 1, 1))!!
        assertEquals(From.At(Spot.InFolder(page(0), 1, 1), ref("y.y")), l.find(inFolder))
        // The first item removed: the folder's slot finds it one place on.
        val removed = f.drop(l.find(l.slotOf(Spot.Apps(page(0), 0))!!)!!, Target.Remove).file.current
        assertEquals(Spot.InFolder(page(0), 0, 1), removed.find(inFolder)!!.spot)
        // The folder removed: its slots find nothing.
        val folder = l.slotOf(Spot.Apps(page(0), 1))!!
        val noFolder = f.drop(l.find(folder)!!, Target.Remove).file.current
        assertNull(noFolder.find(folder))
        assertNull(noFolder.find(inFolder))
        assertNull(l.find(Slot.DrawerApp("a.a/.Main#0")))
    }

    @Test
    fun marksResolveToTheZonesOfTheLayoutDrawn() {
        val f = file(listOf(ref("a.a"), Folder("F", listOf(ref("x.x")))))
        val box = Bounds(0f, 0f, 10f, 10f)
        val folder = Slot.Tile(page(0), "folder:F#1")
        val marks = listOf(
            DropMark.Item(folder, box, 1f, 5f),
            DropMark.Item(Slot.Tile(page(0), "app:gone#1"), box, 1f, 5f),
            DropMark.Area(Space.Apps(page(0)), box),
            DropMark.Area(Space.Apps(BlockAt(Cell(9, 0), 0)), box),
            DropMark.Area(Space.Card(folder), box),
            DropMark.Remove(box),
        )
        assertEquals(
            listOf(
                Zone.Item(Spot.Apps(page(0), 1), box, 1f, 5f),
                Zone.Area(Place.InApps(page(0)), box),
                Zone.Area(Place.NewPanel, box),
                Zone.Area(Place.InFolder(Spot.Apps(page(0), 1)), box),
                Zone.Remove(box),
            ),
            f.current.zones(marks),
        )
    }

    @Test
    fun aHeldAppLeavesAFullScreenFolderAfter400msOutsideItsApps() {
        // lor-gnk0: the folder's name row and apps span y 280 to 500 across the screen.
        val area = card.box
        val exit = FolderExit()
        assertEquals(false, exit.closes(area, 200f, 400f, 0))
        // Above the name row: the clock starts, and runs while the finger stays out.
        assertEquals(false, exit.closes(area, 200f, 200f, 100))
        assertEquals(false, exit.closes(area, 200f, 150f, 499))
        assertEquals(true, exit.closes(area, 200f, 150f, 500))
        // Back inside resets it; below the last row counts as outside too.
        val again = FolderExit()
        assertEquals(false, again.closes(area, 200f, 600f, 0))
        assertEquals(false, again.closes(area, 200f, 450f, 300))
        assertEquals(false, again.closes(area, 200f, 600f, 350))
        assertEquals(false, again.closes(area, 200f, 600f, 700))
        assertEquals(true, again.closes(area, 200f, 600f, 750))
        assertEquals(400L, FolderExit.CLOSE_MS)
    }
}
