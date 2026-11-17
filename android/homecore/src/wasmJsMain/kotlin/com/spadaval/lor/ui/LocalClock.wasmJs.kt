@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.spadaval.lor.ui

internal actual fun zoneOffsetSeconds(zone: String, epochMs: Long): Int = offsetSeconds(zone, epochMs.toDouble()).toInt()

/** The browser's Intl reads the wall clock in [zone]; the offset is that clock less UTC's. */
private fun offsetSeconds(zone: String, ms: Double): Double = js(
    """(() => {
  try {
    const f = new Intl.DateTimeFormat('en-US', { timeZone: zone, hourCycle: 'h23', year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric' });
    const p = {};
    for (const x of f.formatToParts(new Date(ms))) p[x.type] = x.value;
    const wall = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute, +p.second);
    return Math.round((wall - Math.floor(ms / 1000) * 1000) / 1000);
  } catch (e) {
    return 0;
  }
})()"""
)
