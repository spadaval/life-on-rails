package com.spadaval.lor.ui

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** `/device/state` as the Timer tab reads it: JSON's nulls and blanks are none, as the tab drew them before. */
class TimerReplyTest {
    @Test
    fun anIdleTimerListsItsIntents() {
        val r = timerReply(
            JSONObject(
                """{"login":"owner","running":null,
                   "recent":[{"id":"i1","title":"Spec"}],
                   "intents":[{"id":"i1","title":"Spec","type":"task"},{"id":"i2","title":"Run","type":"habit"}]}""",
            ),
        )
        assertEquals("owner", r.login)
        assertNull(r.running)
        assertEquals(listOf(TimerIntent("i1", "Spec", "")), r.recent)
        assertEquals(listOf("Spec · task", "Run · habit"), r.intents.map { "${it.title} · ${it.type}" })
    }

    @Test
    fun aPausedTimerKeepsWhenItStartedAndPaused() {
        val r = timerReply(
            JSONObject(
                """{"login":"owner","running":{"intent":"Spec","title":"null","t_start":"2026-11-17T19:00:00Z","paused_at":"2026-11-17T19:30:00Z"}}""",
            ),
        )
        val running = r.running!!
        assertEquals("Spec", running.intent)
        assertNull(running.title)
        assertEquals(Instant.parse("2026-11-17T19:00:00Z").toEpochMilli(), running.startMs)
        assertEquals(30 * 60_000L, running.pausedAtMs!! - running.startMs)
        assertEquals(emptyList<TimerIntent>(), r.intents)
    }

    @Test
    fun aRunningTimerWithoutAnIntentHasNone() {
        val r = timerReply(JSONObject("""{"running":{"intent":"","t_start":"2026-11-17T19:00:00Z","paused_at":null}}"""))
        assertNull(r.running!!.intent)
        assertNull(r.running!!.pausedAtMs)
        assertEquals("", r.login)
    }
}
