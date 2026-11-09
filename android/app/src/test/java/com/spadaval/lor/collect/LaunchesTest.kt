package com.spadaval.lor.collect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchesTest {
    private val at = 1_793_000_000_123L // with milliseconds, which the id and times keep

    private val search = Launch(
        LaunchFrom.SEARCH, "com.google.android.youtube", "com.google.android.youtube.HomeActivity", "YouTube", query = "you",
    )

    @Test
    fun aSearchLaunchIsAnInstantInTheEnvelopeWithItsSourceAndWhatWasTyped() {
        val o = Launches.observation("pixel-9a", at, search)
        // Exactly the envelope's fields (src/shared/observation.ts), so ingest's zod parse accepts it.
        assertEquals(
            setOf("id", "t_start", "t_end", "device", "source", "app", "calendar_id", "title", "url", "payload", "intent_hint"),
            o.keys().asSequence().toSet(),
        )
        assertTrue(o.getString("id").matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
        assertEquals("2026-10-26T07:33:20.123Z", o.getString("t_start"))
        assertEquals(o.getString("t_start"), o.getString("t_end"))
        assertEquals("pixel-9a", o.getString("device"))
        assertEquals("android", o.getString("source"))
        assertEquals("com.google.android.youtube", o.getString("app"))
        assertEquals("YouTube", o.getString("title"))
        assertTrue(o.isNull("url"))
        assertTrue(o.isNull("calendar_id"))
        assertTrue(o.isNull("intent_hint"))
        val p = o.getJSONObject("payload")
        assertEquals(
            mapOf<String, Any?>(
                "kind" to "launch", "from" to "search",
                "package" to "com.google.android.youtube", "activity" to "com.google.android.youtube.HomeActivity",
                "label" to "YouTube", "query" to "you",
            ),
            p.keys().asSequence().associateWith { p.get(it) },
        )
    }

    @Test
    fun theIdIsStableForOneLaunchAndDiffersForAnother() {
        val a = Launches.observation("pixel-9a", at, search).getString("id")
        assertEquals(a, Launches.observation("pixel-9a", at, search).getString("id"))
        // The query doesn't name the launch: the same moment and app is the same launch.
        assertEquals(a, Launches.observation("pixel-9a", at, search.copy(query = "yout")).getString("id"))
        assertNotEquals(a, Launches.observation("pixel-9a", at + 1, search).getString("id"))
        assertNotEquals(a, Launches.observation("fold-7", at, search).getString("id"))
        assertNotEquals(a, Launches.observation("pixel-9a", at, search.copy(pkg = "com.android.chrome")).getString("id"))
    }

    @Test
    fun anAgentCardCarriesItsThreadAndLinkAndLeavesOutWhatItHasNot() {
        val link = "t3code://threads/env%3A1/thread%3A9"
        val o = Launches.observation(
            "pixel-9a", at,
            Launch(LaunchFrom.AGENT_ICON, "com.t3tools.t3code", label = "T3 Code", thread = "thread:9", url = link),
        )
        assertEquals(link, o.getString("url"))
        val p = o.getJSONObject("payload")
        assertEquals("agent_icon", p.getString("from"))
        assertEquals("thread:9", p.getString("thread"))
        assertFalse(p.has("query"))
        assertFalse(p.has("folder"))
        assertFalse(p.has("activity"))
    }

    @Test
    fun aLinkNoAppTookHasNoPackageAndStillGetsAnId() {
        val url = "http://10.0.2.2:3857/tasks?id=1"
        val o = Launches.observation("pixel-9a", at, Launch(LaunchFrom.AGENT_CARD, null, label = null, url = url))
        assertTrue(o.isNull("app"))
        assertTrue(o.isNull("title"))
        assertFalse(o.getJSONObject("payload").has("package"))
        assertNotEquals(
            o.getString("id"),
            Launches.observation("pixel-9a", at, Launch(LaunchFrom.AGENT_CARD, null, label = null, url = "$url&x")).getString("id"),
        )
    }

    @Test
    fun aSearchThatWentToTheWebHasItsQueryAndNoActivity() {
        // Launcher polish 3.4: the app that took ACTION_WEB_SEARCH, or none when Android asked.
        val web = Launch(LaunchFrom.SEARCH, "com.google.android.googlequicksearchbox", label = "Google", query = "zzqx")
        val o = Launches.observation("pixel-9a", at, web)
        val p = o.getJSONObject("payload")
        assertEquals(mapOf<String, Any?>("kind" to "launch", "from" to "search", "package" to "com.google.android.googlequicksearchbox", "label" to "Google", "query" to "zzqx"), p.keys().asSequence().associateWith { p.get(it) })
        assertTrue(o.isNull("url"))
        val asked = Launches.observation("pixel-9a", at, web.copy(pkg = null, label = null))
        assertTrue(asked.isNull("app"))
        assertEquals("zzqx", asked.getJSONObject("payload").getString("query"))
    }

    @Test
    fun aShortcutLaunchCarriesTheShortcutIdAndWhereTheMenuWasOpened() {
        val app = Launch(LaunchFrom.HOME, "com.google.android.deskclock", "com.android.deskclock.DeskClock", "Clock")
        val withShortcut = app.copy(shortcut = "launch_timer")
        val p = Launches.observation("pixel-9a", at, withShortcut).getJSONObject("payload")
        assertEquals("home", p.getString("from"))
        assertEquals("launch_timer", p.getString("shortcut"))
        assertFalse(Launches.observation("pixel-9a", at, app).getJSONObject("payload").has("shortcut"))
        // A shortcut and its app opened at the same moment are two launches.
        assertNotEquals(
            Launches.observation("pixel-9a", at, app).getString("id"),
            Launches.observation("pixel-9a", at, withShortcut).getString("id"),
        )
    }

    @Test
    fun everySourceHasItsWireName() {
        assertEquals(
            listOf("home", "folder", "drawer", "search", "agent_card", "agent_icon"),
            LaunchFrom.entries.map { it.wire },
        )
    }
}
