package com.spadaval.lor.net

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/*
 * DeviceHome's times are kotlin.time's since it moved to homecore (lor-xdoi); the agent page and
 * the LoR panel format them with java.time as before, through these.
 */

fun Instant.atZone(zone: TimeZone): ZonedDateTime = toJavaInstant().atZone(zone.toJavaZoneId())

fun Instant.atZone(zone: ZoneId): ZonedDateTime = toJavaInstant().atZone(zone)

fun Instant.isAfter(other: Instant): Boolean = this > other

fun Instant.isBefore(other: Instant): Boolean = this < other
