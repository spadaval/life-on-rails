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
}
