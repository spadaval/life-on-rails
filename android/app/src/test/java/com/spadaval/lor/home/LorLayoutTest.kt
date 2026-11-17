package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The `lor` panel and LoR's four blocks in `layout.json` (lor-lz97, decision lor-p86v). */
class LorLayoutTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun error(text: String): String = try {
        LayoutFile.parse(text)
        fail("not refused")
        ""
    } catch (e: LayoutError) {
        e.message!!
    }

    /** A layout "a": a stack at [0, 0] of a clock and [block], and a lor panel at [1, 0] written as [lor]. */
    private fun file(block: String = """{"type": "today"}""", lor: String = """{"at": [1, 0], "type": "lor"}""") =
        """{"version": 2, "active": "a", "layouts": [{"name": "a", "start": {"narrow": [0, 0]}, "panels": [
            {"at": [0, 0], "type": "stack", "blocks": [{"type": "clock"}, $block]}, $lor]}]}"""

    private val lorBlocks = mapOf(
        "today" to StackBlock.Today,
        "needs-you" to StackBlock.NeedsYou,
        "running" to StackBlock.Running,
        "t3-queue" to StackBlock.T3Queue,
    )

    /** Through an edit (another block added, the panel set to half) and its Undo, as the phone writes them. */
    @Test
    fun theyRoundTripThroughAnEditAndUndo() {
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        val text = file("""{"type": "today"}, {"type": "needs-you"}, {"type": "running"}""")
        store.file.writeText(text)
        val read = store.read(null).layout
        val edited = store.edit { f ->
            f.addBlock(Cell(0, 0), 4, "t3-queue").setWide(Cell(1, 0), Panel.Lor(Cell(1, 0)), Wide.HALF)
        }.getOrThrow()
        val written = LayoutFile.parse(store.file.readText())
        assertEquals(
            listOf(StackBlock.Clock, StackBlock.Today, StackBlock.NeedsYou, StackBlock.Running, StackBlock.T3Queue),
            (written.current.panelAt(Cell(0, 0)) as Panel.Stack).blocks,
        )
        assertEquals(Panel.Lor(Cell(1, 0), Wide.HALF), written.current.panelAt(Cell(1, 0)))
        store.undo(edited.undo!!)
        assertEquals(text, store.file.readText())
        assertEquals(read, store.read(null).layout)
    }
}
