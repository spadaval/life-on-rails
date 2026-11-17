package com.spadaval.lor.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * Clock times and days as the panels write them, in a named zone (`America/New_York`). The zone's
 * offset at an instant comes from the platform (java.time on the phone, the browser's Intl in the
 * lab), so neither build carries a zone database. Names are English, as the app's
 * `DateTimeFormatter`s write them on an English phone.
 */

/** The offset from UTC, in seconds, that [zone] has at [epochMs]. An unknown zone is UTC. */
internal expect fun zoneOffsetSeconds(zone: String, epochMs: Long): Int

object LocalClock {
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** [at] as the wall clock in [zone] reads it. */
    fun local(at: Instant, zone: String): LocalDateTime {
        val offset = UtcOffset(seconds = zoneOffsetSeconds(zone, at.toEpochMilliseconds()))
        return at.toLocalDateTime(FixedOffsetTimeZone(offset))
    }

    /** The day [at] falls on in [zone]. */
    fun date(at: Instant, zone: String): LocalDate = local(at, zone).date

    /** "9:05", "15:00": the pattern `H:mm`. */
    fun time(at: Instant, zone: String): String = local(at, zone).let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" }

    /** "Tue 17 Nov": the pattern `EEE d MMM`. */
    fun day(at: Instant, zone: String): String = local(at, zone).date.let { "${weekday(it.dayOfWeek)} ${it.dayOfMonth} ${month(it)}" }

    /** "17 Nov": the pattern `d MMM`. */
    fun dayMonth(at: Instant, zone: String): String = local(at, zone).date.let { "${it.dayOfMonth} ${month(it)}" }

    private fun month(d: LocalDate) = MONTHS[d.month.ordinal]

    private fun weekday(d: DayOfWeek) = d.name.take(1) + d.name.substring(1, 3).lowercase()

    /** An ISO time with `Z` or an offset, or null when it doesn't read. */
    fun parse(iso: String): Instant? = runCatching { Instant.parse(iso) }.getOrNull()
}
