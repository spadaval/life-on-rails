package com.spadaval.lor.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Saira
import com.spadaval.lor.ui.SairaCondensed
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/*
 * Home's header, the `clock` block (launcher polish 6.7, lor-leh4): the date or a clock in
 * DESIGN.md's type, in the wallpaper's ink, with today's next event, the next alarm and the weather
 * under it. The event and the weather come from `/device/home` through the process's reader
 * ([LorApp.reads]), which the block holds while it is composed and home is started; the alarm is
 * Android's (`AlarmManager.getNextAlarmClock`, no permission). A tap opens the calendar at today,
 * one on the alarm line the alarm list. Nothing moves but the minute.
 */

/** [at] in [zone] as home's 24-hour clock writes it: "H:mm". */
fun clockText(at: Instant, zone: TimeZone): String = at.toLocalDateTime(zone).let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" }

/**
 * The event line: the one under way ("<title> until H:mm"), else the next one today ("<title> at
 * H:mm"), else null. [events] are the reply's, not yet over when read; ones over by [now] are
 * dropped, and so are ones on another day than [now]'s.
 */
fun headerEvent(events: List<DeviceHome.Event>, now: Instant, zone: TimeZone): String? {
    val today = now.toLocalDateTime(zone).date
    val left = events.filter { it.end > now && it.start.toLocalDateTime(zone).date <= today }.sortedBy { it.start }
    left.firstOrNull { it.start <= now }?.let { return "${it.title} until ${clockText(it.end, zone)}" }
    return left.firstOrNull { it.start.toLocalDateTime(zone).date == today }?.let { "${it.title} at ${clockText(it.start, zone)}" }
}

/** How long ahead the next alarm is shown. */
private val ALARM_AHEAD = 24.hours

/**
 * The alarm line: "Alarm 7:00" in home's 24-hour clock when the next alarm, at [ringsAt], rings
 * within [ALARM_AHEAD] of [now] (24 hours away still shows); else, or with none, null.
 */
fun alarmLine(ringsAt: Instant?, now: Instant, zone: TimeZone): String? {
    if (ringsAt == null || ringsAt < now || ringsAt > now + ALARM_AHEAD) return null
    return "Alarm ${clockText(ringsAt, zone)}"
}

/** How old a reading may be and still be shown. */
private val WEATHER_FOR = 2.hours

/**
 * The weather line: "18° Partly cloudy", in whole degrees Fahrenheit when [fahrenheit] (the
 * locale's measurement system is US), else Celsius; null without [weather] or when it was observed
 * more than [WEATHER_FOR] before [now].
 */
fun weatherLine(weather: DeviceHome.Weather?, now: Instant, fahrenheit: Boolean): String? {
    if (weather == null || weather.observedAt + WEATHER_FOR < now) return null
    val degrees = if (fahrenheit) weather.tempC * 9 / 5 + 32 else weather.tempC
    // Half up, as Java's Math.round: floor(x + 0.5).
    return "${floor(degrees + 0.5).toLong()}° ${weather.condition}"
}

/**
 * The header: [HomeLook.Header] decides date, clock or nothing; [size] is the date's size in sp.
 * While composed and home is started it holds the process's reader, so its event and weather stay
 * current wherever the block sits on the grid.
 */
@Composable
internal fun HomeHeader(style: HomeLook.Header, size: Int, openCalendar: () -> Unit, openAlarms: () -> Unit) {
    if (style == HomeLook.Header.NONE) return
    val ink = LocalWallInk.current
    val platform = LocalHomePlatform.current
    val now = platform.rememberMinute()
    val ringsAt = platform.rememberNextAlarm()
    val zone = platform.zone
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { platform.holdLor() } }
    val reply = platform.lor.value.home
    val event = reply?.day?.events?.let { headerEvent(it, now, zone) }
    val alarm = alarmLine(ringsAt, now, zone)
    val weather = weatherLine(reply?.weather, now, platform.fahrenheit)
    val date = platform.dateWords(now)
    val shadow = ink.shadow
    val line = TextStyle(fontFamily = Saira, fontSize = 15.sp, color = ink.text, shadow = shadow, textMotion = TextMotion.Animated)
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp)
            .clickable(role = Role.Button, onClickLabel = "Open calendar", onClick = openCalendar)
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(date, event, alarm, weather).joinToString(". ")
                if (alarm != null) customActions = listOf(CustomAccessibilityAction("Open alarms") { openAlarms(); true })
            },
    ) {
        if (style == HomeLook.Header.CLOCK) {
            // DESIGN.md's display readout, then the date as a title.
            Text(clockText(now, zone), style = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 56.sp, color = ink.text, shadow = shadow, fontFeatureSettings = "tnum", textMotion = TextMotion.Animated))
            Text(date.uppercase(), style = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp, color = ink.text, shadow = shadow, textMotion = TextMotion.Animated))
        } else {
            Text(date, style = TextStyle(fontFamily = Saira, fontSize = size.sp, color = ink.text, shadow = shadow, textMotion = TextMotion.Animated))
        }
        event?.let {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // DESIGN.md's calendar mark: a small cyan square turned 45 degrees.
                Spacer(Modifier.size(7.dp).rotate(45f).background(Cy))
                Spacer(Modifier.width(10.dp))
                Text(it, style = line, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // The alarm and the weather sit under the event, flush with the date, without a mark.
        alarm?.let {
            Text(it, style = line, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp).clickable(onClick = openAlarms))
        }
        weather?.let {
            Text(it, style = line, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
