package com.spadaval.lor.home

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.time.TimeSource

/*
 * What home's common code takes from the platform it is compiled for, where homecore's own targets
 * can supply it without the app: Android's own calls in androidMain (so the phone runs exactly
 * what it ran before the split, lor-xdoi), the browser's in wasmJsMain. What only the app has
 * (icons, widgets, panels, launching, LoR's reads) comes through [HomePlatform] instead.
 */

/**
 * `kotlin.jvm.Synchronized` on the phone, nothing in the browser, which has one thread: for a
 * class the app's threads share ([DotBook]).
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class JvmSynchronized()

/** The platform's log: logcat on the phone, the console in the browser. */
expect object Log {
    fun d(tag: String, msg: String): Int
    fun i(tag: String, msg: String): Int
    fun w(tag: String, msg: String, tr: Throwable? = null): Int
}

/** Back while [enabled], as androidx.activity's handler of that name: the newest enabled one gets it. */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * A back gesture while [enabled], as androidx.activity's handler of that name: [onBack] collects
 * the gesture's progress, 0 to 1, and returns when it is committed; a cancelled gesture cancels
 * the collection. Before Android 14 only the commit arrives.
 */
@Composable
expect fun PredictiveBackHandler(enabled: Boolean = true, onBack: suspend (progress: Flow<Float>) -> Unit)

/** The screen's width in dp, as Android's configuration gives it (the lab's: its phone frame's). */
@Composable
expect fun screenWidthDp(): Int

/** The smaller of the screen's width and height in dp, as Android's configuration gives it. */
@Composable
expect fun smallestScreenWidthDp(): Int

/** The screen's width and height in dp and its orientation, as Android's configuration gives them: a change means a fold or a turn. */
@Composable
expect fun screenShape(): List<Int>

/** [s] in Unicode's canonical decomposition (NFD), so accents can be dropped from it. */
expect fun decompose(s: String): String

/** [day]'s full name in the phone's language, as `DateTimeFormatter`'s "EEEE" writes it ("Sunday"). */
expect fun dayName(day: kotlinx.datetime.DayOfWeek): String

/** The locale's decimal point. */
expect fun decimalSeparator(): Char

/**
 * [v] (not 0) rounded half up to [digits] significant digits, trailing zeros dropped, as Java's
 * `BigDecimal` writes it: plain, or from 1e15 and under 1e-6 as `1.5E15` and `1.5E-7`.
 */
expect fun significant(v: Double, digits: Int): String

/** Search's log tag. */
const val SEARCH_TAG = "LorSearch"

private val started = TimeSource.Monotonic.markNow()

/** A monotonic clock in nanoseconds, as `System.nanoTime` is used here: only differences mean anything. */
fun nanoTime(): Long = started.elapsedNow().inWholeNanoseconds

/** [x] with [digits] places after the point, as `"%.2f".format` writes it in English (half up). */
fun fixed(x: Double, digits: Int): String {
    val scale = 10.0.pow(digits)
    val n = floor(abs(x) * scale + 0.5).toLong()
    val whole = (n / scale.toLong()).toString()
    val part = (n % scale.toLong()).toString().padStart(digits, '0')
    return (if (x < 0 && n != 0L) "-" else "") + if (digits > 0) "$whole.$part" else whole
}
