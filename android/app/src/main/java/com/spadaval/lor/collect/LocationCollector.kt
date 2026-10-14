package com.spadaval.lor.collect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import com.spadaval.lor.data.Prefs
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/** One location fix per sync. Places and journeys are worked out on the server later. */
class LocationCollector(private val context: Context, private val prefs: Prefs) {

    fun foregroundPermitted(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun backgroundPermitted(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun permitted(): Boolean = foregroundPermitted() && backgroundPermitted()

    @Suppress("MissingPermission")
    suspend fun collect(now: Long): Collected {
        if (!foregroundPermitted()) return Collected(emptyList())
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = when {
            lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return Collected(emptyList()) // location is off
        }
        val fix: Location? = withTimeoutOrNull(30_000) {
            suspendCancellableCoroutine { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                lm.getCurrentLocation(provider, cancel, context.mainExecutor) { loc -> if (cont.isActive) cont.resume(loc) }
            }
        } ?: lm.getLastKnownLocation(provider)?.takeIf { now - it.time < 20 * 60_000 }
        if (fix == null) return Collected(emptyList())
        val device = prefs.deviceName
        val payload = JSONObject()
            .put("kind", "location")
            .put("lat", fix.latitude)
            .put("lon", fix.longitude)
            .put("accuracy_m", fix.accuracy.toDouble())
            .put("provider", provider)
        return Collected(listOf(Obs.envelope(Obs.id("loc", device, fix.time), device, fix.time, fix.time, "location", null, payload)))
    }
}
