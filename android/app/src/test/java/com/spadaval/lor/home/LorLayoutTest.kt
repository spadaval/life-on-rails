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

    @Test
    fun theLorPanelParsesFullByDefaultAndTakesHalf() {
        assertEquals(Panel.Lor(Cell(1, 0), Wide.FULL), LayoutFile.parse(file()).current.panelAt(Cell(1, 0)))
        val half = LayoutFile.parse(file(lor = """{"at": [1, 0], "type": "lor", "wide": "half"}""")).current.panelAt(Cell(1, 0))
        assertEquals(Panel.Lor(Cell(1, 0), Wide.HALF), half)
        assertTrue("lor" in Panel.TYPES)
        // The default isn't written; half is.
        assertEquals(setOf("at", "type"), Panel.Lor(Cell(1, 0)).toJson().keys().asSequence().toSet())
        assertEquals("half", Panel.Lor(Cell(1, 0), Wide.HALF).toJson().getString("wide"))
    }

    @Test
    fun eachLorBlockParsesAndIsWrittenAsItsTypeAlone() {
        for ((type, block) in lorBlocks) {
            val f = LayoutFile.parse(file("""{"type": "$type"}"""))
            assertEquals(type, block, (f.current.panelAt(Cell(0, 0)) as Panel.Stack).blocks[1])
            assertEquals(setOf("type"), block.toJson().keys().asSequence().toSet())
            assertEquals(type, block.toJson().getString("type"))
            assertEquals(f, LayoutFile.parse(f.toJson()))
            assertTrue(type in StackBlock.TYPES)
        }
    }

    @Test
    fun anExtraFieldIsAnErrorNamingItsPlace() {
        assertEquals(
            "layouts[0].panels[1]: unknown field \"beside\" (allowed: at, type, wide)",
            error(file(lor = """{"at": [1, 0], "type": "lor", "beside": "tasks"}""")),
        )
        assertEquals("layouts[0].panels[1].wide: \"Full\" is not \"full\" or \"half\"", error(file(lor = """{"at": [1, 0], "type": "lor", "wide": "Full"}""")))
        for (type in lorBlocks.keys) {
            assertEquals(type, "layouts[0].panels[0].blocks[1]: unknown field \"rows\" (allowed: type)", error(file("""{"type": "$type", "rows": 3}""")))
        }
        // Each at most once in a stack, as every block but widget.
        assertEquals("layouts[0].panels[0].blocks[2]: a stack holds one \"running\" block", error(file("""{"type": "running"}, {"type": "running"}""")))
    }

    /** lor-j87u's inner-screen rule: `lor` with no `wide` is full; `"wide": "half"` pairs with a half beside it. */
    @Test
    fun theLorPanelOnTheInnerScreen() {
        val full = LayoutFile.parse(file()).current.panels
        assertEquals(listOf(InnerScreen(Cell(0, 0)), InnerScreen(Cell(1, 0))), InnerScreens.of(full))
        val half = LayoutFile.parse(file(lor = """{"at": [1, 0], "type": "lor", "wide": "half"}""")).current.panels
        assertEquals(listOf(InnerScreen(Cell(0, 0), Cell(1, 0))), InnerScreens.of(half))
    }

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

    /** The minimap's edits: one `lor` panel a layout, and the four blocks added as types that take no field. */
    @Test
    fun theMinimapAddsOneLorPanelAndTheBlocks() {
        val f = LayoutFile.parse(file())
        assertEquals("there's already a LoR panel", try {
            f.addPanel(Cell(2, 0), "lor"); fail(); ""
        } catch (e: LayoutError) {
            e.message!!
        })
        val none = f.removePanel(Cell(1, 0), Panel.Lor(Cell(1, 0)))
        assertEquals(Panel.Lor(Cell(1, 0)), none.addPanel(Cell(1, 0), "lor").current.panelAt(Cell(1, 0)))
        val bare = LayoutFile.parse(file("""{"type": "dock"}"""))
        for ((type, block) in lorBlocks) {
            assertEquals(block, (bare.addBlock(Cell(0, 0), 2, type).current.panelAt(Cell(0, 0)) as Panel.Stack).blocks[2])
        }
        assertEquals("a stack holds one \"today\" block", try {
            LayoutFile.parse(file()).addBlock(Cell(0, 0), 0, "today"); fail(); ""
        } catch (e: LayoutError) {
            e.message!!
        })
    }
}
