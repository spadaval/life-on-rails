package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    private val apps = listOf("Calendar", "Café Nero", "Camera", "Google Maps", "Lyft", "Prime Video", "Settings", "T-Mobile", "WhatsApp", "YouTube", "YT Music")

    private fun entries(kind: Kind, names: List<String>) = names.map { Found(kind, "$kind:$it", listOf(Match.prepare(it)), it) }

    private fun find(q: String, list: List<String> = apps, weights: Map<String, Double> = emptyMap()) =
        Search.run(q, entries(Kind.APPS, list), { weights[it.value] ?: 0.0 }).blocks.flatMap { b -> b.entries.map { it.value } }

    private fun tier(q: String, name: String) = Match.tier(Query(q), Match.prepare(name))

    // AppSearchTest's cases (launcher polish 3.2 and before), unchanged in result and order.
    @Test
    fun emptyQueryFindsNothing() {
        assertEquals(emptyList<String>(), find(""))
        assertEquals(emptyList<String>(), find("  "))
    }

    @Test
    fun prefixAndWordStartsMatch() {
        assertEquals(listOf("Calendar", "Café Nero", "Camera"), find("ca"))
        assertEquals(listOf("Google Maps"), find("maps"))
        assertEquals(listOf("Google Maps"), find("google m"))
        assertEquals(listOf("YouTube"), find("tube"))
        assertEquals(listOf("WhatsApp"), find("app"))
        assertEquals(listOf("T-Mobile"), find("mob"))
        assertEquals(listOf("YouTube", "YT Music"), find("y"))
    }

    @Test
    fun caseAndAccentsDoNotMatter() {
        assertEquals(listOf("Café Nero"), find("CAFE"))
        assertEquals(listOf("Café Nero"), find("café n"))
    }

    @Test
    fun aStartBeatsAWordBeatsTheMiddle() {
        val list = listOf("Contacts", "Messages", "My Actions", "Activity Log")
        assertEquals(listOf("Activity Log", "My Actions", "Contacts"), find("act", list))
        assertEquals(emptyList<String>(), find("ou"))
        assertEquals(listOf("YouTube"), find("out"))
    }

    @Test
    fun eachTierAndItsEdge() {
        assertEquals(Match.WORDS, tier("goo map", "Google Maps"))
        assertEquals(Match.INITIALS, tier("yt", "YouTube"))
        assertEquals(Match.INITIALS, tier("gm", "Google Maps"))
        assertEquals(Match.IN_ORDER, tier("wfi", "Wi-Fi"))
        assertEquals(Match.IN_ORDER, tier("clk", "Clock"))
        // Camera's "a" isn't at a word start.
        assertNull(tier("ae", "Camera"))
        assertEquals(Match.TYPO, tier("settimgs", "Settings"))
        assertEquals(Match.TYPO, tier("yuotube", "YouTube"))
        // A dropped letter leaves the rest in order; an added one is a typo.
        assertEquals(Match.IN_ORDER, tier("settngs", "Settings"))
        assertEquals(Match.IN_ORDER, tier("yotube", "YouTube"))
        assertEquals(Match.TYPO, tier("setttings", "Settings"))
        assertEquals(Match.TYPO, tier("youtubbe", "YouTube"))
        assertEquals(Match.TYPO, tier("pfotos", "Photos"))
        // Three characters are too few to guess a typo from.
        assertNull(tier("cak", "Calendar"))
        // The compact forms.
        assertEquals(Match.PREFIX, tier("wifi", "Wi-Fi"))
        assertEquals(Match.PREFIX, tier("tmobile", "T-Mobile"))
        // YT Music starts with it; YouTube only has it as initials.
        assertEquals(listOf("YT Music", "YouTube"), find("yt"))
        assertEquals(listOf("Google Maps"), find("goo map"))
        assertEquals(listOf("YouTube"), find("yuotube"))
    }

    @Test
    fun aTypoShowsOnlyWhenNothingMatchedBetter() {
        // "cafe" is one letter from Camera and Calendar, but Café Nero starts with it.
        assertEquals(Match.TYPO, tier("cafe", "Camera"))
        assertEquals(listOf("Café Nero"), find("cafe"))
        assertEquals(listOf("Settings"), find("settimgs"))
    }

    @Test
    fun weightsOrderATierButNeverBeatABetterOne() {
        // Calendar, Café Nero and Camera are all prefix matches: the heaviest first.
        assertEquals(listOf("Camera", "Calendar", "Café Nero"), find("ca", weights = mapOf("Camera" to 0.5, "Calendar" to 0.1)))
        // Activity Log starts with "act"; a heavy My Actions (a later word) stays behind it.
        val list = listOf("Contacts", "My Actions", "Activity Log")
        assertEquals(listOf("Activity Log", "My Actions", "Contacts"), find("act", list, mapOf("My Actions" to 0.9, "Contacts" to 0.9)))
    }

    private val mixed =
        entries(Kind.APPS, listOf("WiFi Analyzer", "Wish", "Maps", "Clock")) +
            entries(Kind.SHORTCUTS, listOf("New timer", "Maps: Home")) +
            entries(Kind.SETTINGS, listOf("Wi-Fi", "Bluetooth")) +
            entries(Kind.CONTACTS, listOf("Wilma Fish", "Mapper Joe"))

    private fun blocks(q: String, answer: Answer? = null) = Search.run(q, mixed, answer = { answer })

    @Test
    fun blocksByBestTierThenTheFixedOrder() {
        // "wfi": the Wi-Fi page and Wilma Fish both have it in order; Settings comes before Contacts, and Wolf has no match.
        val wfi = Search.run("wfi", entries(Kind.CONTACTS, listOf("Wilma Fish")) + entries(Kind.SETTINGS, listOf("Wi-Fi")) + entries(Kind.APPS, listOf("Wolf")))
        assertEquals(listOf(Kind.SETTINGS, Kind.CONTACTS), wfi.blocks.map { it.kind })
        assertEquals("Wi-Fi", wfi.go?.value)
        // "wifi": the Wi-Fi page starts with it (0); Wilma Fish has it in order (5).
        assertEquals(listOf(Kind.SETTINGS, Kind.CONTACTS), Search.run("wifi", entries(Kind.CONTACTS, listOf("Wilma Fish")) + entries(Kind.SETTINGS, listOf("Wi-Fi"))).blocks.map { it.kind })
        // With WiFi Analyzer, the app and the Wi-Fi page both start with "wifi"; the tie goes to Apps.
        assertEquals(listOf(Kind.APPS, Kind.SETTINGS), blocks("wifi").blocks.map { it.kind }.take(2))
        // "maps": an app and a shortcut start with it; the fixed order.
        assertEquals(listOf(Kind.APPS, Kind.SHORTCUTS), blocks("maps").blocks.map { it.kind })
        // "timer": a shortcut's later word, ahead of nothing else.
        assertEquals(listOf(Kind.SHORTCUTS), blocks("timer").blocks.map { it.kind })
    }

    @Test
    fun oneCharacterFindsAppsOnly() {
        assertEquals(listOf(Kind.APPS), blocks("w").blocks.map { it.kind })
    }

    @Test
    fun anAnswerComesFirstButIsNotGo() {
        val r = blocks("clock", Answer("84", "12*7"))
        assertEquals("84", r.answer?.value)
        assertEquals("Clock", r.go?.value)
        // With nothing but an answer, the no-results rows show under it.
        val only = blocks("12*7", Answer("84", "12*7"))
        assertTrue(only.nothing)
        assertNull(only.go)
        assertTrue(!blocks("clock").nothing)
    }

    @Test
    fun aSearchChecksForCancellation() {
        var checks = 0
        Search.run("x", entries(Kind.APPS, (1..20).map { "App $it" }), check = { checks++ })
        assertEquals(3, checks)
    }

    @Test
    fun theWaysOutOfAnEmptySearch() {
        assertEquals(listOf(Elsewhere.Kind.AGENT), elsewhere(SearchTargets(null, null), "zzqx").map { it.kind })
        val all = elsewhere(SearchTargets("com.google", "F-Droid"), " zzqx ")
        assertEquals(listOf(Elsewhere.Kind.WEB, Elsewhere.Kind.STORE, Elsewhere.Kind.AGENT), all.map { it.kind })
        assertEquals("Search F-Droid for \"zzqx\"", all[1].text)
    }
}
