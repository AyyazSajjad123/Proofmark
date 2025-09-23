package com.example.proofmark.capture

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

class LocationHelper(private val ctx: Context) {

    /**
     * Fast snapshot: last known location + age => Fresh/Stale/None
     * Fresh  <= 10s,  Stale > 120s,  else Fresh (lenient)
     */
    @SuppressLint("MissingPermission")
    fun getSnapshot(): GpsSnapshot {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val task = client.lastLocation
            // ✅ API 24 compatible (no java.time.Duration)
            val loc: Location? = Tasks.await(task, 2, TimeUnit.SECONDS)

            if (loc == null) {
                GpsSnapshot(null, null, null, Long.MAX_VALUE, "None")
            } else {
                val ageSec = ((System.currentTimeMillis() - loc.time) / 1000L).coerceAtLeast(0)
                val state = when {
                    ageSec <= 10L -> "Fresh"
                    ageSec > 120L -> "Stale"
                    else -> "Fresh"
                }
                GpsSnapshot(
                    lat = loc.latitude,
                    lon = loc.longitude,
                    accM = loc.accuracy,
                    fixAgeSec = ageSec,
                    state = state
                )
            }
        } catch (_: Throwable) {
            // SecurityException / Timeout / Interrupted / Execution exceptions fallback
            GpsSnapshot(null, null, null, Long.MAX_VALUE, "None")
        }
    }
}
