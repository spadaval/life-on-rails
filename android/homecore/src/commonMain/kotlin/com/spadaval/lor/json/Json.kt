package com.spadaval.lor.json

/*
 * The part of org.json that home's files use (`layout.json`, the launch counts, the look, LoR's
 * `/device/home`), in common code so the parser runs the same on the phone and in the browser
 * (lor-xdoi). It follows Android's own org.json (libcore's `org.json`), which the phone used before:
 * the same lenient reading (single quotes, unquoted words, comments, `=` and `;`), the same values
 * (Int when a whole number fits, else Long, else Double; String; Boolean; [JSONObject.NULL];
 * [JSONObject]; [JSONArray]), the same coercing getters, and the same writing, `\/` included, so a
 * file written by the phone is byte for byte what it wrote before.
 *
 * Two differences, both only in what an error says: a type mismatch names Kotlin's type rather than
 * Java's class, and nesting deeper than [JSONTokener.MAX_DEPTH] is refused rather than left to
 * overflow the stack.
 */

class JSONException(message: String) : RuntimeException(message)

class JSONObject() {
    private val map = LinkedHashMap<String, Any>()

    /** Parses [text]'s first value, which must be an object; what follows it is not read. */
    constructor(text: String) : this(JSONTokener(text))

    constructor(tokener: JSONTokener) : this() {
        val v = tokener.nextValue()
        if (v !is JSONObject) throw typeMismatch(v, "JSONObject")
        map.putAll(v.map)
    }

    /** Puts [value], or removes [name] when it is null. A non-finite number is refused. */
    fun put(name: String, value: Any?): JSONObject {
        if (value == null) {
            map.remove(name)
            return this
        }
        if (value is Number) checkDouble(value.toDouble())
        map[name] = value
        return this
    }

    fun put(name: String, value: Boolean): JSONObject = put(name, value as Any?)
    fun put(name: String, value: Int): JSONObject = put(name, value as Any?)
    fun put(name: String, value: Long): JSONObject = put(name, value as Any?)
    fun put(name: String, value: Double): JSONObject = put(name, value as Any?)

    fun remove(name: String): Any? = map.remove(name)
    fun has(name: String): Boolean = name in map
    fun isNull(name: String): Boolean = map[name].let { it == null || it === NULL }
    fun length(): Int = map.size
    fun keys(): Iterator<String> = map.keys.iterator()
    fun opt(name: String): Any? = map[name]
    fun get(name: String): Any = map[name] ?: throw JSONException("No value for $name")

    fun getBoolean(name: String): Boolean = toBoolean(get(name)) ?: throw typeMismatch(name, get(name), "boolean")
    fun optBoolean(name: String, fallback: Boolean = false): Boolean = toBoolean(opt(name)) ?: fallback
    fun getDouble(name: String): Double = toDouble(get(name)) ?: throw typeMismatch(name, get(name), "double")
    fun optDouble(name: String, fallback: Double = Double.NaN): Double = toDouble(opt(name)) ?: fallback
    fun getInt(name: String): Int = toInt(get(name)) ?: throw typeMismatch(name, get(name), "int")
    fun optInt(name: String, fallback: Int = 0): Int = toInt(opt(name)) ?: fallback
    fun getLong(name: String): Long = toLong(get(name)) ?: throw typeMismatch(name, get(name), "long")
    fun optLong(name: String, fallback: Long = 0L): Long = toLong(opt(name)) ?: fallback
    fun getString(name: String): String = toStr(get(name)) ?: throw typeMismatch(name, get(name), "String")
    fun optString(name: String, fallback: String = ""): String = toStr(opt(name)) ?: fallback
    fun getJSONArray(name: String): JSONArray = get(name) as? JSONArray ?: throw typeMismatch(name, get(name), "JSONArray")
    fun optJSONArray(name: String): JSONArray? = opt(name) as? JSONArray
    fun getJSONObject(name: String): JSONObject = get(name) as? JSONObject ?: throw typeMismatch(name, get(name), "JSONObject")
    fun optJSONObject(name: String): JSONObject? = opt(name) as? JSONObject

    override fun toString(): String = JSONStringer(0).also { writeTo(it) }.toString()

    /** Written as Android's org.json writes it: [indentSpaces] per level, `"key": value`. */
    fun toString(indentSpaces: Int): String = JSONStringer(indentSpaces).also { writeTo(it) }.toString()

    internal fun writeTo(s: JSONStringer) {
        s.open(JSONStringer.Scope.EMPTY_OBJECT, "{")
        for ((k, v) in map) s.key(k).value(v)
        s.close(JSONStringer.Scope.EMPTY_OBJECT, JSONStringer.Scope.NONEMPTY_OBJECT, "}")
    }

    companion object {
        /** JSON's `null`, kept apart from a missing key as org.json does. */
        val NULL: Any = object {
            override fun equals(other: Any?): Boolean = other === this || other == null
            override fun hashCode(): Int = 0
            override fun toString(): String = "null"
        }

        /** As Android's `JSONObject.numberToString`: a Double that is a whole number loses its `.0`. */
        fun numberToString(number: Number): String {
            val d = number.toDouble()
            checkDouble(d)
            if (number is Double) {
                val l = number.toLong()
                if (d == l.toDouble()) return l.toString()
            }
            return number.toString()
        }
    }
}

class JSONArray() {
    private val values = ArrayList<Any?>()

    /** [items] as values; a null becomes [JSONObject.NULL]. */
    constructor(items: Collection<*>) : this() {
        items.forEach { values.add(it ?: JSONObject.NULL) }
    }

    /** Parses [text]'s first value, which must be an array. */
    constructor(text: String) : this() {
        val v = JSONTokener(text).nextValue()
        if (v !is JSONArray) throw typeMismatch(v, "JSONArray")
        values.addAll(v.values)
    }

    fun put(value: Any?): JSONArray { values.add(value); return this }
    fun put(value: Boolean): JSONArray = put(value as Any?)
    fun put(value: Int): JSONArray = put(value as Any?)
    fun put(value: Long): JSONArray = put(value as Any?)
    fun put(value: Double): JSONArray { checkDouble(value); return put(value as Any?) }

    fun length(): Int = values.size
    fun isNull(index: Int): Boolean = opt(index).let { it == null || it === JSONObject.NULL }
    fun opt(index: Int): Any? = values.getOrNull(index)
    fun get(index: Int): Any {
        if (index < 0 || index >= values.size) throw JSONException("Index $index out of range [0..${values.size})")
        return values[index] ?: throw JSONException("Value at $index is null.")
    }

    fun getBoolean(index: Int): Boolean = toBoolean(get(index)) ?: throw typeMismatch(index, get(index), "boolean")
    fun getDouble(index: Int): Double = toDouble(get(index)) ?: throw typeMismatch(index, get(index), "double")
    fun getInt(index: Int): Int = toInt(get(index)) ?: throw typeMismatch(index, get(index), "int")
    fun getLong(index: Int): Long = toLong(get(index)) ?: throw typeMismatch(index, get(index), "long")
    fun getString(index: Int): String = toStr(get(index)) ?: throw typeMismatch(index, get(index), "String")
    fun optString(index: Int, fallback: String = ""): String = toStr(opt(index)) ?: fallback
    fun getJSONArray(index: Int): JSONArray = get(index) as? JSONArray ?: throw typeMismatch(index, get(index), "JSONArray")
    fun optJSONArray(index: Int): JSONArray? = opt(index) as? JSONArray
    fun getJSONObject(index: Int): JSONObject = get(index) as? JSONObject ?: throw typeMismatch(index, get(index), "JSONObject")
    fun optJSONObject(index: Int): JSONObject? = opt(index) as? JSONObject

    override fun toString(): String = JSONStringer(0).also { writeTo(it) }.toString()
    fun toString(indentSpaces: Int): String = JSONStringer(indentSpaces).also { writeTo(it) }.toString()

    internal fun writeTo(s: JSONStringer) {
        s.open(JSONStringer.Scope.EMPTY_ARRAY, "[")
        for (v in values) s.value(v)
        s.close(JSONStringer.Scope.EMPTY_ARRAY, JSONStringer.Scope.NONEMPTY_ARRAY, "]")
    }
}

/** Android's `JSONStringer`, for [JSONObject.toString] and [JSONArray.toString]. */
internal class JSONStringer(indentSpaces: Int) {
    enum class Scope { EMPTY_ARRAY, NONEMPTY_ARRAY, EMPTY_OBJECT, DANGLING_KEY, NONEMPTY_OBJECT }

    private val out = StringBuilder()
    private val stack = ArrayList<Scope>()
    private val indent: String? = if (indentSpaces > 0) " ".repeat(indentSpaces) else null

    fun open(empty: Scope, bracket: String) {
        beforeValue()
        stack.add(empty)
        out.append(bracket)
    }

    fun close(empty: Scope, nonempty: Scope, bracket: String) {
        val context = stack.last()
        if (context != nonempty && context != empty) throw JSONException("Nesting problem")
        stack.removeAt(stack.lastIndex)
        if (context == nonempty) newline()
        out.append(bracket)
    }

    fun key(name: String): JSONStringer {
        when (stack.last()) {
            Scope.NONEMPTY_OBJECT -> out.append(',')
            Scope.EMPTY_OBJECT -> {}
            else -> throw JSONException("Nesting problem")
        }
        newline()
        stack[stack.lastIndex] = Scope.DANGLING_KEY
        string(name)
        return this
    }

    fun value(value: Any?): JSONStringer {
        when (value) {
            is JSONArray -> value.writeTo(this)
            is JSONObject -> value.writeTo(this)
            else -> {
                beforeValue()
                when {
                    value == null || value is Boolean || value === JSONObject.NULL -> out.append(value.toString())
                    value is Number -> out.append(JSONObject.numberToString(value))
                    else -> string(value.toString())
                }
            }
        }
        return this
    }

    private fun string(value: String) {
        out.append('"')
        for (c in value) {
            when (c) {
                '"', '\\', '/' -> out.append('\\').append(c)
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\u000C' -> out.append("\\f")
                else -> if (c.code <= 0x1F) out.append("\\u").append(c.code.toString(16).padStart(4, '0')) else out.append(c)
            }
        }
        out.append('"')
    }

    private fun newline() {
        val indent = indent ?: return
        out.append('\n')
        repeat(stack.size) { out.append(indent) }
    }

    private fun beforeValue() {
        if (stack.isEmpty()) return
        when (stack.last()) {
            Scope.EMPTY_ARRAY -> {
                stack[stack.lastIndex] = Scope.NONEMPTY_ARRAY
                newline()
            }
            Scope.NONEMPTY_ARRAY -> {
                out.append(',')
                newline()
            }
            Scope.DANGLING_KEY -> {
                out.append(if (indent == null) ":" else ": ")
                stack[stack.lastIndex] = Scope.NONEMPTY_OBJECT
            }
            else -> throw JSONException("Nesting problem")
        }
    }

    override fun toString(): String = out.toString()
}

/**
 * Android's `JSONTokener`: reads one value at a time, leniently. Besides JSON it takes single-quoted
 * strings, unquoted words (a bare word is a string), `//`, `/* */` and `#` comments, `=` or `=>` for
 * `:`, and `;` for `,`.
 */
class JSONTokener(text: String) {
    /** The text, without the optional byte-order mark Android's reader also skips. */
    private val text: String = text.removePrefix("\uFEFF")

    private var pos = 0
    private var depth = 0

    fun more(): Boolean = pos < text.length

    /** The next character that is not whitespace or a comment, or `\u0000` at the end. */
    fun nextClean(): Char = nextCleanInternal().let { if (it == -1) 0.toChar() else it.toChar() }

    fun nextValue(): Any = when (val c = nextCleanInternal()) {
        -1 -> throw syntaxError("End of input")
        '{'.code -> nested { readObject() }
        '['.code -> nested { readArray() }
        '\''.code, '"'.code -> nextString(c.toChar())
        else -> {
            pos--
            readLiteral()
        }
    }

    private inline fun <T> nested(read: () -> T): T {
        if (++depth > MAX_DEPTH) throw syntaxError("Nested too deeply")
        try {
            return read()
        } finally {
            depth--
        }
    }

    private fun nextCleanInternal(): Int {
        while (pos < text.length) {
            val c = text[pos++]
            when (c) {
                '\t', ' ', '\n', '\r' -> continue
                '/' -> {
                    if (pos == text.length) return c.code
                    when (text[pos]) {
                        '*' -> {
                            pos++
                            val end = text.indexOf("*/", pos)
                            if (end == -1) throw syntaxError("Unterminated comment")
                            pos = end + 2
                        }
                        '/' -> {
                            pos++
                            skipToEndOfLine()
                        }
                        else -> return c.code
                    }
                }
                '#' -> skipToEndOfLine()
                else -> return c.code
            }
        }
        return -1
    }

    private fun skipToEndOfLine() {
        while (pos < text.length) {
            val c = text[pos]
            if (c == '\r' || c == '\n') {
                pos++
                break
            }
            pos++
        }
    }

    private fun nextString(quote: Char): String {
        var builder: StringBuilder? = null
        var start = pos
        while (pos < text.length) {
            val c = text[pos++]
            if (c == quote) {
                return builder?.append(text, start, pos - 1)?.toString() ?: text.substring(start, pos - 1)
            }
            if (c == '\\') {
                if (pos == text.length) throw syntaxError("Unterminated escape sequence")
                val b = builder ?: StringBuilder().also { builder = it }
                b.append(text, start, pos - 1)
                b.append(readEscapeCharacter())
                start = pos
            }
        }
        throw syntaxError("Unterminated string")
    }

    private fun readEscapeCharacter(): Char = when (val escaped = text[pos++]) {
        'u' -> {
            if (pos + 4 > text.length) throw syntaxError("Unterminated escape sequence")
            val hex = text.substring(pos, pos + 4)
            pos += 4
            hex.toIntOrNull(16)?.toChar() ?: throw syntaxError("Invalid escape sequence: $hex")
        }
        't' -> '\t'
        'b' -> '\b'
        'n' -> '\n'
        'r' -> '\r'
        'f' -> '\u000C'
        else -> escaped
    }

    private fun readLiteral(): Any {
        val literal = nextToInternal("{}[]/\\:,=;# \t\u000C")
        if (literal.isEmpty()) throw syntaxError("Expected literal value")
        if (literal.equals("null", ignoreCase = true)) return JSONObject.NULL
        if (literal.equals("true", ignoreCase = true)) return true
        if (literal.equals("false", ignoreCase = true)) return false
        if ('.' !in literal) {
            var base = 10
            var number = literal
            if (number.startsWith("0x") || number.startsWith("0X")) {
                number = number.substring(2)
                base = 16
            } else if (number.startsWith("0") && number.length > 1) {
                number = number.substring(1)
                base = 8
            }
            parseJavaLong(number, base)?.let { return if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
        }
        parseJavaDouble(literal)?.let { return it }
        return literal
    }

    private fun nextToInternal(excluded: String): String {
        val start = pos
        while (pos < text.length) {
            val c = text[pos]
            if (c == '\r' || c == '\n' || c in excluded) return text.substring(start, pos)
            pos++
        }
        return text.substring(start)
    }

    private fun readObject(): JSONObject {
        val result = JSONObject()
        val first = nextCleanInternal()
        if (first == '}'.code) return result
        if (first != -1) pos--
        while (true) {
            val name = nextValue()
            if (name !is String) {
                if (name === JSONObject.NULL) throw syntaxError("Names cannot be null")
                throw syntaxError("Names must be strings, but $name is of type ${typeName(name)}")
            }
            val separator = nextCleanInternal()
            if (separator != ':'.code && separator != '='.code) throw syntaxError("Expected ':' after $name")
            if (pos < text.length && text[pos] == '>') pos++
            result.put(name, nextValue())
            when (nextCleanInternal()) {
                '}'.code -> return result
                ';'.code, ','.code -> continue
                else -> throw syntaxError("Unterminated object")
            }
        }
    }

    private fun readArray(): JSONArray {
        val result = JSONArray()
        var trailingSeparator = false
        while (true) {
            when (nextCleanInternal()) {
                -1 -> throw syntaxError("Unterminated array")
                ']'.code -> {
                    if (trailingSeparator) result.put(null)
                    return result
                }
                ','.code, ';'.code -> {
                    result.put(null)
                    trailingSeparator = true
                    continue
                }
                else -> pos--
            }
            result.put(nextValue())
            when (nextCleanInternal()) {
                ']'.code -> return result
                ','.code, ';'.code -> trailingSeparator = true
                else -> throw syntaxError("Unterminated array")
            }
        }
    }

    private fun syntaxError(message: String) = JSONException("$message$this")

    override fun toString(): String = " at character $pos of $text"

    companion object {
        /** Deeper input is refused; Android's reader recursed until the stack overflowed. */
        const val MAX_DEPTH = 500
    }
}

// ---- Values as Android's org.json coerces them (its JSON class) ----

private fun checkDouble(d: Double) {
    if (d.isInfinite() || d.isNaN()) throw JSONException("Forbidden numeric value: $d")
}

private fun toBoolean(v: Any?): Boolean? = when {
    v is Boolean -> v
    v is String && v.equals("true", ignoreCase = true) -> true
    v is String && v.equals("false", ignoreCase = true) -> false
    else -> null
}

private fun toDouble(v: Any?): Double? = when (v) {
    is Double -> v
    is Number -> v.toDouble()
    is String -> parseJavaDouble(v)
    else -> null
}

private fun toInt(v: Any?): Int? = when (v) {
    is Int -> v
    is Number -> v.toInt()
    is String -> parseJavaDouble(v)?.toInt()
    else -> null
}

private fun toLong(v: Any?): Long? = when (v) {
    is Long -> v
    is Number -> v.toLong()
    is String -> parseJavaDouble(v)?.toLong()
    else -> null
}

private fun toStr(v: Any?): String? = when {
    v is String -> v
    v is Double -> v.toString()
    v != null -> v.toString()
    else -> null
}

/** As Java's `Long.parseLong(s, radix)`: an optional sign, then digits; null where it would throw. */
private fun parseJavaLong(s: String, radix: Int): Long? {
    if (s.isEmpty()) return null
    val digits = if (s[0] == '-' || s[0] == '+') s.substring(1) else s
    if (digits.isEmpty() || digits.any { it.digitToIntOrNull(radix) == null }) return null
    return s.toLongOrNull(radix)
}

/**
 * As Java's `Double.valueOf` on what a JSON file holds: a decimal number, with or without exponent,
 * `NaN` and `Infinity`; null for anything else. (Java also takes hex floats and an `f` or `d`
 * suffix, which no file of ours writes.)
 */
private fun parseJavaDouble(s: String): Double? {
    val t = s.trim()
    if (!DOUBLE.matches(t)) return null
    return t.toDoubleOrNull()
}

private val DOUBLE = Regex("""[+-]?(NaN|Infinity|((\d+\.?\d*|\.\d+)([eE][+-]?\d+)?))""")

private fun typeName(v: Any): String = when (v) {
    is String -> "java.lang.String"
    is Int -> "java.lang.Integer"
    is Long -> "java.lang.Long"
    is Double -> "java.lang.Double"
    is Boolean -> "java.lang.Boolean"
    is JSONObject -> "org.json.JSONObject"
    is JSONArray -> "org.json.JSONArray"
    else -> v::class.simpleName ?: "?"
}

private fun typeMismatch(actual: Any, required: String) =
    JSONException("Value $actual of type ${typeName(actual)} cannot be converted to $required")

private fun typeMismatch(at: Any, actual: Any, required: String) =
    JSONException("Value $actual at $at of type ${typeName(actual)} cannot be converted to $required")
