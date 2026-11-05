package com.spadaval.lor.t3

import org.junit.Assert.assertEquals
import org.junit.Test

class T3LinksTest {
    @Test
    fun aThreadLinkEncodesEachSegmentAsEncodeURIComponentDoes() {
        val app = T3Links.APPS.first()
        assertEquals(
            "t3code://threads/69ac2497-08d4-4207-89f6-767759a8ef3c/thread%3Aproject%3Ae83c%3Aab12",
            T3Links.thread(app, "69ac2497-08d4-4207-89f6-767759a8ef3c", "thread:project:e83c:ab12"),
        )
        // encodeURIComponent keeps these, and escapes UTF-8 bytes.
        assertEquals("a-_.!~*'()b", T3Links.component("a-_.!~*'()b"))
        assertEquals("%2F%20%3F%23%C3%A9", T3Links.component("/ ?#é"))
    }

    @Test
    fun productionComesFirstThenPreviewThenDev() {
        assertEquals(listOf("t3code", "t3code-preview", "t3code-dev"), T3Links.APPS.map { it.scheme })
        assertEquals("com.t3tools.t3code.dev", T3Links.APPS.last().packageName)
    }
}
