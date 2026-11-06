package com.spadaval.lor.home

import android.content.BroadcastReceiver
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spadaval.lor.agent.AgentModel
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Saira
import com.spadaval.lor.ui.SairaCondensed
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Home's header (launcher polish 6.7): the date or a clock in DESIGN.md's type, in the wallpaper's
 * ink, with today's next event under it from the agent page's last read of `/device/home` (no
 * request of its own). A tap opens the calendar at today. Nothing moves but the minute.
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

/**
 * Now, read again each minute (`ACTION_TIME_TICK`) and when the time or zone changes, while home is
 * started: the receiver is registered on start and let go on stop, so a home in the background
 * takes no ticks. Starting again reads the time at once.
 */
@Composable
private fun rememberMinute(): Instant {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableStateOf(Instant.now()) }
    DisposableEffect(context, lifecycle) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                now = Instant.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        var registered = false
        fun start() {
            if (registered) return
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
            now = Instant.now()
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
    return now
}

/** The header: [HomeLook.Header] decides date, clock or nothing; [size] is the date's size in sp. */
@Composable
internal fun HomeHeader(style: HomeLook.Header, size: Int, openCalendar: () -> Unit) {
    if (style == HomeLook.Header.NONE) return
    val ink = LocalWallInk.current
    val now = rememberMinute()
    val zone = ZoneId.systemDefault()
    val reply by AgentModel.last.collectAsState()
    val event = reply?.day?.events?.let { headerEvent(it, now, zone) }
    val date = DateTimeFormatter.ofPattern("EEEE d MMMM").format(now.atZone(zone))
    val shadow = ink.shadow
    Column(
        Modifier.padding(start = 20.dp, top = 24.dp, bottom = 28.dp)
            .clickable(role = Role.Button, onClickLabel = "Open calendar", onClick = openCalendar)
            .clearAndSetSemantics { contentDescription = listOfNotNull(date, event).joinToString(". ") },
    ) {
        if (style == HomeLook.Header.CLOCK) {
            // DESIGN.md's display readout, then the date as a title.
            Text(Clock.format(now.atZone(zone)), style = TextStyle(fontFamily = SairaCondensed, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 56.sp, color = ink.text, shadow = shadow, fontFeatureSettings = "tnum"))
            Text(date.uppercase(), style = TextStyle(fontFamily = SairaCondensed, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp, color = ink.text, shadow = shadow))
        } else {
            Text(date, style = TextStyle(fontFamily = Saira, fontSize = size.sp, color = ink.text, shadow = shadow))
        }
        event?.let {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // DESIGN.md's calendar mark: a small cyan square turned 45 degrees.
                Spacer(Modifier.size(7.dp).rotate(45f).background(Cy))
                Spacer(Modifier.width(10.dp))
                Text(it, style = TextStyle(fontFamily = Saira, fontSize = 15.sp, color = ink.text, shadow = shadow), maxLines = 1)
            }
        }
    }
}
