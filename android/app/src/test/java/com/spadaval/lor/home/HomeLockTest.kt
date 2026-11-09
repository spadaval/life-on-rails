package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The lock on home's screens (lor-zzyy): the menu's edit entries and what a drop writes. */
class HomeLockTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val onHome = Places(home = true, dock = false, folder = null)
    private val inFolder = Places(home = false, dock = false, folder = "Work")
    private val inDock = Places(home = false, dock = true, folder = null)
    private val nowhere = Places(home = false, dock = false, folder = null)

    @Test
    fun unlockedTheMenuIsAsBefore() {
        assertEquals(listOf(AppEdit.REMOVE_FROM_HOME, AppEdit.ADD_TO_DOCK, AppEdit.MOVE_TO_SECTION, AppEdit.HIDE), appEdits(onHome, false))
        assertEquals(listOf(AppEdit.REMOVE_FROM_FOLDER, AppEdit.ADD_TO_HOME, AppEdit.ADD_TO_DOCK, AppEdit.MOVE_TO_SECTION, AppEdit.HIDE), appEdits(inFolder, false))
        assertEquals(listOf(AppEdit.ADD_TO_HOME, AppEdit.REMOVE_FROM_DOCK, AppEdit.MOVE_TO_SECTION, AppEdit.HIDE), appEdits(inDock, false))
        assertEquals(listOf(AppEdit.ADD_TO_HOME, AppEdit.ADD_TO_DOCK, AppEdit.MOVE_TO_SECTION, AppEdit.HIDE), appEdits(nowhere, false))
    }

    @Test
    fun lockedOnlyWhatTheStoreTakesShows() {
        listOf(onHome, inFolder, inDock).forEach { assertEquals(listOf(AppEdit.MOVE_TO_SECTION), appEdits(it, true)) }
        assertEquals(listOf(AppEdit.MOVE_TO_SECTION, AppEdit.HIDE), appEdits(nowhere, true))
    }

    private val one = LayoutFile("default", listOf(Layout.grid("default", listOf(AppRef("com.whatsapp")), listOf(AppRef("com.android.chrome")))))
    private val moved = one.copy(layouts = listOf(Layout.grid("default", listOf(AppRef("com.whatsapp"), AppRef("org.example")), listOf(AppRef("com.android.chrome")))))

    private fun refused(block: () -> Unit): String = try {
        block()
        "taken"
    } catch (e: LayoutError) {
        e.message!!
    }

    @Test
    fun aDropOverTheFileHomeDrewWritesIt() {
        assertEquals(moved, landOver(one, one, moved))
    }

    @Test
    fun aDropOverAnotherFileIsHomeChanged() {
        assertEquals(HOME_CHANGED, refused { landOver(moved, one, moved) })
    }

    @Test
    fun aLockWrittenFromOutsideRefusesTheDropAsLocked() {
        val store = LayoutStore(tmp.root.resolve("layout.json"))
        store.write(one)
        // Home drew `one`; then the lock was written from outside (adb, an agent).
        store.file.writeText(one.setLocked(true).toJson() + "\n")
        val before = store.file.readBytes()
        val r = store.edit { now -> landOver(now, one, moved) }
        assertEquals(LOCKED, r.exceptionOrNull()?.message)
        assertTrue(before.contentEquals(store.file.readBytes()))
    }
}
