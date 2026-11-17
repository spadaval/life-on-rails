package com.spadaval.lor.home

/*
 * kotlinx-datetime in the browser knows only UTC and fixed offsets until js-joda's zone database is
 * loaded; the lab's fixtures are in America/New_York. [loadTimeZones] is called once at start.
 */

@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule : JsAny

private var zones: JsAny? = null

fun loadTimeZones() {
    zones = JsJodaTimeZoneModule
}
