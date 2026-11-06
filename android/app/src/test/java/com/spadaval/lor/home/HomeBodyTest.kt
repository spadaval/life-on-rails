package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Test

/** What home's grid shows while it loads, when the list comes back empty, and when home is empty (launcher polish 3.2). */
class HomeBodyTest {
    private val file = LayoutFile("default", listOf(Layout("default", listOf(AppRef("com.whatsapp")), emptyList())))

    @Test
    fun loadingShowsTheFilesShapeOrNothing() {
        assertEquals(HomeBody.SKELETON, homeBody(AppsLoad.LOADING, file, 0))
        // No file yet (first run): nothing to shape a skeleton with.
        assertEquals(HomeBody.NOTHING, homeBody(AppsLoad.LOADING, null, 0))
    }

    @Test
    fun anEmptyListSaysSoInsteadOfAnEmptyHome() {
        assertEquals(HomeBody.APPS_DIDNT_LOAD, homeBody(AppsLoad.EMPTY, file, 0))
        assertEquals(HomeBody.APPS_DIDNT_LOAD, homeBody(AppsLoad.EMPTY, null, 0))
    }

    @Test
    fun aLoadedListShowsHomeOrItsEmptyState() {
        assertEquals(HomeBody.GRID, homeBody(AppsLoad.LOADED, file, 1))
        assertEquals(HomeBody.EMPTY, homeBody(AppsLoad.LOADED, file, 0))
        // A broken file with no last good layout: the banner alone.
        assertEquals(HomeBody.NOTHING, homeBody(AppsLoad.LOADED, null, 0))
    }
}
