@file:OptIn(ExperimentalStdlibApi::class)

package com.spadaval.lor.home

/*
 * kotlinx-datetime in the browser knows only UTC and fixed offsets until js-joda's zone database is
 * loaded; the lab's fixtures are in New York. Importing the package registers it with the js-joda
 * kotlinx-datetime reads, and the eager value keeps the import in the bundle, for the lab and the
 * Node tests alike.
 */

@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule : JsAny

@EagerInitialization
private val zones: JsAny = JsJodaTimeZoneModule
