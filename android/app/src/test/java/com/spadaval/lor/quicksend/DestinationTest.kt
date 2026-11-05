package com.spadaval.lor.quicksend

import com.spadaval.lor.quicksend.Destination.Screen
import com.spadaval.lor.quicksend.Destination.Why
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationTest {
    private val projects = mapOf(
        "p-inbox" to "assistant",
        "p-lor" to "life-on-rails",
        "p-life" to "life",
        "p-t3" to "T3 Code",
    )
    private val threads = mapOf(
        "thread:project:p-lor:1b2c" to "p-lor",
        "5f1e7d0a-uuid" to "p-t3",
        "thread-gone" to "p-deleted",
    )

    private fun choose(prompt: String, screen: Screen? = null, picked: String? = null) =
        Destination.choose(prompt, projects, threads, screen, picked)

    @Test
    fun withNothingElseItGoesToTheInbox() {
        assertEquals(Destination.Choice("p-inbox", Why.INBOX, "fix the tile"), choose("  fix the tile "))
        assertEquals(Destination.Choice(null, Why.INBOX, "x"), Destination.choose("x", mapOf("p" to "other"), emptyMap(), null))
    }

    @Test
    fun aLeadingMentionNamesTheProjectAndIsTakenOut() {
        assertEquals(Destination.Choice("p-lor", Why.NAMED, "fix the tile"), choose("@life-on-rails fix the tile"))
        assertEquals(Destination.Choice("p-lor", Why.NAMED, "fix it"), choose("@Life-On-Rails: fix it"))
        // A title with a space, and the mention alone.
        assertEquals(Destination.Choice("p-t3", Why.NAMED, "why"), choose("@t3 code, why"))
        assertEquals(Destination.Choice("p-t3", Why.NAMED, ""), choose("@T3 Code"))
    }

    @Test
    fun theLongestTitleWinsAndAMentionMustEndAtAWordBoundary() {
        assertEquals("p-life", choose("@life plan the week").projectId)
        assertEquals("p-lor", choose("@life-on-rails plan").projectId)
        // "@lifeline" is not "@life".
        assertEquals(Destination.Choice("p-inbox", Why.INBOX, "@lifeline call"), choose("@lifeline call"))
    }

    @Test
    fun onlyTheStartCounts() {
        assertEquals(Why.INBOX, choose("ask @life-on-rails about it").why)
        assertEquals(Why.INBOX, choose("@nobody hi").why)
    }

    @Test
    fun aNamedProjectWinsOverTheScreenAndAPickWinsOverBoth() {
        val screen = Screen(uris = listOf("https://x/e/5f1e7d0a-uuid"))
        assertEquals("p-lor", choose("@life-on-rails x", screen).projectId)
        assertEquals(Destination.Choice("p-life", Why.PICKED, "x"), choose("@life-on-rails x", screen, picked = "p-life"))
        // A pick of a project the server no longer has is ignored.
        assertEquals(Why.NAMED, choose("@life-on-rails x", screen, picked = "p-deleted").why)
    }

    @Test
    fun aThreadLinkOnScreenGivesItsProject() {
        val app = Screen(uris = listOf("t3code://threads/env-1/thread%3Aproject%3Ap-lor%3A1b2c"))
        assertEquals(Destination.Choice("p-lor", Why.ON_SCREEN, "x"), choose("x", app))
        val web = Screen(uris = listOf("https://buildbot.ts.net/env-1/5f1e7d0a-uuid?tab=diff#end"))
        assertEquals("p-t3", choose("x", web).projectId)
        // The web URI comes first; a thread whose project is gone, an unknown thread, or a project's
        // title in the path says nothing.
        assertEquals("p-t3", choose("x", Screen(uris = listOf("https://x/e/5f1e7d0a-uuid", "t3code://threads/e/thread%3Aproject%3Ap-lor%3A1b2c"))).projectId)
        assertEquals(Why.INBOX, choose("x", Screen(uris = listOf("https://x/e/thread-gone", "https://github.com/me/life-on-rails"))).why)
        assertNull(Destination.onScreen(Screen(), projects, threads))
    }

    @Test
    fun segmentsAreDecodedWithoutSchemeQueryOrFragment() {
        assertEquals(listOf("threads", "e 1", "a:b+c"), Destination.segments("t3code://threads/e%201/a%3Ab+c?x=1#y"))
        assertEquals(listOf("x"), Destination.segments("mailto:x"))
    }
}
