package com.spadaval.lor.lab

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The lab's quick-send sheet draws T3's projects as the fixture lists them, as the app draws `shell.projects` (review lor-rgx4, finding 1). */
class LabPanelsTest {
    @Test
    fun theSheetsProjectsAreTheFixtures() {
        val maps = Fixtures.empty.copy(t3 = T3(T3Status.LIVE, projects = mapOf("p-maps" to "Maps")), where = Where(open = Opened.QUICK_SEND))
        val sheet = LabPanels.sheet(maps)
        // A project with no thread is there, and no Inbox the server doesn't have.
        assertEquals(mapOf("p-maps" to "Maps"), sheet.projects)
        assertTrue(sheet.loaded)

        val none = LabPanels.sheet(Fixtures.empty.copy(t3 = T3(T3Status.LIVE), where = Where(open = Opened.QUICK_SEND)))
        assertEquals(emptyMap(), none.projects)
    }

    @Test
    fun eachThreadIsUnderTheProjectItsTitleNames() {
        val sheet = LabPanels.sheet(Fixtures.demo.copy(where = Where(open = Opened.QUICK_SEND)))
        assertEquals(Fixtures.demo.t3.projects, sheet.projects)
        assertEquals("p-field", sheet.threads["t-tiles"])
    }
}
