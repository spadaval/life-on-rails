package com.spadaval.lor.sync

import android.content.Context
import com.spadaval.lor.collect.AppUsageCollector
import com.spadaval.lor.collect.CallCollector
import com.spadaval.lor.collect.Collected
import com.spadaval.lor.collect.HealthCollector
import com.spadaval.lor.collect.LocationCollector
import com.spadaval.lor.collect.SmsCollector
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.data.Queue
import com.spadaval.lor.data.Sensor
import com.spadaval.lor.net.LorClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SyncReport(val collected: Map<Sensor, Int>, val uploaded: Int, val pending: Long, val errors: List<String>)

/**
 * One pass: every switched-on, permitted sensor collects what is new into the
 * local queue, then the queue is uploaded. Collection works offline; the queue
 * waits for the next pass if the server cannot be reached.
 */
object Sync {
    private val lock = Mutex()
    private const val BATCH = 400

    suspend fun run(context: Context): SyncReport = lock.withLock {
        withContext(Dispatchers.IO) { runLocked(context.applicationContext) }
    }

    private suspend fun runLocked(context: Context): SyncReport {
        val prefs = Prefs(context)
        val queue = Queue(context)
        val now = System.currentTimeMillis()
        val collected = mutableMapOf<Sensor, Int>()
        val errors = mutableListOf<String>()

        suspend fun from(sensor: Sensor, permitted: suspend () -> Boolean, collect: suspend () -> Collected) {
            if (!prefs.enabled(sensor)) return
            try {
                if (!permitted()) {
                    errors += "${sensor.label}: not allowed yet"
                    return
                }
                val result = collect()
                queue.put(result.items)
                result.commit() // only once the items are safely in the queue
                collected[sensor] = result.items.size
            } catch (e: SecurityException) {
                errors += "${sensor.label}: permission denied (${e.message})"
            } catch (e: Exception) {
                errors += "${sensor.label}: ${e.message ?: e.javaClass.simpleName}"
            }
        }

        val apps = AppUsageCollector(context, prefs)
        val calls = CallCollector(context, prefs)
        val texts = SmsCollector(context, prefs)
        val health = HealthCollector(context, prefs)
        val location = LocationCollector(context, prefs)
        from(Sensor.APPS, { apps.permitted() }) { apps.collect(now) }
        from(Sensor.CALLS, { calls.permitted() }) { calls.collect(now) }
        from(Sensor.TEXTS, { texts.permitted() }) { texts.collect(now) }
        from(Sensor.HEALTH, { health.permitted() }) { health.collect(now) }
        from(Sensor.LOCATION, { location.foregroundPermitted() }) { location.collect(now) }

        var uploaded = 0
        val client = LorClient(prefs.serverUrl)
        try {
            while (true) {
                val batch = queue.take(BATCH)
                if (batch.isEmpty()) break
                val response = client.ingest(prefs.deviceName, batch.map { it.second })
                uploaded += response.optInt("stored", 0)
                val rejected = response.optJSONArray("rejected")
                if (rejected != null && rejected.length() > 0) {
                    // A rejected observation will never be accepted; keep the first reason and drop it.
                    errors += "server rejected ${rejected.length()}: ${rejected.getJSONObject(0).optString("error")}"
                }
                queue.remove(batch.map { it.first })
                prefs.lastUploadAt = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            errors += "upload: ${e.message ?: e.javaClass.simpleName}"
        }

        val pending = queue.count()
        prefs.lastSyncAt = now
        prefs.uploadedTotal = prefs.uploadedTotal + uploaded
        prefs.lastSyncSummary = buildString {
            append(if (collected.isEmpty()) "Nothing collected" else "Collected " + collected.entries.joinToString(", ") { "${it.value} ${it.key.unit}" })
            append("; $uploaded new on the server")
            if (pending > 0) append("; $pending waiting to upload")
            append('.')
        }
        prefs.lastSyncErrors = errors.joinToString("\n")
        return SyncReport(collected, uploaded, pending, errors)
    }
}
