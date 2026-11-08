package com.spadaval.lor.lor

import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.net.DevicePeople
import com.spadaval.lor.net.DeviceTasks
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What LoR's panel and blocks write in their rows, the way the web writes it (`src/components/format.ts`,
 * `src/routes/index.tsx`, `tasks.tsx`, `people.tsx`), and which rows a block shows. Pure, for the JVM tests.
 */
object LorRows {
    /** The first [max] of [rows] and how many more there are (a block's "N more" line); all of them for a null [max]. */
    fun <T> cap(rows: List<T>, max: Int?): Pair<List<T>, Int> =
        if (max == null || rows.size <= max) rows to 0 else rows.take(max) to rows.size - max

    /** en-GB's short months and weekdays, as the web's `Intl.DateTimeFormat("en-GB")` writes them, whatever the phone's locale. */
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")
    private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /** "15 Oct", as the web's `shortDate`. */
    fun shortDate(d: LocalDate): String = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}"

    /** A due date the way a list reads it, as the web's `dueLabel`: Today, Tomorrow, Yesterday, a weekday within the week, else the date. */
    fun dueLabel(due: LocalDate, today: LocalDate): String {
        val d = ChronoUnit.DAYS.between(today, due)
        return when {
            d == 0L -> "Today"
            d == 1L -> "Tomorrow"
            d == -1L -> "Yesterday"
            d in 2..6 -> WEEKDAYS[due.dayOfWeek.value - 1]
            else -> shortDate(due)
        }
    }

    /** The line under a task in Due soon, as the web's: "due 15 Oct · waiting on Priya · House move · Google Tasks". */
    fun dueSoonLine(t: DeviceTasks.Task): String =
        listOfNotNull(t.due?.let { "due ${shortDate(it)}" }, t.waitingOn?.let { "waiting on $it" }, t.parent, "Google Tasks".takeIf { t.google }).joinToString(" · ")

    /** The line under a task in the Tasks tab: what the web's Tasks row shows beside the title, its due date as [dueLabel]. */
    fun taskLine(t: DeviceTasks.Task, today: LocalDate): String =
        listOfNotNull(
            t.due?.let { dueLabel(it, today) },
            t.waitingOn?.let { "waiting on $it" },
            t.parent,
            "Google Tasks".takeIf { t.google },
        ).joinToString(" · ")

    /** A group of the Tasks tab: the web's Tasks grouped by due date, flagged first. */
    data class Group(val title: String, val tasks: List<DeviceTasks.Task>)

    /**
     * The open tasks as the web's Tasks groups them by due date (`grouped` in `tasks.tsx`): Needs
     * you (flagged), Past due, Today, This week (the next six days), Later on and No date, each in
     * the order given (by due date, then title), empty groups left out.
     */
    fun groups(tasks: List<DeviceTasks.Task>, today: LocalDate): List<Group> {
        val week = today.plusDays(6)
        val (flagged, rest) = tasks.partition { it.flagged }
        return listOf(
            Group("Needs you", flagged),
            Group("Past due", rest.filter { it.due != null && it.due < today }),
            Group("Today", rest.filter { it.due == today }),
            Group("This week", rest.filter { it.due != null && it.due > today && it.due <= week }),
            Group("Later on", rest.filter { it.due != null && it.due > week }),
            Group("No date", rest.filter { it.due == null }),
        ).filter { it.tasks.isNotEmpty() }
    }

    /** How long ago, as the web's `ago`: "40s ago", "12m ago", "5h ago", "3d ago"; "never" for none. */
    fun ago(at: Instant?, now: Instant): String {
        if (at == null) return "never"
        val s = Duration.between(at, now).seconds.coerceAtLeast(0)
        return when {
            s < 90 -> "${s}s ago"
            s < 90 * 60 -> "${Math.round(s / 60.0)}m ago"
            s < 36 * 3600 -> "${Math.round(s / 3600.0)}h ago"
            else -> "${Math.round(s / 86400.0)}d ago"
        }
    }

    /** How someone was last in touch, as the web's People list says it. */
    private val VIA = mapOf("mail_in" to "they emailed", "mail_out" to "you emailed", "meeting" to "you met", "phone" to "call, text or WhatsApp")

    /** Last in touch, as the web's People column: "40d ago · call, text or WhatsApp", or "Not seen yet". */
    fun lastTouch(p: DevicePeople.Person, now: Instant): String =
        if (p.lastAt == null) "Not seen yet" else listOfNotNull(ago(p.lastAt, now), p.lastVia?.let { VIA[it] ?: it }).joinToString(" · ")

    /** "10 days past the cadence", for someone past it; null otherwise. */
    fun overdueLine(p: DevicePeople.Person): String? = p.overdueBy?.takeIf { it > 0 }?.let { "$it days past the cadence" }

    /** "in 5 days, turning 36" or "today", for a birthday within 30 days; null otherwise. */
    fun birthdayLine(p: DevicePeople.Person): String? {
        val days = p.birthdayIn?.takeIf { it <= 30 } ?: return null
        return (if (days == 0) "today" else "in $days days") + (p.turning?.let { ", turning $it" } ?: "")
    }

    /** What an agent task is waiting on: queued for the next run, or worked in a T3 thread. */
    fun agentLine(t: DeviceHome.AgentTask): String = when {
        t.thread != null -> "in a T3 thread"
        t.queuedAt != null -> "queued for the next run"
        else -> "given to the agent"
    }

    /** Where a row's text opens: the dashboard's Tasks or People at the row's id. */
    fun taskPath(id: String) = "/tasks?id=$id"
    fun personPath(id: String) = "/people?id=$id"
}
