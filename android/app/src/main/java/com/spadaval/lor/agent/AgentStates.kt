package com.spadaval.lor.agent

import com.spadaval.lor.net.DeviceHome
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * What the agent page's Needs you section says besides its cards (launcher polish 3.3). Pure.
 *
 * An all-clear claims only what the page could see: LoR's last read succeeded and T3 is live with
 * its shell loaded. A source that can't be seen, with nothing of its own listed, is named instead.
 * A source still being read (the first read of LoR, T3 connecting) says nothing either way.
 */
object AgentStates {
    enum class Source { SEEN, READING, UNSEEN }

    enum class Note { ALL_CLEAR, LOR_UNSEEN, T3_UNSEEN }

    fun notes(lor: Source, t3: Source, t3Cards: Int, lorNeeds: Int): List<Note> {
        if (lor == Source.SEEN && t3 == Source.SEEN && t3Cards == 0 && lorNeeds == 0) return listOf(Note.ALL_CLEAR)
        return listOfNotNull(
            Note.LOR_UNSEEN.takeIf { lor == Source.UNSEEN && lorNeeds == 0 },
            Note.T3_UNSEEN.takeIf { t3 == Source.UNSEEN && t3Cards == 0 },
        )
    }

    private val Clock = DateTimeFormatter.ofPattern("H:mm")

    /** What's next today, for the all-clear: the event under way, else the next one today, else none. */
    fun next(events: List<DeviceHome.Event>, now: Instant, zone: ZoneId): String {
        val today = now.atZone(zone).toLocalDate()
        events.firstOrNull { !it.start.isAfter(now) && it.end.isAfter(now) }
            ?.let { return "${it.title} until ${it.end.atZone(zone).format(Clock)}." }
        events.filter { it.start.isAfter(now) && it.start.atZone(zone).toLocalDate() == today }.minByOrNull { it.start }
            ?.let { return "Next: ${it.title} at ${it.start.atZone(zone).format(Clock)}." }
        return "Nothing else on the calendar today."
    }
}
