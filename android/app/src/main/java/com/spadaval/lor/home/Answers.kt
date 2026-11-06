package com.spadaval.lor.home

import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormatSymbols

/*
 * Quick answers in search (launcher polish 5.4): a calculator and unit conversions. No Android
 * here, so the JVM tests reach all of it. Kvaesitso (GPL-3.0, `CalculatorRepository.kt`,
 * `UnitConverterRepository.kt`; ideas only) answers with mXparser and splits a conversion as
 * "<number> <unit> [> or to] [<unit>]"; here a small recursive-descent parser and a unit table, no
 * library, and no currency (its rates need the network).
 */

object Answers {
    /** The answer to [query], or null. [decimal] is the locale's decimal point. */
    fun of(query: String, decimal: Char = DecimalFormatSymbols.getInstance().decimalSeparator): Answer? {
        val q = query.trim()
        if (q.isEmpty() || q.none { it.isDigit() }) return null
        return calculate(q, decimal) ?: convert(q, decimal)
    }

    /** "2 + 3 * 4" → 14, to 10 significant digits; null unless [q] is a sum that parses whole. */
    internal fun calculate(q: String, decimal: Char = '.'): Answer? {
        // A phone number or a date ("555-1234", "2026-11-06"): only '-', each between digits.
        val ops = q.indices.filter { q[it] in OPERATORS }
        if (ops.isNotEmpty() && ops.all { q[it] == '-' && it > 0 && q[it - 1].isDigit() && q.getOrNull(it + 1)?.isDigit() == true }) return null
        val p = Parser(q, decimal)
        val v = try {
            p.whole()
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (!p.binary || !v.isFinite()) return null
        return Answer(format(v, 10, decimal), q)
    }

    /** "5 km" → "3.107 mi", "5 km to m" → "5000 m", to 4 significant digits; null when it isn't one. */
    internal fun convert(q: String, decimal: Char = '.'): Answer? {
        val m = NUMBER_THEN.matchEntire(q.lowercase()) ?: return null
        val n = parseNumber(m.groupValues[1], decimal) ?: return null
        val rest = m.groupValues[2].trim()
        val (from, to) = Measure.find(rest)?.let { it to it.default }
            ?: SPLIT.findAll(rest).firstNotNullOfOrNull { s ->
                val a = Measure.find(rest.substring(0, s.range.first)) ?: return@firstNotNullOfOrNull null
                val b = Measure.find(rest.substring(s.range.last + 1)) ?: return@firstNotNullOfOrNull null
                a to b
            }
            ?: return null
        if (to == null || from.dimension != to.dimension || from == to) return null
        val v = to.fromBase(from.toBase(n))
        if (!v.isFinite()) return null
        return Answer("${format(v, 4, decimal)} ${to.symbol}", q)
    }

    /** [v] to [digits] significant digits, without trailing zeros, with [decimal] as the point. */
    internal fun format(v: Double, digits: Int, decimal: Char = '.'): String {
        if (v == 0.0) return "0"
        val d = BigDecimal(v).round(MathContext(digits)).stripTrailingZeros()
        val big = d.abs() >= BigDecimal("1e15") || d.abs() < BigDecimal("1e-6")
        val s = if (big) d.toString().replace("E+", "E") else d.toPlainString()
        return if (decimal == '.') s else s.replace('.', decimal)
    }

    private const val OPERATORS = "+-*/×÷^"
    private val NUMBER_THEN = Regex("""(-?[\d.,]+)\s*([^\d].*)""")
    private val SPLIT = Regex("""\s+(?:to|in|>)\s+""")

    /**
     * A number as typed: [decimal] is the decimal point, and the other separator is too, except
     * before exactly three digits, where it separates thousands ("1,000" and "1,5" with a '.'
     * locale).
     */
    internal fun parseNumber(s: String, decimal: Char): Double? {
        val other = if (decimal == '.') ',' else '.'
        val out = StringBuilder()
        var point = false
        for (i in s.indices) {
            val c = s[i]
            when {
                c.isDigit() || (c == '-' && i == 0) -> out.append(c)
                c == decimal || c == other -> {
                    val thousands = c == other && !point && i > 0 &&
                        (1..3).all { s.getOrNull(i + it)?.isDigit() == true } && s.getOrNull(i + 4)?.isDigit() != true
                    if (thousands) continue
                    if (point) return null
                    point = true
                    out.append('.')
                }
                else -> return null
            }
        }
        return out.toString().toDoubleOrNull()
    }

    /** expr = term (('+'|'-') term)*; term = unary (('*'|'/') unary)*; unary = '-' unary | power; power = primary ('^' unary)?. */
    private class Parser(val s: String, val decimal: Char) {
        var i = 0
        /** It held an operator between two values (so "-3" or "(4)" alone isn't a sum). */
        var binary = false

        fun whole(): Double {
            val v = expr()
            skip()
            require(i == s.length) { "trailing text" }
            return v
        }

        private fun skip() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun at(vararg cs: Char): Char? {
            skip()
            return s.getOrNull(i)?.takeIf { it in cs }?.also { i++ }
        }

        private fun expr(): Double {
            var v = term()
            while (true) {
                val op = at('+', '-') ?: return v
                binary = true
                v = if (op == '+') v + term() else v - term()
            }
        }

        private fun term(): Double {
            var v = unary()
            while (true) {
                val op = at('*', '/', '×', '÷') ?: return v
                binary = true
                v = if (op == '*' || op == '×') v * unary() else v / unary()
            }
        }

        private fun unary(): Double = when (at('-', '+')) {
            '-' -> -unary()
            '+' -> unary()
            else -> power()
        }

        private fun power(): Double {
            val base = primary()
            if (at('^') == null) return base
            binary = true
            return Math.pow(base, unary())
        }

        private fun primary(): Double {
            if (at('(') != null) {
                val v = expr()
                require(at(')') != null) { "unclosed" }
                return v
            }
            skip()
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) i++
            require(i > start) { "a number" }
            return requireNotNull(parseNumber(s.substring(start, i), decimal)) { "a number" }
        }
    }

    /** What a unit measures; only units of one dimension convert. */
    enum class Dimension { LENGTH, MASS, VOLUME, TEMPERATURE, SPEED, DATA }

    /**
     * A unit: [names] as typed (lower case), the [symbol] it is written with, its size in the
     * dimension's base unit ([factor]; temperature has its own sums), and [to], the unit "5 km"
     * converts to when no target is typed.
     */
    enum class Measure(val dimension: Dimension, val symbol: String, val factor: Double, val names: List<String>, private val to: String) {
        MM(Dimension.LENGTH, "mm", 0.001, listOf("mm", "millimeter", "millimeters", "millimetre", "millimetres"), "IN"),
        CM(Dimension.LENGTH, "cm", 0.01, listOf("cm", "centimeter", "centimeters", "centimetre", "centimetres"), "IN"),
        M(Dimension.LENGTH, "m", 1.0, listOf("m", "meter", "meters", "metre", "metres"), "FT"),
        KM(Dimension.LENGTH, "km", 1000.0, listOf("km", "kilometer", "kilometers", "kilometre", "kilometres"), "MI"),
        IN(Dimension.LENGTH, "in", 0.0254, listOf("in", "inch", "inches", "\""), "CM"),
        FT(Dimension.LENGTH, "ft", 0.3048, listOf("ft", "foot", "feet", "'"), "M"),
        YD(Dimension.LENGTH, "yd", 0.9144, listOf("yd", "yard", "yards"), "M"),
        MI(Dimension.LENGTH, "mi", 1609.344, listOf("mi", "mile", "miles"), "KM"),
        MG(Dimension.MASS, "mg", 0.001, listOf("mg", "milligram", "milligrams"), "G"),
        G(Dimension.MASS, "g", 1.0, listOf("g", "gram", "grams"), "OZ"),
        KG(Dimension.MASS, "kg", 1000.0, listOf("kg", "kilo", "kilos", "kilogram", "kilograms"), "LB"),
        OZ(Dimension.MASS, "oz", 28.349523125, listOf("oz", "ounce", "ounces"), "G"),
        LB(Dimension.MASS, "lb", 453.59237, listOf("lb", "lbs", "pound", "pounds"), "KG"),
        ML(Dimension.VOLUME, "ml", 0.001, listOf("ml", "milliliter", "milliliters", "millilitre", "millilitres"), "FLOZ"),
        L(Dimension.VOLUME, "l", 1.0, listOf("l", "liter", "liters", "litre", "litres"), "GAL"),
        FLOZ(Dimension.VOLUME, "fl oz", 0.0295735295625, listOf("fl oz", "floz", "fluid ounce", "fluid ounces"), "ML"),
        CUP(Dimension.VOLUME, "cup", 0.2365882365, listOf("cup", "cups"), "ML"),
        GAL(Dimension.VOLUME, "gal", 3.785411784, listOf("gal", "gallon", "gallons"), "L"),
        C(Dimension.TEMPERATURE, "°C", 1.0, listOf("c", "°c", "celsius"), "F"),
        F(Dimension.TEMPERATURE, "°F", 1.0, listOf("f", "°f", "fahrenheit"), "C"),
        K(Dimension.TEMPERATURE, "K", 1.0, listOf("k", "kelvin"), "C"),
        KMH(Dimension.SPEED, "km/h", 1 / 3.6, listOf("km/h", "kmh", "kph"), "MPH"),
        MPH(Dimension.SPEED, "mph", 0.44704, listOf("mph", "mi/h"), "KMH"),
        MS(Dimension.SPEED, "m/s", 1.0, listOf("m/s"), "KMH"),
        KN(Dimension.SPEED, "kn", 1852 / 3600.0, listOf("kn", "knot", "knots"), "KMH"),
        B(Dimension.DATA, "B", 1.0, listOf("b", "byte", "bytes"), "KB"),
        KB(Dimension.DATA, "KB", 1e3, listOf("kb", "kilobyte", "kilobytes"), "MB"),
        MB(Dimension.DATA, "MB", 1e6, listOf("mb", "megabyte", "megabytes"), "GB"),
        GB(Dimension.DATA, "GB", 1e9, listOf("gb", "gigabyte", "gigabytes"), "MB"),
        TB(Dimension.DATA, "TB", 1e12, listOf("tb", "terabyte", "terabytes"), "GB");

        val default: Measure? get() = entries.firstOrNull { it.name == to }

        fun toBase(v: Double): Double = when (this) {
            C -> v
            F -> (v - 32) * 5 / 9
            K -> v - 273.15
            else -> v * factor
        }

        fun fromBase(v: Double): Double = when (this) {
            C -> v
            F -> v * 9 / 5 + 32
            K -> v + 273.15
            else -> v / factor
        }

        companion object {
            fun find(s: String): Measure? {
                val n = s.trim().replace(Regex("\\s+"), " ")
                return entries.firstOrNull { n in it.names }
            }
        }
    }
}
