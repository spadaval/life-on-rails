package com.spadaval.lor.data

/** The sensors, each of which the owner can switch off. */
enum class Sensor(val key: String, val label: String, val unit: String, val description: String, val defaultOn: Boolean) {
    APPS("apps", "App usage", "app sessions", "Which app was on screen, and for how long.", true),
    CALLS("calls", "Calls", "calls", "Who you called or who called you, and for how long. Never audio.", true),
    TEXTS("texts", "Texts", "texts", "Every text, who and when and what was said, back to the oldest on the phone. RCS chats are not visible to other apps.", true),
    HEALTH("health", "Sleep, exercise, steps", "health records", "Read from Health Connect.", true),
    LAUNCHES("launches", "Launches", "launches", "How each app was opened from this launcher: home, a folder, the drawer, search (with what was typed), or an agent card.", true),
    LOCATION("location", "Location", "locations", "Where the phone was, every 15 minutes or so.", false),
    WHATSAPP("whatsapp", "WhatsApp", "backups", "Uploads WhatsApp's nightly backup, still encrypted, over Wi-Fi. LoR reads every message with your 64-digit backup key.", false),
}

/**
 * The one line a sync leaves for the Sensors tab's Sync card: what it [collected] by sensor, how
 * many observations the server stored, what still waits, and what the WhatsApp upload said.
 */
fun syncSummary(collected: Map<Sensor, Int>, uploaded: Int, pending: Long, whatsapp: String? = null): String = buildString {
    append(if (collected.isEmpty()) "Nothing collected" else "Collected " + collected.entries.joinToString(", ") { "${it.value} ${it.key.unit}" })
    append("; $uploaded new on the server")
    if (pending > 0) append("; $pending waiting to upload")
    if (whatsapp != null) append("; $whatsapp")
    append('.')
}
