package com.spadaval.lor.agent

import com.spadaval.lor.agent.AgentStates.Note
import com.spadaval.lor.agent.AgentStates.Source
import com.spadaval.lor.net.DeviceHome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** The agent page's all-clear and what it says it can't see (launcher polish 3.3). */
class AgentStatesTest {
    @Test
    fun allClearOnlyWhenBothSourcesAreSeenAndEmpty() {
        assertEquals(listOf(Note.ALL_CLEAR), AgentStates.notes(Source.SEEN, Source.SEEN, 0, 0))
        assertEquals(emptyList<Note>(), AgentStates.notes(Source.SEEN, Source.SEEN, 1, 0))
        assertEquals(emptyList<Note>(), AgentStates.notes(Source.SEEN, Source.SEEN, 0, 2))
    }

    @Test
    fun anUnseenSourceWithNothingListedIsNamed() {
        assertEquals(listOf(Note.LOR_UNSEEN), AgentStates.notes(Source.UNSEEN, Source.SEEN, 0, 0))
        assertEquals(listOf(Note.T3_UNSEEN), AgentStates.notes(Source.SEEN, Source.UNSEEN, 0, 0))
        assertEquals(listOf(Note.LOR_UNSEEN, Note.T3_UNSEEN), AgentStates.notes(Source.UNSEEN, Source.UNSEEN, 0, 0))
        // LoR's last needs are still listed, so they aren't called missing.
        assertEquals(listOf(Note.T3_UNSEEN), AgentStates.notes(Source.UNSEEN, Source.UNSEEN, 0, 1))
        assertEquals(listOf(Note.LOR_UNSEEN), AgentStates.notes(Source.UNSEEN, Source.UNSEEN, 1, 0))
    }

    @Test
    fun aSourceStillBeingReadSaysNothing() {
        assertEquals(emptyList<Note>(), AgentStates.notes(Source.READING, Source.SEEN, 0, 0))
        assertEquals(emptyList<Note>(), AgentStates.notes(Source.SEEN, Source.READING, 0, 0))
    }

    private val zone = ZoneId.of("America/New_York")
    private fun at(s: String) = Instant.parse(s)
    private val now = at("2026-11-06T15:00:00Z") // 10:00 in New York
    private val dentist = DeviceHome.Event("Dentist", at("2026-11-06T14:30:00Z"), at("2026-11-06T15:30:00Z"))
    private val dinner = DeviceHome.Event("Dinner", at("2026-11-06T23:00:00Z"), at("2026-11-07T00:30:00Z"))
    private val tomorrow = DeviceHome.Event("Standup", at("2026-11-07T14:00:00Z"), at("2026-11-07T14:15:00Z"))

    @Test
    fun nextIsTheEventUnderWayThenTheNextTodayThenNone() {
        assertEquals("In Dentist until 10:30.", AgentStates.next(listOf(dentist, dinner), now, zone))
        assertEquals("Free until 18:00.", AgentStates.next(listOf(dinner, tomorrow), now, zone))
        assertEquals("Nothing else on the calendar today.", AgentStates.next(listOf(tomorrow), now, zone))
        assertEquals("Nothing else on the calendar today.", AgentStates.next(emptyList(), now, zone))
    }

    @Test
    fun aFoldKeepsThePlaceReadLastAndAnUnfoldGivesItBack() {
        // lor-ozj2: the queue scrolled 900 px, Today 400 px tall in the single column.
        assertEquals(1300, FormScroll.narrow(left = 0, queue = 900, today = 400))
        assertEquals(0 to 900, FormScroll.full(1300, 400))
        // Only Today scrolled: the single column takes its scroll, and gives it back.
        assertEquals(150, FormScroll.narrow(left = 150, queue = 0, today = 400))
        assertEquals(150 to 0, FormScroll.full(150, 400))
        // At the queue's end, the other form's end, whose length differs.
        assertEquals(Int.MAX_VALUE, FormScroll.narrow(left = 0, queue = 900, today = 400, atEnd = true))
        assertEquals(0 to Int.MAX_VALUE, FormScroll.full(1100, 400, atEnd = true))
        assertEquals("Today alone at the end is Today's", 150 to 0, FormScroll.full(150, 400, atEnd = true))
    }
}
