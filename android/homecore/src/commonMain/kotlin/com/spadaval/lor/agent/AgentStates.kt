package com.spadaval.lor.agent

import com.spadaval.lor.lor.LorHome
import com.spadaval.lor.ui.LocalClock
import kotlin.time.Instant

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

    /**
     * The all-clear's second line: what Today's list above it doesn't say. The event under way and
     * when it ends (the list shows start times only), else how long he is free (the next event is
     * already listed, so it isn't named again), else that nothing else is on today.
     */
    fun next(events: List<LorHome.Event>, now: Instant, zone: String): String {
        val today = LocalClock.date(now, zone)
        events.firstOrNull { it.start <= now && it.end > now }
            ?.let { return "In ${it.title} until ${LocalClock.time(it.end, zone)}." }
        events.filter { it.start > now && LocalClock.date(it.start, zone) == today }.minByOrNull { it.start }
            ?.let { return "Free until ${LocalClock.time(it.start, zone)}." }
        return "Nothing else on the calendar today."
    }
}
