package com.spadaval.lor.net

import com.spadaval.lor.net.DeviceHome.Companion.str
import com.spadaval.lor.net.DeviceHome.Companion.time
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** `GET /device/tasks`: the open tasks as the web's Due soon lists them, and those done today (`deviceTasks` in `src/server/device.ts`). */
data class DeviceTasks(
    val now: Instant,
    val zone: ZoneId,
    /** The local day the server read them on. */
    val today: LocalDate,
    /** Every open task, including those past the cap of [tasks]. */
    val open: Int,
    /** Open tasks by due date, then title, undated last; at most 100. */
    val tasks: List<Task>,
    val doneToday: List<Task>,
) {
    /** A task as `TaskRow` in `src/server/graph.ts` has it. [agent] is made by the agent; [parent] the first project or goal it serves. */
    data class Task(
        val id: String,
        val title: String,
        val due: LocalDate?,
        val overdue: Boolean,
        /** Who a waiting task waits on ("someone" when nobody is named); null when not waiting. */
        val waitingOn: String?,
        val parent: String?,
        val flagged: Boolean,
        val agent: Boolean,
        val google: Boolean,
    )

    companion object {
        fun parse(o: JSONObject): DeviceTasks = DeviceTasks(
            now = time(o.getString("now")),
            zone = ZoneId.of(o.getString("timezone")),
            today = LocalDate.parse(o.getString("today")),
            open = o.getInt("open"),
            tasks = tasks(o.getJSONArray("tasks")),
            doneToday = tasks(o.getJSONArray("done_today")),
        )

        private fun tasks(a: JSONArray) = List(a.length()) { i ->
            a.getJSONObject(i).let {
                Task(
                    id = it.getString("id"),
                    title = it.getString("title"),
                    due = str(it, "due")?.let(LocalDate::parse),
                    overdue = it.getBoolean("overdue"),
                    waitingOn = str(it, "waiting_on"),
                    parent = str(it, "parent"),
                    flagged = it.getBoolean("flagged"),
                    agent = it.getBoolean("agent"),
                    google = it.getBoolean("google"),
                )
            }
        }
    }
}

/** `GET /device/people`: who is past their keep-in-touch cadence and whose birthday is near (`devicePeople`). No numbers or addresses. */
data class DevicePeople(
    val today: LocalDate,
    /** Past their cadence, most overdue first; at most 30. */
    val overdue: List<Person>,
    /** A birthday within 30 days, soonest first; at most 30. */
    val birthdays: List<Person>,
) {
    /**
     * [lastVia] is how they were last in touch (`mail_in`, `mail_out`, `meeting`, `phone`);
     * [overdueBy] days past [keepInTouchDays]; [birthdayIn] days to the birthday, [turning] the age then.
     */
    data class Person(
        val id: String,
        val name: String,
        val kind: String,
        val lastAt: Instant?,
        val lastVia: String?,
        val overdueBy: Int?,
        val keepInTouchDays: Int?,
        val birthdayIn: Int?,
        val turning: Int?,
    )

    companion object {
        fun parse(o: JSONObject): DevicePeople = DevicePeople(
            today = LocalDate.parse(o.getString("today")),
            overdue = people(o.getJSONArray("overdue")),
            birthdays = people(o.getJSONArray("birthdays")),
        )

        private fun people(a: JSONArray) = List(a.length()) { i ->
            a.getJSONObject(i).let {
                Person(
                    id = it.getString("id"),
                    name = it.getString("name"),
                    kind = it.getString("kind"),
                    lastAt = str(it, "last_at")?.let(::time),
                    lastVia = str(it, "last_via"),
                    overdueBy = int(it, "overdue_by"),
                    keepInTouchDays = int(it, "keep_in_touch_days"),
                    birthdayIn = int(it, "birthday_in"),
                    turning = int(it, "turning"),
                )
            }
        }

        private fun int(o: JSONObject, key: String): Int? = if (o.isNull(key)) null else o.getInt(key)
    }
}
