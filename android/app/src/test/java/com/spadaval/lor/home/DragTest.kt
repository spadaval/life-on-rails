package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which target is under the finger (launcher polish 4.3). */
class DragTest {
    // Two tiles on page 0, 100 wide, icons 52 across from 4 down; the page's space under them; a dock; Remove on top.
    private val a = Zone.Item(Spot.Page(0, 0), Bounds(0f, 100f, 100f, 200f), 104f, 52f)
    private val b = Zone.Item(Spot.Page(0, 1), Bounds(100f, 100f, 200f, 200f), 104f, 52f)
    private val page = Zone.Area(Place.OnPage(0), Bounds(0f, 100f, 400f, 600f))
    private val dock = Zone.Area(Place.Dock, Bounds(0f, 700f, 400f, 780f))
    private val d0 = Zone.Item(Spot.Dock(0), Bounds(0f, 710f, 100f, 770f), 710f, 48f)
    private val remove = Zone.Remove(Bounds(0f, 0f, 400f, 60f))
    private val inFolder = Zone.Item(Spot.InFolder(0, 1, 0), Bounds(50f, 300f, 150f, 400f), 304f, 52f)
    private val card = Zone.Area(Place.InFolder(Spot.Page(0, 1)), Bounds(0f, 280f, 400f, 500f))
    private val zones = listOf(a, b, page, dock, d0, remove, inFolder, card)

    private fun at(x: Float, y: Float, folder: Boolean = false, open: Spot.Page? = null) = targetAt(x, y, zones, folder, open)

    @Test
    fun ontoNearAnIconsCentreElseBeforeOrAfter() {
        assertEquals(Target.Onto(Spot.Page(0, 0)), at(50f, 130f))
        assertEquals("a held folder only reorders", Target.Before(Spot.Page(0, 0)), at(45f, 130f, folder = true))
        assertEquals(Target.Before(Spot.Page(0, 0)), at(5f, 190f))
        assertEquals("the right half is after it", Target.Before(Spot.Page(0, 1)), at(95f, 190f))
        assertEquals("after the last item: one past its end", Target.Before(Spot.Page(0, 2)), at(195f, 190f))
    }

    @Test
    fun listsTakeADropAfterTheirItems() {
        assertEquals(Target.AtEnd(Place.OnPage(0)), at(300f, 400f))
        assertEquals(Target.AtEnd(Place.Dock), at(300f, 740f))
        assertEquals("no onto in the dock", Target.Before(Spot.Dock(0)), at(40f, 740f))
        assertEquals(Target.Remove, at(200f, 30f))
        assertNull("between the grid and the dock", at(200f, 650f))
    }

    @Test
    fun anOpenFolderTakesOnlyItsOwn() {
        assertEquals(Target.Before(Spot.InFolder(0, 1, 0)), at(60f, 350f, open = Spot.Page(0, 1)))
        assertEquals(Target.AtEnd(Place.InFolder(Spot.Page(0, 1))), at(300f, 350f, open = Spot.Page(0, 1)))
        assertNull("outside the card, nothing until it closes", at(50f, 130f, open = Spot.Page(0, 1)))
        assertEquals("closed, the folder's apps aren't targets", Target.AtEnd(Place.OnPage(0)), at(60f, 350f))
    }
}
