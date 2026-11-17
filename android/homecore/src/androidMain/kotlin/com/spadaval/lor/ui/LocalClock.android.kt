package com.spadaval.lor.ui

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId

internal actual fun zoneOffsetSeconds(zone: String, epochMs: Long): Int =
    try {
        ZoneId.of(zone).rules.getOffset(Instant.ofEpochMilli(epochMs)).totalSeconds
    } catch (e: DateTimeException) {
        0
    }
