package com.spadaval.lor.json

import com.spadaval.lor.home.LayoutFile
import com.spadaval.lor.home.v2Empty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The common reader against what Android's org.json does with the same text (lor-xdoi's review, 1-cd0k.1). */
class JsonTest {
    @Test
    fun aLeadingByteOrderMarkIsSkippedAsAndroidSkipsIt() {
        val text = v2Empty()
        assertEquals(LayoutFile.parse(text), LayoutFile.parse("﻿" + text))
        assertTrue(JSONTokener("﻿{\"a\": 1}").nextValue() is JSONObject)
        // Only a leading one: inside a string it stays.
        assertEquals("﻿x", JSONObject("{\"a\": \"﻿x\"}").getString("a"))
    }
}
