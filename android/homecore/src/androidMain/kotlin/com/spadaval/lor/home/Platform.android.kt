package com.spadaval.lor.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormatSymbols
import java.text.Normalizer

actual typealias JvmSynchronized = kotlin.jvm.Synchronized

actual object Log {
    actual fun d(tag: String, msg: String): Int = android.util.Log.d(tag, msg)
    actual fun i(tag: String, msg: String): Int = android.util.Log.i(tag, msg)
    actual fun w(tag: String, msg: String, tr: Throwable?): Int = android.util.Log.w(tag, msg, tr)
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = androidx.activity.compose.BackHandler(enabled, onBack)

@Composable
actual fun PredictiveBackHandler(enabled: Boolean, onBack: suspend (progress: Flow<Float>) -> Unit) =
    androidx.activity.compose.PredictiveBackHandler(enabled) { events -> onBack(events.map { it.progress }) }

@Composable
actual fun screenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

@Composable
actual fun smallestScreenWidthDp(): Int = LocalConfiguration.current.smallestScreenWidthDp

@Composable
actual fun screenShape(): List<Int> = LocalConfiguration.current.let { listOf(it.screenWidthDp, it.screenHeightDp, it.orientation) }

actual fun decompose(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD)

actual fun decimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator

actual fun significant(v: Double, digits: Int): String {
    val d = BigDecimal(v).round(MathContext(digits)).stripTrailingZeros()
    val big = d.abs() >= BigDecimal("1e15") || d.abs() < BigDecimal("1e-6")
    return if (big) d.toString().replace("E+", "E") else d.toPlainString()
}

actual fun dayName(day: kotlinx.datetime.DayOfWeek): String =
    java.time.DayOfWeek.of(day.ordinal + 1).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault(java.util.Locale.Category.FORMAT))
