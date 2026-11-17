package com.spadaval.lor.lab

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LabLinkTest {
    private val versions = listOf("current", "astral")
    private val states = listOf("home-full", "first-run")
    private fun parse(h: String) = parseHash(h, versions, states)

    @Test
    fun theFullFormNamesEveryPart() {
        val p = parse("#astral/first-run/inner/pale")
        assertEquals(LabLink("astral", "first-run", LabScreen.Inner, Wallpaper.Pale), p.link)
        assertNull(p.problem)
    }

    @Test
    fun theScreenAndWallpaperDefaultToCoverOnDark() {
        assertEquals(LabLink("current", "home-full", LabScreen.Cover, Wallpaper.Dark), parse("#current/home-full").link)
        assertEquals(LabLink("current", "home-full", LabScreen.Inner, Wallpaper.Dark), parse("current/home-full/inner").link)
    }

    @Test
    fun anEmptyHashIsTheFirstVersionAndState() {
        assertEquals(Parsed(LabLink("current", "home-full"), null), parse(""))
        assertEquals(Parsed(LabLink("current", "home-full"), null), parse("#"))
    }

    @Test
    fun aHashLeavesOffOnlyTrailingDefaults() {
        assertEquals("current/home-full", LabLink("current", "home-full").hash)
        assertEquals("current/home-full/inner", LabLink("current", "home-full", LabScreen.Inner).hash)
        assertEquals("current/home-full/cover/pale", LabLink("current", "home-full", wallpaper = Wallpaper.Pale).hash)
        assertEquals("current/home-full/inner/mixed", LabLink("current", "home-full", LabScreen.Inner, Wallpaper.Mixed).hash)
    }

    @Test
    fun everyLinkReadsBackAsItself() {
        for (v in versions) for (s in states) for (sc in LabScreen.entries) for (w in Wallpaper.entries) {
            val link = LabLink(v, s, sc, w)
            assertEquals(Parsed(link, null), parse("#" + link.hash))
        }
    }

    @Test
    fun anUnknownNameIsNamedAndFallsBack() {
        val p = parse("#concept-x/home-full/tablet")
        assertEquals(LabLink("current", "home-full", LabScreen.Cover), p.link)
        assertEquals("No version \"concept-x\"; No screen \"tablet\"", p.problem)
    }

    @Test
    fun anEncodedPartIsDecoded() {
        assertEquals("first-run", parse("#current/first%2Drun").link.state)
    }

    @Test
    fun theRegistryStartsWithCurrent() {
        assertEquals("current", Lab.versions.first().name)
        assertEquals("home-full", Lab.states.first().name)
    }
}
