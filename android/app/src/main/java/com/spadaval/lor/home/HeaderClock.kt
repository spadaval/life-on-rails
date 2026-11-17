package com.spadaval.lor.home

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.LocaleData
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Instant

/*
 * The Android half of home's header (Header.kt, launcher polish 6.7, lor-leh4): the minute and
 * the next alarm read from Android's broadcasts while home is started, and the locale's
 * measurement system. [AndroidHome] hands them to the header in homecore.
 */

/** The locale's measurement system is the US's, whose people read Fahrenheit (decision lor-g4ta). */
internal fun usesFahrenheit(): Boolean =
    LocaleData.getMeasurementSystem(ULocale.getDefault(ULocale.Category.FORMAT)) == LocaleData.MeasurementSystem.US

/**
 * Now, read again each minute (`ACTION_TIME_TICK`) and when the time or zone changes, while home is
 * started: the receiver is registered on start and let go on stop, so a home in the background
 * takes no ticks. Starting again reads the time at once.
 */
@Composable
internal fun rememberMinute(): Instant = rememberWhileStarted(
    listOf(Intent.ACTION_TIME_TICK, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED),
) { Instant.now() }

/** When the next alarm rings, read again when it changes, while home is started; null with none. */
@Composable
internal fun rememberNextAlarm(): Instant? {
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

