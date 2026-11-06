package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** Icon packs, themed icons and the icon look fields (launcher polish 6.5). */
class IconsTest {
    @Test
    fun appfilterIndexUnwrapsAndKeepsTheFirst() {
        val index = appfilterIndex(sequenceOf(
            "ComponentInfo{com.a/com.a.Main}" to "a_icon",
            "com.b/.Start" to "b_icon",
            "not a component" to "x",
            "com.c/" to "c",
            "ComponentInfo{com.a/com.a.Main}" to "a_second",
            "com.d/com.d.Main" to "",
        ))
        assertEquals(mapOf("com.a/com.a.Main" to "a_icon", "com.b/com.b.Start" to "b_icon"), index)
    }

    @Test
    fun theIconComesFromThePackThenTheMonochromeLayerThenTheApp() {
        assertEquals(IconSource.PACK, iconSource(inPack = true, themed = true, hasMonochrome = true))
        assertEquals(IconSource.THEMED, iconSource(inPack = false, themed = true, hasMonochrome = true))
        assertEquals(IconSource.OWN, iconSource(inPack = false, themed = true, hasMonochrome = false))
        assertEquals(IconSource.OWN, iconSource(inPack = false, themed = false, hasMonochrome = true))
    }

    @Test
    fun theStyleCarriesTheIndexState() {
        val look = HomeLook(iconPack = "com.pack")
        assertEquals(0, IconStyle.of(look, null).packVersion)
        // An index for another pack (the look just changed) doesn't count.
        assertEquals(IconStyle(null, 0, false, HomeLook.IconShape.SYSTEM), IconStyle.of(HomeLook(), null))
    }

    private fun file(look: String) = """{"version": 1, "active": "a", "look": $look, "layouts": [{"name": "a"}]}"""

    @Test
    fun iconFieldsParseAndRoundTrip() {
        val f = LayoutFile.parse(file("""{"iconPack": "com.donnnno.arcticons", "themedIcons": true, "iconShape": "square"}"""))
        assertEquals("com.donnnno.arcticons", f.look.iconPack)
        assertEquals(true, f.look.themedIcons)
        assertEquals(HomeLook.IconShape.SQUARE, f.look.iconShape)
        assertEquals(f, LayoutFile.parse(f.toJson()))
        for ((look, message) in listOf(
            """{"iconPack": ""}""" to "look.iconPack: \"\" is not a package name",
            """{"iconPack": "arcticons"}""" to "look.iconPack: \"arcticons\" is not a package name",
            """{"iconShape": "squircle"}""" to "look.iconShape: must be one of system, square, circle",
            """{"themedIcons": 1}""" to "look.themedIcons: must be true or false",
        )) {
            try {
                LayoutFile.parse(file(look)); fail("parsed $look")
            } catch (e: LayoutError) {
                assertEquals(message, e.message)
            }
        }
    }
}
