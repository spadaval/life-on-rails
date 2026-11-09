package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The `widget` block, its edits by id and the sweep of ids no layout names (lor-vtmn, decision lor-9wlx). */
class WidgetTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun ref(s: String) = AppRef.parse(s)!!

    private fun refused(edit: () -> Any): String = try {
        edit()
        fail("not refused")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    private fun error(text: String) = refused { LayoutFile.parse(text) }

    private val clockProvider = "com.google.android.deskclock/com.android.alarmclock.DigitalAppWidgetProvider"

    /** A layout "a": a t3 panel at [0, 0], and a stack at [1, 0] of a clock and [block]. */
    private fun second(block: String) =
        """{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": [1, 0]}, "panels": [
            {"at": [0, 0], "type": "t3"}, {"at": [1, 0], "type": "stack", "blocks": [{"type": "clock"}, $block]}]}]}"""

    private fun widget(fields: String) = second("""{"type": "widget", $fields}""")

    private fun blocks(f: LayoutFile, column: Int, row: Int = 0) = (f.current.panelAt(Cell(column, row)) as Panel.Stack).blocks

    @Test
    fun aWidgetBlockParses() {
        val f = LayoutFile.parse(widget(""""id": 42, "provider": "$clockProvider", "height": 160"""))
        assertEquals(StackBlock.Widget(42, clockProvider, 160), blocks(f, 1)[1])
        assertEquals(listOf(StackBlock.Widget(42, clockProvider, 160)), f.current.widgets)
        // package/.Short expands as AppRef does; the limits are taken.
        val short = LayoutFile.parse(widget(""""id": 1, "provider": "org.example/.Clock", "height": 40"""))
        assertEquals(StackBlock.Widget(1, "org.example/org.example.Clock", 40), blocks(short, 1)[1])
        assertEquals(2000, (blocks(LayoutFile.parse(widget(""""id": 2147483647, "provider": "a.b/C${'$'}D", "height": 2000""")), 1)[1] as StackBlock.Widget).height)
    }

    @Test
    fun eachWrongFormOfAWidgetIsAnErrorNamingItsPlace() {
        val at = "layouts[0].panels[1].blocks[1]"
        val p = """"provider": "a.b/.W""""
        // Missing fields.
        assertEquals("$at.id: must be a whole number from 1", error(widget("""$p, "height": 100""")))
        assertEquals("$at.provider: must be a string", error(widget(""""id": 1, "height": 100""")))
        assertEquals("$at.height: must be a whole number from 40 to 2000", error(widget(""""id": 1, $p""")))
        // An unknown field.
        assertEquals("$at: unknown field \"width\" (allowed: type, id, provider, height)", error(widget(""""id": 1, $p, "height": 100, "width": 200""")))
        // id
        for (bad in listOf("0", "-1", "1.5", "\"42\"", "2147483648", "null", "true")) {
            assertEquals(bad, "$at.id: must be a whole number from 1", error(widget(""""id": $bad, $p, "height": 100""")))
        }
        // provider
        assertEquals("$at.provider: \"com.tesla\" is not package/class", error(widget(""""id": 1, "provider": "com.tesla", "height": 100""")))
        for (bad in listOf("Tesla", "com.tesla/", "com.tesla/a b", "/com.tesla.W")) {
            assertEquals(bad, "$at.provider: \"$bad\" is not package/class", error(widget(""""id": 1, "provider": "$bad", "height": 100""")))
        }
        assertEquals("$at.provider: must be a string", error(widget(""""id": 1, "provider": 42, "height": 100""")))
        // height
        for (bad in listOf("39", "2001", "0", "160.5", "\"160\"", "null")) {
            assertEquals(bad, "$at.height: must be a whole number from 40 to 2000", error(widget(""""id": 1, $p, "height": $bad""")))
        }
    }

    @Test
    fun anIdUsedTwiceInOneLayoutNamesBothPlaces() {
        fun w(id: Int) = """{"type": "widget", "id": $id, "provider": "a.b/.W", "height": 100}"""
        val twoStacks = """{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": [0, 0]}, "panels": [
            {"at": [0, 0], "type": "t3"},
            {"at": [1, 0], "type": "stack", "blocks": [{"type": "clock"}, ${w(42)}]},
            {"at": [2, 0], "type": "stack", "blocks": [${w(42)}, {"type": "today"}]}]}]}"""
        assertEquals("layouts[0].panels[2].blocks[0].id: 42 is taken by panels[1].blocks[1]", error(twoStacks))
        assertEquals("layouts[0].panels[1].blocks[2].id: 7 is taken by panels[1].blocks[1]", error(second("${w(7)}, ${w(7)}")))
        // The same id in two layouts is two layouts' business, not an error.
        val layout = { name: String -> """{"name": "$name", "start": {"narrow": [0, 0]}, "panels": [{"at": [0, 0], "type": "stack", "blocks": [${w(5)}]}]}""" }
        val f = LayoutFile.parse("""{"version": 2, "active": "a", "layouts": [${layout("a")}, ${layout("b")}]}""")
        assertEquals(5, f.layouts[1].widgets.single().id)
    }

    @Test
    fun aStackHoldsSeveralWidgetsButOneOfEachOtherType() {
        fun w(id: Int) = """{"type": "widget", "id": $id, "provider": "a.b/.W", "height": 100}"""
        val f = LayoutFile.parse(second("${w(1)}, ${w(2)}, {\"type\": \"apps\"}, ${w(3)}"))
        assertEquals(listOf(1, 2, 3), f.current.widgets.map { it.id })
        val at = "layouts[0].panels[1].blocks"
        assertEquals("$at[2]: a stack holds one \"clock\" block", error(second("""${w(1)}, {"type": "clock"}""")))
        assertEquals("$at[3]: a stack holds one \"apps\" block", error(second("""{"type": "apps"}, ${w(1)}, {"type": "apps"}""")))
        assertEquals("$at[3]: a stack holds one \"today\" block", error(second("""{"type": "today"}, ${w(1)}, {"type": "today"}""")))
    }

    /** The default grid with two widgets in home's stack and one in the page to its right, as the phone writes it. */
    private val phone: LayoutFile by lazy {
        val grid = Layout.grid("default", listOf(ref("com.whatsapp")), listOf(listOf(ref("org.example"))))
        LayoutFile("default", listOf(grid))
            .addWidget(Layout.HOME, 42, clockProvider, 160)
            .addWidget(Layout.HOME, 43, "com.teslamotors.tesla/.widget.TeslaWidget", 220)
            .addWidget(Cell(2, 0), 44, "com.google.android.calendar/com.android.calendar.widget.CalendarAppWidgetProvider", 400)
    }

    @Test
    fun aFileWithWidgetsWritesBackByteForByteAndKeepsThemThroughAnEditAndItsUndo() {
        val text = phone.toJson() + "\n"
        assertTrue(text.contains("\"provider\": \"$clockProvider\""))
        assertEquals(text, LayoutFile.parse(text).toJson() + "\n")
        val widgets = phone.current.widgets
        assertEquals(listOf(42, 43, 44), widgets.map { it.id })
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        store.file.writeText(text)
        val added = store.edit { it.addToHome(AppId("org.added", "org.added.Main"), ref("org.added")) }.getOrThrow()
        val edited = LayoutFile.parse(store.file.readText())
        assertTrue(ref("org.added") in edited.current.allItems)
        assertEquals(widgets, edited.current.widgets)
        assertEquals(blocks(phone, 1).filterIsInstance<StackBlock.Widget>(), blocks(edited, 1).filterIsInstance<StackBlock.Widget>())
        assertEquals(blocks(phone, 2).filterIsInstance<StackBlock.Widget>(), blocks(edited, 2).filterIsInstance<StackBlock.Widget>())
        store.undo(added.undo!!)
        assertEquals(text, store.file.readText())
        assertEquals(phone, LayoutFile.parse(store.file.readText()))
    }

    @Test
    fun addWidgetGoesBeforeTheAppsBlockElseLast() {
        val grid = LayoutFile("a", listOf(Layout.grid("a", emptyList())))
        val f = grid.withCurrent(
            grid.current.copy(
                panels = grid.current.panels +
                    Panel.Stack(Cell(3, 0), listOf(StackBlock.Clock)),
            ),
        )
        val twice = f.addWidget(Layout.HOME, 1, "a.b/.W", 100).addWidget(Layout.HOME, 2, "a.b/.V", 120)
        assertEquals(
            listOf(StackBlock.Clock, StackBlock.Widget(1, "a.b/a.b.W", 100), StackBlock.Widget(2, "a.b/a.b.V", 120), StackBlock.Apps(emptyList())),
            blocks(twice, 1),
        )
        assertEquals(listOf(StackBlock.Clock, StackBlock.Widget(3, "a.b/a.b.W", 100)), blocks(f.addWidget(Cell(3, 0), 3, "a.b/.W", 100), 3))
        assertEquals(twice, LayoutFile.parse(twice.toJson()))
        // Refusals.
        assertEquals("widget 2 is already on home", refused { twice.addWidget(Cell(3, 0), 2, "a.b/.W", 100) })
        assertEquals(HOME_CHANGED, refused { f.addWidget(Cell(0, 0), 3, "a.b/.W", 100) })
        assertEquals(HOME_CHANGED, refused { f.addWidget(Cell(5, 0), 3, "a.b/.W", 100) })
        assertEquals("\"com.tesla\" is not package/class", refused { f.addWidget(Layout.HOME, 3, "com.tesla", 100) })
        assertEquals("a widget's height is 40 to 2000 dp, got 39", refused { f.addWidget(Layout.HOME, 3, "a.b/.W", 39) })
        assertEquals("a widget's id is a whole number from 1, got 0", refused { f.addWidget(Layout.HOME, 0, "a.b/.W", 100) })
    }

    @Test
    fun removeWidgetTakesOneByIdAndRefusesWhatIsntThere() {
        val gone = phone.removeWidget(43)
        assertEquals(listOf(42, 44), gone.current.widgets.map { it.id })
        assertEquals(listOf(StackBlock.Clock, StackBlock.Widget(42, clockProvider, 160)), blocks(gone, 1).take(2))
        assertEquals(gone, LayoutFile.parse(gone.toJson()))
        assertEquals("widget 9 isn't on home", refused { phone.removeWidget(9) })
        assertEquals("widget 43 isn't on home", refused { gone.removeWidget(43) })
        // A stack's last block stays, as the minimap's Remove refuses it.
        val alone = phone.withCurrent(phone.current.copy(panels = phone.current.panels + Panel.Stack(Cell(3, 0), listOf(StackBlock.Widget(9, "a.b/a.b.W", 100)))))
        assertEquals(LAST_BLOCK, refused { alone.removeWidget(9) })
        // Only the active layout's widgets are on home.
        val other = phone.copy(layouts = phone.layouts + Layout("b", listOf(Panel.Stack(Cell(0, 0), listOf(StackBlock.Widget(9, "a.b/a.b.W", 100)))), Start(Cell(0, 0))))
        assertEquals("widget 9 isn't on home", refused { other.removeWidget(9) })
    }

    @Test
    fun setWidgetHeightResizesOneByIdWithinTheFilesLimits() {
        val taller = phone.setWidgetHeight(44, 600)
        assertEquals(StackBlock.Widget(44, "com.google.android.calendar/com.android.calendar.widget.CalendarAppWidgetProvider", 600), blocks(taller, 2)[0])
        assertEquals(phone.current.widgets.filter { it.id != 44 }, taller.current.widgets.filter { it.id != 44 })
        assertEquals(taller, LayoutFile.parse(taller.toJson()))
        assertEquals(40, (phone.setWidgetHeight(42, 40).current.widgets[0]).height)
        assertSame("the height it has changes nothing", phone, phone.setWidgetHeight(42, 160))
        assertEquals("widget 9 isn't on home", refused { phone.setWidgetHeight(9, 100) })
        assertEquals("a widget's height is 40 to 2000 dp, got 2001", refused { phone.setWidgetHeight(42, 2001) })
        assertEquals("a widget's height is 40 to 2000 dp, got 39", refused { phone.setWidgetHeight(42, 39) })
    }

    @Test
    fun theSweepDeletesOnlyIdsNoLayoutOfAnyParsedFileNames() {
        val b = Layout("b", listOf(Panel.Stack(Cell(0, 0), listOf(StackBlock.Widget(50, "a.b/a.b.W", 100)))), Start(Cell(0, 0)))
        val file = phone.copy(layouts = phone.layouts + b)
        // 42, 43, 44 in the active layout; 50 in layout b, which isn't active.
        assertEquals(listOf(7, 8), widgetIdsToDelete(listOf(7, 42, 43, 44, 50, 8), listOf(file)))
        // An id only layout.good.json names is kept.
        val good = file.copy(layouts = listOf(Layout("default", listOf(Panel.Stack(Cell(0, 0), listOf(StackBlock.Widget(7, "a.b/a.b.W", 100)))), Start(Cell(0, 0)))))
        assertEquals(listOf(8), widgetIdsToDelete(listOf(7, 42, 8), listOf(file, good)))
        // With no parsed file, nothing is deleted.
        assertEquals(emptyList<Int>(), widgetIdsToDelete(listOf(7, 42, 8), emptyList()))
        assertEquals(emptyList<Int>(), widgetIdsToDelete(emptyList(), listOf(file)))
    }

    @Test
    fun aNewWidgetIsItsProvidersMinimumHeightHeldInTheFilesLimits() {
        assertEquals(110, WidgetSize.newHeight(110.4f))
        // A provider with no minimum, or one taller than the file holds, still makes a block the file takes.
        assertEquals(40, WidgetSize.newHeight(0f))
        assertEquals(2000, WidgetSize.newHeight(2400f))
    }

    @Test
    fun aResizeRunsFromTheMinimumResizeHeightToTheSmallerOfTheMaximumAndThePanel() {
        // minResizeHeight below minHeight is the floor; no maxResizeHeight caps at the panel.
        assertEquals(60..700, WidgetSize.resizeRange(110f, 60f, 0f, 700.6f))
        // minResizeHeight unset, or above minHeight: minHeight is the floor.
        assertEquals(110..700, WidgetSize.resizeRange(110f, 0f, 0f, 700f))
        assertEquals(110..700, WidgetSize.resizeRange(110f, 150f, 0f, 700f))
        // maxResizeHeight below the panel caps it; above the panel the panel does.
        assertEquals(110..300, WidgetSize.resizeRange(110f, 0f, 300f, 700f))
        assertEquals(110..700, WidgetSize.resizeRange(110f, 0f, 900f, 700f))
        // A cap below the floor leaves the floor alone, and the file's limits hold at both ends.
        assertEquals(400..400, WidgetSize.resizeRange(400f, 0f, 0f, 300f))
        assertEquals(40..2000, WidgetSize.resizeRange(10f, 0f, 0f, 2600f))
    }

    @Test
    fun aWidgetIsItsColumnsWidthUnlessItsMaximumIsNarrower() {
        assertEquals(380f, WidgetSize.width(380f, 0f))
        assertEquals(300f, WidgetSize.width(380f, 300f))
        assertEquals(380f, WidgetSize.width(380f, 500f))
    }
}
