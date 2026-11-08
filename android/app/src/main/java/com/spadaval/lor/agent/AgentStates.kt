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

    /**
     * The all-clear's second line: what Today's list above it doesn't say. The event under way and
     * when it ends (the list shows start times only), else how long he is free (the next event is
     * already listed, so it isn't named again), else that nothing else is on today.
     */
    fun next(events: List<DeviceHome.Event>, now: Instant, zone: ZoneId): String {
        val today = now.atZone(zone).toLocalDate()
        events.firstOrNull { !it.start.isAfter(now) && it.end.isAfter(now) }
            ?.let { return "In ${it.title} until ${it.end.atZone(zone).format(Clock)}." }
        events.filter { it.start.isAfter(now) && it.start.atZone(zone).toLocalDate() == today }.minByOrNull { it.start }
            ?.let { return "Free until ${it.start.atZone(zone).format(Clock)}." }
        return "Nothing else on the calendar today."
    }
}

/**
 * Where the agent page's scroll goes when its form changes (lor-ozj2), in px. The full form has
 * two columns, Today ([left]) and the queue ([queue]); the narrow form one column, Today then the
 * queue, Today [today] px tall in it. A fold keeps the place read last: down the queue when it was
 * scrolled, else Today's; an unfold puts it back, so a fold and an unfold return where they were.
 * A column at its end ([atEnd]) stays at the end of the other form, whose lengths differ.
 * [Int.MAX_VALUE] means the end (a scroll stops there).
 */
object FormScroll {
    fun narrow(left: Int, queue: Int, today: Int, atEnd: Boolean = false): Int = when {
        queue > 0 && atEnd -> Int.MAX_VALUE
        queue > 0 -> today + queue
        else -> left
    }

    /** The full form's (left, queue) from the narrow form's [narrow]. */
    fun full(narrow: Int, today: Int, atEnd: Boolean = false): Pair<Int, Int> = when {
        narrow <= today -> narrow to 0
        atEnd -> 0 to Int.MAX_VALUE
        else -> 0 to narrow - today
    }
}
