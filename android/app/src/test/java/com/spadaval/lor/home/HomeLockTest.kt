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

    private val onHome = Places(home = true, folder = null)
    private val inFolder = Places(home = false, folder = "Work")
    private val nowhere = Places(home = false, folder = null)

    private val one = LayoutFile("default", listOf(Layout.grid("default", listOf(AppRef("com.whatsapp")))))
    private val moved = one.copy(layouts = listOf(Layout.grid("default", listOf(AppRef("com.whatsapp"), AppRef("org.example")))))

    private fun refused(block: () -> Unit): String = try {
        block()
        "taken"
    } catch (e: LayoutError) {
        e.message!!
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
