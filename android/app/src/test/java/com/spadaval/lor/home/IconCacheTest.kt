package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The process icon cache (lor-ozj2): keyed by size, sized in bytes, and the icon-cache race (T3 Launcher 3.2). */
class IconCacheTest {
    private val style = IconStyle(null, 0, false, HomeLook.IconShape.SYSTEM)
    private fun key(pkg: String, px: Int, user: String = "0") = IconKey("$pkg/.Main#$user", pkg, user, style, px)
    private fun cache(max: Int = 1000) = IconCache<String>(max) { it.length }

    @Test
    fun anIconWhoseDrawBeganBeforeItsPackageChangedIsNotKept() {
        val c = cache()
        val k = key("com.a", 147)
        val ticket = c.ticket(k)
        // The package changes while the old icon draws.
        c.evict(listOf("com.a"), "0")
        assertFalse(c.put(k, "old icon", ticket))
        assertNull("the next draw is fresh", c.get(k))
        assertTrue(c.put(k, "new icon", c.ticket(k)))
        assertEquals("new icon", c.get(k))
    }

    @Test
    fun anotherPackagesChangeLeavesADrawAlone() {
        val c = cache()
        val k = key("com.a", 147)
        val ticket = c.ticket(k)
        c.evict(listOf("com.b"), "0")
        c.evict(listOf("com.a"), "10")
        assertTrue(c.put(k, "icon", ticket))
    }

    @Test
    fun eachSizeIsKeptAndAChangeDropsEverySize() {
        val c = cache()
        c.put(key("com.a", 147), "folded", 0)
        c.put(key("com.a", 126), "unfolded", 0)
        c.put(key("com.b", 147), "b", 0)
        assertEquals("folded", c.get(key("com.a", 147)))
        assertEquals("unfolded", c.get(key("com.a", 126)))
        c.evict(listOf("com.a"), "0")
        assertNull(c.get(key("com.a", 147)))
        assertNull(c.get(key("com.a", 126)))
        assertEquals("b", c.get(key("com.b", 147)))
    }

    @Test
    fun theLeastRecentlyUsedGoFirstPastTheBytes() {
        val c = cache(max = 10)
        c.put(key("com.a", 1), "aaaa", 0)
        c.put(key("com.b", 1), "bbbb", 0)
        c.get(key("com.a", 1))
        c.put(key("com.c", 1), "cccc", 0)
        assertNull(c.get(key("com.b", 1)))
        assertEquals("aaaa", c.get(key("com.a", 1)))
        assertEquals(2, c.size)
    }
}
