package com.spadaval.lor.lab

import com.spadaval.lor.home.LayoutFile
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.net.DevicePeople
import com.spadaval.lor.net.DeviceTasks
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The fixtures' wire-format parts are what the app reads: every layout parses with the app's own
 * parser (but `failed`'s, which is broken on purpose), and every LoR reply with its own.
 */
class FixtureWireTest {
    @Test
    fun everyLayoutParsesButTheBrokenOne() {
        for (f in Fixtures.all) {
            val text = f.layout?.toString() ?: continue
            if (f.name == "failed") assertThrows(Exception::class.java) { LayoutFile.parse(text) }
            else LayoutFile.parse(text)
        }
    }

    @Test
    fun everyReplyParses() {
        for (f in Fixtures.all) {
            f.lor.home.reply?.let { DeviceHome.parse(JSONObject(it.toString())) }
            f.lor.tasks.reply?.let { DeviceTasks.parse(JSONObject(it.toString())) }
            f.lor.people.reply?.let { DevicePeople.parse(JSONObject(it.toString())) }
        }
    }

    @Test
    fun theDemoDayIsTheDashboards() {
        val home = DeviceHome.parse(JSONObject(Fixtures.demo.lor.home.reply.toString()))
        assertEquals("2026-11-17T20:00:00Z", home.now.toString())
        assertEquals(3, home.needsYou.size)
        assertEquals(9, DeviceHome.parse(JSONObject(Fixtures.overfull.lor.home.reply.toString())).needsYou.size)
    }
}
