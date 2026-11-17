package com.spadaval.lor.lab

import com.spadaval.lor.data.Sensor
import com.spadaval.lor.data.syncSummary
import com.spadaval.lor.home.TabKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the lab's `current` reads from a fixture for the app's own screens. */
class AppScreensTest {
    @Test
    fun homeSettingsReadsTheHiddenAppsAndTabsOfTheFile() {
        val read = readSettings(Fixtures.homeSettingsTabs)
        val file = assertNotNull(read.file)
        assertNull(read.broken)
        assertEquals(listOf("Play Store", "Duolingo", "Kindle", "com.example.oldreader"), file.hidden.map { it.label })
        assertEquals(listOf(true, true, true, false), file.hidden.map { it.installed })
        assertEquals(listOf("Quick dial", "Work", "Travel", "All"), file.tabs.map { it.name })
        assertEquals(listOf(TabKind.QUICK, TabKind.LIST, TabKind.LIST, TabKind.ALL), file.tabs.map { it.kind })
        assertEquals(5, file.tabs[1].apps)
        assertTrue(file.canAddTab)
    }

    @Test
    fun aBrokenFileShowsItsBannerAndNoSections() {
        val read = readSettings(Fixtures.failed)
        assertNull(read.file)
        assertEquals("version: must be 2 (or 1, which is converted), got 3", assertNotNull(read.broken).message)
    }

    @Test
    fun aFirstRunHasTheDefaultTabs() {
        val file = assertNotNull(readSettings(Fixtures.empty).file)
        assertEquals(listOf(TabKind.QUICK, TabKind.ALL), file.tabs.map { it.kind })
        assertTrue(file.hidden.isEmpty())
    }

    @Test
    fun theNamedAppStatesOpenTheirScreens() {
        assertEquals(Opened.APP_TAB to "sensors", Fixtures.sensorsFailed.where.open to Fixtures.sensorsFailed.where.target)
        assertEquals(2, Fixtures.sensorsFailed.phone.sensors.count { !it.allowed })
        assertFalse(Fixtures.sensorsFailed.phone.sync.errors.isEmpty())
        assertEquals(Opened.HOME_SETTINGS, Fixtures.homeSettingsTabs.where.open)
    }

    @Test
    fun theSyncSummaryNamesWhatWasCollectedAndWhatWaits() {
        assertEquals("Nothing collected; 0 new on the server.", syncSummary(emptyMap(), 0, 0))
        assertEquals(
            "Collected 41 app sessions, 2 calls; 72 new on the server; 3 waiting to upload; backup sent.",
            syncSummary(mapOf(Sensor.APPS to 41, Sensor.CALLS to 2), 72, 3, "backup sent"),
        )
    }
}
