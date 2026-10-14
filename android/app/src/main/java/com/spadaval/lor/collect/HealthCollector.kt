package com.spadaval.lor.collect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.spadaval.lor.data.Prefs
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

/** Sleep, exercise, and daily steps from Health Connect. */
class HealthCollector(private val context: Context, private val prefs: Prefs) {

    fun available(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun granted(): Set<String> = if (available()) client.permissionController.getGrantedPermissions() else emptySet()

    /** Reading is allowed once the three data permissions are granted; background reading needs one more. */
    suspend fun permitted(): Boolean = available() && granted().containsAll(DATA_PERMISSIONS)

    suspend fun collect(now: Long): Collected {
        val device = prefs.deviceName
        val end = Instant.ofEpochMilli(now)
        // Re-read a few days each time: records arrive late (a watch syncs in the morning), and ids keep it idempotent.
        val start = end.minusSeconds(LOOKBACK_DAYS * 86_400L)
        val items = mutableListOf<JSONObject>()

        for (r in readAll(SleepSessionRecord::class, start, end)) {
            val stages = JSONObject()
            for (s in r.stages) {
                val name = STAGES[s.stage] ?: "unknown"
                val minutes = (s.endTime.toEpochMilli() - s.startTime.toEpochMilli()) / 60_000
                stages.put(name, stages.optLong(name, 0) + minutes)
            }
            val payload = JSONObject()
                .put("kind", "sleep")
                .put("stages_min", stages)
                .put("source_app", r.metadata.dataOrigin.packageName)
            items += Obs.envelope(Obs.id("sleep", r.metadata.id), device, r.startTime.toEpochMilli(), r.endTime.toEpochMilli(), "health", r.title ?: "Sleep", payload)
        }

        for (r in readAll(ExerciseSessionRecord::class, start, end)) {
            val payload = JSONObject()
                .put("kind", "exercise")
                .put("exercise_type", r.exerciseType)
                .put("source_app", r.metadata.dataOrigin.packageName)
            items += Obs.envelope(Obs.id("exercise", r.metadata.id), device, r.startTime.toEpochMilli(), r.endTime.toEpochMilli(), "health", r.title ?: "Exercise", payload)
        }

        // Steps: one running total per local day, for today and the two days before.
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        for (back in 0L..2L) {
            val day = today.minusDays(back)
            val dayStart = day.atStartOfDay(zone).toInstant()
            val dayEnd = minOf(day.plusDays(1).atStartOfDay(zone).toInstant(), end)
            val result = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(dayStart, dayEnd)))
            val count = result[StepsRecord.COUNT_TOTAL] ?: continue
            val payload = JSONObject().put("kind", "steps").put("count", count).put("date", day.toString())
            // The end grows through the day, which is what lets the server replace the count.
            items += Obs.envelope(Obs.id("steps", device, day), device, dayStart.toEpochMilli(), dayEnd.toEpochMilli(), "health", "$count steps", payload)
        }
        return Collected(items)
    }

    private suspend fun <T : Record> readAll(type: KClass<T>, start: Instant, end: Instant): List<T> {
        val out = mutableListOf<T>()
        var token: String? = null
        do {
            val response = client.readRecords(ReadRecordsRequest(type, TimeRangeFilter.between(start, end), pageToken = token))
            out += response.records
            token = response.pageToken
        } while (!token.isNullOrEmpty())
        return out
    }

    companion object {
        private const val LOOKBACK_DAYS = 3L
        const val BACKGROUND_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        val DATA_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
        )
        val ALL_PERMISSIONS = DATA_PERMISSIONS + BACKGROUND_PERMISSION
        private val STAGES = mapOf(
            SleepSessionRecord.STAGE_TYPE_AWAKE to "awake",
            SleepSessionRecord.STAGE_TYPE_SLEEPING to "sleeping",
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED to "out_of_bed",
            SleepSessionRecord.STAGE_TYPE_LIGHT to "light",
            SleepSessionRecord.STAGE_TYPE_DEEP to "deep",
            SleepSessionRecord.STAGE_TYPE_REM to "rem",
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED to "awake_in_bed",
        )
    }
}
