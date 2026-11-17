package com.spadaval.lor.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

actual object Log {
    actual fun d(tag: String, msg: String): Int = 0.also { console("debug", "$tag: $msg") }
    actual fun i(tag: String, msg: String): Int = 0.also { console("info", "$tag: $msg") }
    actual fun w(tag: String, msg: String, tr: Throwable?): Int = 0.also { console("warn", "$tag: $msg${tr?.let { " ($it)" } ?: ""}") }
}

private fun console(level: String, line: String): Unit = js("console[level](line)")

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    val back = rememberNavigationEventState(NavigationEventInfo.None)
    val current by rememberUpdatedState(onBack)
    NavigationBackHandler(back, isBackEnabled = enabled, onBackCompleted = { current() })
}

/**
 * The lab's back (its Back button and gesture.mjs's `back()`, through the navigation-event
 * dispatcher) as androidx.activity's PredictiveBackHandler hands it to its caller: one collection
 * per gesture, of its progress, cancelled with the gesture and ended by its commit.
 */
@Composable
actual fun PredictiveBackHandler(enabled: Boolean, onBack: suspend (progress: Flow<Float>) -> Unit) {
    val back = rememberNavigationEventState(NavigationEventInfo.None)
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(onBack)
    val gesture = remember { arrayOfNulls<Pair<Channel<Float>, Job>>(1) }
    fun start(): Pair<Channel<Float>, Job> = gesture[0] ?: Channel<Float>(Channel.UNLIMITED).let { c ->
        (c to scope.launch { current(c.consumeAsFlow()) }).also { gesture[0] = it }
    }
    LaunchedEffect(back) {
        snapshotFlow { (back.transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent?.progress }
            .collect { p -> if (p != null) start().first.trySend(p) }
    }
    NavigationBackHandler(
        back,
        isBackEnabled = enabled,
        onBackCancelled = {
            gesture[0]?.second?.cancel(CancellationException("back cancelled"))
            gesture[0] = null
        },
        onBackCompleted = {
            start().first.close()
            gesture[0] = null
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun screenWidthDp(): Int = LocalWindowInfo.current.containerSize.width.let { with(LocalDensity.current) { it.toDp().value.toInt() } }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun smallestScreenWidthDp(): Int = LocalWindowInfo.current.containerSize.let { s ->
    with(LocalDensity.current) { minOf(s.width, s.height).toDp().value.toInt() }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun screenShape(): List<Int> = LocalWindowInfo.current.containerSize.let { s ->
    with(LocalDensity.current) { listOf(s.width.toDp().value.toInt(), s.height.toDp().value.toInt(), if (s.width > s.height) 2 else 1) }
}

actual fun decompose(s: String): String = normalize(s)

private fun normalize(s: String): String = js("s.normalize('NFD')")

actual fun decimalSeparator(): Char = '.'

/**
 * JavaScript's `toExponential` rounds the exact binary value half away from zero, as
 * `BigDecimal.round` with HALF_UP does; the digits and exponent are then written as Java would.
 */
actual fun significant(v: Double, digits: Int): String {
    val e = toExponential(v, digits - 1)
    val negative = e.startsWith("-")
    val mantissa = e.removePrefix("-").substringBefore('e')
    val exponent = e.substringAfter('e').toInt()
    val unscaled = mantissa.replace(".", "").trimEnd('0').ifEmpty { "0" }
    val sign = if (negative) "-" else ""
    // The value is 0.<unscaled> x 10^(exponent + 1).
    if (exponent >= 15 || exponent < -6) {
        val point = if (unscaled.length > 1) unscaled.substring(0, 1) + "." + unscaled.substring(1) else unscaled
        return "$sign${point}E$exponent"
    }
    val intDigits = exponent + 1
    val plain = when {
        intDigits <= 0 -> "0." + "0".repeat(-intDigits) + unscaled
        intDigits >= unscaled.length -> unscaled + "0".repeat(intDigits - unscaled.length)
        else -> unscaled.substring(0, intDigits) + "." + unscaled.substring(intDigits)
    }
    return sign + plain
}

private fun toExponential(v: Double, fractionDigits: Int): String = js("v.toExponential(fractionDigits)")

actual fun dayName(day: kotlinx.datetime.DayOfWeek): String = day.name.lowercase().replaceFirstChar { it.uppercase() }
