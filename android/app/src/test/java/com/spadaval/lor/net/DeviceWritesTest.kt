package com.spadaval.lor.net

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

/**
 * The panels' write replies (lor-rz50), recorded from the test server (`:3857`, `life_on_rails_test`)
 * with `curl -X POST /device/task` and `/device/timer` on 2026-11-08: `lor/task-queue.json`,
 * `task-unqueue.json`, `task-done.json`, `task-reopen.json` (one task through each action),
 * `task-refused.json` (409: Done on a Google task) and `timer-paused.json` (a pause's reply).
 */
class DeviceWritesTest {
    private fun text(name: String) = javaClass.classLoader!!.getResource("lor/$name")!!.readText()
    private fun json(name: String) = JSONObject(text(name))

    @Test
    fun readsATaskAfterEachWrite() {
        val queued = DeviceTask.parse(json("task-queue.json"))
        assertEquals("Record the replies", queued.title)
        assertTrue(queued.agent)
        assertEquals(Instant.parse("2026-11-08T09:03:46.348Z"), queued.queuedAt)
        assertFalse(queued.done)

        val back = DeviceTask.parse(json("task-unqueue.json"))
        assertFalse(back.agent)
        assertNull(back.queuedAt)

        val done = DeviceTask.parse(json("task-done.json"))
        assertTrue(done.done)
        assertFalse(done.flagged)
        assertFalse(DeviceTask.parse(json("task-reopen.json")).done)
    }

    /** A refusal shows the server's own message, in [LorClient.describe]'s words. */
    @Test
    fun aRefusalSaysWhatTheServerSaid() {
        val message = LorClient.refusal(409, text("task-refused.json"))
        assertEquals("409: this task lives in Google Tasks: complete it there and the next poll marks it done here", message)
        assertEquals(
            "409: this task lives in Google Tasks: complete it there and the next poll marks it done here.",
            LorClient.describe(IOException(message)),
        )
        // A reply without the server's error body says its code and the start of its text.
        assertEquals("502: Bad gateway", LorClient.refusal(502, "Bad gateway"))
    }

    @Test
    fun readsWhatTheTimerLeftRunning() {
        val s = DeviceState.parse(json("timer-paused.json"))
        val r = s.running!!
        assertTrue(r.paused)
        assertEquals("Write the moving checklist", r.name)
        assertEquals(Instant.parse("2026-11-08T07:53:45.269Z"), r.start)
        assertEquals(listOf("Write the moving checklist"), s.recent.map { it.title })
        assertEquals("Unlabelled", DeviceState.Running(null, null, r.start, false).name)
        assertEquals("Mail", DeviceState.Running(null, "Mail", r.start, false).name)
        val stopped = json("timer-paused.json").put("running", JSONObject.NULL)
        assertNull(DeviceState.parse(stopped).running)
    }
}
