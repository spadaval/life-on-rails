package com.spadaval.lor.home

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spadaval.lor.LorApp
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Saira
import com.spadaval.lor.ui.SairaCondensed
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Home's header, the `clock` block (launcher polish 6.7, lor-leh4): the date or a clock in
 * DESIGN.md's type, in the wallpaper's ink, with today's next event, the next alarm and the weather
 * under it. The event and the weather come from `/device/home` through the process's reader
 * ([LorApp.reads]), which the block holds while it is composed and home is started; the alarm is
 * Android's (`AlarmManager.getNextAlarmClock`, no permission). A tap opens the calendar at today,
 * one on the alarm line the alarm list. Nothing moves but the minute.
 */

private val Clock = DateTimeFormatter.ofPattern("H:mm")

/**
 * The event line: the one under way ("<title> until H:mm"), else the next one today ("<title> at
 * H:mm"), else null. [events] are the reply's, not yet over when read; ones over by [now] are
 * dropped, and so are ones on another day than [now]'s.
 */
fun headerEvent(events: List<DeviceHome.Event>, now: Instant, zone: ZoneId): String? {
    val today = now.atZone(zone).toLocalDate()
    val left = events.filter { it.end.isAfter(now) && it.start.atZone(zone).toLocalDate() <= today }.sortedBy { it.start }
    left.firstOrNull { !it.start.isAfter(now) }?.let { return "${it.title} until ${Clock.format(it.end.atZone(zone))}" }
    return left.firstOrNull { it.start.atZone(zone).toLocalDate() == today }?.let { "${it.title} at ${Clock.format(it.start.atZone(zone))}" }
}

/** How long ahead the next alarm is shown. */
private val ALARM_AHEAD: Duration = Duration.ofHours(24)

/**
 * The alarm line: "Alarm 7:00" in home's 24-hour clock when the next alarm, at [ringsAt], rings
 * within [ALARM_AHEAD] of [now] (24 hours away still shows); else, or with none, null.
 */
fun alarmLine(ringsAt: Instant?, now: Instant, zone: ZoneId): String? {
    if (ringsAt == null || ringsAt.isBefore(now) || ringsAt.isAfter(now.plus(ALARM_AHEAD))) return null
    return "Alarm ${Clock.format(ringsAt.atZone(zone))}"
}

/** How old a reading may be and still be shown. */
private val WEATHER_FOR: Duration = Duration.ofHours(2)

/**
 * The weather line: "18° Partly cloudy", in whole degrees Fahrenheit when [fahrenheit] (the
 * locale's measurement system is US), else Celsius; null without [weather] or when it was observed
 * more than [WEATHER_FOR] before [now].
 */
fun weatherLine(weather: DeviceHome.Weather?, now: Instant, fahrenheit: Boolean): String? {
    if (weather == null || weather.observedAt.plus(WEATHER_FOR).isBefore(now)) return null
    val degrees = if (fahrenheit) weather.tempC * 9 / 5 + 32 else weather.tempC
    return "${Math.round(degrees)}° ${weather.condition}"
}

/** The locale's measurement system is the US's, whose people read Fahrenheit (decision lor-g4ta). */
private fun usesFahrenheit(): Boolean =
    LocaleData.getMeasurementSystem(ULocale.getDefault(ULocale.Category.FORMAT)) == LocaleData.MeasurementSystem.US

/**
 * Now, read again each minute (`ACTION_TIME_TICK`) and when the time or zone changes, while home is
 * started: the receiver is registered on start and let go on stop, so a home in the background
 * takes no ticks. Starting again reads the time at once.
 */
@Composable
private fun rememberMinute(): Instant = rememberWhileStarted(
    listOf(Intent.ACTION_TIME_TICK, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED),
) { Instant.now() }

/** When the next alarm rings, read again when it changes, while home is started; null with none. */
@Composable
private fun rememberNextAlarm(): Instant? {
    val context = LocalContext.current
    return rememberWhileStarted(listOf(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime?.let(Instant::ofEpochMilli)
    }
}

/**
 * [read], read again on each broadcast of [actions] while home is started, and on each start: the
 * receiver is registered on start and let go on stop, so a home in the background takes none.
 */
@Composable
private fun <T> rememberWhileStarted(actions: List<String>, read: () -> T): T {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var value by remember { mutableStateOf(read()) }
    DisposableEffect(context, lifecycle) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                value = read()
            }
        }
        val filter = IntentFilter().apply { actions.forEach(::addAction) }
        var registered = false
        fun start() {
            if (registered) return
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
            value = read()
        }
        fun stop() {
            if (!registered) return
            context.unregisterReceiver(receiver)
            registered = false
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            stop()
        }
    }
    return value
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
    val now = rememberMinute()
    val ringsAt = rememberNextAlarm()
    val zone = ZoneId.systemDefault()
    val reads = (LocalContext.current.applicationContext as LorApp).reads
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { reads.hold() } }
    val reply = reads.home
    val event = reply?.day?.events?.let { headerEvent(it, now, zone) }
    val alarm = alarmLine(ringsAt, now, zone)
    val weather = weatherLine(reply?.weather, now, remember { usesFahrenheit() })
    val date = DateTimeFormatter.ofPattern("EEEE d MMMM").format(now.atZone(zone))
    val shadow = ink.shadow
    val line = TextStyle(fontFamily = Saira, fontSize = 15.sp, color = ink.text, shadow = shadow)
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
            Text(Clock.format(now.atZone(zone)), style = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 56.sp, color = ink.text, shadow = shadow, fontFeatureSettings = "tnum"))
            Text(date.uppercase(), style = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp, color = ink.text, shadow = shadow))
        } else {
            Text(date, style = TextStyle(fontFamily = Saira, fontSize = size.sp, color = ink.text, shadow = shadow))
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
