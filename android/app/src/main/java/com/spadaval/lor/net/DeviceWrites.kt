package com.spadaval.lor.net

import com.spadaval.lor.net.DeviceHome.Companion.str
import com.spadaval.lor.net.DeviceHome.Companion.time
import org.json.JSONObject
import java.time.Instant

/** What `POST /device/task` can do to a task (`deviceTask` in `src/server/device.ts`, decision lor-dofd). */
enum class TaskAction(val wire: String) {
    DONE("done"),
    REOPEN("reopen"),
    SEEN("seen"),
    QUEUE("queue"),
    UNQUEUE("unqueue"),
}

/** `POST /device/task`'s reply: the task after the write. */
data class DeviceTask(
    val id: String,
    val title: String,
    val done: Boolean,
    /** Given to the agent (`assignee = 'agent'`). */
    val agent: Boolean,
    /** When it was queued for the agent's next run; null when not queued. */
    val queuedAt: Instant?,
    val flagged: Boolean,
) {
    companion object {
        fun parse(o: JSONObject) = DeviceTask(
            id = o.getString("id"),
            title = o.getString("title"),
            done = o.getBoolean("done"),
            agent = o.getString("assignee") == "agent",
            queuedAt = str(o, "queued_at")?.let(::time),
            flagged = o.getBoolean("flagged"),
        )
    }
}

/**
 * `GET /device/state` and `POST /device/timer`'s reply, as far as the panels use it: what is
 * running and the intents timed most recently (`deviceState`).
 */
data class DeviceState(val running: Running?, val recent: List<Intent>) {
    /** [intent] is the intent's title, [title] the block's own label. */
    data class Running(val intent: String?, val title: String?, val start: Instant, val paused: Boolean) {
        /** What `/device/home` calls it (`runningName` in `src/server/trmnl/device.ts`). */
        val name: String get() = intent ?: title ?: "Unlabelled"
    }

    data class Intent(val id: String, val title: String)

    companion object {
        fun parse(o: JSONObject): DeviceState {
            val recent = o.getJSONArray("recent")
            return DeviceState(
                running = o.optJSONObject("running")?.let {
                    Running(str(it, "intent"), str(it, "title"), time(it.getString("t_start")), it.getBoolean("paused"))
                },
                recent = List(recent.length()) { i -> recent.getJSONObject(i).let { Intent(it.getString("id"), it.getString("title")) } },
            )
        }
    }
}
